package com.mangastudio.backend.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mangastudio.backend.dto.request.GoogleLoginRequest;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import com.mangastudio.backend.dto.request.RegisterRequest;
import com.mangastudio.backend.dto.request.RequestOtpRequest;
import com.mangastudio.backend.dto.request.VerifyOtpRequest;
import com.mangastudio.backend.dto.response.JwtResponse;
import com.mangastudio.backend.dto.response.MessageResponse;
import com.mangastudio.backend.entity.OtpCode;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.OtpRepository;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.security.JwtUtils;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.AuthService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;

@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    // Inject the necessary dependencies for OTP and Email
    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;

    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository,
                           RoleRepository roleRepository, PasswordEncoder encoder, JwtUtils jwtUtils,
                           OtpRepository otpRepository, JavaMailSender mailSender) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.otpRepository = otpRepository;
        this.mailSender = mailSender;
    }

    @Value("${manga.app.googleClientId}")
    private String googleClientId;

    @Override
    @Transactional
    public MessageResponse generateOtpForEmail(RequestOtpRequest request) {
        String requestedEmail = request.getEmail().trim();
        User user = userRepository.findByEmailIgnoreCase(requestedEmail)
                .orElseThrow(() -> new RuntimeException("Error: No account is linked to this email."));
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccessDeniedException("Your account has been locked. Please contact Admin.");
        }
        String userEmail = user.getEmail();
        if (userEmail == null || userEmail.isBlank()) {
            throw new RuntimeException("Error: This account does not have an email address for OTP login.");
        }

        // 2. Delete any old OTPs for this email to prevent spam/confusion
        otpRepository.deleteByEmail(userEmail);

        // 3. Generate a random 6-digit OTP
        String otpCode = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));

        // 4. Save the OTP to the database with a 5-minute expiration
        OtpCode newOtp = OtpCode.builder()
                .email(userEmail)
                .code(otpCode)
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(newOtp);

        // 5. Send the OTP via Email
        sendOtpEmail(userEmail, otpCode);

        // 6. Return response asking user to check email
        return new MessageResponse("An OTP has been sent to your email.");
    }

    @Override
    @Transactional
    public JwtResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new RuntimeException("Error: Invalid email or OTP code!"));
        String canonicalEmail = user.getEmail();

        // 1. Find the OTP record in the database
        OtpCode otpRecord = otpRepository.findByEmailAndCode(canonicalEmail, request.getOtpCode().trim())
                .orElseThrow(() -> new RuntimeException("Error: Invalid OTP code!"));

        // 2. Check if the OTP has expired
        if (LocalDateTime.now().isAfter(otpRecord.getExpirationTime())) {
            otpRepository.delete(otpRecord);
            throw new RuntimeException("Error: OTP code has expired. Please login again.");
        }

        // 3. OTP is valid and belongs to the resolved account.
        if (Boolean.FALSE.equals(user.getIsActive())) {
            otpRepository.delete(otpRecord);
            throw new AccessDeniedException("Your account has been locked. Please contact Admin.");
        }

        // 4. Create authentication token and set it in context
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 5. Generate the final JWT Token and invalidate any older login.
        String sessionId = UUID.randomUUID().toString();
        user.setActiveSessionId(sessionId);
        userRepository.saveAndFlush(user);
        String jwt = jwtUtils.generateJwtToken(authentication, sessionId);

        String role = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .findFirst()
                .orElse("ROLE_USER");

        // 6. Clean up: Delete the used OTP so it cannot be reused
        otpRepository.delete(otpRecord);

        return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), role);
    }

    private static final java.util.Map<String, String> PUBLIC_REGISTRATION_ROLES = java.util.Map.of(
            "mangaka", "Mangaka",
            "assistant", "Assistant",
            "tantou editor", "Tantou Editor",
            "editorial board", "Editorial Board"
    );

    @Override
    @Transactional
    public MessageResponse registerUser(RegisterRequest request) {
        String requestedRole = request.getRole() == null ? "" : request.getRole().trim();
        String normalizedRole = requestedRole
                .replaceFirst("(?i)^ROLE_", "")
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(java.util.Locale.ROOT);

        if ("admin".equals(normalizedRole)) {
            throw new AccessDeniedException("Admin accounts cannot be created through public registration.");
        }

        String publicRoleName = PUBLIC_REGISTRATION_ROLES.get(normalizedRole);
        if (publicRoleName == null) {
            throw new RuntimeException("Error: Public registration only supports Mangaka, Assistant, Tantou Editor, or Editorial Board.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        
        // 1. Chỉ check trùng Email nếu người dùng CÓ NHẬP email
        String email = request.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Error: Email is already in use!");
            }
        } else {
            email = null; // Chuẩn hóa thành null để lưu vào DB
        }

        // 2. Chỉ check trùng Số điện thoại nếu người dùng CÓ NHẬP số điện thoại
        String phone = request.getPhoneNumber();
        if (phone != null && !phone.trim().isEmpty()) {
            if (userRepository.existsByPhoneNumber(phone)) {
                throw new RuntimeException("Error: Phone number is already in use!");
            }
        } else {
            phone = null;
        }

        Role userRole = roleRepository.findByRoleName(publicRoleName)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        User user = User.builder()
                .username(request.getUsername())
                .email(email) // Sẽ truyền null nếu user chỉ nhập Username
                .phoneNumber(phone) // Lưu số điện thoại
                .passwordHash(encoder.encode(request.getPassword()))
                .role(userRole)
                .isActive(true)
                .build();
        userRepository.save(user);

        return new MessageResponse("User registered successfully!");
    }

    // --- Helper Method to send Email ---
    private void sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[MangaStudio] Your Login Security Code");
            message.setText("Hello,\n\nYour One-Time Password (OTP) for login is: " + otpCode
                    + "\n\nThis code will expire in 5 minutes. Please do not share this code with anyone.\n\n"
                    + "If you did not request this code, please secure your account immediately.\n\n"
                    + "Best regards,\nMangaStudio Security System");

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println(">>> [EMAIL ERROR] Failed to send OTP email: " + e.getMessage());
            throw new RuntimeException("Error: Failed to send OTP email. Please try again later.");
        }
    }

    @Override
    @Transactional
    public JwtResponse googleLogin(GoogleLoginRequest request) {
        try {
            if (googleClientId == null || googleClientId.isBlank()) {
                throw new RuntimeException("Error: Google login is not configured on the server.");
            }
            if (request == null || request.getToken() == null || request.getToken().isBlank()) {
                throw new RuntimeException("Error: Google ID token is required.");
            }

            // 1. Khởi tạo cỗ máy xác thực của Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId.trim()))
                    .build();

            // 2. Xác minh Token từ Frontend gửi lên
            GoogleIdToken idToken = verifier.verify(request.getToken().trim());
            if (idToken == null) {
                throw new RuntimeException("Error: Invalid or expired Google ID token.");
            }

            // 3. Rút trích thông tin người dùng từ Google
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail() == null ? "" : payload.getEmail().trim();
            String name = (String) payload.get("name");
            if (email.isBlank() || !Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new RuntimeException("Error: Google did not provide a verified email address.");
            }

            // 4. Kiểm tra xem người dùng đã có tài khoản trong hệ thống chưa
            Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
            User user;

            if (userOptional.isPresent()) {
                user = userOptional.get();
                // Bắt buộc kiểm tra xem tài khoản có bị Admin khóa không (FE-03)
                if (Boolean.FALSE.equals(user.getIsActive())) {
                    throw new RuntimeException("Error: Your account has been locked. Please contact Admin.");
                }
            } else {
                // 5. Nếu chưa có, tự động đăng ký tài khoản mới cho họ
                Role defaultRole = roleRepository.findByRoleName("Mangaka")
                        .orElseThrow(() -> new RuntimeException("Error: Default role not found."));

                // Tạo một mật khẩu ngẫu nhiên siêu khó để bảo vệ tài khoản (họ sẽ luôn đăng
                // nhập qua Google)
                String randomPassword = UUID.randomUUID().toString();

                // Cắt tên email ra làm username mặc định (Ví dụ: "ducky@gmail.com" ->
                // "ducky_abcd")
                String baseUsername = createUniqueGoogleUsername(email);

                user = User.builder()
                        .username(baseUsername)
                        .email(email)
                        .fullName(name == null || name.isBlank() ? email.split("@")[0] : name.trim())
                        .passwordHash(encoder.encode(randomPassword))
                        .role(defaultRole)
                        .isActive(true)
                        .build();
                user = userRepository.save(user);
            }

            // 6. Gắn quyền và cấp phát JWT cho Frontend truy cập
            UserDetailsImpl userDetails = UserDetailsImpl.build(user);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String sessionId = UUID.randomUUID().toString();
            user.setActiveSessionId(sessionId);
            userRepository.saveAndFlush(user);
            String jwt = jwtUtils.generateJwtToken(authentication, sessionId);
            String role = userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .findFirst()
                    .orElse("ROLE_USER");

            return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), role);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error: Google token verification could not be completed. Please try again.", e);
        }
    }

    private String createUniqueGoogleUsername(String email) {
        String localPart = email.split("@")[0]
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (localPart.isBlank()) localPart = "google_user";
        if (localPart.length() > 70) localPart = localPart.substring(0, 70);

        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = localPart + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            if (!userRepository.existsByUsername(candidate)) return candidate;
        }
        throw new RuntimeException("Error: Could not create a unique username for this Google account.");
    }
}

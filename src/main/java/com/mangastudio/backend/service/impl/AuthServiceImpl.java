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
import com.mangastudio.backend.dto.request.LoginRequest;
import com.mangastudio.backend.dto.request.RegisterRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    // Inject the necessary dependencies for OTP and Email
    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;

    @Value("${manga.app.googleClientId}")
    private String googleClientId;

    @Override
    @Transactional
    public MessageResponse authenticateUserAndGenerateOtp(LoginRequest loginRequest) {
        // 1. Verify username and password using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String userEmail = userDetails.getEmail();

        // 2. Delete any old OTPs for this email to prevent spam/confusion
        otpRepository.deleteByEmail(userEmail);

        // 3. Generate a random 6-digit OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));

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
        return new MessageResponse("Authentication successful! An OTP has been sent to your email.");
    }

    @Override
    @Transactional
    public JwtResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        // 1. Find the OTP record in the database
        OtpCode otpRecord = otpRepository.findByEmailAndCode(request.getEmail(), request.getOtpCode())
                .orElseThrow(() -> new RuntimeException("Error: Invalid OTP code!"));

        // 2. Check if the OTP has expired
        if (LocalDateTime.now().isAfter(otpRecord.getExpirationTime())) {
            otpRepository.delete(otpRecord);
            throw new RuntimeException("Error: OTP code has expired. Please login again.");
        }

        // 3. OTP is valid! Retrieve the user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User not found!"));

        // 4. Create authentication token and set it in context
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 5. Generate the final JWT Token
        String jwt = jwtUtils.generateJwtToken(authentication);

        String role = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .findFirst()
                .orElse("ROLE_USER");

        // 6. Clean up: Delete the used OTP so it cannot be reused
        otpRepository.delete(otpRecord);

        return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), role);
    }

    @Override
    @Transactional
    public MessageResponse registerUser(RegisterRequest request) {
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

        Role userRole = roleRepository.findByRoleName(request.getRole())
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
            // 1. Khởi tạo cỗ máy xác thực của Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // 2. Xác minh Token từ Frontend gửi lên
            GoogleIdToken idToken = verifier.verify(request.getToken());
            if (idToken == null) {
                throw new RuntimeException("Error: Invalid or expired Google ID token.");
            }

            // 3. Rút trích thông tin người dùng từ Google
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // 4. Kiểm tra xem người dùng đã có tài khoản trong hệ thống chưa
            Optional<User> userOptional = userRepository.findByEmail(email);
            User user;

            if (userOptional.isPresent()) {
                user = userOptional.get();
                // Bắt buộc kiểm tra xem tài khoản có bị Admin khóa không (FE-03)
                if (!user.getIsActive()) {
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
                String baseUsername = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);

                user = User.builder()
                        .username(baseUsername)
                        .email(email)
                        .fullName(name)
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

            String jwt = jwtUtils.generateJwtToken(authentication);
            String role = userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .findFirst()
                    .orElse("ROLE_USER");

            return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), role);

        } catch (Exception e) {
            throw new RuntimeException("Error: Google login failed - " + e.getMessage());
        }
    }
}
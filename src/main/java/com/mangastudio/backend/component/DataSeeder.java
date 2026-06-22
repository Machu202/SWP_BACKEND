package com.mangastudio.backend.component;

import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. Bơm Role nếu Database đang trống rỗng
        if (roleRepository.count() == 0) {
            List<String> roles = List.of("ADMIN", "EDITORIAL_BOARD", "TANTOU_EDITOR", "MANGAKA", "ASSISTANT", "READER");
            for (String roleName : roles) {
                Role role = new Role();
                role.setRoleName(roleName);
                roleRepository.save(role);
            }
            System.out.println("Successfully seeded roles into the database.");
        }

        // 2. Bơm 4 User mẫu phục vụ Demo ngày mai
        if (userRepository.count() == 0) {
            createUser("admin_vip", "123456", "admin@mangastudio.com", "ADMIN");
            createUser("mangaka_oda", "123456", "oda@mangastudio.com", "MANGAKA");
            createUser("tantou_linh", "123456", "linh@mangastudio.com", "TANTOU_EDITOR");
            createUser("assistant_huy", "123456", "huy@mangastudio.com", "ASSISTANT");
            System.out.println("Successfully seeded demo users into the database.");
        }
    }

    private void createUser(String username, String password, String email, String roleName) {
        Role role = roleRepository.findByRoleName(roleName);
        if (role != null) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password); // Mật khẩu thô dùng tạm cho Demo, sau này sẽ bọc BCrypt sau
            user.setEmail(email);
            user.setRole(role);
            userRepository.save(user);
        }
    }
}
package com.mangastudio.backend.component;

import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra xem database đã có Role chưa
        if (roleRepository.count() == 0) {
            System.out.println(">>> Initializing Seed Data for the system...");
            
            List<String> roles = Arrays.asList("Mangaka", "Assistant", "Tantou Editor", "Editorial Board", "Admin");
            
            for (String roleName : roles) {
                Role role = Role.builder()
                        .roleName(roleName)
                        .build();
                roleRepository.save(role);
            }
            
            System.out.println(">>> 5 Roles Added");
        }
    }
}
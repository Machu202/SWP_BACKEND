package com.mangastudio.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// import com.mangastudio.backend.security.AuthEntryPointJwt;
// import com.mangastudio.backend.security.AuthTokenFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // TODO: Uncomment these lines after creating the security components
    // private final AuthEntryPointJwt unauthorizedHandler;
    // private final AuthTokenFilter authTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Defines the algorithm used to hash passwords before saving to the database
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF because we use JWT (Stateless)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Enable CORS using the CorsConfig setup
            .cors(cors -> cors.configure(http))
            
            // TODO: Uncomment this to handle unauthorized access errors (401)
            // .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            
            // Set session management to stateless (no server-side sessions)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure route permissions
            // Configure route permissions
            .authorizeHttpRequests(auth -> auth
                // Allow public access to Authentication APIs
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Allow public access to Swagger UI documentation
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // [BỔ SUNG FE-09] Mở cửa cho kết nối WebSocket Handshake
                .requestMatchers("/ws/**").permitAll()
                
                // All other API requests must be authenticated with a valid JWT
                .anyRequest().authenticated()
            );

        // TODO: Uncomment this to add the JWT filter before the standard authentication filter
        // http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
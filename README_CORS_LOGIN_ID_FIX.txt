Patch: Backend 90% CORS + login id fix

Fixed:
1. SecurityConfig.java
   - Replaced invalid Spring Security 6 CORS config:
     .cors(cors -> cors.configure(http))
   - With:
     .cors(Customizer.withDefaults())
   - Added import:
     org.springframework.security.config.Customizer

2. AuthController.java
   - Added id to /api/v1/auth/login response:
     responseBody.put("id", userDetails.getId());

Why:
- Fixes browser CORS/preflight errors from frontend localhost:5173.
- Lets frontend localStorage save userId after login.

Only needed backend changes included:
1. SecurityConfig.java
   - Spring Security 6 CORS fix: .cors(Customizer.withDefaults())

2. AuthController.java
   - /auth/login returns type=Bearer and id for frontend localStorage userId.
   - /auth/request-otp endpoint added for frontend OTP login flow.

3. PageVersionController.java
   - GET /api/v1/page-versions/pages/{pageId}
   - PATCH /api/v1/page-versions/{versionId}/restore

Nothing else was intentionally changed.
- application.yml kept from the working backend.
- .env.example kept from the working backend.
- entities/repositories/services were not changed.
- role/security authority mapping was not changed.
- User table mapping was not changed.

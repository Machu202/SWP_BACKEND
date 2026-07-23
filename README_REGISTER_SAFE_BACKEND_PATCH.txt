90% backend register-safe patch

Base: original working SWP_BACKEND(6).zip.

This patch intentionally does NOT change application.yml, datasource, Supabase URL, entities, repositories, or services.

Changed only:
- SecurityConfig.java: Spring Security 6 CORS defaults + OPTIONS preflight permit.
- CorsConfig.java: adds CorsConfigurationSource so Security CORS and MVC CORS use the same allowed origins from app.cors.allowed-origins.
- AuthController.java: login response returns type/id and adds request-otp endpoint. Register method is unchanged.
- PageVersionController.java: page version listing/restoration endpoints.

Reason:
The previous 90% safe patch changed SecurityConfig but did not add a Security-level CorsConfigurationSource. In some browsers this can block POST /api/v1/auth/register preflight, so the request never reaches Supabase.

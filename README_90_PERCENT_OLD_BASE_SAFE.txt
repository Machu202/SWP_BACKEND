Backend patch rebuilt from the exact uploaded working backend: SWP_BACKEND-BackEndTest2.zip

Only changed:
- AuthController.java: /auth/login response now also returns type="Bearer" and id.
- Added PageVersionController.java: list and restore page versions using existing repositories.

Not changed:
- application.yml
- SecurityConfig.java
- CorsConfig.java
- AuthServiceImpl.java
- UserDetailsServiceImpl.java
- UserRepository.java
- User.java
- Supabase datasource/config
- password hashing/login authentication flow

Reason:
The uploaded old backend already logs into Supabase correctly, so this patch does not touch the working auth manager, password verification, database config, or CORS setup.

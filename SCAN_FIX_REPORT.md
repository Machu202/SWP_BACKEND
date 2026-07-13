> **Historical note:** The final credential-restored package intentionally restores the original runtime values. See `/CREDENTIAL_RESTORE_NOTES.md` and `/README_RUN.md`.

# Backend Scan and Fix Report

## Fixed issues

1. **Database config was unsafe and error-prone**
   - Removed hardcoded passwords/secrets from `application.yml`.
   - Moved DB password, mail password, Cloudinary secrets, Google client ID, and JWT secret to environment variables / `.env`.
   - Added `.env.example`.

2. **Supabase connection pool errors**
   - Reduced Hikari pool default to `maximum-pool-size=2` and `minimum-idle=1`.
   - Added idle timeout, connection timeout, and max lifetime settings.
   - Removed duplicate username/password from JDBC URL pattern. Use `DB_USERNAME` and `DB_PASSWORD` instead.

3. **PostgreSQL table mapping for `User`**
   - Changed `@Table(name = "[User]")` to `@Table(name = "\"User\"")`.
   - This matches the existing Supabase table named `User` and avoids SQL Server bracket syntax in PostgreSQL.

4. **CORS and WebSocket compatibility**
   - Added support for Vite and Live Server origins:
     - `http://localhost:5173`
     - `http://127.0.0.1:5173`
     - `http://localhost:5500`
     - `http://127.0.0.1:5500`
   - WebSocket `/ws` uses the same origin list.

5. **Spring Security CORS configuration**
   - Replaced the fragile `.cors(cors -> cors.configure(http))` setup with `Customizer.withDefaults()`.
   - Kept JWT stateless security.
   - Kept auth, Swagger, health, WebSocket handshake, and telemetry view tracking public where appropriate.

6. **Login response compatibility**
   - `/api/v1/auth/login` now returns:
     - `token`
     - `type`
     - `id`
     - `role`
     - `username`
     - `email`
   - This supports frontend token storage and WebSocket subscriptions by user ID.

7. **Role authority normalization**
   - Role names with spaces, such as `Editorial Board` and `Tantou Editor`, are normalized to Spring authorities:
     - `ROLE_EDITORIAL_BOARD`
     - `ROLE_TANTOU_EDITOR`
   - Updated Board Vote authorization accordingly.

8. **Manga series dashboard endpoint**
   - Added `GET /api/v1/manga-series`.
   - Added optional status filtering: `GET /api/v1/manga-series?status=REVIEWING`.
   - This lets Tantou/Admin/Editorial Board dashboards load manga data.

9. **Manga response display names**
   - Added username/email fallback when `fullName` is empty.
   - Prevents frontend from showing blank mangaka/tantou names.

10. **Clean export**
    - Generated a clean ZIP without `.git`, `target`, IDE files, or compiled classes.

## Files changed

- `src/main/resources/application.yml`
- `.env.example`
- `src/main/java/com/mangastudio/backend/config/CorsConfig.java`
- `src/main/java/com/mangastudio/backend/config/WebSocketConfig.java`
- `src/main/java/com/mangastudio/backend/config/SecurityConfig.java`
- `src/main/java/com/mangastudio/backend/controller/AuthController.java`
- `src/main/java/com/mangastudio/backend/controller/BoardVoteController.java`
- `src/main/java/com/mangastudio/backend/controller/MangaSeriesController.java`
- `src/main/java/com/mangastudio/backend/entity/User.java`
- `src/main/java/com/mangastudio/backend/security/UserDetailsImpl.java`
- `src/main/java/com/mangastudio/backend/repository/MangaSeriesRepository.java`
- `src/main/java/com/mangastudio/backend/service/MangaSeriesService.java`
- `src/main/java/com/mangastudio/backend/service/impl/MangaSeriesServiceImpl.java`

## How to run

1. Copy `.env.example` to `.env` in the backend root.
2. Fill in your real Supabase password:

```env
DB_PASSWORD=YOUR_REAL_SUPABASE_DATABASE_PASSWORD
```

3. Run:

```powershell
cd C:\Users\admin\Documents\GitHub\SWP_BACKEND
taskkill /F /IM java.exe
.\mvnw.cmd clean
.\mvnw.cmd spring-boot:run -DskipTests
```

4. Test Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

## Important security note

You previously pasted a database password and other secrets. Rotate/reset those credentials in Supabase/Cloudinary/Gmail before pushing or sharing the project.

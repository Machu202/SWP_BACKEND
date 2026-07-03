# Backend Config Fix Notes

Applied only the requested backend configuration fixes.

## Changed

- `pom.xml`
  - Spring Boot parent version set to `4.1.0`
  - `springdoc.version` set to `2.8.9`

- `src/main/resources/application.yml`
  - server port set to `8080`
  - JWT settings filled
  - Google Client ID filled
  - Cloudinary config filled
  - Supabase PostgreSQL datasource filled
  - Hikari pool set to max `5`, min idle `1`, timeout values added
  - Hibernate PostgreSQL dialect and formatted SQL enabled
  - Gmail SMTP config filled
  - multipart upload limits kept at `10MB`/`50MB`
  - CORS origins kept for Vite/localhost frontend

- `.env.example`
  - Updated to match the runtime configuration keys.

## Not changed

No Java controller/service/entity logic was changed.
No frontend files were changed.

## Run

```bash
./mvnw spring-boot:run
```

Swagger should be available at:

```text
http://localhost:8080/swagger-ui.html
```

The Maven wrapper needs internet access the first time it downloads Maven/dependencies.

90-percent backend completion patch

Adds:
- POST /api/v1/auth/request-otp so frontend OTP login can request an OTP before /verify-otp.
- GET /api/v1/page-versions/pages/{pageId} to list image history for a page.
- PATCH /api/v1/page-versions/{versionId}/restore to restore an old page image version and archive the restore as a new version.

Changed/added files:
- src/main/java/com/mangastudio/backend/controller/AuthController.java
- src/main/java/com/mangastudio/backend/controller/PageVersionController.java

Note:
- The frontend build was verified. Backend Maven build could not be run in this sandbox because the Maven wrapper attempted to download Maven from the internet, which is blocked here. The backend patch is source-level and follows the existing repository/entity patterns.

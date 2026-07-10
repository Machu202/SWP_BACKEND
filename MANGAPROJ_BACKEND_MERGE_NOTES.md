# MANGAPROJ Backend Merge Notes

## Request

Merge the backend from `MANGAPROJ(1).rar` with `SWP_BACKEND(4).zip` and keep it working.

## Done

Started from `SWP_BACKEND(4).zip` because it includes newer frontend-compatible task DTO fixes.

The RAR backend was inspected, but it was not blindly overlaid because it would remove the newer `TaskResponse` endpoints used by Assistant/Mangaka screens.

Applied safe working fixes:

- pom.xml
- src/main/java/com/mangastudio/backend/entity/Page.java
- src/main/java/com/mangastudio/backend/repository/PageVersionRepository.java
- src/main/java/com/mangastudio/backend/service/PageService.java
- src/main/java/com/mangastudio/backend/service/impl/PageServiceImpl.java
- src/main/resources/application.yml

## Why this is safer

- Keeps newer `TaskResponse` support so Assistant receives `referenceImageUrl` and task context.
- Keeps the PageVersion transient-instance fix by saving `Page` first and creating `PageVersion` inside `PageServiceImpl`.
- Uses the known working Spring Boot 3.2.4 dependency set instead of the unstable Boot 4.1.0 POM from the uploaded base.
- Uses environment variables in `application.yml` so database/cloud/mail secrets are not hardcoded.

## Required local setup

Copy `.env.example` to `.env` and fill in your real Supabase, Cloudinary, JWT, Google, and mail values.

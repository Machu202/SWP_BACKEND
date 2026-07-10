# Backend Compile Fix

This package fixes the compilation errors from the latest Maven output.

## Problems shown in the log

1. `PageRepository.java` contained the wrong public interface.
   - Error: `interface PageVersionRepository is public, should be declared in a file named PageVersionRepository.java`
   - Error: `file does not contain class com.mangastudio.backend.repository.PageRepository`

2. Lombok was not generating methods/builders.
   - Many errors like `cannot find symbol getId()`, `builder()`, `getUsername()`, etc.
   - This happens when Lombok annotation processing is not configured correctly.

## Fixed

- Restored `PageRepository.java` so it contains only `PageRepository`.
- Restored `PageVersionRepository.java` so it contains only `PageVersionRepository`.
- Updated `pom.xml` to:
  - Spring Boot `3.2.4`
  - Java 17
  - explicit `lombok.version`
  - explicit Maven compiler plugin configuration
  - explicit Lombok annotation processor version
- Kept the Page/PageVersion save-order fix:
  - Save and flush `Page`
  - Then create `PageVersion`
- Removed `@EntityListeners(PageVersioningListener.class)` from `Page.java`.

## After replacing files

Run:

```powershell
cd C:\Users\admin\Documents\GitHub\SWP_BACKEND
.\mvnw.cmd clean
.\mvnw.cmd spring-boot:run -DskipTests
```

Backend 90% patch rebuilt from the uploaded working old backend (SWP_BACKEND-BackEndTest2.zip).

Only changes:
1. AuthController.java
   - /auth/login still uses the old working AuthenticationManager flow.
   - Added responseBody.put("type", "Bearer");
   - Added responseBody.put("id", userDetails.getId());

2. PageVersionController.java
   - Added GET /api/v1/page-versions/pages/{pageId}
   - Added PATCH /api/v1/page-versions/{versionId}/restore

3. PageVersionRepository.java
   - Added findTopByPageIdOrderByVersionNumberDesc for restore snapshot numbering.

Not changed:
- application.yml / Supabase connection
- pom.xml
- SecurityConfig.java
- CorsConfig.java
- AuthServiceImpl.registerUser()
- UserDetailsServiceImpl
- UserRepository login methods except PageVersionRepository only
- User entity / role entity
- password/authentication mechanism

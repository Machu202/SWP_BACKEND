Patch: Manga series cover image backend persistence fix

Main cause:
- The backend MangaSeries entity/DTO/response did not include cover_image_url / coverImageUrl.
- The frontend uploaded a cover to Cloudinary, but the create-series request field was ignored by backend.
- Therefore Supabase Resources had the image, but manga_series did not store/return that image for series cards.

Changed backend files:
- src/main/java/com/mangastudio/backend/entity/MangaSeries.java
- src/main/java/com/mangastudio/backend/dto/request/MangaSeriesCreateRequest.java
- src/main/java/com/mangastudio/backend/dto/request/MangaSeriesUpdateRequest.java
- src/main/java/com/mangastudio/backend/dto/response/MangaSeriesResponse.java
- src/main/java/com/mangastudio/backend/service/impl/MangaSeriesServiceImpl.java

No auth/database/application.yml config was changed.
Important: existing old series with empty cover_image_url still need their cover_image_url filled once. New series created after this patch will save the cover correctly.

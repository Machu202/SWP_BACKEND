# Page Upload / PageVersion Fix

## Fixed error

`org.hibernate.TransientPropertyValueException: Instance of 'PageVersion' references an unsaved transient instance of 'Page'`

## Changed files

- `src/main/java/com/mangastudio/backend/entity/Page.java`
- `src/main/java/com/mangastudio/backend/repository/PageVersionRepository.java`
- `src/main/java/com/mangastudio/backend/service/PageService.java`
- `src/main/java/com/mangastudio/backend/service/impl/PageServiceImpl.java`

## What changed

1. Removed `@EntityListeners(PageVersioningListener.class)` from `Page`.
   - Versioning is now controlled in `PageServiceImpl`, not by a JPA listener.

2. `PageServiceImpl.addPageToChapter(...)`
   - Saves `Page` first using `pageRepository.saveAndFlush(page)`.
   - Then creates `PageVersion` with the saved/managed page reference.

3. `PageServiceImpl.updatePageImage(...)`
   - Saves updated `Page` first using `pageRepository.saveAndFlush(page)`.
   - Then creates the next `PageVersion`.

4. `PageVersionRepository`
   - Added `findTopByPageIdOrderByVersionNumberDesc(Long pageId)` to calculate the next version safely.

## Why this fixes it

`PageVersion.page` is a foreign key to `Page`, so the `Page` row must exist before `PageVersion` is inserted.

The correct order is now:

1. Upload image
2. Save and flush `Page`
3. Create and save `PageVersion`

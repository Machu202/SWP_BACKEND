# Backend Series Cover Persistence Fix

## Fixed

MangaSeries now persists real cover/description data so other users can see covers instead of relying only on frontend localStorage.

## Changed backend files

- `MangaSeries.java`
- `MangaSeriesCreateRequest.java`
- `MangaSeriesUpdateRequest.java`
- `MangaSeriesResponse.java`
- `MangaSeriesServiceImpl.java`

## New persisted fields

- `description`
- `coverImageUrl` / DB column `cover_image_url`

`ddl-auto:update` should add the missing columns automatically.

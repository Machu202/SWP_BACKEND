Patch: Task metadata response fix

Problem:
- Kanban/Assignments showed "No series", "Page ?", and "Assistant: Unassigned" even when task data was assigned.
- Backend returned raw Task entities. Task.hitbox.page is @JsonIgnore, so page/chapter/series data was not serialized.
- Frontend expected flat fields like seriesTitle, pageNumber, assistantName.

Fixed:
- TaskController now returns a safe response map with flat fields:
  assistantId, assistantName, pageId, pageNumber, pageImageUrl, chapterId, chapterNumber, chapterTitle, seriesId, seriesTitle, hitboxId.
- TaskRepository uses fetch-join queries so the controller can access hitbox/page/chapter/series/assistant data.
- Datasource, application.yml, auth, Supabase config were not changed.

Changed files:
- src/main/java/com/mangastudio/backend/controller/TaskController.java
- src/main/java/com/mangastudio/backend/repository/TaskRepository.java

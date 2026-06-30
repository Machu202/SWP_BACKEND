package com.mangastudio.backend.dto.response;

import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;

import java.time.LocalDateTime;

/**
 * DTO used by Frontend task screens.
 *
 * Important:
 * Task -> Hitbox -> Page is hidden in the entity graph because Hitbox.page has @JsonIgnore.
 * Without this DTO, Assistant cannot receive the original manga page image.
 */
public class TaskResponse {

    private Long id;
    private String status;
    private String description;
    private String submittedImageUrl;
    private LocalDateTime createdAt;

    private Long hitboxId;
    private Double xCoord;
    private Double yCoord;
    private Double width;
    private Double height;

    private Long pageId;
    private Integer pageNumber;
    private String referenceImageUrl;

    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;

    private Long seriesId;
    private String seriesTitle;

    private Long mangakaId;
    private String mangakaName;

    private Long assistantId;
    private String assistantName;

    public static TaskResponse from(Task task) {
        TaskResponse response = new TaskResponse();

        if (task == null) {
            return response;
        }

        response.setId(task.getId());
        response.setStatus(task.getStatus());
        response.setDescription(task.getDescription());
        response.setSubmittedImageUrl(task.getSubmittedImageUrl());
        response.setCreatedAt(task.getCreatedAt());

        User mangaka = task.getMangaka();
        if (mangaka != null) {
            response.setMangakaId(mangaka.getId());
            response.setMangakaName(firstNonBlank(mangaka.getUsername(), mangaka.getEmail()));
        }

        User assistant = task.getAssistant();
        if (assistant != null) {
            response.setAssistantId(assistant.getId());
            response.setAssistantName(firstNonBlank(assistant.getUsername(), assistant.getEmail()));
        }

        Hitbox hitbox = task.getHitbox();
        if (hitbox != null) {
            response.setHitboxId(hitbox.getId());
            response.setXCoord(hitbox.getXCoord());
            response.setYCoord(hitbox.getYCoord());
            response.setWidth(hitbox.getWidth());
            response.setHeight(hitbox.getHeight());

            Page page = hitbox.getPage();
            if (page != null) {
                response.setPageId(page.getId());
                response.setPageNumber(page.getPageNumber());
                response.setReferenceImageUrl(page.getImageUrl());

                Chapter chapter = page.getChapter();
                if (chapter != null) {
                    response.setChapterId(chapter.getId());
                    response.setChapterNumber(chapter.getChapterNumber());
                    response.setChapterTitle(chapter.getTitle());

                    MangaSeries series = chapter.getMangaSeries();
                    if (series != null) {
                        response.setSeriesId(series.getId());
                        response.setSeriesTitle(series.getTitle());
                    }
                }
            }
        }

        return response;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubmittedImageUrl() {
        return submittedImageUrl;
    }

    public void setSubmittedImageUrl(String submittedImageUrl) {
        this.submittedImageUrl = submittedImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getHitboxId() {
        return hitboxId;
    }

    public void setHitboxId(Long hitboxId) {
        this.hitboxId = hitboxId;
    }

    public Double getXCoord() {
        return xCoord;
    }

    public void setXCoord(Double xCoord) {
        this.xCoord = xCoord;
    }

    public Double getYCoord() {
        return yCoord;
    }

    public void setYCoord(Double yCoord) {
        this.yCoord = yCoord;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getReferenceImageUrl() {
        return referenceImageUrl;
    }

    public void setReferenceImageUrl(String referenceImageUrl) {
        this.referenceImageUrl = referenceImageUrl;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public Long getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(Long seriesId) {
        this.seriesId = seriesId;
    }

    public String getSeriesTitle() {
        return seriesTitle;
    }

    public void setSeriesTitle(String seriesTitle) {
        this.seriesTitle = seriesTitle;
    }

    public Long getMangakaId() {
        return mangakaId;
    }

    public void setMangakaId(Long mangakaId) {
        this.mangakaId = mangakaId;
    }

    public String getMangakaName() {
        return mangakaName;
    }

    public void setMangakaName(String mangakaName) {
        this.mangakaName = mangakaName;
    }

    public Long getAssistantId() {
        return assistantId;
    }

    public void setAssistantId(Long assistantId) {
        this.assistantId = assistantId;
    }

    public String getAssistantName() {
        return assistantName;
    }

    public void setAssistantName(String assistantName) {
        this.assistantName = assistantName;
    }
}

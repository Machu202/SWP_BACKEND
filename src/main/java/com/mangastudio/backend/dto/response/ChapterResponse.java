package com.mangastudio.backend.dto.response;

public class ChapterResponse {
    private Long id;
    private Long seriesId;
    private Long seriesDisplayNumber;
    private String seriesTitle;
    private Integer chapterNumber;
    private String title;
    private String publishStatus;
    private String mangakaName;
    private Long tantouId;
    private String tantouName;
    private Boolean reviewReady;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ChapterResponse value = new ChapterResponse();
        public Builder id(Long v) { value.id = v; return this; }
        public Builder seriesId(Long v) { value.seriesId = v; return this; }
        public Builder seriesDisplayNumber(Long v) { value.seriesDisplayNumber = v; return this; }
        public Builder seriesTitle(String v) { value.seriesTitle = v; return this; }
        public Builder chapterNumber(Integer v) { value.chapterNumber = v; return this; }
        public Builder title(String v) { value.title = v; return this; }
        public Builder publishStatus(String v) { value.publishStatus = v; return this; }
        public Builder mangakaName(String v) { value.mangakaName = v; return this; }
        public Builder tantouId(Long v) { value.tantouId = v; return this; }
        public Builder tantouName(String v) { value.tantouName = v; return this; }
        public Builder reviewReady(Boolean v) { value.reviewReady = v; return this; }
        public ChapterResponse build() { return value; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSeriesId() { return seriesId; }
    public void setSeriesId(Long seriesId) { this.seriesId = seriesId; }
    public Long getSeriesDisplayNumber() { return seriesDisplayNumber; }
    public void setSeriesDisplayNumber(Long seriesDisplayNumber) { this.seriesDisplayNumber = seriesDisplayNumber; }
    public String getSeriesTitle() { return seriesTitle; }
    public void setSeriesTitle(String seriesTitle) { this.seriesTitle = seriesTitle; }
    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }
    public String getMangakaName() { return mangakaName; }
    public void setMangakaName(String mangakaName) { this.mangakaName = mangakaName; }
    public Long getTantouId() { return tantouId; }
    public void setTantouId(Long tantouId) { this.tantouId = tantouId; }
    public String getTantouName() { return tantouName; }
    public void setTantouName(String tantouName) { this.tantouName = tantouName; }
    public Boolean getReviewReady() { return reviewReady; }
    public void setReviewReady(Boolean reviewReady) { this.reviewReady = reviewReady; }
}

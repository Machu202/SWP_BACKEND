package com.mangastudio.backend.dto.response;

import java.time.LocalDateTime;

public class MangaSeriesResponse {
    private Long id;
    private Long displayNumber;
    private String title;
    private String genre;
    private String summary;
    private String description;
    private String coverImageUrl;
    private String status;
    private String mangakaName;
    private Long tantouId;
    private String tantouName;
    private LocalDateTime createdAt;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final MangaSeriesResponse value = new MangaSeriesResponse();
        public Builder id(Long v) { value.id = v; return this; }
        public Builder displayNumber(Long v) { value.displayNumber = v; return this; }
        public Builder title(String v) { value.title = v; return this; }
        public Builder genre(String v) { value.genre = v; return this; }
        public Builder summary(String v) { value.summary = v; return this; }
        public Builder description(String v) { value.description = v; return this; }
        public Builder coverImageUrl(String v) { value.coverImageUrl = v; return this; }
        public Builder status(String v) { value.status = v; return this; }
        public Builder mangakaName(String v) { value.mangakaName = v; return this; }
        public Builder tantouId(Long v) { value.tantouId = v; return this; }
        public Builder tantouName(String v) { value.tantouName = v; return this; }
        public Builder createdAt(LocalDateTime v) { value.createdAt = v; return this; }
        public MangaSeriesResponse build() { return value; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDisplayNumber() { return displayNumber; }
    public void setDisplayNumber(Long displayNumber) { this.displayNumber = displayNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMangakaName() { return mangakaName; }
    public void setMangakaName(String mangakaName) { this.mangakaName = mangakaName; }
    public Long getTantouId() { return tantouId; }
    public void setTantouId(Long tantouId) { this.tantouId = tantouId; }
    public String getTantouName() { return tantouName; }
    public void setTantouName(String tantouName) { this.tantouName = tantouName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

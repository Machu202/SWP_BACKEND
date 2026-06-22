package com.mangastudio.backend.dto;

import lombok.Data;

@Data
public class MangaSeriesRequest {
    private String title;
    private String genre;
    private String summary;
    private Long mangakaId; // Nhận ID của tác giả đăng truyện
}
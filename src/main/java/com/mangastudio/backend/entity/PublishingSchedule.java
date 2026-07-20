package com.mangastudio.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Publishing_Schedule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PublishingSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private MangaSeries mangaSeries;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Column(name = "publish_date", nullable = false)
    private LocalDateTime publishDate;

    @Column(length = 50)
    private String frequency; // Weekly, Monthly

    @JsonProperty("chapterId")
    public Long chapterId() {
        return chapter != null ? chapter.getId() : null;
    }
}

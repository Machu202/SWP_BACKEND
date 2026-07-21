package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Chapter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByMangaSeriesIdOrderByChapterNumberAsc(Long seriesId);

    // Only chapters belonging to series assigned to the authenticated Tantou.
    List<Chapter> findByMangaSeries_Tantou_IdOrderByChapterNumberAsc(Long tantouId);

    /** Serializes page uploads for one chapter so its configured page limit cannot be exceeded. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select chapter from Chapter chapter where chapter.id = :chapterId")
    Optional<Chapter> findByIdForPageUpload(@Param("chapterId") Long chapterId);
}

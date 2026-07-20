package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.MangaSeries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MangaSeriesRepository extends JpaRepository<MangaSeries, Long> {
    List<MangaSeries> findByMangakaId(Long mangakaId);

    @Query("SELECT ms FROM MangaSeries ms LEFT JOIN FETCH ms.mangaka LEFT JOIN FETCH ms.tantou WHERE ms.tantou.id = :tantouId ORDER BY ms.createdAt DESC, ms.id DESC")
    List<MangaSeries> findAssignedToTantou(@Param("tantouId") Long tantouId);

    boolean existsByTantou_IdAndIdNot(Long tantouId, Long seriesId);

    boolean existsByMangaka_IdAndTantou_Id(Long mangakaId, Long tantouId);

    Page<MangaSeries> findByStatus(String status, Pageable pageable);

    // User-facing series numbers are dense among currently existing rows. The
    // real primary key remains untouched for relationships and API routes.
    long countByIdLessThanEqual(Long id);
}

package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.TantouFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TantouFeedbackRepository extends JpaRepository<TantouFeedback, Long> {
    // Lấy toàn bộ feedback của Biên tập viên trên một trang truyện
    List<TantouFeedback> findByPageId(Long pageId);
}
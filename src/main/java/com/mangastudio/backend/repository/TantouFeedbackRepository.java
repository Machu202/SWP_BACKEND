package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.TantouFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TantouFeedbackRepository extends JpaRepository<TantouFeedback, Long> {
    // Returns all Tantou Editor feedback for a manga page.
    List<TantouFeedback> findByPageId(Long pageId);
}

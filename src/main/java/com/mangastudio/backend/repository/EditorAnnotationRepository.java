package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.EditorAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EditorAnnotationRepository extends JpaRepository<EditorAnnotation, Long> {
    List<EditorAnnotation> findByPageId(Long pageId);
}
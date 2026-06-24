package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PageRepository extends JpaRepository<Page, Long> {
}
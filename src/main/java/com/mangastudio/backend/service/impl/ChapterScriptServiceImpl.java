package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.ChapterScript;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.ChapterScriptRepository;
import com.mangastudio.backend.service.ChapterScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChapterScriptServiceImpl implements ChapterScriptService {

    private final ChapterScriptRepository scriptRepository;
    private final ChapterRepository chapterRepository;

    @Override
    public ChapterScript getScriptByChapter(Long chapterId) {
        return scriptRepository.findByChapterId(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Script not found for this chapter"));
    }

    @Override
    @Transactional
    public ChapterScript saveOrUpdateScript(Long chapterId, Long mangakaId, String content) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));

        if (!chapter.getMangaSeries().getMangaka().getId().equals(mangakaId)) {
            throw new RuntimeException("Error: Only the Mangaka of this series can edit the script.");
        }

        // Tìm kịch bản cũ, nếu chưa có thì tạo mới
        ChapterScript script = scriptRepository.findByChapterId(chapterId)
                .orElse(new ChapterScript());

        script.setChapter(chapter);
        script.setContent(content);
        script.setUpdatedAt(LocalDateTime.now());

        return scriptRepository.save(script);
    }
}
package com.mangastudio.backend;

import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.ChapterScriptRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.service.impl.ChapterScriptServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChapterScriptEmptyStateTests {
    @Test
    void existingChapterWithoutScriptReturnsEmptyInsteadOfError() {
        ChapterScriptRepository scripts = mock(ChapterScriptRepository.class);
        ChapterRepository chapters = mock(ChapterRepository.class);
        MangaSeriesRepository series = mock(MangaSeriesRepository.class);
        when(chapters.findById(10L)).thenReturn(Optional.of(new Chapter()));
        when(scripts.findByChapterId(10L)).thenReturn(Optional.empty());

        ChapterScriptServiceImpl service = new ChapterScriptServiceImpl(scripts, chapters, series);
        assertNull(service.getScriptByChapter(10L));
    }
}

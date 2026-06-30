package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.ChapterScript;
import java.util.List;

public interface ChapterScriptService {
    ChapterScript getScriptByChapter(Long chapterId);
    ChapterScript saveOrUpdateScript(Long chapterId, Long mangakaId, String content);

    List<ChapterScript> getScriptsBySeries(Long seriesId);
}
package com.mangastudio.backend;

import com.cloudinary.Cloudinary;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.SystemParameter;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.repository.SystemParameterRepository;
import com.mangastudio.backend.service.impl.PageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MaxPagesPerChapterTests {

    private PageRepository pageRepository;
    private ChapterRepository chapterRepository;
    private Cloudinary cloudinary;
    private SystemParameterRepository parameterRepository;
    private PageServiceImpl service;

    @BeforeEach
    void setUp() {
        pageRepository = mock(PageRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        cloudinary = mock(Cloudinary.class);
        parameterRepository = mock(SystemParameterRepository.class);
        service = new PageServiceImpl(
                pageRepository,
                chapterRepository,
                cloudinary,
                mock(PageVersionRepository.class),
                parameterRepository);
    }

    @Test
    void blocksUploadBeforeCloudinaryWhenAdminLimitHasBeenReached() {
        User mangaka = User.builder().id(7L).username("mangaka").build();
        MangaSeries series = MangaSeries.builder().id(15L).mangaka(mangaka).build();
        Chapter chapter = Chapter.builder().id(21L).mangaSeries(series).build();
        SystemParameter limit = SystemParameter.builder()
                .paramKey("MAX_PAGES_PER_CHAPTER")
                .paramType("INTEGER")
                .paramValue("2")
                .build();

        when(chapterRepository.findByIdForPageUpload(21L)).thenReturn(Optional.of(chapter));
        when(pageRepository.existsByChapterIdAndPageNumber(21L, 3)).thenReturn(false);
        when(parameterRepository.findByParamKeyIgnoreCase("MAX_PAGES_PER_CHAPTER"))
                .thenReturn(Optional.of(limit));
        when(pageRepository.countByChapterId(21L)).thenReturn(2L);

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                service.addPageToChapter(21L, 3, mock(MultipartFile.class), 7L));

        assertEquals("Error: This chapter has reached the Admin limit of 2 pages.", error.getMessage());
        verifyNoInteractions(cloudinary);
    }
}

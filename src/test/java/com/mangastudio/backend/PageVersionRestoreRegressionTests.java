package com.mangastudio.backend;

import com.mangastudio.backend.controller.PageVersionController;
import com.mangastudio.backend.entity.*;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PageVersionRestoreRegressionTests {
    @Test
    void restoreSelectsHistoricalImageWithoutCreatingAnotherVersion() {
        User owner = User.builder().id(1L).username("owner").passwordHash("x").isActive(true)
                .role(Role.builder().id(1L).roleName("Mangaka").build()).build();
        MangaSeries series = MangaSeries.builder().id(2L).title("Series").mangaka(owner).status("DRAFT").build();
        Chapter chapter = Chapter.builder().id(3L).mangaSeries(series).chapterNumber(1).title("Chapter").build();
        Page page = Page.builder().id(4L).chapter(chapter).pageNumber(1).imageUrl("current.png").build();
        PageVersion version = PageVersion.builder().id(5L).page(page).imageUrl("version-1.png")
                .versionNumber(1).createdAt(LocalDateTime.now()).build();

        PageVersionRepository versionRepository = mock(PageVersionRepository.class);
        PageRepository pageRepository = mock(PageRepository.class);
        when(versionRepository.findById(5L)).thenReturn(Optional.of(version));
        when(pageRepository.saveAndFlush(page)).thenReturn(page);
        var principal = UserDetailsImpl.build(owner);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        var controller = new PageVersionController(versionRepository, pageRepository);
        assertEquals("version-1.png", controller.restoreVersion(5L, authentication).getBody().getImageUrl());
        verify(versionRepository, never()).save(any(PageVersion.class));
    }
}

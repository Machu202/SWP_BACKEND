package com.mangastudio.backend;

import com.mangastudio.backend.controller.PageVersionController;
import com.mangastudio.backend.entity.*;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.HitboxRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        HitboxRepository hitboxRepository = mock(HitboxRepository.class);
        Hitbox archived = Hitbox.builder().id(6L).page(page).pageVersion(version).createdBy(owner)
                .xCoord(10d).yCoord(20d).width(30d).height(40d).build();
        when(versionRepository.findById(5L)).thenReturn(Optional.of(version));
        when(versionRepository.findTopByPageIdAndImageUrlOrderByVersionNumberDesc(4L, "current.png"))
                .thenReturn(Optional.empty());
        when(hitboxRepository.findByPageIdAndPageVersionIsNull(4L)).thenReturn(List.of());
        when(hitboxRepository.findByPageVersionId(5L)).thenReturn(List.of(archived));
        when(pageRepository.saveAndFlush(page)).thenReturn(page);
        var principal = UserDetailsImpl.build(owner);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        var controller = new PageVersionController(versionRepository, pageRepository, hitboxRepository);
        assertEquals("version-1.png", controller.restoreVersion(5L, authentication).getBody().getImageUrl());
        verify(versionRepository, never()).save(any(PageVersion.class));
        verify(hitboxRepository).saveAll(argThat(items -> {
            Hitbox restored = items.iterator().next();
            return restored.getPage() == page
                    && restored.getPageVersion() == null
                    && restored.getXCoord().equals(10d)
                    && restored.getYCoord().equals(20d)
                    && restored.getWidth().equals(30d)
                    && restored.getHeight().equals(40d);
        }));
    }
}

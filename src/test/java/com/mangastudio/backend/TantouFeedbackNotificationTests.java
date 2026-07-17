package com.mangastudio.backend;

import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.TantouFeedback;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.TantouFeedbackRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.NotificationService;
import com.mangastudio.backend.service.impl.TantouFeedbackServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TantouFeedbackNotificationTests {

    @Test
    void savingFeedbackNotifiesOwningMangakaWithTantouFullName() {
        Fixture fixture = new Fixture();
        when(fixture.pages.findById(30L)).thenReturn(Optional.of(fixture.page));
        when(fixture.users.findById(2L)).thenReturn(Optional.of(fixture.tantou));
        when(fixture.feedbacks.save(any(TantouFeedback.class))).thenAnswer(invocation -> {
            TantouFeedback saved = invocation.getArgument(0);
            saved.setId(40L);
            return saved;
        });

        fixture.service.createFeedback(30L, 2L, 10D, 20D, 30D, 40D, "Adjust this panel");

        verify(fixture.notifications).createNotification(
                1L,
                "Tantou \"Taro Editor\" has sent you a feedback. Go check it out!",
                "/assistant-review?tab=tantou&seriesId=10&chapterId=20&pageId=30&feedbackId=40");
    }

    @Test
    void mangakaResolvingFeedbackNotifiesItsTantouAuthorWithExactCanvasLink() {
        Fixture fixture = new Fixture();
        TantouFeedback feedback = fixture.feedback(false);
        when(fixture.feedbacks.findById(40L)).thenReturn(Optional.of(feedback));
        when(fixture.users.findById(1L)).thenReturn(Optional.of(fixture.mangaka));
        when(fixture.feedbacks.save(feedback)).thenReturn(feedback);

        TantouFeedback resolved = fixture.service.resolveFeedback(40L, 1L);

        assertTrue(resolved.getIsResolved());
        verify(fixture.notifications).createNotification(
                2L,
                "\"Doraemon\" Mangaka has reviewed your feedback!",
                "/canvas-workspace?seriesId=10&chapterId=20&pageId=30&feedbackId=40");
    }

    @Test
    void resolvingAnAlreadyResolvedFeedbackDoesNotSendADuplicateNotification() {
        Fixture fixture = new Fixture();
        TantouFeedback feedback = fixture.feedback(true);
        when(fixture.feedbacks.findById(40L)).thenReturn(Optional.of(feedback));
        when(fixture.users.findById(1L)).thenReturn(Optional.of(fixture.mangaka));

        fixture.service.resolveFeedback(40L, 1L);

        verify(fixture.feedbacks, never()).save(any(TantouFeedback.class));
        verify(fixture.notifications, never()).createNotification(anyLong(), anyString(), anyString());
    }

    private static class Fixture {
        private final TantouFeedbackRepository feedbacks = mock(TantouFeedbackRepository.class);
        private final PageRepository pages = mock(PageRepository.class);
        private final UserRepository users = mock(UserRepository.class);
        private final NotificationService notifications = mock(NotificationService.class);
        private final User mangaka = User.builder()
                .id(1L)
                .role(Role.builder().roleName("Mangaka").build())
                .fullName("Mika Mangaka")
                .build();
        private final User tantou = User.builder()
                .id(2L)
                .role(Role.builder().roleName("Tantou Editor").build())
                .fullName("Taro Editor")
                .build();
        private final MangaSeries series = MangaSeries.builder()
                .id(10L)
                .title("Doraemon")
                .mangaka(mangaka)
                .tantou(tantou)
                .build();
        private final Chapter chapter = Chapter.builder().id(20L).mangaSeries(series).build();
        private final Page page = Page.builder().id(30L).chapter(chapter).build();
        private final TantouFeedbackServiceImpl service = new TantouFeedbackServiceImpl(
                feedbacks, pages, users, notifications);

        private TantouFeedback feedback(boolean resolved) {
            return TantouFeedback.builder()
                    .id(40L)
                    .page(page)
                    .editor(tantou)
                    .xCoord(10D)
                    .yCoord(20D)
                    .width(30D)
                    .height(40D)
                    .content("Adjust this panel")
                    .isResolved(resolved)
                    .build();
        }
    }
}

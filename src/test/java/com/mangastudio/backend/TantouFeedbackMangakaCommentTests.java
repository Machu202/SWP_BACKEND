package com.mangastudio.backend;

import com.mangastudio.backend.entity.*;
import com.mangastudio.backend.repository.*;
import com.mangastudio.backend.service.impl.TantouFeedbackServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TantouFeedbackMangakaCommentTests {
    @Test
    void owningMangakaCanCommentUsingDedicatedFeedbackEndpoint() {
        TantouFeedbackRepository feedbacks = mock(TantouFeedbackRepository.class); PageRepository pages = mock(PageRepository.class); UserRepository users = mock(UserRepository.class);
        TantouFeedbackServiceImpl service = new TantouFeedbackServiceImpl(feedbacks, pages, users);
        User mangaka = User.builder().id(1L).role(Role.builder().roleName("Mangaka").build()).build();
        MangaSeries series = MangaSeries.builder().mangaka(mangaka).build(); Chapter chapter = Chapter.builder().mangaSeries(series).build();
        Page page = Page.builder().id(5L).chapter(chapter).build();
        TantouFeedback parent = TantouFeedback.builder().id(9L).page(page).xCoord(10D).yCoord(20D).width(30D).height(40D).content("Fix this").build();
        when(feedbacks.findById(9L)).thenReturn(Optional.of(parent)); when(users.findById(1L)).thenReturn(Optional.of(mangaka));
        when(feedbacks.save(any())).thenAnswer(i -> i.getArgument(0));
        TantouFeedback comment = service.addMangakaComment(9L, 1L, "Done");
        assertTrue(comment.getContent().startsWith("[Mangaka Comment on Feedback #9]")); assertEquals(30D, comment.getWidth());
    }
}

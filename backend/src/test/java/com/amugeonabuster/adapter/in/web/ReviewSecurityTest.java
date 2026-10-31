package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.application.port.in.WelstoryMenuReviewUseCase;
import com.amugeonabuster.application.port.in.WelstoryMenuReviewUseCase.SubmitReviewCommand;
import com.amugeonabuster.domain.model.WelstoryMenuReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewSecurityTest {

    private final WelstoryMenuReviewUseCase useCase = mock(WelstoryMenuReviewUseCase.class);
    private final WelstoryMenuReviewController controller = new WelstoryMenuReviewController(useCase);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void reviewListAndSubmitResponsesNeverExposeUserFingerprint() throws Exception {
        WelstoryMenuReview domainReview = WelstoryMenuReview.builder()
                .id(10L)
                .cafeteriaName("수원 식당")
                .menuDate(LocalDate.of(2026, 10, 31))
                .courseName("A코스")
                .nickname("익명")
                .rating(5)
                .comment("맛있어요")
                .userFingerprint("secret-client-fingerprint")
                .createdAt(LocalDateTime.of(2026, 10, 31, 12, 0))
                .build();

        when(useCase.getReviews("수원 식당", LocalDate.of(2026, 10, 31))).thenReturn(List.of(domainReview));
        when(useCase.submitReview(any())).thenReturn(domainReview);

        String listJson = objectMapper.writeValueAsString(
                controller.getReviews("수원 식당", "2026-10-31").getBody());
        String submitJson = objectMapper.writeValueAsString(
                controller.submitReview(new WelstoryMenuReviewController.SubmitReviewRequest(
                        "수원 식당", "2026-10-31", "A코스", "제육볶음", "익명", 5, "맛있어요", "secret-client-fingerprint"
                ), new MockHttpSession()).getBody());

        assertThat(listJson).doesNotContain("userFingerprint", "secret-client-fingerprint");
        assertThat(submitJson).doesNotContain("userFingerprint", "secret-client-fingerprint");
    }

    @Test
    void bindsFingerprintToHttpSessionToPreventClientSideRotation() {
        MockHttpSession session = new MockHttpSession();
        when(useCase.submitReview(any())).thenAnswer(invocation -> {
            SubmitReviewCommand cmd = invocation.getArgument(0);
            return WelstoryMenuReview.builder()
                    .id(1L)
                    .cafeteriaName(cmd.getCafeteriaName())
                    .menuDate(cmd.getMenuDate())
                    .courseName(cmd.getCourseName())
                    .nickname("익명")
                    .rating(cmd.getRating())
                    .comment(cmd.getComment())
                    .userFingerprint(cmd.getUserFingerprint())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        controller.submitReview(new WelstoryMenuReviewController.SubmitReviewRequest(
                "수원 식당", "2026-10-31", "A코스", "제육", "익명", 4, "괜찮음", "first-fp"
        ), session);
        controller.submitReview(new WelstoryMenuReviewController.SubmitReviewRequest(
                "수원 식당", "2026-10-31", "A코스", "제육", "익명", 1, "중복시도", "rotated-second-fp"
        ), session);

        ArgumentCaptor<SubmitReviewCommand> captor = ArgumentCaptor.forClass(SubmitReviewCommand.class);
        verify(useCase, times(2)).submitReview(captor.capture());
        List<SubmitReviewCommand> commands = captor.getAllValues();
        assertThat(commands.get(0).getUserFingerprint()).isEqualTo("first-fp");
        assertThat(commands.get(1).getUserFingerprint()).isEqualTo("first-fp");
    }

    @Test
    void rejectsMalformedMenuDateWithIllegalArgumentException() {
        assertThatThrownBy(() -> controller.getReviews("수원 식당", "not-a-date"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(useCase);
    }
}

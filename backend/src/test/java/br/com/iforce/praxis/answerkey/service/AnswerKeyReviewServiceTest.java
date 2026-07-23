package br.com.iforce.praxis.answerkey.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerKeyReviewServiceTest {

    @Test
    void shouldCalculateFullConsensusWhenThereIsNoDispersion() {
        assertThat(AnswerKeyReviewService.consensusFromDispersion(BigDecimal.ZERO))
                .isEqualByComparingTo("1.0000");
    }

    @Test
    void shouldReduceConsensusAccordingToScoreDispersion() {
        assertThat(AnswerKeyReviewService.consensusFromDispersion(new BigDecimal("25")))
                .isEqualByComparingTo("0.7500");
    }

    @Test
    void shouldNeverReturnNegativeConsensus() {
        assertThat(AnswerKeyReviewService.consensusFromDispersion(new BigDecimal("140")))
                .isEqualByComparingTo("0.0000");
    }
}

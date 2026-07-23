package br.com.iforce.praxis.integrity.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrityReviewServiceSpringWiringTest {

    @Test
    void declaresProductionConstructorForSpringInjection() {
        assertThat(IntegrityReviewService.class.getConstructors()).hasSize(1);
        assertThat(IntegrityReviewService.class.getConstructors()[0]
                .isAnnotationPresent(Autowired.class)).isTrue();
    }
}

package br.com.iforce.praxis.integrity.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrityEvidenceRetentionServiceSpringWiringTest {

    @Test
    void declaresProductionConstructorForSpringInjection() {
        assertThat(IntegrityEvidenceRetentionService.class.getConstructors()).hasSize(1);
        assertThat(IntegrityEvidenceRetentionService.class.getConstructors()[0]
                .isAnnotationPresent(Autowired.class)).isTrue();
    }
}

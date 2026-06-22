package br.com.iforce.praxis.term.service;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.term.dto.AcceptTermRequest;
import br.com.iforce.praxis.term.dto.TermAcceptanceStatusResponse;
import br.com.iforce.praxis.term.model.HealthUseTerm;
import br.com.iforce.praxis.term.model.ResponsibilityTerm;
import br.com.iforce.praxis.term.persistence.entity.TermAcceptanceEntity;
import br.com.iforce.praxis.term.persistence.repository.TermAcceptanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TermAcceptanceServiceTest {

    @Mock
    private TermAcceptanceRepository termAcceptanceRepository;

    @Mock
    private CurrentTenantService currentTenantService;

    @Mock
    private CurrentUserService currentUserService;

    private TermAcceptanceService service;

    @BeforeEach
    void setUp() {
        service = new TermAcceptanceService(termAcceptanceRepository, currentTenantService, currentUserService);
        lenient().when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        lenient().when(currentUserService.requiredUserId()).thenReturn("42");
    }

    @Test
    void exposesCurrentTermVersionAndText() {
        assertThat(service.responsibilityTerm().version()).isEqualTo(ResponsibilityTerm.VERSION);
        assertThat(service.responsibilityTerm().text()).isNotBlank();
    }

    @Test
    void statusIsNotAcceptedWhenNoRecord() {
        when(termAcceptanceRepository.findFirstByTenantIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
                "tenant-1", "42", ResponsibilityTerm.TYPE)).thenReturn(Optional.empty());

        TermAcceptanceStatusResponse status = service.responsibilityStatus();

        assertThat(status.accepted()).isFalse();
        assertThat(status.acceptedVersion()).isNull();
        assertThat(status.currentVersion()).isEqualTo(ResponsibilityTerm.VERSION);
    }

    @Test
    void statusRequiresReacceptWhenOnlyOldVersionAccepted() {
        TermAcceptanceEntity old = acceptanceWithVersion("2020-01-01");
        when(termAcceptanceRepository.findFirstByTenantIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
                "tenant-1", "42", ResponsibilityTerm.TYPE)).thenReturn(Optional.of(old));

        TermAcceptanceStatusResponse status = service.responsibilityStatus();

        assertThat(status.accepted()).isFalse();
        assertThat(status.acceptedVersion()).isEqualTo("2020-01-01");
    }

    @Test
    void acceptingCurrentVersionPersistsRecordAndReturnsAccepted() {
        TermAcceptanceStatusResponse status = service.acceptResponsibility(new AcceptTermRequest(ResponsibilityTerm.VERSION));

        ArgumentCaptor<TermAcceptanceEntity> captor = ArgumentCaptor.forClass(TermAcceptanceEntity.class);
        verify(termAcceptanceRepository).save(captor.capture());
        TermAcceptanceEntity saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getUserId()).isEqualTo("42");
        assertThat(saved.getTermType()).isEqualTo(ResponsibilityTerm.TYPE);
        assertThat(saved.getTermVersion()).isEqualTo(ResponsibilityTerm.VERSION);
        assertThat(saved.getAcceptedAt()).isNotNull();

        assertThat(status.accepted()).isTrue();
    }

    @Test
    void rejectsAcceptanceOfStaleVersion() {
        assertThatThrownBy(() -> service.acceptResponsibility(new AcceptTermRequest("1999-01-01")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");

        verify(termAcceptanceRepository, never()).save(any());
    }

    @Test
    void exposesCurrentHealthUseTerm() {
        assertThat(service.healthUseTerm().type()).isEqualTo(HealthUseTerm.TYPE);
        assertThat(service.healthUseTerm().version()).isEqualTo(HealthUseTerm.VERSION);
        assertThat(service.healthUseTerm().text()).isNotBlank();
    }

    @Test
    void healthUseNotAcceptedByCurrentUserWhenNoRecord() {
        when(termAcceptanceRepository.findFirstByTenantIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
                "tenant-1", "42", HealthUseTerm.TYPE)).thenReturn(Optional.empty());

        assertThat(service.isHealthUseAcceptedByCurrentUser()).isFalse();
    }

    @Test
    void healthUseAcceptedByCurrentUserWhenCurrentVersionRecorded() {
        TermAcceptanceEntity current = new TermAcceptanceEntity();
        current.setTenantId("tenant-1");
        current.setUserId("42");
        current.setTermType(HealthUseTerm.TYPE);
        current.setTermVersion(HealthUseTerm.VERSION);
        current.setAcceptedAt(Instant.now());
        when(termAcceptanceRepository.findFirstByTenantIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
                "tenant-1", "42", HealthUseTerm.TYPE)).thenReturn(Optional.of(current));

        assertThat(service.isHealthUseAcceptedByCurrentUser()).isTrue();
    }

    @Test
    void acceptingCurrentHealthUseVersionPersistsRecord() {
        TermAcceptanceStatusResponse status = service.acceptHealthUse(new AcceptTermRequest(HealthUseTerm.VERSION));

        ArgumentCaptor<TermAcceptanceEntity> captor = ArgumentCaptor.forClass(TermAcceptanceEntity.class);
        verify(termAcceptanceRepository).save(captor.capture());
        assertThat(captor.getValue().getTermType()).isEqualTo(HealthUseTerm.TYPE);
        assertThat(status.accepted()).isTrue();
    }

    private TermAcceptanceEntity acceptanceWithVersion(String version) {
        TermAcceptanceEntity entity = new TermAcceptanceEntity();
        entity.setTenantId("tenant-1");
        entity.setUserId("42");
        entity.setTermType(ResponsibilityTerm.TYPE);
        entity.setTermVersion(version);
        entity.setAcceptedAt(Instant.now());
        return entity;
    }
}

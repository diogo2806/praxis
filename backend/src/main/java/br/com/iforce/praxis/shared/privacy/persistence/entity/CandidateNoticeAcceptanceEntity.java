package br.com.iforce.praxis.shared.privacy.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidate_notice_acceptances")
public class CandidateNoticeAcceptanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "attempt_id", nullable = false, length = 80)
    private String attemptId;

    @Column(name = "notice_version", nullable = false, length = 80)
    private String noticeVersion;

    @Column(name = "notice_language", nullable = false, length = 20)
    private String noticeLanguage;

    @Column(name = "notice_hash", nullable = false, length = 64)
    private String noticeHash;

    @Column(name = "acknowledged_at", nullable = false)
    private Instant acknowledgedAt;

    @Column(name = "terms_version", length = 80)
    private String termsVersion;

    @Column(name = "terms_hash", length = 64)
    private String termsHash;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

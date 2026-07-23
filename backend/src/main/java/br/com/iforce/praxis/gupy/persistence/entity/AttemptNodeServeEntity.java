package br.com.iforce.praxis.gupy.persistence.entity;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.FetchType;

import jakarta.persistence.GeneratedValue;

import jakarta.persistence.GenerationType;

import jakarta.persistence.Id;

import br.com.iforce.praxis.shared.model.MediaType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;

import jakarta.persistence.ManyToOne;

import jakarta.persistence.Table;

import jakarta.persistence.UniqueConstraint;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;


import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "attempt_node_serves",
        uniqueConstraints = @UniqueConstraint(name = "uk_attempt_node_serve", columnNames = {"candidate_attempt_id", "node_id"})
)
public class AttemptNodeServeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_attempt_id", nullable = false)
    private CandidateAttemptEntity candidateAttempt;

    @Column(name = "node_id", nullable = false, length = 120)
    private String nodeId;

    @Column(name = "served_at", nullable = false)
    private Instant servedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 16)
    private MediaType mediaType;

    @Column(name = "media_version", length = 120)
    private String mediaVersion;
}

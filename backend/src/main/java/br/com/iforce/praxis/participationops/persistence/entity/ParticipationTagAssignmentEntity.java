package br.com.iforce.praxis.participationops.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "participation_tag_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participation_tag_assignment",
                columnNames = {"empresa_id", "participation_type", "participation_id", "tag_id"}
        )
)
public class ParticipationTagAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "participation_type", nullable = false, length = 20)
    private String participationType;

    @Column(name = "participation_id", nullable = false, length = 120)
    private String participationId;

    @Column(name = "tag_id", nullable = false, length = 36)
    private String tagId;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

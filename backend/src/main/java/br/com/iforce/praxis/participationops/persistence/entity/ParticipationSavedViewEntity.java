package br.com.iforce.praxis.participationops.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "participation_saved_views",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participation_saved_view_owner_name",
                columnNames = {"empresa_id", "owner_user_id", "name"}
        )
)
public class ParticipationSavedViewEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "owner_user_id", nullable = false, length = 120)
    private String ownerUserId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "shared", nullable = false)
    private boolean shared;

    @Column(name = "filters_json", nullable = false, columnDefinition = "TEXT")
    private String filtersJson;

    @Column(name = "sort_json", columnDefinition = "TEXT")
    private String sortJson;

    @Column(name = "columns_json", columnDefinition = "TEXT")
    private String columnsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}

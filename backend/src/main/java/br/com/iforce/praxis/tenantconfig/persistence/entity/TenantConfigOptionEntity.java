package br.com.iforce.praxis.tenantconfig.persistence.entity;

import br.com.iforce.praxis.tenantconfig.model.TenantConfigType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "tenant_config_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tenant_config_option",
                columnNames = {"tenant_id", "config_type", "option_value"}
        )
)
public class TenantConfigOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_type", nullable = false, length = 40)
    private TenantConfigType configType;

    @Column(name = "option_value", nullable = false, length = 200)
    private String optionValue;

    @Column(name = "option_label", nullable = false, length = 200)
    private String optionLabel;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Column(name = "selected_by_default", nullable = false)
    private boolean selectedByDefault;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}

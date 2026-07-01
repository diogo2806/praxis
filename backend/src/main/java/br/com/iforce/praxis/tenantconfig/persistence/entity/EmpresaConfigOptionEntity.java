package br.com.iforce.praxis.tenantconfig.persistence.entity;

import br.com.iforce.praxis.tenantconfig.model.EmpresaConfigType;

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
        name = "empresa_config_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_empresa_config_option",
                columnNames = {"empresa_id", "config_type", "option_value"}
        )
)
public class EmpresaConfigOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_type", nullable = false, length = 40)
    private EmpresaConfigType configType;

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
}

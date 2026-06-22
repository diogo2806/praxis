package br.com.iforce.praxis.auth.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    @Column(name = "id", nullable = false, length = 120)
    private String id;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "company_id", nullable = false, length = 120)
    private String companyId;

    @Column(name = "integration_token_hash", length = 120)
    private String integrationTokenHash;

    /**
     * Habilita a vertical de saúde (uso educativo). Quando verdadeiro, a publicação exige aceite do
     * termo de uso em saúde pelo recrutador e o fluxo do candidato coleta o consentimento do
     * paciente para tratamento de dado sensível (LGPD, arts. 11 e 14).
     */
    @Column(name = "health_vertical", nullable = false)
    private boolean healthVertical;
}

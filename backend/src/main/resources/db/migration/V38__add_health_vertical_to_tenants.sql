-- Vertical de saúde por tenant (uso educativo, LGPD dado sensível):
-- habilita o termo de uso em saúde para o recrutador e o consentimento do paciente.
-- Default false mantém o comportamento atual de todos os tenants existentes.

ALTER TABLE tenants
    ADD COLUMN health_vertical BOOLEAN NOT NULL DEFAULT FALSE;

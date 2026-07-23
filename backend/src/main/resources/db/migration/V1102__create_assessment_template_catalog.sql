CREATE TABLE assessment_templates (
    id UUID PRIMARY KEY,
    owner_empresa_id VARCHAR(120) NOT NULL,
    source_empresa_id VARCHAR(120) NOT NULL,
    source_simulation_id VARCHAR(120) NOT NULL,
    source_version_number INTEGER NOT NULL,
    template_version INTEGER NOT NULL DEFAULT 1,
    scope VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(1200) NOT NULL,
    job_role VARCHAR(180) NOT NULL,
    business_area VARCHAR(180) NOT NULL,
    seniority VARCHAR(80) NOT NULL,
    sector VARCHAR(180) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    language_code VARCHAR(16) NOT NULL,
    complexity VARCHAR(40) NOT NULL,
    methodology_evidence TEXT NOT NULL,
    usage_limitations TEXT NOT NULL,
    author_user_id VARCHAR(180) NOT NULL,
    reviewed_by VARCHAR(180),
    reviewed_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    CONSTRAINT ck_assessment_template_scope CHECK (scope IN ('INTERNAL', 'SHARED', 'OFFICIAL')),
    CONSTRAINT ck_assessment_template_status CHECK (status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'ARCHIVED')),
    CONSTRAINT ck_assessment_template_duration CHECK (duration_minutes BETWEEN 1 AND 480),
    CONSTRAINT ck_assessment_template_source_version CHECK (source_version_number > 0),
    CONSTRAINT ck_assessment_template_version CHECK (template_version > 0),
    CONSTRAINT uk_assessment_template_version UNIQUE (owner_empresa_id, source_simulation_id, source_version_number, template_version)
);

CREATE TABLE assessment_template_competencies (
    id BIGSERIAL PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES assessment_templates(id) ON DELETE CASCADE,
    competency_name VARCHAR(140) NOT NULL,
    CONSTRAINT uk_assessment_template_competency UNIQUE (template_id, competency_name)
);

CREATE TABLE assessment_template_favorites (
    id BIGSERIAL PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES assessment_templates(id) ON DELETE CASCADE,
    user_id VARCHAR(180) NOT NULL,
    CONSTRAINT uk_assessment_template_favorite UNIQUE (template_id, user_id)
);

CREATE INDEX ix_assessment_template_visibility
    ON assessment_templates (status, scope, owner_empresa_id, published_at DESC);
CREATE INDEX ix_assessment_template_filters
    ON assessment_templates (job_role, business_area, seniority, sector, language_code, complexity);
CREATE INDEX ix_assessment_template_competency_name
    ON assessment_template_competencies (competency_name, template_id);
CREATE INDEX ix_assessment_template_favorite_user
    ON assessment_template_favorites (user_id, template_id);

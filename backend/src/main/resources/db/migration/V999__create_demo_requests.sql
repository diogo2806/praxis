CREATE TABLE demo_requests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL,
    company VARCHAR(160) NOT NULL,
    role VARCHAR(120),
    hiring_volume VARCHAR(80),
    message VARCHAR(1200),
    source VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_demo_requests_created_at ON demo_requests (created_at DESC);
CREATE INDEX idx_demo_requests_email ON demo_requests (email);

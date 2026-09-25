CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    registration VARCHAR(20) NOT NULL,
    manufacturer VARCHAR(64) NOT NULL,
    model VARCHAR(64) NOT NULL,
    production_year SMALLINT,
    vin VARCHAR(17),
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vehicles_registration UNIQUE (registration),
    CONSTRAINT uk_vehicles_vin UNIQUE (vin),
    CONSTRAINT ck_vehicles_year CHECK (production_year IS NULL OR production_year >= 1900)
);

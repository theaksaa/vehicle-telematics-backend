CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    last_telemetry_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(16) NOT NULL,
    start_latitude DOUBLE PRECISION,
    start_longitude DOUBLE PRECISION,
    end_latitude DOUBLE PRECISION,
    end_longitude DOUBLE PRECISION,
    telemetry_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT ck_trips_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_trips_time CHECK (ended_at IS NULL OR ended_at >= started_at),
    CONSTRAINT ck_trips_telemetry_count CHECK (telemetry_count >= 0)
);

CREATE INDEX ix_trips_vehicle_started_at ON trips (vehicle_id, started_at DESC);
CREATE INDEX ix_trips_vehicle_status ON trips (vehicle_id, status);

ALTER TABLE telemetry
    ADD CONSTRAINT fk_telemetry_trip FOREIGN KEY (trip_id) REFERENCES trips (id);

CREATE INDEX ix_telemetry_trip_recorded_at ON telemetry (trip_id, recorded_at ASC);

CREATE TABLE vehicle_state (
    vehicle_id BIGINT PRIMARY KEY,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    last_telemetry_at TIMESTAMP WITH TIME ZONE,
    last_location_at TIMESTAMP WITH TIME ZONE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    speed_kph REAL,
    rpm INTEGER,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_state_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE,
    CONSTRAINT ck_vehicle_state_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_vehicle_state_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_vehicle_state_speed CHECK (speed_kph IS NULL OR speed_kph BETWEEN 0 AND 500),
    CONSTRAINT ck_vehicle_state_rpm CHECK (rpm IS NULL OR rpm BETWEEN 0 AND 20000)
);

INSERT INTO vehicle_state (vehicle_id, online)
SELECT v.id, CASE WHEN d.status = 'ONLINE' THEN TRUE ELSE FALSE END
FROM vehicles v
LEFT JOIN devices d ON d.vehicle_id = v.id;

CREATE TABLE telemetry (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,
    vehicle_id BIGINT,
    trip_id BIGINT,
    protocol_version SMALLINT NOT NULL,
    boot_id BIGINT NOT NULL,
    sequence_number BIGINT NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    gnss_speed_kph REAL,
    vehicle_speed_kph REAL,
    rpm INTEGER,
    accelerator_pct REAL,
    rssi_dbm SMALLINT,
    CONSTRAINT uk_telemetry_device_boot_sequence UNIQUE (device_id, boot_id, sequence_number),
    CONSTRAINT fk_telemetry_device FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT fk_telemetry_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT ck_telemetry_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_telemetry_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_telemetry_gnss_speed CHECK (gnss_speed_kph IS NULL OR gnss_speed_kph BETWEEN 0 AND 500),
    CONSTRAINT ck_telemetry_vehicle_speed CHECK (vehicle_speed_kph IS NULL OR vehicle_speed_kph BETWEEN 0 AND 500),
    CONSTRAINT ck_telemetry_rpm CHECK (rpm IS NULL OR rpm BETWEEN 0 AND 20000),
    CONSTRAINT ck_telemetry_accelerator CHECK (accelerator_pct IS NULL OR accelerator_pct BETWEEN 0 AND 100),
    CONSTRAINT ck_telemetry_rssi CHECK (rssi_dbm IS NULL OR rssi_dbm BETWEEN -200 AND 50)
);

CREATE INDEX ix_telemetry_device_recorded_at ON telemetry (device_id, recorded_at DESC);
CREATE INDEX ix_telemetry_vehicle_recorded_at ON telemetry (vehicle_id, recorded_at DESC);

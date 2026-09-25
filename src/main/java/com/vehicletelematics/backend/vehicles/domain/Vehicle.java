package com.vehicletelematics.backend.vehicles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "vehicles")
@Getter
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String registration;

    @Column(nullable = false, length = 64)
    private String manufacturer;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(name = "production_year")
    private Short year;

    @Column(unique = true, length = 17)
    private String vin;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Vehicle() {
    }

    public Vehicle(
            String registration,
            String manufacturer,
            String model,
            Short year,
            String vin,
            String description) {
        this.registration = registration;
        this.manufacturer = manufacturer;
        this.model = model;
        this.year = year;
        this.vin = vin;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(
            String registration,
            String manufacturer,
            String model,
            Short year,
            String vin,
            String description,
            boolean active) {
        this.registration = registration;
        this.manufacturer = manufacturer;
        this.model = model;
        this.year = year;
        this.vin = vin;
        this.description = description;
        this.active = active;
    }

    public void deactivate() {
        active = false;
    }
}

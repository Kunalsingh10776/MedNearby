package com.mednearby;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// ============================================================
// This one file defines all 4 database tables + the 2 status
// enums. Each @Entity class below becomes a MySQL table
// automatically (Hibernate creates them for you on startup).
// ============================================================

@Entity
class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    String strength;
    String genericName;

    Medicine() {}
    Medicine(String name, String strength, String genericName) {
        this.name = name;
        this.strength = strength;
        this.genericName = genericName;
    }
}

@Entity
class Pharmacy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    String address;
    String phone;
    Double latitude;
    Double longitude;

    Pharmacy() {}
    Pharmacy(String name, String address, String phone, Double latitude, Double longitude) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"pharmacyId", "medicineId"}))
class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Long pharmacyId;
    Long medicineId;

    @Enumerated(EnumType.STRING)
    StockStatus status;

    LocalDateTime lastUpdated;

    Stock() {}
    Stock(Long pharmacyId, Long medicineId, StockStatus status, LocalDateTime lastUpdated) {
        this.pharmacyId = pharmacyId;
        this.medicineId = medicineId;
        this.status = status;
        this.lastUpdated = lastUpdated;
    }
}

@Entity
class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Long pharmacyId;
    Long medicineId;
    Integer quantity;

    @Enumerated(EnumType.STRING)
    ReservationStatus status;

    LocalDateTime createdAt;
    LocalDateTime expiresAt;
}

enum StockStatus {
    AVAILABLE, LOW_STOCK, OUT_OF_STOCK
}

enum ReservationStatus {
    PENDING, CONFIRMED, REJECTED, EXPIRED
}

package com.mednearby;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// ============================================================
// Spring Data JPA generates the actual SQL for these methods
// automatically based on their names. You never write SQL here.
// ============================================================

interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByNameContainingIgnoreCase(String name);
}

interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
}

interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByMedicineId(Long medicineId);
    Optional<Stock> findByPharmacyIdAndMedicineId(Long pharmacyId, Long medicineId);
}

interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByPharmacyId(Long pharmacyId);
}

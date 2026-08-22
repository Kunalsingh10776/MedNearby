package com.mednearby;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// ============================================================
// Every API endpoint lives here. In a bigger app you'd split
// this into Controller + Service layers, but for a hackathon
// MVP, one file you can read top-to-bottom is easier to follow.
//
// If a lookup fails (medicine/pharmacy/stock/reservation not
// found, or bad input), we throw ResponseStatusException, which
// Spring automatically turns into a clean JSON error response
// like: {"status":404,"error":"Not Found", ...} — no Java stack
// traces are ever shown to the caller.
// ============================================================

@RestController
public class MedNearbyController {

    private final MedicineRepository medicineRepo;
    private final PharmacyRepository pharmacyRepo;
    private final StockRepository stockRepo;
    private final ReservationRepository reservationRepo;

    private static final int RESERVATION_EXPIRY_MINUTES = 20;

    public MedNearbyController(MedicineRepository medicineRepo, PharmacyRepository pharmacyRepo,
                                StockRepository stockRepo, ReservationRepository reservationRepo) {
        this.medicineRepo = medicineRepo;
        this.pharmacyRepo = pharmacyRepo;
        this.stockRepo = stockRepo;
        this.reservationRepo = reservationRepo;
    }

    // ---------------- MEDICINE SEARCH ----------------

    @GetMapping("/api/medicines/search")
    public List<Medicine> searchMedicines(@RequestParam(required = false) String name) {
        if (name == null || name.trim().isEmpty()) {
            return List.of();
        }
        return medicineRepo.findByNameContainingIgnoreCase(name.trim());
    }

    @GetMapping("/api/medicines/{id}")
    public Medicine getMedicine(@PathVariable Long id) {
        return medicineRepo.findById(id)
                .orElseThrow(() -> notFound("Medicine not found"));
    }

    // ---------------- PHARMACIES ----------------

    @GetMapping("/api/pharmacies")
    public List<Pharmacy> getPharmacies() {
        return pharmacyRepo.findAll();
    }

    @GetMapping("/api/pharmacies/{id}")
    public Pharmacy getPharmacy(@PathVariable Long id) {
        return pharmacyRepo.findById(id)
                .orElseThrow(() -> notFound("Pharmacy not found"));
    }

    // ---------------- STOCK / AVAILABILITY ----------------

    // Returns pharmacies that have reported stock for a medicine, ranked
    // by availability + freshness + distance ("availability ranking algorithm").
    @GetMapping("/api/stock/medicine/{medicineId}")
    public List<Map<String, Object>> getAvailability(@PathVariable Long medicineId,
                                                       @RequestParam(required = false) Double latitude,
                                                       @RequestParam(required = false) Double longitude) {
        List<Stock> stocks = stockRepo.findByMedicineId(medicineId);

        List<Map<String, Object>> results = new ArrayList<>();
        for (Stock stock : stocks) {
            Pharmacy pharmacy = pharmacyRepo.findById(stock.pharmacyId).orElse(null);
            if (pharmacy == null) continue;

            Double distanceKm = null;
            if (latitude != null && longitude != null && pharmacy.latitude != null && pharmacy.longitude != null) {
                distanceKm = haversineKm(latitude, longitude, pharmacy.latitude, pharmacy.longitude);
            }

            int score = availabilityScore(stock.status) + freshnessScore(stock.lastUpdated) + distanceScore(distanceKm);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("stockId", stock.id);
            entry.put("pharmacyId", pharmacy.id);
            entry.put("pharmacyName", pharmacy.name);
            entry.put("address", pharmacy.address);
            entry.put("distanceKm", distanceKm);
            entry.put("status", stock.status);
            entry.put("lastUpdated", stock.lastUpdated);
            entry.put("freshnessMessage", freshnessMessage(stock.lastUpdated));
            entry.put("score", score);
            results.add(entry);
        }

        results.sort((a, b) -> (int) b.get("score") - (int) a.get("score"));
        return results;
    }

    // Pharmacy updates their own stock status for a medicine.
    @PutMapping("/api/stock/{stockId}")
    public Map<String, Object> updateStock(@PathVariable Long stockId, @RequestBody Map<String, String> body) {
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> notFound("Stock record not found"));

        String statusText = body.get("status");
        if (statusText == null) {
            throw badRequest("status is required");
        }

        StockStatus newStatus;
        try {
            newStatus = StockStatus.valueOf(statusText);
        } catch (IllegalArgumentException e) {
            throw badRequest("Invalid status value");
        }

        stock.status = newStatus;
        stock.lastUpdated = LocalDateTime.now();
        stockRepo.save(stock);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Stock updated successfully");
        response.put("status", stock.status);
        response.put("lastUpdated", stock.lastUpdated);
        return response;
    }

    // ---------------- RESERVATIONS ----------------

    @PostMapping("/api/reservations")
    public Reservation createReservation(@RequestBody Map<String, Object> body) {
        Long pharmacyId = toLong(body.get("pharmacyId"));
        Long medicineId = toLong(body.get("medicineId"));
        Integer quantity = toInt(body.get("quantity"));

        if (!pharmacyRepo.existsById(pharmacyId)) throw notFound("Pharmacy not found");
        if (!medicineRepo.existsById(medicineId)) throw notFound("Medicine not found");
        if (quantity == null || quantity <= 0) throw badRequest("Quantity must be greater than zero");

        Stock stock = stockRepo.findByPharmacyIdAndMedicineId(pharmacyId, medicineId)
                .orElseThrow(() -> badRequest("This pharmacy has not reported stock for this medicine"));

        if (stock.status == StockStatus.OUT_OF_STOCK) {
            throw badRequest("Medicine is currently reported as out of stock.");
        }

        Reservation reservation = new Reservation();
        reservation.pharmacyId = pharmacyId;
        reservation.medicineId = medicineId;
        reservation.quantity = quantity;
        reservation.status = ReservationStatus.PENDING;
        reservation.createdAt = LocalDateTime.now();
        reservation.expiresAt = reservation.createdAt.plusMinutes(RESERVATION_EXPIRY_MINUTES);

        return reservationRepo.save(reservation);
    }

    @GetMapping("/api/reservations/{id}")
    public Reservation getReservation(@PathVariable Long id) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> notFound("Reservation not found"));
        return expireIfNeeded(reservation);
    }

    @GetMapping("/api/reservations/pharmacy/{pharmacyId}")
    public List<Reservation> getReservationsForPharmacy(@PathVariable Long pharmacyId) {
        return reservationRepo.findByPharmacyId(pharmacyId).stream()
                .map(this::expireIfNeeded)
                .collect(Collectors.toList());
    }

    @PutMapping("/api/reservations/{id}/accept")
    public Reservation acceptReservation(@PathVariable Long id) {
        Reservation reservation = expireIfNeeded(
                reservationRepo.findById(id).orElseThrow(() -> notFound("Reservation not found")));

        if (reservation.status != ReservationStatus.PENDING) {
            throw badRequest("Reservation cannot be accepted because it is " + reservation.status);
        }

        Stock stock = stockRepo.findByPharmacyIdAndMedicineId(reservation.pharmacyId, reservation.medicineId).orElse(null);
        if (stock == null || stock.status == StockStatus.OUT_OF_STOCK) {
            throw badRequest("Medicine is no longer available at this pharmacy");
        }

        reservation.status = ReservationStatus.CONFIRMED;
        return reservationRepo.save(reservation);
    }

    @PutMapping("/api/reservations/{id}/reject")
    public Reservation rejectReservation(@PathVariable Long id) {
        Reservation reservation = expireIfNeeded(
                reservationRepo.findById(id).orElseThrow(() -> notFound("Reservation not found")));

        if (reservation.status != ReservationStatus.PENDING) {
            throw badRequest("Reservation cannot be rejected because it is " + reservation.status);
        }

        reservation.status = ReservationStatus.REJECTED;
        return reservationRepo.save(reservation);
    }

    // If a reservation's time has passed and it's still PENDING, flip it to EXPIRED.
    // Called every time a reservation is read - no background job needed.
    private Reservation expireIfNeeded(Reservation reservation) {
        if (reservation.status == ReservationStatus.PENDING && LocalDateTime.now().isAfter(reservation.expiresAt)) {
            reservation.status = ReservationStatus.EXPIRED;
            reservationRepo.save(reservation);
        }
        return reservation;
    }

    // ---------------- SCORING / DISTANCE / FRESHNESS HELPERS ----------------

    private int availabilityScore(StockStatus status) {
        return switch (status) {
            case AVAILABLE -> 50;
            case LOW_STOCK -> 30;
            case OUT_OF_STOCK -> 0;
        };
    }

    private int freshnessScore(LocalDateTime lastUpdated) {
        long minutes = Duration.between(lastUpdated, LocalDateTime.now()).toMinutes();
        if (minutes <= 15) return 30;
        if (minutes <= 60) return 20;
        if (minutes <= 180) return 10;
        return 0;
    }

    private int distanceScore(Double distanceKm) {
        if (distanceKm == null) return 0;
        if (distanceKm < 1) return 20;
        if (distanceKm <= 3) return 10;
        return 5;
    }

    private String freshnessMessage(LocalDateTime lastUpdated) {
        long minutes = Duration.between(lastUpdated, LocalDateTime.now()).toMinutes();
        if (minutes <= 15) return "Recently verified";
        if (minutes <= 60) return "Updated recently";
        return "Availability may be outdated";
    }

    // Great-circle distance between two lat/lng points, in kilometers.
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    // ---------------- SMALL UTILITIES ----------------

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        return Long.valueOf(value.toString());
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        return Integer.valueOf(value.toString());
    }
}

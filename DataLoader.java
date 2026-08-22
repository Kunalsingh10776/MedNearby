package com.mednearby;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

// ============================================================
// Runs once automatically when the app starts. Fills the
// database with demo medicines/pharmacies/stock so you have
// something to search and reserve immediately - no manual
// data entry needed for the demo.
// ============================================================

@Component
class DataLoader implements CommandLineRunner {

    private final MedicineRepository medicineRepo;
    private final PharmacyRepository pharmacyRepo;
    private final StockRepository stockRepo;

    DataLoader(MedicineRepository medicineRepo, PharmacyRepository pharmacyRepo, StockRepository stockRepo) {
        this.medicineRepo = medicineRepo;
        this.pharmacyRepo = pharmacyRepo;
        this.stockRepo = stockRepo;
    }

    @Override
    public void run(String... args) {
        if (medicineRepo.count() > 0) return; // already seeded, don't duplicate

        Medicine paracetamol = medicineRepo.save(new Medicine("Paracetamol", "500mg", "Paracetamol"));
        Medicine azithromycin = medicineRepo.save(new Medicine("Azithromycin", "500mg", "Azithromycin"));
        Medicine cetirizine = medicineRepo.save(new Medicine("Cetirizine", "10mg", "Cetirizine"));
        Medicine ors = medicineRepo.save(new Medicine("ORS", "-", "Oral Rehydration Salts"));
        Medicine amoxicillin = medicineRepo.save(new Medicine("Amoxicillin", "500mg", "Amoxicillin"));

        Pharmacy sharma = pharmacyRepo.save(new Pharmacy("Sharma Medical", "12 MG Road", "9990000001", 23.2599, 77.4126));
        Pharmacy city = pharmacyRepo.save(new Pharmacy("City Pharmacy", "45 Station Road", "9990000002", 23.2650, 77.4200));
        Pharmacy krishna = pharmacyRepo.save(new Pharmacy("Krishna Medical", "8 Market Street", "9990000003", 23.2700, 77.4050));
        Pharmacy healthPlus = pharmacyRepo.save(new Pharmacy("HealthPlus Pharmacy", "22 Civil Lines", "9990000004", 23.2550, 77.4300));
        Pharmacy apollo = pharmacyRepo.save(new Pharmacy("Apollo Pharmacy", "5 Ring Road", "9990000005", 23.2620, 77.4180));

        LocalDateTime now = LocalDateTime.now();

        stockRepo.save(new Stock(sharma.id, paracetamol.id, StockStatus.AVAILABLE, now.minusMinutes(3)));
        stockRepo.save(new Stock(sharma.id, ors.id, StockStatus.AVAILABLE, now.minusMinutes(10)));

        stockRepo.save(new Stock(city.id, paracetamol.id, StockStatus.AVAILABLE, now.minusMinutes(8)));
        stockRepo.save(new Stock(city.id, cetirizine.id, StockStatus.AVAILABLE, now.minusMinutes(20)));

        stockRepo.save(new Stock(krishna.id, paracetamol.id, StockStatus.LOW_STOCK, now.minusMinutes(5)));

        stockRepo.save(new Stock(healthPlus.id, paracetamol.id, StockStatus.OUT_OF_STOCK, now.minusMinutes(30)));
        stockRepo.save(new Stock(healthPlus.id, ors.id, StockStatus.AVAILABLE, now.minusMinutes(12)));

        stockRepo.save(new Stock(apollo.id, paracetamol.id, StockStatus.AVAILABLE, now.minusMinutes(2)));
        stockRepo.save(new Stock(apollo.id, azithromycin.id, StockStatus.AVAILABLE, now.minusMinutes(45)));

        stockRepo.save(new Stock(city.id, amoxicillin.id, StockStatus.LOW_STOCK, now.minusMinutes(90)));

        System.out.println("MedNearby: demo data loaded.");
    }
}

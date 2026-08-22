# MedNearby

**Right Medicine. Right Place. Right Time.**

MedNearby is a lightweight, community-driven web platform that helps patients find nearby pharmacies currently reporting a required medicine in stock — and reserve it before travelling. Small pharmacies often lack inventory-management software, so MedNearby gives them a one-tap way to signal availability, while giving patients a way to stop guessing which pharmacy to visit.

---

## Problem

Patients often visit several pharmacies searching for a medicine because they have no way to know in advance which nearby pharmacy currently has it in stock. Many small pharmacies don't have the tools to broadcast their inventory digitally.

## Solution

- Patients search a medicine and see nearby pharmacies ranked by **availability**, **freshness of the reported stock**, and **distance**.
- Patients can **reserve** an available medicine for a 20-minute pickup window.
- Pharmacies **accept or reject** incoming reservations.
- Pharmacies update their own stock status in one tap through a simple page, reachable instantly via a **QR code** — no inventory software required.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot (Spring Web, Spring Data JPA) |
| ORM | Hibernate |
| Database | MySQL |
| Frontend | HTML5, CSS3, Vanilla JavaScript |
| Build tool | Maven |
| QR code generation | ZXing |
| API testing | Postman |

---

## Core Features

- Case-insensitive, whitespace-tolerant medicine search
- Ranked search results using a transparent **availability ranking algorithm** (availability + freshness + distance)
- Reservation system with automatic 20-minute expiry
- Pharmacy dashboard to accept/reject reservations, with stock re-validation before confirming
- QR-code-based stock update page for pharmacies
- Clean JSON error responses — no stack traces ever exposed to the client
- Graceful handling of denied location access and database failures

---

## Project Structure

```
mednearby/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/mednearby/
│   │   │   ├── MedNearbyApplication.java   # entry point
│   │   │   ├── Models.java                 # entities: Medicine, Pharmacy, Stock, Reservation
│   │   │   ├── Repositories.java           # Spring Data JPA repositories
│   │   │   ├── MedNearbyController.java    # all REST endpoints + business logic
│   │   │   └── DataLoader.java             # seeds demo data on startup
│   │   └── resources/
│   │       └── application.properties      # DB config (reads from env vars)
│   └── test/java/com/mednearby/
└── README.md
```

---

## Getting Started

### Prerequisites

- **Java 17+** (JDK) — check with `java -version`
- **Maven** — check with `mvn -version`
- **MySQL** running locally or accessible remotely
- An IDE such as IntelliJ IDEA or VS Code (with the Java extension)

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/mednearby.git
cd mednearby
```

### 2. Configure the database

Set these environment variables (or edit `application.properties` directly for local testing):

```bash
export DB_URL=jdbc:mysql://localhost:3306/mednearby?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
```

The `mednearby` database is created automatically on first run — no manual schema setup needed.

### 3. Run the app

```bash
mvn spring-boot:run
```

Or run `MedNearbyApplication.java` directly from your IDE.

On success, you'll see Spring Boot's startup banner followed by:
```
MedNearby: demo data loaded.
```

### 4. Test it

```
GET http://localhost:8080/api/medicines/search?name=paracetamol
```

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/medicines/search?name={name}` | Search medicines by name |
| GET | `/api/medicines/{id}` | Get a medicine by ID |
| GET | `/api/pharmacies` | List all pharmacies |
| GET | `/api/pharmacies/{id}` | Get a pharmacy by ID |
| GET | `/api/stock/medicine/{medicineId}` | Ranked pharmacy availability for a medicine |
| PUT | `/api/stock/{stockId}` | Update stock status |
| POST | `/api/reservations` | Create a reservation |
| GET | `/api/reservations/{id}` | Get a reservation |
| GET | `/api/reservations/pharmacy/{pharmacyId}` | List reservations for a pharmacy |
| PUT | `/api/reservations/{id}/accept` | Accept a reservation |
| PUT | `/api/reservations/{id}/reject` | Reject a reservation |

**Example: check availability**
```
GET /api/stock/medicine/1?latitude=23.26&longitude=77.41
```

**Example: create a reservation**
```json
POST /api/reservations
{
  "pharmacyId": 1,
  "medicineId": 1,
  "quantity": 2
}
```

---

## Demo Data

The app seeds itself with demo medicines (Paracetamol, Azithromycin, Cetirizine, ORS, Amoxicillin) and demo pharmacies (Sharma Medical, City Pharmacy, Krishna Medical, HealthPlus Pharmacy, Apollo Pharmacy) on first run. These are for demonstration purposes only and are not real participating pharmacies.

---

## Limitations & Future Scope

- No authentication layer yet
- Pharmacy coordinates are seeded manually rather than geocoded
- Reservation expiry is checked lazily on access rather than via a background job
- Future scope: patient/pharmacy authentication, real-time notifications, live inventory integration, mobile app

---

## Medical Disclaimer

MedNearby is only a tool for locating pharmacy-reported medicine availability. It does not diagnose, prescribe, or recommend dosages or substitute medications. Always follow applicable laws and consult a pharmacist or doctor.

---

## Team

**Pixel Pirates**

- **Team Leader:** Kunal Kumar Singh
- **Team Members:** Prince Raj, Sameer Belwal, Karan Sharma

---

## License

This project was built as a hackathon MVP and is provided as-is for demonstration purposes.

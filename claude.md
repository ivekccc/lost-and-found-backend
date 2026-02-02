# Lost and Found Backend

## Pregled projekta

Spring Boot 4.0.1 REST API aplikacija za Lost and Found platformu. Koristi Java 21, PostgreSQL bazu i JWT autentifikaciju.

## Tehnologije

- **Framework**: Spring Boot 4.0.1
- **Java verzija**: 21
- **Baza**: PostgreSQL
- **Migracije**: Flyway
- **Autentifikacija**: JWT
- **Dokumentacija**: Swagger/OpenAPI (springdoc)
- **Build tool**: Maven

## Struktura projekta

```
src/main/java/com/example/demo/
├── config/
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   └── FlywayConfig.java
├── controller/
│   ├── AuthController.java
│   ├── HelloController.java
│   └── ReportController.java
├── dto/
│   ├── AuthRequestDTO.java
│   ├── AuthResponseDTO.java
│   ├── RegisterRequestDTO.java
│   ├── RefreshTokenRequestDTO.java
│   ├── RefreshTokenResponseDTO.java
│   ├── ErrorResponseDTO.java
│   ├── ReportListDTO.java
│   └── ReportDetailsDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── UserAlreadyExistsException.java
├── model/
│   ├── User.java
│   ├── UserStatus.java
│   ├── Report.java
│   ├── ReportType.java
│   ├── ReportStatus.java
│   └── ReportCategory.java
├── repository/
│   ├── UserRepository.java
│   ├── ReportRepository.java
│   └── ReportCategoryRepository.java
├── service/
│   ├── AuthService.java
│   ├── ReportService.java
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   └── MyUserDetailsService.java
└── LostAndFountBackendApplication.java

src/main/resources/
├── application.properties
└── db/
    ├── migration/
    │   ├── V1__create_users_table.sql
    │   ├── V2__add_username_to_users.sql
    │   └── V3__create_report_tables.sql
    └── seed/
        ├── R__seed_users.sql
        ├── R__seed_report_categories.sql
        └── R__seed_reports.sql
```

## Flyway

### Struktura foldera

| Folder | Prefix | Namena | Izvršava se |
|--------|--------|--------|-------------|
| `db/migration/` | `V__` | Šema (CREATE, ALTER) | Jednom |
| `db/seed/` | `R__` | Seed podaci | Kad se fajl promeni |

### Dodavanje migracije

1. Kreiraj `V{broj}__{opis}.sql` u `db/migration/`
2. Restartuj aplikaciju

### Dodavanje seed podataka

1. Kreiraj ili edituj `R__{opis}.sql` u `db/seed/`
2. Koristi DELETE + INSERT pattern
3. Restartuj aplikaciju

### Primer seed fajla

```sql
DELETE FROM users WHERE email IN ('user1@example.com', 'user2@example.com');

INSERT INTO users (email, username, password, status, created_at) VALUES
('user1@example.com', 'user1', '$2a$12$...hash...', 'ACTIVE', NOW()),
('user2@example.com', 'user2', '$2a$12$...hash...', 'ACTIVE', NOW());
```

### BCrypt hash

Za generisanje hasha: https://bcrypt-generator.com/

### Reset migracije

```sql
DELETE FROM flyway_schema_history WHERE version = 'X';
```

## Environment varijable

```properties
JWT_SECRET=your-secret-key
DB_USERNAME=postgres
DB_PASSWORD=admin
```

## Baza podataka

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/lost-and-found
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=false
```

## API Dokumentacija

- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8082/v3/api-docs

### Report Endpoints

| Method | Endpoint | Opis | Response |
|--------|----------|------|----------|
| GET | `/reports/{id}` | Detalji reporta | `ReportDetailsDTO` |

## Enum definicije za NSwag

Da bi NSwag generisao zajedničke enum tipove (umjesto inline duplicate), enum klase moraju imati `@Schema(enumAsRef = true)`:

```java
@Schema(name = "ReportType", enumAsRef = true)
public enum ReportType {
    LOST,
    FOUND
}
```

Ovo osigurava da NSwag generiše:
```typescript
export enum ReportType { LOST = "LOST", FOUND = "FOUND" }
```

Umjesto:
```typescript
export enum ReportDetailsDTOType { LOST = "LOST", FOUND = "FOUND" }
```

## Seed korisnici

| Email | Username | Password |
|-------|----------|----------|
| user1@lostandfound.com | user1 | password123 |
| user2@lostandfound.com | user2 | password123 |

## Seed kategorije prijava

| Kategorija |
|------------|
| Electronics |
| Documents |
| Keys |
| Wallet |
| Jewelry |
| Clothing |
| Bags |
| Pets |
| Other |

## Seed reports

| Title | Type | Category | Status | User |
|-------|------|----------|--------|------|
| Lost iPhone 15 Pro | LOST | Electronics | ACTIVE | user1 |
| Found car keys near park | FOUND | Keys | ACTIVE | user2 |
| Lost brown leather wallet | LOST | Wallet | ACTIVE | user1 |
| Found golden ring | FOUND | Jewelry | ACTIVE | user2 |
| Lost black backpack | LOST | Bags | RESOLVED | user1 |

## Report entiteti

### ReportType (enum)
- `LOST` - Izgubljen predmet
- `FOUND` - Pronađen predmet

### ReportStatus (enum)
- `ACTIVE` - Aktivna prijava
- `RESOLVED` - Riješeno (predmet vraćen)
- `EXPIRED` - Istekla prijava
- `FLAGGED` - Označena za pregled
- `DELETED` - Obrisana (soft delete)

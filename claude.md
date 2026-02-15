# Lost and Found Backend

> **VAŽNO ZA CLAUDE:** Pre svake akcije koju dobiješ kao zadatak, OBAVEZNO pročitaj ovaj CLAUDE.md fajl i proveri koje prakse koristimo u projektu. Prati ustanovljene konvencije i pattern-e opisane ovde.

## Pregled projekta

Spring Boot 4.0.1 REST API aplikacija za Lost and Found platformu. Koristi Java 21, PostgreSQL bazu i JWT autentifikaciju.

## Tehnologije

- **Framework**: Spring Boot 4.0.1
- **Java verzija**: 21
- **Baza**: PostgreSQL
- **Migracije**: Flyway
- **Autentifikacija**: JWT (access + refresh tokeni)
- **Dokumentacija**: Swagger/OpenAPI (springdoc)
- **Build tool**: Maven
- **Email**: Thymeleaf templates + JavaMailSender

## Struktura projekta

```
src/main/java/com/example/demo/
├── config/
│   ├── SecurityConfig.java          # Spring Security + JWT
│   ├── SwaggerConfig.java           # OpenAPI dokumentacija
│   └── FlywayConfig.java            # Database migracije
├── controller/
│   ├── AuthController.java          # Auth endpoints
│   ├── ReportController.java        # Report CRUD
│   ├── ReportCategoryController.java
│   └── HelloController.java
├── dto/
│   ├── AuthRequestDTO.java
│   ├── AuthResponseDTO.java
│   ├── RegisterRequestDTO.java
│   ├── VerifyRequestDTO.java
│   ├── RefreshTokenRequestDTO.java
│   ├── RefreshTokenResponseDTO.java
│   ├── CreateReportRequestDto.java
│   ├── ReportListDTO.java
│   ├── ReportDetailsDTO.java
│   ├── ReportCategoryDto.java
│   └── ErrorResponseDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── UserAlreadyExistsException.java
│   ├── InvalidVerificationException.java
│   ├── InvalidTokenException.java
│   ├── ResourceNotFoundException.java
│   └── EmailSendException.java
├── model/
│   ├── User.java
│   ├── UserStatus.java
│   ├── Report.java
│   ├── ReportType.java
│   ├── ReportStatus.java
│   ├── ReportCategory.java
│   └── PreRegistration.java
├── repository/
│   ├── UserRepository.java
│   ├── ReportRepository.java
│   ├── ReportCategoryRepository.java
│   └── PreRegistrationRepository.java
├── service/
│   ├── AuthService.java
│   ├── ReportService.java
│   ├── ReportCategoryService.java
│   ├── EmailService.java
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   ├── MyUserDetailsService.java
│   └── ScheduleTasks.java           # Cleanup expired pre-registrations
├── util/
│   └── VerificationCodeGenerator.java
└── LostAndFountBackendApplication.java

src/main/resources/
├── application.properties
├── templates/email/
│   └── verification.html            # Email template
└── db/
    ├── migration/
    │   ├── V1__create_users_table.sql
    │   ├── V2__add_username_to_users.sql
    │   ├── V3__create_report_categories_table.sql
    │   ├── V4__create_reports_table.sql
    │   └── V5__create_pre_registrations_table.sql
    └── seed/
        ├── R__1_seed_users.sql
        ├── R__2_seed_report_categories.sql
        └── R__3_seed_reports.sql
```

---

## Flyway Migracije

### Struktura foldera

| Folder | Prefix | Namena | Izvršava se |
|--------|--------|--------|-------------|
| `db/migration/` | `V__` | Šema (CREATE, ALTER) | Jednom |
| `db/seed/` | `R__` | Seed podaci | Kad se fajl promeni |

### Trenutne migracije

| Verzija | Fajl | Sadržaj |
|---------|------|---------|
| V1 | `V1__create_users_table.sql` | `users` tabela |
| V2 | `V2__add_username_to_users.sql` | Dodaje `username` kolonu |
| V3 | `V3__create_report_categories_table.sql` | `report_categories` tabela |
| V4 | `V4__create_reports_table.sql` | `reports` tabela + indexi |
| V5 | `V5__create_pre_registrations_table.sql` | `pre_registrations` tabela |

### Pravilo: Jedna tabela = jedna migracija

Svaka tabela treba biti u zasebnoj migraciji. Ako tabela B ima FK na tabelu A, migracija za B mora imati veći broj od migracije za A.

### Dodavanje migracije

1. Kreiraj `V{broj}__{opis}.sql` u `db/migration/`
2. Restartuj aplikaciju

### Dodavanje seed podataka

1. Kreiraj ili edituj `R__{broj}_{opis}.sql` u `db/seed/`
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

### Reset baze (dev)

```sql
-- Opcija A: Drop i ponovo kreiraj
DROP DATABASE "lost-and-found";
CREATE DATABASE "lost-and-found";

-- Opcija B: Reset flyway history
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS report_categories;
DROP TABLE IF EXISTS pre_registrations;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS flyway_schema_history;
```

---

## JPA Model Konvencije

### Eksplicitno mapiranje kolona (OBAVEZNO)

Svako polje MORA imati eksplicitan `@Column` sa `name` atributom za snake_case mapiranje:

```java
@Column(name = "created_at", nullable = false)
private LocalDateTime createdAt;

@Column(name = "contact_email", length = 255)
private String contactEmail;

@Column(name = "is_active", nullable = false)
private Boolean isActive = true;
```

### Naming strategija

U `application.properties`:
```properties
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
```

### Foreign Key relacije

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = false)
private ReportCategory category;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

---

## DTO Validacija

### Request DTO - koristi Jakarta validaciju

```java
@Data
public class CreateReportRequestDto {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @NotNull(message = "Type is required")
    private ReportType type;

    @NotNull(message = "Category is required")
    private Long categoryId;

    // Opciona polja - bez @NotNull
    @Size(max = 2000)
    private String description;
}
```

### Response DTO - koristi @NotNull/@NotBlank za required polja

**VAŽNO:** Response DTO-ovi MORAJU imati `@NotNull`/`@NotBlank` anotacije na required poljima. Ovo omogućava da OpenAPI/Swagger pravilno generiše TypeScript tipove sa required poljima (bez `?`).

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportListDTO {

    @NotNull
    private Long id;

    @NotBlank
    private String title;

    @NotNull
    private ReportType type;

    @NotBlank
    private String categoryName;

    @NotNull
    private ReportStatus status;

    // Opciono polje - bez anotacije
    private String location;

    @NotNull
    private LocalDateTime createdAt;
}
```

### Rezultat u TypeScript

```typescript
// Sa @NotNull/@NotBlank
export interface ReportListDTO {
  id: number;           // required
  title: string;        // required
  type: ReportType;     // required
  location?: string;    // optional
  createdAt: Date;      // required
}

// Bez anotacija (POGREŠNO)
export interface ReportListDTO {
  id?: number;          // sve optional
  title?: string;
  // ...
}
```

---

## Environment varijable

```properties
JWT_SECRET=your-secret-key-min-32-chars
DB_USERNAME=postgres
DB_PASSWORD=admin
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=your-gmail-app-password
```

## Baza podataka

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/lost-and-found
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=false
```

**VAŽNO:** `ddl-auto=none` - Hibernate NE generiše šemu. Samo Flyway upravlja bazom.

---

## API Endpoints

### Auth Endpoints

| Method | Endpoint | Summary | Response |
|--------|----------|---------|----------|
| POST | `/auth/register` | Register new user | 204 No Content |
| POST | `/auth/verify` | Verify email | `AuthResponseDTO` |
| POST | `/auth/login` | Login | `AuthResponseDTO` |
| POST | `/auth/refresh` | Refresh token | `RefreshTokenResponseDTO` |

### Report Endpoints

| Method | Endpoint | Opis | Response |
|--------|----------|------|----------|
| GET | `/reports` | Lista aktivnih reporta | `List<ReportListDTO>` |
| GET | `/reports?type=LOST` | Filtrirano po tipu | `List<ReportListDTO>` |
| GET | `/reports/{id}` | Detalji reporta | `ReportDetailsDTO` |
| POST | `/reports` | Kreira novi report | `ReportDetailsDTO` (201) |

### Report Category Endpoints

| Method | Endpoint | Opis | Response |
|--------|----------|------|----------|
| GET | `/report-categories` | Lista aktivnih kategorija | `List<ReportCategoryDto>` |

---

## Swagger/OpenAPI

- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8082/v3/api-docs

### Konfiguracija

```properties
springdoc.api-docs.version=openapi_3_0
```

**VAŽNO:** Koristimo OpenAPI 3.0 (ne 3.1) jer 3.1 ima bug sa `@ArraySchema`.

### Dokumentovanje endpointa

```java
@GetMapping
@Operation(summary = "Get all reports", description = "Returns a list of all active reports")
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved list",
        content = @Content(
            mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = ReportListDTO.class))
        )
    )
})
public ResponseEntity<List<ReportListDTO>> getAllReports() {
    // ...
}
```

### Enum definicije za NSwag

```java
@Schema(name = "ReportType", enumAsRef = true)
public enum ReportType {
    LOST,
    FOUND
}
```

---

## Auth Flow

### Registration Flow

1. POST `/auth/register` → Kreira `PreRegistration`, šalje email sa kodom
2. POST `/auth/verify` → Validira kod, kreira `User`, briše `PreRegistration`, vraća tokene

### JWT Tokeni

```properties
jwt.access-token.expiration-ms=36000000      # 10 sati
jwt.refresh-token.expiration-ms=604800000    # 7 dana
```

### Scheduled Cleanup

`ScheduleTasks.java` briše expired `PreRegistration` zapise svakog sata.

---

## Error Handling

| Exception | HTTP Status | Opis |
|-----------|-------------|------|
| `UserAlreadyExistsException` | 409 CONFLICT | Email već postoji |
| `InvalidVerificationException` | 400 BAD_REQUEST | Pogrešan/istekao kod |
| `InvalidTokenException` | 401 UNAUTHORIZED | Neispravan refresh token |
| `ResourceNotFoundException` | 404 NOT_FOUND | Resurs ne postoji |
| `EmailSendException` | 503 SERVICE_UNAVAILABLE | Greška pri slanju emaila |
| `BadCredentialsException` | 401 UNAUTHORIZED | Pogrešan email/password |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | Validaciona greška |

---

## Seed podaci

### Korisnici

| Email | Username | Password |
|-------|----------|----------|
| user1@lostandfound.com | user1 | password123 |
| user2@lostandfound.com | user2 | password123 |

### Kategorije

Electronics, Documents, Keys, Wallet, Jewelry, Clothing, Bags, Pets, Other

### Reports

| Title | Type | Category | Status |
|-------|------|----------|--------|
| Lost iPhone 15 Pro | LOST | Electronics | ACTIVE |
| Found car keys near park | FOUND | Keys | ACTIVE |
| Lost brown leather wallet | LOST | Wallet | ACTIVE |
| Found golden ring | FOUND | Jewelry | ACTIVE |
| Lost black backpack | LOST | Bags | RESOLVED |

---

## Enumi

### ReportType
- `LOST` - Izgubljen predmet
- `FOUND` - Pronađen predmet

### ReportStatus
- `ACTIVE` - Aktivna prijava
- `RESOLVED` - Riješeno
- `EXPIRED` - Istekla
- `FLAGGED` - Označena za pregled
- `DELETED` - Soft delete

### UserStatus
- `ACTIVE` - Aktivan korisnik
- `BLOCKED` - Blokiran
- `PARTIALLY_BLOCKED` - Djelimično blokiran

---

## Best Practices

### JPA Repository

```java
// NE koristi derived delete - radi SELECT + DELETE
void deleteByEmail(String email);  // ❌

// KORISTI @Query za direktan DELETE
@Modifying
@Query("DELETE FROM PreRegistration p WHERE p.email = :email")
void deleteByEmail(@Param("email") String email);  // ✅
```

### @Transactional

Uvek na service metodama koje rade više operacija:

```java
@Transactional
public void register(RegisterRequestDTO request) {
    // ...multiple operations...
}
```

### Dobijanje trenutnog korisnika

U controlleru (NE u servisu):

```java
@PostMapping
public ResponseEntity<ReportDetailsDTO> create(
        @Valid @RequestBody CreateReportRequestDto request,
        @AuthenticationPrincipal UserDetails userDetails) {

    return reportService.create(request, userDetails.getUsername());
}
```

### POST Response (201 Created)

```java
URI location = URI.create("/reports/" + created.getId());
return ResponseEntity.created(location).body(created);
```

---

## Checklist za novi endpoint

- [ ] Request DTO sa validacijom (`@NotBlank`, `@NotNull`, `@Size`)
- [ ] Response DTO sa `@NotNull`/`@NotBlank` za required polja
- [ ] Custom exception ako treba
- [ ] Handler u `GlobalExceptionHandler`
- [ ] Service metoda sa `@Transactional`
- [ ] Controller sa `@Valid`, `@AuthenticationPrincipal`
- [ ] `@Operation` i `@ApiResponse` za Swagger
- [ ] Migracija ako treba nova tabela/kolona

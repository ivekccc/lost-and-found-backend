# Lost and Found Backend

> **VAŽNO ZA CLAUDE:** Pre svake akcije koju dobiješ kao zadatak, OBAVEZNO pročitaj ovaj CLAUDE.md fajl i proveri koje prakse koristimo u projektu. Prati ustanovljene konvencije i pattern-e opisane ovde.

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
| GET | `/reports` | Lista svih aktivnih reporta | `List<ReportListDTO>` |
| GET | `/reports/{id}` | Detalji reporta | `ReportDetailsDTO` |
| POST | `/reports` | Kreira novi report | `ReportDetailsDTO` (201 Created) |

### Report Category Endpoints

| Method | Endpoint | Opis | Response |
|--------|----------|------|----------|
| GET | `/report-categories` | Lista aktivnih kategorija | `List<ReportCategoryDTO>` |

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

## Swagger/OpenAPI dokumentacija endpointa

### Potrebna konfiguracija (application.properties)

```properties
springdoc.api-docs.version=openapi_3_0
```

**VAŽNO:** Koristimo OpenAPI 3.0 umesto 3.1 jer 3.1 ima bug sa `@ArraySchema` - ne generiše `"type": "array"`.

### Dokumentovanje endpointa

Svaki endpoint MORA imati `@Operation` anotaciju sa `summary` i `description`:

```java
@PostMapping("/login")
@Operation(summary = "Login", description = "Authenticates user and returns auth tokens")
public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO req) {
    // ...
}
```

### Endpoint koji vraća 204 No Content

Kada endpoint ne vraća body (npr. register koji samo šalje email):

```java
@PostMapping("/register")
@Operation(summary = "Register new user", description = "Sends verification code to email")
@ApiResponse(responseCode = "204", description = "Verification code sent successfully")
public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO req) {
    authService.register(req);
    return ResponseEntity.noContent().build();
}
```

- Koristi `ResponseEntity<Void>` kao return type
- Dodaj `@ApiResponse(responseCode = "204", ...)` da Swagger prikaže 204 umesto 200
- Service metoda vraća `void`

### Endpoint koji vraća listu (Array)

Za pravilno prikazivanje tipa u Swagger-u (npr. `array<ReportListDTO>` umesto `array<object>`):

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

**Potrebni importi:**
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
```

### DTO klase

DTO klase mogu imati `@Schema` anotaciju za bolji opis u Swagger-u:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReportListDTO", description = "Report summary for list view")
public class ReportListDTO {
    private Long id;
    private String title;
    // ...
}
```

### Error handling

Greške se obrađuju globalno u `GlobalExceptionHandler` i vraćaju `ErrorResponseDTO`:

| Exception | HTTP Status | Opis |
|-----------|-------------|------|
| `UserAlreadyExistsException` | 409 CONFLICT | Email već postoji |
| `InvalidVerificationException` | 400 BAD_REQUEST | Pogrešan/istekao kod |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | Validaciona greška |
| `BadCredentialsException` | 401 UNAUTHORIZED | Pogrešan email/password |
| `Exception` | 500 INTERNAL_SERVER_ERROR | Neočekivana greška |

## Auth Endpoints

| Method | Endpoint | Summary | Response |
|--------|----------|---------|----------|
| POST | `/auth/register` | Register new user | 204 No Content |
| POST | `/auth/verify` | Verify email | `AuthResponseDTO` |
| POST | `/auth/login` | Login | `AuthResponseDTO` |
| POST | `/auth/refresh` | Refresh token | `RefreshTokenResponseDTO` |

## Email Templates

- Koristimo **Thymeleaf** za HTML email template-e
- Template-i se čuvaju u `src/main/resources/templates/email/`
- Nakon promene template fajla: `mvn clean compile` (keširanje u target/)

## Exception Handling

- Svaka vrsta greške ima **custom exception** + handler u `GlobalExceptionHandler`
- NE vraćaj generičke 500 greške korisniku - uvek smislena poruka

| Exception | HTTP Status |
|-----------|-------------|
| `EmailSendException` | 503 SERVICE_UNAVAILABLE |
| `DataIntegrityViolationException` | 409 CONFLICT |
| `ResourceNotFoundException` | 404 NOT_FOUND |

## JPA Repository pravila

- **NE koristi** derived delete metode (`deleteByEmail`) - rade SELECT + DELETE
- **KORISTI** `@Query` za direktan DELETE:

```java
@Modifying
@Query("DELETE FROM Entity e WHERE e.field = :value")
void deleteByField(String value);
```

## @Transactional

- Uvek na service metodama koje rade više operacija
- Garantuje **atomicity** - ako bilo šta baci exception, sve se ROLLBACK-uje

## Kreiranje POST Endpoint-a (Clean Code)

### Struktura

1. **Request DTO** - sa validacijom (`@NotBlank`, `@NotNull`, `@Email`, `@Size`)
2. **Service metoda** - `@Transactional`, prima DTO + userEmail
3. **Controller** - `@Valid @RequestBody`, vraća `201 Created`

### Dobijanje trenutnog korisnika

Koristi `@AuthenticationPrincipal` u controlleru (NE u servisu):

```java
@PostMapping
public ResponseEntity<MyDTO> create(
        @Valid @RequestBody CreateRequestDTO request,
        @AuthenticationPrincipal UserDetails userDetails) {  // Spring injektuje iz JWT-a

    MyDTO created = myService.create(request, userDetails.getUsername());
    // ...
}
```

**Zašto u controlleru?**
- Service ostaje čist (ne zavisi od SecurityContext)
- Lakše testiranje (proslijediš string, ne mockaš SecurityContext)
- Service je reusable (scheduled tasks, message listeners, itd.)

### Response za POST (201 Created + Location)

```java
@PostMapping
public ResponseEntity<ReportDetailsDTO> createReport(...) {
    ReportDetailsDTO created = reportService.createReport(request, userDetails.getUsername());

    URI location = URI.create("/reports/" + created.getId());
    return ResponseEntity.created(location).body(created);
}
```

**Rezultat:**
```
HTTP/1.1 201 Created
Location: /reports/6
Content-Type: application/json

{ "id": 6, "title": "...", ... }
```

### Checklist za novi POST endpoint

- [ ] `CreateXxxRequestDTO` sa validacijom
- [ ] `ResourceNotFoundException` ako treba
- [ ] Handler u `GlobalExceptionHandler`
- [ ] Service metoda sa `@Transactional`
- [ ] Controller sa `@Valid`, `@AuthenticationPrincipal`, `ResponseEntity.created()`

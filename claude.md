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
│   └── HelloController.java
├── dto/
│   ├── AuthRequestDTO.java
│   ├── AuthResponseDTO.java
│   ├── RegisterRequestDTO.java
│   ├── RefreshTokenRequestDTO.java
│   ├── RefreshTokenResponseDTO.java
│   └── ErrorResponseDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── UserAlreadyExistsException.java
├── model/
│   ├── User.java
│   └── UserStatus.java
├── repository/
│   └── UserRepository.java
├── service/
│   ├── AuthService.java
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   └── MyUserDetailsService.java
└── LostAndFountBackendApplication.java

src/main/resources/
├── application.properties
└── db/
    ├── migration/
    │   ├── V1__create_users_table.sql
    │   └── V2__add_username_to_users.sql
    └── seed/
        └── R__seed_users.sql
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

## Seed korisnici

| Email | Username | Password |
|-------|----------|----------|
| user1@lostandfound.com | user1 | password123 |
| user2@lostandfound.com | user2 | password123 |

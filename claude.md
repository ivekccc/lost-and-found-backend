# Lost and Found Backend

## Pregled projekta

Spring Boot 4.0.1 REST API aplikacija za Lost and Found platformu. Koristi Java 21, PostgreSQL bazu i JWT autentifikaciju.

## Tehnologije

- **Framework**: Spring Boot 4.0.1
- **Java verzija**: 21
- **Baza**: PostgreSQL
- **Autentifikacija**: JWT + OAuth2 (Google)
- **Dokumentacija**: Swagger/OpenAPI (springdoc)
- **Build tool**: Maven

## Struktura projekta

```
src/main/java/com/example/demo/
├── config/
│   ├── SecurityConfig.java      # Spring Security konfiguracija
│   └── SwaggerConfig.java       # OpenAPI/Swagger konfiguracija
├── controller/
│   ├── AuthController.java      # /auth/login, /auth/register, /auth/refresh
│   ├── GoogleAuthController.java # /auth/google
│   └── HelloController.java     # /secret (test endpoint)
├── dto/
│   ├── AuthRequestDTO.java      # Login request
│   ├── AuthResponseDTO.java     # Auth response (token, refreshToken, message)
│   ├── RegisterRequestDTO.java  # Register request
│   ├── RefreshTokenRequestDTO.java
│   ├── RefreshTokenResponseDTO.java
│   └── ErrorResponseDTO.java    # Standardni error response
├── exception/
│   ├── GlobalExceptionHandler.java    # @ControllerAdvice za sve greške
│   └── UserAlreadyExistsException.java # Custom exception
├── model/
│   ├── User.java               # @Entity - korisnik
│   ├── Listing.java            # @Entity - oglas (nije implementirano)
│   ├── Category.java           # @Entity - kategorija (nije implementirano)
│   ├── Location.java           # @Entity - lokacija (nije implementirano)
│   ├── AuthProvider.java       # Enum: LOCAL, GOOGLE
│   ├── ListingStatus.java      # Enum (nije implementirano)
│   └── ListingType.java        # Enum (nije implementirano)
├── repository/
│   └── UserRepository.java     # JpaRepository za User
├── service/
│   ├── AuthService.java        # Poslovna logika za auth
│   ├── JwtUtil.java            # JWT generisanje i validacija
│   ├── JwtAuthFilter.java      # Servlet filter za JWT
│   ├── MyUserDetailsService.java # Spring Security UserDetailsService
│   ├── CustomOAuth2UserService.java # OAuth2 user loading
│   └── OAuth2LoginSuccessHandler.java # OAuth2 success handler
└── LostAndFountBackendApplication.java # Main klasa
```

## Konvencije pisanja koda

### Lombok anotacije

```java
// Za DTO klase
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SomeDTO { }

// Za Entity klase (izbegavati @Data zbog JPA lazy loading)
@Entity
@Getter
@Setter
public class SomeEntity { }

// Za Service/Controller klase
@Service
@RequiredArgsConstructor
public class SomeService {
    private final SomeRepository someRepository; // final + @RequiredArgsConstructor
}
```

### Naming konvencije

- **DTO klase**: `{Ime}DTO.java` (npr. `AuthRequestDTO.java`)
- **Entity klase**: `{Ime}.java` (npr. `User.java`)
- **Repository**: `{Entity}Repository.java`
- **Service**: `{Feature}Service.java`
- **Controller**: `{Feature}Controller.java`
- **Exception**: `{Opis}Exception.java`

### Validacija

```java
@Data
public class RegisterRequestDTO {
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be between 0 and 50 characters")
    private String firstName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
```

### Controller pattern

```java
@RestController
@RequestMapping("/feature")
@RequiredArgsConstructor
@Tag(name = "Feature", description = "Feature description") // Swagger
public class FeatureController {
    private final FeatureService featureService;

    @PostMapping("/action")
    public ResponseEntity<ResponseDTO> action(@Valid @RequestBody RequestDTO req) {
        ResponseDTO response = featureService.action(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Service pattern

```java
@Service
@RequiredArgsConstructor
public class FeatureService {
    private final SomeRepository someRepository;

    @Transactional // za metode koje menjaju bazu
    public ResponseDTO action(RequestDTO req) {
        // validacija
        // poslovna logika
        // return response
    }
}
```

### Exception handling

```java
// Custom exception
public class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}

// U GlobalExceptionHandler
@ExceptionHandler(CustomException.class)
public ResponseEntity<ErrorResponseDTO> handleCustomException(CustomException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponseDTO(ex.getMessage(), HttpStatus.CONFLICT.value()));
}
```

## Autentifikacija

### JWT Flow

1. Korisnik šalje credentials na `/auth/login`
2. Server vraća `token` (10h) i `refreshToken` (7 dana)
3. Klijent šalje `Authorization: Bearer {token}` header
4. `JwtAuthFilter` validira token na svakom request-u
5. Kada token istekne, klijent šalje `refreshToken` na `/auth/refresh`

### Endpoints koji preskaču JWT filter

```java
// JwtAuthFilter.java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.startsWith("/auth/")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs");
}
```

### Security konfiguracija

```java
// Dva SecurityFilterChain-a:
// 1. Za Swagger (bez OAuth2)
@Order(1) - /swagger-ui/**, /v3/api-docs/**

// 2. Za ostatak aplikacije
@Order(2) - JWT + OAuth2
```

## Environment varijable

```properties
# Obavezne (bez default vrednosti)
JWT_SECRET=your-secret-key
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Opcione (imaju default)
DB_USERNAME=postgres
DB_PASSWORD=admin
```

### Podešavanje u IntelliJ

Run → Edit Configurations → Environment variables:
```
JWT_SECRET=xxx;GOOGLE_CLIENT_ID=xxx;GOOGLE_CLIENT_SECRET=xxx
```

## API Dokumentacija

- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8082/v3/api-docs

### NSwag generisanje tipova

Za generisanje TypeScript tipova iz OpenAPI spec-a:
1. Otvori NSwag Studio
2. Input URL: `http://localhost:8082/v3/api-docs`
3. Output: TypeScript Client

## Pokretanje

```bash
# Backend
cd lost-and-found-backend
mvnw spring-boot:run

# Ili kroz IntelliJ (sa podešenim env varijablama)
```

## Baza podataka

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/lost-and-found
spring.jpa.hibernate.ddl-auto=update
```

## TODO / Nije implementirano

- [ ] Listing CRUD (model postoji, nema repository/service/controller)
- [ ] Category CRUD
- [ ] Location CRUD
- [ ] Slike za oglase
- [ ] Pretraga oglasa
- [ ] Unit/Integration testovi

## Česte greške

### "User not found" na /auth/register
- Proveriti da li `JwtAuthFilter` preskače `/auth/**` putanje

### Google OAuth redirect_uri_mismatch
- Dodati `http://localhost:8082/login/oauth2/code/google` u Google Cloud Console

### Swagger traži login
- Proveriti da li je `swaggerFilterChain` sa `@Order(1)` definisan pre glavnog filter chain-a

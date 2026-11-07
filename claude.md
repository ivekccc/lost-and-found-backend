# Lost and Found Backend

Spring Boot 4.0.9 REST API za Lost and Found platformu. Java 21, PostgreSQL, Flyway, JWT (access + refresh), springdoc OpenAPI, Maven. Paket: `com.example.demo`. Port: `8082`.

> Za implementaciju bilo koje izmene u ovom projektu koristi `backend-endpoint` skill; za feature koji dodiruje više projekata kreni od `feature-workflow` skila. Stanje feature-a (endpointi, migracije, servisi) uvek čitaj iz koda — ovaj fajl ga namerno ne nabraja.

## Komande

```bash
./mvnw compile              # kompilacija
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run   # pokretanje (primenjuje Flyway migracije; dev profil uključuje Swagger i seed)
```

- Swagger UI: http://localhost:8082/swagger-ui.html
- OpenAPI spec: http://localhost:8082/v3/api-docs (iz njega se generiše @lost-and-found/api paket)

## Struktura

Organizacija je **po domenima** (`user/ report/ verification/ location/ matching/ notification/ shared/`), sa slojevima unutar svakog modula. Servis jednog modula NE SME da koristi repozitorijum drugog — komunikacija ide kroz `*Facade` u korenu modula ili kroz Spring događaje. Detalji u `HANDOFF.md` §3.

## Invarijante

- Šemom upravlja ISKLJUČIVO Flyway (`ddl-auto=none`): šema u `db/migration/V{n}__*.sql`, referentni podaci u `db/seed/R__*.sql` (svuda), test nalozi u `db/dev-seed/R__*.sql` (samo dev profil). Jedna tabela = jedna migracija.
- Svako JPA polje ima eksplicitan `@Column(name = "snake_case")`; relacije su `FetchType.LAZY`.
- Request DTO: Jakarta validacija. Response DTO: `@NotNull`/`@NotBlank` na obaveznim poljima — od toga zavise generisani TypeScript tipovi. Enumi: `@Schema(name = "...", enumAsRef = true)`.
- OpenAPI verzija ostaje 3.0 (`springdoc.api-docs.version=openapi_3_0`) — 3.1 ima bug sa `@ArraySchema`.
- Svaki endpoint ima `@Operation`/`@ApiResponse`; posle izmene DTO-a ili endpointa regeneriši tipove (`api-types-sync` skill).
- Admin funkcionalnost ide u zasebne kontrolere pod `/admin/**` (rola `ADMIN`), ne u grananje unutar korisničkih endpointa.

## Environment varijable

Lokalno: `SPRING_PROFILES_ACTIVE=dev` + tajne (`JWT_SECRET`, `MAIL_*`, `LOCATIONIQ_API_KEY`, `CLOUDINARY_*`, `GOOGLE_CLIENT_ID`); ostalo daje `application-dev.properties`.

Van dev-a (bez podrazumevanih vrednosti, start pada ako fale): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ALLOWED_ORIGINS`, `PUBLIC_BASE_URL`, `FIREBASE_CREDENTIALS_PATH`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `LOCATIONIQ_API_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `GOOGLE_CLIENT_ID`. Opciono `FLYWAY_USERNAME`/`FLYWAY_PASSWORD`, `FORWARD_HEADERS_STRATEGY` (podrazumevano `native`).

Deploy: GitHub Actions (`.github/workflows/deploy.yml`) → GHCR image → Azure Container App `lost-and-found-api` (resource group `lost-and-found-rg`, Germany West Central), prijava preko OIDC (repo variables `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`).

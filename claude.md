# Lost and Found Backend

Spring Boot 4.0.1 REST API za Lost and Found platformu. Java 21, PostgreSQL, Flyway, JWT (access + refresh), springdoc OpenAPI, Maven. Paket: `com.example.demo`. Port: `8082`.

> Za implementaciju bilo koje izmene u ovom projektu koristi `backend-endpoint` skill; za feature koji dodiruje više projekata kreni od `feature-workflow` skila. Stanje feature-a (endpointi, migracije, servisi) uvek čitaj iz koda — ovaj fajl ga namerno ne nabraja.

## Komande

```bash
./mvnw compile              # kompilacija
./mvnw spring-boot:run      # pokretanje (primenjuje Flyway migracije)
```

- Swagger UI: http://localhost:8082/swagger-ui.html
- OpenAPI spec: http://localhost:8082/v3/api-docs (iz njega se generiše @lost-and-found/api paket)

## Invarijante

- Šemom upravlja ISKLJUČIVO Flyway (`ddl-auto=none`): šema u `db/migration/V{n}__*.sql`, seed u `db/seed/R__*.sql` (DELETE + INSERT). Jedna tabela = jedna migracija.
- Svako JPA polje ima eksplicitan `@Column(name = "snake_case")`; relacije su `FetchType.LAZY`.
- Request DTO: Jakarta validacija. Response DTO: `@NotNull`/`@NotBlank` na obaveznim poljima — od toga zavise generisani TypeScript tipovi. Enumi: `@Schema(name = "...", enumAsRef = true)`.
- OpenAPI verzija ostaje 3.0 (`springdoc.api-docs.version=openapi_3_0`) — 3.1 ima bug sa `@ArraySchema`.
- Svaki endpoint ima `@Operation`/`@ApiResponse`; posle izmene DTO-a ili endpointa regeneriši tipove (`api-types-sync` skill).
- Admin funkcionalnost ide u zasebne kontrolere pod `/admin/**` (rola `ADMIN`), ne u grananje unutar korisničkih endpointa.

## Environment varijable

`JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `LOCATIONIQ_API_KEY` (+ Cloudinary i Firebase kredencijali — vidi `application.properties`).

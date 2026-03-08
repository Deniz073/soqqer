[![Java CI](https://github.com/Deniz073/soqqer/actions/workflows/tests.yml/badge.svg)](https://github.com/Deniz073/soqqer/actions/workflows/tests.yml)
[![Docker](https://github.com/Deniz073/soqqer/actions/workflows/docker.yml/badge.svg)](https://github.com/Deniz073/soqqer/actions/workflows/docker.yml)

# Soqqer

Soqqer is een applicatie om tafelvoetbalscores bij te houden over de verschillende vestigingen van Quintor.

## Vereisten

- Java 25
- Docker (voor de PostgreSQL-database)

## Gebruikte libraries/packages

- Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator)
- Spring Modulith (modulaire applicatiestructuur en events)
- Flyway (database migraties)
- PostgreSQL (runtime database)
- springdoc-openapi (Swagger/OpenAPI documentatie)
- MapStruct (DTO/entity mapping)
- Lombok (boilerplate-reductie)
- Testcontainers (PostgreSQL-integratietests)

Swagger UI is lokaal beschikbaar op: `http://localhost:8080/swagger-ui/index.html`

## Lokaal draaien

1. Start PostgreSQL met Docker Compose:

```powershell
docker compose up -d
```

2. Start de applicatie vanuit de projectroot:

```powershell
.\mvnw spring-boot:run
```

De applicatie gebruikt standaard deze database-instellingen:

- Database: `soqqer`
- Gebruiker: `myuser`
- Wachtwoord: `secret`
- Poort: `5432`

3. Stoppen van PostgreSQL:

```powershell
docker compose down
```

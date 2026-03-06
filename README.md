[![Java CI](https://github.com/Deniz073/soqqer/actions/workflows/tests.yml/badge.svg)](https://github.com/Deniz073/soqqer/actions/workflows/tests.yml)

# Soqqer

Soqqer is een applicatie om tafelvoetbalscores bij te houden over de verschillende vestigingen van Quintor.

## Vereisten

- Java 25
- Docker (voor de PostgreSQL-database)

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

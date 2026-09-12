# Merkato Circle

**Digital Iqub Manager** — rotating savings circles built for the modern web.

A Spring Boot application with Thymeleaf frontend, enforcing spec §3 business rules,
Chapa payment integration, and comprehensive automated testing (unit + integration + Selenium).

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security 6 (BCrypt, form login) |
| ORM | Spring Data JPA (Hibernate 6) |
| Database | H2 file-based (`data/iqub.mv.db`) |
| Frontend | Thymeleaf + custom CSS |
| Build | Maven |
| Testing | JUnit 5, Mockito, AssertJ, Selenium 4 |

## Quick Start

```bash
# Build and run
mvn spring-boot:run

# Or build the JAR and run it
mvn package
java -jar target/iqub-0.1.0.jar
```

Open **http://localhost:8080** and log in with any seeded account below.

## Seeded Accounts

Password for all accounts: **`password123`**

| Email | Role | Scenario |
|---|---|---|
| `selam@merkatocircle.et` | Organizer | Won round 1; active on rounds 2 & 3 |
| `abel@merkatocircle.et` | Member | Paid round 2 late (5% penalty band) |
| `marta@merkatocircle.et` | Member | Eligible for round 3 draw |
| `yonas@merkatocircle.et` | Member | Unpaid on round 3 — use this to drive the payment flow |
| `betty@merkatocircle.et` | Member | Unpaid on round 3 |

## Running Tests

```bash
# All tests — unit + integration + Selenium
mvn test

# Unit tests only
mvn test -Dtest=ContributionServiceTest,EligibilityCheckerTest,MemberServiceTest,MembershipServiceTest,RoundServiceTest,BidServiceTest

# Integration tests only
mvn test -Dtest=PersistenceIntegrationTest

# Selenium E2E tests only
mvn test -Dtest=IqubSeleniumTest
```

## Payment Gateway

**No API key or network access required for local development.**

`FakePaymentGateway` is active by default. "Pay with Chapa" redirects to the app's own
`/test/fake-checkout` page same round trip (initiate → checkout → verify → settle),
just with no real money and no outbound calls.

To use real Chapa test-mode:

```bash
export CHAPA_SECRET_KEY=CHASECK_TEST-xxxxxxxxxxxxxxxx
mvn spring-boot:run -Dspring-boot.run.profiles=chapa
```

## CI/CD

### GitHub Actions

The pipeline runs on every push and pull request to `main`/`develop`:
- Builds with Java 21
- Runs all tests (unit + integration + Selenium)
- Uploads JaCoCo coverage report
- Uploads Surefire test results

### Jenkins

A `Jenkinsfile` is provided for Docker-based Jenkins:
- Installs Chromium for Selenium
- Runs build, unit tests, Selenium tests, and coverage
- Publishes JaCoCo HTML report and JUnit results

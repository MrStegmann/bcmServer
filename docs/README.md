# bcmServer Documentation Suite

Welcome to the comprehensive technical documentation for **bcmServer** (Basket Club Manager Server).

---

## Documentation Index

| Document | Audience | Description |
| :--- | :--- | :--- |
| [**Architecture & Design**](file:///j:/GitHub/BasketApp/bcmServer/docs/architecture.md) | Architects & Engineers | Deep dive into Hexagonal (Ports & Adapters) design, layer boundaries, domain isolation, sequence diagrams, and Architecture Decision Records (ADRs). |
| [**API Reference Manual**](file:///j:/GitHub/BasketApp/bcmServer/docs/api_reference.md) | Frontend & Integration Engineers | Complete REST API reference covering Auth, Users, Teams, Players, Games, and Player Stats with JSON schemas, validation rules, error handling (RFC 7807), and curl examples. |
| [**Data Models & Database Schema**](file:///j:/GitHub/BasketApp/bcmServer/docs/data_models.md) | Backend & DB Engineers | Authoritative PostgreSQL 16 schema contracts, Flyway version history (`V1` to `V6`), ER diagram, relationship cascade rules, and basketball PIR formulas. |
| [**Operational Runbook**](file:///j:/GitHub/BasketApp/bcmServer/docs/runbook.md) | DevOps & SREs | Operations guide for Docker Compose, Supabase (IPv4 pooler on port 6543 vs direct IPv6), Flyway repair procedures, secret rotation, actuator health monitoring, and incident triage. |
| [**Developer Onboarding Guide**](file:///j:/GitHub/BasketApp/bcmServer/docs/onboarding.md) | New Contributors | Setup guide (< 5 minutes to first run), IDE annotation processing configuration, step-by-step tutorial on implementing a new hexagonal feature, and testing instructions. |

---

## Interactive Documentation

When the application is running locally:
- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3 JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Actuator Health Endpoint**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)


# AGENTS.md

## Project Overview

Mars Printer Hub is a Spring Boot backend for managing Bambu Lab 3D printers — providing device monitoring via MQTT, filament/spool tracking, print job management, real-time status push (SSE), and multi-channel notifications (Bark/Telegram/Feishu).

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.4.6
- **Build:** Maven (use `./mvnw`)
- **Database:** MySQL 8.0+ with Flyway migrations
- **ORM:** MyBatis-Plus 3.5.12
- **Cache:** Redis (session/state) + Caffeine (in-memory device status)
- **Auth:** Sa-Token 1.44.0 + WebAuthn/Passkey (Yubico)
- **Real-time:** MQTT (Eclipse Paho) for device comms, SSE for frontend push
- **API Docs:** Knife4j (OpenAPI 3)
- **Object Storage:** Tencent COS / Cloudflare R2 / RustFS (S3-compatible), auto-selected
- **Deploy:** Docker (JDK 21 + FFmpeg base image)

## Build & Run Commands

```bash
# Build
./mvnw clean package -DskipTests

# Run (local profile, default)
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Docker build & run
docker build -t mars-printer-hub:latest .
docker run -d -p 8123:8123 -e SPRING_PROFILES_ACTIVE=prod mars-printer-hub:latest
```

**No test suite exists.** There are no tests under `src/test/`.

## Architecture

Base package: `cn.mars.marsprinterhub`

| Package | Role |
|---------|------|
| `controller/` | REST API endpoints |
| `service/` + `service/impl/` | Business logic interfaces and implementations |
| `service/cache/` | Device status caching (Caffeine) |
| `mapper/` | MyBatis-Plus data access (+ XML mappers in `resources/mapper/`) |
| `model/entity/` | Database entity classes |
| `model/dto/` | Request/response DTOs (organized by domain: printer, order, spool, ams, user, etc.) |
| `model/vo/` | View objects |
| `model/enums/` | Enum definitions |
| `config/` | Spring configuration (Redis, MyBatis, Sa-Token, CORS, Flyway, thread pools, Passkey, storage) |
| `bambu/` | Bambu Lab cloud integration (client, MQTT, service, config, DTOs) |
| `websocket/` | WebSocket layer (dto, message, manager, service) |
| `sse/` | Server-Sent Events layer (dto, message, manager, service) |
| `notification/` | Multi-channel notification (impl for each channel, templates) |
| `homeassistant/` | Home Assistant integration (client, model) |
| `processor/` | Event processors |
| `task/` | Scheduled tasks |
| `manager/` | Business managers (cross-service coordination) |
| `openapi/` | Open API endpoints (external integrations) |
| `license/` | License validation |
| `common/` | Shared utilities and base classes |
| `constant/` | Constants |
| `utils/` | Utility classes |

## Key Design Decisions & Conventions

- **Layered architecture:** Controller -> Service -> Mapper. Use `manager/` for cross-service orchestration.
- **Lombok everywhere:** All entities/DTOs use Lombok annotations (`@Data`, `@Builder`, etc.).
- **Flyway migrations:** Schema changes go in `src/main/resources/db/migration/` as `V{N}__description.sql`. FlywayConfig runs `repair()` before `migrate()` to auto-fix checksum mismatches.
- **Object storage strategy:** Conditional bean loading picks the first available backend (COS > R2 > RustFS > NoOp).
- **MQTT connection strategy:** Prefers local network connection to printers; falls back to Bambu cloud MQTT.
- **Auth exclusions:** Health check (`/health`), auth endpoints, Swagger/Knife4j docs, and open API paths bypass authentication.
- **Long serialization:** Jackson is configured to serialize `Long` as `String` to avoid JavaScript precision loss.
- **SSE for frontend updates:** Device state changes are pushed via SSE; the frontend re-fetches full details on notification.
- **Async thread pool:** Core 2, max 4, queue 100 — defined in `TaskExecutorConfig`.
- **Profiles:** `local` (default), `dev`, `prod`. Config in `application-{profile}.yml`.
- **Port:** 8123, context path `/api`.
- **API docs URL:** `http://localhost:8123/api/swagger-ui.html`
- **Adding notifications:** Extend `AbstractNotifier`, implement `doSend`, register in config.

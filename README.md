# SGAF Backend (monorepo)

Prerequisites:
- Docker
- Docker Compose v2 (the `docker compose` command)

Quickstart:
1. Copy environment example:

```bash
cp .env.example .env
```

2. Start services:

```bash
docker compose up -d --build
```

Expected healthy state:
- `postgres` on host port `5432`
- `redis` on host port `6379`
- `traefik` HTTP on port `80`, dashboard on `8080`
- ms-auth -> routed at `/api/auth` (service port 8080)
- ms-usuarios -> `/api/usuarios` (service port 8081)
- ms-tramites -> `/api/tramites` (service port 8082)
- ms-integraciones -> `/api/integraciones` (service port 8083)
- ms-reportes -> `/api/reportes` (service port 8084)
- ms-auditoria -> `/api/auditoria` (service port 8085)

Notes:
- The kotlin services are scaffolded with `build.gradle.kts` and expect a Gradle build producing a fat JAR.
- The Go services include a multi-stage `Dockerfile` and expect a `cmd/server` entrypoint.

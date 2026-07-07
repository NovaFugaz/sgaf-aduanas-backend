# SGAF Backend (monorepo)

## Overview

This repository contains the SGAF backend monorepo with multiple microservices:
- `ms-auth` (Go)
- `ms-usuarios` (Kotlin)
- `ms-tramites` (Kotlin)
- `ms-integraciones` (Go)
- `ms-reportes` (Kotlin)
- `ms-auditoria` (Go)

The project is wired together with Docker Compose and Traefik for local integration.

## Prerequisites

- Docker
- Docker Compose v2 (`docker compose` command)

## Quick start

1. Copy the example environment file:

```bash
cp .env.example .env
```

2. Start the services:

```bash
docker compose up -d --build
```

3. Confirm startup with:

```bash
docker compose ps -a
```

## Services and routing

The stack exposes the following endpoints through Traefik:

- `http://localhost/api/auth` -> `ms-auth` (Go, container port `8080`)
- `http://localhost/api/usuarios` -> `ms-usuarios` (Kotlin, container port `8081`)
- `http://localhost/api/tramites` -> `ms-tramites` (Kotlin, container port `8082`)
- `http://localhost/api/integraciones` -> `ms-integraciones` (Go, container port `8083`)
- `http://localhost/api/reportes` -> `ms-reportes` (Kotlin, container port `8084`)
- `http://localhost/api/auditoria` -> `ms-auditoria` (Go, container port `8085`)

Infrastructure services:

- `postgres` on host port `5432`
- `redis` on host port `6379`
- `traefik` dashboard on `http://localhost:8080`

## Environment variables

The root `.env.example` file contains the variables consumed by the Compose stack:

- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_DB_MAIN`
- `POSTGRES_DB_AUDIT`
- `POSTGRES_DSN`
- `JWT_SECRET`
- `REDIS_URL`
- `MS_AUTH_PORT`
- `MS_USUARIOS_PORT`
- `MS_TRAMITES_PORT`
- `MS_INTEGRACIONES_PORT`
- `MS_REPORTES_PORT`
- `MS_AUDITORIA_PORT`
- `TRAEFIK_DASHBOARD_AUTH`
- `ENVIRONMENT`

> Note: the Go services use `POSTGRES_DSN` and `REDIS_URL` for database and cache connections.

## Build behavior

- Kotlin services are built inside their Dockerfiles using Gradle and produce a runnable JAR.
- Go services are built inside their Dockerfiles using a multi-stage build and produce a single binary at `/app/server`.

## Local development override

The `docker-compose.override.yml` file mounts service source code into containers for local iteration.

## Useful commands

```bash
docker compose logs -f ms-auth ms-usuarios ms-tramites ms-integraciones ms-reportes ms-auditoria

docker compose down

docker compose down -v
```

## Troubleshooting

- If a service does not start, inspect its logs:

```bash
docker compose logs -f <service-name>
```

- If migrated or generated files are being tracked accidentally, ensure `.gitignore` is correct and remove cached tracked files, e.g.:

```bash
git rm -r --cached ms-reportes/.gradle ms-reportes/build ms-tramites/.gradle ms-tramites/build ms-usuarios/.gradle ms-usuarios/build
```

- If Traefik routing is not working, verify the service labels and that `traefik` is healthy.

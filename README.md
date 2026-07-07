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
  - `POST /api/auth/login` - Authenticate user credentials
  - `POST /api/auth/refresh` - Refresh active session token
  - `POST /api/auth/logout` - End current user session
  - `GET /api/auth/validate` - Validate current session token
  - `GET /api/auth/me` - Get current user profile details
- `http://localhost/api/usuarios` -> `ms-usuarios` (Kotlin, container port `8081`)
  - `GET /api/usuarios` - List or filter users
  - `GET /api/usuarios/{id}` - Retrieve user by ID
  - `POST /api/usuarios` - Create new user account
  - `PUT /api/usuarios/{id}` - Modify user details
  - `DELETE /api/usuarios/{id}` - Delete user profile
  - `GET /api/usuarios/por-aduana/{aduana}` - Filter users by aduana
- `http://localhost/api/tramites` -> `ms-tramites` (Kotlin, container port `8082`)
  - `POST /api/tramites` - File a new tramite
  - `GET /api/tramites` - List and filter tramites (paginated)
  - `GET /api/tramites/{id}` - Retrieve tramite details by ID
  - `PATCH /api/tramites/{id}/estado` - Transition tramite state (requires Funcionador/Admin)
  - `GET /api/tramites/{id}/documento` - Retrieve printable document details (SALIDA_VEHICULO/AUTORIZACION_MENOR)
  - `GET /api/tramites/mis-tramites` - Retrieve passenger's recent tramites (last 30 days)
- `http://localhost/api/integraciones` -> `ms-integraciones` (Go, container port `8083`)
  - `POST /api/integraciones/pdi/consultar-rut` - Query RUT database in PDI mock API
  - `POST /api/integraciones/sag/validar-declaracion` - Validate SAG declaration status
  - `POST /api/integraciones/aduana-argentina/consultar-vehiculo` - Query Argentine vehicle data
  - `GET /api/integraciones/estado` - Retrieve mock integration health/status details
- `http://localhost/api/reportes` -> `ms-reportes` (Kotlin, container port `8084`)
  - `GET /api/reportes/flujo-diario` - Fetch daily flow statistics
  - `GET /api/reportes/flujo-semanal` - Fetch weekly flow statistics
  - `GET /api/reportes/vehiculos` - Fetch vehicle statistics
  - `GET /api/reportes/dashboard` - Retrieve dashboard status overview
  - `GET /api/reportes/exportar` - Export report data
- `http://localhost/api/auditoria` -> `ms-auditoria` (Go, container port `8085`)
  - `POST /api/auditoria/eventos` - Publish an audit event
  - `GET /api/auditoria/eventos` - Search/retrieve audit logs
  - `GET /api/auditoria/eventos/resumen` - Get summary metrics of audit logs
  - `GET /api/auditoria/eventos/exportar` - Export audit events data

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

## Postman collection

A Postman collection is available at [sgaf-aduanas.postman_collection.json](file:///c:/Users/nacch/Desktop/sgaf-aduanas-backend/sgaf-aduanas.postman_collection.json) to test all the microservices.

### How to use:
1. Open Postman.
2. Click **Import** and select the [sgaf-aduanas.postman_collection.json](file:///c:/Users/nacch/Desktop/sgaf-aduanas-backend/sgaf-aduanas.postman_collection.json) file.
3. Ensure the collection variable `baseUrl` is set to `http://localhost`.
4. Run any of the login requests under the **Authentication (ms-auth)** folder (e.g., *Login as Admin*). This will execute a script that automatically populates the `token`, `userId`, `userRol`, and `userAduana` collection variables.
5. All requests to **Usuarios** and **Trámites** will automatically consume these variables in their headers to authorize your requests.

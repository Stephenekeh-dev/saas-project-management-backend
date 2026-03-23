# SaaS Project Management API

A multi-tenant project management backend built with Spring Boot. Each tenant (organisation) has fully isolated data — users, projects, and tasks are all scoped to the tenant they belong to.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Entity Relationship Model](#entity-relationship-model)
- [JWT Authentication Flow](#jwt-authentication-flow)
- [Multi-Tenant Isolation](#multi-tenant-isolation)
- [Request Lifecycle](#request-lifecycle)
- [Database Migration Sequence](#database-migration-sequence)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Database Migrations](#database-migrations)
- [API Reference](#api-reference)
- [Security](#security)
- [Configuration](#configuration)
- [Error Handling](#error-handling)
- [Monitoring](#monitoring)

---

## Overview

This application is a REST API for managing projects and tasks across multiple organisations. The data model is simple:

```
Tenant (Organisation)
  └── Users  (ADMIN | MANAGER | MEMBER)
        └── Projects
              └── Tasks  (assigned to users)
```

All operations are tenant-scoped. A user from Tenant A cannot read, modify, or delete resources belonging to Tenant B. This is enforced at the service layer on every request.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| Security | Spring Security + JJWT |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Build | Maven |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Monitoring | Spring Actuator |
| Utilities | Lombok |

---

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        Client                           │
│             (Postman / Frontend / Mobile)               │
└───────────────────────┬─────────────────────────────────┘
                        │  HTTP + Bearer Token
                        ▼
┌─────────────────────────────────────────────────────────┐
│                   Security Layer                        │
│   JwtAuthenticationFilter · SecurityConfig             │
│   CustomUserDetails · JwtUtil                          │
└───────────────────────┬─────────────────────────────────┘
                        │  Authenticated principal
                        ▼
┌─────────────────────────────────────────────────────────┐
│                  Controller Layer                       │
│  AuthController · ProjectController · TaskController   │
│  TenantController · UserController                     │
└───────────────────────┬─────────────────────────────────┘
                        │  DTO request objects
                        ▼
┌─────────────────────────────────────────────────────────┐
│                   Service Layer                         │
│  ProjectServiceImpl · TaskServiceImpl                  │
│  TenantServiceImpl  · UserServiceImpl · AuthService    │
└──────────────┬──────────────────────┬───────────────────┘
               │                      │
               ▼                      ▼
┌──────────────────────┐   ┌─────────────────────────────┐
│   Repository Layer   │   │          Mappers            │
│  ProjectRepository   │   │  ProjectMapper · TaskMapper │
│  TaskRepository      │   └─────────────────────────────┘
│  UserRepository      │
│  TenantRepository    │
└──────────┬───────────┘
           │  JPA / Hibernate
           ▼
┌─────────────────────────────────────────────────────────┐
│          PostgreSQL  ·  saas_db  ·  Flyway              │
└─────────────────────────────────────────────────────────┘
```

---

## Entity Relationship Model

```
┌────────────────────────┐
│         TENANT         │
│────────────────────────│
│ PK  id                 │
│     name  (unique)     │
│     created_at         │
└──────────┬─────────────┘
           │ 1
           │ has many
    ┌──────┴────────────────────────────────┐
    │                                       │
    │ N                                     │ N
┌───┴────────────────────┐    ┌─────────────┴──────────────┐
│          USER          │    │          PROJECT           │
│────────────────────────│    │────────────────────────────│
│ PK  id                 │    │ PK  id                     │
│ FK  tenant_id          │    │ FK  tenant_id              │
│     name               │    │ FK  created_by  → USER     │
│     email  (unique)    │    │     name                   │
│     password (hashed)  │    │     description            │
│     role               │    │     status                 │
│     created_at         │    │     start_date             │
└────────────┬───────────┘    │     end_date               │
             │                │     is_deleted             │
             │ assigned to    │     created_at             │
             │ (0..N tasks)   └────────────┬───────────────┘
             │                             │ 1
             │                             │ has many
             │                             │ N
             │              ┌──────────────┴─────────────┐
             │              │           TASK             │
             │              │────────────────────────────│
             │              │ PK  id                     │
             │              │ FK  project_id             │
             └──────────────┤ FK  assigned_to  → USER    │
                            │     title                  │
                            │     description            │
                            │     status                 │
                            │       TO_DO                │
                            │       IN_PROGRESS          │
                            │       DONE                 │
                            │     priority               │
                            │       LOW                  │
                            │       MEDIUM               │
                            │       HIGH                 │
                            │     due_date               │
                            │     is_deleted             │
                            │     created_at             │
                            └────────────────────────────┘
```

---

## JWT Authentication Flow

```
  Client              JWT Filter           AuthService          Database
    │                     │                     │                   │
    │  POST /auth/login   │                     │                   │
    │  { email, password }│                     │                   │
    │────────────────────►│                     │                   │
    │                     │  Public route —     │                   │
    │                     │  pass through       │                   │
    │                     │────────────────────►│                   │
    │                     │                     │  findByEmail()    │
    │                     │                     │──────────────────►│
    │                     │                     │  User entity      │
    │                     │                     │◄──────────────────│
    │                     │                     │                   │
    │                     │                     │ BCrypt.verify()   │
    │                     │                     │ JwtUtil.generate()│
    │                     │                     │                   │
    │   { token: "..." }  │                     │                   │
    │◄────────────────────┼─────────────────────│                   │
    │                     │                     │                   │
    │                     │                     │                   │
    │  ── Subsequent authenticated requests ──  │                   │
    │                     │                     │                   │
    │  GET /api/projects  │                     │                   │
    │  Authorization:     │                     │                   │
    │  Bearer <token>     │                     │                   │
    │────────────────────►│                     │                   │
    │                     │ extractEmail(token) │                   │
    │                     │ isTokenValid()      │                   │
    │                     │ setAuthentication() │                   │
    │                     │                     │                   │
    │                     │──── proceed to controller ────────────► │
    │                     │                     │                   │
    │  ← 401 Unauthorized if token is missing, expired, or invalid  │
```

---

## Multi-Tenant Isolation

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Single Database                             │
│                                                                     │
│  ┌──────────────────────────┐    ┌──────────────────────────────┐  │
│  │      Tenant A            │    │       Tenant B               │  │
│  │      Acme Corp           │    │       Globex Inc             │  │
│  │  ┌────────────────────┐  │    │  ┌──────────────────────┐   │  │
│  │  │ alice@acme.com     │  │    │  │ carol@globex.com     │   │  │
│  │  │ bob@acme.com       │  │    │  │ dave@globex.com      │   │  │
│  │  └────────────────────┘  │    │  └──────────────────────┘   │  │
│  │  ┌────────────────────┐  │    │  ┌──────────────────────┐   │  │
│  │  │ Project: Apollo    │  │    │  │ Project: Phoenix     │   │  │
│  │  │ Project: Gemini    │  │    │  │ Project: Titan       │   │  │
│  │  └────────────────────┘  │    │  └──────────────────────┘   │  │
│  │  ┌────────────────────┐  │    │  ┌──────────────────────┐   │  │
│  │  │ Tasks (tenant_id=1)│  │    │  │ Tasks (tenant_id=2)  │   │  │
│  │  └────────────────────┘  │    │  └──────────────────────┘   │  │
│  └──────────────────────────┘    └──────────────────────────────┘  │
│                                                                     │
│         Cross-tenant access → 403 Forbidden (service layer)        │
└─────────────────────────────────────────────────────────────────────┘
```

Tenant isolation is not handled by separate schemas or databases. Instead, every service method checks that the resource being accessed belongs to the requesting user's tenant before proceeding. Any mismatch throws an `AccessDeniedException`.

---

## Request Lifecycle

```
 ①            ②              ③             ④             ⑤            ⑥
HTTP        JWT            Controller    Service       Repository   PostgreSQL
Request     Filter                       Layer
  │           │               │             │               │            │
  │──────────►│               │             │               │            │
  │           │ validate      │             │               │            │
  │           │ token         │             │               │            │
  │           │──────────────►│             │               │            │
  │           │               │ extract     │               │            │
  │           │               │ user from   │               │            │
  │           │               │ principal   │               │            │
  │           │               │────────────►│               │            │
  │           │               │             │ tenant        │            │
  │           │               │             │ ownership     │            │
  │           │               │             │ check         │            │
  │           │               │             │──────────────►│            │
  │           │               │             │               │ SQL query  │
  │           │               │             │               │───────────►│
  │           │               │             │               │   result   │
  │           │               │             │               │◄───────────│
  │           │               │             │◄──────────────│            │
  │           │               │             │ map to DTO    │            │
  │◄──────────┼───────────────┼─────────────│               │            │
  │  JSON                                                                │
  │  Response                                                            │
```

---

## Database Migration Sequence

```
  Flyway on startup
        │
        ▼
  ┌───────────┐     ┌───────────┐     ┌───────────┐     ┌───────────┐
  │    V1     │────►│    V2     │────►│    V3     │────►│    V4     │
  │  tenants  │     │   users   │     │  projects │     │   tasks   │
  └───────────┘     └───────────┘     └───────────┘     └─────┬─────┘
                                                               │
        ┌──────────────────────────────────────────────────────┘
        │
        ▼
  ┌───────────┐     ┌───────────┐
  │    V5     │────►│    V6     │────► Hibernate validates schema ✓
  │is_deleted │     │is_deleted │
  │(projects) │     │ (tasks)   │
  └───────────┘     └───────────┘

  Rules:
  - Scripts run in version order, exactly once
  - Checksums are stored in flyway_schema_history
  - spring.jpa.hibernate.ddl-auto=validate (Flyway owns the schema)
  - Never modify an already-applied migration script
```

---

## Project Structure

```
src/main/java/com/steve/saasapp/
│
├── controller/
│   ├── AuthController.java
│   ├── ProjectController.java
│   ├── TaskController.java
│   ├── TenantController.java
│   └── UserController.java
│
├── service/
│   ├── AuthService.java
│   ├── ProjectService.java            (interface)
│   ├── TaskService.java               (interface)
│   ├── TenantService.java             (interface)
│   ├── UserService.java               (interface)
│   └── impl/
│       ├── ProjectServiceImpl.java
│       ├── TaskServiceImpl.java
│       ├── TenantServiceImpl.java
│       └── UserServiceImpl.java
│
├── model/
│   ├── Project.java
│   ├── Task.java
│   ├── Tenant.java
│   ├── User.java
│   └── enums/
│       ├── TaskStatus.java            (TO_DO | IN_PROGRESS | DONE)
│       └── TaskPriority.java          (LOW | MEDIUM | HIGH)
│
├── repository/
│   ├── ProjectRepository.java
│   ├── TaskRepository.java
│   ├── TenantRepository.java
│   └── UserRepository.java
│
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── ProjectRequestDTO.java
│   ├── ProjectResponseDTO.java
│   ├── TaskRequestDTO.java
│   ├── TaskResponseDTO.java
│   ├── TenantRegistrationRequest.java
│   ├── UserRegistrationRequest.java
│   └── UserResponseDTO.java
│
├── mapper/
│   ├── ProjectMapper.java
│   └── TaskMapper.java
│
├── security/
│   ├── SecurityConfig.java
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetails.java
│   └── CurrentUser.java
│
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ProjectAlreadyExistsException.java
    └── ResourceNotFoundException.java

src/main/resources/
├── application.properties
└── db/migration/
    ├── V1__create_tenants_table.sql
    ├── V2__create_users_table.sql
    ├── V3__create_projects_table.sql
    ├── V4__create_tasks_table.sql
    ├── V5__add_is_deleted_to_projects.sql
    └── V6__add_is_deleted_to_tasks.sql
```

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 13+

### 1. Create the database

```sql
CREATE DATABASE saas_db;
```

### 2. Configure application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/saas_db
spring.datasource.username=your_username
spring.datasource.password=your_password

app.jwt.secret=your_secret_key_minimum_32_characters
app.jwt.expiration=86400000
```

### 3. Build and run

```bash
mvn clean install
mvn spring-boot:run
```

Flyway will apply all migration scripts automatically on startup. The application starts on `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Typical first-use flow

```
1. POST /api/tenants/register   →  create your organisation
2. POST /api/users/register     →  create a user under that tenant
3. POST /api/auth/login         →  receive your JWT token
4. POST /api/projects           →  create a project
5. POST /api/projects/{id}/tasks  →  add tasks to the project
```

---

## Database Migrations

Flyway manages all schema changes. Scripts live in `src/main/resources/db/migration/` and follow the naming convention `V{version}__{description}.sql`.

| Version | File | Description |
|---|---|---|
| V1 | `V1__create_tenants_table.sql` | Creates the `tenants` table |
| V2 | `V2__create_users_table.sql` | Creates the `users` table with FK to tenants |
| V3 | `V3__create_projects_table.sql` | Creates the `projects` table |
| V4 | `V4__create_tasks_table.sql` | Creates the `tasks` table |
| V5 | `V5__add_is_deleted_to_projects.sql` | Adds `is_deleted` column to projects |
| V6 | `V6__add_is_deleted_to_tasks.sql` | Adds `is_deleted` column to tasks |

> `spring.jpa.hibernate.ddl-auto` is set to `validate`. Flyway owns the schema — Hibernate only validates against it on startup. Never use `ddl-auto=update` alongside Flyway.

---

## API Reference

All protected endpoints require the header:
```
Authorization: Bearer <token>
```

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Login and receive a JWT token |

**Request body:**
```json
{
  "email": "alice@acme.com",
  "password": "yourpassword"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### Tenants

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/tenants/register` | Public | Register a new tenant organisation |

**Request body:**
```json
{
  "name": "Acme Corp"
}
```

---

### Users

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/users/register` | Public | Register a user under an existing tenant |
| `GET` | `/api/users/me` | Required | Get the authenticated user's profile |

**Register request body:**
```json
{
  "name": "Alice Smith",
  "email": "alice@acme.com",
  "password": "secret123",
  "tenantName": "Acme Corp",
  "role": "ADMIN"
}
```

> Available roles: `ADMIN`, `MANAGER`, `MEMBER`. Defaults to `MEMBER` if not provided.

---

### Projects

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/projects` | Required | Create a project |
| `GET` | `/api/projects` | Required | Get paginated and filtered projects |
| `GET` | `/api/projects/all` | Required | Get all projects (no pagination) |
| `GET` | `/api/projects/{id}` | Required | Get a single project by ID |
| `PUT` | `/api/projects/{id}` | Required | Update a project |
| `DELETE` | `/api/projects/{id}` | Required | Soft-delete a project |

**Create / update request body:**
```json
{
  "name": "Website Redesign",
  "description": "Full overhaul of the company website",
  "startDate": "2025-01-01",
  "endDate": "2025-06-30",
  "status": "IN_PROGRESS"
}
```

**Pagination and filtering query parameters:**
```
GET /api/projects?page=0&size=10&name=website&status=IN_PROGRESS
```

---

### Tasks

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/projects/{projectId}/tasks` | Required | Create a task under a project |
| `GET` | `/api/projects/{projectId}/tasks` | Required | Get all tasks for a project |
| `PUT` | `/api/tasks/{taskId}` | Required | Update a task |
| `DELETE` | `/api/tasks/{taskId}` | Required | Soft-delete a task |

**Create / update request body:**
```json
{
  "title": "Design homepage mockup",
  "description": "Create wireframes and hi-fi mockup in Figma",
  "status": "TO_DO",
  "priority": "HIGH",
  "dueDate": "2025-02-15",
  "assignedToUserId": 3
}
```

> Task statuses: `TO_DO`, `IN_PROGRESS`, `DONE`
> Task priorities: `LOW`, `MEDIUM`, `HIGH`

---

## Security

- All endpoints except registration and login require a valid JWT token.
- Tokens expire after **24 hours** (configurable via `app.jwt.expiration`).
- All sessions are **stateless** — no HTTP sessions are created or stored.
- Passwords are hashed using **BCrypt** before storage and never returned in any response.
- **Tenant isolation** is enforced at the service layer. Accessing a resource from a different tenant returns `403 Forbidden`.

### Public endpoints

```
POST  /api/tenants/register
POST  /api/users/register
POST  /api/auth/login
GET   /actuator/**
GET   /swagger-ui/**
GET   /v3/api-docs/**
```

---

## Configuration

Full `application.properties` reference:

```properties
# Application
spring.application.name=saasapp

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/saas_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=org.postgresql.Driver

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JPA — let Flyway own the schema
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# JWT
app.jwt.secret=your_secret_key_minimum_32_characters_long
# 1 day in milliseconds
app.jwt.expiration=86400000

# Logging
logging.level.org.springframework=INFO
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.springframework.security=DEBUG

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,httpexchanges,heapdump
management.endpoint.heapdump.enabled=true
management.endpoint.metrics.enabled=true

# Swagger
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
```

---

## Error Handling

All errors return a plain HTTP status with a descriptive message body.

| HTTP Status | Scenario |
|---|---|
| `409 Conflict` | Project name already exists for this tenant |
| `404 Not Found` | Project, task, or user not found |
| `403 Forbidden` | Attempting to access another tenant's resource |
| `401 Unauthorized` | Missing, expired, or invalid JWT token |
| `400 Bad Request` | Request body fails validation |

---

## Monitoring

Spring Actuator endpoints are exposed at `/actuator/`:

| Endpoint | Description |
|---|---|
| `/actuator/health` | Application health status |
| `/actuator/info` | Application metadata |
| `/actuator/metrics` | JVM and HTTP metrics |
| `/actuator/httpexchanges` | Recent HTTP request and response log |
| `/actuator/heapdump` | JVM heap dump for diagnostics |

---

*Spring Boot · Spring Security · PostgreSQL · Flyway · JWT*

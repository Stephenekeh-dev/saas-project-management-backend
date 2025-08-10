MULTI-TENANT PROJECT MANAGEMENT BACKEND IMPLEMENTATION

A Spring Boot and Postgresql based backend for a Multi-tenant project management SaaS application.  
Each tenant (organization) manages its own users, projects, and tasks independently with 
role-based access control  and JWT authentication.

**Functional Requirements**
Multi-Tenant Architecture
User Registration with Tenant Association
User Login with JWT-Based Authentication
Role-Based Access Control (ADMIN, MANAGER, USER)
Tenant Creation and Management
Project creation, update, soft-delete, and retrieval (CRUD)
Task creation, assignment, update, deletion, and retrieval (CRUD)
Users can only interact with data belonging to their tenant
Secure session and token management

**Non-Functional Requirements**
Scalable microservice-friendly architecture
Secure RESTful APIs
Passwords encrypted using BCrypt
Token-based stateless authentication (JWT)
Robust error handling and logging
Soft deletion of critical entities
Extensible entity design for future enhancements

**Key Features**
Multi-Tenant Architecture
Role-Based Access Control (ADMIN, MANAGER, USER)
Spring Security and JWT Authentication
CRUD for Tenants, Projects, and Tasks**
Soft Deletion for Critical Entities**
BCrypt Password Hashing
Tenant-Aware API Security

**Entities and Relationships**
One Tenant has many Users
One Tenant has many Projects
One Project has many Tasks
One User can be assigned to many Tasks

**Entity Design**

*Tenant*
id: Long
name: String
createdAt: LocalDateTime

*User*
id: Long
name: String
email: String (unique)
password: String (hashed with BCrypt)
role: Enum (ADMIN, MANAGER, USER)
tenant: Tenant
createdAt: LocalDateTime

*Project*
id: Long
name: String
description: String
startDate: LocalDate
endDate: LocalDate
status: Enum (PLANNED, IN_PROGRESS, COMPLETED)
createdBy: User
tenant: Tenant
isDeleted: Boolean
createdAt: LocalDateTime

*Task*
id: Long
title: String
description: String
priority: Enum (LOW, MEDIUM, HIGH)
status: Enum (TO_DO, IN_PROGRESS, DONE)
dueDate: LocalDate
project: Project
assignedTo: User
isDeleted: Boolean
createdAt: LocalDateTime

**Recommended Technology Stack**
Java 17+
Spring Boot 3+
Spring Security + JWT
Spring Data JPA (Hibernate)
PostgreSQL
Maven

**API Endpoints**

*Authentication APIs*
POST /api/auth/register → Register a new user with tenant
POST /api/auth/login → Authenticate and receive JWT

*Tenant APIs*
POST /api/tenants/register → Create a new tenant
GET /api/tenants → Get all tenants (Admin only)
GET /api/tenants/{id} → Get tenant details by ID

*Project APIs*
POST /api/projects → Create new project
GET /api/projects → Retrieve all projects (for tenant)
GET /api/projects/{id} → Get single project by ID
PUT /api/projects/{id} → Update project
DELETE /api/projects/{id} → Soft-delete project

*Task APIs*
POST /api/projects/{projectId}/tasks → Create task under project
GET /api/tasks → Get all tasks for current tenant
GET /api/tasks/{id} → Get task by ID
PUT /api/tasks/{id} → Update task
DELETE /api/tasks/{id} → Delete task with confirmation message

Clone the Repository

git clone https://github.com/Stephenekeh-dev/saas-project-management-backend

Author
Stephen Ekeh
Email: stevenadibee@yahoo.com
portfolio website:stephen-portfolio-lime.vercel.com




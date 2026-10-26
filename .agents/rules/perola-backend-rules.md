---
trigger: always_on
description: Regras obrigatórias de engenharia, arquitetura (Modular Monolith / Spring Modulith), domínio DDD, banco de dados (PostgreSQL 18 com GiST anti-overbooking), multi-tenancy e padrões de código para o backend Pérola.
---

# Pérola Backend AI Agent Rules & Engineering Guidelines

You are an expert senior Java/Spring Boot software engineer specialized in building the **Pérola** backend (`cv.perola`), a modular SaaS marketplace and booking platform for beauty and wellness salons in Cape Verde.

You MUST strictly comply with these rules and architectural decisions across all tasks, code generations, refactorings, and reviews. These guidelines are adapted from the official project documentation:
- **`docs/PRD.md`**: Product Requirements Document (Core booking MVP, user roles, business rules).
- **`docs/DOMAIN.md`**: Domain-Driven Design (Aggregates, entities, value objects, lifecycle states).
- **`docs/ARCHITECTURE.md`**: Modular Monolith design, boundaries, Spring Modulith, multi-tenancy, and anti-overbooking.
- **`docs/DATABASE.md`**: PostgreSQL 18 physical schema, GiST exclusion constraints, Flyway migrations.
- **`docs/API.md`**: RESTful API contracts, OpenAPI 3.1, RFC 7807 Problem Details.
- **`docs/BACKEND_TASKS.md`**: Backend execution roadmap and critical path.
- **`TRACKING.md`**: Live progress dashboard and task matrix.
- **`MEMORY.md`**: Operational project state, history, and roadmap memory.

---

## ⚡ MANDATORY OPERATIONAL PROTOCOL: TASK LIFECYCLE (MEMORY.md & TRACKING.md)

The AI Agent MUST AUTOMATICALLY and WITHOUT EXPLICIT USER PROMPTING follow this two-phase protocol for EVERY task assigned or executed:

### 1. Pre-Task Phase (Upon Starting ANY Task):
- **Update [TRACKING.md](file:///Users/leonildogomes/Documents/meus-projetos/perola-app/perola-api/TRACKING.md)**:
  - Set the task status in the matrix from `⬜ Pendente` to `🟡 Em Progresso`.
- **Update [MEMORY.md](file:///Users/leonildogomes/Documents/meus-projetos/perola-app/perola-api/MEMORY.md)**:
  - In `## 1. Estado Atual do Projeto`, register that the task is actively underway.
  - In `## 4. Onde o Projeto Parou & Backlog Imediato`, mark the task as in progress.

### 2. Post-Task Phase (Upon Completing ANY Task):
- **Verify Acceptance Criteria**:
  - Run build/verification commands (`mvn clean compile`, `mvn test`, container check).
- **Update [TRACKING.md](file:///Users/leonildogomes/Documents/meus-projetos/perola-app/perola-api/TRACKING.md)**:
  - Change the task status to `✅ Concluído` and record the completion date.
  - Mark all completed sub-items and criteria checkboxes with `[x]`.
  - Recalculate and update the global progress counter (e.g., `X / 17 tarefas concluídas (Y%)`) and Wave counter.
  - Append an entry to `## 📜 Histórico de Atualizações (Execution Log)` with date, task ID, and deliverable summary.
- **Update [MEMORY.md](file:///Users/leonildogomes/Documents/meus-projetos/perola-app/perola-api/MEMORY.md)**:
  - In `## 1. Estado Atual do Projeto`, advance the state to the next task.
  - In `## 3. Log de Progresso e Marcos Concluídos`, append the completed deliverables.
  - In `## 4. Onde o Projeto Parou & Backlog Imediato`, specify the exact stopping point and the immediate next backlog item.

---

---

## 1. Modular Monolith Architecture (Spring Modulith)

The backend is structured as a **Modular Monolith** under the root package `cv.perola`:
```
cv.perola
├── core/                           # Global configs & Cross-cutting infrastructure
│   ├── config/                     # Security, Database, Jackson, Flyway
│   ├── security/                   # JWT, OAuth2, TenantContext, Multi-tenant Filter
│   ├── exception/                  # Global Exception Handler (RFC 7807 Problem Details)
│   └── shared/                     # BaseEntity, Generic DTOs, Time Utils
└── modules/                        # Bounded Contexts (Isolated Modules)
    ├── identity/                   # Users, UserRoles (CUSTOMER, OWNER, EMPLOYEE), JWT, Refresh Tokens
    ├── business/                   # Business (Salons), Services catalog, Employees, EmployeeServices
    ├── schedule/                   # Weekly Schedules, Breaks, TimeOffs, Monthly Opening, Holidays, Slot Engine
    ├── booking/                    # Appointments, AppointmentHistories, Reviews, Availability Service
    └── notifications/              # Async Domain Event Listeners (alerts, logs, emails)
```

### Module Boundary Invariants
1. **Strict Acyclic Dependencies**: Dependencies flow strictly downward:
   - `booking` depends on `schedule`, `business`, `identity`.
   - `schedule` depends on `business`.
   - `business` depends on `identity`.
   - `notifications` depends on nothing (listens to domain events).
2. **Table Ownership**: Only the module that owns a database table may read from or write to it via its own JPA repository.
3. **No Cross-Module Repository Injection**: NEVER inject a `Repository` belonging to another module. Cross-module communication must happen strictly via public `Service` interfaces and DTO records.
4. **Decoupled Side Effects**: Side effects across modules (e.g., booking created -> send notification) MUST be decoupled using Spring Modulith domain events (`ApplicationEventPublisher` and `@EventListener`).
5. **Architectural Verification**: Architecture boundary integrity must be verified by Spring Modulith tests (`ApplicationModules.of(PerolaApplication.class).verify()`).

---

## 2. Core Business Rules & Domain Invariants

1. **Zero Overbooking via PostgreSQL GiST Constraint**:
   - Overbooking prevention is mathematically guaranteed by PostgreSQL 18 with the `btree_gist` extension:
     ```sql
     ALTER TABLE appointments ADD CONSTRAINT ex_appointments_no_overlap
       EXCLUDE USING gist (
         employee_id WITH =,
         tstzrange(start_time, end_time, '[)') WITH &&
       )
       WHERE (status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_PENDING'));
     ```
   - The backend validates schedule rules first, then persists.
   - Any PostgreSQL exclusion constraint violation (error code `23P01`) MUST be caught and mapped by the global exception handler to HTTP `409 Conflict` (RFC 7807 Problem Details) with `type: "https://perola.cv/errors/slot-already-booked"`.
   - NEVER use `SELECT ... FOR UPDATE` to search for free slots (it does not lock non-existent rows and creates race conditions).

2. **Row-Level Multi-Tenancy & Data Isolation**:
   - All tenant-owned tables (`businesses`, `services`, `employees`, `employee_services`, `appointments`, `schedules`) contain `business_id`.
   - Dashboard endpoints require the `X-Business-Id: <UUID>` HTTP header.
   - The multi-tenant filter validates that the authenticated user has an active `OWNER` or `EMPLOYEE` role for that specific `business_id`, populating `TenantContext`.
   - All repository queries in tenant scope MUST enforce filtering by the active `business_id`. Cross-tenant data leakage is strictly prohibited (return `403 Forbidden` if unauthorized).

3. **Mandatory Monthly Schedule Opening**:
   - Before clients can view or book slots in any given month, the employee must have explicitly activated that month (`EmployeeMonthlySchedule.is_opened = true`).
   - If no opened record exists for that employee/year/month, the availability service MUST return empty slots.

4. **Slot Calculation Engine**:
   - `SlotCalculationEngine` in the `schedule` module is a pure, deterministic function. It receives working shifts, pauses, time-offs, holidays, and already-booked intervals, and outputs free discrete slots matching the requested service duration.
   - `AvailabilityService` in the `booking` module acts as the orchestrator: fetches schedule windows, appends active bookings, and delegates to `SlotCalculationEngine`.

5. **Historical Snapshotting**:
   - When an `Appointment` is created, it MUST snapshot `service_name`, `service_price`, and `service_duration_minutes` into the appointment entity. Future changes to the service catalog must never alter historical records.

6. **Immutable Audit Trail (`AppointmentHistory`)**:
   - `appointment_histories` is **append-only**.
   - Every state transition (`CREATED`, `CONFIRMED`, `RESCHEDULE_REQUESTED`, `RESCHEDULE_CONFIRMED`, `RESCHEDULE_REJECTED`, `CANCEL_REQUESTED`, `CANCELLED`, `COMPLETED`) MUST record a new history entry with `performed_by`, `performed_at`, timestamps, status transition, and notes.
   - `UPDATE` and `DELETE` queries on `appointment_histories` are strictly forbidden.

7. **Soft-Deactivation**:
   - Real-world operational entities (`businesses`, `employees`, `services`) utilize `is_active = false` (soft-deactivation). Hard deletes are prevented by `ON DELETE RESTRICT` constraints to protect historical and financial integrity.

8. **Reviews**:
   - A `Review` can ONLY be submitted by the customer after the `Appointment` has transitioned to `COMPLETED`.

---

## 3. Dates, Timezones, and Temporal Rules

1. **UTC Storage**: All absolute timestamps (`start_time`, `end_time`, `created_at`, `updated_at`, `opened_at`) MUST be stored in PostgreSQL as `TIMESTAMPTZ` in UTC and represented in Java as `Instant` or `OffsetDateTime`.
2. **Business Timezone**: Each business defines a `timezone` (default `Atlantic/Cape_Verde`, UTC-1 without daylight saving time).
3. **Local Times**: Weekly standard working shifts and breaks are stored as `TIME WITHOUT TIME ZONE` (`LocalTime`). They are converted to UTC instants when calculating availability slots for specific calendar dates.
4. **API Serialization**: All date-time responses in the API MUST be formatted in ISO-8601 with offset (e.g., `2026-11-03T10:00:00-01:00`).

---

## 4. Code Style & Engineering Standards

### Java & Spring Boot Specifics
- **Java 21+ / Java 26**: Use modern Java features:
  - Java `record` for all DTOs (Request / Response), projection records, and immutable Value Objects.
  - Pattern matching for `switch` and `instanceof`.
  - Sealed interfaces and records for domain events.
- **Dependency Injection**:
  - Always use **Constructor Injection** with `private final` fields.
  - Use Lombok's `@RequiredArgsConstructor` for classes with injected dependencies.
  - Field injection (`@Autowired` on attributes) is STRICTLY FORBIDDEN.
- **Separation of Concerns**:
  - Never expose JPA entities in controller endpoints. Always map to and from DTO records.
  - Controllers handle routing, HTTP validation, and mapping.
  - Services / Use Cases encapsulate business logic and transaction boundaries (`@Transactional`).
  - Repositories handle database interactions via Spring Data JPA.
- **Validation**:
  - Use Jakarta Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`, `@Positive`, `@Size`, etc.) on all incoming request DTOs.
  - Custom business validations must be asserted before database interaction.
- **Error Handling (RFC 7807)**:
  - All errors (`4xx`, `5xx`) must produce `application/problem+json` formatted responses managed by `@RestControllerAdvice`.
  - Include fields: `type`, `title`, `status`, `detail`, `instance`, and `invalidParams` (for validation errors).

### Naming Conventions
- **Classes & Interfaces**: PascalCase (e.g., `AppointmentService`, `SlotCalculationEngine`, `UserRoleRepository`).
- **Methods & Variables**: camelCase (e.g., `findAvailableSlots`, `businessId`, `isOpened`).
- **Constants**: ALL_CAPS with underscores (e.g., `DEFAULT_TIMEZONE`, `MAX_PAGE_SIZE`).
- **Database Schema**: `snake_case` in lowercase for tables, columns, and constraints (e.g., `employee_monthly_schedules`, `duration_minutes`, `ck_appointments_time_order`).

---

## 5. Database & Persistence (PostgreSQL 18 & Flyway)

- **Flyway Migrations**: All database schema modifications MUST be versioned Flyway SQL scripts located in `src/main/resources/db/migration/` (e.g., `V1__initial_schema.sql`, `V2__seed_cape_verde_holidays.sql`).
- **DDL Auto**: Hibernate automatic DDL changes (`ddl-auto=update`, `ddl-auto=create`) are strictly prohibited. Use `validate` or `none`.
- **Primary Keys**: Always `UUID` generated natively via `gen_random_uuid()`.
- **Postgres Enums**: Enums (`user_role`, `day_of_week`, `time_off_type`, `appointment_status`, `appointment_action`) are mapped via `@Enumerated(EnumType.STRING)`.
- **Monetary Values & Durations**: Prices stored as `NUMERIC(10, 2)` (CVE). Durations stored as `INTEGER` representing minutes.

---

## 6. Security, RBAC & Authentication

- **Stateless JWT**: Authentication uses Bearer JWT tokens signed with HS256.
- **Roles**: `CUSTOMER`, `OWNER`, `EMPLOYEE`. Users can have multiple roles across businesses (`Set<Role>`).
- **Endpoint Protection**:
  - Public endpoints (`/api/v1/marketplace/**`, `/api/v1/auth/**`) are open.
  - Owner endpoints require `hasRole('OWNER')` and validation of `X-Business-Id`.
  - Employee endpoints require `hasRole('EMPLOYEE')` or `hasRole('OWNER')`.
  - Customer endpoints require `hasRole('CUSTOMER')` and restrict operations strictly to the user's own data (`customer_id == authentication.userId`).
- **Password Encoding**: Passwords hashed using `BCryptPasswordEncoder`.

---

## 7. Testing Requirements

- **Unit Tests**: JUnit 5, Mockito, and AssertJ for pure domain logic, calculators (`SlotCalculationEngine`), and isolated service logic.
- **Repository & Slice Tests**: Use `@DataJpaTest` and `@WebMvcTest` with MockMvc.
- **Integration Tests**: `@SpringBootTest` with Testcontainers running real PostgreSQL 18 with `btree_gist` enabled.
- **Concurrency & GiST Tests**: Mandatory integration test verifying the anti-overbooking constraint under multi-threaded concurrency (e.g., 20 parallel requests attempting to book the identical employee slot; verify exactly 1 succeeds and 19 receive `409 Conflict`).
- **Modular Monolith Verification**: Spring Modulith verification test verifying dependency boundaries and detecting any cyclic dependencies.

---

## 8. Build, Environment & Tooling

- **Build**: Apache Maven (`pom.xml`).
- **Lombok**: Use `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`, and `@NoArgsConstructor(access = AccessLevel.PROTECTED)` for JPA entities.
- **Configuration**: Use `application.yml` with Spring profiles (`dev`, `test`, `prod`). Sensitive credentials must be injected via environment variables.
- **Containerization**: Provide a `docker-compose.yml` defining PostgreSQL 18 with the `btree_gist` extension for local development and testing.

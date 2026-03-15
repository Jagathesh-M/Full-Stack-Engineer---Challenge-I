# Workflow Engine

Spring Boot API for workflows: define steps and rules, run executions, track status. Runs on **Java 17**, **Maven**, **MySQL**.

---

## Techs Used 

=>Frontend : React,CSS,JavaScript
=>Backend  : SpringBoot
=>DataBase : MySql

---

## Setup

1. **Prerequisites:** Java 17, Maven 3.6+, MySQL 8.

2. **Database:** Create a DB and set URL/user/password in `src/main/resources/application.yml`:
   ```sql
   CREATE DATABASE workflowdb;
   ```
   In `application.yml`: set `spring.datasource.url`, `username`, `password`.

3. **Run:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   API: **http://localhost:8080**

---

## Dependencies

- Spring Boot 3.2 (Web, JPA, Validation)
- MySQL connector
- Lombok

---

## Design

- **Workflow** → has **Steps**; each **Step** has **Rules** (condition + next step + priority).
- **Execution** = one run: has `data` (JSON), `logs`, `currentStepId`, `status`. Start at workflow’s start step; each step’s rules are evaluated in priority order; first matching rule sets next step (or end if `nextStepId` is null).
- **Rule conditions:** `field == 'value'`, `!=`, `<`, `>`, `&&`, `||`, `contains(field, "x")`, `startsWith`, `endsWith`, `DEFAULT` (always true). Nested: `payload.amount`.
- **API:** `/workflows` (CRUD), `/workflows/{id}/detail` (with steps+rules), `/steps`, `/rules`, `/workflows/{id}/execute` (POST with `data`), `/executions` (list, get, cancel, retry).
- **Config:** `server.port` (8080), `workflow.max-loop-iterations` (100). CORS allowed for `localhost:3000`.

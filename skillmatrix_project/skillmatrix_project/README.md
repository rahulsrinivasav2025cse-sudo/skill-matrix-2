# SkillMatrix - Employee Skill Matrix and Project Staffing Recommender

Spring Boot 3 + Spring Data JPA + MySQL, with an HTML/CSS/JS frontend served from `src/main/resources/static`.

## Run
1. Install JDK 17+, Maven, and MySQL. Start MySQL.
2. Edit `src/main/resources/application.properties` (username/password). The database `skillmatrix_db` and all tables are created automatically.
3. `mvn spring-boot:run`
4. Open http://localhost:8080  (every change made in the UI is saved in MySQL).

## Entities
Employee, Skill, Project, Allocation, plus join entities EmployeeSkill (employee x skill x proficiency 1-5) and ProjectSkill (project x skill x minimum proficiency).

## REST API
| Feature | Endpoint |
|---|---|
| Employees CRUD | GET/POST `/api/employees`, GET/PUT/DELETE `/api/employees/{id}` |
| Employee skill + proficiency | PUT `/api/employees/{id}/skills` `{skillId, proficiency}`; DELETE `/api/employees/{id}/skills/{skillId}` |
| Skills CRUD | GET/POST `/api/skills`, GET/PUT/DELETE `/api/skills/{id}` |
| Projects CRUD | GET/POST `/api/projects`, GET/PUT/DELETE `/api/projects/{id}` |
| Project required skills | PUT `/api/projects/{id}/skills` `{skillId, proficiency}`; DELETE `/api/projects/{id}/skills/{skillId}` |
| Recommendations | GET `/api/projects/{id}/recommendations` |
| Allocations CRUD | GET/POST `/api/allocations`, GET/PUT/DELETE `/api/allocations/{id}` (PUT body `{percentage}`) |
| Coverage and gaps | GET `/api/coverage?projectId=1` or `/api/coverage?skillIds=1,2,3&minProficiency=3` |

## Business rules (enforced in the service layer, before saving)
- Total allocation per employee can never exceed their available capacity (409 with a clear message). Also enforced when editing an allocation or lowering an employee's capacity.
- Recommendations: more matched skills first, then higher proficiency, then more free capacity. Unavailable employees are excluded.
- Errors return JSON `{status, error, message, fieldErrors}` (400 validation, 404 not found, 409 rule violation) - never stack traces.

## Suggested Postman/Swagger tests
Create skills -> employees -> projects -> set skills -> allocate 60% then 50% to the same employee (second must return 409) -> recommendations -> coverage.

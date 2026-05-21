# AEM Full-Stack Optimization Assessment

## 1. Objective
Refactor this legacy AEM implementation to meet modern enterprise standards for AEM 6.5 and AEM as a Cloud Service. Identify and fix technical debt, security risks, and performance bottlenecks without changing the functional intent of the weather component.

## 2. Assessment Scenario
A technical audit has flagged the Weather Data component for:

- Security: API keys are exposed in the frontend, and Dispatcher filters are too permissive.
- Performance: There is no server-side caching, and network calls are synchronous.
- Architecture: The implementation relies on global configuration patterns instead of tenant-aware configuration.

This repository intentionally contains flawed legacy code. Do not treat the current implementation as a recommended AEM pattern.

## 3. Runtime Target
- Primary target: local AEM as a Cloud Service SDK development environment.
- Architectural expectation: choose patterns that are valid for AEM as a Cloud Service and defensible for AEM 6.5 where applicable.
- Java version: 11.
- Build tool: Maven 3.9+ recommended.

## 4. Project Structure
This repository includes a fuller archetype-style layout. Not every module requires changes for this exercise.

- `core`: Java backend code, Sling Models, and services. This is a primary focus area.
- `ui.apps`: component definitions, HTL, client libraries, and embedded bundle install path. This is a primary focus area.
- `ui.config`: application-level OSGi config package. Use if your refactor needs configuration packaging.
- `ui.content`: sample content and configuration content. This is a primary focus area.
- `dispatcher`: Apache and Dispatcher configuration. This is a primary focus area.
- `all`: aggregate install package.
- `ui.apps.structure`: structure package for `/apps`.
- `ui.frontend`: lightweight placeholder module included for archetype parity. You are not expected to build a frontend pipeline for this exercise.
- `it.tests`: placeholder module for archetype parity.
- `ui.tests`: placeholder module for archetype parity.

## 5. What To Review First
The main legacy implementation is centered around:

- `core/src/main/java/com/assessment/core/services/impl/WeatherServiceImpl.java`
- `core/src/main/java/com/assessment/core/models/WeatherModel.java`
- `ui.apps/src/main/content/jcr_root/apps/assessment/components/weather/weather.html`
- `dispatcher/src/conf.dispatcher.d/filters/filters.any`

Sample content already includes the component on:

- `/content/assessment/us/en.html`

## 6. Build And Local Validation
Build the project from the repository root:

```bash
mvn -U -DskipTests clean package
```

Primary build artifact:

- `all/target/assessment.all-1.0.0-SNAPSHOT.zip`

Install the package into your local AEM author instance using Package Manager or your preferred local deployment workflow.

Typical local author URL:

- `http://localhost:4502`

After installation, validate the sample page:

- `http://localhost:4502/content/assessment/us/en.html`

Notes:

- The current legacy implementation calls an external weather service. Runtime behavior depends on that third-party endpoint being reachable.
- The repository does not include an opinionated local Dispatcher container setup. Treat the `dispatcher` module as configuration source for review and refactoring.

## 7. Required Tasks
- Backend: Refactor the backend integration to improve security, resilience, performance, and maintainability.
- Architecture: Replace the current configuration approach with a tenant-aware solution suitable for enterprise AEM.
- Frontend: Remove inline JavaScript and business logic from HTL. Use a server-side pattern to provide data safely.
- Dispatcher: Harden `filters.any` so only the required paths, selectors, and endpoints are allowed.
- Testing: Add automated tests that validate the refactored behavior.

## 8. Constraints
- Do not submit only a written review. Implement the refactor.
- Do not remove the component entirely or replace the exercise with a different feature.
- Do not add the solution directly to this README.
- Add and manage any dependencies required by your solution as part of the refactor.

## 9. Submission
Do not submit a public Pull Request.

Include:

- your code changes
- a `DECISIONS.md` file explaining key architectural choices such as caching strategy, configuration strategy, and Dispatcher hardening decisions
- any assumptions needed to run or evaluate your solution
- a public fork or public repository link shared by email

Submission instructions:

- keep your fork or repository public
- send the fork or repository URL by email to `ext-facundo.capua@globant.com`, `oscar.salas@globant.com`, `galvis.herrera@globant.com`, and `d.garciabojaca@globant.com`
- use the email subject `aem assessment - name`
- include the branch name or commit SHA that should be reviewed
- include any setup notes required to build, install, and validate the solution locally

## 10. Evaluation Criteria
Your submission will be evaluated on:

- correctness
- security improvements
- performance improvements
- AEM architectural quality
- clarity of configuration approach
- code quality and maintainability
- test quality
- reasoning documented in `DECISIONS.md`

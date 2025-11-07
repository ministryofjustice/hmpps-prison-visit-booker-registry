# HMPPS Prison Visit Booker Registry

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-prison-visit-booker-registry)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-prison-visit-booker-registry "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-prison-visit-booker-registry)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://hmpps-prison-visit-booker-registry-dev.prison.service.justice.gov.uk/swagger-ui/index.html)
[![GitHub Actions Pipeline](https://github.com/ministryofjustice/hmpps-prison-visit-booker-registry/actions/workflows/pipeline.yml/badge.svg)](https://github.com/ministryofjustice/hmpps-prison-visit-booker-registry/actions/workflows/pipeline.yml)

This is a Spring Boot application, written in Kotlin, providing API services for prison visit booker. Used by [Visits UI](https://github.com/ministryofjustice/book-a-prison-visit-staff-ui).

## Building

To build the project (without tests):
```
./gradlew clean build -x test
```

## Testing

Run:
```
./gradlew test 
```

## Running

The prison-visit-booker-registry uses the deployed dev environment to connect to most of the required services,
with the exception of prison-visit-booker-registry-db and localstack (for AWS SNS/SQS services locally).

To run the prison-visit-booker-registry, first start the required local services using docker-compose.
```
docker-compose up -d
```
Next create a .env file at the project root and add 2 secrets to it
```
SYSTEM_CLIENT_ID="get from kubernetes secrets for dev namespace"
SYSTEM_CLIENT_SECRET"get from kubernetes secrets for dev namespace"
```

Then create a Spring Boot run configuration with active profile of 'dev' and set an environments file to the
`.env` file we just created. Run the service in your chosen IDE.

Ports

| Service                         | Port |  
|---------------------------------|------|
| prison-visit-booker-registry    | 8083 |
| prison-visit-booker-registry-db | 5444 |
| localstack                      | 4566 |


### Auth token retrieval

To create a Token via curl (local):
```
curl --location --request POST "https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token?grant_type=client_credentials" --header "Authorization: Basic $(echo -n {Client}:{ClientSecret} | base64)"
```

or via postman collection using the following authorisation urls:
```
Grant type: Client Credentials
Access Token URL: https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token
Client ID: <get from kubernetes secrets for dev namespace>
Client Secret: <get from kubernetes secrets for dev namespace>
Client Authentication: "Send as Basic Auth Header"
```

Call info endpoint:
```
$ curl 'http://localhost:8083/info' -i -X GET
```

## Swagger v3
Prison Visit Booker Registry
```
http://localhost:8083/swagger-ui/index.html
```

Export Spec
```
http://localhost:8083/v3/api-docs?group=full-api
```

## Application Tracing
The application sends telemetry information to Azure Application Insights which allows log queries and end-to-end request tracing across services

##### Application Insights Events

Show all significant prison visit booker registry events
```azure
customEvents 
| where cloud_RoleName == 'hmpps=prison-visit-booker-registry' 
```

## Common gradle tasks

To list project dependencies, run:

```
./gradlew dependencies
``` 

To check for dependency updates, run:
```
./gradlew dependencyUpdates --warning-mode all
```

To run an OWASP dependency check, run:
```
./gradlew clean dependencyCheckAnalyze --info
```

To upgrade the gradle wrapper version, run:
```
./gradlew wrapper --gradle-version=<VERSION>
```

To automatically update project dependencies, run:
```
./gradlew useLatestVersions
```

#### Ktlint Gradle Tasks

To run Ktlint check:
```
./gradlew ktlintCheck
```

To run Ktlint format:
```
./gradlew ktlintFormat
```

To apply ktlint styles to intellij
```
./gradlew ktlintApplyToIdea
```

To register pre-commit check to run Ktlint format:
```
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook 
```

...or to register pre-commit check to only run Ktlint check:
```
./gradlew ktlintApplyToIdea addKtlintCheckGitPreCommitHook
```

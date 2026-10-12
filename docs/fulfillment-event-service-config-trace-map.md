# Fulfillment Event Service Configuration Trace Map

> This map traces checked-in configuration sources for `fulfillment-event-service`; it does not represent effective live configuration. Secret values are omitted.

## Configuration trace map

| `application.yml` | `Default` | `application-local.yml` | `census31-fwmt-ci-terraform/k8s/fulfillment-event-service/fulfillment-event-service-deployment.yaml` | `census31-fwmt-terraform/secrets.tf` | `census31-fwmt-terraform/secret-manager.tf` |
|---|---|---|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost/postgres?currentSchema=fwmtg` | — | `DB_NAME <- db-config[db-name]`; `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/$(DB_NAME)?currentSchema=fwmtg` | `db-config[db-name]=var.db_name` | — |
| `spring.datasource.username` | `postgres` | — | `SPRING_DATASOURCE_USERNAME <- db-credentials[username]` | `db-credentials[username]=var.db_user_name` | — |
| `spring.datasource.password` | `[MASKED]` | — | `SPRING_DATASOURCE_PASSWORD <- db-credentials[password]` | `db-credentials[password]=random_password.cloudsqlUser.result` | — |
| `spring.cloud.gcp.project-id` | `fwmt-local` | — | `SPRING_CLOUD_GCP_PROJECT_ID <- project-config[project-id]` | `project-config[project-id]=var.project_id` | — |
| `spring.cloud.gcp.pubsub.emulator-host` | — | `spring.cloud.gcp.pubsub.emulator-host=localhost:8085` | — | — | — |
| `management.endpoint.env.enabled` | `${APP_TESTING:false}` | `management.endpoint.env.enabled=true` | — | — | — |
| `management.endpoint.env.post.enabled` | `${APP_TESTING:false}` | `management.endpoint.env.post.enabled=true` | — | — | — |
| `management.endpoint.refresh.enabled` | `${APP_TESTING:false}` | `management.endpoint.refresh.enabled=true` | — | — | — |
| `totalmobile.username` | `user` | — | `TOTALMOBILE_USERNAME <- tm-config[username]` | `tm-config[username]=lookup(local.tm_config_secret_payload, "tm_username", "")` | `secret-manager[tm-config]` |
| `totalmobile.password` | `[MASKED]` | — | `TOTALMOBILE_PASSWORD <- tm-config[password]` | `tm-config[password]=lookup(local.tm_config_secret_payload, "tm_password", "")` | `secret-manager[tm-config]` |
| `totalmobile.baseUrl` | `http://localhost:8000/` | — | `TOTALMOBILE_BASEURL <- tm-config[url]` | `tm-config[url]=lookup(local.tm_config_secret_payload, "tm_url", "")` | `secret-manager[tm-config]` |
| `totalmobile.comet.clientID` | `""` | — | `TOTALMOBILE_COMET_CLIENTID <- tm-config[clientid]` | `tm-config[clientid]=lookup(local.tm_config_secret_payload, "tm_clientid", "")` | `secret-manager[tm-config]` |
| `totalmobile.comet.clientSecret` | `[MASKED]` | — | `TOTALMOBILE_COMET_CLIETSECRET <- tm-config[clientsecret]` | `tm-config[clientsecret]=lookup(local.tm_config_secret_payload, "tm_clientsecret", "")` | `secret-manager[tm-config]` |
| `app.messaging.pubsub.rm-project-id` | `${RM_PUBSUB_PROJECT:}` | — | `RM_PUBSUB_PROJECT <- project-config[rm-pubsub-project-id]` | `project-config[rm-pubsub-project-id]=var.rm_pubsub_project_id` | — |

## Verification items

- `TOTALMOBILE_COMET_CLIETSECRET` in `census31-fwmt-ci-terraform/k8s/fulfillment-event-service/fulfillment-event-service-deployment.yaml` is spelled differently from `totalmobile.comet.clientSecret` in `src/main/resources/application.yml`; verify the binding before assuming the deployed secret overrides the default. The `clientID` binding also uses mixed case and has not been verified.
- The GCP project setting is in a second YAML document activated by `spring.config.activate.on-property` for `app.messaging.provider=pubsub` in `src/main/resources/application.yml`; verify that activation is supported by the resolved Spring Boot version.
- `APP_TESTING` is referenced by `src/main/resources/application.yml` but is not set in `census31-fwmt-ci-terraform/k8s/fulfillment-event-service/fulfillment-event-service-deployment.yaml`.

## Omitted non-property configuration

- Probes, resources, Cloud SQL proxy, and service account: `census31-fwmt-ci-terraform/k8s/fulfillment-event-service/fulfillment-event-service-deployment.yaml`.
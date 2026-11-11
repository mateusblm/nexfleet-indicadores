# NexFleet — Indicadores

Microsserviço do painel de custos e situação da frota do NexFleet, plataforma de gestão de frotas. Parte da v1.0.0; o sistema completo está no repositório guarda-chuva [nexfleet](https://github.com/mateusblm/nexfleet).

## Arquitetura

Indicadores consome os eventos `AbastecimentoRegistrado` v1 e `ManutencaoRegistrada` v1 publicados pela outbox de Operações, do **Azure Service Bus** (nuvem, assinatura `indicadores-v1` do tópico `nexfleet-eventos-v1`) ou do **RabbitMQ** (local, com retry e DLQ), e grava projeções **idempotentes** no **Azure SQL** (schema `indicadores`, login próprio, migrações Flyway): um evento repetido tem efeito uma única vez. Expõe `GET /api/v1/indicadores/dashboard`, que o BFF usa para o Painel.

```text
Navegador → shell + microfrontends (Static Web Apps)
          → Azure API Management (APIM) → Gateway → BFF
              ├─ MS1 Frota ........ MongoDB Atlas
              ├─ MS2 Operações .... Azure SQL (schema operacoes)
              ├─ Identidade, Indicadores ... Azure SQL (um schema e um login por serviço)
              └─ Azure Function HTTP calcular-eficiencia
Operações → outbox → Service Bus (nuvem) / RabbitMQ (local) → Indicadores + Function (notificação)
```

**Clean Architecture + Vertical Slice.** Fatias em `br.com.nexfleet.indicadores`:

| Fatia | Camadas | Responsabilidade |
|---|---|---|
| `projetarabastecimento` | application, infrastructure | projetar `AbastecimentoRegistrado` |
| `projetarmanutencao` | application, infrastructure | projetar `ManutencaoRegistrada` |
| `consultardashboard` | api, application, infrastructure | painel de gastos e próximas manutenções |
| `consultarservico` | api, application, infrastructure | informações do serviço |

Pacotes técnicos: `servicebus/infrastructure` (consumidor do Service Bus), `infrastructure` (inserção idempotente), `config` e `observability`.

**Testes:** unitários (ex.: `ProjetarAbastecimentoTest`, `ConsultarDashboardTest`, `ServiceBusEventoListenerTest`), de integração com SQL Server real em Testcontainers (`ProjecaoAbastecimentoIdempotenteIT`, `DashboardSqlServerIT`) e de arquitetura com ArchUnit (`ArchitectureTest`: persistência só em `infrastructure`; código só nas camadas das fatias ou em pacotes técnicos conhecidos).

## Tecnologias

- Java 21, Spring Boot 4.1.1 (Web MVC, Security, OAuth2 Resource Server, JDBC, AMQP, Flyway, Actuator)
- SQL Server / Azure SQL (`mssql-jdbc`, `flyway-sqlserver`)
- Azure SDK BOM 1.3.9 (`azure-messaging-servicebus`, `azure-identity`); RabbitMQ local
- springdoc-openapi 3.2.0 (OpenAPI em `/v3/api-docs`)
- Testcontainers (SQL Server)
- Micrometer + Prometheus
- JUnit 5, ArchUnit 1.4.1
- Maven Wrapper (Maven 3.9.16)
- Docker (`eclipse-temurin:21`), GitHub Actions (testes em todo push; imagem no Docker Hub em tag `v*`)

## Como rodar localmente

```bash
./mvnw -B -ntp verify                                      # unitários, ArchUnit e ITs (Testcontainers; exige Docker)
docker build -t mateusblm/nexfleet-indicadores:local .     # imagem (porta 8080)
```

Variáveis de ambiente: `PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `IDENTIDADE_URL`, `MESSAGING_PROVIDER` (`rabbit` ou `servicebus`), `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `SERVICEBUS_FULLY_QUALIFIED_NAMESPACE`, `SERVICEBUS_TOPIC`, `SERVICEBUS_INDICADORES_SUBSCRIPTION` e `SPRING_PROFILES_ACTIVE=jwt-rsa`.

**Ambiente completo** (todos os serviços, SQL Server, MongoDB e RabbitMQ locais) sobe pelo repositório guarda-chuva [nexfleet](https://github.com/mateusblm/nexfleet), com os repositórios clonados lado a lado (`scripts/clonar-tudo.sh`):

```bash
scripts/iniciar-local.sh                                     # 1ª vez: gera o .env e a chave RSA local
docker compose -f compose.yaml -f compose.build.yaml up -d --build
scripts/seed-demo.sh                                         # dados de demonstração
```

Sem o override `compose.build.yaml`, o `docker compose up -d` usa as imagens publicadas no Docker Hub.

## URLs na nuvem

Indicadores não tem URL pública direta: é acessado só pelo APIM (via Gateway e BFF, ex.: `/api/v1/dashboard`). O OpenAPI aparece na Swagger UI abaixo, opção "Indicadores".

Imagem Docker: [`mateusblm/nexfleet-indicadores:v1.0.0`](https://hub.docker.com/r/mateusblm/nexfleet-indicadores)

| Recurso | Endereço |
|---|---|
| Frontend | https://white-mushroom-0cadda010.3.azurestaticapps.net |
| API (APIM) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net |
| Swagger UI (BFF, Frota, Operações, Identidade, Indicadores) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net/swagger-ui.html |
| Dados agregados (exige token) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net/aggregated-data |
| AsyncAPI dos eventos | https://white-mushroom-0cadda010.3.azurestaticapps.net/docs/eventos/ |
| Repositório guarda-chuva | https://github.com/mateusblm/nexfleet |

> **Partida a frio:** os Container Apps escalam a zero e o Azure SQL pausa sem uso. A primeira chamada após um período de inatividade pode levar de 1 a 2 minutos (ou responder 503/504); tente de novo em seguida.

## Equipe

- Vinícius dos Reis Góes
- Mateus Burlamaqui
- Lucas Roussenq
- Erik Carvalho

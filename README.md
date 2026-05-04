# Backend

REST API антифрод-системы на `Spring Boot 3` и `Java 21`.

Сервис:
- принимает транзакцию в JSON;
- считает риск по набору правил и статистических признаков;
- сохраняет транзакцию, решение и сработавшие триггеры;
- отдаёт результат проверки и историю операций.

<<<<<<< HEAD
## Что реализовано
=======
## Ручки
>>>>>>> 1c298e111ab0389d9e4ea729a90c2ac8b3245b4b

- `POST /api/v1/transactions/check` для проверки транзакции
- `GET /api/v1/transactions/{transactionId}` для получения решения по транзакции
- `GET /api/v1/transactions?page=0&size=10` для списка транзакций
<<<<<<< HEAD
- `GET /api/v1/rules` для списка правил
- `GET /api/v1/rules/{ruleCode}` для чтения одного правила
- `PUT /api/v1/rules/{ruleCode}` для изменения веса, порога и активности правила

Для совместимости также поддержаны `GET /transactions/{transactionId}` и `GET /transactions`.

## Модель скоринга

Скоринг сейчас построен на базовых антифрод-признаках и статистике по истории клиента.
=======
- `GET /api/v1/rules` для получения списка правил
- `GET /api/v1/rules/{ruleCode}` для чтения одного правила
- `PUT /api/v1/rules/{ruleCode}` для изменения веса, порога и активности правила


## Модель скоринга

Скоринг построен на базовых антифрод-признаках и статистике по истории клиента.
>>>>>>> 1c298e111ab0389d9e4ea729a90c2ac8b3245b4b

Используются:
- большие суммы: `HIGH_AMOUNT`, `VERY_HIGH_AMOUNT`
- новое устройство: `NEW_DEVICE`
- новая страна: `NEW_COUNTRY`
- ночная транзакция: `NIGHT_TRANSACTION`
- скользящее окно по частоте операций: `VELOCITY_24H`
- `Z-score` по сумме относительно истории клиента: `HIGH_Z_SCORE`
- агрегированный коэффициент риска по нескольким факторам: `HIGH_RISK_COEFFICIENT`
- индекс необычности транзакции: `HIGH_UNUSUALNESS`

### Как считается риск

1. История клиента загружается из предыдущих транзакций.
2. Считаются статистики:
- средняя сумма
- стандартное отклонение суммы
- число операций за последний час
- число операций за последние 24 часа

3. Затем вычисляются признаки:
- `zScore = |amount - mean| / stdDev`
- `riskCoefficient` как взвешенная сумма факторов:
  `z-score + частота + новое устройство + новая страна + ночь + относительная величина суммы`
- `unusualnessIndex` в диапазоне `0..100`

4. Если правила срабатывают, их веса суммируются в итоговый `riskScore`.

### Порог решений

- `ALLOW`: `riskScore < 40`
- `REVIEW`: `40 <= riskScore < 70`
- `DECLINE`: `riskScore >= 70`

## Форматы данных

<<<<<<< HEAD
### Запрос на проверку транзакции

```json
{
  "transactionId": "TRX-100001",
  "customerId": "CUST-001",
  "merchantId": "SHOP-001",
  "cardToken": "CARD-001",
  "amount": 14990.50,
  "currency": "RUB",
  "timestamp": "2026-04-16T18:45:00",
  "ipAddress": "91.142.33.10",
  "deviceId": "DEVICE-01",
  "country": "RU",
  "city": "Moscow",
  "paymentMethod": "BANK_CARD"
}
```

### Ответ по транзакции

```json
{
  "transactionId": "TRX-100001",
  "riskScore": 68,
  "decision": "DECLINE",
  "processedAt": "2026-04-16T18:30:01",
  "triggers": [
    {
      "ruleCode": "HIGH_AMOUNT",
      "scoreAdded": 25
    },
    {
      "ruleCode": "NEW_DEVICE",
      "scoreAdded": 15
    }
  ]
}
```

### Список транзакций

```json
{
  "page": 0,
  "size": 10,
  "totalElements": 2,
  "items": [
    {
      "transactionId": "TRX-20260416-0001",
      "customerId": "CUST-1001",
      "amount": 48900.00,
      "currency": "RUB",
      "timestamp": "2026-04-16T14:32:10",
      "decision": "DECLINE",
      "riskScore": 80
    }
  ]
}
```

### Изменение правила

```json
{
  "ruleCode": "HIGH_AMOUNT",
  "weight": 30,
  "threshold": 50000,
  "enabled": true
}
```

### Ответ по правилу

```json
{
  "ruleCode": "HIGH_AMOUNT",
  "ruleName": "Высокая сумма",
  "weight": 30,
  "threshold": 50000,
  "enabled": true,
  "updatedAt": "2026-04-16T15:05:00"
}
```

### Ошибка валидации

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "amount must be greater than zero",
  "field": "amount"
}
```
=======

>>>>>>> 1c298e111ab0389d9e4ea729a90c2ac8b3245b4b

## Ручки

### `POST /api/v1/transactions/check`

Проверяет транзакцию:
- валидирует входные данные
- проверяет дубликат `transactionId`
- сохраняет сырую транзакцию
- загружает профиль и историю клиента
- рассчитывает статистики и скоринг
- принимает решение `ALLOW`, `REVIEW` или `DECLINE`
- сохраняет решение и сработавшие правила

### `GET /api/v1/transactions/{transactionId}`

Возвращает решение по уже обработанной транзакции.

### `GET /api/v1/transactions?page=0&size=10`

Возвращает пагинированный список транзакций.

### `GET /api/v1/rules`

Возвращает список антифрод-правил и их текущие параметры.

### `GET /api/v1/rules/{ruleCode}`

Возвращает одно правило по коду.

### `PUT /api/v1/rules/{ruleCode}`

Позволяет менять:
- вес правила
- порог
- флаг активности

## Как запустить

### Требования

- `Java 21`
- `Maven 3.9+`

<<<<<<< HEAD
### Быстрый локальный запуск на H2

По умолчанию проект стартует на встроенной in-memory БД `H2`.

```bash
mvn spring-boot:run
```
=======

>>>>>>> 1c298e111ab0389d9e4ea729a90c2ac8b3245b4b

После запуска:
- API: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- JDBC URL: `jdbc:h2:mem:antifraud`
- user: `sa`
- password: пустой

<<<<<<< HEAD
### Запуск с PostgreSQL

```bash
export DB_URL=jdbc:postgresql://localhost:5432/antifraud
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export DB_DRIVER=org.postgresql.Driver
mvn spring-boot:run
```

Flyway сам создаст схему и стартовые правила.

После запуска с PostgreSQL Swagger тоже будет доступен:
- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`
=======
>>>>>>> 1c298e111ab0389d9e4ea729a90c2ac8b3245b4b

### Запуск в Docker Compose

Требуется установленный `Docker` с поддержкой `docker compose`.

```bash
docker compose up --build
```

После старта будут подняты:
- `postgres` на `localhost:5432`
- backend на `http://localhost:8080`

Backend внутри compose подключается к PostgreSQL автоматически через:
- `DB_URL=jdbc:postgresql://postgres:5432/antifraud`
- `DB_USERNAME=antifraud`
- `DB_PASSWORD=antifraud`

Остановка:

```bash
docker compose down
```

Остановка с удалением volume PostgreSQL:

```bash
docker compose down -v
```

<<<<<<< HEAD
### Сборка

```bash
mvn clean package
```

### Тесты

```bash
mvn clean test
```
=======

Разовый запуск генератора при уже поднятом backend:

```bash
docker compose run --rm generator --scenario all --count 35
```

`
>>>>>>> 1c298e111ab0389d9e4ea729a90c2ac8b3245b4b

## Примеры запросов

### Проверка транзакции

```bash
curl -X POST http://localhost:8080/api/v1/transactions/check \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TRX-100001",
    "customerId": "CUST-001",
    "merchantId": "SHOP-001",
    "cardToken": "CARD-001",
    "amount": 70000.00,
    "currency": "RUB",
    "timestamp": "2026-04-17T01:30:00",
    "ipAddress": "91.142.33.10",
    "deviceId": "DEVICE-02",
    "country": "RU",
    "city": "Moscow",
    "paymentMethod": "BANK_CARD"
  }'
```

### Получение списка правил

```bash
curl http://localhost:8080/api/v1/rules
```

### Изменение правила

```bash
curl -X PUT http://localhost:8080/api/v1/rules/HIGH_AMOUNT \
  -H "Content-Type: application/json" \
  -d '{
    "ruleCode": "HIGH_AMOUNT",
    "weight": 30,
    "threshold": 45000,
    "enabled": true
  }'
```

## База данных

<<<<<<< HEAD
Миграция находится в:

- `src/main/resources/db/migration/V1__init_schema.sql`

=======
>>>>>>> 1c298e111ab0389d9e4ea729a90c2ac8b3245b4b
Создаются таблицы:
- `transactions`
- `fraud_decisions`
- `rule_triggers`
- `rule_configs`
- `customer_profiles`

<<<<<<< HEAD
## Тесты

Интеграционные тесты API покрывают:
- успешную проверку транзакции с несколькими триггерами
- ошибку дубликата `transactionId`
- чтение транзакции
- пагинацию списка транзакций
- чтение и обновление правил
=======
>>>>>>> 1c298e111ab0389d9e4ea729a90c2ac8b3245b4b

## Как устроен код

Проект разделен на стандартные слои Spring Boot:
- `controller` принимает HTTP-запросы и отдает DTO наружу;
- `service` содержит основную бизнес-логику антифрода;
- `repository` работает с `JPA`-сущностями и БД;
- `entity` описывает таблицы `transactions`, `fraud_decisions`, `rule_triggers`, `rule_configs`, `customer_profiles`;
- `dto` описывает входные и выходные модели API.

Основной сценарий проверки транзакции проходит так:
- `TransactionController` принимает `POST /api/v1/transactions/check`;
- `TransactionService` валидирует уникальность `transactionId`, сохраняет транзакцию и собирает историю клиента;
- `FraudRuleEngine` считает `zScore`, `riskCoefficient`, `unusualnessIndex`, применяет правила и возвращает итоговый `riskScore`;
- затем сервис сохраняет решение, сработавшие триггеры и обновляет профиль клиента.

Правила хранятся в таблице `rule_configs`, поэтому их можно читать и менять через API без перекомпиляции приложения.

<<<<<<< HEAD
## Актуальный запуск проекта

Сейчас основной рекомендуемый способ запуска для разработки и демо: `Docker Compose`.

1. Убедиться, что установлен `Docker` и доступна команда `docker compose`.
2. Перейти в корень backend-проекта.
3. Выполнить:

```bash
docker compose up --build
```

После успешного старта доступны:
- backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- PostgreSQL: `localhost:5432`

Полезные команды:

```bash
docker compose up --build -d
docker compose logs -f
docker compose down
docker compose down -v
```

Локальный запуск без Docker тоже возможен:
- через `H2` по умолчанию: `mvn spring-boot:run`
- через внешний `PostgreSQL`: задать `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER` и затем выполнить `mvn spring-boot:run`

## Как следующему разработчику добавить Python-скрипт

Для MVP логично добавить отдельный Python-генератор транзакций, который будет отправлять запросы в уже существующий backend API.

Рекомендуемый подход:
- создать отдельную папку, например `tools/transaction-generator` или `scripts/python-generator`;
- использовать `Python 3.11+`;
- отправлять события через `POST /api/v1/transactions/check`;
- хранить настройки в `.env` или переменных окружения: `API_URL`, `REQUESTS_PER_SECOND`, `CUSTOMER_POOL_SIZE`, `COUNTRY_SET`, `DEVICE_ROTATION`;
- разделить генерацию на сценарии: нормальное поведение, крупные суммы, всплеск частоты, новое устройство, новая страна, ночные операции.

Минимальная структура Python-части:
- `generator.py` или `main.py` как точка входа;
- `scenarios.py` с наборами правил генерации;
- `client.py` с HTTP-вызовами в backend;
- `requirements.txt` или `pyproject.toml`.

Если потом понадобится запускать генератор вместе с backend, его можно добавить в `docker-compose.yml` как отдельный сервис `generator`, который зависит от `backend`.

## Как расширять работу с БД

База данных уже добавлена в проект и работает в двух режимах:
- встроенная `H2` для простого локального старта;
- `PostgreSQL` для основного dev/demo запуска и Docker Compose.

Если следующему разработчику нужно менять схему, делать это надо через новые `Flyway`-миграции:
- создавать файлы вида `src/main/resources/db/migration/V2__...sql`, `V3__...sql`;
- не редактировать старую миграцию `V1__init_schema.sql`, если проект уже используется на других окружениях.

Что обычно можно расширять дальше:
- добавлять индексы для ускорения выборок по `customer_id`, `event_time`, `transaction_id`;
- вводить отдельные справочники или таблицы для внешних сигналов;
- хранить сырые события генератора или результатов пакетной загрузки;
- добавлять audit-поля и технические статусы обработки.

## Что еще нужно реализовать для MVP

Текущий backend уже содержит базовую БД, миграцию схемы, REST API проверки транзакций, хранение решений и настройку антифрод-правил. Поэтому для MVP не требуется "добавить БД" в backend с нуля: `H2` и `PostgreSQL` уже поддержаны, а схема создается через `Flyway`.

Для полноценного MVP не хватает в первую очередь внешнего источника данных и сценария наполнения системы:
- генератора транзакций на `Python` для массовой отправки тестовых и псевдореальных событий в API;
- подготовленных сценариев генерации: нормальное поведение, всплеск частоты, смена устройства, смена страны, ночные операции, крупные суммы;
- отдельной инструкции по запуску backend вместе с `PostgreSQL` для демонстрационного стенда;
- набора тестовых данных и/или сидов для воспроизводимой демонстрации антифрод-срабатываний;
- базовых нефункциональных доработок для demo-MVP: логирование, обработка ошибок на уровне интеграции и минимальный мониторинг состояния сервиса.
=======
>>>>>>> 1c298e111ab0389d9e4ea729a90c2ac8b3245b4b

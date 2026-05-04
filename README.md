# Backend

REST API антифрод-системы на `Spring Boot 3` и `Java 21`.

Сервис:
- принимает транзакцию в JSON;
- считает риск по набору правил и статистических признаков;
- сохраняет транзакцию, решение и сработавшие триггеры;
- отдаёт результат проверки и историю операций.

## Ручки

- `POST /api/v1/transactions/check` для проверки транзакции
- `GET /api/v1/transactions/{transactionId}` для получения решения по транзакции
- `GET /api/v1/transactions?page=0&size=10` для списка транзакций
- `GET /api/v1/rules` для получения списка правил
- `GET /api/v1/rules/{ruleCode}` для чтения одного правила
- `PUT /api/v1/rules/{ruleCode}` для изменения веса, порога и активности правила


## Модель скоринга

Скоринг построен на базовых антифрод-признаках и статистике по истории клиента.

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



После запуска:
- API: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- JDBC URL: `jdbc:h2:mem:antifraud`
- user: `sa`
- password: пустой


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


Разовый запуск генератора при уже поднятом backend:

```bash
docker compose run --rm generator --scenario all --count 35
```

`

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

Создаются таблицы:
- `transactions`
- `fraud_decisions`
- `rule_triggers`
- `rule_configs`
- `customer_profiles`


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


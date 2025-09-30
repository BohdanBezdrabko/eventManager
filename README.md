# Sport Administration System — Backend

Легкий REST-бекенд для подій і постів. Підключаєш свою БД, запускаєш і користуєшся.

---

## Зміст
- [Огляд можливостей](#огляд-можливостей)
- [Технології](#технології)
- [Швидкий старт](#швидкий-старт)
- [Конфігурація](#конфігурація)
- [Ендпоїнти API](#ендпоїнти-api)
- [Планувальник публікацій](#планувальник-публікацій)
- [Статуси та довідники](#статуси-та-довідники)
- [Структура проєкту](#структура-проєкту)
- [Примітки](#примітки)
- [Ліцензія](#ліцензія)

---

## Огляд можливостей
- Події: створення, читання, фільтрація за категорією та тегами.
- Теги: Many-to-Many з подіями.
- Пости подій: CRUD, планування за `publishAt`.
- Шаблони постів: автогенерація відносно дати події (T–30/–14/–7/–1/0).
- Планувальник: автоматично публікує `SCHEDULED` пости, коли настає час.
- Готовність до інтеграції месенджерів (`TELEGRAM` на наступних етапах).

---

## Технології
- Java 17+ (рекомендовано 21)
- Spring Boot 3: Web, Validation, Data JPA, Security (JWT), Scheduling
- PostgreSQL 14+
- Flyway
- Maven
- Lombok

---

## Швидкий старт

### 1) Клонування
```bash
git clone <repo-url>
cd <repo>
```

### 2) База даних
Створи БД у PostgreSQL:
```sql
CREATE DATABASE sport_admin;
```
Міграції Flyway застосуються автоматично при старті.

### 3) Налаштування і запуск
Заповни параметри підключення до БД у `application.yml` або через змінні середовища і запусти:
```bash
mvn clean spring-boot:run
# або
mvn -DskipTests package && java -jar target/*.jar
```
Базовий URL: `http://localhost:8081/api/v1`

---

## Конфігурація

### Варіант через `application.yml`
```yaml
server:
  port: 8081

spring:
  application:
    name: SportAdministrationSystem
  jackson:
    serialization:
      WRITE_DATES_AS_TIMESTAMPS: false
  datasource:
    url: jdbc:postgresql://localhost:5432/sport_admin
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
  docker:
    compose:
      enabled: true

app:
  cors:
    allowed-origins:
      - http://localhost:5175
  dispatch:
    enabled: true
    batch-size: 20
    delay-ms: 60000

jwt:
  secret: "Y0urSup3r$3cretJWTK3yWith32+CharsMin"
  access-exp-min: 60
  refresh-exp-days: 14

logging:
  level:
    org.springframework.security: INFO
    com.example.sportadministrationsystem: INFO
```

### Перекриття змінними середовища
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sport_admin
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET="your-very-long-secret"
export APP_DISPATCH_ENABLED=true
export APP_DISPATCH_BATCH_SIZE=20
export APP_DISPATCH_DELAY_MS=60000
```

---

## Ендпоїнти API
Базовий префікс: `/api/v1`

### Події
- `GET /events`  
  Параметри: `category`, `tag` (опційно)  
  Приклад: `/events?category=SPORTS&tag=running`
- `GET /events/{id}`
- `GET /events/by-name/{name}`
- `GET /events/by-location/{location}`

### Пости події
- `POST /events/{eventId}/posts`
- `GET /events/{eventId}/posts`  
  Параметри: `status`, `audience`, `channel` (опційно)
- `PUT /events/{eventId}/posts/{postId}`
- `DELETE /events/{eventId}/posts/{postId}`
- `PATCH /events/{eventId}/posts/{postId}/status`

### Дерево події
- `GET /events/{id}/tree` — подія + згорнуті пости

### Приклади `curl`
```bash
# Список подій за категорією і тегом
curl 'http://localhost:8081/api/v1/events?category=SPORTS&tag=running'

# Створити запланований пост (INTERNAL)
curl -X POST 'http://localhost:8081/api/v1/events/1/posts'   -H 'Content-Type: application/json'   -d '{
    "title":"Анонс",
    "body":"Текст поста",
    "publishAt":"2025-10-01T09:00:00",
    "status":"SCHEDULED",
    "audience":"PUBLIC",
    "channel":"INTERNAL"
  }'
```

> Примітка: якщо ввімкнено безпеку з JWT, додай заголовок `Authorization: Bearer <token>`.

---

## Планувальник публікацій
- Працює за фіксованою затримкою (`app.dispatch.delay-ms`).
- Обирає пости зі `status=SCHEDULED` і `publishAt <= now`.
- Обробляє батчами (`app.dispatch.batch-size`).
- Канал `INTERNAL`: лог запису та позначення як `PUBLISHED`.
- Канал `TELEGRAM`: підключається на наступних етапах.

Налаштування:
```yaml
app:
  dispatch:
    enabled: true
    batch-size: 20
    delay-ms: 60000
```

---

## Статуси та довідники
- `Post.status`: `DRAFT`, `SCHEDULED`, `PUBLISHED`, `FAILED`, `CANCELED`
- `Post.audience`: `PUBLIC`, `SUBSCRIBERS`
- `Post.channel`: `INTERNAL`, `TELEGRAM`
- Категорії подій: enum або таблиця `event_categories`
- Теги: `Tag` + зв’язок Many-to-Many з `Event`

---

## Структура проєкту
```
src/main/java/com/example/sportadministrationsystem/
  controller/   # REST-контролери
  dto/          # DTO для API
  model/        # JPA сутності
  repository/   # Spring Data репозиторії
  service/      # Домашня логіка, генерація, планувальник
src/main/resources/
  application.yml
  db/migration  # міграції Flyway
```

---

## Примітки
- Зберігай `publishAt` і `startAt` послідовно: або в UTC, або з чітко заданою TZ.
- Для продакшена додай пул з’єднань і метрики.
- Логування налаштовується через `logging.level.*`.

---


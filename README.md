# Sport Administration System (eventManager)

---

## Огляд

**Sport Administration System (eventManager)** — застосунок для менеджменту подій та пов’язаних постів (анонси/нагадування/публікації).

Складається з:
- **Бекенду** на **Spring Boot 3** (REST API, планувальник публікацій, JWT).
- **Фронтенду** на **Vite** (SPA), що працює проти бекенду.
- **PostgreSQL** як основної БД.

За замовчуванням:
- Базовий префікс API: `http://localhost:8081/api/v1`
- Дев-фронтенд: `http://localhost:5175` (цей origin має бути дозволений у CORS)

---

## Фічі

- Події: створення/перегляд/пошук (`by-name`, `by-location`), категорії, теги.
- Пости: CRUD, статуси (`DRAFT`, `SCHEDULED`, `PUBLISHED`, `FAILED`, `CANCELED`), аудиторія (`PUBLIC`, `SUBSCRIBERS`), канал (`INTERNAL`, `TELEGRAM`).
- Планувальник: автоматично публікує заплановані пости за `publishAt`.
- JWT (за потреби), CORS, Flyway-міграції, профілі `dev/prod`.

---

## Архітектура (високий рівень)
Frontend (Vite SPA, :5175) <—HTTP—> Backend (Spring Boot, :8081) <—JDBC—> PostgreSQL (sport_admin)
---
# Структура репозиторію
```text
eventManager/
├─ SportAdministrationSystem/           # Backend (Java, Spring Boot)
│  ├─ src/main/java/...                 # Код бекенду (контролери, сервіси, ентіті тощо)
│  ├─ src/main/resources/
│  │  ├─ application.yml                # Головний конфіг Spring Boot (порт, БД, CORS, JWT, тощо)
│  │  └─ db/migration/                  # Flyway-міграції (V1__..., V2__..., ...), що змінюють схему БД
│  └─ pom.xml                           # Maven-конфігурація (залежності, плагіни, профілі)
└─ sportadmfront/                       # Frontend (Vite)
   ├─ src/                              # Вихідний код SPA (компоненти/сторінки/стилі)
   ├─ index.html                        # Вхідна HTML-сторінка для Vite
   ├─ package.json                      # Залежності та скрипти фронтенду
   └─ vite.config.*                     # Налаштування Vite (dev/build, alias-и тощо)
```

## Що потрібно мати

- **Сервер з базою даних** (або локальна машина)
- **PostgreSQL 14+**
- **Java 17+** (рекомендовано 21) та **Maven** — для бекенду
- **Node.js 18+** та **npm/pnpm/yarn** — для фронтенду
- Вільні порти: **8081** (бекенд) і **5175** (фронтенд dev)
- Домен/SSL для продакшну (рекомендовано)

---

## Швидкий старт (локально)

### 1) Клонування

```bash
git clone https://github.com/BohdanBezdrabko/eventManager.git
cd eventManager
```
# Запуск

## Backend (Spring Boot, Maven Wrapper)
```bash
cd SportAdministrationSystem
./mvnw clean spring-boot:run
```
## Frontend
```bash
cd sportadmfront
npm install
npm run dev
```

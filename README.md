🎬 Filmorate
REST API сервис для оценки и рекомендации фильмов

https://img.shields.io/badge/Java-21-orange.svg
https://img.shields.io/badge/Spring%2520Boot-3.2.4-green.svg
https://img.shields.io/badge/H2-Database-blue.svg
https://img.shields.io/badge/Docker-%E2%9C%93-blue.svg
https://img.shields.io/badge/Maven-3.x-red.svg

📖 Описание
Filmorate — это бэкенд-приложение для социальной сети, где пользователи могут:

✅ Ставить оценки фильмам от 1 до 10

✅ Писать отзывы и оценивать их полезность

✅ Добавлять друзей и находить общие фильмы

✅ Получать персонализированные рекомендации

✅ Отслеживать ленту событий

🛠️ Технологии
Категория	Технологии
Язык	Java 21
Фреймворк	Spring Boot 3.2.4, Spring MVC, Spring JDBC
База данных	H2 Database (PostgreSQL режим)
Сборка	Maven 3.x
Тестирование	JUnit 5, Mockito, Spring Boot Test
Дополнительно	Lombok, Hibernate Validator, SLF4J
🏗️ Архитектура
text
┌─────────────────────────────────────────────────────────────────┐
│                       Клиент (REST API)                        │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                     Controller Layer                           │
│  FilmController | UserController | ReviewController | Director │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                      Service Layer                             │
│     FilmService | UserService | ReviewService | EventService   │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                    Storage Layer                               │
│           JdbcTemplate + SimpleJdbcInsert                     │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                    Database (H2 / PostgreSQL)                  │
└─────────────────────────────────────────────────────────────────┘
🗄️ Схема базы данных
text
┌─────────────────────────────────────────────────────────────────────────────┐
│                              СХЕМА БАЗЫ ДАННЫХ                             │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌─────────────┐          ┌─────────────┐
    │    users    │          │    films    │
    ├─────────────┤          ├─────────────┤
    │ id (PK)     │◄─────────│ id (PK)     │
    │ email       │          │ name        │
    │ login       │          │ description │
    │ name        │          │ release_date│
    │ birthday    │          │ duration    │
    └─────────────┘          │ mpa_id (FK) │
          │                  └─────────────┘
          │                        │
          │                        │
    ┌─────┴─────┐            ┌─────┴─────┐
    │ friendship│            │    mpa    │
    ├───────────┤            ├───────────┤
    │ user_id   │            │ id (PK)   │
    │ friend_id │            │ name      │
    └───────────┘            └───────────┘
          │                        │
          │                  ┌─────┴─────┐
          │                  │  genres   │
          │                  ├───────────┤
          │                  │ id (PK)   │
          │                  │ name      │
    ┌─────┴─────┐            └───────────┘
    │   marks   │                  │
    ├───────────┤            ┌─────┴─────┐
    │ film_id   │◄───────────│ film_genre│
    │ user_id   │            ├───────────┤
    │ mark_value│            │ film_id   │
    │ created_at│            │ genre_id  │
    │ updated_at│            │ position  │
    └───────────┘            └───────────┘
          │
          │                  ┌─────────────┐
          │                  │  directors  │
          │                  ├─────────────┤
    ┌─────┴─────┐            │ id (PK)     │
    │  reviews  │            │ name        │
    ├───────────┤            └─────────────┘
    │ id (PK)   │                  │
    │ content   │            ┌─────┴─────┐
    │ is_positive│           │film_director│
    │ user_id   │            ├───────────┤
    │ film_id   │            │ film_id   │
    │ useful    │            │ director_id│
    │ created_at│            └───────────┘
    └───────────┘
          │
    ┌─────┴─────┐
    │review_ratings│
    ├───────────┤
    │ review_id │
    │ user_id   │
    │ is_like   │
    └───────────┘
          │
    ┌─────┴─────┐
    │  events   │
    ├───────────┤
    │ id (PK)   │
    │ timestamp │
    │ user_id   │
    │ event_type│
    │ operation │
    │ entity_id │
    └───────────┘
🚀 Запуск
Требования
Java 21

Maven 3.x

Локальный запуск
bash
# Клонирование репозитория
git clone https://github.com/AlexandrSanych/filmorate.git
cd filmorate

# Сборка проекта
mvn clean package

# Запуск приложения
java -jar target/filmorate-0.0.1-SNAPSHOT.jar
H2 Console (для разработки)
text
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./db/filmorate
Username: sa
Password: password
📚 Функциональные возможности
👤 Пользователи
Метод	Эндпоинт	Описание
POST	/users	Регистрация пользователя
PUT	/users	Обновление профиля
DELETE	/users/{userId}	Удаление пользователя
GET	/users	Все пользователи
GET	/users/{id}	Пользователь по ID
PUT	/users/{id}/friends/{friendId}	Добавить друга
DELETE	/users/{id}/friends/{friendId}	Удалить друга
GET	/users/{id}/friends	Список друзей
GET	/users/{id}/friends/common/{otherId}	Общие друзья
GET	/users/{id}/recommendations	Рекомендации фильмов
GET	/users/{userId}/feed	Лента событий
🎬 Фильмы
Метод	Эндпоинт	Описание
POST	/films	Добавление фильма
PUT	/films	Обновление фильма
DELETE	/films/{filmId}	Удаление фильма
GET	/films	Все фильмы
GET	/films/{id}	Фильм по ID
GET	/films/popular?count=&genreId=&year=	Популярные фильмы
GET	/films/common?userId=&friendId=	Общие фильмы
GET	/films/search?query=&by=	Поиск фильмов
GET	/films/director/{directorId}?sortBy=	Фильмы режиссёра
⭐ Оценки
Метод	Эндпоинт	Описание
PUT	/films/{id}/mark/{userId}?mark=	Поставить оценку (1-10)
PUT	/films/{id}/mark/{userId}/update?mark=	Обновить оценку
DELETE	/films/{id}/mark/{userId}	Удалить оценку
💬 Отзывы
Метод	Эндпоинт	Описание
POST	/reviews	Создать отзыв
PUT	/reviews	Обновить отзыв (только автор)
DELETE	/reviews/{id}	Удалить отзыв
GET	/reviews/{id}	Отзыв по ID
GET	/reviews?filmId=&count=	Список отзывов
PUT	/reviews/{id}/like/{userId}	Лайк отзыву
PUT	/reviews/{id}/dislike/{userId}	Дизлайк отзыву
DELETE	/reviews/{id}/like/{userId}	Удалить лайк
DELETE	/reviews/{id}/dislike/{userId}	Удалить дизлайк
🎥 Режиссёры
Метод	Эндпоинт	Описание
POST	/directors	Создать режиссёра
PUT	/directors	Обновить режиссёра
DELETE	/directors/{id}	Удалить режиссёра
GET	/directors	Все режиссёры
GET	/directors/{id}	Режиссёр по ID
🎭 Жанры
Метод	Эндпоинт	Описание
GET	/genres	Все жанры
GET	/genres/{id}	Жанр по ID
🏷️ Рейтинги MPA
Метод	Эндпоинт	Описание
GET	/mpa	Все рейтинги
GET	/mpa/{id}	Рейтинг по ID
🧩 Паттерны и подходы
Паттерн	Использование
Storage Pattern	FilmStorage, UserStorage и другие для работы с БД
Service Layer	Бизнес-логика вынесена в сервисы
Global Exception Handler	@RestControllerAdvice для обработки ошибок
Dependency Injection	Внедрение зависимостей через конструкторы
Builder Pattern	Lombok @Builder для создания объектов
Validation	@Valid, @NotNull, @Size, @Min, @Max
Transactional	@Transactional для управления транзакциями
🔒 Обработка ошибок
Исключение	Статус	Описание
NotFoundException	404	Объект не найден
ValidationException	400	Ошибка валидации данных
DuplicateException	409	Конфликт дублирования
MethodArgumentNotValidException	400	Ошибка валидации DTO
ConstraintViolationException	400	Ошибка ограничений
Throwable	500	Внутренняя ошибка сервера
📝 Пример ответа об ошибке
json
{
  "error": "Объект не найден",
  "description": "Фильм с id=999 не найден"
}
🧪 Тестирование
Покрытие тестами: ~80%

✅ Unit-тесты (JUnit 5 + Mockito)

✅ Integration-тесты (Spring Boot Test)

✅ Repository-тесты (JdbcTemplate)

📁 Структура проекта
text
filmorate/
├── src/
│   ├── main/
│   │   ├── java/ru/yandex/practicum/filmorate/
│   │   │   ├── controller/          # REST контроллеры
│   │   │   ├── service/             # Бизнес-логика
│   │   │   ├── storage/             # Хранилища (DAO)
│   │   │   │   ├── db/              # JdbcTemplate реализации
│   │   │   │   └── mapper/          # RowMapper'ы
│   │   │   ├── model/               # Модели данных
│   │   │   ├── exception/           # Обработка ошибок
│   │   │   └── validation/          # Валидаторы
│   │   └── resources/
│   │       ├── schema.sql           # Схема БД
│   │       ├── data.sql             # Начальные данные
│   │       └── application.properties
│   └── test/                        # Тесты
└── pom.xml
🛠️ Особенности реализации
⭐ Оценки (marks) вместо лайков
Пользователи ставят оценки от 1 до 10

Положительной считается оценка >= 6

Средняя оценка фильма вычисляется автоматически

События: MARK с операциями ADD, UPDATE, REMOVE

📰 Лента событий (feed)
Отслеживает: добавление/обновление/удаление отзывов, друзей, оценок

Поддерживаемые типы: REVIEW, FRIEND, MARK

Возвращается в хронологическом порядке (сначала новые)

🔥 Рекомендации
Основаны на корреляции оценок между пользователями

Рекомендуются фильмы с оценкой >= 6 от похожего пользователя

Требуется минимум 2 общих фильма для расчёта корреляции

📌 Планы по развитию
□ Добавить кеширование (Redis)
□ Реализовать пагинацию для всех эндпоинтов
□ Добавить OpenAPI/Swagger документацию
□ Внедрить Spring Security и JWT аутентификацию
□ Перейти на PostgreSQL в production
🏆 Навыки
Java 21: Stream API, Optional, Records

Spring Boot: MVC, JDBC Template, Validation, AOP

H2 Database: Оптимизация запросов, индексы, транзакции

JUnit 5: Написание тестов, Mockito

Maven: Управление зависимостями

Git: Работа с ветками, Pull Request'ы

REST API: Правильное проектирование эндпоинтов

Clean Code: Чистый и поддерживаемый код

📫 Контакты
Telegram: @AlexandrSanychP

GitHub: AlexandrSanych

⭐️ Если вам понравился проект — поставьте звезду!

🎬 Filmorate
REST API сервис для оценки и рекомендации фильмов

https://img.shields.io/badge/Java-21-orange.svg
https://img.shields.io/badge/Spring%2520Boot-3.2.4-green.svg
https://img.shields.io/badge/H2-Database-blue.svg
https://img.shields.io/badge/Maven-3.x-red.svg

📖 Описание
Filmorate — бэкенд-приложение для социальной сети, где пользователи могут:

☑ Ставить оценки фильмам от 1 до 10
☑ Писать отзывы и оценивать их полезность
☑ Добавлять друзей и находить общие фильмы
☑ Получать персонализированные рекомендации
☑ Отслеживать ленту событий
🛠️ Технологии
Категория	Технологии
Язык	Java 21
Фреймворк	Spring Boot 3.2.4, Spring MVC, Spring JDBC
База данных	H2 Database
Сборка	Maven 3.x
Тестирование	JUnit 5, Mockito
Дополнительно	Lombok, Hibernate Validator, SLF4J
🏗️ Архитектура
text
┌─────────────────────────────────────────────────────────┐
│                    REST Controllers                    │
│  FilmController / UserController / ReviewController   │
├─────────────────────────────────────────────────────────┤
│                    Service Layer                       │
│  FilmService / UserService / ReviewService / Event     │
├─────────────────────────────────────────────────────────┤
│                    Storage Layer                       │
│            JdbcTemplate / SimpleJdbcInsert            │
├─────────────────────────────────────────────────────────┤
│                    Database (H2)                      │
└─────────────────────────────────────────────────────────┘
🗄️ Схема базы данных
https://diagram%2520Filmorate.png

🚀 Запуск
Требования
Java 21

Maven 3.x

Локальный запуск
bash
git clone https://github.com/AlexandrSanych/filmorate.git
cd filmorate
mvn clean package
java -jar target/filmorate-0.0.1-SNAPSHOT.jar
H2 Console
text
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./db/filmorate
Username: sa
Password: password
📚 API Эндпоинты
👤 Пользователи (/users)
Метод	Эндпоинт	Описание
POST	/users	Создание пользователя
PUT	/users	Обновление пользователя
DELETE	/users/{userId}	Удаление пользователя
GET	/users	Все пользователи
GET	/users/{id}	Пользователь по ID
PUT	/users/{id}/friends/{friendId}	Добавить друга
DELETE	/users/{id}/friends/{friendId}	Удалить друга
GET	/users/{id}/friends	Список друзей
GET	/users/{id}/friends/common/{otherId}	Общие друзья
GET	/users/{id}/recommendations	Рекомендации
GET	/users/{userId}/feed	Лента событий
🎬 Фильмы (/films)
Метод	Эндпоинт	Описание
POST	/films	Создание фильма
PUT	/films	Обновление фильма
DELETE	/films/{filmId}	Удаление фильма
GET	/films	Все фильмы
GET	/films/{id}	Фильм по ID
PUT	/films/{id}/mark/{userId}?mark=	Поставить оценку (1-10)
PUT	/films/{id}/mark/{userId}/update?mark=	Обновить оценку
DELETE	/films/{id}/mark/{userId}	Удалить оценку
GET	/films/popular?count=&genreId=&year=	Популярные фильмы
GET	/films/common?userId=&friendId=	Общие фильмы
GET	/films/search?query=&by=	Поиск фильмов
GET	/films/director/{directorId}?sortBy=	Фильмы режиссёра
💬 Отзывы (/reviews)
Метод	Эндпоинт	Описание
POST	/reviews	Создание отзыва
PUT	/reviews	Обновление отзыва
DELETE	/reviews/{id}	Удаление отзыва
GET	/reviews/{id}	Отзыв по ID
GET	/reviews?filmId=&count=	Список отзывов
PUT	/reviews/{id}/like/{userId}	Лайк отзыву
PUT	/reviews/{id}/dislike/{userId}	Дизлайк отзыву
DELETE	/reviews/{id}/like/{userId}	Удалить лайк
DELETE	/reviews/{id}/dislike/{userId}	Удалить дизлайк
🎥 Режиссёры (/directors)
Метод	Эндпоинт	Описание
POST	/directors	Создание режиссёра
PUT	/directors	Обновление режиссёра
DELETE	/directors/{id}	Удаление режиссёра
GET	/directors	Все режиссёры
GET	/directors/{id}	Режиссёр по ID
🎭 Жанры (/genres)
Метод	Эндпоинт	Описание
GET	/genres	Все жанры
GET	/genres/{id}	Жанр по ID
🏷️ Рейтинги MPA (/mpa)
Метод	Эндпоинт	Описание
GET	/mpa	Все рейтинги
GET	/mpa/{id}	Рейтинг по ID
🧩 Паттерны и подходы
Паттерн	Использование
Storage Pattern	FilmStorage, UserStorage для работы с БД
Service Layer	Бизнес-логика в сервисах
Global Exception Handler	@RestControllerAdvice
Dependency Injection	Внедрение через конструкторы
Builder Pattern	Lombok @Builder
Validation	@Valid, @NotNull, @Size
🔒 Обработка ошибок
Исключение	Статус	Описание
NotFoundException	404	Объект не найден
ValidationException	400	Ошибка валидации
DuplicateException	409	Конфликт дублирования
Throwable	500	Внутренняя ошибка
Пример ответа:

json
{
  "error": "Объект не найден",
  "description": "Фильм с id=999 не найден"
}
📁 Структура проекта
text
filmorate/
├── src/main/java/.../filmorate/
│   ├── controller/          # REST контроллеры
│   ├── service/             # Бизнес-логика
│   ├── storage/             # Хранилища (DAO)
│   │   ├── db/              # JdbcTemplate реализации
│   │   └── mapper/          # RowMapper'ы
│   ├── model/               # Модели данных
│   ├── exception/           # Обработка ошибок
│   └── validation/          # Валидаторы
├── src/main/resources/
│   ├── schema.sql           # Схема БД
│   ├── data.sql             # Начальные данные
│   └── application.properties
├── pom.xml
└── README.md
🛠️ Особенности реализации
⭐ Оценки (marks) вместо лайков
Оценки от 1 до 10

Положительная оценка >= 6

События: MARK с операциями ADD, UPDATE, REMOVE

📰 Лента событий
Типы: REVIEW, FRIEND, MARK

Операции: ADD, UPDATE, REMOVE

Хронологический порядок (сначала новые)

🔥 Рекомендации
На основе корреляции оценок

Минимум 2 общих фильма

📌 Планы по развитию
□ Добавить кеширование (Redis)
□ Реализовать пагинацию
□ Добавить OpenAPI/Swagger
□ Внедрить Spring Security
□ Перейти на PostgreSQL
🏆 Навыки
Java 21: Stream API, Optional

Spring Boot: MVC, JDBC Template, Validation

H2: Индексы, транзакции

JUnit 5: Unit / Integration тесты

Maven: Управление зависимостями

REST API: Проектирование эндпоинтов

Clean Code: Чистый код

📫 Контакты
Telegram: @AlexandrSanychP

GitHub: AlexandrSanych

⭐️ Если вам понравился проект — поставьте звезду!

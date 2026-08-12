# 🎬 Filmorate — Социальная сеть для оценки фильмов

Проект Filmorate — это бэкенд-приложение для социальной сети, где пользователи могут:
- оценивать фильмы по шкале от 1 до 10,
- писать отзывы и оценивать их полезность,
- добавлять друзей,
- получать рекомендации на основе вкусов других пользователей,
- отслеживать ленту событий.

## 📦 Технологии

- **Java 21**
- **Spring Boot 3.2.4**
- **Spring MVC**
- **Spring JDBC / JdbcTemplate**
- **H2 Database** (встроенная БД для разработки и тестов)
- **Lombok**
- **Maven**

---

## 🗄️ Структура базы данных

### 📊 ER-диаграмма

![ER-диаграмма Filmorate](diagram%20Filmorate.png)

---

### 📋 Описание таблиц

#### 👤 users — пользователи

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY | Уникальный идентификатор пользователя |
| email | VARCHAR(255) | NOT NULL UNIQUE | Электронная почта |
| login | VARCHAR(100) | NOT NULL UNIQUE | Логин пользователя |
| name | VARCHAR(100) | | Имя для отображения (если не задано, используется login) |
| birthday | DATE | | Дата рождения |

#### 🎬 films — фильмы

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY | Уникальный идентификатор фильма |
| name | VARCHAR(255) | NOT NULL | Название фильма |
| description | VARCHAR(200) | | Описание фильма (максимум 200 символов) |
| release_date | DATE | CHECK (release_date >= '1895-12-28') | Дата выхода (не раньше рождения кино) |
| duration | INTEGER | CHECK (duration > 0) | Длительность в минутах |
| mpa_id | INTEGER | FOREIGN KEY REFERENCES mpa(id) | ID рейтинга MPA |

#### 🏷️ mpa — рейтинги MPA

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | INTEGER | PRIMARY KEY | ID рейтинга |
| name | VARCHAR(10) | NOT NULL UNIQUE | Код рейтинга (G, PG, PG-13, R, NC-17) |

#### 🎭 genres — жанры

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | INTEGER | PRIMARY KEY | ID жанра |
| name | VARCHAR(100) | NOT NULL UNIQUE | Название жанра |

#### 🎬 directors — режиссёры

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY | ID режиссёра |
| name | VARCHAR(255) | NOT NULL UNIQUE | Имя режиссёра |

#### ⭐ marks — оценки фильмов (вместо лайков)

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| film_id | BIGINT | PRIMARY KEY, FOREIGN KEY REFERENCES films(id) | ID фильма |
| user_id | BIGINT | PRIMARY KEY, FOREIGN KEY REFERENCES users(id) | ID пользователя |
| mark_value | INTEGER | NOT NULL CHECK (1-10) | Оценка от 1 до 10 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Дата создания оценки |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Дата обновления оценки |

#### 💬 reviews — отзывы

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY | ID отзыва |
| content | VARCHAR(1000) | NOT NULL | Содержание отзыва |
| is_positive | BOOLEAN | NOT NULL | Положительный (true) или отрицательный (false) |
| user_id | BIGINT | FOREIGN KEY REFERENCES users(id) | Автор отзыва |
| film_id | BIGINT | FOREIGN KEY REFERENCES films(id) | Фильм, на который написан отзыв |
| useful | INTEGER | DEFAULT 0 | Рейтинг полезности (лайки - дизлайки) |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Дата создания отзыва |

#### 👍 review_ratings — оценки полезности отзывов

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| review_id | BIGINT | PRIMARY KEY, FOREIGN KEY REFERENCES reviews(id) | ID отзыва |
| user_id | BIGINT | PRIMARY KEY, FOREIGN KEY REFERENCES users(id) | ID пользователя |
| is_like | BOOLEAN | NOT NULL | true = лайк, false = дизлайк |

#### 🔗 film_genre — связь фильмов и жанров

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| film_id | BIGINT | PRIMARY KEY, FOREIGN KEY REFERENCES films(id) | ID фильма |
| genre_id | INTEGER | PRIMARY KEY, FOREIGN KEY REFERENCES genres(id) | ID жанра |
| position | INTEGER | NOT NULL | Порядок жанра (для сохранения порядка) |

#### 🔗 film_director — связь фильмов и режиссёров

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| film_id | BIGINT | PRIMARY KEY, FOREIGN KEY REFERENCES films(id) | ID фильма |
| director_id | BIGINT | PRIMARY KEY, FOREIGN KEY REFERENCES directors(id) | ID режиссёра |

#### 🤝 friendship — друзья пользователей

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| user_id | BIGINT | PRIMARY KEY, FOREIGN KEY REFERENCES users(id) | ID пользователя, который добавляет в друзья |
| friend_id | BIGINT | PRIMARY KEY, FOREIGN KEY REFERENCES users(id) | ID пользователя, которого добавляют в друзья |

#### 📰 events — лента событий

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | BIGINT | PRIMARY KEY | ID события |
| timestamp | BIGINT | NOT NULL | Время события в миллисекундах |
| user_id | BIGINT | FOREIGN KEY REFERENCES users(id) | ID пользователя |
| event_type | VARCHAR(20) | NOT NULL | Тип события (LIKE, REVIEW, FRIEND, MARK) |
| operation | VARCHAR(20) | NOT NULL | Операция (REMOVE, ADD, UPDATE) |
| entity_id | BIGINT | NOT NULL | ID сущности (фильма, отзыва, друга) |

---

## 🔗 Связи между таблицами

| От | К | Тип связи | Описание |
|----|----|-----------|----------|
| films | mpa | Многие к одному | Много фильмов → один рейтинг |
| film_genre | films | Многие к одному | Много жанров → один фильм |
| film_genre | genres | Многие к одному | Много фильмов → один жанр |
| film_director | films | Многие к одному | Много режиссёров → один фильм |
| film_director | directors | Многие к одному | Много фильмов → один режиссёр |
| marks | users | Многие к одному | Много оценок → один пользователь |
| marks | films | Многие к одному | Много оценок → один фильм |
| reviews | users | Многие к одному | Много отзывов → один пользователь |
| reviews | films | Многие к одному | Много отзывов → один фильм |
| review_ratings | reviews | Многие к одному | Много оценок → один отзыв |
| review_ratings | users | Многие к одному | Много оценок → один пользователь |
| friendship | users (user_id) | Многие к одному | Много связей → один пользователь-инициатор |
| friendship | users (friend_id) | Многие к одному | Много связей → один пользователь-получатель |
| events | users | Многие к одному | Много событий → один пользователь |

---

## 🚀 Индексы для оптимизации запросов

```sql
-- Индексы для таблицы users
CREATE INDEX idx_users_login ON users(login);
CREATE INDEX idx_users_email ON users(email);

-- Индексы для таблицы films
CREATE INDEX idx_films_release_date ON films(release_date);
CREATE INDEX idx_films_mpa_id ON films(mpa_id);
CREATE INDEX idx_films_name ON films(name);

-- Индексы для таблицы marks
CREATE INDEX idx_marks_film_id ON marks(film_id);
CREATE INDEX idx_marks_user_id ON marks(user_id);
CREATE INDEX idx_marks_value ON marks(mark_value);

-- Индексы для таблицы reviews
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_film_id ON reviews(film_id);
CREATE INDEX idx_reviews_useful ON reviews(useful DESC);

-- Индексы для таблицы friendship
CREATE INDEX idx_friendship_user_id ON friendship(user_id);
CREATE INDEX idx_friendship_friend_id ON friendship(friend_id);

-- Индексы для таблицы events
CREATE INDEX idx_events_user_id ON events(user_id);
CREATE INDEX idx_events_timestamp ON events(timestamp);

-- Индексы для связующих таблиц
CREATE INDEX idx_film_genre_film_id ON film_genre(film_id);
CREATE INDEX idx_film_genre_genre_id ON film_genre(genre_id);
CREATE INDEX idx_film_director_film_id ON film_director(film_id);
CREATE INDEX idx_film_director_director_id ON film_director(director_id);

📝 Примеры SQL-запросов

1. Топ-10 популярных фильмов (по средней оценке)

sql
SELECT 
    f.id,
    f.name,
    f.description,
    f.release_date,
    f.duration,
    m.name AS mpa_rating,
    COALESCE(AVG(mrk.mark_value), 0) AS avg_rating,
    COUNT(mrk.user_id) AS votes_count
FROM films f
LEFT JOIN mpa m ON f.mpa_id = m.id
LEFT JOIN marks mrk ON f.id = mrk.film_id
GROUP BY f.id, f.name, f.description, f.release_date, f.duration, m.name
ORDER BY avg_rating DESC, votes_count DESC
LIMIT 10;

2. Получение друзей пользователя

sql
SELECT u.* 
FROM users u
JOIN friendship f ON u.id = f.friend_id
WHERE f.user_id = 1;

3. Общие фильмы двух пользователей (только с положительными оценками)

sql
SELECT f.*, COALESCE(AVG(mrk.mark_value), 0) AS avg_rating
FROM films f
LEFT JOIN marks mrk ON f.id = mrk.film_id
WHERE f.id IN (
    SELECT mrk1.film_id FROM marks mrk1 WHERE mrk1.user_id = 1 AND mrk1.mark_value >= 6
    INTERSECT
    SELECT mrk2.film_id FROM marks mrk2 WHERE mrk2.user_id = 2 AND mrk2.mark_value >= 6
)
GROUP BY f.id
ORDER BY avg_rating DESC;

4. Фильмы с жанрами и режиссёрами

sql
SELECT 
    f.id,
    f.name,
    STRING_AGG(DISTINCT g.name, ', ') AS genres,
    STRING_AGG(DISTINCT d.name, ', ') AS directors,
    m.name AS mpa_rating
FROM films f
LEFT JOIN film_genre fg ON f.id = fg.film_id
LEFT JOIN genres g ON fg.genre_id = g.id
LEFT JOIN film_director fd ON f.id = fd.film_id
LEFT JOIN directors d ON fd.director_id = d.id
LEFT JOIN mpa m ON f.mpa_id = m.id
GROUP BY f.id, f.name, m.name
ORDER BY f.id;

5. Лента событий пользователя

sql
SELECT * FROM events 
WHERE user_id = 1 
ORDER BY timestamp DESC;

6. Рекомендации фильмов

sql
-- Находим пользователя с наиболее похожими оценками
SELECT mrk2.user_id, CORR(mrk1.mark_value, mrk2.mark_value) AS rating_correlation
FROM marks mrk1
JOIN marks mrk2 ON mrk1.film_id = mrk2.film_id
WHERE mrk1.user_id = 1 AND mrk2.user_id != 1
GROUP BY mrk2.user_id
ORDER BY rating_correlation DESC
LIMIT 1;

-- Рекомендуем фильмы, которые похожий пользователь оценил положительно (>=6)
SELECT f.*
FROM films f
WHERE f.id IN (
    SELECT mrk2.film_id
    FROM marks mrk2
    WHERE mrk2.user_id = ? 
      AND mrk2.mark_value >= 6
      AND mrk2.film_id NOT IN (
          SELECT mrk1.film_id FROM marks mrk1 WHERE mrk1.user_id = 1
      )
);
🚀 Запуск приложения
Требования
Java 21

Maven 3.8+

Установка и запуск
bash
# Клонирование репозитория
git clone https://github.com/your-repo/filmorate.git
cd filmorate

# Сборка проекта
mvn clean package

# Запуск приложения
java -jar target/filmorate-0.0.1-SNAPSHOT.jar
H2 Console
После запуска приложения H2 Console доступна по адресу:

text
http://localhost:8080/h2-console
Параметры подключения:

JDBC URL: jdbc:h2:file:./db/filmorate

Username: sa

Password: password

📚 API Эндпоинты
Фильмы (/films)
Метод	Эндпоинт	Описание
POST	/films	Создание фильма
PUT	/films	Обновление фильма
DELETE	/films/{filmId}	Удаление фильма
GET	/films	Получение всех фильмов
GET	/films/{id}	Получение фильма по ID
PUT	/films/{id}/mark/{userId}?mark=	Добавление оценки фильму
PUT	/films/{id}/mark/{userId}/update?mark=	Обновление оценки
DELETE	/films/{id}/mark/{userId}	Удаление оценки
GET	/films/popular?count=&genreId=&year=	Популярные фильмы
GET	/films/common?userId=&friendId=	Общие фильмы с другом
GET	/films/search?query=&by=	Поиск фильмов
GET	/films/director/{directorId}?sortBy=	Фильмы режиссёра
Пользователи (/users)
Метод	Эндпоинт	Описание
POST	/users	Создание пользователя
PUT	/users	Обновление пользователя
DELETE	/users/{userId}	Удаление пользователя
GET	/users	Получение всех пользователей
GET	/users/{id}	Получение пользователя по ID
PUT	/users/{id}/friends/{friendId}	Добавление в друзья
DELETE	/users/{id}/friends/{friendId}	Удаление из друзей
GET	/users/{id}/friends	Список друзей
GET	/users/{id}/friends/common/{otherId}	Общие друзья
GET	/users/{id}/recommendations	Рекомендации фильмов
GET	/users/{userId}/feed	Лента событий
Отзывы (/reviews)
Метод	Эндпоинт	Описание
POST	/reviews	Создание отзыва
PUT	/reviews	Обновление отзыва
DELETE	/reviews/{id}	Удаление отзыва
GET	/reviews/{id}	Получение отзыва по ID
GET	/reviews?filmId=&count=	Получение отзывов
PUT	/reviews/{id}/like/{userId}	Лайк отзыву
PUT	/reviews/{id}/dislike/{userId}	Дизлайк отзыву
DELETE	/reviews/{id}/like/{userId}	Удаление лайка
DELETE	/reviews/{id}/dislike/{userId}	Удаление дизлайка
Режиссёры (/directors)
Метод	Эндпоинт	Описание
GET	/directors	Получение всех режиссёров
GET	/directors/{id}	Получение режиссёра по ID
POST	/directors	Создание режиссёра
PUT	/directors	Обновление режиссёра
DELETE	/directors/{id}	Удаление режиссёра
Жанры (/genres)
Метод	Эндпоинт	Описание
GET	/genres	Получение всех жанров
GET	/genres/{id}	Получение жанра по ID
Рейтинги MPA (/mpa)
Метод	Эндпоинт	Описание
GET	/mpa	Получение всех рейтингов
GET	/mpa/{id}	Получение рейтинга по ID


📁 Структура проекта
text
src/
├── main/
│   ├── java/ru/yandex/practicum/filmorate/
│   │   ├── controller/     # REST контроллеры
│   │   ├── model/          # Модели данных
│   │   ├── service/        # Бизнес-логика
│   │   ├── storage/        # Хранилища (DAO)
│   │   ├── exception/      # Обработка ошибок
│   │   └── validation/     # Валидаторы
│   └── resources/
│       ├── schema.sql      # Схема БД
│       ├── data.sql        # Начальные данные
│       └── application.properties
└── test/                   # Тесты
👥 Авторы
SANSAN — Initial work

📄 Лицензия
Этот проект является учебным и не предназначен для коммерческого использования.
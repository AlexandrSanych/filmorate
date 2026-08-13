🎬 Filmorate — Социальная сеть для оценки фильмов
Проект Filmorate — это бэкенд-приложение для социальной сети, где пользователи могут:

оценивать фильмы по шкале от 1 до 10

писать отзывы и оценивать их полезность

добавлять друзей

получать рекомендации на основе вкусов других пользователей

отслеживать ленту событий

📦 Технологии
Java 21

Spring Boot 3.2.4

Spring MVC

Spring JDBC / JdbcTemplate

H2 Database

Lombok

Maven

🗄️ Структура базы данных
📊 ER-диаграмма
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
📋 Описание таблиц
👤 Таблица users — пользователи
Поле	Тип	Ограничения	Описание
id	BIGINT	PRIMARY KEY	Уникальный идентификатор
email	VARCHAR(255)	NOT NULL, UNIQUE	Электронная почта
login	VARCHAR(100)	NOT NULL, UNIQUE	Логин пользователя
name	VARCHAR(100)	-	Имя для отображения
birthday	DATE	-	Дата рождения
🎬 Таблица films — фильмы
Поле	Тип	Ограничения	Описание
id	BIGINT	PRIMARY KEY	Уникальный идентификатор
name	VARCHAR(255)	NOT NULL	Название фильма
description	VARCHAR(200)	-	Описание (макс. 200 символов)
release_date	DATE	CHECK (>= '1895-12-28')	Дата выхода
duration	INTEGER	CHECK (> 0)	Длительность в минутах
mpa_id	INTEGER	FOREIGN KEY → mpa(id)	ID рейтинга MPA
🏷️ Таблица mpa — рейтинги MPA
Поле	Тип	Ограничения	Описание
id	INTEGER	PRIMARY KEY	ID рейтинга
name	VARCHAR(10)	NOT NULL, UNIQUE	Код рейтинга
Возможные значения: G, PG, PG-13, R, NC-17

🎭 Таблица genres — жанры
Поле	Тип	Ограничения	Описание
id	INTEGER	PRIMARY KEY	ID жанра
name	VARCHAR(100)	NOT NULL, UNIQUE	Название жанра
🎬 Таблица directors — режиссёры
Поле	Тип	Ограничения	Описание
id	BIGINT	PRIMARY KEY	ID режиссёра
name	VARCHAR(255)	NOT NULL, UNIQUE	Имя режиссёра
⭐ Таблица marks — оценки фильмов
Поле	Тип	Ограничения	Описание
film_id	BIGINT	PRIMARY KEY, FOREIGN KEY → films(id)	ID фильма
user_id	BIGINT	PRIMARY KEY, FOREIGN KEY → users(id)	ID пользователя
mark_value	INTEGER	NOT NULL, CHECK (1-10)	Оценка от 1 до 10
created_at	TIMESTAMP	DEFAULT CURRENT_TIMESTAMP	Дата создания
updated_at	TIMESTAMP	DEFAULT CURRENT_TIMESTAMP	Дата обновления
💬 Таблица reviews — отзывы
Поле	Тип	Ограничения	Описание
id	BIGINT	PRIMARY KEY	ID отзыва
content	VARCHAR(1000)	NOT NULL	Содержание отзыва
is_positive	BOOLEAN	NOT NULL	true=положительный, false=отрицательный
user_id	BIGINT	FOREIGN KEY → users(id)	Автор отзыва
film_id	BIGINT	FOREIGN KEY → films(id)	Фильм
useful	INTEGER	DEFAULT 0	Рейтинг полезности
created_at	TIMESTAMP	DEFAULT CURRENT_TIMESTAMP	Дата создания
👍 Таблица review_ratings — оценки полезности отзывов
Поле	Тип	Ограничения	Описание
review_id	BIGINT	PRIMARY KEY, FOREIGN KEY → reviews(id)	ID отзыва
user_id	BIGINT	PRIMARY KEY, FOREIGN KEY → users(id)	ID пользователя
is_like	BOOLEAN	NOT NULL	true=лайк, false=дизлайк
🔗 Таблица film_genre — связь фильмов и жанров
Поле	Тип	Ограничения	Описание
film_id	BIGINT	PRIMARY KEY, FOREIGN KEY → films(id) ON DELETE CASCADE	ID фильма
genre_id	INTEGER	PRIMARY KEY, FOREIGN KEY → genres(id) ON DELETE CASCADE	ID жанра
position	INTEGER	NOT NULL	Порядок жанра
🔗 Таблица film_director — связь фильмов и режиссёров
Поле	Тип	Ограничения	Описание
film_id	BIGINT	PRIMARY KEY, FOREIGN KEY → films(id) ON DELETE CASCADE	ID фильма
director_id	BIGINT	PRIMARY KEY, FOREIGN KEY → directors(id) ON DELETE CASCADE	ID режиссёра
🤝 Таблица friendship — друзья пользователей
Поле	Тип	Ограничения	Описание
user_id	BIGINT	PRIMARY KEY, FOREIGN KEY → users(id) ON DELETE CASCADE	ID пользователя
friend_id	BIGINT	PRIMARY KEY, FOREIGN KEY → users(id) ON DELETE CASCADE	ID друга
📰 Таблица events — лента событий
Поле	Тип	Ограничения	Описание
id	BIGINT	PRIMARY KEY	ID события
timestamp	BIGINT	NOT NULL	Время в миллисекундах
user_id	BIGINT	NOT NULL, FOREIGN KEY → users(id)	ID пользователя
event_type	VARCHAR(20)	NOT NULL	LIKE, REVIEW, FRIEND, MARK
operation	VARCHAR(20)	NOT NULL	REMOVE, ADD, UPDATE
entity_id	BIGINT	NOT NULL	ID сущности
🔗 Связи между таблицами
От	К	Тип связи
films	mpa	Многие к одному
film_genre	films	Многие к одному
film_genre	genres	Многие к одному
film_director	films	Многие к одному
film_director	directors	Многие к одному
marks	users	Многие к одному
marks	films	Многие к одному
reviews	users	Многие к одному
reviews	films	Многие к одному
review_ratings	reviews	Многие к одному
review_ratings	users	Многие к одному
friendship	users	Многие к одному
events	users	Многие к одному
🚀 Индексы для оптимизации
sql
-- Пользователи
CREATE INDEX idx_users_login ON users(login);
CREATE INDEX idx_users_email ON users(email);

-- Фильмы
CREATE INDEX idx_films_release_date ON films(release_date);
CREATE INDEX idx_films_mpa_id ON films(mpa_id);
CREATE INDEX idx_films_name ON films(name);

-- Оценки
CREATE INDEX idx_marks_film_id ON marks(film_id);
CREATE INDEX idx_marks_user_id ON marks(user_id);
CREATE INDEX idx_marks_value ON marks(mark_value);
CREATE INDEX idx_marks_film_user ON marks(film_id, user_id);
CREATE INDEX idx_marks_avg ON marks(film_id, mark_value);

-- Отзывы
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_film_id ON reviews(film_id);
CREATE INDEX idx_reviews_useful ON reviews(useful DESC);

-- Оценки отзывов
CREATE INDEX idx_review_ratings_review_id ON review_ratings(review_id);
CREATE INDEX idx_review_ratings_user_id ON review_ratings(user_id);

-- Друзья
CREATE INDEX idx_friendship_user_id ON friendship(user_id);
CREATE INDEX idx_friendship_friend_id ON friendship(friend_id);

-- События
CREATE INDEX idx_events_user_id ON events(user_id);
CREATE INDEX idx_events_timestamp ON events(timestamp);
CREATE INDEX idx_events_user_timestamp ON events(user_id, timestamp DESC);
CREATE INDEX idx_events_entity ON events(entity_id);

-- Связующие таблицы
CREATE INDEX idx_film_genre_film_id ON film_genre(film_id);
CREATE INDEX idx_film_genre_genre_id ON film_genre(genre_id);
CREATE INDEX idx_film_director_film_id ON film_director(film_id);
CREATE INDEX idx_film_director_director_id ON film_director(director_id);

-- Режиссёры
CREATE INDEX idx_directors_name ON directors(name);
📝 Примеры SQL-запросов
1. Топ-10 популярных фильмов
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
2. Друзья пользователя
sql
SELECT u.* 
FROM users u
JOIN friendship f ON u.id = f.friend_id
WHERE f.user_id = 1;
3. Общие фильмы двух пользователей
sql
SELECT f.*, COALESCE(AVG(mrk.mark_value), 0) AS avg_rating
FROM films f
LEFT JOIN marks mrk ON f.id = mrk.film_id
WHERE f.id IN (
    SELECT mrk1.film_id FROM marks mrk1 
    WHERE mrk1.user_id = 1 AND mrk1.mark_value >= 6
    INTERSECT
    SELECT mrk2.film_id FROM marks mrk2 
    WHERE mrk2.user_id = 2 AND mrk2.mark_value >= 6
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
-- Находим похожего пользователя
SELECT mrk2.user_id, 
       COUNT(*) as common_films,
       CORR(mrk1.mark_value, mrk2.mark_value) AS rating_correlation
FROM marks mrk1
JOIN marks mrk2 ON mrk1.film_id = mrk2.film_id
WHERE mrk1.user_id = 1 AND mrk2.user_id != 1
GROUP BY mrk2.user_id
HAVING COUNT(*) >= 2
ORDER BY rating_correlation DESC, common_films DESC
LIMIT 1;

-- Рекомендуем фильмы
SELECT f.*, COALESCE(AVG(mrk.mark_value), 0) AS avg_rating
FROM films f
LEFT JOIN marks mrk ON f.id = mrk.film_id
WHERE f.id IN (
    SELECT mrk2.film_id
    FROM marks mrk2
    WHERE mrk2.user_id = ? 
      AND mrk2.mark_value >= 6
      AND mrk2.film_id NOT IN (
          SELECT mrk1.film_id FROM marks mrk1 WHERE mrk1.user_id = 1
      )
)
GROUP BY f.id
ORDER BY avg_rating DESC;
🚀 Запуск приложения
Требования
Java 21

Maven 3.8+

Установка и запуск
bash
# Клонирование репозитория
git clone https://github.com/AlexandrSanych/filmorate.git
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
GET	/films	Все фильмы
GET	/films/{id}	Фильм по ID
PUT	/films/{id}/mark/{userId}?mark=	Добавить оценку (1-10)
PUT	/films/{id}/mark/{userId}/update?mark=	Обновить оценку
DELETE	/films/{id}/mark/{userId}	Удалить оценку
GET	/films/popular?count=&genreId=&year=	Популярные фильмы
GET	/films/common?userId=&friendId=	Общие фильмы
GET	/films/search?query=&by=	Поиск фильмов
GET	/films/director/{directorId}?sortBy=	Фильмы режиссёра
Пользователи (/users)
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
Отзывы (/reviews)
Метод	Эндпоинт	Описание
POST	/reviews	Создание отзыва
PUT	/reviews	Обновление отзыва
DELETE	/reviews/{id}	Удаление отзыва
GET	/reviews/{id}	Отзыв по ID
GET	/reviews?filmId=&count=	Список отзывов
PUT	/reviews/{id}/like/{userId}	Лайк
PUT	/reviews/{id}/dislike/{userId}	Дизлайк
DELETE	/reviews/{id}/like/{userId}	Убрать лайк
DELETE	/reviews/{id}/dislike/{userId}	Убрать дизлайк
Режиссёры (/directors)
Метод	Эндпоинт	Описание
GET	/directors	Все режиссёры
GET	/directors/{id}	Режиссёр по ID
POST	/directors	Создание режиссёра
PUT	/directors	Обновление режиссёра
DELETE	/directors/{id}	Удаление режиссёра
Жанры (/genres)
Метод	Эндпоинт	Описание
GET	/genres	Все жанры
GET	/genres/{id}	Жанр по ID
Рейтинги MPA (/mpa)
Метод	Эндпоинт	Описание
GET	/mpa	Все рейтинги
GET	/mpa/{id}	Рейтинг по ID
🛠️ Особенности реализации
Оценки (marks) вместо лайков
Пользователи ставят оценки от 1 до 10

Положительной считается оценка >= 6

Средняя оценка фильма вычисляется автоматически

События: MARK с операциями ADD, UPDATE, REMOVE

Лента событий (feed)
Отслеживает: добавление/обновление/удаление отзывов, друзей, оценок

Поддерживаемые типы: REVIEW, FRIEND, MARK

Возвращается в хронологическом порядке (сначала новые)

Рекомендации
Основаны на корреляции оценок между пользователями

Рекомендуются фильмы с оценкой >= 6 от похожего пользователя

Требуется минимум 2 общих фильма для расчёта корреляции

📫 Контакты
Telegram: @AlexandrSanychP

GitHub: AlexandrSanych

⭐️ Если вам понравился проект — поставьте звезду!

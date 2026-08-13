🎬 Filmorate — Социальная сеть для оценки фильмов
Проект Filmorate — это бэкенд-приложение для социальной сети, где пользователи могут:

оценивать фильмы по шкале от 1 до 10,

писать отзывы и оценивать их полезность,

добавлять друзей,

получать рекомендации на основе вкусов других пользователей,

отслеживать ленту событий.

📦 Технологии
Технология	Версия
Java	21
Spring Boot	3.2.4
Spring MVC	-
Spring JDBC / JdbcTemplate	-
H2 Database	-
Lombok	-
Maven	3.8+
🗄️ Структура базы данных
📊 ER-диаграмма
https://diagram%2520Filmorate.png

📋 Описание таблиц
👤 users — пользователи
Поле	Тип	Описание
id	BIGINT	Уникальный идентификатор
email	VARCHAR(255)	Электронная почта
login	VARCHAR(100)	Логин пользователя
name	VARCHAR(100)	Имя для отображения
birthday	DATE	Дата рождения
Ограничения: id PRIMARY KEY, email NOT NULL UNIQUE, login NOT NULL UNIQUE

🎬 films — фильмы
Поле	Тип	Описание
id	BIGINT	Уникальный идентификатор
name	VARCHAR(255)	Название фильма
description	VARCHAR(200)	Описание
release_date	DATE	Дата выхода
duration	INTEGER	Длительность в минутах
mpa_id	INTEGER	ID рейтинга MPA
Ограничения: id PRIMARY KEY, name NOT NULL, mpa_id → mpa(id)

🏷️ mpa — рейтинги MPA
Поле	Тип	Описание
id	INTEGER	ID рейтинга
name	VARCHAR(10)	Код рейтинга
Значения: G, PG, PG-13, R, NC-17

🎭 genres — жанры
Поле	Тип	Описание
id	INTEGER	ID жанра
name	VARCHAR(100)	Название жанра
🎬 directors — режиссёры
Поле	Тип	Описание
id	BIGINT	ID режиссёра
name	VARCHAR(255)	Имя режиссёра
⭐ marks — оценки фильмов
Поле	Тип	Описание
film_id	BIGINT	ID фильма
user_id	BIGINT	ID пользователя
mark_value	INTEGER	Оценка от 1 до 10
created_at	TIMESTAMP	Дата создания
updated_at	TIMESTAMP	Дата обновления
Ограничения: (film_id, user_id) PRIMARY KEY, film_id → films(id), user_id → users(id)

💬 reviews — отзывы
Поле	Тип	Описание
id	BIGINT	ID отзыва
content	VARCHAR(1000)	Содержание
is_positive	BOOLEAN	true/false
user_id	BIGINT	Автор
film_id	BIGINT	Фильм
useful	INTEGER	Рейтинг полезности
created_at	TIMESTAMP	Дата создания
Ограничения: id PRIMARY KEY, user_id → users(id), film_id → films(id)

👍 review_ratings — оценки отзывов
Поле	Тип	Описание
review_id	BIGINT	ID отзыва
user_id	BIGINT	ID пользователя
is_like	BOOLEAN	true=лайк, false=дизлайк
Ограничения: (review_id, user_id) PRIMARY KEY

🔗 film_genre — связь фильмов и жанров
Поле	Тип	Описание
film_id	BIGINT	ID фильма
genre_id	INTEGER	ID жанра
position	INTEGER	Порядок жанра
Ограничения: (film_id, genre_id) PRIMARY KEY

🔗 film_director — связь фильмов и режиссёров
Поле	Тип	Описание
film_id	BIGINT	ID фильма
director_id	BIGINT	ID режиссёра
Ограничения: (film_id, director_id) PRIMARY KEY

🤝 friendship — друзья
Поле	Тип	Описание
user_id	BIGINT	ID пользователя
friend_id	BIGINT	ID друга
Ограничения: (user_id, friend_id) PRIMARY KEY

📰 events — лента событий
Поле	Тип	Описание
id	BIGINT	ID события
timestamp	BIGINT	Время в мс
user_id	BIGINT	ID пользователя
event_type	VARCHAR(20)	LIKE/REVIEW/FRIEND/MARK
operation	VARCHAR(20)	REMOVE/ADD/UPDATE
entity_id	BIGINT	ID сущности
🔗 Связи таблиц
Связь	Тип
films → mpa	Многие к одному
film_genre → films	Многие к одному
film_genre → genres	Многие к одному
film_director → films	Многие к одному
film_director → directors	Многие к одному
marks → users	Многие к одному
marks → films	Многие к одному
reviews → users	Многие к одному
reviews → films	Многие к одному
review_ratings → reviews	Многие к одному
review_ratings → users	Многие к одному
friendship → users	Многие к одному
events → users	Многие к одному
🚀 Индексы
sql
-- Users
CREATE INDEX idx_users_login ON users(login);
CREATE INDEX idx_users_email ON users(email);

-- Films
CREATE INDEX idx_films_release_date ON films(release_date);
CREATE INDEX idx_films_mpa_id ON films(mpa_id);
CREATE INDEX idx_films_name ON films(name);

-- Marks
CREATE INDEX idx_marks_film_id ON marks(film_id);
CREATE INDEX idx_marks_user_id ON marks(user_id);
CREATE INDEX idx_marks_value ON marks(mark_value);
CREATE INDEX idx_marks_film_user ON marks(film_id, user_id);
CREATE INDEX idx_marks_avg ON marks(film_id, mark_value);

-- Reviews
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_film_id ON reviews(film_id);
CREATE INDEX idx_reviews_useful ON reviews(useful DESC);

-- Review ratings
CREATE INDEX idx_review_ratings_review_id ON review_ratings(review_id);
CREATE INDEX idx_review_ratings_user_id ON review_ratings(user_id);

-- Friendship
CREATE INDEX idx_friendship_user_id ON friendship(user_id);
CREATE INDEX idx_friendship_friend_id ON friendship(friend_id);

-- Events
CREATE INDEX idx_events_user_id ON events(user_id);
CREATE INDEX idx_events_timestamp ON events(timestamp);
CREATE INDEX idx_events_user_timestamp ON events(user_id, timestamp DESC);
CREATE INDEX idx_events_entity ON events(entity_id);

-- Junction tables
CREATE INDEX idx_film_genre_film_id ON film_genre(film_id);
CREATE INDEX idx_film_genre_genre_id ON film_genre(genre_id);
CREATE INDEX idx_film_director_film_id ON film_director(film_id);
CREATE INDEX idx_film_director_director_id ON film_director(director_id);

-- Directors
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
5. Лента событий
sql
SELECT * FROM events 
WHERE user_id = 1 
ORDER BY timestamp DESC;
6. Рекомендации
sql
-- Похожий пользователь
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

-- Рекомендации
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
Требования: Java 21, Maven 3.8+

bash
git clone https://github.com/AlexandrSanych/filmorate.git
cd filmorate
mvn clean package
java -jar target/filmorate-0.0.1-SNAPSHOT.jar
H2 Console: http://localhost:8080/h2-console

JDBC URL: jdbc:h2:file:./db/filmorate

User: sa

Password: password

📚 API Эндпоинты
Фильмы (/films)
Метод	Эндпоинт	Описание
POST	/films	Создание
PUT	/films	Обновление
DELETE	/films/{filmId}	Удаление
GET	/films	Все фильмы
GET	/films/{id}	По ID
PUT	/films/{id}/mark/{userId}?mark=	Оценка
PUT	/films/{id}/mark/{userId}/update?mark=	Обновить оценку
DELETE	/films/{id}/mark/{userId}	Удалить оценку
GET	/films/popular?count=&genreId=&year=	Популярные
GET	/films/common?userId=&friendId=	Общие
GET	/films/search?query=&by=	Поиск
GET	/films/director/{directorId}?sortBy=	По режиссёру
Пользователи (/users)
Метод	Эндпоинт	Описание
POST	/users	Создание
PUT	/users	Обновление
DELETE	/users/{userId}	Удаление
GET	/users	Все
GET	/users/{id}	По ID
PUT	/users/{id}/friends/{friendId}	Добавить друга
DELETE	/users/{id}/friends/{friendId}	Удалить друга
GET	/users/{id}/friends	Друзья
GET	/users/{id}/friends/common/{otherId}	Общие друзья
GET	/users/{id}/recommendations	Рекомендации
GET	/users/{userId}/feed	Лента событий
Отзывы (/reviews)
Метод	Эндпоинт	Описание
POST	/reviews	Создание
PUT	/reviews	Обновление
DELETE	/reviews/{id}	Удаление
GET	/reviews/{id}	По ID
GET	/reviews?filmId=&count=	Список
PUT	/reviews/{id}/like/{userId}	Лайк
PUT	/reviews/{id}/dislike/{userId}	Дизлайк
DELETE	/reviews/{id}/like/{userId}	Убрать лайк
DELETE	/reviews/{id}/dislike/{userId}	Убрать дизлайк
Режиссёры (/directors)
Метод	Эндпоинт	Описание
GET	/directors	Все
GET	/directors/{id}	По ID
POST	/directors	Создание
PUT	/directors	Обновление
DELETE	/directors/{id}	Удаление
Жанры (/genres)
Метод	Эндпоинт	Описание
GET	/genres	Все
GET	/genres/{id}	По ID
Рейтинги MPA (/mpa)
Метод	Эндпоинт	Описание
GET	/mpa	Все
GET	/mpa/{id}	По ID
🛠️ Особенности реализации
Оценки (marks): от 1 до 10, положительная >= 6

Лента событий: REVIEW, FRIEND, MARK с операциями ADD, UPDATE, REMOVE

Рекомендации: на основе корреляции оценок, минимум 2 общих фильма

📫 Контакты
Telegram: @AlexandrSanychP

GitHub: AlexandrSanych

⭐️ Если вам понравился проект — поставьте звезду!


-- =====================================================
-- ОЧИСТКА ВСЕХ ТАБЛИЦ
-- =====================================================
DELETE FROM film_genre;
DELETE FROM film_director;
DELETE FROM review_ratings;
DELETE FROM reviews;
DELETE FROM marks;
DELETE FROM events;
DELETE FROM friendship;
DELETE FROM films;
DELETE FROM users;
DELETE FROM directors;
DELETE FROM genres;
DELETE FROM mpa;

-- =====================================================
-- СБРОС АВТОИНКРЕМЕНТОВ ДЛЯ H2
-- =====================================================
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;
ALTER TABLE films ALTER COLUMN id RESTART WITH 1;
ALTER TABLE directors ALTER COLUMN id RESTART WITH 1;
ALTER TABLE reviews ALTER COLUMN id RESTART WITH 1;
ALTER TABLE events ALTER COLUMN id RESTART WITH 1;

-- =====================================================
-- ЗАПОЛНЕНИЕ ТЕСТОВЫМИ ДАННЫМИ
-- =====================================================

-- MPA рейтинги
INSERT INTO mpa (id, name) VALUES
    (1, 'G'),
    (2, 'PG'),
    (3, 'PG-13'),
    (4, 'R'),
    (5, 'NC-17');

-- Жанры
INSERT INTO genres (id, name) VALUES
    (1, 'Комедия'),
    (2, 'Драма'),
    (3, 'Мультфильм'),
    (4, 'Триллер'),
    (5, 'Документальный'),
    (6, 'Боевик');

-- Режиссёры
INSERT INTO directors (id, name) VALUES
    (1, 'Кристофер Нолан'),
    (2, 'Квентин Тарантино'),
    (3, 'Джеймс Кэмерон');

-- Пользователи
INSERT INTO users (id, email, login, name, birthday) VALUES
    (1, 'test1@test.com', 'testuser1', 'Test User 1', '1990-01-01'),
    (2, 'test2@test.com', 'testuser2', 'Test User 2', '1992-02-02'),
    (3, 'test3@test.com', 'testuser3', 'Test User 3', '1994-03-03');

-- Фильмы
INSERT INTO films (id, name, description, release_date, duration, mpa_id) VALUES
    (1, 'Test Film 1', 'Description 1', '2020-01-01', 120, 1),
    (2, 'Test Film 2', 'Description 2', '2020-02-02', 90, 2),
    (3, 'Test Film 3', 'Description 3', '2021-03-03', 150, 3);

-- Связи фильмов с режиссёрами (film_director)
INSERT INTO film_director (film_id, director_id) VALUES
    (1, 1),
    (2, 2);
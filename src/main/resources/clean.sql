-- =====================================================
-- ОЧИСТКА ВСЕХ ТАБЛИЦ ПЕРЕД ЗАПУСКОМ ТЕСТОВ
-- =====================================================

DELETE FROM review_ratings;
DELETE FROM reviews;
DELETE FROM marks;
DELETE FROM film_genre;
DELETE FROM film_director;
DELETE FROM events;
DELETE FROM friendship;
DELETE FROM films;
DELETE FROM users;
DELETE FROM directors;
DELETE FROM genres;
DELETE FROM mpa;

-- Сброс автоинкремента
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;
ALTER TABLE films ALTER COLUMN id RESTART WITH 1;
ALTER TABLE directors ALTER COLUMN id RESTART WITH 1;
ALTER TABLE reviews ALTER COLUMN id RESTART WITH 1;
ALTER TABLE events ALTER COLUMN id RESTART WITH 1;

-- Заполнение начальными данными
INSERT INTO mpa (id, name) VALUES
(1, 'G'), (2, 'PG'), (3, 'PG-13'), (4, 'R'), (5, 'NC-17');

INSERT INTO genres (id, name) VALUES
(1, 'Комедия'), (2, 'Драма'), (3, 'Мультфильм'),
(4, 'Триллер'), (5, 'Документальный'), (6, 'Боевик');
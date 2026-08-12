package ru.yandex.practicum.filmorate.storage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.*;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"classpath:schema.sql", "classpath:test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FilmDbStorageAdditionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private FilmDbStorage filmStorage;
    private UserDbStorage userStorage;
    private MarkDbStorage markStorage;
    private DirectorDbStorage directorStorage;

    private Long userId1;
    private Long userId2;
    private Long filmId1;
    private Long filmId2;
    private Long filmId3;
    private Long directorId1;

    @BeforeEach
    void setUp() {
        markStorage = new MarkDbStorage(jdbcTemplate);
        filmStorage = new FilmDbStorage(jdbcTemplate, markStorage);
        userStorage = new UserDbStorage(jdbcTemplate);
        directorStorage = new DirectorDbStorage(jdbcTemplate);

        // Очистка
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM film_director");
        jdbcTemplate.execute("DELETE FROM review_ratings");
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM marks");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM directors");

        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE directors ALTER COLUMN id RESTART WITH 1");

        // Создаём пользователей
        User user1 = User.builder()
                .email("user1@test.com")
                .login("user1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User user2 = User.builder()
                .email("user2@test.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();
        User user3 = User.builder()
                .email("user3@test.com")
                .login("user3")
                .name("User Three")
                .birthday(LocalDate.of(1994, 3, 3))
                .build();

        userId1 = userStorage.create(user1).getId();
        userId2 = userStorage.create(user2).getId();
        userStorage.create(user3);

        // Создаём режиссёра
        Director director = Director.builder().name("Test Director").build();
        directorId1 = directorStorage.create(director).getId();

        // Создаём фильмы
        Film film1 = Film.builder()
                .name("Film A")
                .description("Description A")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();
        Film film2 = Film.builder()
                .name("Film B")
                .description("Description B")
                .releaseDate(LocalDate.of(2020, 2, 2))
                .duration(90)
                .mpa(Mpa.builder().id(2).build())
                .build();
        Film film3 = Film.builder()
                .name("Film C")
                .description("Description C")
                .releaseDate(LocalDate.of(2021, 3, 3))
                .duration(150)
                .mpa(Mpa.builder().id(3).build())
                .build();

        filmId1 = filmStorage.create(film1).getId();
        filmId2 = filmStorage.create(film2).getId();
        filmId3 = filmStorage.create(film3).getId();
    }

    // ==================== ТЕСТЫ ДЛЯ getCommonFilms ====================

    @Test
    void shouldGetCommonFilms() {
        // User1 ставит оценки фильмам A, B, C
        filmStorage.addMark(filmId1, userId1, 10);
        filmStorage.addMark(filmId2, userId1, 8);
        filmStorage.addMark(filmId3, userId1, 6);

        // User2 ставит оценки фильмам B, C
        filmStorage.addMark(filmId2, userId2, 9);
        filmStorage.addMark(filmId3, userId2, 7);

        List<Film> commonFilms = filmStorage.getCommonFilms(userId1, userId2);

        // Общие фильмы: Film B и Film C (оба получили оценки >= 6)
        assertThat(commonFilms).hasSize(2);
        assertThat(commonFilms).extracting(Film::getName)
                .containsExactlyInAnyOrder("Film B", "Film C");
    }

    @Test
    void shouldReturnEmptyWhenNoCommonFilms() {
        // User1 оценил только Film A
        filmStorage.addMark(filmId1, userId1, 10);

        // User2 оценил только Film B
        filmStorage.addMark(filmId2, userId2, 9);

        List<Film> commonFilms = filmStorage.getCommonFilms(userId1, userId2);

        assertThat(commonFilms).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoPositiveMarks() {
        // User1 поставил низкие оценки (ниже 6)
        filmStorage.addMark(filmId1, userId1, 5);
        filmStorage.addMark(filmId2, userId1, 4);

        // User2 поставил высокие оценки
        filmStorage.addMark(filmId2, userId2, 9);
        filmStorage.addMark(filmId3, userId2, 8);

        List<Film> commonFilms = filmStorage.getCommonFilms(userId1, userId2);

        // У User1 нет положительных оценок → пустой результат
        assertThat(commonFilms).isEmpty();
    }

    // ==================== ТЕСТЫ ДЛЯ searchFilms ====================

    @Test
    void shouldSearchFilmsByTitle() {
        // Добавляем связи с режиссёром
        addDirectorToFilm(filmId1, directorId1);
        addDirectorToFilm(filmId2, directorId1);
        addDirectorToFilm(filmId3, directorId1);

        List<Film> found = filmStorage.search("Film", true, false);

        assertThat(found).hasSize(3);
        assertThat(found).extracting(Film::getName)
                .containsExactlyInAnyOrder("Film A", "Film B", "Film C");
    }

    @Test
    void shouldSearchFilmsByTitleCaseInsensitive() {
        addDirectorToFilm(filmId1, directorId1);

        List<Film> foundLower = filmStorage.search("film a", true, false);
        List<Film> foundUpper = filmStorage.search("FILM A", true, false);

        assertThat(foundLower).hasSize(1);
        assertThat(foundUpper).hasSize(1);
        assertThat(foundLower.get(0).getName()).isEqualTo("Film A");
    }

    @Test
    void shouldSearchFilmsByDirector() {
        addDirectorToFilm(filmId1, directorId1);
        addDirectorToFilm(filmId2, directorId1);
        // filmId3 без режиссёра

        List<Film> found = filmStorage.search("Test Director", false, true);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Film::getName)
                .containsExactlyInAnyOrder("Film A", "Film B");
    }

    @Test
    void shouldSearchFilmsByDirectorCaseInsensitive() {
        addDirectorToFilm(filmId1, directorId1);

        List<Film> foundLower = filmStorage.search("test director", false, true);
        List<Film> foundUpper = filmStorage.search("TEST DIRECTOR", false, true);

        assertThat(foundLower).hasSize(1);
        assertThat(foundUpper).hasSize(1);
    }

    @Test
    void shouldSearchFilmsByTitleAndDirector() {
        addDirectorToFilm(filmId1, directorId1);
        addDirectorToFilm(filmId2, directorId1);
        // filmId3 без режиссёра, но подходит по названию

        List<Film> found = filmStorage.search("Film", true, true);

        assertThat(found).hasSize(3);
    }

    @Test
    void shouldSearchFilmsByTitleAndDirectorWithPartialMatch() {
        addDirectorToFilm(filmId1, directorId1);
        addDirectorToFilm(filmId2, directorId1);

        List<Film> found = filmStorage.search("Film", true, true);

        assertThat(found).isNotEmpty();
        assertThat(found).allMatch(f -> f.getName().contains("Film") ||
                (f.getDirectors() != null && !f.getDirectors().isEmpty()));
    }

    @Test
    void shouldReturnEmptyForNoMatchSearch() {
        List<Film> found = filmStorage.search("NonExistentFilm", true, true);

        assertThat(found).isEmpty();
    }

    // ==================== ТЕСТЫ ДЛЯ getRecommendations ====================

    @Test
    void shouldGetRecommendations() {
        // User1 оценил Film A (10) и Film B (9)
        filmStorage.addMark(filmId1, userId1, 10);
        filmStorage.addMark(filmId2, userId1, 9);

        // User2 оценил Film A (8) и Film B (7) — два общих фильма!
        filmStorage.addMark(filmId1, userId2, 8);
        filmStorage.addMark(filmId2, userId2, 7);

        // User2 также оценил Film C (10) — это будет рекомендовано
        filmStorage.addMark(filmId3, userId2, 10);

        List<Film> recommendations = filmStorage.getRecommendations(userId1);

        // Должен быть рекомендован Film C
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).extracting(Film::getName)
                .contains("Film C");
    }

    @Test
    void shouldReturnEmptyRecommendationsForNoSimilarUsers() {
        // User1 — единственный пользователь
        List<Film> recommendations = filmStorage.getRecommendations(userId1);
        assertThat(recommendations).isEmpty();
    }

    @Test
    void shouldReturnEmptyRecommendationsWhenAllFilmsAlreadyWatched() {
        // User1 оценил Film A и Film B
        filmStorage.addMark(filmId1, userId1, 10);
        filmStorage.addMark(filmId2, userId1, 9);

        // User2 оценил Film A и Film B (те же фильмы)
        filmStorage.addMark(filmId1, userId2, 8);
        filmStorage.addMark(filmId2, userId2, 7);

        List<Film> recommendations = filmStorage.getRecommendations(userId1);

        // Нет непросмотренных фильмов у похожего пользователя
        assertThat(recommendations).isEmpty();
    }

    @Test
    void shouldRecommendFilmsWithPositiveRatingOnly() {
        // User1 высоко оценил Film A
        filmStorage.addMark(filmId1, userId1, 10);

        // User2 высоко оценил Film A, но Film B оценил низко (4)
        filmStorage.addMark(filmId1, userId2, 9);
        filmStorage.addMark(filmId2, userId2, 4);  // отрицательная оценка

        List<Film> recommendations = filmStorage.getRecommendations(userId1);

        // Рекомендуем только Film ? (Film B не должен рекомендоваться, т.к. оценка < 6)
        // Если Film B единственный непросмотренный, но у него низкая оценка → не рекомендуем
        assertThat(recommendations).doesNotContain(filmStorage.findById(filmId2).get());
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private void addDirectorToFilm(Long filmId, Long directorId) {
        Film film = filmStorage.findById(filmId).get();
        List<Director> directors = List.of(Director.builder().id(directorId).build());
        film.setDirectors(directors);
        filmStorage.update(film);
    }
}
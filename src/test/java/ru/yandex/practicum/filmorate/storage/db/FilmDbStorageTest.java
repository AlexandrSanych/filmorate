package ru.yandex.practicum.filmorate.storage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.MarkStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FilmDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private FilmDbStorage filmStorage;
    private UserDbStorage userStorage;
    private MarkStorage markStorage;

    @BeforeEach
    void setUp() {
        markStorage = new MarkDbStorage(jdbcTemplate);
        filmStorage = new FilmDbStorage(jdbcTemplate, markStorage);
        userStorage = new UserDbStorage(jdbcTemplate);

        // Очистка таблиц
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM marks");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM film_director");
        jdbcTemplate.execute("DELETE FROM directors");

        // Сброс автоинкремента
        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE directors ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    void shouldCreateFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        assertNotNull(created.getId());
        assertEquals(1L, created.getId());
        assertEquals("Test Film", created.getName());
        assertEquals("Test Description", created.getDescription());
        assertEquals(LocalDate.of(2020, 1, 1), created.getReleaseDate());
        assertEquals(120, created.getDuration());
        assertNotNull(created.getMpa());
        assertEquals(1, created.getMpa().getId());
        assertEquals("G", created.getMpa().getName());
    }

    @Test
    void shouldCreateFilmWithGenres() {
        Film film = createTestFilm();
        film.setGenres(List.of(
                Genre.builder().id(1).build(),
                Genre.builder().id(2).build()
        ));

        Film created = filmStorage.create(film);

        Film found = filmStorage.findById(created.getId()).get();
        assertThat(found.getGenres()).hasSize(2);
        assertThat(found.getGenres()).extracting(Genre::getId).containsExactly(1, 2);
        assertThat(found.getGenres()).extracting(Genre::getName).containsExactly("Комедия", "Драма");
    }

    @Test
    void shouldCreateFilmWithDirectors() {
        // Создаём режиссёров
        Director director1 = Director.builder().name("Director 1").build();
        Director director2 = Director.builder().name("Director 2").build();
        DirectorDbStorage directorStorage = new DirectorDbStorage(jdbcTemplate);
        Director createdDirector1 = directorStorage.create(director1);
        Director createdDirector2 = directorStorage.create(director2);

        Film film = createTestFilm();
        film.setDirectors(List.of(createdDirector1, createdDirector2));

        Film created = filmStorage.create(film);

        Film found = filmStorage.findById(created.getId()).get();
        assertThat(found.getDirectors()).hasSize(2);
        assertThat(found.getDirectors()).extracting(Director::getId)
                .containsExactly(createdDirector1.getId(), createdDirector2.getId());
    }

    @Test
    void shouldUpdateFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        created.setName("Updated Film");
        created.setDescription("Updated Description");
        created.setDuration(150);
        created.setMpa(Mpa.builder().id(2).build());

        Film updated = filmStorage.update(created);

        assertEquals("Updated Film", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
        assertEquals(150, updated.getDuration());
        assertEquals(2, updated.getMpa().getId());
        assertEquals("PG", updated.getMpa().getName());
    }

    @Test
    void shouldUpdateFilmWithGenres() {
        Film film = createTestFilm();
        film.setGenres(List.of(Genre.builder().id(1).build()));
        Film created = filmStorage.create(film);

        created.setGenres(List.of(Genre.builder().id(2).build(), Genre.builder().id(3).build()));
        filmStorage.update(created);

        Film updated = filmStorage.findById(created.getId()).get();
        assertThat(updated.getGenres()).hasSize(2);
        assertThat(updated.getGenres()).extracting(Genre::getId).containsExactly(2, 3);
        assertThat(updated.getGenres()).extracting(Genre::getName).containsExactly("Драма", "Мультфильм");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentFilm() {
        Film film = createTestFilm();
        film.setId(999L);

        assertThatThrownBy(() -> filmStorage.update(film))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Фильм с id=999 не найден");
    }

    @Test
    void shouldFindAllFilms() {
        Film film1 = createTestFilm("Film 1", LocalDate.of(2020, 1, 1));
        Film film2 = createTestFilm("Film 2", LocalDate.of(2020, 2, 2));

        filmStorage.create(film1);
        filmStorage.create(film2);

        List<Film> films = filmStorage.findAll();

        assertThat(films).hasSize(2);
        assertThat(films).extracting(Film::getName).containsExactly("Film 1", "Film 2");
    }

    @Test
    void shouldFindFilmById() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        Optional<Film> found = filmStorage.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals("Test Film", found.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenFilmNotFound() {
        Optional<Film> found = filmStorage.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void shouldDeleteFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        assertTrue(filmStorage.findById(created.getId()).isPresent());

        filmStorage.delete(created.getId());

        assertFalse(filmStorage.findById(created.getId()).isPresent());
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentFilm() {
        assertThatThrownBy(() -> filmStorage.delete(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Фильм с id=999 не найден");
    }

    @Test
    void shouldAddMark() {
        User user = createTestUser();
        User createdUser = userStorage.create(user);
        Film film = createTestFilm();
        Film createdFilm = filmStorage.create(film);

        filmStorage.addMark(createdFilm.getId(), createdUser.getId(), 10);

        Double rating = markStorage.getAverageRating(createdFilm.getId());
        assertEquals(10.0, rating);
    }

    @Test
    void shouldUpdateMark() {
        User user = createTestUser();
        User createdUser = userStorage.create(user);
        Film film = createTestFilm();
        Film createdFilm = filmStorage.create(film);

        filmStorage.addMark(createdFilm.getId(), createdUser.getId(), 5);
        Double rating1 = markStorage.getAverageRating(createdFilm.getId());
        assertEquals(5.0, rating1);

        filmStorage.updateMark(createdFilm.getId(), createdUser.getId(), 10);
        Double rating2 = markStorage.getAverageRating(createdFilm.getId());
        assertEquals(10.0, rating2);
    }

    @Test
    void shouldRemoveMark() {
        User user = createTestUser();
        User createdUser = userStorage.create(user);
        Film film = createTestFilm();
        Film createdFilm = filmStorage.create(film);

        filmStorage.addMark(createdFilm.getId(), createdUser.getId(), 10);
        Double rating1 = markStorage.getAverageRating(createdFilm.getId());
        assertEquals(10.0, rating1);

        filmStorage.removeMark(createdFilm.getId(), createdUser.getId());
        Double rating2 = markStorage.getAverageRating(createdFilm.getId());
        assertEquals(0.0, rating2);
    }

    @Test
    void shouldGetPopularFilms() {
        User user1 = createTestUser("user1@test.com", "user1");
        User user2 = createTestUser("user2@test.com", "user2");
        User createdUser1 = userStorage.create(user1);
        User createdUser2 = userStorage.create(user2);

        Film film1 = createTestFilm("Popular 1", LocalDate.of(2020, 1, 1));
        Film film2 = createTestFilm("Popular 2", LocalDate.of(2020, 2, 2));
        Film createdFilm1 = filmStorage.create(film1);
        Film createdFilm2 = filmStorage.create(film2);

        // Film1 получает две оценки 10 и 9 → средняя 9.5
        filmStorage.addMark(createdFilm1.getId(), createdUser1.getId(), 10);
        filmStorage.addMark(createdFilm1.getId(), createdUser2.getId(), 9);
        // Film2 получает одну оценку 5 → средняя 5.0
        filmStorage.addMark(createdFilm2.getId(), createdUser1.getId(), 5);

        List<Film> popular = filmStorage.getPopular(10, null, null);

        assertThat(popular).hasSize(2);
        assertEquals(createdFilm1.getId(), popular.get(0).getId());
        assertEquals(createdFilm2.getId(), popular.get(1).getId());
    }

    @Test
    void shouldGetLimitedPopularFilms() {
        List<Film> films = new ArrayList<>();

        // Создаём 5 фильмов
        for (int i = 1; i <= 5; i++) {
            Film film = createTestFilm("Film " + i, LocalDate.of(2020, i, 1));
            films.add(filmStorage.create(film));
        }

        // Даём разные средние оценки для каждого фильма
        // Film 1: 1 оценка 10 → средняя 10.0
        // Film 2: 2 оценки 9 → средняя 9.0
        // Film 3: 3 оценки 8 → средняя 8.0
        // Film 4: 4 оценки 7 → средняя 7.0
        // Film 5: 5 оценок 6 → средняя 6.0
        for (int i = 0; i < films.size(); i++) {
            Film film = films.get(i);
            int markCount = i + 1;           // 1, 2, 3, 4, 5
            int markValue = 10 - i;           // 10, 9, 8, 7, 6

            for (int j = 1; j <= markCount; j++) {
                String email = "user_film" + (i + 1) + "_" + j + "@test.com";
                String login = "user_film" + (i + 1) + "_" + j;
                User user = createTestUser(email, login);
                User createdUser = userStorage.create(user);
                filmStorage.addMark(film.getId(), createdUser.getId(), markValue);
            }
        }

        List<Film> popular = filmStorage.getPopular(3, null, null);

        assertThat(popular).hasSize(3);
        // Ожидаемый порядок: Film 1 (средняя 10), Film 2 (средняя 9), Film 3 (средняя 8)
        assertEquals("Film 1", popular.get(0).getName());
        assertEquals("Film 2", popular.get(1).getName());
        assertEquals("Film 3", popular.get(2).getName());
    }

    @Test
    void shouldGetPopularFilmsWithGenreFilter() throws Exception {
        User user = createTestUser();
        User createdUser = userStorage.create(user);

        // Создаём фильм с жанром 1 (Комедия)
        Film film1 = createTestFilm("Comedy Film", LocalDate.of(2020, 1, 1));
        film1.setGenres(List.of(Genre.builder().id(1).build()));
        Film createdFilm1 = filmStorage.create(film1);

        // Создаём фильм с жанром 2 (Драма)
        Film film2 = createTestFilm("Drama Film", LocalDate.of(2020, 2, 2));
        film2.setGenres(List.of(Genre.builder().id(2).build()));
        Film createdFilm2 = filmStorage.create(film2);

        filmStorage.addMark(createdFilm1.getId(), createdUser.getId(), 10);
        filmStorage.addMark(createdFilm2.getId(), createdUser.getId(), 8);

        // Получаем популярные фильмы с жанром 1
        List<Film> popular = filmStorage.getPopular(10, 1, null);

        assertThat(popular).hasSize(1);
        assertEquals(createdFilm1.getId(), popular.get(0).getId());
        assertEquals("Comedy Film", popular.get(0).getName());
    }

    @Test
    void shouldGetPopularFilmsWithYearFilter() throws Exception {
        User user = createTestUser();
        User createdUser = userStorage.create(user);

        Film film1 = createTestFilm("Film 2020", LocalDate.of(2020, 1, 1));
        Film film2 = createTestFilm("Film 2021", LocalDate.of(2021, 2, 2));
        Film createdFilm1 = filmStorage.create(film1);
        Film createdFilm2 = filmStorage.create(film2);

        filmStorage.addMark(createdFilm1.getId(), createdUser.getId(), 10);
        filmStorage.addMark(createdFilm2.getId(), createdUser.getId(), 9);

        // Получаем популярные фильмы за 2020 год
        List<Film> popular = filmStorage.getPopular(10, null, 2020);

        assertThat(popular).hasSize(1);
        assertEquals(createdFilm1.getId(), popular.get(0).getId());
        assertEquals("Film 2020", popular.get(0).getName());
    }

    @Test
    void shouldGetPopularFilmsWithGenreAndYearFilter() throws Exception {
        User user = createTestUser();
        User createdUser = userStorage.create(user);

        Film film1 = createTestFilm("Comedy 2020", LocalDate.of(2020, 1, 1));
        film1.setGenres(List.of(Genre.builder().id(1).build()));
        Film createdFilm1 = filmStorage.create(film1);

        Film film2 = createTestFilm("Comedy 2021", LocalDate.of(2021, 2, 2));
        film2.setGenres(List.of(Genre.builder().id(1).build()));
        Film createdFilm2 = filmStorage.create(film2);

        Film film3 = createTestFilm("Drama 2020", LocalDate.of(2020, 3, 3));
        film3.setGenres(List.of(Genre.builder().id(2).build()));
        Film createdFilm3 = filmStorage.create(film3);

        filmStorage.addMark(createdFilm1.getId(), createdUser.getId(), 10);
        filmStorage.addMark(createdFilm2.getId(), createdUser.getId(), 9);
        filmStorage.addMark(createdFilm3.getId(), createdUser.getId(), 8);

        // Получаем популярные фильмы: жанр 1 (Комедия), год 2020
        List<Film> popular = filmStorage.getPopular(10, 1, 2020);

        assertThat(popular).hasSize(1);
        assertEquals(createdFilm1.getId(), popular.get(0).getId());
        assertEquals("Comedy 2020", popular.get(0).getName());
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private Film createTestFilm() {
        return createTestFilm("Test Film", LocalDate.of(2020, 1, 1));
    }

    private Film createTestFilm(String name, LocalDate releaseDate) {
        return Film.builder()
                .name(name)
                .description("Test Description")
                .releaseDate(releaseDate)
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();
    }

    private User createTestUser() {
        return createTestUser("test@test.com", "testlogin");
    }

    private User createTestUser(String email, String login) {
        return User.builder()
                .email(email)
                .login(login)
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }
}
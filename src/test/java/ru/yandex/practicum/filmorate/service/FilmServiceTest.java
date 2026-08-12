package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.*;
import ru.yandex.practicum.filmorate.storage.db.*;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FilmServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private FilmService filmService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        // Создаём зависимости
        MarkStorage markStorage = new MarkDbStorage(jdbcTemplate);
        FilmStorage filmStorage = new FilmDbStorage(jdbcTemplate, markStorage);
        UserStorage userStorage = new UserDbStorage(jdbcTemplate);
        GenreStorage genreStorage = new GenreDbStorage(jdbcTemplate);
        MpaStorage mpaStorage = new MpaDbStorage(jdbcTemplate);
        DirectorStorage directorStorage = new DirectorDbStorage(jdbcTemplate);
        EventStorage eventStorage = new EventDbStorage(jdbcTemplate);
        EventService eventService = new EventService(eventStorage, userStorage);

        filmService = new FilmService(filmStorage, userStorage, genreStorage, mpaStorage, directorStorage, eventService);
        userService = new UserService(userStorage, eventService);

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
    void createFilm_ShouldAddMpaNameFromDatabase() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        Film created = filmService.createFilm(film);

        assertNotNull(created.getMpa());
        assertEquals(1, created.getMpa().getId());
        assertEquals("G", created.getMpa().getName());
    }

    @Test
    void createFilm_ShouldWorkWithoutMpa() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(null)
                .build();

        Film created = filmService.createFilm(film);
        assertNull(created.getMpa());
    }

    @Test
    void createFilm_ShouldAddGenreNamesFromDatabase() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .genres(List.of(
                        Genre.builder().id(1).build(),
                        Genre.builder().id(2).build()
                ))
                .build();

        Film created = filmService.createFilm(film);

        assertThat(created.getGenres()).hasSize(2);
        assertThat(created.getGenres().get(0).getName()).isEqualTo("Комедия");
        assertThat(created.getGenres().get(1).getName()).isEqualTo("Драма");
    }

    @Test
    void createFilm_ShouldRemoveDuplicateGenres() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .genres(List.of(
                        Genre.builder().id(1).build(),
                        Genre.builder().id(1).build(),
                        Genre.builder().id(2).build()
                ))
                .build();

        Film created = filmService.createFilm(film);

        assertThat(created.getGenres()).hasSize(2);
        assertThat(created.getGenres()).extracting(Genre::getId).containsExactly(1, 2);
    }

    @Test
    void createFilm_ShouldHandleNullGenres() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .genres(null)
                .build();

        Film created = filmService.createFilm(film);

        assertNotNull(created.getGenres());
        assertThat(created.getGenres()).isEmpty();
    }

    @Test
    void updateFilm_ShouldAddMpaNameFromDatabase() {
        Film original = Film.builder()
                .name("Original Film")
                .description("Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();
        Film created = filmService.createFilm(original);

        created.setMpa(Mpa.builder().id(2).build());
        Film updated = filmService.updateFilm(created);

        assertEquals(2, updated.getMpa().getId());
        assertEquals("PG", updated.getMpa().getName());
    }

    @Test
    void updateFilm_ShouldAddGenreNamesFromDatabase() {
        Film original = Film.builder()
                .name("Original Film")
                .description("Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .genres(List.of(Genre.builder().id(1).build()))
                .build();
        Film created = filmService.createFilm(original);

        created.setGenres(List.of(
                Genre.builder().id(2).build(),
                Genre.builder().id(3).build()
        ));
        Film updated = filmService.updateFilm(created);

        assertThat(updated.getGenres()).hasSize(2);
        assertThat(updated.getGenres().get(0).getName()).isEqualTo("Драма");
        assertThat(updated.getGenres().get(1).getName()).isEqualTo("Мультфильм");
    }

    @Test
    void updateFilm_ShouldThrowExceptionWhenFilmNotFound() {
        Film film = Film.builder()
                .id(999L)
                .name("Test Film")
                .description("Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        assertThrows(NotFoundException.class, () -> filmService.updateFilm(film));
    }

    @Test
    void getFilmById_ShouldThrowExceptionWhenFilmNotFound() {
        assertThrows(NotFoundException.class, () -> filmService.getFilmById(999L));
    }

    @Test
    void addMark_ShouldThrowExceptionWhenFilmNotFound() {
        User user = createTestUser();
        User createdUser = userService.createUser(user);

        assertThrows(NotFoundException.class,
                () -> filmService.addMark(999L, createdUser.getId(), 10));
    }

    @Test
    void addMark_ShouldThrowExceptionWhenUserNotFound() {
        Film film = createTestFilm();
        Film createdFilm = filmService.createFilm(film);

        assertThrows(NotFoundException.class,
                () -> filmService.addMark(createdFilm.getId(), 999L, 10));
    }

    @Test
    void addMark_ShouldWorkWhenBothExist() {
        User user = createTestUser();
        User createdUser = userService.createUser(user);
        Film film = createTestFilm();
        Film createdFilm = filmService.createFilm(film);

        assertDoesNotThrow(() -> filmService.addMark(createdFilm.getId(), createdUser.getId(), 10));
    }

    @Test
    void removeMark_ShouldThrowExceptionWhenFilmNotFound() {
        User user = createTestUser();
        User createdUser = userService.createUser(user);

        assertThrows(NotFoundException.class,
                () -> filmService.removeMark(999L, createdUser.getId()));
    }

    @Test
    void removeMark_ShouldThrowExceptionWhenUserNotFound() {
        Film film = createTestFilm();
        Film createdFilm = filmService.createFilm(film);

        assertThrows(NotFoundException.class,
                () -> filmService.removeMark(createdFilm.getId(), 999L));
    }

    private Film createTestFilm() {
        return Film.builder()
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();
    }

    private User createTestUser() {
        return User.builder()
                .email("test@test.com")
                .login("testlogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }
}
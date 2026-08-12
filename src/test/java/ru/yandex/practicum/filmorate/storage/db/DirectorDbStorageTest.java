package ru.yandex.practicum.filmorate.storage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DirectorDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DirectorDbStorage directorStorage;
    private FilmDbStorage filmStorage;
    private MarkDbStorage markStorage;

    @BeforeEach
    void setUp() {
        // Инициализация хранилищ
        markStorage = new MarkDbStorage(jdbcTemplate);
        filmStorage = new FilmDbStorage(jdbcTemplate, markStorage);
        directorStorage = new DirectorDbStorage(jdbcTemplate);

        // ========== ОЧИСТКА ВСЕХ ТАБЛИЦ ПЕРЕД КАЖДЫМ ТЕСТОМ ==========
        // 1. Удаляем связи (сначала внешние ключи)
        jdbcTemplate.execute("DELETE FROM film_director");
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM marks");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM review_ratings");
        jdbcTemplate.execute("DELETE FROM reviews");

        // 2. Удаляем основные таблицы
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM directors");
        jdbcTemplate.execute("DELETE FROM users");

        // 3. Сбрасываем счётчики автоинкремента
        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE directors ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE reviews ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE events ALTER COLUMN id RESTART WITH 1");

        // 4. Заполняем справочные данные (mpa, genres)
        jdbcTemplate.execute("MERGE INTO mpa (id, name) VALUES (1, 'G')");
        jdbcTemplate.execute("MERGE INTO mpa (id, name) VALUES (2, 'PG')");
        jdbcTemplate.execute("MERGE INTO mpa (id, name) VALUES (3, 'PG-13')");
        jdbcTemplate.execute("MERGE INTO mpa (id, name) VALUES (4, 'R')");
        jdbcTemplate.execute("MERGE INTO mpa (id, name) VALUES (5, 'NC-17')");

        jdbcTemplate.execute("MERGE INTO genres (id, name) VALUES (1, 'Комедия')");
        jdbcTemplate.execute("MERGE INTO genres (id, name) VALUES (2, 'Драма')");
        jdbcTemplate.execute("MERGE INTO genres (id, name) VALUES (3, 'Мультфильм')");
        jdbcTemplate.execute("MERGE INTO genres (id, name) VALUES (4, 'Триллер')");
        jdbcTemplate.execute("MERGE INTO genres (id, name) VALUES (5, 'Документальный')");
        jdbcTemplate.execute("MERGE INTO genres (id, name) VALUES (6, 'Боевик')");
    }

    // ==================== ТЕСТЫ ====================

    @Test
    void shouldCreateDirector() {
        Director director = Director.builder()
                .name("Christopher Nolan")
                .build();

        Director created = directorStorage.create(director);

        assertNotNull(created.getId());
        assertEquals(1L, created.getId());
        assertEquals("Christopher Nolan", created.getName());
    }

    @Test
    void shouldFindAllDirectors() {
        Director director1 = Director.builder().name("Director 1").build();
        Director director2 = Director.builder().name("Director 2").build();
        directorStorage.create(director1);
        directorStorage.create(director2);

        List<Director> directors = directorStorage.findAll();

        assertThat(directors).hasSize(2);
        assertThat(directors).extracting(Director::getName)
                .containsExactly("Director 1", "Director 2");
    }

    @Test
    void shouldFindDirectorById() {
        Director director = Director.builder().name("Quentin Tarantino").build();
        Director created = directorStorage.create(director);

        Optional<Director> found = directorStorage.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals("Quentin Tarantino", found.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenDirectorNotFound() {
        Optional<Director> found = directorStorage.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void shouldUpdateDirector() {
        Director director = Director.builder().name("Old Name").build();
        Director created = directorStorage.create(director);

        created.setName("New Name");
        Director updated = directorStorage.update(created);

        assertEquals("New Name", updated.getName());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentDirector() {
        Director director = Director.builder().id(999L).name("Not Exist").build();

        assertThatThrownBy(() -> directorStorage.update(director))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Режиссёр с id=999 не найден");
    }

    @Test
    void shouldDeleteDirector() {
        Director director = Director.builder().name("To Delete").build();
        Director created = directorStorage.create(director);

        assertTrue(directorStorage.findById(created.getId()).isPresent());

        directorStorage.delete(created.getId());

        assertFalse(directorStorage.findById(created.getId()).isPresent());
    }

    @Test
    void shouldDeleteDirectorWithAssociatedFilms() {
        // Создаём уникального режиссёра
        Director director = Director.builder()
                .name("Director With Films")
                .build();
        Director createdDirector = directorStorage.create(director);

        // Создаём уникальный фильм
        Film film = Film.builder()
                .name("Test Film For Director")
                .description("Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();
        Film createdFilm = filmStorage.create(film);

        // Связываем фильм с режиссёром
        createdFilm.setDirectors(List.of(createdDirector));
        filmStorage.update(createdFilm);

        // Проверяем, что связь есть
        Film filmWithDirector = filmStorage.findById(createdFilm.getId()).get();
        assertThat(filmWithDirector.getDirectors()).hasSize(1);
        assertThat(filmWithDirector.getDirectors().get(0).getId()).isEqualTo(createdDirector.getId());

        // Удаляем режиссёра
        directorStorage.delete(createdDirector.getId());

        // Проверяем, что режиссёр удалён
        assertFalse(directorStorage.findById(createdDirector.getId()).isPresent());

        // Проверяем, что фильм всё ещё существует
        Film filmAfterDelete = filmStorage.findById(createdFilm.getId()).get();
        assertThat(filmAfterDelete.getDirectors()).isEmpty();
    }

    @Test
    void shouldGetDirectorById() {
        Director director = Director.builder().name("James Cameron").build();
        Director created = directorStorage.create(director);

        Director found = directorStorage.getById(created.getId());

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals("James Cameron", found.getName());
    }

    @Test
    void shouldThrowExceptionWhenGetByIdNotFound() {
        assertThatThrownBy(() -> directorStorage.getById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Режиссёр с id=999 не найден");
    }
}
package ru.yandex.practicum.filmorate.storage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class GenreDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private GenreDbStorage genreStorage;

    @BeforeEach
    void setUp() {
        genreStorage = new GenreDbStorage(jdbcTemplate);
    }

    @Test
    void shouldFindAllGenres() {
        List<Genre> genres = genreStorage.findAll();

        assertThat(genres).hasSize(6);
        assertThat(genres.get(0).getId()).isEqualTo(1);
        assertThat(genres.get(0).getName()).isEqualTo("Комедия");
        assertThat(genres.get(1).getId()).isEqualTo(2);
        assertThat(genres.get(1).getName()).isEqualTo("Драма");
        assertThat(genres.get(2).getId()).isEqualTo(3);
        assertThat(genres.get(2).getName()).isEqualTo("Мультфильм");
        assertThat(genres.get(3).getId()).isEqualTo(4);
        assertThat(genres.get(3).getName()).isEqualTo("Триллер");
        assertThat(genres.get(4).getId()).isEqualTo(5);
        assertThat(genres.get(4).getName()).isEqualTo("Документальный");
        assertThat(genres.get(5).getId()).isEqualTo(6);
        assertThat(genres.get(5).getName()).isEqualTo("Боевик");
    }

    @Test
    void shouldFindGenreById() {
        Optional<Genre> genre = genreStorage.findById(1);

        assertTrue(genre.isPresent());
        assertEquals(1, genre.get().getId());
        assertEquals("Комедия", genre.get().getName());
    }

    @Test
    void shouldFindGenreById2() {
        Optional<Genre> genre = genreStorage.findById(2);

        assertTrue(genre.isPresent());
        assertEquals(2, genre.get().getId());
        assertEquals("Драма", genre.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenGenreNotFound() {
        Optional<Genre> genre = genreStorage.findById(999);
        assertFalse(genre.isPresent());
    }

    @Test
    void shouldGetGenreById() {
        Genre genre = genreStorage.getById(1);

        assertNotNull(genre);
        assertEquals(1, genre.getId());
        assertEquals("Комедия", genre.getName());
    }

    @Test
    void shouldThrowExceptionWhenGenreNotFound() {
        assertThatThrownBy(() -> genreStorage.getById(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Жанр с id=999 не найден");
    }
}
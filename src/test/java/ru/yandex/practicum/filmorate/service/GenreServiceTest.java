package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:test-data.sql")
class GenreServiceTest {

    private final GenreService genreService;

    @Test
    void shouldGetAllGenres() {
        List<Genre> genres = genreService.getAllGenres();

        assertThat(genres).isNotNull();
        assertThat(genres).hasSize(6);
        assertThat(genres.get(0).getName()).isEqualTo("Комедия");
        assertThat(genres.get(1).getName()).isEqualTo("Драма");
    }

    @Test
    void shouldGetGenreById() {
        Genre genre = genreService.getGenreById(1);

        assertThat(genre).isNotNull();
        assertThat(genre.getId()).isEqualTo(1);
        assertThat(genre.getName()).isEqualTo("Комедия");
    }

    @Test
    void shouldThrowExceptionWhenGenreNotFound() {
        Exception exception = assertThrows(NotFoundException.class, () -> {
            genreService.getGenreById(999);
        });

        assertThat(exception.getMessage()).contains("Жанр с id=999 не найден");
    }
}
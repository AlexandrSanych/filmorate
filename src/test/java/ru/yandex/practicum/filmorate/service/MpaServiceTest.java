package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:test-data.sql")
class MpaServiceTest {

    private final MpaService mpaService;

    @Test
    void shouldGetAllMpa() {
        List<Mpa> mpaList = mpaService.getAllMpa();

        assertThat(mpaList).isNotNull();
        assertThat(mpaList).hasSize(5);
        assertThat(mpaList.get(0).getName()).isEqualTo("G");
        assertThat(mpaList.get(1).getName()).isEqualTo("PG");
        assertThat(mpaList.get(2).getName()).isEqualTo("PG-13");
        assertThat(mpaList.get(3).getName()).isEqualTo("R");
        assertThat(mpaList.get(4).getName()).isEqualTo("NC-17");
    }

    @Test
    void shouldGetMpaById() {
        Mpa mpa = mpaService.getMpaById(1);

        assertThat(mpa).isNotNull();
        assertThat(mpa.getId()).isEqualTo(1);
        assertThat(mpa.getName()).isEqualTo("G");
    }

    @Test
    void shouldThrowExceptionWhenMpaNotFound() {
        Exception exception = assertThrows(NotFoundException.class, () -> {
            mpaService.getMpaById(999);
        });

        assertThat(exception.getMessage()).contains("Рейтинг MPA с id=999 не найден");
    }
}
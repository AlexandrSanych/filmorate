package ru.yandex.practicum.filmorate.storage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MpaDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MpaDbStorage mpaStorage;

    @BeforeEach
    void setUp() {
        mpaStorage = new MpaDbStorage(jdbcTemplate);
    }

    @Test
    void shouldFindAllMpa() {
        List<Mpa> mpaList = mpaStorage.findAll();

        assertThat(mpaList).hasSize(5);
        assertThat(mpaList.get(0).getId()).isEqualTo(1);
        assertThat(mpaList.get(0).getName()).isEqualTo("G");
        assertThat(mpaList.get(1).getId()).isEqualTo(2);
        assertThat(mpaList.get(1).getName()).isEqualTo("PG");
        assertThat(mpaList.get(2).getId()).isEqualTo(3);
        assertThat(mpaList.get(2).getName()).isEqualTo("PG-13");
        assertThat(mpaList.get(3).getId()).isEqualTo(4);
        assertThat(mpaList.get(3).getName()).isEqualTo("R");
        assertThat(mpaList.get(4).getId()).isEqualTo(5);
        assertThat(mpaList.get(4).getName()).isEqualTo("NC-17");
    }

    @Test
    void shouldFindMpaById() {
        Optional<Mpa> mpa = mpaStorage.findById(1);

        assertTrue(mpa.isPresent());
        assertEquals(1, mpa.get().getId());
        assertEquals("G", mpa.get().getName());
    }

    @Test
    void shouldFindMpaById3() {
        Optional<Mpa> mpa = mpaStorage.findById(3);

        assertTrue(mpa.isPresent());
        assertEquals(3, mpa.get().getId());
        assertEquals("PG-13", mpa.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenMpaNotFound() {
        Optional<Mpa> mpa = mpaStorage.findById(999);
        assertFalse(mpa.isPresent());
    }

    @Test
    void shouldGetMpaById() {
        Mpa mpa = mpaStorage.getById(1);

        assertNotNull(mpa);
        assertEquals(1, mpa.getId());
        assertEquals("G", mpa.getName());
    }

    @Test
    void shouldThrowExceptionWhenMpaNotFound() {
        assertThatThrownBy(() -> mpaStorage.getById(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Рейтинг MPA с id=999 не найден");
    }
}
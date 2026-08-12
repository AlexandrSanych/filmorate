package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Director> findAll() {
        String sql = "SELECT * FROM directors ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToDirector);
    }

    @Override
    public Optional<Director> findById(Long id) {
        String sql = "SELECT * FROM directors WHERE id = ?";
        List<Director> directors = jdbcTemplate.query(sql, this::mapRowToDirector, id);
        return directors.stream().findFirst();
    }

    @Override
    public Director create(Director director) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("directors")
                .usingGeneratedKeyColumns("id");

        Map<String, Object> params = new HashMap<>();
        params.put("name", director.getName());

        Number key = insert.executeAndReturnKey(params);
        director.setId(key.longValue());

        log.debug("Создан режиссёр: id={}, name={}", director.getId(), director.getName());
        return director;
    }

    @Override
    public Director update(Director director) {
        if (findById(director.getId()).isEmpty()) {
            throw new NotFoundException("Режиссёр с id=" + director.getId() + " не найден");
        }
        String sql = "UPDATE directors SET name = ? WHERE id = ?";
        jdbcTemplate.update(sql, director.getName(), director.getId());
        log.debug("Обновлён режиссёр: id={}", director.getId());
        return director;
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM directors WHERE id = ?", id);
        log.debug("Удалён режиссёр: id={}", id);
    }

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        return Director.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .build();
    }

    @Override
    public Director getById(Long id) {
        return findById(id)
                .orElseThrow(() -> new NotFoundException("Режиссёр с id=" + id + " не найден"));
    }
}
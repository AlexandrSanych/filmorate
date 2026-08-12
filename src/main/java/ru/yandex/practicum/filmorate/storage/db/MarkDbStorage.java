package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mark;
import ru.yandex.practicum.filmorate.storage.MarkStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MarkDbStorage implements MarkStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Mark addMark(Long filmId, Long userId, Integer markValue) {
        Optional<Mark> existing = getMark(filmId, userId);
        if (existing.isPresent()) {
            return updateMark(filmId, userId, markValue);
        }

        String sql = "INSERT INTO marks (film_id, user_id, mark_value) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, filmId, userId, markValue);

        log.debug("Пользователь {} поставил оценку {} фильму {}", userId, markValue, filmId);
        return Mark.builder()
                .filmId(filmId)
                .userId(userId)
                .markValue(markValue)
                .build();
    }

    @Override
    public Mark updateMark(Long filmId, Long userId, Integer markValue) {
        String sql = "UPDATE marks SET mark_value = ? WHERE film_id = ? AND user_id = ?";
        int updated = jdbcTemplate.update(sql, markValue, filmId, userId);

        if (updated == 0) {
            throw new NotFoundException("Оценка для фильма " + filmId + " от пользователя " + userId + " не найдена");
        }

        log.debug("Пользователь {} обновил оценку для фильма {}: {}", userId, filmId, markValue);
        return Mark.builder()
                .filmId(filmId)
                .userId(userId)
                .markValue(markValue)
                .build();
    }

    @Override
    public void removeMark(Long filmId, Long userId) {
        String sql = "DELETE FROM marks WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("Пользователь {} удалил оценку с фильма {}", userId, filmId);
    }

    @Override
    public Optional<Mark> getMark(Long filmId, Long userId) {
        String sql = "SELECT * FROM marks WHERE film_id = ? AND user_id = ?";
        List<Mark> marks = jdbcTemplate.query(sql, this::mapRowToMark, filmId, userId);
        return marks.stream().findFirst();
    }

    @Override
    public List<Mark> getMarksByFilm(Long filmId) {
        String sql = "SELECT * FROM marks WHERE film_id = ?";
        return jdbcTemplate.query(sql, this::mapRowToMark, filmId);
    }

    @Override
    public List<Mark> getMarksByUser(Long userId) {
        String sql = "SELECT * FROM marks WHERE user_id = ?";
        return jdbcTemplate.query(sql, this::mapRowToMark, userId);
    }

    @Override
    public Double getAverageRating(Long filmId) {
        String sql = "SELECT AVG(mark_value) FROM marks WHERE film_id = ?";
        Double avg = jdbcTemplate.queryForObject(sql, Double.class, filmId);
        return avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0;
    }

    @Override
    public Integer getMarksCount(Long filmId) {
        String sql = "SELECT COUNT(*) FROM marks WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId);
        return count != null ? count : 0;
    }

    private Mark mapRowToMark(ResultSet rs, int rowNum) throws SQLException {
        return Mark.builder()
                .filmId(rs.getLong("film_id"))
                .userId(rs.getLong("user_id"))
                .markValue(rs.getInt("mark_value"))
                .build();
    }
}
package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Review create(Review review) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("reviews")
                .usingGeneratedKeyColumns("id");

        Map<String, Object> params = new HashMap<>();
        params.put("content", review.getContent());
        params.put("is_positive", review.getIsPositive());
        params.put("user_id", review.getUserId());
        params.put("film_id", review.getFilmId());
        params.put("useful", 0);  // ← добавить эту строку!

        Number key = insert.executeAndReturnKey(params);
        review.setReviewId(key.longValue());
        review.setUseful(0);  // ← и эту

        return review;
    }

    @Override
    public Review update(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ? WHERE id = ?";
        jdbcTemplate.update(sql, review.getContent(), review.getIsPositive(), review.getReviewId());
        return findById(review.getReviewId()).orElseThrow();
    }

    @Override
    public void delete(Long reviewId) {
        jdbcTemplate.update("DELETE FROM reviews WHERE id = ?", reviewId);
    }

    @Override
    public Optional<Review> findById(Long reviewId) {
        String sql = "SELECT * FROM reviews WHERE id = ?";
        List<Review> reviews = jdbcTemplate.query(sql, this::mapRowToReview, reviewId);
        return reviews.stream().findFirst();
    }

    @Override
    public List<Review> findAll(Long filmId, Integer count) {
        String sql;
        Object[] params;

        if (filmId != null) {
            sql = "SELECT * FROM reviews WHERE film_id = ? ORDER BY useful DESC LIMIT ?";
            params = new Object[]{filmId, count};
        } else {
            sql = "SELECT * FROM reviews ORDER BY useful DESC LIMIT ?";
            params = new Object[]{count};
        }

        return jdbcTemplate.query(sql, this::mapRowToReview, params);
    }

    @Override
    public void addRating(Long reviewId, Long userId, boolean isLike) {
        removeRating(reviewId, userId);
        String sql = "INSERT INTO review_ratings (review_id, user_id, is_like) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, reviewId, userId, isLike);
    }

    @Override
    public void removeRating(Long reviewId, Long userId) {
        jdbcTemplate.update("DELETE FROM review_ratings WHERE review_id = ? AND user_id = ?", reviewId, userId);
    }

    @Override
    public void updateUsefulScore(Long reviewId) {
        String sql = """
            UPDATE reviews 
            SET useful = (
                SELECT COALESCE(SUM(CASE WHEN is_like = true THEN 1 ELSE -1 END), 0)
                FROM review_ratings
                WHERE review_id = ?
            )
            WHERE id = ?
        """;
        jdbcTemplate.update(sql, reviewId, reviewId);
    }

    private Review mapRowToReview(ResultSet rs, int rowNum) throws SQLException {
        return Review.builder()
                .reviewId(rs.getLong("id"))
                .content(rs.getString("content"))
                .isPositive(rs.getBoolean("is_positive"))
                .userId(rs.getLong("user_id"))
                .filmId(rs.getLong("film_id"))
                .useful(rs.getInt("useful"))
                .build();
    }
}
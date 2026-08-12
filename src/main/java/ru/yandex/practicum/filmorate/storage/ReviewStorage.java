package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {
    Review create(Review review);

    Review update(Review review);

    void delete(Long reviewId);

    Optional<Review> findById(Long reviewId);

    List<Review> findAll(Long filmId, Integer count);

    void addRating(Long reviewId, Long userId, boolean isLike);

    void removeRating(Long reviewId, Long userId);

    void updateUsefulScore(Long reviewId);
}
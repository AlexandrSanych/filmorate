package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final EventService eventService;

    @Transactional
    public Review createReview(Review review) {
        validateUserExists(review.getUserId());
        validateFilmExists(review.getFilmId());

        review.setUseful(0);
        Review created = reviewStorage.create(review);

        eventService.addEvent(review.getUserId(), "REVIEW", "ADD", created.getReviewId());

        log.debug("Создан отзыв: id={}, userId={}, filmId={}", created.getReviewId(), created.getUserId(), created.getFilmId());
        return created;
    }

    @Transactional
    public Review updateReview(Review review) {
        Review existing = getReviewById(review.getReviewId());

        // ИСПРАВЛЕНО: проверка прав — нельзя редактировать чужой отзыв
        if (!existing.getUserId().equals(review.getUserId())) {
            throw new ValidationException("Нельзя редактировать чужой отзыв");
        }

        validateUserExists(review.getUserId());
        validateFilmExists(review.getFilmId());

        existing.setContent(review.getContent());
        existing.setIsPositive(review.getIsPositive());

        Review updated = reviewStorage.update(existing);

        eventService.addEvent(review.getUserId(), "REVIEW", "UPDATE", updated.getReviewId());

        log.debug("Обновлен отзыв: id={}", updated.getReviewId());
        return updated;
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review existing = getReviewById(reviewId);

        reviewStorage.delete(reviewId);

        eventService.addEvent(existing.getUserId(), "REVIEW", "REMOVE", reviewId);

        log.debug("Удален отзыв: id={}, userId={}", reviewId, existing.getUserId());
    }

    @Transactional(readOnly = true)
    public Review getReviewById(Long reviewId) {
        return reviewStorage.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Отзыв с id=" + reviewId + " не найден"));
    }

    @Transactional(readOnly = true)
    public List<Review> getReviews(Long filmId, Integer count) {
        return reviewStorage.findAll(filmId, count);
    }

    @Transactional
    public void addLike(Long reviewId, Long userId) {
        validateUserExists(userId);
        getReviewById(reviewId);
        reviewStorage.addRating(reviewId, userId, true);
        reviewStorage.updateUsefulScore(reviewId);
        log.debug("Пользователь {} поставил лайк отзыву {}", userId, reviewId);
    }

    @Transactional
    public void addDislike(Long reviewId, Long userId) {
        validateUserExists(userId);
        getReviewById(reviewId);
        reviewStorage.addRating(reviewId, userId, false);
        reviewStorage.updateUsefulScore(reviewId);
        log.debug("Пользователь {} поставил дизлайк отзыву {}", userId, reviewId);
    }

    @Transactional
    public void removeRating(Long reviewId, Long userId) {
        validateUserExists(userId);
        getReviewById(reviewId);
        reviewStorage.removeRating(reviewId, userId);
        reviewStorage.updateUsefulScore(reviewId);
        log.debug("Пользователь {} удалил оценку с отзыва {}", userId, reviewId);
    }

    private void validateUserExists(Long userId) {
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private void validateFilmExists(Long filmId) {
        filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));
    }
}
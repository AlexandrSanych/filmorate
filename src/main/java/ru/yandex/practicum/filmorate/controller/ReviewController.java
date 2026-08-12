package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Review create(@Valid @RequestBody Review review) {
        log.info("POST /reviews - создание отзыва");
        return reviewService.createReview(review);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public Review update(@Valid @RequestBody Review review) {
        log.info("PUT /reviews - обновление отзыва с id={}", review.getReviewId());
        return reviewService.updateReview(review);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        log.info("DELETE /reviews/{} - удаление отзыва", id);
        reviewService.deleteReview(id);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Review getById(@PathVariable Long id) {
        log.info("GET /reviews/{} - получение отзыва", id);
        return reviewService.getReviewById(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Review> getReviews(
            @RequestParam(required = false) Long filmId,
            @RequestParam(defaultValue = "10") Integer count) {
        log.info("GET /reviews - filmId={}, count={}", filmId, count);
        return reviewService.getReviews(filmId, count);
    }

    @PutMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("PUT /reviews/{}/like/{} - добавление лайка", id, userId);
        reviewService.addLike(id, userId);
    }

    @PutMapping("/{id}/dislike/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void addDislike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("PUT /reviews/{}/dislike/{} - добавление дизлайка", id, userId);
        reviewService.addDislike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("DELETE /reviews/{}/like/{} - удаление лайка", id, userId);
        reviewService.removeRating(id, userId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDislike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("DELETE /reviews/{}/dislike/{} - удаление дизлайка", id, userId);
        reviewService.removeRating(id, userId);
    }
}
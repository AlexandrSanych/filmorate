package ru.yandex.practicum.filmorate.storage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"classpath:schema.sql", "classpath:test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ReviewDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ReviewDbStorage reviewStorage;
    private UserDbStorage userStorage;
    private FilmDbStorage filmStorage;
    private MarkDbStorage markStorage;

    private Long existingUserId;
    private Long existingFilmId;

    @BeforeEach
    void setUp() {
        markStorage = new MarkDbStorage(jdbcTemplate);
        reviewStorage = new ReviewDbStorage(jdbcTemplate);
        userStorage = new UserDbStorage(jdbcTemplate);
        filmStorage = new FilmDbStorage(jdbcTemplate, markStorage);

        // Очистка
        jdbcTemplate.execute("DELETE FROM review_ratings");
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM marks");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM film_director");
        jdbcTemplate.execute("DELETE FROM directors");
        jdbcTemplate.execute("DELETE FROM review_ratings");
        jdbcTemplate.execute("DELETE FROM reviews");

        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE reviews ALTER COLUMN id RESTART WITH 1");

        // Создаём тестового пользователя
        User user = User.builder()
                .email("reviewer@test.com")
                .login("reviewer")
                .name("Reviewer")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User createdUser = userStorage.create(user);
        existingUserId = createdUser.getId();

        // Создаём тестовый фильм
        Film film = Film.builder()
                .name("Review Film")
                .description("Film for review testing")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();
        Film createdFilm = filmStorage.create(film);
        existingFilmId = createdFilm.getId();
    }

    @Test
    void shouldCreateReview() {
        Review review = Review.builder()
                .content("Great movie!")
                .isPositive(true)
                .userId(existingUserId)
                .filmId(existingFilmId)
                .build();

        Review created = reviewStorage.create(review);

        assertNotNull(created.getReviewId());
        assertEquals(1L, created.getReviewId());
        assertEquals("Great movie!", created.getContent());
        assertTrue(created.getIsPositive());
        assertEquals(0, created.getUseful());  // ← должно быть 0, не null
        assertEquals(existingUserId, created.getUserId());
        assertEquals(existingFilmId, created.getFilmId());
    }

    @Test
    void shouldUpdateReview() {
        Review review = Review.builder()
                .content("Original content")
                .isPositive(true)
                .userId(existingUserId)
                .filmId(existingFilmId)
                .build();
        Review created = reviewStorage.create(review);

        created.setContent("Updated content");
        created.setIsPositive(false);
        Review updated = reviewStorage.update(created);

        assertEquals("Updated content", updated.getContent());
        assertFalse(updated.getIsPositive());
    }

    @Test
    void shouldDeleteReview() {
        Review review = Review.builder()
                .content("To delete")
                .isPositive(true)
                .userId(existingUserId)
                .filmId(existingFilmId)
                .build();
        Review created = reviewStorage.create(review);

        assertTrue(reviewStorage.findById(created.getReviewId()).isPresent());

        reviewStorage.delete(created.getReviewId());

        assertFalse(reviewStorage.findById(created.getReviewId()).isPresent());
    }

    @Test
    void shouldFindReviewById() {
        Review review = Review.builder()
                .content("Find me")
                .isPositive(true)
                .userId(existingUserId)
                .filmId(existingFilmId)
                .build();
        Review created = reviewStorage.create(review);

        Optional<Review> found = reviewStorage.findById(created.getReviewId());

        assertTrue(found.isPresent());
        assertEquals("Find me", found.get().getContent());
    }

    @Test
    void shouldReturnEmptyWhenReviewNotFound() {
        Optional<Review> found = reviewStorage.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void shouldFindAllReviewsWithoutFilmId() {
        createTestReview("Review 1");
        createTestReview("Review 2");
        createTestReview("Review 3");

        List<Review> reviews = reviewStorage.findAll(null, 10);

        assertThat(reviews).hasSize(3);
        assertThat(reviews).extracting(Review::getContent)
                .containsExactlyInAnyOrder("Review 1", "Review 2", "Review 3");
    }

    @Test
    void shouldFindReviewsByFilmId() {
        // Создаём другой фильм
        Film anotherFilm = Film.builder()
                .name("Another Film")
                .description("Another film for reviews")
                .releaseDate(LocalDate.of(2020, 2, 2))
                .duration(90)
                .mpa(Mpa.builder().id(2).build())
                .build();
        Film createdAnotherFilm = filmStorage.create(anotherFilm);

        // Отзывы для первого фильма
        Review review1 = createTestReview("For Film 1 - Good");
        Review review2 = createTestReview("For Film 1 - Bad", false);
        // Отзыв для другого фильма
        createTestReview("For Another Film", true, existingUserId, createdAnotherFilm.getId());

        List<Review> reviewsForFilm1 = reviewStorage.findAll(existingFilmId, 10);

        assertThat(reviewsForFilm1).hasSize(2);
        assertThat(reviewsForFilm1).extracting(Review::getContent)
                .containsExactlyInAnyOrder("For Film 1 - Good", "For Film 1 - Bad");
    }

    @Test
    void shouldLimitReviewsByCount() {
        for (int i = 1; i <= 15; i++) {
            createTestReview("Review " + i);
        }

        List<Review> reviews = reviewStorage.findAll(null, 10);

        assertThat(reviews).hasSize(10);
    }

    @Test
    void shouldAddLikeToReview() {
        Review review = createTestReview("Likeable review");

        reviewStorage.addRating(review.getReviewId(), existingUserId, true);
        reviewStorage.updateUsefulScore(review.getReviewId());

        Review updated = reviewStorage.findById(review.getReviewId()).get();
        assertEquals(1, updated.getUseful());
    }

    @Test
    void shouldAddDislikeToReview() {
        Review review = createTestReview("Dislikeable review");

        reviewStorage.addRating(review.getReviewId(), existingUserId, false);
        reviewStorage.updateUsefulScore(review.getReviewId());

        Review updated = reviewStorage.findById(review.getReviewId()).get();
        assertEquals(-1, updated.getUseful());
    }

    @Test
    void shouldRemoveRatingFromReview() {
        Review review = createTestReview("Rating to remove");

        reviewStorage.addRating(review.getReviewId(), existingUserId, true);
        reviewStorage.updateUsefulScore(review.getReviewId());

        Review afterAdd = reviewStorage.findById(review.getReviewId()).get();
        assertEquals(1, afterAdd.getUseful());

        reviewStorage.removeRating(review.getReviewId(), existingUserId);
        reviewStorage.updateUsefulScore(review.getReviewId());

        Review afterRemove = reviewStorage.findById(review.getReviewId()).get();
        assertEquals(0, afterRemove.getUseful());
    }

    @Test
    void shouldToggleRatingFromLikeToDislike() {
        Review review = createTestReview("Toggle rating");

        // Добавляем лайк
        reviewStorage.addRating(review.getReviewId(), existingUserId, true);
        reviewStorage.updateUsefulScore(review.getReviewId());
        assertEquals(1, reviewStorage.findById(review.getReviewId()).get().getUseful());

        // Меняем на дизлайк
        reviewStorage.addRating(review.getReviewId(), existingUserId, false);
        reviewStorage.updateUsefulScore(review.getReviewId());
        assertEquals(-1, reviewStorage.findById(review.getReviewId()).get().getUseful());
    }

    @Test
    void shouldSortReviewsByUsefulDesc() {
        Review review1 = createTestReview("Popular review");
        Review review2 = createTestReview("Unpopular review");

        // review1 получает 2 лайка
        reviewStorage.addRating(review1.getReviewId(), existingUserId, true);
        reviewStorage.updateUsefulScore(review1.getReviewId());

        // Создаём второго пользователя
        User user2 = User.builder()
                .email("user2@test.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();
        User createdUser2 = userStorage.create(user2);

        reviewStorage.addRating(review1.getReviewId(), createdUser2.getId(), true);
        reviewStorage.updateUsefulScore(review1.getReviewId());

        // review2 получает дизлайк
        reviewStorage.addRating(review2.getReviewId(), existingUserId, false);
        reviewStorage.updateUsefulScore(review2.getReviewId());

        List<Review> reviews = reviewStorage.findAll(null, 10);

        assertThat(reviews).hasSize(2);
        assertThat(reviews.get(0).getReviewId()).isEqualTo(review1.getReviewId());
        assertThat(reviews.get(0).getUseful()).isEqualTo(2);
        assertThat(reviews.get(1).getReviewId()).isEqualTo(review2.getReviewId());
        assertThat(reviews.get(1).getUseful()).isEqualTo(-1);
    }

    private Review createTestReview(String content) {
        return createTestReview(content, true, existingUserId, existingFilmId);
    }

    private Review createTestReview(String content, boolean isPositive) {
        return createTestReview(content, isPositive, existingUserId, existingFilmId);
    }

    private Review createTestReview(String content, boolean isPositive, Long userId, Long filmId) {
        Review review = Review.builder()
                .content(content)
                .isPositive(isPositive)
                .userId(userId)
                .filmId(filmId)
                .build();
        return reviewStorage.create(review);
    }
}
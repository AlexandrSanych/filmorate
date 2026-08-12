package ru.yandex.practicum.filmorate.storage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"classpath:schema.sql", "classpath:test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EventDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private EventDbStorage eventStorage;
    private UserDbStorage userStorage;

    private Long existingUserId;

    @BeforeEach
    void setUp() {
        eventStorage = new EventDbStorage(jdbcTemplate);
        userStorage = new UserDbStorage(jdbcTemplate);

        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE events ALTER COLUMN id RESTART WITH 1");

        // Создаём тестового пользователя
        User user = User.builder()
                .email("eventuser@test.com")
                .login("eventuser")
                .name("Event User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User createdUser = userStorage.create(user);
        existingUserId = createdUser.getId();
    }

    @Test
    void shouldCreateEvent() {
        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(existingUserId)
                .eventType("LIKE")
                .operation("ADD")
                .entityId(100L)
                .build();

        Event created = eventStorage.create(event);

        assertThat(created.getEventId()).isNotNull();
        assertThat(created.getUserId()).isEqualTo(existingUserId);
        assertThat(created.getEventType()).isEqualTo("LIKE");
        assertThat(created.getOperation()).isEqualTo("ADD");
        assertThat(created.getEntityId()).isEqualTo(100L);
    }

    @Test
    void shouldFindEventsByUserId() {
        // Создаём несколько событий
        createTestEvent(existingUserId, "LIKE", "ADD", 1L);
        createTestEvent(existingUserId, "REVIEW", "ADD", 2L);
        createTestEvent(existingUserId, "FRIEND", "ADD", 3L);
        createTestEvent(existingUserId, "LIKE", "REMOVE", 1L);

        List<Event> events = eventStorage.findByUserId(existingUserId);

        assertThat(events).hasSize(4);
        assertThat(events).extracting(Event::getEventType)
                .containsExactlyInAnyOrder("LIKE", "REVIEW", "FRIEND", "LIKE");
        assertThat(events).extracting(Event::getOperation)
                .containsExactlyInAnyOrder("ADD", "ADD", "ADD", "REMOVE");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoEvents() {
        // Создаём другого пользователя без событий
        User anotherUser = User.builder()
                .email("noevents@test.com")
                .login("noevents")
                .name("No Events")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();
        User createdAnother = userStorage.create(anotherUser);

        List<Event> events = eventStorage.findByUserId(createdAnother.getId());

        assertThat(events).isEmpty();
    }

    @Test
    void shouldOrderEventsByTimestampDesc() {
        long timestamp1 = System.currentTimeMillis();
        sleep(1);
        long timestamp2 = System.currentTimeMillis();
        sleep(1);
        long timestamp3 = System.currentTimeMillis();

        Event event1 = createTestEventWithTimestamp(existingUserId, "LIKE", "ADD", 1L, timestamp1);
        Event event2 = createTestEventWithTimestamp(existingUserId, "LIKE", "ADD", 2L, timestamp2);
        Event event3 = createTestEventWithTimestamp(existingUserId, "LIKE", "ADD", 3L, timestamp3);

        List<Event> events = eventStorage.findByUserId(existingUserId);

        assertThat(events).hasSize(3);
        // Ожидаем порядок: сначала более новые (больший timestamp)
        assertThat(events.get(0).getTimestamp()).isEqualTo(timestamp3);
        assertThat(events.get(1).getTimestamp()).isEqualTo(timestamp2);
        assertThat(events.get(2).getTimestamp()).isEqualTo(timestamp1);
    }

    @Test
    void shouldStoreAllEventTypes() {
        // LIKE
        createTestEvent(existingUserId, "LIKE", "ADD", 10L);
        createTestEvent(existingUserId, "LIKE", "REMOVE", 10L);

        // REVIEW
        createTestEvent(existingUserId, "REVIEW", "ADD", 20L);
        createTestEvent(existingUserId, "REVIEW", "UPDATE", 20L);
        createTestEvent(existingUserId, "REVIEW", "REMOVE", 20L);

        // FRIEND
        createTestEvent(existingUserId, "FRIEND", "ADD", 30L);
        createTestEvent(existingUserId, "FRIEND", "REMOVE", 30L);

        List<Event> events = eventStorage.findByUserId(existingUserId);

        assertThat(events).hasSize(7);
        assertThat(events).extracting(Event::getEventType)
                .containsExactlyInAnyOrder("LIKE", "LIKE", "REVIEW", "REVIEW", "REVIEW", "FRIEND", "FRIEND");
        assertThat(events).extracting(Event::getOperation)
                .containsExactlyInAnyOrder("ADD", "REMOVE", "ADD", "UPDATE", "REMOVE", "ADD", "REMOVE");
    }

    @Test
    void shouldNotMixEventsBetweenUsers() {
        // Создаём второго пользователя
        User user2 = User.builder()
                .email("user2@test.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();
        User createdUser2 = userStorage.create(user2);

        // События для первого пользователя
        createTestEvent(existingUserId, "LIKE", "ADD", 1L);
        createTestEvent(existingUserId, "REVIEW", "ADD", 2L);

        // События для второго пользователя
        createTestEvent(createdUser2.getId(), "FRIEND", "ADD", 3L);
        createTestEvent(createdUser2.getId(), "FRIEND", "REMOVE", 3L);

        List<Event> eventsForUser1 = eventStorage.findByUserId(existingUserId);
        List<Event> eventsForUser2 = eventStorage.findByUserId(createdUser2.getId());

        assertThat(eventsForUser1).hasSize(2);
        assertThat(eventsForUser1).allMatch(e -> e.getUserId().equals(existingUserId));

        assertThat(eventsForUser2).hasSize(2);
        assertThat(eventsForUser2).allMatch(e -> e.getUserId().equals(createdUser2.getId()));
    }

    private void createTestEvent(Long userId, String eventType, String operation, Long entityId) {
        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(userId)
                .eventType(eventType)
                .operation(operation)
                .entityId(entityId)
                .build();
        eventStorage.create(event);
    }

    private Event createTestEventWithTimestamp(Long userId, String eventType, String operation, Long entityId, long timestamp) {
        Event event = Event.builder()
                .timestamp(timestamp)
                .userId(userId)
                .eventType(eventType)
                .operation(operation)
                .entityId(entityId)
                .build();
        return eventStorage.create(event);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
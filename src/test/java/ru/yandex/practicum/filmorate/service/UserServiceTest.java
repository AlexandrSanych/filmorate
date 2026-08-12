package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.storage.db.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserService userService;

    @BeforeEach
    void setUp() {
        // Создаём зависимости
        UserStorage userStorage = new UserDbStorage(jdbcTemplate);
        EventStorage eventStorage = new EventDbStorage(jdbcTemplate);
        EventService eventService = new EventService(eventStorage, userStorage);

        // Исправленный конструктор: теперь передаём eventService
        userService = new UserService(userStorage, eventService);

        // Очистка таблиц
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM marks");
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM films");

        // Сброс автоинкремента
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE events ALTER COLUMN id RESTART WITH 1");
    }

    // ==================== ТЕСТЫ НА УНИКАЛЬНОСТЬ ====================

    @Test
    void createUser_ShouldThrowException_WhenEmailAlreadyExists() {
        User user1 = User.builder()
                .email("existing@test.com")
                .login("login1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User user2 = User.builder()
                .email("existing@test.com")
                .login("login2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();

        userService.createUser(user1);
        assertThrows(DuplicateException.class, () -> userService.createUser(user2));
    }

    @Test
    void createUser_ShouldThrowException_WhenLoginAlreadyExists() {
        User user1 = User.builder()
                .email("email1@test.com")
                .login("existingLogin")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User user2 = User.builder()
                .email("email2@test.com")
                .login("existingLogin")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();

        userService.createUser(user1);
        assertThrows(DuplicateException.class, () -> userService.createUser(user2));
    }

    @Test
    void updateUser_ShouldThrowException_WhenEmailAlreadyExists() {
        User user1 = User.builder()
                .email("email1@test.com")
                .login("login1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User user2 = User.builder()
                .email("email2@test.com")
                .login("login2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();

        User created1 = userService.createUser(user1);
        User created2 = userService.createUser(user2);

        created2.setEmail("email1@test.com");
        assertThrows(DuplicateException.class, () -> userService.updateUser(created2));
    }

    @Test
    void updateUser_ShouldThrowException_WhenLoginAlreadyExists() {
        User user1 = User.builder()
                .email("email1@test.com")
                .login("login1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User user2 = User.builder()
                .email("email2@test.com")
                .login("login2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();

        User created1 = userService.createUser(user1);
        User created2 = userService.createUser(user2);

        created2.setLogin("login1");
        assertThrows(DuplicateException.class, () -> userService.updateUser(created2));
    }

    @Test
    void updateUser_ShouldWork_WhenUpdatingWithSameEmail() {
        User user = User.builder()
                .email("email@test.com")
                .login("login")
                .name("Original Name")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User created = userService.createUser(user);

        created.setName("New Name");
        User updated = userService.updateUser(created);

        assertEquals("email@test.com", updated.getEmail());
        assertEquals("New Name", updated.getName());
        assertDoesNotThrow(() -> userService.updateUser(created));
    }

    // ==================== ОСТАЛЬНЫЕ ТЕСТЫ ====================

    @Test
    void createUser_ShouldSetNameAsLogin_WhenNameIsBlank() {
        User user = User.builder()
                .email("test@example.com")
                .login("testlogin")
                .name("")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User created = userService.createUser(user);
        assertEquals("testlogin", created.getName());
    }

    @Test
    void createUser_ShouldSetNameAsLogin_WhenNameIsNull() {
        User user = User.builder()
                .email("test@example.com")
                .login("testlogin")
                .name(null)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User created = userService.createUser(user);
        assertEquals("testlogin", created.getName());
    }

    @Test
    void createUser_ShouldKeepOriginalName_WhenNameIsProvided() {
        User user = User.builder()
                .email("test@example.com")
                .login("testlogin")
                .name("Original Name")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User created = userService.createUser(user);
        assertEquals("Original Name", created.getName());
    }

    @Test
    void updateUser_ShouldSetNameAsLogin_WhenNameIsBlank() {
        User user = User.builder()
                .email("test@example.com")
                .login("testlogin")
                .name("Original Name")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User created = userService.createUser(user);

        created.setName("");
        User updated = userService.updateUser(created);
        assertEquals("testlogin", updated.getName());
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailIsInvalid() {
        User user = User.builder()
                .email("invalid-email")
                .login("testlogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        assertThrows(ValidationException.class, () -> userService.createUser(user));
    }

    @Test
    void updateUser_ShouldValidateUser() {
        User user = User.builder()
                .email("test@example.com")
                .login("testlogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User created = userService.createUser(user);

        created.setEmail("invalid-email");
        assertThrows(ValidationException.class, () -> userService.updateUser(created));
    }

    @Test
    void updateUser_ShouldThrowException_WhenUserNotFound() {
        User user = User.builder()
                .id(999L)
                .email("test@example.com")
                .login("testlogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        assertThrows(NotFoundException.class, () -> userService.updateUser(user));
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        assertThrows(NotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    void deleteUser_ShouldWork() {
        User user = User.builder()
                .email("delete@test.com")
                .login("deleteuser")
                .name("Delete User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User created = userService.createUser(user);
        assertDoesNotThrow(() -> userService.getUserById(created.getId()));

        userService.deleteUser(created.getId());
        assertThrows(NotFoundException.class, () -> userService.getUserById(created.getId()));
    }

    @Test
    void addFriend_ShouldThrowException_WhenAddingSelf() {
        User user = User.builder()
                .email("user@example.com")
                .login("user")
                .name("User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User created = userService.createUser(user);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.addFriend(created.getId(), created.getId()));
        assertTrue(exception.getMessage().contains("самого себя"));
    }

    @Test
    void addFriend_ShouldThrowException_WhenUserNotFound() {
        User user = User.builder()
                .email("user@example.com")
                .login("user")
                .name("User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User created = userService.createUser(user);

        assertThrows(NotFoundException.class,
                () -> userService.addFriend(created.getId(), 999L));
    }

    @Test
    void addFriend_ShouldWorkWhenBothExist() {
        User user1 = User.builder()
                .email("user1@example.com")
                .login("user1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User user2 = User.builder()
                .email("user2@example.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();

        User created1 = userService.createUser(user1);
        User created2 = userService.createUser(user2);

        assertDoesNotThrow(() -> userService.addFriend(created1.getId(), created2.getId()));
    }

    @Test
    void removeFriend_ShouldWork() {
        User user1 = User.builder()
                .email("user1@example.com")
                .login("user1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User user2 = User.builder()
                .email("user2@example.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();

        User created1 = userService.createUser(user1);
        User created2 = userService.createUser(user2);

        userService.addFriend(created1.getId(), created2.getId());
        assertDoesNotThrow(() -> userService.removeFriend(created1.getId(), created2.getId()));
    }
}
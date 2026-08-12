package ru.yandex.practicum.filmorate.storage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserDbStorage userStorage;

    @BeforeEach
    void setUp() {
        userStorage = new UserDbStorage(jdbcTemplate);

        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
    }

    // ==================== ТЕСТЫ НА УНИКАЛЬНОСТЬ ====================

    @Test
    void shouldNotCreateUserWithDuplicateEmail() {
        // given
        User user1 = createTestUser("unique@test.com", "login1");
        User user2 = createTestUser("unique@test.com", "login2");

        // when
        userStorage.create(user1);

        // then
        assertThatThrownBy(() -> userStorage.create(user2))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("email");
    }

    @Test
    void shouldNotCreateUserWithDuplicateLogin() {
        // given
        User user1 = createTestUser("email1@test.com", "uniqueLogin");
        User user2 = createTestUser("email2@test.com", "uniqueLogin");

        // when
        userStorage.create(user1);

        // then
        assertThatThrownBy(() -> userStorage.create(user2))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("login");
    }

    @Test
    void shouldNotUpdateUserWithDuplicateEmail() {
        // given
        User user1 = createTestUser("email1@test.com", "login1");
        User user2 = createTestUser("email2@test.com", "login2");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        // when: пытаемся обновить user2, задав ему email user1
        created2.setEmail("email1@test.com");

        // then
        assertThatThrownBy(() -> userStorage.update(created2))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("email");
    }

    @Test
    void shouldNotUpdateUserWithDuplicateLogin() {
        // given
        User user1 = createTestUser("email1@test.com", "login1");
        User user2 = createTestUser("email2@test.com", "login2");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        // when: пытаемся обновить user2, задав ему login user1
        created2.setLogin("login1");

        // then
        assertThatThrownBy(() -> userStorage.update(created2))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("login");
    }

    @Test
    void shouldUpdateUserWithSameEmail() {
        // given
        User user = createTestUser("email@test.com", "login");
        User created = userStorage.create(user);

        // when: обновляем с тем же email (должно работать)
        created.setName("New Name");
        User updated = userStorage.update(created);

        // then
        assertEquals("email@test.com", updated.getEmail());
        assertEquals("New Name", updated.getName());
    }

    @Test
    void shouldUpdateUserWithSameLogin() {
        // given
        User user = createTestUser("email@test.com", "login");
        User created = userStorage.create(user);

        // when: обновляем с тем же login (должно работать)
        created.setName("New Name");
        User updated = userStorage.update(created);

        // then
        assertEquals("login", updated.getLogin());
        assertEquals("New Name", updated.getName());
    }

    // ==================== ОСТАЛЬНЫЕ ТЕСТЫ ====================

    @Test
    void shouldCreateUser() {
        User user = createTestUser();

        User created = userStorage.create(user);

        assertNotNull(created.getId());
        assertEquals(1L, created.getId());
        assertEquals("test@test.com", created.getEmail());
        assertEquals("testlogin", created.getLogin());
        assertEquals("Test User", created.getName());
        assertEquals(LocalDate.of(1990, 1, 1), created.getBirthday());
    }

    @Test
    void shouldCreateUserWithEmptyName() {
        User user = User.builder()
                .email("test@test.com")
                .login("testlogin")
                .name(null)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User created = userStorage.create(user);

        assertNotNull(created.getId());
        assertNull(created.getName());
    }

    @Test
    void shouldUpdateUser() {
        User user = createTestUser();
        User created = userStorage.create(user);

        created.setName("Updated Name");
        created.setEmail("updated@test.com");

        User updated = userStorage.update(created);

        assertEquals("Updated Name", updated.getName());
        assertEquals("updated@test.com", updated.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        User user = createTestUser();
        user.setId(999L);

        assertThatThrownBy(() -> userStorage.update(user))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=999 не найден");
    }

    @Test
    void shouldFindAllUsers() {
        User user1 = createTestUser("user1@test.com", "user1");
        User user2 = createTestUser("user2@test.com", "user2");

        userStorage.create(user1);
        userStorage.create(user2);

        List<User> users = userStorage.findAll();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getEmail)
                .containsExactly("user1@test.com", "user2@test.com");
    }

    @Test
    void shouldFindUserById() {
        User user = createTestUser();
        User created = userStorage.create(user);

        Optional<User> found = userStorage.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals("test@test.com", found.get().getEmail());
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        Optional<User> found = userStorage.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void shouldAddFriend() {
        User user1 = createTestUser("user1@test.com", "user1");
        User user2 = createTestUser("user2@test.com", "user2");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        userStorage.addFriend(created1.getId(), created2.getId());

        List<User> friends = userStorage.getFriends(created1.getId());
        assertThat(friends).hasSize(1);
        assertEquals(created2.getId(), friends.get(0).getId());
    }

    @Test
    void shouldNotAddDuplicateFriend() {
        User user1 = createTestUser("user1@test.com", "user1");
        User user2 = createTestUser("user2@test.com", "user2");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        userStorage.addFriend(created1.getId(), created2.getId());
        userStorage.addFriend(created1.getId(), created2.getId());

        List<User> friends = userStorage.getFriends(created1.getId());
        assertThat(friends).hasSize(1);
    }

    @Test
    void shouldRemoveFriend() {
        User user1 = createTestUser("user1@test.com", "user1");
        User user2 = createTestUser("user2@test.com", "user2");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        userStorage.addFriend(created1.getId(), created2.getId());
        userStorage.removeFriend(created1.getId(), created2.getId());

        List<User> friends = userStorage.getFriends(created1.getId());
        assertThat(friends).isEmpty();
    }

    @Test
    void shouldGetCommonFriends() {
        User user1 = createTestUser("user1@test.com", "user1");
        User user2 = createTestUser("user2@test.com", "user2");
        User commonFriend = createTestUser("common@test.com", "common");
        User otherFriend = createTestUser("other@test.com", "other");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);
        User createdCommon = userStorage.create(commonFriend);
        User createdOther = userStorage.create(otherFriend);

        userStorage.addFriend(created1.getId(), createdCommon.getId());
        userStorage.addFriend(created1.getId(), createdOther.getId());
        userStorage.addFriend(created2.getId(), createdCommon.getId());

        List<User> commonFriends = userStorage.getCommonFriends(created1.getId(), created2.getId());

        assertThat(commonFriends).hasSize(1);
        assertEquals(createdCommon.getId(), commonFriends.get(0).getId());
    }

    @Test
    void shouldReturnEmptyWhenNoCommonFriends() {
        User user1 = createTestUser("user1@test.com", "user1");
        User user2 = createTestUser("user2@test.com", "user2");
        User friend1 = createTestUser("friend1@test.com", "friend1");
        User friend2 = createTestUser("friend2@test.com", "friend2");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);
        User createdFriend1 = userStorage.create(friend1);
        User createdFriend2 = userStorage.create(friend2);

        userStorage.addFriend(created1.getId(), createdFriend1.getId());
        userStorage.addFriend(created2.getId(), createdFriend2.getId());

        List<User> commonFriends = userStorage.getCommonFriends(created1.getId(), created2.getId());

        assertThat(commonFriends).isEmpty();
    }

    @Test
    void shouldLoadFriendsForAllUsers() {
        User user1 = createTestUser("user1@test.com", "user1");
        User user2 = createTestUser("user2@test.com", "user2");
        User user3 = createTestUser("user3@test.com", "user3");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);
        User created3 = userStorage.create(user3);

        userStorage.addFriend(created1.getId(), created2.getId());
        userStorage.addFriend(created1.getId(), created3.getId());
        userStorage.addFriend(created2.getId(), created3.getId());

        List<User> allUsers = userStorage.findAll();

        for (User user : allUsers) {
            assertNotNull(user.getFriends());
        }

        User found1 = allUsers.stream().filter(u -> u.getId().equals(created1.getId())).findFirst().get();
        assertThat(found1.getFriends()).hasSize(2);
        assertTrue(found1.getFriends().contains(created2.getId()));
        assertTrue(found1.getFriends().contains(created3.getId()));

        User found2 = allUsers.stream().filter(u -> u.getId().equals(created2.getId())).findFirst().get();
        assertThat(found2.getFriends()).hasSize(1);
        assertTrue(found2.getFriends().contains(created3.getId()));
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private User createTestUser() {
        return createTestUser("test@test.com", "testlogin");
    }

    private User createTestUser(String email, String login) {
        return User.builder()
                .email(email)
                .login(login)
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }
}
package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;
    private final EventService eventService;  // ДОБАВЛЕНО

    @Transactional
    public User createUser(User user) {
        validateUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя пользователя пустое, установлено значение логина: {}", user.getLogin());
        }

        return userStorage.create(user);
    }

    @Transactional
    public User updateUser(User user) {
        validateUser(user);

        User existingUser = userStorage.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + user.getId() + " не найден"));

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.update(user);
    }

    public List<User> getAllUsers() {
        return userStorage.findAll();
    }

    public User getUserById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    @Transactional
    public void deleteUser(Long userId) {
        getUserById(userId);
        userStorage.delete(userId);
        log.debug("Удален пользователь с id={}", userId);
    }

    @Transactional
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        userStorage.findById(friendId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + friendId + " не найден"));

        userStorage.addFriend(userId, friendId);

        // ========== СОБЫТИЯ ==========
        eventService.addEvent(userId, "FRIEND", "ADD", friendId);
        eventService.addEvent(friendId, "FRIEND", "ADD", userId);
        // =============================

        log.debug("Пользователь {} добавил в друзья {}", userId, friendId);
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        userStorage.findById(friendId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + friendId + " не найден"));

        userStorage.removeFriend(userId, friendId);

        // ========== СОБЫТИЯ ==========
        eventService.addEvent(userId, "FRIEND", "REMOVE", friendId);
        eventService.addEvent(friendId, "FRIEND", "REMOVE", userId);
        // =============================

        log.debug("Пользователь {} удалил из друзей {}", userId, friendId);
    }

    public List<User> getUserFriends(Long userId) {
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        userStorage.findById(otherId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + otherId + " не найден"));

        return userStorage.getCommonFriends(userId, otherId);
    }

    public List<Film> getRecommendations(Long userId) {
        getUserById(userId);
        return userStorage.getRecommendations(userId);
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }

        if (!user.getEmail().contains("@")) {
            throw new ValidationException("Email должен содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может содержать пробелы");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }
}
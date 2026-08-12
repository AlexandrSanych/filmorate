package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    // ==================== ПРОВЕРКИ УНИКАЛЬНОСТИ ====================

    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public boolean isLoginExists(String login) {
        String sql = "SELECT COUNT(*) FROM users WHERE login = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, login);
        return count != null && count > 0;
    }

    // ==================== CRUD ОПЕРАЦИИ ====================

    @Override
    public User create(User user) {
        if (isEmailExists(user.getEmail())) {
            throw new DuplicateException("Пользователь с email '" + user.getEmail() + "' уже существует");
        }

        if (isLoginExists(user.getLogin())) {
            throw new DuplicateException("Пользователь с login '" + user.getLogin() + "' уже существует");
        }

        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("id");

        Number key = simpleJdbcInsert.executeAndReturnKey(userToMap(user));
        user.setId(key.longValue());

        log.debug("Создан пользователь: {}", user);
        return user;
    }

    @Override
    public User update(User user) {
        User existingUser = findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + user.getId() + " не найден"));

        if (!existingUser.getEmail().equals(user.getEmail()) && isEmailExists(user.getEmail())) {
            throw new DuplicateException("Пользователь с email '" + user.getEmail() + "' уже существует");
        }

        if (!existingUser.getLogin().equals(user.getLogin()) && isLoginExists(user.getLogin())) {
            throw new DuplicateException("Пользователь с login '" + user.getLogin() + "' уже существует");
        }

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());

        if (updated == 0) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }

        log.debug("Обновлен пользователь: {}", user);
        return user;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);

        if (users.isEmpty()) {
            return users;
        }

        List<Long> userIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Long, Set<Long>> friendsByUser = loadFriendsForUsers(userIds);

        for (User user : users) {
            user.setFriends(friendsByUser.getOrDefault(user.getId(), new HashSet<>()));
        }

        return users;
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser, id);

        if (users.isEmpty()) {
            return Optional.empty();
        }

        User user = users.get(0);
        loadFriendsForSingleUser(user);

        return Optional.of(user);
    }

    @Override
    public void delete(Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, userId);
        if (deleted == 0) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        log.debug("Удален пользователь с id={}", userId);
    }

    // ==================== ОПЕРАЦИИ С ДРУЗЬЯМИ ====================

    @Override
    public void addFriend(Long userId, Long friendId) {
        String sql = "MERGE INTO friendship (user_id, friend_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, userId, friendId);
        log.debug("Пользователь {} добавил в друзья {}", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
        log.debug("Пользователь {} удалил из друзей {}", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                "INNER JOIN friendship f ON u.id = f.friend_id " +
                "WHERE f.user_id = ? " +
                "ORDER BY u.id";

        List<User> friends = jdbcTemplate.query(sql, this::mapRowToUser, userId);

        if (friends.isEmpty()) {
            return friends;
        }

        List<Long> friendIds = friends.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Long, Set<Long>> friendsByUser = loadFriendsForUsers(friendIds);

        for (User friend : friends) {
            friend.setFriends(friendsByUser.getOrDefault(friend.getId(), new HashSet<>()));
        }

        log.debug("У пользователя {} найдено {} друзей", userId, friends.size());
        return friends;
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        String sql = "SELECT u.* FROM users u " +
                "INNER JOIN friendship f1 ON u.id = f1.friend_id AND f1.user_id = ? " +
                "INNER JOIN friendship f2 ON u.id = f2.friend_id AND f2.user_id = ? " +
                "ORDER BY u.id";

        List<User> common = jdbcTemplate.query(sql, this::mapRowToUser, userId, otherId);

        if (common.isEmpty()) {
            return common;
        }

        List<Long> commonIds = common.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Long, Set<Long>> friendsByUser = loadFriendsForUsers(commonIds);

        for (User user : common) {
            user.setFriends(friendsByUser.getOrDefault(user.getId(), new HashSet<>()));
        }

        log.debug("У пользователей {} и {} найдено {} общих друзей", userId, otherId, common.size());
        return common;
    }

    // ==================== РЕКОМЕНДАЦИИ (ИСПРАВЛЕНО — МЕТОД ВНУТРИ КЛАССА) ====================

    @Override
    public List<Film> getRecommendations(Long userId) {
        String sqlFindSimilarUser = """
            SELECT mrk2.user_id, 
                   COUNT(*) as common_films,
                   CORR(mrk1.mark_value, mrk2.mark_value) as rating_correlation
            FROM marks mrk1
            JOIN marks mrk2 ON mrk1.film_id = mrk2.film_id
            WHERE mrk1.user_id = ? AND mrk2.user_id != ?
            GROUP BY mrk2.user_id
            HAVING COUNT(*) >= 2
            ORDER BY rating_correlation DESC, common_films DESC
            LIMIT 1
        """;

        List<Long> similarUsers = jdbcTemplate.query(sqlFindSimilarUser,
                (rs, rowNum) -> rs.getLong("user_id"), userId, userId);

        if (similarUsers.isEmpty()) {
            return new ArrayList<>();
        }

        Long similarUserId = similarUsers.get(0);

        String sqlRecommendations = """
            SELECT f.*, m.id as mpa_id, m.name as mpa_name,
                   COALESCE(AVG(mrk.mark_value), 0) as avg_rating
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.id
            LEFT JOIN marks mrk ON f.id = mrk.film_id
            WHERE f.id IN (
                SELECT mrk2.film_id
                FROM marks mrk2
                WHERE mrk2.user_id = ?
                  AND mrk2.mark_value >= 6
                  AND mrk2.film_id NOT IN (
                      SELECT mrk1.film_id
                      FROM marks mrk1
                      WHERE mrk1.user_id = ?
                  )
            )
            GROUP BY f.id, m.id, m.name
            ORDER BY avg_rating DESC, f.id
        """;

        List<Film> films = jdbcTemplate.query(sqlRecommendations, this::mapRowToFilmWithMpaAndRating,
                similarUserId, userId);

        if (!films.isEmpty()) {
            List<Long> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());
            Map<Long, List<Genre>> genresByFilm = loadGenresForFilms(filmIds);
            Map<Long, List<Director>> directorsByFilm = loadDirectorsForFilms(filmIds);

            for (Film film : films) {
                film.setGenres(genresByFilm.getOrDefault(film.getId(), new ArrayList<>()));
                film.setDirectors(directorsByFilm.getOrDefault(film.getId(), new ArrayList<>()));
            }
        }

        return films;
    }

    // ==================== ПРИВАТНЫЕ МЕТОДЫ ====================

    private Map<Long, Set<Long>> loadFriendsForUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = userIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT user_id, friend_id FROM friendship WHERE user_id IN (" + placeholders + ")";

        Map<Long, Set<Long>> friendsByUser = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            Long userIdVal = rs.getLong("user_id");
            Long friendId = rs.getLong("friend_id");
            friendsByUser.computeIfAbsent(userIdVal, k -> new HashSet<>()).add(friendId);
        }, userIds.toArray());

        return friendsByUser;
    }

    private void loadFriendsForSingleUser(User user) {
        String sql = "SELECT friend_id FROM friendship WHERE user_id = ?";
        List<Long> friends = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong("friend_id"),
                user.getId());
        user.setFriends(new HashSet<>(friends));
    }

    private Map<Long, List<Genre>> loadGenresForFilms(List<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT fg.film_id, g.id, g.name, fg.position " +
                "FROM film_genre fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + placeholders + ") " +
                "ORDER BY fg.film_id, fg.position";

        Map<Long, List<Genre>> genresByFilm = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = Genre.builder()
                    .id(rs.getInt("id"))
                    .name(rs.getString("name"))
                    .build();
            genresByFilm.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
        }, filmIds.toArray());

        return genresByFilm;
    }

    private Map<Long, List<Director>> loadDirectorsForFilms(List<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT fd.film_id, d.id, d.name FROM film_director fd " +
                "JOIN directors d ON fd.director_id = d.id " +
                "WHERE fd.film_id IN (" + placeholders + ")";

        Map<Long, List<Director>> directorsByFilm = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            Long filmId = rs.getLong("film_id");
            Director director = Director.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .build();
            directorsByFilm.computeIfAbsent(filmId, k -> new ArrayList<>()).add(director);
        }, filmIds.toArray());

        return directorsByFilm;
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        return User.builder()
                .id(rs.getLong("id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday") != null ?
                        rs.getDate("birthday").toLocalDate() : null)
                .build();
    }

    private Film mapRowToFilmWithMpaAndRating(ResultSet rs, int rowNum) throws SQLException {
        Mpa mpa = null;
        Object mpaIdObj = rs.getObject("mpa_id");
        if (mpaIdObj != null) {
            mpa = Mpa.builder()
                    .id(rs.getInt("mpa_id"))
                    .name(rs.getString("mpa_name"))
                    .build();
        }

        Film film = Film.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date") != null ?
                        rs.getDate("release_date").toLocalDate() : null)
                .duration(rs.getInt("duration"))
                .mpa(mpa)
                .build();
        film.setRating(rs.getDouble("avg_rating"));
        return film;
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("email", user.getEmail());
        map.put("login", user.getLogin());
        map.put("name", user.getName());
        map.put("birthday", user.getBirthday());
        return map;
    }
}
package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.MarkStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final MarkStorage markStorage;

    // ==================== CRUD ОПЕРАЦИИ ====================

    @Override
    public Film create(Film film) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("films")
                .usingGeneratedKeyColumns("id");

        Number key = simpleJdbcInsert.executeAndReturnKey(filmToMap(film));
        film.setId(key.longValue());

        updateFilmGenres(film);
        updateFilmDirectors(film);

        Film created = findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм не найден после создания"));
        updateFilmRating(created);
        return created;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, " +
                "duration = ?, mpa_id = ? WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (updated == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        updateFilmGenres(film);
        updateFilmDirectors(film);

        Film updatedFilm = findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм не найден после обновления"));
        updateFilmRating(updatedFilm);
        return updatedFilm;
    }

    @Override
    public void delete(Long filmId) {
        String sql = "DELETE FROM films WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, filmId);
        if (deleted == 0) {
            throw new NotFoundException("Фильм с id=" + filmId + " не найден");
        }
        log.debug("Удален фильм с id={}", filmId);
    }

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.*, m.id as mpa_id, m.name as mpa_name FROM films f " +
                "LEFT JOIN mpa m ON f.mpa_id = m.id";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilmWithMpa);

        if (films.isEmpty()) {
            return films;
        }

        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        Map<Long, List<Genre>> genresByFilm = loadGenresForFilms(filmIds);
        Map<Long, List<Director>> directorsByFilm = loadDirectorsForFilms(filmIds);

        for (Film film : films) {
            film.setGenres(genresByFilm.getOrDefault(film.getId(), new ArrayList<>()));
            film.setDirectors(directorsByFilm.getOrDefault(film.getId(), new ArrayList<>()));
            updateFilmRating(film);
        }

        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = "SELECT f.*, m.id as mpa_id, m.name as mpa_name FROM films f " +
                "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                "WHERE f.id = ?";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilmWithMpa, id);

        if (films.isEmpty()) {
            return Optional.empty();
        }

        Film film = films.get(0);
        loadGenresForSingleFilm(film);
        loadDirectorsForSingleFilm(film);
        updateFilmRating(film);

        return Optional.of(film);
    }

    // ==================== РАБОТА С ОЦЕНКАМИ ====================

    @Override
    public void addMark(Long filmId, Long userId, Integer markValue) {
        markStorage.addMark(filmId, userId, markValue);
        log.debug("Пользователь {} поставил оценку {} фильму {}", userId, markValue, filmId);
    }

    @Override
    public void updateMark(Long filmId, Long userId, Integer markValue) {
        markStorage.updateMark(filmId, userId, markValue);
        log.debug("Пользователь {} обновил оценку для фильма {}: {}", userId, filmId, markValue);
    }

    @Override
    public void removeMark(Long filmId, Long userId) {
        markStorage.removeMark(filmId, userId);
        log.debug("Пользователь {} удалил оценку с фильма {}", userId, filmId);
    }

    // ==================== ПОПУЛЯРНЫЕ ФИЛЬМЫ ====================

    @Override
    public List<Film> getPopular(Integer count, Integer genreId, Integer year) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT f.*, m.id as mpa_id, m.name as mpa_name, ");
        sql.append("COALESCE(AVG(mrk.mark_value), 0) as avg_rating ");
        sql.append("FROM films f ");
        sql.append("LEFT JOIN mpa m ON f.mpa_id = m.id ");
        sql.append("LEFT JOIN marks mrk ON f.id = mrk.film_id ");

        if (genreId != null) {
            sql.append("LEFT JOIN film_genre fg ON f.id = fg.film_id ");
        }

        sql.append("WHERE 1=1 ");

        if (genreId != null) {
            sql.append("AND fg.genre_id = ? ");
            params.add(genreId);
        }

        if (year != null) {
            sql.append("AND EXTRACT(YEAR FROM f.release_date) = ? ");
            params.add(year);
        }

        sql.append("GROUP BY f.id, m.id, m.name ");
        sql.append("ORDER BY avg_rating DESC, f.id ");
        sql.append("LIMIT ?");
        params.add(count);

        List<Film> films = jdbcTemplate.query(sql.toString(), this::mapRowToFilmWithMpaAndRating, params.toArray());

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

    // ==================== ФИЛЬМЫ РЕЖИССЁРА ====================

    @Override
    public List<Film> findByDirector(Long directorId, String sortBy) {
        String orderBy = "year".equals(sortBy) ? "f.release_date" : "avg_rating";
        String orderDirection = "year".equals(sortBy) ? "ASC" : "DESC";

        String sql = String.format("""
                SELECT f.*, m.id as mpa_id, m.name as mpa_name,
                       COALESCE(AVG(mrk.mark_value), 0) as avg_rating
                FROM films f
                JOIN film_director fd ON f.id = fd.film_id
                LEFT JOIN mpa m ON f.mpa_id = m.id
                LEFT JOIN marks mrk ON f.id = mrk.film_id
                WHERE fd.director_id = ?
                GROUP BY f.id, m.id, m.name
                ORDER BY %s %s, f.id
                """, orderBy, orderDirection);

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilmWithMpaAndRating, directorId);

        if (!films.isEmpty()) {
            List<Long> filmIds = films.stream()
                    .map(Film::getId)
                    .collect(Collectors.toList());
            loadGenresAndDirectorsForFilms(films, filmIds);
        }

        return films;
    }

    // ==================== ОБЩИЕ ФИЛЬМЫ ====================

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = """
                SELECT f.*, m.id as mpa_id, m.name as mpa_name,
                       COALESCE(AVG(mrk.mark_value), 0) as avg_rating
                FROM films f
                LEFT JOIN mpa m ON f.mpa_id = m.id
                LEFT JOIN marks mrk ON f.id = mrk.film_id
                WHERE f.id IN (
                    SELECT mrk1.film_id
                    FROM marks mrk1
                    WHERE mrk1.user_id = ? AND mrk1.mark_value >= 6
                    INTERSECT
                    SELECT mrk2.film_id
                    FROM marks mrk2
                    WHERE mrk2.user_id = ? AND mrk2.mark_value >= 6
                )
                GROUP BY f.id, m.id, m.name
                ORDER BY avg_rating DESC, f.id
                """;

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilmWithMpaAndRating, userId, friendId);

        if (!films.isEmpty()) {
            List<Long> filmIds = films.stream()
                    .map(Film::getId)
                    .collect(Collectors.toList());
            loadGenresAndDirectorsForFilms(films, filmIds);
        }

        return films;
    }

    // ==================== ПОИСК ====================

    @Override
    public List<Film> search(String query, boolean searchByTitle, boolean searchByDirector) {
        String likePattern = "%" + query.toLowerCase() + "%";
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT f.*, m.id as mpa_id, m.name as mpa_name, ");
        sql.append("COALESCE(AVG(mrk.mark_value), 0) as avg_rating ");
        sql.append("FROM films f ");
        sql.append("LEFT JOIN mpa m ON f.mpa_id = m.id ");
        sql.append("LEFT JOIN marks mrk ON f.id = mrk.film_id ");

        if (searchByDirector) {
            sql.append("LEFT JOIN film_director fd ON f.id = fd.film_id ");
            sql.append("LEFT JOIN directors d ON fd.director_id = d.id ");
        }

        sql.append("WHERE ");

        List<String> conditions = new ArrayList<>();
        if (searchByTitle) {
            conditions.add("LOWER(f.name) LIKE ?");
            params.add(likePattern);
        }
        if (searchByDirector) {
            conditions.add("LOWER(d.name) LIKE ?");
            params.add(likePattern);
        }

        sql.append(String.join(" OR ", conditions));
        sql.append(" GROUP BY f.id, m.id, m.name ");
        sql.append("ORDER BY avg_rating DESC, f.id");

        List<Film> films = jdbcTemplate.query(sql.toString(), this::mapRowToFilmWithMpaAndRating, params.toArray());

        if (!films.isEmpty()) {
            List<Long> filmIds = films.stream()
                    .map(Film::getId)
                    .collect(Collectors.toList());
            loadGenresAndDirectorsForFilms(films, filmIds);
        }

        return films;
    }

    // ==================== РЕКОМЕНДАЦИИ ====================

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
            List<Long> filmIds = films.stream()
                    .map(Film::getId)
                    .collect(Collectors.toList());
            loadGenresAndDirectorsForFilms(films, filmIds);
        }

        return films;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private void updateFilmRating(Film film) {
        Double rating = markStorage.getAverageRating(film.getId());
        film.setRating(rating);
    }

    private void updateFilmGenres(Film film) {
        jdbcTemplate.update("DELETE FROM film_genre WHERE film_id = ?", film.getId());

        List<Genre> genres = film.getGenres();
        if (genres != null && !genres.isEmpty()) {
            String sql = "INSERT INTO film_genre (film_id, genre_id, position) VALUES (?, ?, ?)";
            int position = 0;
            Set<Integer> addedGenreIds = new HashSet<>();

            for (Genre genre : genres) {
                if (genre != null && genre.getId() != null && !addedGenreIds.contains(genre.getId())) {
                    jdbcTemplate.update(sql, film.getId(), genre.getId(), position++);
                    addedGenreIds.add(genre.getId());
                }
            }
        }
    }

    private void updateFilmDirectors(Film film) {
        jdbcTemplate.update("DELETE FROM film_director WHERE film_id = ?", film.getId());

        List<Director> directors = film.getDirectors();
        if (directors != null && !directors.isEmpty()) {
            String sql = "INSERT INTO film_director (film_id, director_id) VALUES (?, ?)";
            Set<Long> addedDirectorIds = new HashSet<>();

            for (Director director : directors) {
                if (director != null && director.getId() != null && !addedDirectorIds.contains(director.getId())) {
                    jdbcTemplate.update(sql, film.getId(), director.getId());
                    addedDirectorIds.add(director.getId());
                }
            }
        }
    }

    private void loadGenresAndDirectorsForFilms(List<Film> films, List<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return;
        }

        Map<Long, List<Genre>> genresByFilm = loadGenresForFilms(filmIds);
        Map<Long, List<Director>> directorsByFilm = loadDirectorsForFilms(filmIds);

        for (Film film : films) {
            film.setGenres(genresByFilm.getOrDefault(film.getId(), new ArrayList<>()));
            film.setDirectors(directorsByFilm.getOrDefault(film.getId(), new ArrayList<>()));
        }
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

    private void loadGenresForSingleFilm(Film film) {
        String sql = "SELECT g.* FROM genres g " +
                "JOIN film_genre fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY fg.position ASC";

        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) ->
                        Genre.builder()
                                .id(rs.getInt("id"))
                                .name(rs.getString("name"))
                                .build(),
                film.getId());

        film.setGenres(genres);
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

    private void loadDirectorsForSingleFilm(Film film) {
        String sql = "SELECT d.* FROM directors d " +
                "JOIN film_director fd ON d.id = fd.director_id " +
                "WHERE fd.film_id = ?";

        List<Director> directors = jdbcTemplate.query(sql, (rs, rowNum) ->
                        Director.builder()
                                .id(rs.getLong("id"))
                                .name(rs.getString("name"))
                                .build(),
                film.getId());

        film.setDirectors(directors);
    }

    private Film mapRowToFilmWithMpa(ResultSet rs, int rowNum) throws SQLException {
        Mpa mpa = null;
        Object mpaIdObj = rs.getObject("mpa_id");
        if (mpaIdObj != null) {
            mpa = Mpa.builder()
                    .id(rs.getInt("mpa_id"))
                    .name(rs.getString("mpa_name"))
                    .build();
        }

        return Film.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date") != null ?
                        rs.getDate("release_date").toLocalDate() : null)
                .duration(rs.getInt("duration"))
                .mpa(mpa)
                .build();
    }

    private Film mapRowToFilmWithMpaAndRating(ResultSet rs, int rowNum) throws SQLException {
        Film film = mapRowToFilmWithMpa(rs, rowNum);
        film.setRating(rs.getDouble("avg_rating"));
        return film;
    }

    private Map<String, Object> filmToMap(Film film) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", film.getName());
        map.put("description", film.getDescription());
        map.put("release_date", film.getReleaseDate());
        map.put("duration", film.getDuration());
        map.put("mpa_id", film.getMpa() != null ? film.getMpa().getId() : null);
        return map;
    }
}
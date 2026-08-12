package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    // ==================== CRUD ====================
    Film create(Film film);
    Film update(Film film);
    List<Film> findAll();
    Optional<Film> findById(Long id);
    void delete(Long filmId);

    // ==================== ОЦЕНКИ (марки) ====================
    void addMark(Long filmId, Long userId, Integer markValue);
    void updateMark(Long filmId, Long userId, Integer markValue);
    void removeMark(Long filmId, Long userId);

    // ==================== ПОПУЛЯРНЫЕ ФИЛЬМЫ ====================
    List<Film> getPopular(Integer count, Integer genreId, Integer year);

    // ==================== ФИЛЬМЫ РЕЖИССЁРА ====================
    List<Film> findByDirector(Long directorId, String sortBy);

    // ==================== ОБЩИЕ ФИЛЬМЫ ====================
    List<Film> getCommonFilms(Long userId, Long friendId);

    // ==================== ПОИСК ====================
    List<Film> search(String query, boolean searchByTitle, boolean searchByDirector);

    // ==================== РЕКОМЕНДАЦИИ ====================
    List<Film> getRecommendations(Long userId);
}
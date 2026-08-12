package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Mark;

import java.util.List;
import java.util.Optional;

public interface MarkStorage {

    // Поставить оценку фильму
    Mark addMark(Long filmId, Long userId, Integer markValue);

    // Обновить оценку
    Mark updateMark(Long filmId, Long userId, Integer markValue);

    // Удалить оценку
    void removeMark(Long filmId, Long userId);

    // Получить оценку пользователя для фильма
    Optional<Mark> getMark(Long filmId, Long userId);

    // Получить все оценки фильма
    List<Mark> getMarksByFilm(Long filmId);

    // Получить все оценки пользователя
    List<Mark> getMarksByUser(Long userId);

    // Получить среднюю оценку фильма
    Double getAverageRating(Long filmId);

    // Получить количество оценок фильма
    Integer getMarksCount(Long filmId);

    // Проверить, является ли оценка положительной (6-10)
    default boolean isPositive(Integer markValue) {
        return markValue != null && markValue >= 6;
    }

    // Проверить, является ли оценка отрицательной (1-5)
    default boolean isNegative(Integer markValue) {
        return markValue != null && markValue <= 5;
    }
}
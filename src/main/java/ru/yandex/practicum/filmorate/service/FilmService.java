package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;
    private final DirectorStorage directorStorage;
    private final EventService eventService;

    @Transactional
    public Film createFilm(Film film) {
        if (film.getMpa() != null) {
            Mpa validMpa = mpaStorage.getById(film.getMpa().getId());
            film.setMpa(validMpa);
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Genre> validatedGenres = new ArrayList<>();
            for (Genre genre : film.getGenres()) {
                if (genre != null && genre.getId() != null) {
                    Genre validGenre = genreStorage.getById(genre.getId());
                    validatedGenres.add(validGenre);
                }
            }
            film.setGenres(validatedGenres);
        }

        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            List<Director> validatedDirectors = new ArrayList<>();
            for (Director director : film.getDirectors()) {
                if (director != null && director.getId() != null) {
                    Director validDirector = directorStorage.getById(director.getId());
                    validatedDirectors.add(validDirector);
                }
            }
            film.setDirectors(validatedDirectors);
        }

        return filmStorage.create(film);
    }

    @Transactional
    public Film updateFilm(Film film) {
        filmStorage.findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + film.getId() + " не найден"));

        if (film.getMpa() != null) {
            Mpa validMpa = mpaStorage.getById(film.getMpa().getId());
            film.setMpa(validMpa);
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Genre> validatedGenres = new ArrayList<>();
            for (Genre genre : film.getGenres()) {
                if (genre != null && genre.getId() != null) {
                    Genre validGenre = genreStorage.getById(genre.getId());
                    validatedGenres.add(validGenre);
                }
            }
            film.setGenres(validatedGenres);
        } else {
            film.setGenres(new ArrayList<>());
        }

        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            List<Director> validatedDirectors = new ArrayList<>();
            for (Director director : film.getDirectors()) {
                if (director != null && director.getId() != null) {
                    Director validDirector = directorStorage.getById(director.getId());
                    validatedDirectors.add(validDirector);
                }
            }
            film.setDirectors(validatedDirectors);
        } else {
            film.setDirectors(new ArrayList<>());
        }

        return filmStorage.update(film);
    }

    @Transactional
    public void deleteFilm(Long filmId) {
        getFilmById(filmId);
        filmStorage.delete(filmId);
        log.debug("Удален фильм с id={}", filmId);
    }

    @Transactional(readOnly = true)
    public List<Film> getAllFilms() {
        return filmStorage.findAll();
    }

    @Transactional(readOnly = true)
    public Film getFilmById(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
    }

    @Transactional
    public void addMark(Long filmId, Long userId, Integer markValue) {
        filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        filmStorage.addMark(filmId, userId, markValue);

        // ИСПРАВЛЕНО: eventType = "MARK", operation = "ADD"
        eventService.addEvent(userId, "MARK", "ADD", filmId);

        log.debug("Пользователь {} поставил оценку {} фильму {}", userId, markValue, filmId);
    }

    @Transactional
    public void updateMark(Long filmId, Long userId, Integer markValue) {
        filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        filmStorage.updateMark(filmId, userId, markValue);

        eventService.addEvent(userId, "MARK", "UPDATE", filmId);
        log.debug("Пользователь {} обновил оценку для фильма {}: {}", userId, filmId, markValue);
    }

    @Transactional
    public void removeMark(Long filmId, Long userId) {
        filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        filmStorage.removeMark(filmId, userId);

        eventService.addEvent(userId, "MARK", "REMOVE", filmId);
        log.debug("Пользователь {} удалил оценку с фильма {}", userId, filmId);
    }

    @Transactional(readOnly = true)
    public List<Film> getPopularFilms(Integer count, Integer genreId, Integer year) {
        return filmStorage.getPopular(count, genreId, year);
    }

    @Transactional(readOnly = true)
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        userStorage.findById(friendId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + friendId + " не найден"));
        return filmStorage.getCommonFilms(userId, friendId);
    }

    @Transactional(readOnly = true)
    public List<Film> searchFilms(String query, String by) {
        List<String> searchBy = Arrays.asList(by.split(","));
        boolean searchByTitle = searchBy.contains("title");
        boolean searchByDirector = searchBy.contains("director");
        return filmStorage.search(query, searchByTitle, searchByDirector);
    }

    @Transactional(readOnly = true)
    public List<Film> getFilmsByDirector(Long directorId, String sortBy) {
        directorStorage.findById(directorId)
                .orElseThrow(() -> new NotFoundException("Режиссёр с id=" + directorId + " не найден"));
        return filmStorage.findByDirector(directorId, sortBy);
    }
}
package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
public class FilmController {

    private final FilmService filmService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film create(@Valid @RequestBody Film film) {
        log.info("POST /films - создание фильма: {}", film);
        return filmService.createFilm(film);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public Film update(@Valid @RequestBody Film film) {
        log.info("PUT /films - обновление фильма: {}", film);
        return filmService.updateFilm(film);
    }

    @DeleteMapping("/{filmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long filmId) {
        log.info("DELETE /films/{} - удаление фильма", filmId);
        filmService.deleteFilm(filmId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Film> findAll() {
        log.info("GET /films - получение всех фильмов");
        return filmService.getAllFilms();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Film findById(@PathVariable Long id) {
        log.info("GET /films/{} - получение фильма по id", id);
        return filmService.getFilmById(id);
    }

    // ИСПРАВЛЕНО: добавлена валидация оценки (1-10)
    @PutMapping("/{id}/mark/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void addMark(@PathVariable Long id, @PathVariable Long userId,
                        @RequestParam @Min(value = 1, message = "Оценка должна быть от 1 до 10")
                        @Max(value = 10, message = "Оценка должна быть от 1 до 10") Integer mark) {
        log.info("PUT /films/{}/mark/{}?mark={} - добавление оценки", id, userId, mark);
        filmService.addMark(id, userId, mark);
    }

    @PutMapping("/{id}/mark/{userId}/update")
    @ResponseStatus(HttpStatus.OK)
    public void updateMark(@PathVariable Long id, @PathVariable Long userId,
                           @RequestParam @Min(1) @Max(10) Integer mark) {
        log.info("PUT /films/{}/mark/{}/update?mark={} - обновление оценки", id, userId, mark);
        filmService.updateMark(id, userId, mark);
    }

    @DeleteMapping("/{id}/mark/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMark(@PathVariable Long id, @PathVariable Long userId) {
        log.info("DELETE /films/{}/mark/{} - удаление оценки", id, userId);
        filmService.removeMark(id, userId);
    }

    @GetMapping("/popular")
    @ResponseStatus(HttpStatus.OK)
    public List<Film> getPopular(
            @RequestParam(defaultValue = "10") Integer count,
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Integer year) {
        log.info("GET /films/popular?count={}&genreId={}&year={} - получение популярных фильмов", count, genreId, year);
        return filmService.getPopularFilms(count, genreId, year);
    }

    @GetMapping("/common")
    @ResponseStatus(HttpStatus.OK)
    public List<Film> getCommonFilms(
            @RequestParam Long userId,
            @RequestParam Long friendId) {
        log.info("GET /films/common?userId={}&friendId={} - получение общих фильмов", userId, friendId);
        return filmService.getCommonFilms(userId, friendId);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<Film> search(
            @RequestParam String query,
            @RequestParam String by) {
        log.info("GET /films/search?query={}&by={} - поиск фильмов", query, by);
        return filmService.searchFilms(query, by);
    }

    @GetMapping("/director/{directorId}")
    @ResponseStatus(HttpStatus.OK)
    public List<Film> getFilmsByDirector(
            @PathVariable Long directorId,
            @RequestParam String sortBy) {
        log.info("GET /films/director/{}?sortBy={} - получение фильмов режиссёра", directorId, sortBy);
        return filmService.getFilmsByDirector(directorId, sortBy);
    }
}
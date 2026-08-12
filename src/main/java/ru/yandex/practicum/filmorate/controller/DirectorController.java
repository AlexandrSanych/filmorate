package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
public class DirectorController {

    private final DirectorService directorService;

    @GetMapping
    public List<Director> findAll() {
        log.info("GET /directors - получение всех режиссёров");
        return directorService.getAllDirectors();
    }

    @GetMapping("/{id}")
    public Director findById(@PathVariable Long id) {
        log.info("GET /directors/{} - получение режиссёра по id", id);
        return directorService.getDirectorById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Director create(@Valid @RequestBody Director director) {
        log.info("POST /directors - создание режиссёра: {}", director.getName());
        return directorService.createDirector(director);
    }

    @PutMapping
    public Director update(@Valid @RequestBody Director director) {
        log.info("PUT /directors - обновление режиссёра с id={}", director.getId());
        return directorService.updateDirector(director);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        log.info("DELETE /directors/{} - удаление режиссёра", id);
        directorService.deleteDirector(id);
    }
}
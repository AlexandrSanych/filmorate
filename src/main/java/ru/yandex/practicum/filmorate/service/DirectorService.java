package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorService {

    private final DirectorStorage directorStorage;

    public List<Director> getAllDirectors() {
        return directorStorage.findAll();
    }

    public Director getDirectorById(Long id) {
        return directorStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Режиссёр с id=" + id + " не найден"));
    }

    @Transactional
    public Director createDirector(Director director) {
        return directorStorage.create(director);
    }

    @Transactional
    public Director updateDirector(Director director) {
        getDirectorById(director.getId());
        return directorStorage.update(director);
    }

    @Transactional
    public void deleteDirector(Long id) {
        getDirectorById(id);
        directorStorage.delete(id);
    }
}
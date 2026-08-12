package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/feed")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Event> getUserFeed(@PathVariable Long userId) {
        log.info("GET /users/{}/feed - получение ленты событий", userId);
        return eventService.getUserFeed(userId);
    }
}
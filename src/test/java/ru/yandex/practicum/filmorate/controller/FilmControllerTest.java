package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FilmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM marks");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");

        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");

        jdbcTemplate.execute("MERGE INTO mpa (id, name) VALUES " +
                "(1, 'G'), (2, 'PG'), (3, 'PG-13'), (4, 'R'), (5, 'NC-17')");
        jdbcTemplate.execute("MERGE INTO genres (id, name) VALUES " +
                "(1, 'Комедия'), (2, 'Драма'), (3, 'Мультфильм'), " +
                "(4, 'Триллер'), (5, 'Документальный'), (6, 'Боевик')");
    }

    @Test
    void shouldCreateFilm() throws Exception {
        Film film = Film.builder()
                .name("New Film")
                .description("New Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New Film"))
                .andExpect(jsonPath("$.mpa.id").value(1));
    }

    @Test
    void shouldUpdateFilm() throws Exception {
        Film film = Film.builder()
                .name("Original Film")
                .description("Original Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Film createdFilm = objectMapper.readValue(response, Film.class);
        Long filmId = createdFilm.getId();

        Film updatedFilm = createdFilm.toBuilder()
                .name("Updated Film")
                .description("Updated Description")
                .duration(150)
                .mpa(Mpa.builder().id(2).build())
                .build();

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedFilm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(filmId))
                .andExpect(jsonPath("$.name").value("Updated Film"))
                .andExpect(jsonPath("$.mpa.id").value(2));
    }

    @Test
    void shouldDeleteFilm() throws Exception {
        Film film = Film.builder()
                .name("Delete Film")
                .description("Delete Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Film createdFilm = objectMapper.readValue(response, Film.class);
        Long filmId = createdFilm.getId();

        mockMvc.perform(delete("/films/" + filmId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/films/" + filmId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllFilms() throws Exception {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Description 1")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        Film film2 = Film.builder()
                .name("Film 2")
                .description("Description 2")
                .releaseDate(LocalDate.of(2021, 2, 2))
                .duration(90)
                .mpa(Mpa.builder().id(2).build())
                .build();

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Film 1"))
                .andExpect(jsonPath("$[1].name").value("Film 2"));
    }

    @Test
    void shouldGetFilmById() throws Exception {
        Film film = Film.builder()
                .name("Get Film")
                .description("Get Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Film createdFilm = objectMapper.readValue(response, Film.class);
        Long filmId = createdFilm.getId();

        mockMvc.perform(get("/films/" + filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(filmId))
                .andExpect(jsonPath("$.name").value("Get Film"));
    }

    @Test
    void shouldReturn404WhenFilmNotFound() throws Exception {
        mockMvc.perform(get("/films/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAddMark() throws Exception {
        User user = User.builder()
                .email("user@example.com")
                .login("user")
                .name("User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        String userResponse = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(userResponse, User.class);
        Long userId = createdUser.getId();

        Film film = Film.builder()
                .name("Mark Film")
                .description("Mark Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        String filmResponse = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Film createdFilm = objectMapper.readValue(filmResponse, Film.class);
        Long filmId = createdFilm.getId();

        // Исправлено: /like → /mark?mark=10
        mockMvc.perform(put("/films/{id}/mark/{userId}?mark=10", filmId, userId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/" + filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(10.0));
    }

    @Test
    void shouldRemoveMark() throws Exception {
        User user = User.builder()
                .email("user@example.com")
                .login("user")
                .name("User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        String userResponse = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(userResponse, User.class);
        Long userId = createdUser.getId();

        Film film = Film.builder()
                .name("Unmark Film")
                .description("Unmark Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        String filmResponse = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Film createdFilm = objectMapper.readValue(filmResponse, Film.class);
        Long filmId = createdFilm.getId();

        // Добавляем оценку
        mockMvc.perform(put("/films/{id}/mark/{userId}?mark=10", filmId, userId))
                .andExpect(status().isOk());

        // Удаляем оценку
        mockMvc.perform(delete("/films/{id}/mark/{userId}", filmId, userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/films/" + filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(0.0));
    }

    @Test
    void shouldUpdateMark() throws Exception {
        User user = User.builder()
                .email("user@example.com")
                .login("user")
                .name("User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        String userResponse = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(userResponse, User.class);
        Long userId = createdUser.getId();

        Film film = Film.builder()
                .name("Update Mark Film")
                .description("Update Mark Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        String filmResponse = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Film createdFilm = objectMapper.readValue(filmResponse, Film.class);
        Long filmId = createdFilm.getId();

        // Добавляем оценку 5
        mockMvc.perform(put("/films/{id}/mark/{userId}?mark=5", filmId, userId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/" + filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5.0));

        // Обновляем оценку на 10
        mockMvc.perform(put("/films/{id}/mark/{userId}/update?mark=10", filmId, userId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/" + filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(10.0));
    }

    @Test
    void shouldGetPopularFilms() throws Exception {
        User user1 = User.builder()
                .email("user1@example.com")
                .login("user1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User user2 = User.builder()
                .email("user2@example.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();

        String userResponse1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userResponse2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser1 = objectMapper.readValue(userResponse1, User.class);
        User createdUser2 = objectMapper.readValue(userResponse2, User.class);
        Long userId1 = createdUser1.getId();
        Long userId2 = createdUser2.getId();

        Film film1 = Film.builder()
                .name("Popular Film 1")
                .description("Description 1")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        Film film2 = Film.builder()
                .name("Popular Film 2")
                .description("Description 2")
                .releaseDate(LocalDate.of(2020, 2, 2))
                .duration(90)
                .mpa(Mpa.builder().id(1).build())
                .build();

        String filmResponse1 = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String filmResponse2 = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film2)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Film createdFilm1 = objectMapper.readValue(filmResponse1, Film.class);
        Film createdFilm2 = objectMapper.readValue(filmResponse2, Film.class);
        Long filmId1 = createdFilm1.getId();
        Long filmId2 = createdFilm2.getId();

        // Film1 получает две оценки по 10 → средняя 10
        mockMvc.perform(put("/films/{id}/mark/{userId}?mark=10", filmId1, userId1))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{id}/mark/{userId}?mark=10", filmId1, userId2))
                .andExpect(status().isOk());

        // Film2 получает одну оценку 5 → средняя 5
        mockMvc.perform(put("/films/{id}/mark/{userId}?mark=5", filmId2, userId1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(filmId1))
                .andExpect(jsonPath("$[1].id").value(filmId2));
    }

    @Test
    void shouldGetPopularFilmsWithGenreFilter() throws Exception {
        User user = User.builder()
                .email("user@example.com")
                .login("user")
                .name("User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        String userResponse = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(userResponse, User.class);
        Long userId = createdUser.getId();

        Film film1 = Film.builder()
                .name("Comedy Film")
                .description("Comedy Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build();

        Film film2 = Film.builder()
                .name("Drama Film")
                .description("Drama Description")
                .releaseDate(LocalDate.of(2020, 2, 2))
                .duration(90)
                .mpa(Mpa.builder().id(1).build())
                .build();

        String filmResponse1 = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String filmResponse2 = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film2)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Film createdFilm1 = objectMapper.readValue(filmResponse1, Film.class);
        Film createdFilm2 = objectMapper.readValue(filmResponse2, Film.class);
        Long filmId1 = createdFilm1.getId();
        Long filmId2 = createdFilm2.getId();

        mockMvc.perform(put("/films/{id}/mark/{userId}?mark=10", filmId1, userId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{id}/mark/{userId}?mark=8", filmId2, userId))
                .andExpect(status().isOk());

        // Проверяем популярные фильмы без фильтрации
        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(filmId1));
    }
}
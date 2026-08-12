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
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    void shouldCreateUser() throws Exception {
        User user = User.builder()
                .email("test@example.com")
                .login("testlogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.login").value("testlogin"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void shouldCreateUserWithEmptyName_ShouldUseLogin() throws Exception {
        User user = User.builder()
                .email("test@example.com")
                .login("testlogin")
                .name("")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("testlogin"));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        User user = User.builder()
                .email("original@example.com")
                .login("original")
                .name("Original User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(response, User.class);
        Long userId = createdUser.getId();

        User updatedUser = createdUser.toBuilder()
                .email("updated@example.com")
                .name("Updated User")
                .build();

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.name").value("Updated User"));
    }

    @Test
    void shouldGetAllUsers() throws Exception {
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

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$[1].email").value("user2@example.com"));
    }

    @Test
    void shouldGetUserById() throws Exception {
        User user = User.builder()
                .email("get@example.com")
                .login("getuser")
                .name("Get User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(response, User.class);
        Long userId = createdUser.getId();

        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("get@example.com"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAddFriend() throws Exception {
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

        String response1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser1 = objectMapper.readValue(response1, User.class);
        User createdUser2 = objectMapper.readValue(response2, User.class);
        Long userId1 = createdUser1.getId();
        Long userId2 = createdUser2.getId();

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId1, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}/friends", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userId2));
    }

    @Test
    void shouldRemoveFriend() throws Exception {
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

        String response1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser1 = objectMapper.readValue(response1, User.class);
        User createdUser2 = objectMapper.readValue(response2, User.class);
        Long userId1 = createdUser1.getId();
        Long userId2 = createdUser2.getId();

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId1, userId2))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/{id}/friends/{friendId}", userId1, userId2))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}/friends", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldGetCommonFriends() throws Exception {
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

        User user3 = User.builder()
                .email("user3@example.com")
                .login("user3")
                .name("User Three")
                .birthday(LocalDate.of(1993, 3, 3))
                .build();

        String response1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response3 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user3)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser1 = objectMapper.readValue(response1, User.class);
        User createdUser2 = objectMapper.readValue(response2, User.class);
        User createdUser3 = objectMapper.readValue(response3, User.class);
        Long userId1 = createdUser1.getId();
        Long userId2 = createdUser2.getId();
        Long userId3 = createdUser3.getId();

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId1, userId3))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId2, userId3))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}/friends/common/{otherId}", userId1, userId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userId3));
    }
}
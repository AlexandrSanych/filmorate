package ru.yandex.practicum.filmorate.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserValidationTest {
    private Validator validator;
    private User user;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        user = User.builder()
                .email("test@test.com")
                .login("testlogin")
                .name("Test")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void shouldValidateValidUser() {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldNotValidateWhenEmailIsInvalid() {
        User invalidUser = user.toBuilder().email("invalid-email").build();
        Set<ConstraintViolation<User>> violations = validator.validate(invalidUser);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldNotValidateWhenLoginHasSpaces() {
        User invalidUser = user.toBuilder().login("test login").build();
        Set<ConstraintViolation<User>> violations = validator.validate(invalidUser);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldNotValidateWhenBirthdayIsInFuture() {
        User invalidUser = user.toBuilder()
                .birthday(LocalDate.now().plusDays(1))
                .build();
        Set<ConstraintViolation<User>> violations = validator.validate(invalidUser);
        assertFalse(violations.isEmpty());
    }
}
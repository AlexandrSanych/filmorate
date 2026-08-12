package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @EqualsAndHashCode.Include
    private Long reviewId;

    @NotBlank(message = "Содержание отзыва не может быть пустым")
    private String content;

    @NotNull(message = "Тип отзыва (isPositive) обязателен")
    private Boolean isPositive;

    @NotNull(message = "ID пользователя обязателен")
    private Long userId;

    @NotNull(message = "ID фильма обязателен")
    private Long filmId;

    private Integer useful;  // рейтинг полезности
}
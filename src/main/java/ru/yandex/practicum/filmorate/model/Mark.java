package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mark {

    @EqualsAndHashCode.Include
    private Long filmId;

    @EqualsAndHashCode.Include
    private Long userId;

    @NotNull(message = "Оценка не может быть пустой")
    @Min(value = 1, message = "Оценка должна быть от 1 до 10")
    @Max(value = 10, message = "Оценка должна быть от 1 до 10")
    private Integer markValue;
}
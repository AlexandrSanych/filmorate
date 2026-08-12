package ru.yandex.practicum.filmorate.model;

import lombok.*;
import jakarta.validation.constraints.*;
import ru.yandex.practicum.filmorate.validation.ReleaseDate;
import java.time.LocalDate;
import java.util.*;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Film {

    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    @Size(max = 200, message = "Максимальная длина описания - 200 символов")
    private String description;

    @NotNull(message = "Дата релиза не может быть пустой")
    @ReleaseDate
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность не может быть пустой")
    @Positive(message = "Продолжительность должна быть положительной")
    private Integer duration;

    private Mpa mpa;

    @Builder.Default
    private List<Genre> genres = new ArrayList<>();

    @Builder.Default
    private List<Director> directors = new ArrayList<>();

    // НОВОЕ ПОЛЕ: средний рейтинг (вычисляется на основе оценок)
    private Double rating;
}
package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Director {

    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Имя режиссёра не может быть пустым")
    private String name;
}
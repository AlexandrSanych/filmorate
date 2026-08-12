package ru.yandex.practicum.filmorate.model;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @EqualsAndHashCode.Include
    private Long eventId;

    private Long timestamp;

    private Long userId;

    private String eventType;  // LIKE, REVIEW, FRIEND

    private String operation;  // REMOVE, ADD, UPDATE

    private Long entityId;
}
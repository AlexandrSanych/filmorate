package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EventDbStorage implements EventStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Event create(Event event) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("events")
                .usingGeneratedKeyColumns("id");

        Map<String, Object> params = new HashMap<>();
        params.put("timestamp", event.getTimestamp());
        params.put("user_id", event.getUserId());
        params.put("event_type", event.getEventType());
        params.put("operation", event.getOperation());
        params.put("entity_id", event.getEntityId());

        Number key = insert.executeAndReturnKey(params);
        event.setEventId(key.longValue());

        log.debug("Создано событие: userId={}, type={}, operation={}",
                event.getUserId(), event.getEventType(), event.getOperation());
        return event;
    }

    @Override
    public List<Event> findByUserId(Long userId) {
        String sql = "SELECT * FROM events WHERE user_id = ? ORDER BY timestamp DESC";
        return jdbcTemplate.query(sql, this::mapRowToEvent, userId);
    }

    private Event mapRowToEvent(ResultSet rs, int rowNum) throws SQLException {
        return Event.builder()
                .eventId(rs.getLong("id"))
                .timestamp(rs.getLong("timestamp"))
                .userId(rs.getLong("user_id"))
                .eventType(rs.getString("event_type"))
                .operation(rs.getString("operation"))
                .entityId(rs.getLong("entity_id"))
                .build();
    }
}
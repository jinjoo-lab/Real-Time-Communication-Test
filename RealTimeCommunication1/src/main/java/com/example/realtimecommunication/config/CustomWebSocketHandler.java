package com.example.realtimecommunication.config;

import com.example.realtimecommunication.module.location.dto.LocationDto;
import com.example.realtimecommunication.module.location.service.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class CustomWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final LocationService locationService;

    private final Map<Long, Set<WebSocketSession>> groupSessions = new ConcurrentHashMap<>();

    public CustomWebSocketHandler(LocationService locationService, ObjectMapper objectMapper) {
        this.locationService = locationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        Long groupId = extractGroupIdFromSession(session);
        LocationDto location = locationService.makeRandomLocation();

        Set<WebSocketSession> set =
                groupSessions.getOrDefault(groupId, new CopyOnWriteArraySet<>());
        set.forEach(
                s -> {
                    if (!s.isOpen()) {
                        set.remove(s);
                        return;
                    }

                    try {
                        log.info("WEBSOCKET : " + location.getX() + " " + location.getY());
                        s.sendMessage(new TextMessage(objectMapper.writeValueAsString(location)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long groupId = (long) (Math.random() * 10);
        Set set = groupSessions.getOrDefault(groupId, new CopyOnWriteArraySet<>());
        set.add(session);
        groupSessions.put(groupId, set);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
            throws Exception {
        Long groupId = extractGroupIdFromSession(session);
        if (groupId != null) {
            Set<WebSocketSession> sessions = groupSessions.get(groupId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    groupSessions.remove(groupId);
                }
            }
        }
    }

    private Long extractGroupIdFromSession(WebSocketSession session) {
        Long groupId = (long) (Math.random() * 10);
        return groupId;
    }
}

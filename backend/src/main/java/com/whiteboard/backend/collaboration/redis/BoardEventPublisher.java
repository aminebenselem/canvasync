package com.whiteboard.backend.collaboration.redis;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BoardEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public BoardEventPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(
            UUID boardId,
            String type,
            Object payload
    ) {

        RedisBoardEvent event =
                new RedisBoardEvent(
                        type,
                        boardId,
                        payload
                );

        try {

            String message =
                    objectMapper.writeValueAsString(event);

            String channel =
                    "board:" + boardId + ":events";

            redisTemplate.convertAndSend(
                    channel,
                    message
            );

        } catch (JacksonException e) {

            throw new IllegalStateException(
                    "Failed to serialize board event",
                    e
            );
        }
    }
}
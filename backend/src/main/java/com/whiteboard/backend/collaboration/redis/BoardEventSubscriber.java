package com.whiteboard.backend.collaboration.redis;

import com.whiteboard.backend.collaboration.events.cursor.CursorLeftEvent;
import com.whiteboard.backend.collaboration.events.cursor.CursorMovedEvent;

import tools.jackson.databind.ObjectMapper;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class BoardEventSubscriber
        implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public BoardEventSubscriber(
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(
            Message message,
            byte[] pattern
    ) {

        try {

            String json =
                    new String(
                            message.getBody(),
                            StandardCharsets.UTF_8
                    );

            RedisBoardEvent event =
                    objectMapper.readValue(
                            json,
                            RedisBoardEvent.class
                    );

            switch (event.type()) {

                case "CURSOR_MOVED" -> {

                    CursorMovedEvent cursor =
                            objectMapper.convertValue(
                                    event.payload(),
                                    CursorMovedEvent.class
                            );

                    messagingTemplate.convertAndSend(
                            "/topic/boards/"
                                    + event.boardId()
                                    + "/cursor",
                            cursor
                    );
                }

                case "DRAWING" -> {
                    messagingTemplate.convertAndSend(
                            "/topic/boards/" + event.boardId() + "/drawing",
                            event
                    );
              }
            case "CURSOR_LEFT" -> {

                    CursorLeftEvent cursorLeft =
                            objectMapper.convertValue(
                                    event.payload(),
                                    CursorLeftEvent.class
                            );

                    messagingTemplate.convertAndSend(
                            "/topic/boards/"
                                    + event.boardId()
                                    + "/cursor-left",
                            cursorLeft
                    );
                }

                case "ELEMENT_CREATED",
                     "ELEMENT_UPDATED",
                     "ELEMENT_DELETED",
                     "ELEMENTS_CLEARED" -> {

                    messagingTemplate.convertAndSend(
                            "/topic/boards/"
                                    + event.boardId()
                                    + "/elements",
                            event
                    );
                }

                default -> {

                    System.out.println(
                            "Unknown board event: "
                                    + event.type()
                    );
                }
            }

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to consume Redis board event",
                    e
            );
        }
    }
}
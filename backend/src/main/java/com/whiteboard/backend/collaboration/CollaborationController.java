package com.whiteboard.backend.collaboration;

import com.whiteboard.backend.collaboration.events.ElementEvent;
import com.whiteboard.backend.collaboration.events.TestEvent;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class WhiteboardWebSocketController {

    @MessageMapping("/boards/{boardId}/elements")
    @SendTo("/topic/boards/{boardId}")
    public ElementEvent handleElement(
            @DestinationVariable UUID boardId,
            ElementEvent event
    ) {
        return event;
    }

    @MessageMapping("/boards/{boardId}/test")
    @SendTo("/topic/boards/{boardId}")
    public TestEvent test(
            @DestinationVariable UUID boardId,
            TestEvent event
    ) {
        System.out.println("Received for board: " + boardId);
        return event;
    }

    @MessageMapping("/test")
    @SendTo("/topic/test")
    public TestEvent test(TestEvent event) {
        return event;
    }
}
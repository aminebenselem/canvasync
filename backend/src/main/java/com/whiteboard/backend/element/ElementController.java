package com.whiteboard.backend.element;

import com.whiteboard.backend.board.exception.UnauthorizedUserException;
import com.whiteboard.backend.element.dto.*;
import com.whiteboard.backend.element.mapper.ElementMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/elements")
public class ElementController {

    private final ElementService elementService;
    private final ElementMapper elementMapper;

    public ElementController(
            ElementService elementService,
            ElementMapper elementMapper
    ) {
        this.elementService = elementService;
        this.elementMapper = elementMapper;
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<ElementDto>> getBoardElements(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        return ResponseEntity.ok(
                elementMapper.toDtoList(
                        elementService.getBoardElements(boardId, userId)
                )
        );
    }

    @PostMapping("/board/{boardId}/shape")
    public ResponseEntity<ElementDto> createShape(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateShapeDto request
    ) {
        Long userId = getUserId(jwt);

        Element element =
                elementService.createShapeElement(boardId, userId, request);

        return ResponseEntity.ok(
                elementMapper.toDto(element)
        );
    }

    @PatchMapping("/board/{boardId}/{elementId}/shape")
    public ResponseEntity<ElementDto> updateShape(
            @PathVariable UUID boardId,
            @PathVariable UUID elementId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateShapeDto request
    ) {
        Long userId = getUserId(jwt);

        Element element =
                elementService.updateShapeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                );

        return ResponseEntity.ok(
                elementMapper.toDto(element)
        );
    }

    @PostMapping("/board/{boardId}/stroke")
    public ResponseEntity<ElementDto> createStroke(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateStrokeDto request
    ) {
        Long userId = getUserId(jwt);

        Element element =
                elementService.createStrokeElement(
                        boardId,
                        userId,
                        request
                );

        return ResponseEntity.ok(
                elementMapper.toDto(element)
        );
    }

    @PatchMapping("/board/{boardId}/{elementId}/stroke")
    public ResponseEntity<ElementDto> updateStroke(
            @PathVariable UUID boardId,
            @PathVariable UUID elementId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateStrokeDto request
    ) {
        Long userId = getUserId(jwt);

        Element element =
                elementService.updateStrokeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                );

        return ResponseEntity.ok(
                elementMapper.toDto(element)
        );
    }

    @DeleteMapping("/board/{boardId}/{elementId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID boardId,
            @PathVariable UUID elementId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        elementService.deleteElement(
                boardId,
                userId,
                elementId
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/board/{boardId}")
    public ResponseEntity<Void> deleteAll(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = getUserId(jwt);

        elementService.deleteAllElements(boardId, userId);

        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/batch/board/{boardId}")
    public ResponseEntity<Void> deleteAllByIds(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DeleteElementsDto request
    ) {
        Long userId = getUserId(jwt);

        elementService.deleteByIds(boardId, userId ,request.elements());

        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new UnauthorizedUserException("JWT or subject is null");
        }

        return Long.parseLong(jwt.getSubject());
    }
}
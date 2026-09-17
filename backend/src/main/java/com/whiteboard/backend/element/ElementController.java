package com.whiteboard.backend.element;

import com.whiteboard.backend.element.dto.CreateShapeDto;
import com.whiteboard.backend.element.dto.CreateStrokeDto;
import com.whiteboard.backend.element.dto.UpdateShapeDto;
import com.whiteboard.backend.element.dto.UpdateStrokeDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/elements")
public class ElementController {

    private final ElementService elementService;

    public ElementController(ElementService elementService) {
        this.elementService = elementService;
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<Element>> getBoardElements(
            @PathVariable UUID boardId,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                elementService.getBoardElements(boardId, userId)
        );
    }

    @PostMapping("/board/{boardId}/shape")
    public ResponseEntity<Element> createShape(
            @PathVariable UUID boardId,
            @RequestParam Long userId,
            @RequestBody CreateShapeDto request
    ) {
        return ResponseEntity.ok(
                elementService.createShapeElement(boardId, userId, request)
        );
    }

    @PatchMapping("/board/{boardId}/{elementId}/shape")
    public ResponseEntity<Element> updateShape(
            @PathVariable UUID boardId,
            @PathVariable UUID elementId,
            @RequestParam Long userId,
            @RequestBody UpdateShapeDto request
    ) {
        return ResponseEntity.ok(
                elementService.updateShapeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                )
        );
    }

    @PostMapping("/board/{boardId}/stroke")
    public ResponseEntity<Element> createStroke(
            @PathVariable UUID boardId,
            @RequestParam Long userId,
            @RequestBody CreateStrokeDto request
    ) {
        return ResponseEntity.ok(
                elementService.createStrokeElement(boardId, userId, request)
        );
    }

    @PatchMapping("/board/{boardId}/{elementId}/stroke")
    public ResponseEntity<Element> updateStroke(
            @PathVariable UUID boardId,
            @PathVariable UUID elementId,
            @RequestParam Long userId,
            @RequestBody UpdateStrokeDto request
    ) {
        return ResponseEntity.ok(
                elementService.updateStrokeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                )
        );
    }

    @DeleteMapping("/board/{boardId}/{elementId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID boardId,
            @PathVariable UUID elementId,
            @RequestParam Long userId
    ) {
        elementService.deleteElement(boardId, userId, elementId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/board/{boardId}")
    public ResponseEntity<Void> deleteAll(
            @PathVariable UUID boardId,
            @RequestParam Long userId
    ) {
        elementService.deleteAllElements(boardId, userId);
        return ResponseEntity.noContent().build();
    }
}
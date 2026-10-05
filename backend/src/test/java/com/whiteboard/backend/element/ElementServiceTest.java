
        package com.whiteboard.backend.element;

import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.BoardRepository;
import com.whiteboard.backend.board.access.BoardAccessPolicy;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.collaboration.events.element.ElementChangeEvent;
import com.whiteboard.backend.collaboration.redis.BoardEventPublisher;
import com.whiteboard.backend.element.dto.*;
import com.whiteboard.backend.element.exception.ElementAccessDeniedException;
import com.whiteboard.backend.element.exception.ElementNotFoundException;
import com.whiteboard.backend.element.mapper.ElementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElementServiceTest {

    @Mock
    private ElementRepository elementRepository;

    @Mock
    private BoardAccessPolicy boardAccessPolicy;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ElementMapper elementMapper;

    @Mock
    private BoardEventPublisher eventPublisher;

    @InjectMocks
    private ElementService elementService;

    private UUID boardId;
    private Long userId;

    @BeforeEach
    void setUp() {
        boardId = UUID.randomUUID();
        userId = 1L;
    }

    // =========================================================
    // GET BOARD ELEMENTS
    // =========================================================

    @Test
    void getBoardElements_shouldRejectUserWithoutViewPermission() {

        when(boardAccessPolicy.canView(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.getBoardElements(boardId, userId)
        );

        verify(elementRepository, never())
                .findByBoardId(boardId);
    }

    @Test
    void getBoardElements_shouldReturnElementsWhenUserCanViewBoard() {

        Element element = new Element();

        when(boardAccessPolicy.canView(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findByBoardId(boardId))
                .thenReturn(List.of(element));

        List<Element> result =
                elementService.getBoardElements(boardId, userId);

        assertEquals(1, result.size());
        assertSame(element, result.get(0));

        verify(elementRepository)
                .findByBoardId(boardId);
    }

    // =========================================================
    // CREATE SHAPE
    // =========================================================

    @Test
    void createShapeElement_shouldRejectUserWithoutEditPermission() {

        CreateShapeDto request = mock(CreateShapeDto.class);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.createShapeElement(
                        boardId,
                        userId,
                        request
                )
        );

        verify(boardRepository, never())
                .getReferenceById(any());

        verify(elementRepository, never())
                .save(any());

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void createShapeElement_shouldCreateAndPersistElement() {

        CreateShapeDto request = mock(CreateShapeDto.class);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(request.type())
                .thenReturn(ElementType.RECTANGLE);

        when(request.text())
                .thenReturn("Rectangle");

        when(request.startPoint())
                .thenReturn(null);

        when(request.endPoint())
                .thenReturn(null);

        when(request.color())
                .thenReturn("#000000");

        when(request.width())
                .thenReturn(2.0F);

        Board board = new Board();
        board.setId(boardId);

        when(boardRepository.getReferenceById(boardId))
                .thenReturn(board);

        Element savedElement = new Element();
        savedElement.setBoard(board);
        savedElement.setType(ElementType.RECTANGLE);

        when(elementRepository.save(any(Element.class)))
                .thenReturn(savedElement);

        ElementDto dto = mock(ElementDto.class);

        when(elementMapper.toDto(savedElement))
                .thenReturn(dto);

        Element result =
                elementService.createShapeElement(
                        boardId,
                        userId,
                        request
                );

        assertSame(savedElement, result);

        ArgumentCaptor<Element> elementCaptor =
                ArgumentCaptor.forClass(Element.class);

        verify(elementRepository)
                .save(elementCaptor.capture());

        Element persisted = elementCaptor.getValue();

        assertSame(board, persisted.getBoard());
        assertEquals(ElementType.RECTANGLE, persisted.getType());

        assertEquals(
                "rectangle",
                persisted.getData().get("type")
        );

        assertEquals(
                "Rectangle",
                persisted.getData().get("text")
        );

        assertEquals(
                "#000000",
                persisted.getData().get("color")
        );

        assertEquals(
                2.0F,
                persisted.getData().get("width")
        );

        verify(boardRepository)
                .getReferenceById(boardId);

        verify(eventPublisher)
                .publish(
                        eq(boardId),
                        eq("ELEMENT_CREATED"),
                        any(ElementChangeEvent.class)
                );
    }

    // =========================================================
    // UPDATE SHAPE
    // =========================================================

    @Test
    void updateShapeElement_shouldRejectUserWithoutEditPermission() {

        UUID elementId = UUID.randomUUID();

        UpdateShapeDto request = mock(UpdateShapeDto.class);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.updateShapeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                )
        );

        verify(elementRepository, never())
                .findById(elementId);

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void updateShapeElement_shouldThrowWhenElementDoesNotExist() {

        UUID elementId = UUID.randomUUID();

        UpdateShapeDto request = mock(UpdateShapeDto.class);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findById(elementId))
                .thenReturn(Optional.empty());

        assertThrows(
                ElementNotFoundException.class,
                () -> elementService.updateShapeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                )
        );

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void updateShapeElement_shouldRejectElementFromAnotherBoard() {

        UUID elementId = UUID.randomUUID();

        UUID otherBoardId = UUID.randomUUID();

        UpdateShapeDto request = mock(UpdateShapeDto.class);

        Board otherBoard = new Board();
        otherBoard.setId(otherBoardId);

        Element element = new Element();
        element.setBoard(otherBoard);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findById(elementId))
                .thenReturn(Optional.of(element));

        assertThrows(
                ElementAccessDeniedException.class,
                () -> elementService.updateShapeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                )
        );

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void updateShapeElement_shouldUpdateElementAndPublishEvent() {

        UUID elementId = UUID.randomUUID();

        UpdateShapeDto request = mock(UpdateShapeDto.class);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(request.type())
                .thenReturn(ElementType.RECTANGLE);

        when(request.text())
                .thenReturn("Updated");

        when(request.startPoint())
                .thenReturn(null);

        when(request.endPoint())
                .thenReturn(null);

        when(request.color())
                .thenReturn("#ff0000");

        when(request.width())
                .thenReturn(4.0F);

        Board board = new Board();
        board.setId(boardId);

        Element element = new Element();
        element.setBoard(board);
        element.setType(ElementType.RECTANGLE);

        when(elementRepository.findById(elementId))
                .thenReturn(Optional.of(element));

        ElementDto dto = mock(ElementDto.class);

        when(elementMapper.toDto(element))
                .thenReturn(dto);

        Element result =
                elementService.updateShapeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                );

        assertSame(element, result);

        assertEquals(
                "rectangle",
                element.getData().get("type")
        );

        assertEquals(
                "Updated",
                element.getData().get("text")
        );

        assertEquals(
                "#ff0000",
                element.getData().get("color")
        );

        assertEquals(
                4.0F,
                element.getData().get("width")
        );

        // updateShapeElement does not call save().
        verify(elementRepository, never())
                .save(any(Element.class));

        verify(eventPublisher)
                .publish(
                        eq(boardId),
                        eq("ELEMENT_UPDATED"),
                        any(ElementChangeEvent.class)
                );
    }

    // =========================================================
    // DELETE ELEMENT
    // =========================================================

    @Test
    void deleteElement_shouldRejectUserWithoutEditPermission() {

        UUID elementId = UUID.randomUUID();

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.deleteElement(
                        boardId,
                        userId,
                        elementId
                )
        );

        verify(elementRepository, never())
                .findById(elementId);

        verify(elementRepository, never())
                .delete(any(Element.class));
    }

    @Test
    void deleteElement_shouldThrowWhenElementDoesNotExist() {

        UUID elementId = UUID.randomUUID();

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findById(elementId))
                .thenReturn(Optional.empty());

        assertThrows(
                ElementNotFoundException.class,
                () -> elementService.deleteElement(
                        boardId,
                        userId,
                        elementId
                )
        );

        verify(elementRepository, never())
                .delete(any(Element.class));

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void deleteElement_shouldRejectElementFromAnotherBoard() {

        UUID elementId = UUID.randomUUID();

        UUID otherBoardId = UUID.randomUUID();

        Board otherBoard = new Board();
        otherBoard.setId(otherBoardId);

        Element element = new Element();
        element.setBoard(otherBoard);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findById(elementId))
                .thenReturn(Optional.of(element));

        assertThrows(
                ElementAccessDeniedException.class,
                () -> elementService.deleteElement(
                        boardId,
                        userId,
                        elementId
                )
        );

        verify(elementRepository, never())
                .delete(any(Element.class));

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void deleteElement_shouldDeleteElementAndPublishEvent() {

        UUID elementId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);

        Element element = new Element();
        element.setBoard(board);
        element.setId(elementId);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findById(elementId))
                .thenReturn(Optional.of(element));

        elementService.deleteElement(
                boardId,
                userId,
                elementId
        );

        verify(elementRepository)
                .delete(element);

        verify(eventPublisher)
                .publish(
                        eq(boardId),
                        eq("ELEMENT_DELETED"),
                        any(ElementChangeEvent.class)
                );
    }

    // =========================================================
    // DELETE ALL ELEMENTS
    // =========================================================

    @Test
    void deleteAllElements_shouldRejectUserWithoutEditPermission() {

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.deleteAllElements(
                        boardId,
                        userId
                )
        );

        verify(elementRepository, never())
                .deleteByBoardId(boardId);

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void deleteAllElements_shouldDeleteAllElementsAndPublishEvent() {

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        elementService.deleteAllElements(
                boardId,
                userId
        );

        verify(elementRepository)
                .deleteByBoardId(boardId);

        verify(eventPublisher)
                .publish(
                        eq(boardId),
                        eq("ELEMENTS_CLEARED"),
                        any(ElementChangeEvent.class)
                );
    }

    // =========================================================
    // DELETE BY IDS
    // =========================================================

    @Test
    void deleteByIds_shouldRejectUserWithoutEditPermission() {

        List<UUID> elementIds = List.of(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.deleteByIds(
                        boardId,
                        userId,
                        elementIds
                )
        );

        verify(elementRepository, never())
                .findAllById(any());

        verify(elementRepository, never())
                .deleteAllById(any());

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void deleteByIds_shouldDeleteOnlyElementsBelongingToBoard() {

        UUID ownedId1 = UUID.randomUUID();
        UUID ownedId2 = UUID.randomUUID();
        UUID foreignId = UUID.randomUUID();

        List<UUID> requestedIds = List.of(
                ownedId1,
                ownedId2,
                foreignId
        );

        Board board = new Board();
        board.setId(boardId);

        Board otherBoard = new Board();
        otherBoard.setId(UUID.randomUUID());

        Element ownedElement1 = new Element();
        ownedElement1.setId(ownedId1);
        ownedElement1.setBoard(board);

        Element ownedElement2 = new Element();
        ownedElement2.setId(ownedId2);
        ownedElement2.setBoard(board);

        Element foreignElement = new Element();
        foreignElement.setId(foreignId);
        foreignElement.setBoard(otherBoard);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findAllById(requestedIds))
                .thenReturn(List.of(
                        ownedElement1,
                        ownedElement2,
                        foreignElement
                ));

        elementService.deleteByIds(
                boardId,
                userId,
                requestedIds
        );

        verify(elementRepository)
                .deleteAllById(
                        List.of(ownedId1, ownedId2)
                );

        verify(eventPublisher)
                .publish(
                        eq(boardId),
                        eq("ELEMENT_DELETED"),
                        any(ElementChangeEvent.class)
                );
    }

    @Test
    void deleteByIds_shouldDoNothingWhenNoElementsBelongToBoard() {

        UUID foreignId = UUID.randomUUID();

        List<UUID> requestedIds = List.of(foreignId);

        Board otherBoard = new Board();
        otherBoard.setId(UUID.randomUUID());

        Element foreignElement = new Element();
        foreignElement.setId(foreignId);
        foreignElement.setBoard(otherBoard);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findAllById(requestedIds))
                .thenReturn(List.of(foreignElement));

        elementService.deleteByIds(
                boardId,
                userId,
                requestedIds
        );

        verify(elementRepository, never())
                .deleteAllById(any());

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void deleteByIds_shouldDoNothingWhenNoElementsAreFound() {

        List<UUID> requestedIds = List.of(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findAllById(requestedIds))
                .thenReturn(List.of());

        elementService.deleteByIds(
                boardId,
                userId,
                requestedIds
        );

        verify(elementRepository, never())
                .deleteAllById(any());

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    // =========================================================
    // CREATE STROKE
    // =========================================================

    @Test
    void createStrokeElement_shouldRejectUserWithoutEditPermission() {

        CreateStrokeDto request = mock(CreateStrokeDto.class);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.createStrokeElement(
                        boardId,
                        userId,
                        request
                )
        );

        verify(boardRepository, never())
                .getReferenceById(any());

        verify(elementRepository, never())
                .save(any());

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void createStrokeElement_shouldCreateAndPersistElement() {

        CreateStrokeDto request = mock(CreateStrokeDto.class);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        Point[] points = new Point[]{
                new Point(10, 10),
                new Point(20, 20)
        };

        when(request.points())
                .thenReturn(points);

        when(request.color())
                .thenReturn("#000000");

        when(request.width())
                .thenReturn(2.0F);

        Board board = new Board();
        board.setId(boardId);

        when(boardRepository.getReferenceById(boardId))
                .thenReturn(board);

        Element savedElement = new Element();
        savedElement.setBoard(board);
        savedElement.setType(ElementType.STROKE);

        when(elementRepository.save(any(Element.class)))
                .thenReturn(savedElement);

        ElementDto dto = mock(ElementDto.class);

        when(elementMapper.toDto(savedElement))
                .thenReturn(dto);

        Element result =
                elementService.createStrokeElement(
                        boardId,
                        userId,
                        request
                );

        assertSame(savedElement, result);

        ArgumentCaptor<Element> elementCaptor =
                ArgumentCaptor.forClass(Element.class);

        verify(elementRepository)
                .save(elementCaptor.capture());

        Element persisted = elementCaptor.getValue();

        assertSame(board, persisted.getBoard());
        assertEquals(
                ElementType.STROKE,
                persisted.getType()
        );

        assertSame(
                points,
                persisted.getData().get("points")
        );

        assertEquals(
                "#000000",
                persisted.getData().get("color")
        );

        assertEquals(
                2.0F,
                persisted.getData().get("width")
        );

        verify(boardRepository)
                .getReferenceById(boardId);

        verify(eventPublisher)
                .publish(
                        eq(boardId),
                        eq("ELEMENT_CREATED"),
                        any(ElementChangeEvent.class)
                );
    }

    // =========================================================
    // UPDATE STROKE
    // =========================================================

    @Test
    void updateStrokeElement_shouldRejectUserWithoutEditPermission() {

        UUID elementId = UUID.randomUUID();

        UpdateStrokeDto request = mock(UpdateStrokeDto.class);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.updateStrokeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                )
        );

        verify(elementRepository, never())
                .findById(elementId);

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void updateStrokeElement_shouldThrowWhenElementDoesNotExist() {

        UUID elementId = UUID.randomUUID();

        UpdateStrokeDto request = mock(UpdateStrokeDto.class);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findById(elementId))
                .thenReturn(Optional.empty());

        assertThrows(
                ElementNotFoundException.class,
                () -> elementService.updateStrokeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                )
        );

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void updateStrokeElement_shouldRejectElementFromAnotherBoard() {

        UUID elementId = UUID.randomUUID();

        UUID otherBoardId = UUID.randomUUID();

        UpdateStrokeDto request = mock(UpdateStrokeDto.class);

        Board otherBoard = new Board();
        otherBoard.setId(otherBoardId);

        Element element = new Element();
        element.setBoard(otherBoard);

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(elementRepository.findById(elementId))
                .thenReturn(Optional.of(element));

        assertThrows(
                ElementAccessDeniedException.class,
                () -> elementService.updateStrokeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                )
        );

        verify(eventPublisher, never())
                .publish(any(), any(), any());
    }

    @Test
    void updateStrokeElement_shouldUpdateElementAndPublishEvent() {

        UUID elementId = UUID.randomUUID();

        UpdateStrokeDto request = mock(UpdateStrokeDto.class);

        Point[] points = new Point[]{
                new Point(1, 1),
                new Point(2, 2),
                new Point(3, 3)
        };

        when(boardAccessPolicy.canEdit(boardId, userId))
                .thenReturn(true);

        when(request.points())
                .thenReturn(points);

        when(request.color())
                .thenReturn("#ff0000");

        when(request.width())
                .thenReturn(5.0F);

        Board board = new Board();
        board.setId(boardId);

        Element element = new Element();
        element.setBoard(board);
        element.setType(ElementType.STROKE);

        when(elementRepository.findById(elementId))
                .thenReturn(Optional.of(element));

        ElementDto dto = mock(ElementDto.class);

        when(elementMapper.toDto(element))
                .thenReturn(dto);

        Element result =
                elementService.updateStrokeElement(
                        boardId,
                        userId,
                        elementId,
                        request
                );

        assertSame(element, result);

        assertSame(
                points,
                element.getData().get("points")
        );

        assertEquals(
                "#ff0000",
                element.getData().get("color")
        );

        assertEquals(
                5.0F,
                element.getData().get("width")
        );

        // The entity is already managed.
        // updateStrokeElement does not explicitly call save().
        verify(elementRepository, never())
                .save(any(Element.class));

        verify(eventPublisher)
                .publish(
                        eq(boardId),
                        eq("ELEMENT_UPDATED"),
                        any(ElementChangeEvent.class)
                );
    }
}

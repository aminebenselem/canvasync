package com.whiteboard.backend.element;

import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.BoardRepository;
import com.whiteboard.backend.board.access.BoardAccessPolicy;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.collaboration.redis.BoardEventPublisher;
import com.whiteboard.backend.element.dto.*;
import com.whiteboard.backend.element.exception.ElementAccessDeniedException;
import com.whiteboard.backend.element.exception.ElementNotFoundException;
import com.whiteboard.backend.user.User;
import com.whiteboard.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ElementServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private ElementService elementService;

    @Autowired
    private ElementRepository elementRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private BoardAccessPolicy boardAccessPolicy;

    @MockitoBean
    private BoardEventPublisher eventPublisher;

    private User owner;
    private Board board;

    @BeforeEach
    void setUp() {

        elementRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();

        owner = createUser(
                "owner-" + UUID.randomUUID(),
                "owner-" + UUID.randomUUID() + "@test.com"
        );

        board = new Board();
        board.setName("Test Board");
        board.setOwner(owner);

        board = boardRepository.save(board);
    }

    // =========================================================
    // GET
    // =========================================================

    @Test
    void getBoardElements_shouldReturnElementsForAuthorizedUser() {

        when(boardAccessPolicy.canView(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Element element = new Element();

        element.setBoard(board);
        element.setType(ElementType.RECTANGLE);
        element.setData(
                Map.of(
                        "text", "Hello",
                        "color", "#000000"
                )
        );

        element = elementRepository.save(element);

        List<Element> result =
                elementService.getBoardElements(
                        board.getId(),
                        owner.getId()
                );

        assertEquals(1, result.size());

        assertEquals(
                element.getId(),
                result.get(0).getId()
        );
    }

    @Test
    void getBoardElements_shouldRejectUnauthorizedUser() {

        User randomUser = createUser(
                "random-" + UUID.randomUUID(),
                "random-" + UUID.randomUUID() + "@test.com"
        );

        when(boardAccessPolicy.canView(
                board.getId(),
                randomUser.getId()
        )).thenReturn(false);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.getBoardElements(
                        board.getId(),
                        randomUser.getId()
                )
        );
    }

    // =========================================================
    // CREATE SHAPE
    // =========================================================

    @Test
    void createShapeElement_shouldPersistElement() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        CreateShapeDto request =
                new CreateShapeDto(
                        ElementType.RECTANGLE,
                        "Rectangle",
                        new Point(10, 10),
                        new Point(100, 100),
                        "#000000",
                        2
                );

        Element result =
                elementService.createShapeElement(
                        board.getId(),
                        owner.getId(),
                        request
                );

        assertNotNull(result.getId());

        assertEquals(
                ElementType.RECTANGLE,
                result.getType()
        );

        assertEquals(
                board.getId(),
                result.getBoard().getId()
        );

        assertEquals(
                "rectangle",
                result.getData().get("type")
        );

        assertEquals(
                "Rectangle",
                result.getData().get("text")
        );

        assertEquals(
                "#000000",
                result.getData().get("color")
        );

        assertEquals(
                2.0,
                number(result.getData(), "width")
        );

        Element persisted =
                elementRepository
                        .findById(result.getId())
                        .orElseThrow();

        assertEquals(
                board.getId(),
                persisted.getBoard().getId()
        );

        assertEquals(
                ElementType.RECTANGLE,
                persisted.getType()
        );

        assertEquals(
                2.0,
                number(persisted.getData(), "width")
        );
    }

    @Test
    void createShapeElement_shouldRejectUnauthorizedUser() {

        User randomUser = createUser(
                "create-random-" + UUID.randomUUID(),
                "create-random-" + UUID.randomUUID() + "@test.com"
        );

        when(boardAccessPolicy.canEdit(
                board.getId(),
                randomUser.getId()
        )).thenReturn(false);

        CreateShapeDto request =
                new CreateShapeDto(
                        ElementType.RECTANGLE,
                        "Rectangle",
                        new Point(10, 10),
                        new Point(100, 100),
                        "#000000",
                        2
                );

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.createShapeElement(
                        board.getId(),
                        randomUser.getId(),
                        request
                )
        );

        assertTrue(
                elementRepository
                        .findByBoardId(board.getId())
                        .isEmpty()
        );
    }

    // =========================================================
    // UPDATE SHAPE
    // =========================================================

    @Test
    void updateShapeElement_shouldUpdatePersistedElement() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Element element = new Element();

        element.setBoard(board);
        element.setType(ElementType.RECTANGLE);

        element.setData(
                Map.of(
                        "type", "rectangle",
                        "text", "Old",
                        "color", "#000000",
                        "width", 2
                )
        );

        element = elementRepository.save(element);

        UUID elementId = element.getId();

        UpdateShapeDto request =
                new UpdateShapeDto(
                        ElementType.RECTANGLE,
                        "#ff0000",
                        4,
                        new Point(20, 20),
                        new Point(200, 200),
                        "Updated"
                );

        Element result =
                elementService.updateShapeElement(
                        board.getId(),
                        owner.getId(),
                        elementId,
                        request
                );

        assertEquals(
                elementId,
                result.getId()
        );

        assertEquals(
                "Updated",
                result.getData().get("text")
        );

        assertEquals(
                "#ff0000",
                result.getData().get("color")
        );

        assertEquals(
                4.0,
                number(result.getData(), "width")
        );

        Element persisted =
                elementRepository
                        .findById(elementId)
                        .orElseThrow();

        assertEquals(
                "Updated",
                persisted.getData().get("text")
        );

        assertEquals(
                "#ff0000",
                persisted.getData().get("color")
        );

        assertEquals(
                4.0,
                number(persisted.getData(), "width")
        );
    }

    @Test
    void updateShapeElement_shouldThrowWhenElementDoesNotExist() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        UUID elementId = UUID.randomUUID();

        UpdateShapeDto request =
                new UpdateShapeDto(
                        ElementType.RECTANGLE,
                        "#ff0000",
                        4,
                        new Point(20, 20),
                        new Point(200, 200),
                        "Updated"
                );

        assertThrows(
                ElementNotFoundException.class,
                () -> elementService.updateShapeElement(
                        board.getId(),
                        owner.getId(),
                        elementId,
                        request
                )
        );
    }

    @Test
    void updateShapeElement_shouldRejectElementFromAnotherBoard() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Board otherBoard = new Board();
        otherBoard.setName("Other Board");
        otherBoard.setOwner(owner);

        otherBoard =
                boardRepository.save(otherBoard);

        Element element = new Element();

        element.setBoard(otherBoard);
        element.setType(ElementType.RECTANGLE);

        element.setData(
                Map.of(
                        "text", "Protected"
                )
        );

        element =
                elementRepository.save(element);

        UpdateShapeDto request =
                new UpdateShapeDto(
                        ElementType.RECTANGLE,
                        "#ff0000",
                        10,
                        new Point(1, 1),
                        new Point(2, 2),
                        "Hacked"
                );

        UUID elementId = element.getId();

        assertThrows(
                ElementAccessDeniedException.class,
                () -> elementService.updateShapeElement(
                        board.getId(),
                        owner.getId(),
                        elementId,
                        request
                )
        );

        Element persisted =
                elementRepository
                        .findById(elementId)
                        .orElseThrow();

        assertEquals(
                "Protected",
                persisted.getData().get("text")
        );
    }

    // =========================================================
    // CREATE STROKE
    // =========================================================

    @Test
    void createStrokeElement_shouldPersistElement() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Point[] points = {
                new Point(10, 10),
                new Point(20, 20),
                new Point(30, 30)
        };

        CreateStrokeDto request =
                new CreateStrokeDto(
                        "#000000",
                        2,
                        points
                );

        Element result =
                elementService.createStrokeElement(
                        board.getId(),
                        owner.getId(),
                        request
                );

        assertNotNull(result.getId());

        assertEquals(
                ElementType.STROKE,
                result.getType()
        );

        assertEquals(
                board.getId(),
                result.getBoard().getId()
        );

        assertEquals(
                "#000000",
                result.getData().get("color")
        );

        assertEquals(
                2.0,
                number(result.getData(), "width")
        );

        assertNotNull(result.getData().get("points"));

        Element persisted =
                elementRepository
                        .findById(result.getId())
                        .orElseThrow();

        assertEquals(
                ElementType.STROKE,
                persisted.getType()
        );

        assertEquals(
                2.0,
                number(persisted.getData(), "width")
        );

        assertTrue(
                elementRepository
                        .existsById(result.getId())
        );
    }

    @Test
    void createStrokeElement_shouldRejectUnauthorizedUser() {

        User randomUser = createUser(
                "stroke-random-" + UUID.randomUUID(),
                "stroke-random-" + UUID.randomUUID() + "@test.com"
        );

        when(boardAccessPolicy.canEdit(
                board.getId(),
                randomUser.getId()
        )).thenReturn(false);

        CreateStrokeDto request =
                new CreateStrokeDto(
                        "#000000",
                        2,
                        new Point[]{
                                new Point(10, 10),
                                new Point(20, 20)
                        }
                );

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.createStrokeElement(
                        board.getId(),
                        randomUser.getId(),
                        request
                )
        );

        assertTrue(
                elementRepository
                        .findByBoardId(board.getId())
                        .isEmpty()
        );
    }

    // =========================================================
    // UPDATE STROKE
    // =========================================================

    @Test
    void updateStrokeElement_shouldUpdatePersistedElement() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Element element = new Element();

        element.setBoard(board);
        element.setType(ElementType.STROKE);

        element.setData(
                Map.of(
                        "points", new Point[]{
                                new Point(1, 1)
                        },
                        "color", "#000000",
                        "width", 2
                )
        );

        element =
                elementRepository.save(element);

        UUID elementId = element.getId();

        Point[] updatedPoints = {
                new Point(10, 10),
                new Point(20, 20),
                new Point(30, 30)
        };

        UpdateStrokeDto request =
                new UpdateStrokeDto(
                        "#ff0000",
                        5,
                        updatedPoints
                );

        Element result =
                elementService.updateStrokeElement(
                        board.getId(),
                        owner.getId(),
                        elementId,
                        request
                );

        assertEquals(
                elementId,
                result.getId()
        );

        assertEquals(
                "#ff0000",
                result.getData().get("color")
        );

        assertEquals(
                5.0,
                number(result.getData(), "width")
        );

        assertNotNull(
                result.getData().get("points")
        );

        Element persisted =
                elementRepository
                        .findById(elementId)
                        .orElseThrow();

        assertEquals(
                "#ff0000",
                persisted.getData().get("color")
        );

        assertEquals(
                5.0,
                number(persisted.getData(), "width")
        );
    }

    @Test
    void updateStrokeElement_shouldThrowWhenElementDoesNotExist() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        UUID elementId = UUID.randomUUID();

        UpdateStrokeDto request =
                new UpdateStrokeDto(
                        "#ff0000",
                        5,
                        new Point[]{
                                new Point(10, 10)
                        }
                );

        assertThrows(
                ElementNotFoundException.class,
                () -> elementService.updateStrokeElement(
                        board.getId(),
                        owner.getId(),
                        elementId,
                        request
                )
        );
    }

    @Test
    void updateStrokeElement_shouldRejectElementFromAnotherBoard() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Board otherBoard = new Board();
        otherBoard.setName("Other Board");
        otherBoard.setOwner(owner);

        otherBoard =
                boardRepository.save(otherBoard);

        Element element = new Element();

        element.setBoard(otherBoard);
        element.setType(ElementType.STROKE);

        element.setData(
                Map.of(
                        "color", "#000000"
                )
        );

        element =
                elementRepository.save(element);

        UUID elementId = element.getId();

        UpdateStrokeDto request =
                new UpdateStrokeDto(
                        "#ff0000",
                        10,
                        new Point[]{
                                new Point(100, 100)
                        }
                );

        assertThrows(
                ElementAccessDeniedException.class,
                () -> elementService.updateStrokeElement(
                        board.getId(),
                        owner.getId(),
                        elementId,
                        request
                )
        );

        Element persisted =
                elementRepository
                        .findById(elementId)
                        .orElseThrow();

        assertEquals(
                "#000000",
                persisted.getData().get("color")
        );
    }

    // =========================================================
    // DELETE ONE
    // =========================================================

    @Test
    void deleteElement_shouldDeletePersistedElement() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Element element = new Element();

        element.setBoard(board);
        element.setType(ElementType.RECTANGLE);
        element.setData(
                Map.of(
                        "text", "Delete me"
                )
        );

        element =
                elementRepository.save(element);

        UUID elementId = element.getId();

        elementService.deleteElement(
                board.getId(),
                owner.getId(),
                elementId
        );

        assertFalse(
                elementRepository
                        .existsById(elementId)
        );
    }

    @Test
    void deleteElement_shouldRejectElementFromAnotherBoard() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Board otherBoard = new Board();
        otherBoard.setName("Other Board");
        otherBoard.setOwner(owner);

        otherBoard =
                boardRepository.save(otherBoard);

        Element element = new Element();

        element.setBoard(otherBoard);
        element.setType(ElementType.RECTANGLE);
        element.setData(
                Map.of(
                        "text", "Protected"
                )
        );

        element =
                elementRepository.save(element);

        UUID elementId = element.getId();

        assertThrows(
                ElementAccessDeniedException.class,
                () -> elementService.deleteElement(
                        board.getId(),
                        owner.getId(),
                        elementId
                )
        );

        assertTrue(
                elementRepository
                        .existsById(elementId)
        );
    }

    @Test
    void deleteElement_shouldThrowWhenElementDoesNotExist() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        UUID elementId = UUID.randomUUID();

        assertThrows(
                ElementNotFoundException.class,
                () -> elementService.deleteElement(
                        board.getId(),
                        owner.getId(),
                        elementId
                )
        );
    }

    // =========================================================
    // DELETE ALL
    // =========================================================

    @Test
    void deleteAllElements_shouldDeleteAllBoardElements() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        for (int i = 0; i < 3; i++) {

            Element element = new Element();

            element.setBoard(board);
            element.setType(ElementType.RECTANGLE);
            element.setData(
                    Map.of(
                            "index", i
                    )
            );

            elementRepository.save(element);
        }

        assertEquals(
                3,
                elementRepository
                        .findByBoardId(board.getId())
                        .size()
        );

        elementService.deleteAllElements(
                board.getId(),
                owner.getId()
        );

        assertTrue(
                elementRepository
                        .findByBoardId(board.getId())
                        .isEmpty()
        );
    }

    @Test
    void deleteAllElements_shouldRejectUnauthorizedUser() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(false);

        Element element = new Element();

        element.setBoard(board);
        element.setType(ElementType.RECTANGLE);
        element.setData(
                Map.of(
                        "text", "Protected"
                )
        );

        elementRepository.save(element);

        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.deleteAllElements(
                        board.getId(),
                        owner.getId()
                )
        );

        assertEquals(
                1,
                elementRepository
                        .findByBoardId(board.getId())
                        .size()
        );
    }

    // =========================================================
    // DELETE BY IDS
    // =========================================================

    @Test
    void deleteByIds_shouldDeleteOnlyElementsFromCurrentBoard() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Board otherBoard = new Board();
        otherBoard.setName("Other Board");
        otherBoard.setOwner(owner);

        otherBoard =
                boardRepository.save(otherBoard);

        Element boardElement1 = new Element();

        boardElement1.setBoard(board);
        boardElement1.setType(ElementType.RECTANGLE);
        boardElement1.setData(
                Map.of("index", 1)
        );

        boardElement1 =
                elementRepository.save(boardElement1);

        Element boardElement2 = new Element();

        boardElement2.setBoard(board);
        boardElement2.setType(ElementType.RECTANGLE);
        boardElement2.setData(
                Map.of("index", 2)
        );

        boardElement2 =
                elementRepository.save(boardElement2);

        Element foreignElement = new Element();

        foreignElement.setBoard(otherBoard);
        foreignElement.setType(ElementType.RECTANGLE);
        foreignElement.setData(
                Map.of("index", 3)
        );

        foreignElement =
                elementRepository.save(foreignElement);

        List<UUID> ids = List.of(
                boardElement1.getId(),
                boardElement2.getId(),
                foreignElement.getId()
        );

        elementService.deleteByIds(
                board.getId(),
                owner.getId(),
                ids
        );

        assertFalse(
                elementRepository
                        .existsById(boardElement1.getId())
        );

        assertFalse(
                elementRepository
                        .existsById(boardElement2.getId())
        );

        // Element belonging to another board survives.
        assertTrue(
                elementRepository
                        .existsById(foreignElement.getId())
        );
    }

    @Test
    void deleteByIds_shouldDoNothingWhenIdsDoNotBelongToBoard() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(true);

        Board otherBoard = new Board();
        otherBoard.setName("Other Board");
        otherBoard.setOwner(owner);

        otherBoard =
                boardRepository.save(otherBoard);

        Element foreignElement = new Element();

        foreignElement.setBoard(otherBoard);
        foreignElement.setType(ElementType.RECTANGLE);
        foreignElement.setData(
                Map.of(
                        "text", "Protected"
                )
        );

        foreignElement =
                elementRepository.save(foreignElement);

        elementService.deleteByIds(
                board.getId(),
                owner.getId(),
                List.of(foreignElement.getId())
        );

        assertTrue(
                elementRepository
                        .existsById(foreignElement.getId())
        );
    }

    @Test
    void deleteByIds_shouldRejectUnauthorizedUser() {

        when(boardAccessPolicy.canEdit(
                board.getId(),
                owner.getId()
        )).thenReturn(false);

        Element element = new Element();

        element.setBoard(board);
        element.setType(ElementType.RECTANGLE);
        element.setData(
                Map.of("text", "Protected")
        );

        element =
                elementRepository.save(element);

        Element finalElement = element;
        assertThrows(
                BoardAccessDeniedException.class,
                () -> elementService.deleteByIds(
                        board.getId(),
                        owner.getId(),
                        List.of(finalElement.getId())
                )
        );

        assertTrue(
                elementRepository
                        .existsById(element.getId())
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Reads a numeric value from JSON/JSONB data without caring
     * whether Jackson/PostgreSQL returned Integer, Float, Double,
     * Long, etc.
     */
    private static double number(
            Map<String, Object> data,
            String key
    ) {
        Object value = data.get(key);

        assertNotNull(
                value,
                "Expected numeric value for key: " + key
        );

        assertInstanceOf(
                Number.class,
                value,
                "Expected a Number for key: " + key
        );

        return ((Number) value).doubleValue();
    }

    private User createUser(
            String username,
            String email
    ) {

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");

        return userRepository.save(user);
    }
}
package com.whiteboard.backend.element;

import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.BoardRepository;
import com.whiteboard.backend.board.access.BoardAccessPolicy;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.collaboration.events.element.ElementChangeEvent;
import com.whiteboard.backend.collaboration.redis.BoardEventPublisher;
import com.whiteboard.backend.element.dto.CreateShapeDto;
import com.whiteboard.backend.element.dto.CreateStrokeDto;
import com.whiteboard.backend.element.dto.UpdateShapeDto;
import com.whiteboard.backend.element.dto.UpdateStrokeDto;
import com.whiteboard.backend.element.exception.ElementAccessDeniedException;
import com.whiteboard.backend.element.exception.ElementNotFoundException;
import com.whiteboard.backend.element.mapper.ElementMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ElementService {

    private static final Logger log =
            LoggerFactory.getLogger(ElementService.class);

    private static final String ELEMENT_CREATED = "ELEMENT_CREATED";
    private static final String ELEMENT_UPDATED = "ELEMENT_UPDATED";
    private static final String ELEMENT_DELETED = "ELEMENT_DELETED";
    private static final String ELEMENTS_CLEARED = "ELEMENTS_CLEARED";

    private final ElementRepository elementRepository;
    private final BoardAccessPolicy boardAccessPolicy;
    private final BoardRepository boardRepository;
    private final ElementMapper elementMapper;
    private final BoardEventPublisher eventPublisher;

    public ElementService(
            ElementRepository elementRepository,
            BoardAccessPolicy boardAccessPolicy,
            BoardRepository boardRepository,
            ElementMapper elementMapper,
            BoardEventPublisher eventPublisher
    ) {
        this.elementRepository = elementRepository;
        this.boardAccessPolicy = boardAccessPolicy;
        this.boardRepository = boardRepository;
        this.elementMapper = elementMapper;
        this.eventPublisher = eventPublisher;
    }

    public List<Element> getBoardElements(
            UUID boardId,
            Long userId
    ) {
        if (!boardAccessPolicy.canView(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User does not have permission to view elements of this board."
            );
        }

        return elementRepository.findByBoardId(boardId);
    }

    public Element createShapeElement(
            UUID boardId,
            Long userId,
            CreateShapeDto request
    ) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User does not have permission to create elements on this board."
            );
        }

        Map<String, Object> data = new HashMap<>();

        data.put("type", toStringType(request.type()));
        data.put("text", request.text());
        data.put("startPoint", request.startPoint());
        data.put("endPoint", request.endPoint());
        data.put("color", request.color());
        data.put("width", request.width());

        Element element = new Element();

        Board board =
                boardRepository.getReferenceById(boardId);

        element.setBoard(board);
        element.setType(request.type());
        element.setData(data);

        Element saved =
                elementRepository.save(element);

        broadcast(
                boardId,
                ELEMENT_CREATED,
                new ElementChangeEvent(
                        userId,
                        elementMapper.toDto(saved),
                        null
                )
        );

        return saved;
    }

    @Transactional
    public Element updateShapeElement(
            UUID boardId,
            Long userId,
            UUID elementId,
            UpdateShapeDto request
    ) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User does not have permission to update elements on this board."
            );
        }

        Element element =
                getElement(elementId);

        if (!element.getBoard().getId().equals(boardId)) {
            throw new ElementAccessDeniedException(
                    "Element does not belong to the specified board."
            );
        }

        Map<String, Object> data = new HashMap<>();

        data.put("type", toStringType(request.type()));
        data.put("text", request.text());
        data.put("startPoint", request.startPoint());
        data.put("endPoint", request.endPoint());
        data.put("color", request.color());
        data.put("width", request.width());

        element.setData(data);

        broadcast(
                boardId,
                ELEMENT_UPDATED,
                new ElementChangeEvent(
                        userId,
                        elementMapper.toDto(element),
                        null
                )
        );

        return element;
    }

    @Transactional
    public void deleteElement(
            UUID boardId,
            Long userId,
            UUID elementId
    ) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User does not have permission to delete elements on this board."
            );
        }

        Element element =
                getElement(elementId);

        if (!element.getBoard().getId().equals(boardId)) {
            throw new ElementAccessDeniedException(
                    "Element does not belong to the specified board."
            );
        }

        elementRepository.delete(element);

        broadcast(
                boardId,
                ELEMENT_DELETED,
                new ElementChangeEvent(
                        userId,
                        null,
                        List.of(elementId)
                )
        );
    }

    @Transactional
    public void deleteAllElements(
            UUID boardId,
            Long userId
    ) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User does not have permission to delete elements on this board."
            );
        }

        elementRepository.deleteByBoardId(boardId);

        broadcast(
                boardId,
                ELEMENTS_CLEARED,
                new ElementChangeEvent(
                        userId,
                        null,
                        null
                )
        );
    }

    @Transactional
    public void deleteByIds(
            UUID boardId,
            Long userId,
            List<UUID> elementIds
    ) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User does not have permission to delete elements on this board."
            );
        }

        List<UUID> owned =
                elementRepository.findAllById(elementIds)
                        .stream()
                        .filter(element ->
                                element.getBoard()
                                        .getId()
                                        .equals(boardId)
                        )
                        .map(Element::getId)
                        .toList();

        if (owned.isEmpty()) {
            return;
        }

        elementRepository.deleteAllById(owned);

        broadcast(
                boardId,
                ELEMENT_DELETED,
                new ElementChangeEvent(
                        userId,
                        null,
                        owned
                )
        );
    }

    public Element createStrokeElement(
            UUID boardId,
            Long userId,
            CreateStrokeDto request
    ) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User does not have permission to create elements on this board."
            );
        }

        Map<String, Object> data = new HashMap<>();

        data.put("points", request.points());
        data.put("color", request.color());
        data.put("width", request.width());

        Element element = new Element();

        Board board =
                boardRepository.getReferenceById(boardId);

        element.setBoard(board);
        element.setData(data);
        element.setType(ElementType.STROKE);

        Element saved =
                elementRepository.save(element);

        broadcast(
                boardId,
                ELEMENT_CREATED,
                new ElementChangeEvent(
                        userId,
                        elementMapper.toDto(saved),
                        null
                )
        );

        return saved;
    }

    @Transactional
    public Element updateStrokeElement(
            UUID boardId,
            Long userId,
            UUID elementId,
            UpdateStrokeDto request
    ) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException(
                    "User does not have permission to update elements on this board."
            );
        }

        Map<String, Object> data = new HashMap<>();

        data.put("points", request.points());
        data.put("color", request.color());
        data.put("width", request.width());

        Element element =
                getElement(elementId);

        if (!element.getBoard().getId().equals(boardId)) {
            throw new ElementAccessDeniedException(
                    "Element does not belong to the specified board."
            );
        }

        element.setData(data);

        broadcast(
                boardId,
                ELEMENT_UPDATED,
                new ElementChangeEvent(
                        userId,
                        elementMapper.toDto(element),
                        null
                )
        );

        return element;
    }

    private void broadcast(
            UUID boardId,
            String type,
            ElementChangeEvent payload
    ) {
        Runnable send = () -> {
            try {
                eventPublisher.publish(
                        boardId,
                        type,
                        payload
                );
            } catch (RuntimeException e) {
                /*
                 * Database write already succeeded.
                 * Redis failure must not make the write fail.
                 */
                log.error(
                        "Failed to publish {} for board {}",
                        type,
                        boardId,
                        e
                );
            }
        };

        if (TransactionSynchronizationManager
                .isSynchronizationActive()) {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {

                                @Override
                                public void afterCommit() {
                                    send.run();
                                }
                            }
                    );

        } else {
            send.run();
        }
    }

    private Element getElement(UUID elementId) {
        return elementRepository
                .findById(elementId)
                .orElseThrow(() ->
                        new ElementNotFoundException(
                                "Element not found with id: " + elementId
                        )
                );
    }

    private String toStringType(ElementType type) {
        return switch (type) {

            case RECTANGLE -> "rectangle";
            case ELLIPSE -> "ellipse";
            case LINE -> "line";
            case ARROW -> "arrow";
            case TEXT -> "text";

            default ->
                    throw new ElementNotFoundException(
                            "Unknown element type: " + type
                    );
        };
    }
}
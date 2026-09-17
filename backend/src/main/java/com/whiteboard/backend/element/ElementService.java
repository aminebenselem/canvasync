package com.whiteboard.backend.element;


import com.whiteboard.backend.board.Board;
import com.whiteboard.backend.board.BoardAccessPolicy;
import com.whiteboard.backend.board.BoardRepository;
import com.whiteboard.backend.board.exception.BoardAccessDeniedException;
import com.whiteboard.backend.element.dto.CreateShapeDto;
import com.whiteboard.backend.element.dto.CreateStrokeDto;
import com.whiteboard.backend.element.dto.UpdateShapeDto;
import com.whiteboard.backend.element.dto.UpdateStrokeDto;
import com.whiteboard.backend.element.exception.ElementAccessDeniedException;
import com.whiteboard.backend.element.exception.ElementNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ElementService {
    private final ElementRepository elementRepository;
    private final BoardAccessPolicy boardAccessPolicy;
    private final BoardRepository boardRepository;
    public ElementService(ElementRepository elementRepository, BoardAccessPolicy boardAccessPolicy, BoardRepository boardRepository) {
        this.elementRepository = elementRepository;
        this.boardAccessPolicy = boardAccessPolicy;
        this.boardRepository = boardRepository;
    }
    public List<Element> getBoardElements(UUID boardId ,Long userId) {
         if (!boardAccessPolicy.canView(boardId, userId)) {
            throw new BoardAccessDeniedException("User does not have permission to view elements of this board.");
         }
        return elementRepository.findByBoardId(boardId);
    }

    public Element createShapeElement(UUID boardId, Long userId, CreateShapeDto request) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException("User does not have permission to create elements on this board.");
        }
        Map<String, Object> data = new HashMap<>();

        data.put("text", request.text());
        data.put("startPoint", request.startPoint());
        data.put("endPoint", request.endPoint());
        data.put("color", request.color());
        data.put("width", request.width());

        Element element = new Element();

        Board board = boardRepository.getReferenceById(boardId);

        element.setBoard(board);
        element.setType(request.type());
        element.setData(data);

        return elementRepository.save(element);
    }

    @Transactional
    public Element updateShapeElement(UUID boardId, Long userId,UUID elementId, UpdateShapeDto request) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException("User does not have permission to update elements on this board.");
        }
        Map<String, Object> data = new HashMap<>();

        data.put("text", request.text());
        data.put("startPoint", request.startPoint());
        data.put("endPoint", request.endPoint());
        data.put("color", request.color());
        data.put("width", request.width());

        Element element = this.getElement(elementId);
        if (!element.getBoard().getId().equals(boardId)) {
            throw new ElementAccessDeniedException("Element does not belong to the specified board.");
        }
        element.setData(data);
        return element;
    }
    @Transactional
    public void deleteElement(UUID boardId, Long userId, UUID elementId) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException("User does not have permission to delete elements on this board.");
        }
        Element element = getElement(elementId);
        if (!element.getBoard().getId().equals(boardId)) {
            throw new ElementAccessDeniedException("Element does not belong to the specified board.");
        }
        elementRepository.delete(element);
    }

    public void deleteAllElements(UUID boardId, Long userId) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException("User does not have permission to delete elements on this board.");
        }

        elementRepository.deleteByBoardId(boardId);

    }
 public Element createStrokeElement(UUID boardId, Long userId, CreateStrokeDto request) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException("User does not have permission to create elements on this board.");
        }
        Map<String, Object> data = new HashMap<>();

        data.put("points", request.points());
        data.put("color", request.color());
        data.put("width", request.width());

        Element element = new Element();

        Board board = boardRepository.getReferenceById(boardId);

        element.setBoard(board);
        element.setData(data);

        return elementRepository.save(element);
    }

    @Transactional
    public Element updateStrokeElement(UUID boardId, Long userId,UUID elementId, UpdateStrokeDto request) {
        if (!boardAccessPolicy.canEdit(boardId, userId)) {
            throw new BoardAccessDeniedException("User does not have permission to update elements on this board.");
        }
        Map<String, Object> data = new HashMap<>();

        data.put("points", request.points());
        data.put("color", request.color());
        data.put("width", request.width());

        Element element = this.getElement(elementId);
        if (!element.getBoard().getId().equals(boardId)) {
            throw new ElementAccessDeniedException("Element does not belong to the specified board.");
        }
        element.setData(data);
        return element;
    }



private Element getElement(UUID elementId) {
    return elementRepository.findById(elementId)
            .orElseThrow(() -> new ElementNotFoundException("Element not found with id: " + elementId));
}


}

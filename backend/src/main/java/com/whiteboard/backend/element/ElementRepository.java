package com.whiteboard.backend.element;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ElementRepository extends JpaRepository<Element, UUID> {
 List<Element> findByBoardId(UUID boardId);
void deleteByBoardId(UUID boardId);

}

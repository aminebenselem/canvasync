package com.whiteboard.backend.element.mapper;

import com.whiteboard.backend.element.Element;
import com.whiteboard.backend.element.dto.ElementDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ElementMapper {

    public ElementDto toDto(Element element) {
        return new ElementDto(
                element.getId(),
                element.getType().name(),
                element.getData()
        );
    }

    public List<ElementDto> toDtoList(List<Element> elements) {
        return elements.stream()
                .map(this::toDto)
                .toList();
    }
}

package ru.practicum.categories.controller;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.categories.dto.CategoryDto;
import ru.practicum.categories.service.CategoryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @PositiveOrZero Integer size) {
        log.info("GET /categories: from={}, size={}", from, size);
        return ResponseEntity.ok(categoryService.getCategories(from, size));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<CategoryDto>> getCategoriesByIds(@RequestParam("ids") List<Long> ids) {
        log.info("GET /categories/ids: ids={}", ids);
        return ResponseEntity.ok(categoryService.getCategoriesByIds(ids));
    }

    @GetMapping("/{catId}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable Long catId) {
        log.info("GET /categories/{}", catId);
        return ResponseEntity.ok(categoryService.getCategory(catId));
    }
}
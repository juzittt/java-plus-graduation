package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.events.dto.CategoryDto;

import java.util.List;

@FeignClient(name = "category-service")
public interface CategoryClient {
    @GetMapping("/categories/{catId}")
    CategoryDto getCategory(@PathVariable("catId") Long catId);

    @GetMapping("/categories/ids")
    List<CategoryDto> getCategoriesByIds(@RequestParam("ids") List<Long> ids);
}
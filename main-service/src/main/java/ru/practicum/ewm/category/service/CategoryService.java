package ru.practicum.ewm.category.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;

import java.util.Collection;

public interface CategoryService {

    CategoryDto create(NewCategoryDto newCategoryDto);

    void delete(long categoryId);

    CategoryDto update(NewCategoryDto categoryDto, long categoryId);

    Collection<CategoryDto> getAll(Pageable page);

    CategoryDto getCategory(long categoryId);
}

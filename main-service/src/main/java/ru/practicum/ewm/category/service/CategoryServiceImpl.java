package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Transactional
    @Override
    public CategoryDto create(NewCategoryDto newCategoryDto) {
        return CategoryMapper.toCategoryDto(categoryRepository.save(CategoryMapper.toCategory(newCategoryDto)));
    }

    @Transactional
    @Override
    public void delete(long catId) {
        Category category = getById(catId);
        if (eventRepository.existsByCategory(category)) {
            throw new ConflictException("Нельзя удалить категорию. Существуют события, связанные с этой категорией.");
        }
        categoryRepository.deleteById(catId);
    }

    @Transactional
    @Override
    public CategoryDto update(NewCategoryDto categoryDto, long catId) {
        checkCategoryExists(catId);
        Category category = categoryRepository.save(categoryToUpdate(categoryDto, catId));
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    public Collection<CategoryDto> getAll(Pageable page) {
        return categoryRepository.findAll(page).stream()
                .map(CategoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(long catId) {
        return CategoryMapper.toCategoryDto(getById(catId));
    }

    private Category getById(long catId) {
        return categoryRepository.findById(catId).orElseThrow(() ->
                new NotFoundException(String.format("Категория с id=%d не найдена.", catId)));
    }

    private void checkCategoryExists(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException(String.format("Категория с id=%d не найдена", id));
        }
    }

    private Category categoryToUpdate(NewCategoryDto categoryDto, Long id) {
        Category category = categoryRepository.getReferenceById(id);
        category.setName(categoryDto.getName());
        return category;
    }
}

package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentUserDto;
import ru.practicum.ewm.comment.service.CommentService;

import javax.validation.Valid;

@RestController
@RequestMapping("/users/{userId}/comments")
@Validated
@RequiredArgsConstructor
@Slf4j
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping("/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@RequestBody @Valid NewCommentDto newCommentDto,
                             @PathVariable(value = "userId") Long userId,
                             @PathVariable(value = "eventId") Long eventId) {
        log.info("POST-Добавление нового комментария.");
        return commentService.createComment(newCommentDto, userId, eventId);
    }

    @PatchMapping("/{commentId}")
    public CommentDto update(@RequestBody @Valid UpdateCommentUserDto updateCommentUserDto,
                             @PathVariable(value = "userId") Long userId,
                             @PathVariable(value = "commentId") Long commentId) {
        log.info("PATCH-Изменение комментария/статуса комментария автором.");
        return commentService.updateCommentByUser(updateCommentUserDto, userId, commentId);
    }

    @GetMapping("/{commentId}")
    public CommentDto getById(@PathVariable(value = "userId") Long userId,
                              @PathVariable(value = "commentId") Long commentId) {
        log.info("GET-Получение комментария автором.");
        return commentService.getCommentsByIdByUser(userId, commentId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable(value = "userId") Long userId,
                           @PathVariable(value = "commentId") Long commentId) {
        log.info("DELETE-Удаление комментария автором.");
        commentService.deleteCommentByUser(userId, commentId);
    }


}

package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentAdminDto;
import ru.practicum.ewm.comment.service.CommentService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCommentController {
    private final CommentService commentService;

    @PatchMapping("/{commentId}")
    public CommentDto update(@RequestBody @Valid UpdateCommentAdminDto commentDto,
                                    @PathVariable(value = "commentId") Long commentId) {
        log.info("PATCH-Редактирование комментария и его статуса (отклонение/публикация)");
        return commentService.updateCommentByAdmin(commentId, commentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(value = "commentId") Long commentId) {
        log.info("DELETE-Удаление комментария администратором.");
        commentService.deleteCommentByAdmin(commentId);
    }

    @GetMapping("/{eventId}")
    public List<CommentDto> getAll(@PathVariable Long eventId,
                                   @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                   @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET-Получение всех комментариев к мероприятию. (Мероприятия со всеми статусами)");
        PageRequest page = PageRequest.of(from, size);
        return commentService.getAllByAdmin(eventId, page);
    }
}

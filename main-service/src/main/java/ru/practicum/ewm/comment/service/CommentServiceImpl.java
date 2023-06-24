package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentAdminDto;
import ru.practicum.ewm.comment.dto.UpdateCommentUserDto;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.model.CommentStateAction;
import ru.practicum.ewm.comment.model.CommentStatus;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentDto createComment(NewCommentDto newCommentDto, Long userId, Long eventId) {
        User author = getUserById(userId);
        Event event = getEventById(eventId);
        Comment comment = CommentMapper.toComment(newCommentDto, author, event);
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateCommentByUser(UpdateCommentUserDto updateCommentUserDto, Long userId, Long commentId) {
        User user = getUserById(userId);
        Comment comment = getCommentById(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Редактирование комментария доступно только автору комментария.");
        }
        if (updateCommentUserDto.getText() != null) {
            comment.setText(updateCommentUserDto.getText());
        }
        changeStatusComment(comment, updateCommentUserDto.getState());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    public CommentDto getCommentsByIdByUser(Long userId, Long commentId) {
        getUserById(userId);
        Comment comment = getCommentById(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Полная информация о комментарии доступна только автору.");
        }
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    public void deleteCommentByUser(Long userId, Long commentId) {
        getUserById(userId);
        Comment comment = getCommentById(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Удалить комментарий может только автор.");
        }
        commentRepository.delete(comment);
    }

    @Override
    public CommentDto updateCommentByAdmin(Long commentId, UpdateCommentAdminDto updateCommentAdminDto) {
        Comment comment = getCommentById(commentId);
        if (updateCommentAdminDto.getText() != null) {
            comment.setText(updateCommentAdminDto.getText());
        }
        changeStatusComment(comment, updateCommentAdminDto.getState());
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    public void deleteCommentByAdmin(Long commentId) {
        getCommentById(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentDto> getAll(Long eventId, Pageable pageable) {
        getEventById(eventId);
        List<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        return comments.stream()
                .map(CommentMapper::toCommentDto)
                .filter(c -> c.getStatus().equals(CommentStatus.PUBLISHED))
                .sorted(Comparator.comparing(CommentDto::getCreated))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getAllByAdmin(Long eventId, PageRequest pageable) {
        getEventById(eventId);
        List<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        return comments.stream()
                .map(CommentMapper::toCommentDto)
                .sorted(Comparator.comparing(CommentDto::getCreated))
                .collect(Collectors.toList());
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с id=%d не найден.", commentId)));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id=%d не найден.", userId)));
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Мероприятие с id=%d не найдено.", eventId)));
    }

    private void changeStatusComment(Comment comment, CommentStateAction state) {
        switch (state) {
            case CANCEL_REVIEW:
                comment.setStatus(CommentStatus.CANCELED);
                break;
            case SEND_TO_REVIEW:
                comment.setStatus(CommentStatus.PENDING);
                break;
            case PUBLISH_COMMENT:
                comment.setStatus(CommentStatus.PUBLISHED);
                break;
            case REJECT_COMMENT:
                comment.setStatus(CommentStatus.CANCELED);
                break;
            default:
                throw new BadRequestException("Неизвестное состояние.");
        }
    }
}


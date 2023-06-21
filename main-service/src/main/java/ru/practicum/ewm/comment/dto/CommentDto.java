package ru.practicum.ewm.comment.dto;

import lombok.*;
import ru.practicum.ewm.comment.model.CommentStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;
    private String text;
    private String authorName;
    private LocalDateTime created;
    private CommentStatus status;
}

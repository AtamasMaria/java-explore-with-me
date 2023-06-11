package ru.practicum.ewm.user.dto;

import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    @Positive
    private Long id;
    @NotBlank
    @Size(min = 2, max = 250)
    private String name;
    @Email
    @NotBlank
    @Size(min = 6, max = 255)
    private String email;
}

package ru.practicum.ewm.user.mapper;

import lombok.NonNull;
import ru.practicum.ewm.user.dto.*;
import ru.practicum.ewm.user.model.User;

public class UserMapper {

    public static UserDto toUserDto(@NonNull User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static User toUser(@NonNull NewUserDto userDto) {
        return User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .build();
    }

    public static UserShortDto toUserShortDto(@NonNull User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}

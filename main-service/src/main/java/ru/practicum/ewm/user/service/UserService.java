package ru.practicum.ewm.user.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.user.dto.UserDto;

import java.util.Collection;

public interface UserService {
    UserDto create(UserDto userDto);

    void delete(long userId);

    Collection<UserDto> getAll(Collection<Long> ids, Pageable page);
}

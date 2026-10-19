package ru.practicum.users.mapper;

import org.mapstruct.Mapper;
import ru.practicum.users.dto.NewUserRequest;
import ru.practicum.users.dto.UserDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.users.model.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toUserDto(User user);

    UserShortDto toUserShortDto(User user);

    User toUser(NewUserRequest newUserRequest);

    List<UserDto> toUserDtoList(List<User> users);
}
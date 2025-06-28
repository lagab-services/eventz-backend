package com.lagab.eventz.app.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.lagab.eventz.app.auth.dto.RegisterRequest;
import com.lagab.eventz.app.auth.dto.UserResponse;
import com.lagab.eventz.app.user.entity.Role;
import com.lagab.eventz.app.user.entity.User;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true) // Sera hashé dans le service
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isEmailVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tokens", ignore = true)
   /* @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "enabled", ignore = true)*/
    User toEntity(RegisterRequest request);

    @Mapping(source = "role", target = "role")
    UserResponse toResponse(User user);

    default String mapRole(Role role) {
        return role != null ? role.name() : null;
    }
}

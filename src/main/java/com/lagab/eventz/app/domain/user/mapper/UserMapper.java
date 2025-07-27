package com.lagab.eventz.app.domain.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import com.lagab.eventz.app.domain.auth.dto.RegisterRequest;
import com.lagab.eventz.app.domain.auth.dto.UserResponse;
import com.lagab.eventz.app.domain.user.model.Role;
import com.lagab.eventz.app.domain.user.model.User;

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

    @Named("mapUserFromId")
    default User fromId(Long id) {
        if (id == null)
            return null;
        User user = new User();
        user.setId(id);
        return user;
    }
}

package com.lagab.eventz.app.domain.org.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.lagab.eventz.app.domain.org.dto.InvitationCreateDto;
import com.lagab.eventz.app.domain.org.dto.InvitationResponseDto;
import com.lagab.eventz.app.domain.org.model.Invitation;
import com.lagab.eventz.app.domain.user.mapper.UserMapper;

@Mapper(componentModel = "spring", uses = { UserMapper.class, OrganizationMapper.class })
public interface InvitationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "organizationId", target = "organization", qualifiedByName = "fromId")
    Invitation createDtoToEntity(InvitationCreateDto dto);

    @Mapping(source = "invitedBy", target = "invitedBy")
    @Mapping(source = "organization", target = "organization")
    InvitationResponseDto entityToResponseDto(Invitation entity);

    List<InvitationResponseDto> entitiesToResponseDtos(List<Invitation> entities);

}

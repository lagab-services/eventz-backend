package com.lagab.eventz.app.domain.org.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.lagab.eventz.app.domain.org.dto.OrganizationMembershipDto;
import com.lagab.eventz.app.domain.org.dto.invitation.MembershipInviteDto;
import com.lagab.eventz.app.domain.org.model.OrganizationMembership;
import com.lagab.eventz.app.domain.user.mapper.UserMapper;

@Mapper(componentModel = "spring", uses = { UserMapper.class, OrganizationMapper.class })
public interface OrganizationMembershipMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "userId", target = "user", qualifiedByName = "mapUserFromId")
    @Mapping(source = "organizationId", target = "organization", qualifiedByName = "fromId")
    OrganizationMembership inviteDtoToEntity(MembershipInviteDto dto);

    OrganizationMembershipDto toDto(OrganizationMembership entity);

    @Mapping(target = "id", ignore = true)
    OrganizationMembership toEntity(OrganizationMembershipDto dto);

    List<OrganizationMembershipDto> entitiesToDtos(List<OrganizationMembership> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromDto(OrganizationMembershipDto dto, @MappingTarget OrganizationMembership entity);
}

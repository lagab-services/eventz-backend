package com.lagab.eventz.app.domain.org.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.lagab.eventz.app.domain.org.dto.OrganizationDto;
import com.lagab.eventz.app.domain.org.model.Organization;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    Organization toEntity(OrganizationDto dto);

    OrganizationDto toDto(Organization entity);

    List<OrganizationDto> toDtos(List<Organization> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(OrganizationDto dto, @MappingTarget Organization entity);

    @Named("fromId")
    default Organization fromId(String id) {
        if (id == null) {
            return null;
        }
        Organization org = new Organization();
        org.setId(id);
        return org;
    }
}

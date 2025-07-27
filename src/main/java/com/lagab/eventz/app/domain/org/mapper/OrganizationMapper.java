package com.lagab.eventz.app.domain.org.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lagab.eventz.app.domain.org.dto.OrganizationDto;
import com.lagab.eventz.app.domain.org.model.Organization;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "metadataJson", source = "metadata", qualifiedByName = "mapToJson")
    Organization toEntity(OrganizationDto dto);

    @Mapping(target = "metadata", source = "metadataJson", qualifiedByName = "jsonToMap")
    OrganizationDto toDto(Organization entity);

    List<OrganizationDto> toDtos(List<Organization> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "metadataJson", source = "metadata", qualifiedByName = "mapToJson")
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

    @Named("jsonToMap")
    default Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isEmpty())
            return new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (json.startsWith("\"{") && json.endsWith("}\"")) {
                json = mapper.readValue(json, String.class);
            }
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @Named("mapToJson")
    default String mapToJson(Map<String, Object> map) {
        if (map == null)
            return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
}

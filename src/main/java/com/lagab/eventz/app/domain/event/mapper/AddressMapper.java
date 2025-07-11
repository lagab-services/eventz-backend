package com.lagab.eventz.app.domain.event.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.lagab.eventz.app.domain.event.dto.AddressDTO;
import com.lagab.eventz.app.domain.event.dto.CreateAddressDTO;
import com.lagab.eventz.app.domain.event.dto.UpdateAddressDTO;
import com.lagab.eventz.app.domain.event.model.Address;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressDTO toDto(Address address);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    Address toEntity(CreateAddressDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateAddressDTO dto, @MappingTarget Address address);
}

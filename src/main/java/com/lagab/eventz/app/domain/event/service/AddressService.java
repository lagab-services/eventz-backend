package com.lagab.eventz.app.domain.event.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.domain.event.dto.CreateAddressDTO;
import com.lagab.eventz.app.domain.event.dto.UpdateAddressDTO;
import com.lagab.eventz.app.domain.event.mapper.AddressMapper;
import com.lagab.eventz.app.domain.event.model.Address;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.repository.AddressRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    public Address createAddress(CreateAddressDTO createAddressDTO, Event event) {
        log.debug("Creating address for event: {}", event.getId());

        Address address = addressMapper.toEntity(createAddressDTO);
        address.setEvent(event);

        Address savedAddress = addressRepository.save(address);
        log.debug("Address created successfully with ID: {}", savedAddress.getId());

        return savedAddress;
    }

    public void updateAddress(Address address, UpdateAddressDTO updateAddressDTO) {
        log.debug("Updating address with ID: {}", address.getId());

        addressMapper.updateEntity(updateAddressDTO, address);
        addressRepository.save(address);

        log.debug("Address updated successfully with ID: {}", address.getId());
    }
}

package com.lagab.eventz.app.domain.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.domain.event.model.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

}

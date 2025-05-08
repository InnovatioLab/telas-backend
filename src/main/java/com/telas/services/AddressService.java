package com.telas.services;

import com.telas.entities.Address;

public interface AddressesService {
    Address findByZipCode(String zipCode);
}

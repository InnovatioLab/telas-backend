package com.telas.dtos.response;

import com.telas.entities.Address;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class AvailablePartnerAddressResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 4764243144016762462L;

  private final UUID addressId;
  private final UUID clientId;
  private final String businessName;
  private final String email;

  private final String street;
  private final String zipCode;
  private final String city;
  private final String state;
  private final String country;
  private final String address2;

  private final String label;

  public AvailablePartnerAddressResponseDto(Address entity) {
    this.addressId = entity.getId();
    this.clientId = entity.getClient() != null ? entity.getClient().getId() : null;
    this.businessName = entity.getClient() != null ? entity.getClient().getBusinessName() : null;
    this.email =
        entity.getClient() != null
                && entity.getClient().getContact() != null
                && entity.getClient().getContact().getEmail() != null
            ? entity.getClient().getContact().getEmail()
            : null;

    this.street = entity.getStreet();
    this.zipCode = entity.getZipCode();
    this.city = entity.getCity();
    this.state = entity.getState();
    this.country = entity.getCountry();
    this.address2 = entity.getAddress2();

    this.label = buildLabel(this.businessName, entity);
  }

  private static String buildLabel(String businessName, Address address) {
    String name = businessName != null && !businessName.isBlank() ? businessName.trim() : "Partner";
    String street = address.getStreet() != null ? address.getStreet().trim() : "";
    String city = address.getCity() != null ? address.getCity().trim() : "";
    String state = address.getState() != null ? address.getState().trim() : "";
    String zip = address.getZipCode() != null ? address.getZipCode().trim() : "";
    return String.format("%s — %s, %s, %s, %s", name, street, city, state, zip).trim();
  }
}


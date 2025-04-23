package com.marketingproject.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.marketingproject.dtos.request.AddressRequestDto;
import com.marketingproject.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "addresses")
@NoArgsConstructor
public class Address extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "country")
    private String country = "USA";

    @Column(name = "complement")
    private String complement;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    public Address(AddressRequestDto request, Client client) {
        this(request);
        this.client = client;
    }

    public Address(AddressRequestDto request) {
        street = request.getStreet();
        number = request.getNumber();
        zipCode = request.getZipCode();
        city = request.getCity();
        state = request.getState();
        country = request.getCountry();
        complement = request.getComplement();
    }

    public void update(AddressRequestDto request) {
        street = request.getStreet();
        number = request.getNumber();
        zipCode = request.getZipCode();
        city = request.getCity();
        state = request.getState();
        country = request.getCountry();
        complement = request.getComplement();
        client = null;
    }

    public boolean hasChanged(AddressRequestDto address) {
        return !street.equals(address.getStreet())
               || !number.equals(address.getNumber())
               || !zipCode.equals(address.getZipCode())
               || !city.equals(address.getCity())
               || !state.equals(address.getState())
               || (address.getCountry() != null && !country.equals(address.getCountry()))
               || (address.getComplement() != null && !complement.equals(address.getComplement()));
    }
}

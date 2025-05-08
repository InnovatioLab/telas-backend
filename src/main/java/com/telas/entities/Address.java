package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.dtos.request.AddressRequestDto;
import com.telas.enums.Role;
import com.telas.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "addresses")
@AuditTable("addresses_aud")
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
    private String country = "US";

    @Column(name = "complement")
    private String complement;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @JsonIgnore
    @OneToMany(mappedBy = "address")
    private List<Monitor> monitors = new ArrayList<>();

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
        latitude = request.getLatitude();
        longitude = request.getLongitude();
    }

    public boolean hasChanged(AddressRequestDto address) {
        return !street.equals(address.getStreet())
               || !number.equals(address.getNumber())
               || !zipCode.equals(address.getZipCode())
               || !city.equals(address.getCity())
               || !state.equals(address.getState())
               || (address.getCountry() != null && !country.equals(address.getCountry()))
               || (address.getComplement() != null && !complement.equals(address.getComplement()))
               || (address.getLatitude() != null && !latitude.equals(address.getLatitude()))
               || (address.getLongitude() != null && !longitude.equals(address.getLongitude()));
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public boolean isPartnerAddress() {
        return client != null && Role.PARTNER.equals(client.getRole());
    }

    public void setLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getCoordinatesParams() {
        return String.join(", ", street, number, city, state, zipCode);
    }
}

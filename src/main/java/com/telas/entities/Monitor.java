package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.enums.MonitorType;
import com.telas.shared.audit.BaseAudit;
import com.telas.shared.utils.ValidateDataUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;

@Getter
@Setter
@Entity
@Table(name = "monitors")
@AuditTable("monitors_aud")
@NoArgsConstructor
public class Monitor extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "fl_active", nullable = false)
    private boolean active = true;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MonitorType type = MonitorType.BASIC;

    @Column(name = "location_description")
    private String locationDescription;

    @Column(name = "size_in_inches", precision = 5, scale = 2, nullable = false)
    private BigDecimal size = BigDecimal.valueOf(0.00);

    @Column(name = "max_blocks", nullable = false)
    private Integer maxBlocks;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    @OneToOne
    @JoinColumn(name = "partner_id", referencedColumnName = "id")
    private Client partner;

    @JsonIgnore
    @OneToMany(mappedBy = "id.monitor")
    private Set<MonitorAdvertisingAttachment> monitorAdvertisingAttachments = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "clients_monitors",
            joinColumns = @JoinColumn(name = "monitor_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    private Set<Client> clients = new HashSet<Client>();

    public Monitor(MonitorRequestDto request, Client partner) {
        this(request, partner, Set.of(), List.of());
    }

    public Monitor(MonitorRequestDto request, Client partner, Set<Client> clients, List<AdvertisingAttachment> advertisingAttachmentsList) {
        type = request.getType();
        size = request.getSize();
        locationDescription = request.getLocationDescription();
        maxBlocks = request.getMaxBlocks();
        latitude = request.getLatitude();
        longitude = request.getLongitude();
        address = new Address(request.getAddress());
        this.partner = partner;

        if (!ValidateDataUtils.isNullOrEmpty(request.getAdvertisingAttachments())) {
            IntStream.range(0, advertisingAttachmentsList.size()).forEach(index -> {
                AdvertisingAttachment advertisingAttachment = advertisingAttachmentsList.get(index);
                MonitorAdvertisingAttachment monitorAdvertisingAttachment = new MonitorAdvertisingAttachment(request.getAdvertisingAttachments().get(index), this, advertisingAttachment);
                monitorAdvertisingAttachments.add(monitorAdvertisingAttachment);
            });
        }

        this.clients.addAll(clients);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Monitor monitor = (Monitor) o;
        return Objects.equals(getId(), monitor.getId());
    }

    public List<AdvertisingAttachment> getAdvertisingAttachments() {
        return monitorAdvertisingAttachments.stream()
                .map(MonitorAdvertisingAttachment::getAdvertisingAttachment)
                .toList();
    }

    public String getPartnerBusinessName() {
        return partner.getBusinessName();
    }

    public String toStringMapper() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return objectMapper.writeValueAsString(this);
    }
}

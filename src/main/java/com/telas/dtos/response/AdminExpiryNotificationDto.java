package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminExpiryNotificationDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String reference;
    private Instant createdAt;
    private String label;
}

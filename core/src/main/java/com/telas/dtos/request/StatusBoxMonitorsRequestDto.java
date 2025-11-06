package com.telas.dtos.request;

import com.telas.enums.DefaultStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StatusBoxMonitorsRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private String ip;

    private UUID monitorId;

    private DefaultStatus status;
}

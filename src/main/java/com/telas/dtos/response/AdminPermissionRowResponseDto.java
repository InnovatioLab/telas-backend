package com.telas.dtos.response;

import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Value
public class AdminPermissionRowResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    UUID clientId;
    String businessName;
    String email;
    List<String> grantedPermissions;
}

package com.telas.services;

import com.telas.entities.Client;
import com.telas.enums.Permission;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PermissionService {

    boolean hasPermission(Client client, Permission permission);

    List<String> listPermissionCodesForClient(UUID clientId);

    void replacePermissionsForAdmin(UUID targetClientId, Set<Permission> permissions, UUID grantedByClientId);

    List<String> listEffectivePermissionCodesForDisplay(Client client);
}

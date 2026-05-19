package com.telas.services.impl;

import com.telas.entities.Client;
import com.telas.entities.ClientGrantedPermission;
import com.telas.enums.Permission;
import com.telas.enums.Role;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.ClientGrantedPermissionRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.PartnerPermissionCodes;
import com.telas.services.PermissionService;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final ClientGrantedPermissionRepository clientGrantedPermissionRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Client client, Permission permission) {
        if (client == null || permission == null) {
            return false;
        }
        if (Role.DEVELOPER.equals(client.getRole())) {
            return true;
        }
        boolean explicit = clientGrantedPermissionRepository.existsByClient_IdAndPermissionCode(
                client.getId(), permission.name());
        if (explicit) {
            return true;
        }
        if (Role.ADMIN.equals(client.getRole())) {
            return clientGrantedPermissionRepository.countByClient_Id(client.getId()) == 0;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listPermissionCodesForClient(UUID clientId) {
        return clientGrantedPermissionRepository.findByClient_Id(clientId).stream()
                .map(ClientGrantedPermission::getPermissionCode)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void replacePermissionsForAdmin(UUID targetClientId, Set<Permission> permissions, UUID grantedByClientId) {
        replacePermissionsForClient(targetClientId, permissions, grantedByClientId);
    }

    @Override
    @Transactional
    public void replacePermissionsForClient(UUID targetClientId, Set<Permission> permissions, UUID grantedByClientId) {
        Client target =
                clientRepository
                        .findById(targetClientId)
                        .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        if (!Role.ADMIN.equals(target.getRole()) && !Role.PARTNER.equals(target.getRole())) {
            throw new IllegalArgumentException("Permissions apply only to ADMIN or PARTNER users.");
        }
        Client grantedBy =
                clientRepository
                        .findById(grantedByClientId)
                        .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));

        Set<Permission> toSave = permissions != null ? new HashSet<>(permissions) : Set.of();
        PartnerPermissionCodes.validateGrantableForRole(target.getRole(), toSave);

        clientGrantedPermissionRepository.deleteByClient_Id(targetClientId);

        Instant now = Instant.now();
        for (Permission p : toSave) {
            ClientGrantedPermission row = new ClientGrantedPermission();
            row.setClient(target);
            row.setPermissionCode(p.name());
            row.setGrantedAt(now);
            row.setGrantedBy(grantedBy);
            clientGrantedPermissionRepository.save(row);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listPermissionCatalogForRole(Role role) {
        return PartnerPermissionCodes.catalogForRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listEffectivePermissionCodesForDisplay(Client client) {
        if (client == null) {
            return List.of();
        }
        if (Role.DEVELOPER.equals(client.getRole())) {
            return Arrays.stream(Permission.values())
                    .map(Permission::name)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
        if (Role.ADMIN.equals(client.getRole())) {
            return listPermissionCodesForClient(client.getId());
        }
        if (Role.PARTNER.equals(client.getRole())) {
            return listPermissionCodesForClient(client.getId());
        }
        return List.of();
    }
}

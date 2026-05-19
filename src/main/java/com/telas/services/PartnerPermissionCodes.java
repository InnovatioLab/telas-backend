package com.telas.services;

import com.telas.enums.Permission;
import com.telas.enums.Role;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PartnerPermissionCodes {

    public static final Set<Permission> PARTNER_GRANTABLE = EnumSet.of(Permission.PARTNER_SLOTS_ANY_LOCATION);

    private PartnerPermissionCodes() {
    }

    public static List<String> catalogForRole(Role role) {
        if (Role.PARTNER.equals(role)) {
            return PARTNER_GRANTABLE.stream().map(Enum::name).sorted().collect(Collectors.toList());
        }
        return java.util.Arrays.stream(Permission.values())
                .filter(p -> !PARTNER_GRANTABLE.contains(p))
                .map(Enum::name)
                .sorted()
                .collect(Collectors.toList());
    }

    public static void validateGrantableForRole(Role role, Set<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        if (Role.PARTNER.equals(role)) {
            for (Permission p : permissions) {
                if (!PARTNER_GRANTABLE.contains(p)) {
                    throw new IllegalArgumentException("Permission not allowed for PARTNER: " + p.name());
                }
            }
        }
    }
}

package com.telas.services;

public interface PartnerPlatformSettingsService {

    boolean isSlotsAnyLocationEnabled();

    boolean setSlotsAnyLocationEnabled(boolean enabled);

    boolean isAdminCanCreatePartnerEnabled();

    boolean setAdminCanCreatePartnerEnabled(boolean enabled);
}

package com.telas.services.impl;

import com.telas.entities.AdminEmailAlertPreference;
import com.telas.entities.Client;
import com.telas.enums.AdminEmailAlertCategory;
import com.telas.enums.Role;
import com.telas.repositories.AdminEmailAlertPreferenceRepository;
import com.telas.repositories.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEmailAlertPreferenceServiceImplTest {

    @Mock private AdminEmailAlertPreferenceRepository preferenceRepository;
    @Mock private ClientRepository clientRepository;

    @InjectMocks private AdminEmailAlertPreferenceServiceImpl service;

    @Test
    void wantsEmail_falseWhenNoRow() {
        UUID id = UUID.randomUUID();
        when(preferenceRepository.findByClient_IdAndAlertCategory(
                        eq(id), eq(AdminEmailAlertCategory.HOST_REBOOT.name())))
                .thenReturn(Optional.empty());

        assertThat(service.wantsEmail(id, AdminEmailAlertCategory.HOST_REBOOT)).isFalse();
    }

    @Test
    void wantsEmail_trueWhenEnabledRow() {
        UUID id = UUID.randomUUID();
        AdminEmailAlertPreference row = new AdminEmailAlertPreference();
        row.setEnabled(true);
        when(preferenceRepository.findByClient_IdAndAlertCategory(
                        eq(id), eq(AdminEmailAlertCategory.BOX_HEARTBEAT_CONNECTIVITY.name())))
                .thenReturn(Optional.of(row));

        assertThat(service.wantsEmail(id, AdminEmailAlertCategory.BOX_HEARTBEAT_CONNECTIVITY)).isTrue();
    }

    @Test
    void ensureDefaultEmailPreferencesForAdmin_insertsConnectivityWhenMissing() {
        UUID id = UUID.randomUUID();
        Client admin = new Client();
        admin.setId(id);
        admin.setRole(Role.ADMIN);
        when(clientRepository.findById(id)).thenReturn(Optional.of(admin));
        when(preferenceRepository.findByClient_IdAndAlertCategory(
                        eq(id), eq(AdminEmailAlertCategory.BOX_HEARTBEAT_CONNECTIVITY.name())))
                .thenReturn(Optional.empty());

        service.ensureDefaultEmailPreferencesForAdmin(id);

        verify(preferenceRepository).save(any(AdminEmailAlertPreference.class));
    }

    @Test
    void ensureDefaultEmailPreferencesForAdmin_skipsWhenRowExists() {
        UUID id = UUID.randomUUID();
        Client admin = new Client();
        admin.setId(id);
        admin.setRole(Role.ADMIN);
        when(clientRepository.findById(id)).thenReturn(Optional.of(admin));
        AdminEmailAlertPreference row = new AdminEmailAlertPreference();
        row.setEnabled(true);
        when(preferenceRepository.findByClient_IdAndAlertCategory(
                        eq(id), eq(AdminEmailAlertCategory.BOX_HEARTBEAT_CONNECTIVITY.name())))
                .thenReturn(Optional.of(row));

        service.ensureDefaultEmailPreferencesForAdmin(id);

        verify(preferenceRepository, never()).save(any(AdminEmailAlertPreference.class));
    }

    @Test
    void replacePreferences_deletesAndInsertsOnlyEnabled() {
        UUID targetId = UUID.randomUUID();
        Client admin = new Client();
        admin.setId(targetId);
        admin.setRole(Role.ADMIN);
        when(clientRepository.findById(targetId)).thenReturn(Optional.of(admin));

        EnumMap<AdminEmailAlertCategory, Boolean> map = new EnumMap<>(AdminEmailAlertCategory.class);
        map.put(AdminEmailAlertCategory.HOST_REBOOT, true);
        map.put(AdminEmailAlertCategory.SMART_PLUG_RELAY_OFF, false);

        service.replacePreferencesForAdmin(targetId, map);

        verify(preferenceRepository).deleteByClient_Id(targetId);
        verify(preferenceRepository).save(org.mockito.ArgumentMatchers.argThat(
                (AdminEmailAlertPreference p) ->
                        AdminEmailAlertCategory.HOST_REBOOT.name().equals(p.getAlertCategory())
                                && p.isEnabled()));
    }
}

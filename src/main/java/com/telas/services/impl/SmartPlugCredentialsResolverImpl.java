package com.telas.services.impl;

import com.telas.monitoring.crypto.AesTextEncryptionService;
import com.telas.monitoring.entities.SmartPlugAccountEntity;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.SmartPlugCredentials;
import com.telas.monitoring.repositories.SmartPlugAccountEntityRepository;
import com.telas.services.SmartPlugCredentialsResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmartPlugCredentialsResolverImpl implements SmartPlugCredentialsResolver {

    private final AesTextEncryptionService encryptionService;
    private final SmartPlugAccountEntityRepository smartPlugAccountEntityRepository;

    @Override
    public SmartPlugCredentials resolve(SmartPlugEntity plug) {
        String username = resolveUsername(plug);
        String password = resolvePassword(plug);
        if ((username == null || username.isBlank()) && (password == null || password.isBlank())) {
            return null;
        }
        return new SmartPlugCredentials(username, password);
    }

    private String resolveUsername(SmartPlugEntity plug) {
        SmartPlugAccountEntity a = resolveAccount(plug);
        if (a != null && a.getAccountEmail() != null && !a.getAccountEmail().isBlank()) {
            return a.getAccountEmail();
        }
        return plug.getAccountEmail();
    }

    private String resolvePassword(SmartPlugEntity plug) {
        SmartPlugAccountEntity a = resolveAccount(plug);
        if (a != null) {
            return decryptOrNull(a.getPasswordCipher());
        }
        return decryptOrNull(plug.getPasswordCipher());
    }

    private SmartPlugAccountEntity resolveAccount(SmartPlugEntity plug) {
        if (plug.getSmartPlugAccount() != null) {
            SmartPlugAccountEntity a = plug.getSmartPlugAccount();
            return a.isEnabled() ? a : null;
        }
        UUID boxId = resolveHeartbeatBoxId(plug);
        if (boxId == null) {
            return null;
        }
        return smartPlugAccountEntityRepository
                .findByBox_IdAndVendorAndEnabledTrue(boxId, plug.getVendor())
                .orElse(null);
    }

    private String decryptOrNull(String cipher) {
        if (cipher == null || cipher.isBlank()) {
            return null;
        }
        if (!encryptionService.isConfigured()) {
            return null;
        }
        return encryptionService.decrypt(cipher);
    }

    private static UUID resolveHeartbeatBoxId(SmartPlugEntity plug) {
        if (plug.getMonitor() != null && plug.getMonitor().getBox() != null) {
            return plug.getMonitor().getBox().getId();
        }
        if (plug.getBox() != null) {
            return plug.getBox().getId();
        }
        return null;
    }
}

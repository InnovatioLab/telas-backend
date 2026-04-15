package com.telas.monitoring.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AesTextEncryptionServiceTest {

    @Test
    void encryptDecrypt_roundTrip() {
        AesTextEncryptionService aes =
                new AesTextEncryptionService("0123456789abcdef0123456789abcdef");
        assertThat(aes.isConfigured()).isTrue();
        String cipher = aes.encrypt("tp-link-secret");
        assertThat(cipher).isNotBlank();
        assertThat(aes.decrypt(cipher)).isEqualTo("tp-link-secret");
    }

    @Test
    void shortKey_isDerivedViaSha256() {
        AesTextEncryptionService aes = new AesTextEncryptionService("short");
        assertThat(aes.isConfigured()).isTrue();
        String c = aes.encrypt("x");
        assertThat(aes.decrypt(c)).isEqualTo("x");
    }
}

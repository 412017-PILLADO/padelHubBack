package org.example.padelback.modules.pagos.infrastructure.crypto;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.example.padelback.infrastructure.config.CryptoProperties;
import org.springframework.stereotype.Component;

/**
 * Cifrado simétrico de los tokens OAuth de Mercado Pago en reposo (tabla tenant_mercadopago).
 * AES-256-GCM con IV aleatorio de 12 bytes; el valor guardado es base64(iv || ciphertext).
 * La key viene de {@code padel.crypto.key} (base64 de 32 bytes, env PADEL_CRYPTO_KEY).
 */
@Component
public class TokenCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public TokenCipher(CryptoProperties props) {
        if (props.key() == null || props.key().isBlank()) {
            this.key = null;
            return;
        }
        byte[] raw = Base64.getDecoder().decode(props.key());
        if (raw.length != 32) {
            throw new IllegalStateException("padel.crypto.key debe ser base64 de 32 bytes (AES-256).");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain) {
        requireKey();
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el token", e);
        }
    }

    public String decrypt(String encrypted) {
        requireKey();
        try {
            byte[] all = Base64.getDecoder().decode(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el token (¿cambió la key?)", e);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new IllegalStateException(
                    "padel.crypto.key no configurada: seteá PADEL_CRYPTO_KEY para usar Mercado Pago.");
        }
    }
}

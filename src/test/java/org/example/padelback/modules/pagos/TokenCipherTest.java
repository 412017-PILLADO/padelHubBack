package org.example.padelback.modules.pagos;

import java.util.Base64;

import org.example.padelback.infrastructure.config.CryptoProperties;
import org.example.padelback.modules.pagos.infrastructure.crypto.TokenCipher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenCipherTest {

    private static final String KEY_B64 = Base64.getEncoder().encodeToString(new byte[32]); // key de test: 32 bytes en cero

    @Test
    void cifraYDescifraElMismoValor() {
        TokenCipher cipher = new TokenCipher(new CryptoProperties(KEY_B64));
        String token = "APP_USR-1234567890-token-de-mp";
        assertEquals(token, cipher.decrypt(cipher.encrypt(token)));
    }

    @Test
    void dosCifradosDelMismoValorSonDistintos() {
        // IV aleatorio: el mismo plaintext nunca produce el mismo ciphertext.
        TokenCipher cipher = new TokenCipher(new CryptoProperties(KEY_B64));
        assertNotEquals(cipher.encrypt("x"), cipher.encrypt("x"));
    }

    @Test
    void sinKeyConfiguradaLanzaAlUsar() {
        TokenCipher cipher = new TokenCipher(new CryptoProperties(""));
        assertThrows(IllegalStateException.class, () -> cipher.encrypt("x"));
    }

    @Test
    void conOtraKeyNoDescifra() {
        TokenCipher a = new TokenCipher(new CryptoProperties(KEY_B64));
        byte[] otra = new byte[32];
        otra[0] = 1;
        TokenCipher b = new TokenCipher(new CryptoProperties(Base64.getEncoder().encodeToString(otra)));
        String cifrado = a.encrypt("secreto");
        assertThrows(IllegalStateException.class, () -> b.decrypt(cifrado));
    }
}

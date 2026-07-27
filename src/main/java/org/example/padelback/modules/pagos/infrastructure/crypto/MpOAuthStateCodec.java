package org.example.padelback.modules.pagos.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.example.padelback.infrastructure.config.CryptoProperties;
import org.example.padelback.modules.pagos.domain.exception.MpCallbackInvalidoException;
import org.springframework.stereotype.Component;

/**
 * El {@code state} del flujo OAuth de MP viaja por el navegador del dueño y vuelve al callback
 * público (sin JWT): tiene que llevar el tenant de forma NO adulterable. Formato:
 * base64url(tenantId|returnTo|epochSeconds) + "." + base64url(HMAC-SHA256 del payload).
 * TTL 10 minutos (mismo vencimiento que el code de MP).
 */
@Component
public class MpOAuthStateCodec {

    public record StateData(long tenantId, String returnTo) {}

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] key;
    private final Clock clock;

    public MpOAuthStateCodec(CryptoProperties props, Clock clock) {
        this.key = props.key() == null || props.key().isBlank()
                ? null : Base64.getDecoder().decode(props.key());
        this.clock = clock;
    }

    public String crear(long tenantId, String returnTo) {
        String payload = tenantId + "|" + returnTo + "|" + clock.instant().getEpochSecond();
        String p = B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return p + "." + B64.encodeToString(hmac(p));
    }

    public StateData validar(String state) {
        if (state == null || !state.contains(".")) {
            throw new MpCallbackInvalidoException("state ausente o malformado");
        }
        try {
            String[] partes = state.split("\\.", 2);
            if (!MessageDigest.isEqual(hmac(partes[0]), B64D.decode(partes[1]))) {
                throw new MpCallbackInvalidoException("firma del state inválida");
            }
            String[] campos = new String(B64D.decode(partes[0]), StandardCharsets.UTF_8).split("\\|", 3);
            Instant emitido = Instant.ofEpochSecond(Long.parseLong(campos[2]));
            if (clock.instant().isAfter(emitido.plus(TTL))) {
                throw new MpCallbackInvalidoException("el state venció; reintentá la conexión");
            }
            return new StateData(Long.parseLong(campos[0]), campos[1]);
        } catch (MpCallbackInvalidoException e) {
            throw e;
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new MpCallbackInvalidoException("state malformado");
        }
    }

    private byte[] hmac(String data) {
        if (key == null) {
            throw new IllegalStateException("padel.crypto.key no configurada");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}

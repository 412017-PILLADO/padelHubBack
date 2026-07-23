package org.example.padelback.modules.auth.infrastructure.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.example.padelback.infrastructure.config.JwtProperties;
import org.example.padelback.modules.auth.domain.model.UsuarioRol;
import org.example.padelback.modules.auth.domain.port.TokenIssuerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuerPort {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    @Override
    public String emitir(Long tenantId, String email, UsuarioRol rol) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.expiration(), ChronoUnit.MILLIS))
                .claim("roles", List.of(rol.name()));
        // El super-admin de plataforma no pertenece a un tenant: sin claim tenantId no se setea contexto.
        if (tenantId != null) {
            builder.claim("tenantId", tenantId);
        }
        JwtClaimsSet claims = builder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public long expiracionMs() {
        return jwtProperties.expiration();
    }
}

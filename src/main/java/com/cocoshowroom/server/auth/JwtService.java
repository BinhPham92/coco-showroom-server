package com.cocoshowroom.server.auth;

import com.cocoshowroom.server.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final AppProperties appProperties;

    public String generateToken(User user) {
        Instant now = Instant.now();
        long expiresInSeconds = (long) appProperties.getJwt().getExpirationHours() * 3600;

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("cocoshowroom")
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiresInSeconds))
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}

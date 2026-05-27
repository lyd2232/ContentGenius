package com.contentgenius.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RefreshScope
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expire-time}")
    private long tokenExpireTime;

    private static final String CLAIM_USERNAME = "username";

    public String createToken(String username) {
        Date now = new Date();
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + tokenExpireTime))
                .withClaim(CLAIM_USERNAME, username)
                .sign(algorithm);
    }

    public boolean verify(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
            return true;
        } catch (Exception ex) {
            log.debug("JWT verify failed: {}", ex.getMessage());
        }
        return false;
    }

    public String getUsername(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.require(algorithm)
                .build()
                .verify(token)
                .getClaim(CLAIM_USERNAME)
                .asString();
    }
}

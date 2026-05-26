package com.contentgenius.user.util;

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
    private String secretKey;//密钥

    @Value("${jwt.expire-time}")
    private long tokenExpireTime;//超时时间



    private static final String CLAIM_USERNAME = "username";
//创建token
    public String createToken(String username) {
        Date now = new Date();
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + tokenExpireTime))
                .withClaim(CLAIM_USERNAME, username)
                .sign(algorithm);
    }
    //解析token
    public  boolean verify(String token) {

        try {
            //secretKey是用来加密数据签名秘钥
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            JWTVerifier verifier = JWT.require(algorithm)
                    .build();
            //如果校验有问题会抛出异常。
            verifier.verify(token);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
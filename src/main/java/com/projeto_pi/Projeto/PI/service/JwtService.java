package com.projeto_pi.Projeto.PI.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET_KEY =
            "chave-secreta-do-projeto-pi-com-no-minimo-32-caracteres";

    private final SecretKey key =
            Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    public String gerarToken(String email, String papel) {

        return Jwts.builder()
                .subject(email)
                .claim("papel", papel)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }

    public String extrairEmail(String token) {

        return extrairClaims(token).getSubject();
    }

    public String extrairPapel(String token) {

        return extrairClaims(token).get("papel", String.class);
    }

    private Claims extrairClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

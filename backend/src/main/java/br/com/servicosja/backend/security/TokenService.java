package br.com.servicosja.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.servicosja.backend.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service

public class TokenService {
	private final SecretKey chave;
	private final long expiracaoMinutos;

	public TokenService(@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration-minutes}") long expiracaoMinutos) {
		this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expiracaoMinutos = expiracaoMinutos;
	}

	public String gerarToken(Usuario usuario) {
		Instant agora = Instant.now();
		return Jwts.builder().subject(usuario.getEmail()).claim("perfil", usuario.getPerfil().name())
				.issuedAt(Date.from(agora)).expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
				.signWith(chave).compact();
	}

	public boolean valido(String token) {
		try {
			lerClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public String extrairEmail(String token) {
		return lerClaims(token).getSubject();
	}

	private Claims lerClaims(String token) {
		return Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
	}
}

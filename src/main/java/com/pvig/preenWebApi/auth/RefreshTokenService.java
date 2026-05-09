package com.pvig.preenWebApi.auth;

import com.pvig.preenWebApi.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.refresh-token.expiration-days:7}")
    private long expirationDays;

    /** Crée une nouvelle paire access + refresh (nouvelle famille). */
    @Transactional
    public TokenPair buildTokenPair(User user) {
        String family = UUID.randomUUID().toString();
        String rawRefresh = createRefreshToken(user, family);
        String accessToken = jwtService.generateAccessToken(user);
        return new TokenPair(accessToken, rawRefresh);
    }

    /**
     * Rotation : révoque l'ancien token, émet un nouveau dans la même famille.
     * Si le token présenté est déjà révoqué, toute la famille est invalidée
     * (reuse detection).
     */
    @Transactional
    public TokenPair rotate(String rawToken) {
        String hash = hash(rawToken);
        RefreshTokenEntity existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (existing.isRevoked()) {
            refreshTokenRepository.revokeAllByFamily(existing.getFamily());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected — all sessions invalidated");
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        User user = existing.getUser();
        String newRaw = createRefreshToken(user, existing.getFamily());
        String accessToken = jwtService.generateAccessToken(user);
        return new TokenPair(accessToken, newRaw);
    }

    /** Révoque tous les tokens actifs de l'utilisateur propriétaire du token brut fourni. */
    @Transactional
    public void revokeByRawToken(String rawToken) {
        String hash = hash(rawToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> refreshTokenRepository.revokeAllByUser(token.getUser()));
    }

    private String createRefreshToken(User user, String family) {
        String raw = UUID.randomUUID().toString();
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setTokenHash(hash(raw));
        token.setUser(user);
        token.setFamily(family);
        token.setExpiresAt(Instant.now().plus(expirationDays, ChronoUnit.DAYS));
        token.setRevoked(false);
        refreshTokenRepository.save(token);
        return raw;
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

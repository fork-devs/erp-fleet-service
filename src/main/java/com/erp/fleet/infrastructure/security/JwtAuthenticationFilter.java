package com.erp.fleet.infrastructure.security;

import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Valida el token de acceso que reenvia el borde y establece el contexto de
 * taller.
 *
 * <p>El tenant se deriva EXCLUSIVAMENTE del claim {@code tenant_id} de un
 * token verificado. La cabecera {@code X-Tenant-ID} es una pista: si viene, se
 * contrasta con el tenant del token y la peticion se rechaza cuando no
 * coinciden.
 *
 * <p>Un token sin firma verificable, expirado o sin taller declarado se
 * rechaza: el servicio no opera sin un principal verificado.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwksKeyManager jwksKeyManager;

    public JwtAuthenticationFilter(JwksKeyManager jwksKeyManager) {
        this.jwksKeyManager = jwksKeyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String tenantHeader = request.getHeader("X-Tenant-ID");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                SignedJWT signedJWT = SignedJWT.parse(token);
                RSAPublicKey publicKey = jwksKeyManager.getPublicKey(signedJWT.getHeader().getKeyID());

                if (publicKey == null) {
                    log.warn("No hay clave publicada para el identificador de clave del token");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token no verificable");
                    return;
                }

                if (!signedJWT.verify(new RSASSAVerifier(publicKey))) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Firma del token invalida");
                    return;
                }

                JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                Date expiration = claims.getExpirationTime();
                if (expiration == null || !expiration.after(new Date())) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expirado");
                    return;
                }

                Object tenantClaim = claims.getClaim("tenant_id");
                if (tenantClaim == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "El token no declara taller");
                    return;
                }

                UUID tenantId;
                try {
                    tenantId = UUID.fromString(tenantClaim.toString());
                } catch (IllegalArgumentException e) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "El taller del token no es valido");
                    return;
                }

                if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
                    UUID solicitado;
                    try {
                        solicitado = UUID.fromString(tenantHeader.trim());
                    } catch (IllegalArgumentException e) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Tenant-ID debe ser un UUID valido");
                        return;
                    }
                    if (!solicitado.equals(tenantId)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "El taller solicitado no coincide con el del token");
                        return;
                    }
                }

                TenantContextHolder.setTenantId(tenantId);

                List<?> rolesList = claims.getClaim("roles") instanceof List<?> list ? list : List.of();
                List<SimpleGrantedAuthority> authorities = rolesList.stream()
                        .map(Object::toString)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                log.warn("Token no procesable: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalido");
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }
}

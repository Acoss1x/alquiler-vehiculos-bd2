package co.edu.unbosque.alquiler.security;

import co.edu.unbosque.alquiler.dto.PerfilResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Emite los tokens de acceso (JWT firmado con HS256). El token lleva el id (sub),
 * el tipo de cuenta (CLIENTE o EMPLEADO), el rol y si la clave es temporal.
 */
@Service
public class JwtService {

    private static final String EMISOR = "rutacar";

    private final JwtEncoder encoder;
    private final long minutosDeVida;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.expiracion-minutos:120}") long minutosDeVida) {
        this.encoder = encoder;
        this.minutosDeVida = minutosDeVida;
    }

    public String generar(PerfilResponse perfil) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(minutosDeVida, ChronoUnit.MINUTES))
                .subject(String.valueOf(perfil.id()))
                .claim("tipo", perfil.tipo())
                .claim("rol", perfil.rol())
                .claim("cambiarClave", perfil.debeCambiarClave())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    public long segundosDeVida() {
        return minutosDeVida * 60;
    }
}
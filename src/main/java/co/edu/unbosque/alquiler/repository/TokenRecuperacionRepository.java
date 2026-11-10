package co.edu.unbosque.alquiler.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Tokens de recuperación de contraseña (tabla token_recuperacion). Solo se guarda el hash
 * del token; consumirlo es atómico, así que un enlace nunca sirve dos veces.
 */
@Repository
public class TokenRecuperacionRepository {

    /** Cuenta a la que pertenece un token válido. */
    public record CuentaRef(String tipo, Long idCuenta) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public TokenRecuperacionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Crea un token nuevo e invalida los anteriores de la misma cuenta. */
    @Transactional
    public void crear(String tipo, Long idCuenta, String tokenHash, int minutosDeVida) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("tipo", tipo)
                .addValue("idCuenta", idCuenta)
                .addValue("hash", tokenHash)
                .addValue("minutos", minutosDeVida);
        jdbc.update("UPDATE token_recuperacion SET usado = TRUE "
                + "WHERE tipo = :tipo AND id_cuenta = :idCuenta AND NOT usado", p);
        jdbc.update("DELETE FROM token_recuperacion WHERE fecha_expiracion < now() - interval '1 day'", p);
        jdbc.update("INSERT INTO token_recuperacion (tipo, id_cuenta, token_hash, fecha_expiracion) "
                + "VALUES (:tipo, :idCuenta, :hash, now() + make_interval(mins => :minutos))", p);
    }

    /** Marca el token como usado y devuelve su cuenta; vacío si no existe, ya se usó o venció. */
    @Transactional
    public Optional<CuentaRef> consumir(String tokenHash) {
        List<CuentaRef> filas = jdbc.query(
                "UPDATE token_recuperacion SET usado = TRUE "
                        + "WHERE token_hash = :hash AND NOT usado AND fecha_expiracion > now() "
                        + "RETURNING tipo, id_cuenta",
                new MapSqlParameterSource("hash", tokenHash),
                (rs, n) -> new CuentaRef(rs.getString("tipo"), rs.getLong("id_cuenta")));
        return filas.stream().findFirst();
    }
}

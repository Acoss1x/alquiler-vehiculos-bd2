package co.edu.unbosque.alquiler.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Repository
public class TokenRecuperacionRepository {

    public record CuentaRef(String tipo, Long idCuenta) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public TokenRecuperacionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

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

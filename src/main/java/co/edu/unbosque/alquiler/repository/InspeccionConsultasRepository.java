package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.document.InspeccionVehiculo;
import co.edu.unbosque.alquiler.document.PiezaCambiada;
import co.edu.unbosque.alquiler.dto.GastoTecnicoResponse;
import co.edu.unbosque.alquiler.dto.PiezaFrecuenciaResponse;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Consultas avanzadas sobre la colección de inspecciones: agregaciones,
 * búsqueda con filtros dinámicos y actualización con operadores de Mongo.
 */
@Repository
public class InspeccionConsultasRepository {

    private final MongoTemplate mongoTemplate;

    public InspeccionConsultasRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** $match → $unwind → $group: suma cantidad × costo de las piezas de un vehículo. */
    public BigDecimal costoTotalMantenimiento(Long idVehiculo) {
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("idVehiculo").is(idVehiculo)),
                Aggregation.unwind("piezasCambiadas"),
                Aggregation.group("idVehiculo")
                        .sum(costoDePieza()).as("costoTotal"));

        Document resultado = mongoTemplate
                .aggregate(pipeline, InspeccionVehiculo.class, Document.class)
                .getUniqueMappedResult();

        return resultado == null ? BigDecimal.ZERO : decimal(resultado.get("costoTotal"));
    }

    /** $unwind → $group por nombre → $sort → $limit: ranking global de piezas. */
    public List<PiezaFrecuenciaResponse> piezasMasCambiadas(int limite) {
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.unwind("piezasCambiadas"),
                Aggregation.group("piezasCambiadas.nombre")
                        .sum("piezasCambiadas.cantidad").as("totalCambiadas")
                        .sum(costoDePieza()).as("costoTotal"),
                Aggregation.sort(Sort.Direction.DESC, "totalCambiadas"),
                Aggregation.limit(limite));

        return mongoTemplate.aggregate(pipeline, InspeccionVehiculo.class, Document.class)
                .getMappedResults().stream()
                .map(d -> new PiezaFrecuenciaResponse(
                        d.getString("_id"),
                        entero(d.get("totalCambiadas")),
                        decimal(d.get("costoTotal"))))
                .toList();
    }

    /** $unwind → $group por técnico → $sort: cuánto gasta en piezas cada técnico. */
    public List<GastoTecnicoResponse> gastoPorTecnico() {
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.unwind("piezasCambiadas"),
                Aggregation.group("idTecnico")
                        .sum("piezasCambiadas.cantidad").as("totalPiezas")
                        .sum(costoDePieza()).as("costoTotal"),
                Aggregation.sort(Sort.Direction.DESC, "costoTotal"));

        return mongoTemplate.aggregate(pipeline, InspeccionVehiculo.class, Document.class)
                .getMappedResults().stream()
                .map(d -> {
                    Long idTecnico = d.get("_id") instanceof Number n ? n.longValue() : null;
                    return new GastoTecnicoResponse(idTecnico, entero(d.get("totalPiezas")), decimal(d.get("costoTotal")));
                })
                .toList();
    }

    /** Búsqueda con filtros opcionales: pieza (parcial, sin mayúsculas), técnico y rango de fechas. */
    public List<InspeccionVehiculo> buscar(String pieza, Long idTecnico, LocalDate desde, LocalDate hasta) {
        Query query = new Query();
        if (pieza != null && !pieza.isBlank()) {
            query.addCriteria(Criteria.where("piezasCambiadas.nombre").regex(Pattern.quote(pieza.trim()), "i"));
        }
        if (idTecnico != null) {
            query.addCriteria(Criteria.where("idTecnico").is(idTecnico));
        }
        if (desde != null || hasta != null) {
            Criteria fecha = Criteria.where("fechaInspeccion");
            if (desde != null) {
                fecha = fecha.gte(desde.atStartOfDay());
            }
            if (hasta != null) {
                fecha = fecha.lt(hasta.plusDays(1).atStartOfDay());
            }
            query.addCriteria(fecha);
        }
        query.with(Sort.by(Sort.Direction.DESC, "fechaInspeccion"));
        return mongoTemplate.find(query, InspeccionVehiculo.class);
    }

    /** Operador $push: agrega una pieza sin reescribir el documento completo. */
    public boolean agregarPieza(String idInspeccion, PiezaCambiada pieza) {
        UpdateResult resultado = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(idInspeccion)),
                new Update().push("piezasCambiadas", pieza),
                InspeccionVehiculo.class);
        return resultado.getMatchedCount() > 0;
    }

    private static ArithmeticOperators.Multiply costoDePieza() {
        return ArithmeticOperators.Multiply.valueOf("piezasCambiadas.cantidad")
                .multiplyBy("piezasCambiadas.costo");
    }

    private static long entero(Object valor) {
        return valor instanceof Number n ? n.longValue() : 0L;
    }

    private static BigDecimal decimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor instanceof Decimal128 d ? d.bigDecimalValue() : new BigDecimal(valor.toString());
    }
}
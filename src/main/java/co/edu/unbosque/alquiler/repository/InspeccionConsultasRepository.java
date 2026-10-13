package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.document.InspeccionVehiculo;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * Consultas con pipeline de agregación sobre la colección de inspecciones.
 */
@Repository
public class InspeccionConsultasRepository {

    private final MongoTemplate mongoTemplate;

    public InspeccionConsultasRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Suma (cantidad × costo) de todas las piezas cambiadas de un vehículo.
     * Pipeline: $match → $unwind → $group.
     */
    public BigDecimal costoTotalMantenimiento(Long idVehiculo) {
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("idVehiculo").is(idVehiculo)),
                Aggregation.unwind("piezasCambiadas"),
                Aggregation.group("idVehiculo")
                        .sum(ArithmeticOperators.Multiply
                                .valueOf("piezasCambiadas.cantidad")
                                .multiplyBy("piezasCambiadas.costo"))
                        .as("costoTotal"));

        Document resultado = mongoTemplate
                .aggregate(pipeline, InspeccionVehiculo.class, Document.class)
                .getUniqueMappedResult();

        if (resultado == null || resultado.get("costoTotal") == null) {
            return BigDecimal.ZERO;
        }
        Object total = resultado.get("costoTotal");
        return total instanceof Decimal128 decimal
                ? decimal.bigDecimalValue()
                : new BigDecimal(total.toString());
    }
}


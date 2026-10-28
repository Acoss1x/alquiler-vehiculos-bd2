-- =====================================================================
-- 09_cambios_backend.sql : lo que el backend ya asume y faltaba en la BD.
-- Corre DESPUÉS de 01 a 08. Se puede repetir.
-- =====================================================================

BEGIN;

-- Cambia la contraseña (hash BCrypt) de un cliente o de un empleado.
-- p_temporal = TRUE obliga a la persona a cambiarla al iniciar sesión.
CREATE OR REPLACE PROCEDURE sp_cambiar_clave(
    p_tipo          VARCHAR,
    p_id            BIGINT,
    p_password_hash VARCHAR,
    p_temporal      BOOLEAN
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
v_filas INTEGER;
BEGIN
    IF p_password_hash IS NULL OR left(p_password_hash, 2) <> '$2' OR length(p_password_hash) < 60 THEN
        RAISE EXCEPTION 'La contraseña debe llegar como hash BCrypt';
END IF;

    IF p_tipo = 'CLIENTE' THEN
UPDATE cliente
SET password_hash = p_password_hash, debe_cambiar_clave = COALESCE(p_temporal, FALSE)
WHERE id_cliente = p_id;
ELSIF p_tipo = 'EMPLEADO' THEN
UPDATE empleado
SET password_hash = p_password_hash, debe_cambiar_clave = COALESCE(p_temporal, FALSE)
WHERE id_empleado = p_id;
ELSE
        RAISE EXCEPTION 'Tipo de cuenta no válido: % (use CLIENTE o EMPLEADO)', p_tipo;
END IF;

GET DIAGNOSTICS v_filas = ROW_COUNT;
IF v_filas = 0 THEN
        RAISE EXCEPTION 'No existe la cuenta % con id %', lower(p_tipo), p_id;
END IF;
END;
$$;

REVOKE ALL ON PROCEDURE sp_cambiar_clave(VARCHAR, BIGINT, VARCHAR, BOOLEAN) FROM PUBLIC;
GRANT EXECUTE ON PROCEDURE sp_cambiar_clave(VARCHAR, BIGINT, VARCHAR, BOOLEAN)
    TO rol_asesor, rol_tecnico, rol_administrador;


                                           -- ---------------------------------------------------------------------
-- Catálogos inactivos (sede, categoría, seguro)
-- ---------------------------------------------------------------------

-- La cotización y la reserva rechazan categoría o seguros inactivos.
CREATE OR REPLACE FUNCTION fn_costo_reserva(
    p_id_vehiculo  BIGINT,
    p_fecha_inicio DATE,
    p_fecha_fin    DATE,
    p_ids_seguros  BIGINT[]
) RETURNS NUMERIC
LANGUAGE plpgsql STABLE
AS $$
DECLARE
v_tarifa        NUMERIC;
    v_estado_cat    VARCHAR;
    v_dias          INTEGER;
    v_seguros_dia   NUMERIC := 0;
    v_encontrados   INTEGER := 0;
    v_activos       INTEGER := 0;
BEGIN
    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL THEN
        RAISE EXCEPTION 'Las fechas de inicio y fin son obligatorias';
END IF;
    IF p_fecha_fin < p_fecha_inicio THEN
        RAISE EXCEPTION 'La fecha de fin no puede ser anterior a la de inicio';
END IF;

SELECT c.tarifa_diaria, c.estado INTO v_tarifa, v_estado_cat
FROM vehiculo v JOIN categoria c ON c.id_categoria = v.categoria_id_categoria
WHERE v.id_vehiculo = p_id_vehiculo;
IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el vehículo con id %', p_id_vehiculo;
END IF;
    IF v_estado_cat <> 'ACTIVO' THEN
        RAISE EXCEPTION 'La categoría del vehículo % está inactiva', p_id_vehiculo;
END IF;

    IF p_ids_seguros IS NOT NULL AND cardinality(p_ids_seguros) > 0 THEN
SELECT COALESCE(SUM(costo_diario), 0), COUNT(*), COUNT(*) FILTER (WHERE estado = 'ACTIVO')
INTO v_seguros_dia, v_encontrados, v_activos
FROM seguro WHERE id_seguro = ANY (p_ids_seguros);
IF v_encontrados <> (SELECT COUNT(DISTINCT x) FROM unnest(p_ids_seguros) AS x) THEN
            RAISE EXCEPTION 'Alguno de los seguros indicados no existe';
END IF;
        IF v_activos <> v_encontrados THEN
            RAISE EXCEPTION 'Alguno de los seguros indicados está inactivo';
END IF;
END IF;

    v_dias := (p_fecha_fin - p_fecha_inicio) + 1;
RETURN v_dias * (v_tarifa + v_seguros_dia);
END;
$$;

-- No se ofrecen vehículos de una categoría o sede inactiva.
CREATE OR REPLACE FUNCTION fn_vehiculos_disponibles(
    p_fecha_inicio  DATE,
    p_fecha_fin     DATE,
    p_id_categoria  BIGINT,
    p_id_sede       BIGINT
) RETURNS SETOF vehiculo
LANGUAGE plpgsql STABLE
AS $$
BEGIN
    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL THEN
        RAISE EXCEPTION 'Las fechas de inicio y fin son obligatorias';
END IF;
    IF p_fecha_fin < p_fecha_inicio THEN
        RAISE EXCEPTION 'La fecha de fin no puede ser anterior a la de inicio';
END IF;

RETURN QUERY
SELECT v.*
FROM vehiculo v
WHERE v.estado NOT IN ('EN_MANTENIMIENTO', 'BAJA', 'VENDIDO')
  AND (p_id_categoria IS NULL OR v.categoria_id_categoria = p_id_categoria)
  AND (p_id_sede      IS NULL OR v.sede_id_sede           = p_id_sede)
  AND EXISTS (SELECT 1 FROM categoria c WHERE c.id_categoria = v.categoria_id_categoria AND c.estado = 'ACTIVO')
  AND EXISTS (SELECT 1 FROM sede s      WHERE s.id_sede      = v.sede_id_sede           AND s.estado = 'ACTIVO')
  AND NOT EXISTS (
    SELECT 1 FROM reserva r
    WHERE r.vehiculo_id_vehiculo = v.id_vehiculo
      AND r.estado_reserva IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO')
      AND daterange(r.fecha_inicio, r.fecha_fin, '[]')
    && daterange(p_fecha_inicio, p_fecha_fin, '[]'))
ORDER BY v.id_vehiculo;
END;
$$;

-- Reserva nueva: sedes de entrega y recibo activas, y categoría del vehículo activa.
CREATE OR REPLACE FUNCTION trg_fn_reserva_activos() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
v_estado VARCHAR;
BEGIN
SELECT estado INTO v_estado FROM sede WHERE id_sede = NEW.sede_entrega;
IF v_estado <> 'ACTIVO' THEN
        RAISE EXCEPTION 'La sede de entrega % está inactiva', NEW.sede_entrega;
END IF;
SELECT estado INTO v_estado FROM sede WHERE id_sede = NEW.sede_recibe;
IF v_estado <> 'ACTIVO' THEN
        RAISE EXCEPTION 'La sede de recibo % está inactiva', NEW.sede_recibe;
END IF;
SELECT c.estado INTO v_estado
FROM vehiculo v JOIN categoria c ON c.id_categoria = v.categoria_id_categoria
WHERE v.id_vehiculo = NEW.vehiculo_id_vehiculo;
IF v_estado <> 'ACTIVO' THEN
        RAISE EXCEPTION 'La categoría del vehículo % está inactiva', NEW.vehiculo_id_vehiculo;
END IF;
RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_reserva_activos ON reserva;
CREATE TRIGGER trg_reserva_activos
    BEFORE INSERT ON reserva
    FOR EACH ROW EXECUTE FUNCTION trg_fn_reserva_activos();

-- Seguro contratado: debe estar activo.
CREATE OR REPLACE FUNCTION trg_fn_seg_res_activo() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF (SELECT estado FROM seguro WHERE id_seguro = NEW.seguro_id_seguro) <> 'ACTIVO' THEN
        RAISE EXCEPTION 'El seguro % está inactivo', NEW.seguro_id_seguro;
END IF;
RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_seg_res_activo ON seg_res;
CREATE TRIGGER trg_seg_res_activo
    BEFORE INSERT ON seg_res
    FOR EACH ROW EXECUTE FUNCTION trg_fn_seg_res_activo();

-- Vehículo nuevo: categoría y sede activas.
CREATE OR REPLACE FUNCTION trg_fn_vehiculo_activos() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF (SELECT estado FROM categoria WHERE id_categoria = NEW.categoria_id_categoria) <> 'ACTIVO' THEN
        RAISE EXCEPTION 'La categoría % está inactiva', NEW.categoria_id_categoria;
END IF;
    IF (SELECT estado FROM sede WHERE id_sede = NEW.sede_id_sede) <> 'ACTIVO' THEN
        RAISE EXCEPTION 'La sede % está inactiva', NEW.sede_id_sede;
END IF;
RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_vehiculo_activos ON vehiculo;
CREATE TRIGGER trg_vehiculo_activos
    BEFORE INSERT ON vehiculo
    FOR EACH ROW EXECUTE FUNCTION trg_fn_vehiculo_activos();

-- Desactivar una sede: solo si no tiene vehículos, reservas activas ni empleados activos.
CREATE OR REPLACE FUNCTION trg_fn_sede_desactivar() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM vehiculo
                WHERE sede_id_sede = NEW.id_sede AND estado NOT IN ('BAJA', 'VENDIDO')) THEN
        RAISE EXCEPTION 'No se puede desactivar la sede %: tiene vehículos asignados', NEW.id_sede;
END IF;
    IF EXISTS (SELECT 1 FROM reserva
                WHERE (sede_entrega = NEW.id_sede OR sede_recibe = NEW.id_sede)
                  AND estado_reserva IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO')) THEN
        RAISE EXCEPTION 'No se puede desactivar la sede %: tiene reservas activas', NEW.id_sede;
END IF;
    IF EXISTS (SELECT 1 FROM empleado WHERE sede_id_sede = NEW.id_sede AND estado = 'ACTIVO') THEN
        RAISE EXCEPTION 'No se puede desactivar la sede %: tiene empleados activos', NEW.id_sede;
END IF;
RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sede_desactivar ON sede;
CREATE TRIGGER trg_sede_desactivar
    BEFORE UPDATE OF estado ON sede
    FOR EACH ROW
    WHEN (OLD.estado IS DISTINCT FROM NEW.estado AND NEW.estado = 'INACTIVO')
    EXECUTE FUNCTION trg_fn_sede_desactivar();

-- ---------------------------------------------------------------------
-- Penalizaciones por daños
-- ---------------------------------------------------------------------

-- Registra el cargo y devuelve el id. Solo sobre reservas EN_CURSO o FINALIZADA.
-- (El backend comprueba además que la inspección exista en Mongo y sea del mismo vehículo.)
CREATE OR REPLACE FUNCTION fn_registrar_penalizacion(
    p_id_reserva     BIGINT,
    p_concepto       VARCHAR,
    p_monto          NUMERIC,
    p_id_inspeccion  VARCHAR
) RETURNS BIGINT
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
v_estado VARCHAR;
    v_id     BIGINT;
BEGIN
    IF btrim(coalesce(p_concepto, '')) = '' THEN
        RAISE EXCEPTION 'El concepto de la penalización es obligatorio';
END IF;
    IF p_monto IS NULL OR p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto de la penalización debe ser mayor que cero';
END IF;
    IF p_id_inspeccion IS NOT NULL AND p_id_inspeccion !~ '^[0-9a-f]{24}$' THEN
        RAISE EXCEPTION 'El identificador de inspección no es válido (24 caracteres hexadecimales)';
END IF;

SELECT estado_reserva INTO v_estado FROM reserva WHERE id_reserva = p_id_reserva FOR UPDATE;
IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe la reserva con id %', p_id_reserva;
END IF;
    IF v_estado NOT IN ('EN_CURSO', 'FINALIZADA') THEN
        RAISE EXCEPTION 'Solo se puede penalizar una reserva EN_CURSO o FINALIZADA (estado actual: %)', v_estado;
END IF;

INSERT INTO penalizacion (concepto, monto, id_inspeccion_mongo, reserva_id_reserva)
VALUES (btrim(p_concepto), p_monto, p_id_inspeccion, p_id_reserva)
    RETURNING id_penalizacion INTO v_id;

RETURN v_id;
END;
$$;

-- PENDIENTE -> PAGADA o ANULADA. Las demás reglas las hace cumplir el trigger.
CREATE OR REPLACE PROCEDURE sp_cambiar_estado_penalizacion(
    p_id_penalizacion BIGINT,
    p_estado          VARCHAR
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF p_estado NOT IN ('PAGADA', 'ANULADA') THEN
        RAISE EXCEPTION 'Estado no válido: % (use PAGADA o ANULADA)', p_estado;
END IF;
    PERFORM 1 FROM penalizacion WHERE id_penalizacion = p_id_penalizacion FOR UPDATE;
IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe la penalización con id %', p_id_penalizacion;
END IF;
UPDATE penalizacion SET estado_cargo = p_estado WHERE id_penalizacion = p_id_penalizacion;
END;
$$;

-- Aunque alguien escriba directo en la tabla: del cargo solo cambia el estado,
-- y una vez PAGADA o ANULADA ya no se mueve.
CREATE OR REPLACE FUNCTION trg_fn_penalizacion_cambios() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF NEW.concepto            IS DISTINCT FROM OLD.concepto
    OR NEW.monto               IS DISTINCT FROM OLD.monto
    OR NEW.fecha_cargo         IS DISTINCT FROM OLD.fecha_cargo
    OR NEW.id_inspeccion_mongo IS DISTINCT FROM OLD.id_inspeccion_mongo
    OR NEW.reserva_id_reserva  IS DISTINCT FROM OLD.reserva_id_reserva THEN
        RAISE EXCEPTION 'De una penalización solo se puede modificar el estado';
END IF;
    IF NEW.estado_cargo IS DISTINCT FROM OLD.estado_cargo AND OLD.estado_cargo <> 'PENDIENTE' THEN
        RAISE EXCEPTION 'La penalización % ya está % y no se puede cambiar', OLD.id_penalizacion, OLD.estado_cargo;
END IF;
RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_penalizacion_cambios ON penalizacion;
CREATE TRIGGER trg_penalizacion_cambios
    BEFORE UPDATE ON penalizacion
    FOR EACH ROW EXECUTE FUNCTION trg_fn_penalizacion_cambios();

REVOKE ALL ON FUNCTION  fn_registrar_penalizacion(BIGINT, VARCHAR, NUMERIC, VARCHAR) FROM PUBLIC;
REVOKE ALL ON PROCEDURE sp_cambiar_estado_penalizacion(BIGINT, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION  fn_registrar_penalizacion(BIGINT, VARCHAR, NUMERIC, VARCHAR) TO rol_asesor, rol_administrador;
GRANT EXECUTE ON PROCEDURE sp_cambiar_estado_penalizacion(BIGINT, VARCHAR) TO rol_administrador;

-- ---------------------------------------------------------------------
-- Kilometraje del vehículo (lo actualizan las inspecciones)
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_actualizar_kilometraje_vehiculo(
    p_id_vehiculo BIGINT,
    p_kilometraje INTEGER
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
v_actual INTEGER;
BEGIN
SELECT kilometraje INTO v_actual FROM vehiculo WHERE id_vehiculo = p_id_vehiculo FOR UPDATE;
IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el vehículo con id %', p_id_vehiculo;
END IF;
    IF p_kilometraje IS NULL OR p_kilometraje < v_actual THEN
        RAISE EXCEPTION 'El kilometraje (%) no puede ser menor que el actual del vehículo (%)',
                        p_kilometraje, v_actual;
END IF;
UPDATE vehiculo SET kilometraje = p_kilometraje WHERE id_vehiculo = p_id_vehiculo;
END;
$$;

REVOKE ALL ON PROCEDURE sp_actualizar_kilometraje_vehiculo(BIGINT, INTEGER) FROM PUBLIC;
GRANT EXECUTE ON PROCEDURE sp_actualizar_kilometraje_vehiculo(BIGINT, INTEGER)
    TO rol_asesor, rol_tecnico, rol_administrador;

COMMIT;
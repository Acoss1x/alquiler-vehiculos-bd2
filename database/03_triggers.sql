
-- 1) Transiciones válidas del estado de la reserva ---------------------------
CREATE OR REPLACE FUNCTION trg_fn_reserva_transicion() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF NOT (
        (OLD.estado_reserva = 'PENDIENTE'  AND NEW.estado_reserva IN ('CONFIRMADA', 'CANCELADA')) OR
        (OLD.estado_reserva = 'CONFIRMADA' AND NEW.estado_reserva IN ('EN_CURSO', 'CANCELADA'))  OR
        (OLD.estado_reserva = 'EN_CURSO'   AND NEW.estado_reserva = 'FINALIZADA')
    ) THEN
        RAISE EXCEPTION 'Transición de reserva no permitida: % -> %', OLD.estado_reserva, NEW.estado_reserva;
END IF;
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_reserva_transicion
    BEFORE UPDATE OF estado_reserva ON reserva
    FOR EACH ROW
    WHEN (OLD.estado_reserva IS DISTINCT FROM NEW.estado_reserva)
    EXECUTE FUNCTION trg_fn_reserva_transicion();

-- 2) Licencia vigente durante toda la reserva --------------------------------
CREATE OR REPLACE FUNCTION trg_fn_reserva_licencia() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
v_vencimiento DATE;
BEGIN
SELECT fecha_vencimiento INTO v_vencimiento
FROM licencia WHERE cliente_id_cliente = NEW.cliente_id_cliente;
IF NOT FOUND THEN
        RAISE EXCEPTION 'El cliente % no tiene licencia de conducción registrada', NEW.cliente_id_cliente;
END IF;
    IF v_vencimiento < NEW.fecha_fin THEN
        RAISE EXCEPTION 'La licencia del cliente vence el %, antes del fin de la reserva (%)',
                        v_vencimiento, NEW.fecha_fin;
END IF;
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_reserva_licencia
    BEFORE INSERT ON reserva
    FOR EACH ROW EXECUTE FUNCTION trg_fn_reserva_licencia();

-- 3) Estado del vehículo según la reserva ------------------------------------
--    EN_CURSO -> ALQUILADO | FINALIZADA -> DISPONIBLE en la sede donde se recibió.
CREATE OR REPLACE FUNCTION trg_fn_reserva_vehiculo() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF NEW.estado_reserva = 'EN_CURSO' THEN
UPDATE vehiculo SET estado = 'ALQUILADO' WHERE id_vehiculo = NEW.vehiculo_id_vehiculo;
ELSIF NEW.estado_reserva = 'FINALIZADA' THEN
UPDATE vehiculo
SET estado = 'DISPONIBLE', sede_id_sede = NEW.sede_recibe
WHERE id_vehiculo = NEW.vehiculo_id_vehiculo;
END IF;
RETURN NULL;
END;
$$;

CREATE TRIGGER trg_reserva_vehiculo
    AFTER UPDATE OF estado_reserva ON reserva
    FOR EACH ROW
    WHEN (OLD.estado_reserva IS DISTINCT FROM NEW.estado_reserva
          AND NEW.estado_reserva IN ('EN_CURSO', 'FINALIZADA'))
    EXECUTE FUNCTION trg_fn_reserva_vehiculo();

-- 4) Auditoría de cambios de estado de la reserva -----------------------------
CREATE OR REPLACE FUNCTION trg_fn_reserva_auditoria() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO auditoria_reserva (id_reserva, estado_anterior, estado_nuevo)
        VALUES (NEW.id_reserva, NULL, NEW.estado_reserva);
ELSE
        INSERT INTO auditoria_reserva (id_reserva, estado_anterior, estado_nuevo)
        VALUES (NEW.id_reserva, OLD.estado_reserva, NEW.estado_reserva);
END IF;
RETURN NULL;
END;
$$;

CREATE TRIGGER trg_reserva_auditoria_ins
    AFTER INSERT ON reserva
    FOR EACH ROW EXECUTE FUNCTION trg_fn_reserva_auditoria();

CREATE TRIGGER trg_reserva_auditoria_upd
    AFTER UPDATE OF estado_reserva ON reserva
    FOR EACH ROW
    WHEN (OLD.estado_reserva IS DISTINCT FROM NEW.estado_reserva)
    EXECUTE FUNCTION trg_fn_reserva_auditoria();

-- 5) Los pagos aprobados no pueden superar el costo pactado -------------------
CREATE OR REPLACE FUNCTION trg_fn_pago_limite() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
v_costo    NUMERIC;
    v_aprobado NUMERIC;
BEGIN
    IF NEW.estado_pago = 'APROBADO' THEN
SELECT costo_total INTO v_costo FROM reserva WHERE id_reserva = NEW.reserva_id_reserva;
SELECT COALESCE(SUM(monto), 0) INTO v_aprobado
FROM pago WHERE reserva_id_reserva = NEW.reserva_id_reserva AND estado_pago = 'APROBADO';
IF v_aprobado + NEW.monto > COALESCE(v_costo, 0) THEN
            RAISE EXCEPTION 'Los pagos aprobados (%) superarían el costo de la reserva (%)',
                            v_aprobado + NEW.monto, COALESCE(v_costo, 0);
END IF;
END IF;
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_pago_limite
    BEFORE INSERT ON pago
    FOR EACH ROW EXECUTE FUNCTION trg_fn_pago_limite();

-- 6) Especialización de empleado: DISJUNTA ----
CREATE OR REPLACE FUNCTION trg_fn_empleado_disjunto() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF (TG_TABLE_NAME <> 'administrador' AND EXISTS (SELECT 1 FROM administrador WHERE id_empleado = NEW.id_empleado))
    OR (TG_TABLE_NAME <> 'asesor'        AND EXISTS (SELECT 1 FROM asesor        WHERE id_empleado = NEW.id_empleado))
    OR (TG_TABLE_NAME <> 'tecnico'       AND EXISTS (SELECT 1 FROM tecnico       WHERE id_empleado = NEW.id_empleado)) THEN
        RAISE EXCEPTION 'El empleado % ya pertenece a otro subtipo (la especialización es disjunta)', NEW.id_empleado;
END IF;
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_disjunto_administrador BEFORE INSERT ON administrador FOR EACH ROW EXECUTE FUNCTION trg_fn_empleado_disjunto();
CREATE TRIGGER trg_disjunto_asesor        BEFORE INSERT ON asesor        FOR EACH ROW EXECUTE FUNCTION trg_fn_empleado_disjunto();
CREATE TRIGGER trg_disjunto_tecnico       BEFORE INSERT ON tecnico       FOR EACH ROW EXECUTE FUNCTION trg_fn_empleado_disjunto();

-- 7) Especialización de empleado: TOTAL (todo empleado debe tener un subtipo) --
--    Trigger de restricción DEFERRED: se verifica al COMMIT, para permitir insertar
--    primero el empleado y luego su subtipo en la misma transacción.
CREATE OR REPLACE FUNCTION trg_fn_empleado_total() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF NOT (EXISTS (SELECT 1 FROM administrador WHERE id_empleado = NEW.id_empleado)
         OR EXISTS (SELECT 1 FROM asesor        WHERE id_empleado = NEW.id_empleado)
         OR EXISTS (SELECT 1 FROM tecnico       WHERE id_empleado = NEW.id_empleado)) THEN
        RAISE EXCEPTION 'El empleado % debe ser administrador, asesor o técnico (especialización total)', NEW.id_empleado;
END IF;
RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_empleado_total
    AFTER INSERT ON empleado
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION trg_fn_empleado_total();

-- 8) Un correo no puede estar en cliente Y en empleado.
--    El bloqueo por correo evita que dos registros simultáneos se crucen.
CREATE OR REPLACE FUNCTION trg_fn_correo_unico() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext(lower(NEW.correo)));
    IF TG_TABLE_NAME = 'cliente' THEN
        IF EXISTS (SELECT 1 FROM empleado WHERE lower(correo) = lower(NEW.correo)) THEN
            RAISE EXCEPTION 'El correo % ya está registrado como empleado', NEW.correo;
END IF;
ELSE
        IF EXISTS (SELECT 1 FROM cliente WHERE lower(correo) = lower(NEW.correo)) THEN
            RAISE EXCEPTION 'El correo % ya está registrado como cliente', NEW.correo;
END IF;
END IF;
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_cliente_correo_unico
    BEFORE INSERT OR UPDATE OF correo ON cliente
    FOR EACH ROW EXECUTE FUNCTION trg_fn_correo_unico();

CREATE TRIGGER trg_empleado_correo_unico
    BEFORE INSERT OR UPDATE OF correo ON empleado
    FOR EACH ROW EXECUTE FUNCTION trg_fn_correo_unico();

-- 9) Reserva nueva: sedes de entrega y recibo activas, y categoría del vehículo activa.
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

CREATE TRIGGER trg_reserva_activos
    BEFORE INSERT ON reserva
    FOR EACH ROW EXECUTE FUNCTION trg_fn_reserva_activos();

-- 10) Seguro contratado: debe estar activo.
CREATE OR REPLACE FUNCTION trg_fn_seg_res_activo() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF (SELECT estado FROM seguro WHERE id_seguro = NEW.seguro_id_seguro) <> 'ACTIVO' THEN
        RAISE EXCEPTION 'El seguro % está inactivo', NEW.seguro_id_seguro;
END IF;
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_seg_res_activo
    BEFORE INSERT ON seg_res
    FOR EACH ROW EXECUTE FUNCTION trg_fn_seg_res_activo();

-- 11) Vehículo nuevo: categoría y sede activas.
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

CREATE TRIGGER trg_vehiculo_activos
    BEFORE INSERT ON vehiculo
    FOR EACH ROW EXECUTE FUNCTION trg_fn_vehiculo_activos();

-- 12) Desactivar una sede: solo si no tiene vehículos, reservas activas ni empleados activos.
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

CREATE TRIGGER trg_sede_desactivar
    BEFORE UPDATE OF estado ON sede
    FOR EACH ROW
    WHEN (OLD.estado IS DISTINCT FROM NEW.estado AND NEW.estado = 'INACTIVO')
    EXECUTE FUNCTION trg_fn_sede_desactivar();

-- 13) Penalización: del cargo solo cambia el estado, y una vez PAGADA o ANULADA ya no se mueve
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

CREATE TRIGGER trg_penalizacion_cambios
    BEFORE UPDATE ON penalizacion
    FOR EACH ROW EXECUTE FUNCTION trg_fn_penalizacion_cambios();

-- =====================================================================
-- 03_triggers.sql : reglas que deben cumplirse SIEMPRE, incluso si alguien
-- escribe directo en las tablas sin pasar por los procedimientos.
-- =====================================================================

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

-- 6) Especialización de empleado: DISJUNTA (no puede estar en dos subtipos) ----
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
--    primero el empleado y luego su subtipo en la misma transacción (como hace Hibernate).
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

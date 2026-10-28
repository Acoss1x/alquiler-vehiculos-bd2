-- =====================================================================
-- 07_cambios_fase2.sql : alinea la base de datos con el backend.
-- Corre DESPUÉS de 01 a 06 sobre la BD ya creada (se puede repetir).
--
-- Qué arregla:
--   1) Credenciales: password_hash y debe_cambiar_clave en cliente y empleado,
--      correo en empleado, y correo único entre las DOS tablas (trigger).
--   2) Estado BLOQUEADO para cliente; BAJA y VENDIDO para vehículo.
--   3) Columna estado en sede, categoria y seguro.
--   4) Reserva sin asesor (el cliente que reserva solo en la web):
--      fn_crear_reserva acepta asesor NULL y sp_iniciar_reserva(reserva, asesor)
--      asigna quien entrega. Se mantiene sp_iniciar_reserva(reserva) para 06_pruebas.
--   5) Tabla penalizacion (cargos por daños, supuesto 24).
--   6) sp_registrar_cliente recibe el hash BCrypt (8 parámetros).
--   7) sp_actualizar_estado_vehiculo acepta BAJA y VENDIDO.
--
-- Después de correrlo, actualiza 04_datos_prueba y 06_pruebas: llaman a
-- sp_registrar_cliente con 7 parámetros (ahora son 8, el último el hash).
--
-- Las filas que ya existían reciben una clave temporal ('Cambiar123*') con
-- debe_cambiar_clave = TRUE, y los empleados un correo provisional
-- (empleadoN@alquiler.com). Cámbialos o vuelve a sembrar 04_datos_prueba.
-- =====================================================================

BEGIN;

-- pgcrypto genera hashes $2a$ (BCrypt), los mismos que lee Spring Security.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Los roles los crea 05_seguridad.sql; aquí se aseguran para poder correr 07 antes o después.
DO $$
DECLARE
    v_rol TEXT;
BEGIN
    FOREACH v_rol IN ARRAY ARRAY['rol_asesor', 'rol_tecnico', 'rol_administrador'] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_rol) THEN
            EXECUTE format('CREATE ROLE %I NOLOGIN', v_rol);
        END IF;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------
-- 1) CLIENTE: credenciales y estado BLOQUEADO
-- ---------------------------------------------------------------------
ALTER TABLE cliente
    ADD COLUMN IF NOT EXISTS password_hash      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS debe_cambiar_clave BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE cliente
   SET password_hash      = crypt('Cambiar123*', gen_salt('bf', 10)),
       debe_cambiar_clave = TRUE
 WHERE password_hash IS NULL;

ALTER TABLE cliente ALTER COLUMN password_hash SET NOT NULL;

ALTER TABLE cliente DROP CONSTRAINT IF EXISTS cliente_estado_check;
ALTER TABLE cliente DROP CONSTRAINT IF EXISTS ck_cliente_estado;
ALTER TABLE cliente ADD CONSTRAINT ck_cliente_estado
    CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO'));

-- ---------------------------------------------------------------------
-- 2) EMPLEADO: correo y credenciales
-- ---------------------------------------------------------------------
ALTER TABLE empleado
    ADD COLUMN IF NOT EXISTS correo             VARCHAR(100),
    ADD COLUMN IF NOT EXISTS password_hash      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS debe_cambiar_clave BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE empleado
   SET correo = 'empleado' || id_empleado || '@alquiler.com'
 WHERE correo IS NULL;

UPDATE empleado
   SET password_hash      = crypt('Cambiar123*', gen_salt('bf', 10)),
       debe_cambiar_clave = TRUE
 WHERE password_hash IS NULL;

ALTER TABLE empleado
    ALTER COLUMN correo        SET NOT NULL,
    ALTER COLUMN password_hash SET NOT NULL;

ALTER TABLE empleado DROP CONSTRAINT IF EXISTS uq_empleado_correo;
ALTER TABLE empleado ADD CONSTRAINT uq_empleado_correo UNIQUE (correo);
ALTER TABLE empleado DROP CONSTRAINT IF EXISTS ck_empleado_correo;
ALTER TABLE empleado ADD CONSTRAINT ck_empleado_correo CHECK (correo LIKE '%_@_%');

-- ---------------------------------------------------------------------
-- 3) Un correo no puede estar en cliente Y en empleado (el login busca en ambas).
--    El bloqueo por correo evita que dos registros simultáneos se crucen.
-- ---------------------------------------------------------------------
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

DROP TRIGGER IF EXISTS trg_cliente_correo_unico ON cliente;
CREATE TRIGGER trg_cliente_correo_unico
    BEFORE INSERT OR UPDATE OF correo ON cliente
    FOR EACH ROW EXECUTE FUNCTION trg_fn_correo_unico();

DROP TRIGGER IF EXISTS trg_empleado_correo_unico ON empleado;
CREATE TRIGGER trg_empleado_correo_unico
    BEFORE INSERT OR UPDATE OF correo ON empleado
    FOR EACH ROW EXECUTE FUNCTION trg_fn_correo_unico();

-- ---------------------------------------------------------------------
-- 4) Estado en los catálogos (ACTIVO / INACTIVO)
-- ---------------------------------------------------------------------
ALTER TABLE sede      ADD COLUMN IF NOT EXISTS estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO';
ALTER TABLE categoria ADD COLUMN IF NOT EXISTS estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO';
ALTER TABLE seguro    ADD COLUMN IF NOT EXISTS estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO';

ALTER TABLE sede      DROP CONSTRAINT IF EXISTS ck_sede_estado;
ALTER TABLE categoria DROP CONSTRAINT IF EXISTS ck_categoria_estado;
ALTER TABLE seguro    DROP CONSTRAINT IF EXISTS ck_seguro_estado;
ALTER TABLE sede      ADD CONSTRAINT ck_sede_estado      CHECK (estado IN ('ACTIVO', 'INACTIVO'));
ALTER TABLE categoria ADD CONSTRAINT ck_categoria_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'));
ALTER TABLE seguro    ADD CONSTRAINT ck_seguro_estado    CHECK (estado IN ('ACTIVO', 'INACTIVO'));

-- ---------------------------------------------------------------------
-- 5) Vehículo: BAJA y VENDIDO (el backend ya los usa) y que no salgan como disponibles
-- ---------------------------------------------------------------------
ALTER TABLE vehiculo DROP CONSTRAINT IF EXISTS vehiculo_estado_check;
ALTER TABLE vehiculo DROP CONSTRAINT IF EXISTS ck_vehiculo_estado;
ALTER TABLE vehiculo ADD CONSTRAINT ck_vehiculo_estado
    CHECK (estado IN ('DISPONIBLE', 'ALQUILADO', 'EN_MANTENIMIENTO', 'BAJA', 'VENDIDO'));

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
       AND NOT EXISTS (
            SELECT 1 FROM reserva r
             WHERE r.vehiculo_id_vehiculo = v.id_vehiculo
               AND r.estado_reserva IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO')
               AND daterange(r.fecha_inicio, r.fecha_fin, '[]')
                   && daterange(p_fecha_inicio, p_fecha_fin, '[]'))
     ORDER BY v.id_vehiculo;
END;
$$;

-- ---------------------------------------------------------------------
-- 6) Reserva sin asesor: el cliente que reserva en la web no tiene asesor;
--    se asigna al entregar el vehículo (sp_iniciar_reserva recibe el asesor).
-- ---------------------------------------------------------------------
ALTER TABLE reserva ALTER COLUMN asesor_id_empleado DROP NOT NULL;

-- fn_crear_reserva: el asesor es opcional (si viene, debe estar activo).
CREATE OR REPLACE FUNCTION fn_crear_reserva(
    p_id_cliente     BIGINT,
    p_id_vehiculo    BIGINT,
    p_id_asesor      BIGINT,
    p_sede_entrega   BIGINT,
    p_sede_recibe    BIGINT,
    p_fecha_inicio   DATE,
    p_fecha_fin      DATE,
    p_ids_seguros    BIGINT[]
) RETURNS BIGINT
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_estado_cli  VARCHAR;
    v_estado_veh  VARCHAR;
    v_dias        INTEGER;
    v_costo       NUMERIC;
    v_id_reserva  BIGINT;
BEGIN
    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL THEN
        RAISE EXCEPTION 'Las fechas de inicio y fin son obligatorias';
    END IF;
    IF p_fecha_fin < p_fecha_inicio THEN
        RAISE EXCEPTION 'La fecha de fin no puede ser anterior a la de inicio';
    END IF;
    IF p_fecha_inicio < current_date THEN
        RAISE EXCEPTION 'La fecha de inicio no puede ser pasada';
    END IF;

    SELECT estado INTO v_estado_cli FROM cliente WHERE id_cliente = p_id_cliente;
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe el cliente con id %', p_id_cliente; END IF;
    IF v_estado_cli <> 'ACTIVO' THEN RAISE EXCEPTION 'El cliente % está inactivo o bloqueado', p_id_cliente; END IF;

    IF p_id_asesor IS NOT NULL THEN
        PERFORM 1 FROM asesor a JOIN empleado e ON e.id_empleado = a.id_empleado
         WHERE a.id_empleado = p_id_asesor AND e.estado = 'ACTIVO';
        IF NOT FOUND THEN RAISE EXCEPTION 'No existe un asesor activo con id %', p_id_asesor; END IF;
    END IF;

    PERFORM 1 FROM sede WHERE id_sede = p_sede_entrega;
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe la sede de entrega con id %', p_sede_entrega; END IF;
    PERFORM 1 FROM sede WHERE id_sede = p_sede_recibe;
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe la sede de recibo con id %', p_sede_recibe; END IF;

    -- Bloqueo de la fila del vehículo: serializa a los asesores que reserven el mismo carro.
    SELECT estado INTO v_estado_veh FROM vehiculo WHERE id_vehiculo = p_id_vehiculo FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe el vehículo con id %', p_id_vehiculo; END IF;
    IF v_estado_veh = 'EN_MANTENIMIENTO' THEN
        RAISE EXCEPTION 'El vehículo % está en mantenimiento', p_id_vehiculo;
    END IF;
    IF v_estado_veh IN ('BAJA', 'VENDIDO') THEN
        RAISE EXCEPTION 'El vehículo % está dado de baja o vendido', p_id_vehiculo;
    END IF;

    IF EXISTS (
        SELECT 1 FROM reserva r
         WHERE r.vehiculo_id_vehiculo = p_id_vehiculo
           AND r.estado_reserva IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO')
           AND daterange(r.fecha_inicio, r.fecha_fin, '[]')
               && daterange(p_fecha_inicio, p_fecha_fin, '[]')) THEN
        RAISE EXCEPTION 'El vehículo % ya tiene una reserva activa que se cruza con esas fechas', p_id_vehiculo;
    END IF;

    v_costo := fn_costo_reserva(p_id_vehiculo, p_fecha_inicio, p_fecha_fin, p_ids_seguros);
    v_dias  := (p_fecha_fin - p_fecha_inicio) + 1;

    INSERT INTO reserva (estado_reserva, fecha_inicio, fecha_fin, costo_total,
                         cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado,
                         sede_entrega, sede_recibe)
    VALUES ('PENDIENTE', p_fecha_inicio, p_fecha_fin, v_costo,
            p_id_cliente, p_id_vehiculo, p_id_asesor, p_sede_entrega, p_sede_recibe)
    RETURNING id_reserva INTO v_id_reserva;

    -- Se congela el costo de cada seguro (costo_diario x días) como precio pactado.
    IF p_ids_seguros IS NOT NULL AND cardinality(p_ids_seguros) > 0 THEN
        INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado)
        SELECT s.id_seguro, v_id_reserva, s.costo_diario * v_dias
          FROM seguro s WHERE s.id_seguro = ANY (p_ids_seguros);
    END IF;

    RETURN v_id_reserva;
END;
$$;

-- sp_iniciar_reserva(reserva, asesor): quien entrega el vehículo queda como asesor.
CREATE OR REPLACE PROCEDURE sp_iniciar_reserva(p_id_reserva BIGINT, p_id_asesor BIGINT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_res reserva%ROWTYPE;
    v_estado_veh VARCHAR;
BEGIN
    SELECT * INTO v_res FROM reserva WHERE id_reserva = p_id_reserva FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe la reserva con id %', p_id_reserva; END IF;
    IF v_res.estado_reserva <> 'CONFIRMADA' THEN
        RAISE EXCEPTION 'Solo se puede iniciar una reserva CONFIRMADA (estado actual: %)', v_res.estado_reserva;
    END IF;
    IF current_date < v_res.fecha_inicio THEN
        RAISE EXCEPTION 'La reserva inicia el %: aún no se puede entregar el vehículo', v_res.fecha_inicio;
    END IF;
    IF current_date > v_res.fecha_fin THEN
        RAISE EXCEPTION 'La reserva terminó el %: ya no se puede iniciar', v_res.fecha_fin;
    END IF;

    PERFORM 1 FROM asesor a JOIN empleado e ON e.id_empleado = a.id_empleado
     WHERE a.id_empleado = p_id_asesor AND e.estado = 'ACTIVO';
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe un asesor activo con id %', p_id_asesor; END IF;

    SELECT estado INTO v_estado_veh FROM vehiculo WHERE id_vehiculo = v_res.vehiculo_id_vehiculo FOR UPDATE;
    IF v_estado_veh <> 'DISPONIBLE' THEN
        RAISE EXCEPTION 'El vehículo no está disponible para la entrega (estado: %)', v_estado_veh;
    END IF;

    UPDATE reserva
       SET estado_reserva = 'EN_CURSO', asesor_id_empleado = p_id_asesor
     WHERE id_reserva = p_id_reserva;
END;
$$;

-- Versión de un parámetro (la usan 06_pruebas y reservas que ya tienen asesor).
CREATE OR REPLACE PROCEDURE sp_iniciar_reserva(p_id_reserva BIGINT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_asesor BIGINT;
BEGIN
    SELECT asesor_id_empleado INTO v_asesor FROM reserva WHERE id_reserva = p_id_reserva;
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe la reserva con id %', p_id_reserva; END IF;
    IF v_asesor IS NULL THEN
        RAISE EXCEPTION 'La reserva % no tiene asesor: indique quién entrega el vehículo', p_id_reserva;
    END IF;
    CALL sp_iniciar_reserva(p_id_reserva, v_asesor);
END;
$$;

REVOKE ALL ON PROCEDURE sp_iniciar_reserva(BIGINT, BIGINT) FROM PUBLIC;
GRANT EXECUTE ON PROCEDURE sp_iniciar_reserva(BIGINT, BIGINT) TO rol_asesor, rol_administrador;

-- sp_actualizar_estado_vehiculo: agrega BAJA y VENDIDO (estados finales).
CREATE OR REPLACE PROCEDURE sp_actualizar_estado_vehiculo(
    p_id_vehiculo  BIGINT,
    p_nuevo_estado VARCHAR
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_actual VARCHAR;
BEGIN
    SELECT estado INTO v_actual FROM vehiculo WHERE id_vehiculo = p_id_vehiculo FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el vehículo con id %', p_id_vehiculo;
    END IF;
    IF p_nuevo_estado NOT IN ('DISPONIBLE', 'ALQUILADO', 'EN_MANTENIMIENTO', 'BAJA', 'VENDIDO') THEN
        RAISE EXCEPTION 'Estado de vehículo no válido: %', p_nuevo_estado;
    END IF;
    IF p_nuevo_estado = 'ALQUILADO' THEN
        RAISE EXCEPTION 'El estado ALQUILADO solo se asigna al iniciar una reserva';
    END IF;
    IF v_actual = 'ALQUILADO' THEN
        RAISE EXCEPTION 'El vehículo está alquilado: finalice la reserva en curso antes de cambiar su estado';
    END IF;
    IF v_actual IN ('BAJA', 'VENDIDO') THEN
        RAISE EXCEPTION 'El vehículo está dado de baja o vendido: su estado ya no se puede cambiar';
    END IF;
    IF v_actual = p_nuevo_estado THEN
        RETURN;                                        -- sin cambios
    END IF;

    UPDATE vehiculo SET estado = p_nuevo_estado WHERE id_vehiculo = p_id_vehiculo;
END;
$$;

-- ---------------------------------------------------------------------
-- 7) Penalización por daños (supuesto 24).
--    Es un cargo por cobrar, distinto del pago (que es lo ya pagado).
--    id_inspeccion_mongo enlaza con la inspección que documenta el daño.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS penalizacion (
    id_penalizacion     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_cargo         DATE          NOT NULL DEFAULT current_date,
    concepto            VARCHAR(200)  NOT NULL,
    monto               NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    estado_cargo        VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado_cargo IN ('PENDIENTE', 'PAGADA', 'ANULADA')),
    id_inspeccion_mongo VARCHAR(24),
    reserva_id_reserva  BIGINT        NOT NULL REFERENCES reserva (id_reserva)
);
CREATE INDEX IF NOT EXISTS ix_penalizacion_reserva ON penalizacion (reserva_id_reserva);

GRANT SELECT ON penalizacion TO rol_asesor;
GRANT ALL    ON penalizacion TO rol_administrador;

-- ---------------------------------------------------------------------
-- 8) sp_registrar_cliente: ahora recibe el hash BCrypt (8.º parámetro).
--    Las funciones nuevas nacen con EXECUTE para PUBLIC: se revoca.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_registrar_cliente(VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR);

CREATE OR REPLACE PROCEDURE sp_registrar_cliente(
    p_documento        VARCHAR,
    p_primer_nombre    VARCHAR,
    p_segundo_nombre   VARCHAR,
    p_primer_apellido  VARCHAR,
    p_segundo_apellido VARCHAR,
    p_correo           VARCHAR,
    p_telefono         VARCHAR,
    p_password_hash    VARCHAR
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF btrim(coalesce(p_documento, '')) = '' OR btrim(coalesce(p_primer_nombre, '')) = ''
       OR btrim(coalesce(p_primer_apellido, '')) = '' OR btrim(coalesce(p_correo, '')) = '' THEN
        RAISE EXCEPTION 'Documento, primer nombre, primer apellido y correo son obligatorios';
    END IF;
    IF p_password_hash IS NULL OR left(p_password_hash, 2) <> '$2' OR length(p_password_hash) < 60 THEN
        RAISE EXCEPTION 'La contraseña debe llegar como hash BCrypt';
    END IF;
    IF EXISTS (SELECT 1 FROM cliente WHERE numero_documento = p_documento) THEN
        RAISE EXCEPTION 'Ya existe un cliente con el documento %', p_documento;
    END IF;
    IF EXISTS (SELECT 1 FROM cliente WHERE lower(correo) = lower(p_correo)) THEN
        RAISE EXCEPTION 'Ya existe un cliente con el correo %', p_correo;
    END IF;

    INSERT INTO cliente (numero_documento, primer_nombre, segundo_nombre,
                         primer_apellido, segundo_apellido, correo, telefono, estado,
                         password_hash)
    VALUES (p_documento, p_primer_nombre, p_segundo_nombre,
            p_primer_apellido, p_segundo_apellido, p_correo, p_telefono, 'ACTIVO',
            p_password_hash);
END;
$$;

REVOKE ALL ON PROCEDURE sp_registrar_cliente(VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC;
GRANT EXECUTE ON PROCEDURE sp_registrar_cliente(VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR)
    TO rol_asesor, rol_administrador;

COMMIT;

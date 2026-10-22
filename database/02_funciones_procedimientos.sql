-- =====================================================================
-- 02_funciones_procedimientos.sql
-- Contrato que el backend (Spring) ya asume. El ORDEN de los parámetros debe
-- coincidir con el de los métodos @Procedure / @Query del repositorio Java.
--
-- * Los errores de negocio se lanzan con RAISE EXCEPTION y un mensaje legible:
--   el ApiExceptionHandler lo devuelve al cliente como HTTP 422.
-- * No se usa COMMIT/ROLLBACK dentro de los procedimientos: la transacción la
--   abre Spring (@Transactional). Al lanzarse una excepción, PostgreSQL revierte
--   todo lo hecho por el procedimiento (rollback automático).
-- * Los que escriben datos son SECURITY DEFINER con search_path fijo: los roles
--   de la aplicación pueden ejecutarlos sin tener DML directo sobre las tablas.
-- =====================================================================

-- ---------------------------------------------------------------------
-- FUNCIONES DE CONSULTA (STABLE, solo leen)
-- ---------------------------------------------------------------------

-- Días x (tarifa de la categoría + suma de seguros diarios).
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
    v_dias          INTEGER;
    v_seguros_dia   NUMERIC := 0;
    v_encontrados   INTEGER := 0;
BEGIN
    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL THEN
        RAISE EXCEPTION 'Las fechas de inicio y fin son obligatorias';
    END IF;
    IF p_fecha_fin < p_fecha_inicio THEN
        RAISE EXCEPTION 'La fecha de fin no puede ser anterior a la de inicio';
    END IF;

    SELECT c.tarifa_diaria INTO v_tarifa
      FROM vehiculo v JOIN categoria c ON c.id_categoria = v.categoria_id_categoria
     WHERE v.id_vehiculo = p_id_vehiculo;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el vehículo con id %', p_id_vehiculo;
    END IF;

    IF p_ids_seguros IS NOT NULL AND cardinality(p_ids_seguros) > 0 THEN
        SELECT COALESCE(SUM(costo_diario), 0), COUNT(*) INTO v_seguros_dia, v_encontrados
          FROM seguro WHERE id_seguro = ANY (p_ids_seguros);
        IF v_encontrados <> (SELECT COUNT(DISTINCT x) FROM unnest(p_ids_seguros) AS x) THEN
            RAISE EXCEPTION 'Alguno de los seguros indicados no existe';
        END IF;
    END IF;

    v_dias := (p_fecha_fin - p_fecha_inicio) + 1;     -- el día de inicio y el de fin se cobran
    RETURN v_dias * (v_tarifa + v_seguros_dia);
END;
$$;

-- Costo pactado de la reserva menos los pagos aprobados.
CREATE OR REPLACE FUNCTION fn_saldo_pendiente(p_id_reserva BIGINT)
RETURNS NUMERIC
LANGUAGE plpgsql STABLE
AS $$
DECLARE
    v_costo NUMERIC;
BEGIN
    SELECT costo_total INTO v_costo FROM reserva WHERE id_reserva = p_id_reserva;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe la reserva con id %', p_id_reserva;
    END IF;
    RETURN COALESCE(v_costo, 0) - COALESCE(
        (SELECT SUM(monto) FROM pago
          WHERE reserva_id_reserva = p_id_reserva AND estado_pago = 'APROBADO'), 0);
END;
$$;

-- Vehículos que no están en mantenimiento y no tienen una reserva activa que se
-- cruce con el rango. Devuelve filas completas de VEHICULO (el backend las mapea
-- directamente a la entidad). Filtros de categoría y sede opcionales (NULL = todos).
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
     WHERE v.estado <> 'EN_MANTENIMIENTO'
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
-- CLIENTES Y LICENCIAS
-- ---------------------------------------------------------------------

CREATE OR REPLACE PROCEDURE sp_registrar_cliente(
    p_documento        VARCHAR,
    p_primer_nombre    VARCHAR,
    p_segundo_nombre   VARCHAR,
    p_primer_apellido  VARCHAR,
    p_segundo_apellido VARCHAR,
    p_correo           VARCHAR,
    p_telefono         VARCHAR
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF btrim(coalesce(p_documento, '')) = '' OR btrim(coalesce(p_primer_nombre, '')) = ''
       OR btrim(coalesce(p_primer_apellido, '')) = '' OR btrim(coalesce(p_correo, '')) = '' THEN
        RAISE EXCEPTION 'Documento, primer nombre, primer apellido y correo son obligatorios';
    END IF;
    IF EXISTS (SELECT 1 FROM cliente WHERE numero_documento = p_documento) THEN
        RAISE EXCEPTION 'Ya existe un cliente con el documento %', p_documento;
    END IF;
    IF EXISTS (SELECT 1 FROM cliente WHERE lower(correo) = lower(p_correo)) THEN
        RAISE EXCEPTION 'Ya existe un cliente con el correo %', p_correo;
    END IF;

    INSERT INTO cliente (numero_documento, primer_nombre, segundo_nombre,
                         primer_apellido, segundo_apellido, correo, telefono, estado)
    VALUES (p_documento, p_primer_nombre, p_segundo_nombre,
            p_primer_apellido, p_segundo_apellido, p_correo, p_telefono, 'ACTIVO');
END;
$$;

CREATE OR REPLACE PROCEDURE sp_registrar_licencia(
    p_id_cliente        BIGINT,
    p_numero_licencia   VARCHAR,
    p_categoria         VARCHAR,
    p_fecha_expedicion  DATE,
    p_fecha_vencimiento DATE
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    PERFORM 1 FROM cliente WHERE id_cliente = p_id_cliente FOR UPDATE;   -- serializa por cliente
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el cliente con id %', p_id_cliente;
    END IF;
    IF EXISTS (SELECT 1 FROM licencia WHERE cliente_id_cliente = p_id_cliente) THEN
        RAISE EXCEPTION 'El cliente % ya tiene una licencia registrada', p_id_cliente;
    END IF;
    IF EXISTS (SELECT 1 FROM licencia WHERE numero_licencia = p_numero_licencia) THEN
        RAISE EXCEPTION 'Ya existe una licencia con el número %', p_numero_licencia;
    END IF;
    IF p_fecha_expedicion > current_date THEN
        RAISE EXCEPTION 'La fecha de expedición no puede ser futura';
    END IF;
    IF p_fecha_vencimiento <= p_fecha_expedicion THEN
        RAISE EXCEPTION 'La fecha de vencimiento debe ser posterior a la de expedición';
    END IF;
    IF p_fecha_vencimiento < current_date THEN
        RAISE EXCEPTION 'La licencia está vencida (venció el %)', p_fecha_vencimiento;
    END IF;

    INSERT INTO licencia (numero_licencia, categoria_licencia, fecha_expedicion,
                          fecha_vencimiento, cliente_id_cliente)
    VALUES (p_numero_licencia, p_categoria, p_fecha_expedicion, p_fecha_vencimiento, p_id_cliente);
END;
$$;

-- ---------------------------------------------------------------------
-- VEHÍCULOS
-- ---------------------------------------------------------------------

CREATE OR REPLACE PROCEDURE sp_registrar_vehiculo(
    p_placa         VARCHAR,
    p_marca         VARCHAR,
    p_modelo_anio   INTEGER,
    p_kilometraje   INTEGER,
    p_color         VARCHAR,
    p_id_categoria  BIGINT,
    p_id_sede       BIGINT
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_capacidad INTEGER;
    v_ocupados  INTEGER;
BEGIN
    PERFORM 1 FROM categoria WHERE id_categoria = p_id_categoria;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe la categoría con id %', p_id_categoria;
    END IF;

    -- Se bloquea la sede para que dos registros simultáneos no superen su capacidad.
    SELECT capacidad_estacionamiento INTO v_capacidad FROM sede WHERE id_sede = p_id_sede FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe la sede con id %', p_id_sede;
    END IF;

    IF EXISTS (SELECT 1 FROM vehiculo WHERE upper(placa) = upper(p_placa)) THEN
        RAISE EXCEPTION 'Ya existe un vehículo con la placa %', p_placa;
    END IF;

    SELECT COUNT(*) INTO v_ocupados FROM vehiculo WHERE sede_id_sede = p_id_sede;
    IF v_ocupados >= v_capacidad THEN
        RAISE EXCEPTION 'La sede % no tiene cupo de estacionamiento (capacidad %)', p_id_sede, v_capacidad;
    END IF;

    INSERT INTO vehiculo (placa, marca, modelo_anio, kilometraje, estado, color,
                          categoria_id_categoria, sede_id_sede)
    VALUES (upper(p_placa), p_marca, p_modelo_anio, p_kilometraje, 'DISPONIBLE', p_color,
            p_id_categoria, p_id_sede);
END;
$$;

-- Cambio manual de estado: solo DISPONIBLE <-> EN_MANTENIMIENTO.
-- ALQUILADO lo asigna únicamente el trigger al iniciar una reserva.
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
    IF p_nuevo_estado NOT IN ('DISPONIBLE', 'ALQUILADO', 'EN_MANTENIMIENTO') THEN
        RAISE EXCEPTION 'Estado de vehículo no válido: %', p_nuevo_estado;
    END IF;
    IF p_nuevo_estado = 'ALQUILADO' THEN
        RAISE EXCEPTION 'El estado ALQUILADO solo se asigna al iniciar una reserva';
    END IF;
    IF v_actual = 'ALQUILADO' THEN
        RAISE EXCEPTION 'El vehículo está alquilado: finalice la reserva en curso antes de cambiar su estado';
    END IF;
    IF v_actual = p_nuevo_estado THEN
        RETURN;                                        -- sin cambios
    END IF;

    UPDATE vehiculo SET estado = p_nuevo_estado WHERE id_vehiculo = p_id_vehiculo;
END;
$$;

-- ---------------------------------------------------------------------
-- RESERVAS
-- ---------------------------------------------------------------------

-- Crea la reserva en PENDIENTE con sus seguros y devuelve el id.
-- La licencia vigente y la sede se validan además en triggers (03_triggers.sql).
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
    IF v_estado_cli <> 'ACTIVO' THEN RAISE EXCEPTION 'El cliente % está inactivo', p_id_cliente; END IF;

    PERFORM 1 FROM asesor a JOIN empleado e ON e.id_empleado = a.id_empleado
     WHERE a.id_empleado = p_id_asesor AND e.estado = 'ACTIVO';
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe un asesor activo con id %', p_id_asesor; END IF;

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

-- Aprobado: confirma una reserva PENDIENTE. Rechazado: si la reserva estaba
-- PENDIENTE la cancela (libera el vehículo); en otro estado solo deja el registro.
CREATE OR REPLACE PROCEDURE sp_registrar_pago(
    p_id_reserva BIGINT,
    p_monto      NUMERIC,
    p_metodo     VARCHAR,
    p_estado     VARCHAR
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_estado_res VARCHAR;
    v_saldo      NUMERIC;
BEGIN
    IF p_estado NOT IN ('APROBADO', 'RECHAZADO') THEN
        RAISE EXCEPTION 'Solo se pueden registrar pagos APROBADO o RECHAZADO (recibido: %)', p_estado;
    END IF;
    IF p_monto IS NULL OR p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto del pago debe ser mayor que cero';
    END IF;

    SELECT estado_reserva INTO v_estado_res FROM reserva WHERE id_reserva = p_id_reserva FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe la reserva con id %', p_id_reserva;
    END IF;
    IF v_estado_res IN ('CANCELADA', 'FINALIZADA') THEN
        RAISE EXCEPTION 'No se pueden registrar pagos en una reserva %', v_estado_res;
    END IF;

    IF p_estado = 'APROBADO' THEN
        v_saldo := fn_saldo_pendiente(p_id_reserva);
        IF p_monto > v_saldo THEN
            RAISE EXCEPTION 'El pago (%) excede el saldo pendiente (%)', p_monto, v_saldo;
        END IF;
    END IF;

    INSERT INTO pago (monto, metodo_pago, estado_pago, reserva_id_reserva)
    VALUES (p_monto, p_metodo, p_estado, p_id_reserva);

    IF p_estado = 'APROBADO' AND v_estado_res = 'PENDIENTE' THEN
        UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = p_id_reserva;
    ELSIF p_estado = 'RECHAZADO' AND v_estado_res = 'PENDIENTE' THEN
        UPDATE reserva SET estado_reserva = 'CANCELADA' WHERE id_reserva = p_id_reserva;
    END IF;
END;
$$;

-- CONFIRMADA -> EN_CURSO (entrega). El trigger deja el vehículo ALQUILADO.
CREATE OR REPLACE PROCEDURE sp_iniciar_reserva(p_id_reserva BIGINT)
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

    SELECT estado INTO v_estado_veh FROM vehiculo WHERE id_vehiculo = v_res.vehiculo_id_vehiculo FOR UPDATE;
    IF v_estado_veh <> 'DISPONIBLE' THEN
        RAISE EXCEPTION 'El vehículo no está disponible para la entrega (estado: %)', v_estado_veh;
    END IF;

    UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = p_id_reserva;
END;
$$;

-- EN_CURSO -> FINALIZADA (devolución). Exige saldo en cero. El trigger libera el
-- vehículo y lo deja en la sede donde se recibió.
CREATE OR REPLACE PROCEDURE sp_finalizar_reserva(p_id_reserva BIGINT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_estado VARCHAR;
    v_saldo  NUMERIC;
BEGIN
    SELECT estado_reserva INTO v_estado FROM reserva WHERE id_reserva = p_id_reserva FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe la reserva con id %', p_id_reserva; END IF;
    IF v_estado <> 'EN_CURSO' THEN
        RAISE EXCEPTION 'Solo se puede finalizar una reserva EN_CURSO (estado actual: %)', v_estado;
    END IF;

    v_saldo := fn_saldo_pendiente(p_id_reserva);
    IF v_saldo > 0 THEN
        RAISE EXCEPTION 'No se puede finalizar: la reserva tiene un saldo pendiente de %', v_saldo;
    END IF;

    UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = p_id_reserva;
END;
$$;

-- Cancela una reserva PENDIENTE o CONFIRMADA y reembolsa los pagos aprobados.
CREATE OR REPLACE PROCEDURE sp_cancelar_reserva(p_id_reserva BIGINT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_estado VARCHAR;
BEGIN
    SELECT estado_reserva INTO v_estado FROM reserva WHERE id_reserva = p_id_reserva FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe la reserva con id %', p_id_reserva; END IF;
    IF v_estado NOT IN ('PENDIENTE', 'CONFIRMADA') THEN
        RAISE EXCEPTION 'Solo se puede cancelar una reserva PENDIENTE o CONFIRMADA (estado actual: %)', v_estado;
    END IF;

    UPDATE pago SET estado_pago = 'REEMBOLSADO'
     WHERE reserva_id_reserva = p_id_reserva AND estado_pago = 'APROBADO';
    UPDATE reserva SET estado_reserva = 'CANCELADA' WHERE id_reserva = p_id_reserva;
END;
$$;

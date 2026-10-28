-- =====================================================================
-- 08_pruebas_fase2.sql : pruebas de lo agregado en 07_cambios_fase2.sql.
-- Corre DESPUÉS de 01..07 y con los datos de 04. Todo termina en ROLLBACK.
-- Uso:  psql -v ON_ERROR_STOP=1 -d alquiler_vehiculos -f 08_pruebas_fase2.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;

CREATE FUNCTION pg_temp.esperar_error(p_sql TEXT, p_fragmento TEXT) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE p_sql;
    EXCEPTION WHEN OTHERS THEN
        IF position(p_fragmento IN SQLERRM) = 0 THEN
            RAISE EXCEPTION 'Error distinto al esperado. Esperaba "%" pero llegó: %', p_fragmento, SQLERRM;
        END IF;
        RAISE NOTICE 'OK  (error esperado) % -> %', p_fragmento, left(SQLERRM, 110);
        RETURN;
    END;
    RAISE EXCEPTION 'Se esperaba un error con "%" pero la sentencia terminó bien: %', p_fragmento, p_sql;
END $$;

DO $$
DECLARE
    v_andres BIGINT;
    v_asesor BIGINT;
    v_veh1 BIGINT; v_veh2 BIGINT;
    v_sede1 BIGINT;
    v_res BIGINT;
    v_estado TEXT;
BEGIN
    SELECT id_cliente  INTO v_andres FROM cliente  WHERE numero_documento = '1020304050';
    SELECT id_empleado INTO v_asesor FROM empleado WHERE numero_documento = '1010000002';

    -- Dos vehículos DISPONIBLES y libres hoy y mañana.
    SELECT id_vehiculo, sede_id_sede INTO v_veh1, v_sede1
      FROM fn_vehiculos_disponibles(current_date, current_date + 1, NULL, NULL)
     WHERE estado = 'DISPONIBLE' ORDER BY id_vehiculo LIMIT 1;
    SELECT id_vehiculo INTO v_veh2
      FROM fn_vehiculos_disponibles(current_date, current_date + 1, NULL, NULL)
     WHERE estado = 'DISPONIBLE' AND id_vehiculo <> v_veh1 ORDER BY id_vehiculo LIMIT 1;
    ASSERT v_veh1 IS NOT NULL AND v_veh2 IS NOT NULL, 'Se necesitan dos vehículos disponibles en los datos de prueba';

    RAISE NOTICE '--- 1. Credenciales y correo único entre cliente y empleado';
    PERFORM pg_temp.esperar_error(
        'CALL sp_registrar_cliente(''900'', ''X'', NULL, ''Y'', NULL, ''camilo.pardo@alquiler.com'', NULL, crypt(''Clave123*'', gen_salt(''bf'', 10)))',
        'ya está registrado como empleado');
    PERFORM pg_temp.esperar_error(
        format('UPDATE empleado SET correo = ''andres.gomez@correo.com'' WHERE id_empleado = %s', v_asesor),
        'ya está registrado como cliente');
    PERFORM pg_temp.esperar_error(
        'CALL sp_registrar_cliente(''901'', ''X'', NULL, ''Y'', NULL, ''hash.malo@correo.com'', NULL, ''abc'')',
        'hash BCrypt');
    ASSERT NOT EXISTS (SELECT 1 FROM cliente WHERE password_hash IS NULL), 'Todo cliente debe tener hash';
    ASSERT NOT EXISTS (SELECT 1 FROM empleado WHERE password_hash IS NULL OR correo IS NULL), 'Todo empleado debe tener hash y correo';

    RAISE NOTICE '--- 2. Cliente BLOQUEADO no puede reservar';
    UPDATE cliente SET estado = 'BLOQUEADO' WHERE id_cliente = v_andres;
    PERFORM pg_temp.esperar_error(
        format('SELECT fn_crear_reserva(%s,%s,%s,%s,%s,current_date,current_date+1,NULL)', v_andres, v_veh1, v_asesor, v_sede1, v_sede1),
        'inactivo o bloqueado');
    UPDATE cliente SET estado = 'ACTIVO' WHERE id_cliente = v_andres;

    RAISE NOTICE '--- 3. Reserva sin asesor (cliente que reserva solo en la web)';
    v_res := fn_crear_reserva(v_andres, v_veh1, NULL, v_sede1, v_sede1, current_date, current_date + 1, NULL);
    ASSERT (SELECT asesor_id_empleado FROM reserva WHERE id_reserva = v_res) IS NULL, 'La reserva debe nacer sin asesor';

    RAISE NOTICE '--- 4. La entrega asigna el asesor';
    CALL sp_registrar_pago(v_res, fn_saldo_pendiente(v_res), 'TARJETA', 'APROBADO');
    PERFORM pg_temp.esperar_error(format('CALL sp_iniciar_reserva(%s)', v_res), 'no tiene asesor');
    PERFORM pg_temp.esperar_error(format('CALL sp_iniciar_reserva(%s, 999999)', v_res), 'No existe un asesor activo');
    CALL sp_iniciar_reserva(v_res, v_asesor);
    ASSERT (SELECT asesor_id_empleado FROM reserva WHERE id_reserva = v_res) = v_asesor, 'Debe quedar el asesor que entrega';
    SELECT estado INTO v_estado FROM vehiculo WHERE id_vehiculo = v_veh1;
    ASSERT v_estado = 'ALQUILADO', 'El vehículo debe quedar ALQUILADO';

    RAISE NOTICE '--- 5. Vehículo dado de baja: no se reserva, no aparece y no vuelve atrás';
    CALL sp_actualizar_estado_vehiculo(v_veh2, 'BAJA');
    ASSERT NOT EXISTS (SELECT 1 FROM fn_vehiculos_disponibles(current_date + 3, current_date + 4, NULL, NULL)
                        WHERE id_vehiculo = v_veh2), 'Un vehículo de baja no debe aparecer como disponible';
    PERFORM pg_temp.esperar_error(
        format('SELECT fn_crear_reserva(%s,%s,%s,%s,%s,current_date+3,current_date+4,NULL)', v_andres, v_veh2, v_asesor, v_sede1, v_sede1),
        'dado de baja o vendido');
    PERFORM pg_temp.esperar_error(
        format('CALL sp_actualizar_estado_vehiculo(%s, ''DISPONIBLE'')', v_veh2), 'ya no se puede cambiar');

    RAISE NOTICE '--- 6. Penalización por daños';
    INSERT INTO penalizacion (concepto, monto, reserva_id_reserva) VALUES ('Rayón en la puerta', 150000, v_res);
    ASSERT (SELECT estado_cargo FROM penalizacion WHERE reserva_id_reserva = v_res) = 'PENDIENTE', 'Nace PENDIENTE';
    PERFORM pg_temp.esperar_error(
        format('INSERT INTO penalizacion (concepto, monto, reserva_id_reserva) VALUES (''X'', 0, %s)', v_res),
        'penalizacion_monto_check');

    RAISE NOTICE '--- 7. Estado en catálogos';
    ASSERT NOT EXISTS (SELECT 1 FROM sede WHERE estado <> 'ACTIVO'), 'Las sedes existentes nacen ACTIVO';
    PERFORM pg_temp.esperar_error('UPDATE categoria SET estado = ''BORRADA''', 'ck_categoria_estado');

    RAISE NOTICE '=== PRUEBAS DE FASE 2 PASARON ===';
END $$;

ROLLBACK;

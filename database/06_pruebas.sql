
\set ON_ERROR_STOP on
BEGIN;

-- Ejecuta una sentencia que debe fallar y comprueba que el mensaje contenga un fragmento.
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
v_andres BIGINT; v_laura BIGINT; v_camila BIGINT; v_diego BIGINT;
    v_asesor BIGINT;
    v_sede_chap BIGINT; v_sede_apto BIGINT; v_sede_pob BIGINT;
    v_abc BIGINT; v_def BIGINT; v_ghi BIGINT; v_stu BIGINT; v_vwx BIGINT;
    v_seguros BIGINT[];
    v_res BIGINT; v_res2 BIGINT; v_res3 BIGINT; v_res4 BIGINT;
    v_n INTEGER; v_estado TEXT; v_sede BIGINT; v_costo NUMERIC; v_saldo NUMERIC;
BEGIN
SELECT id_cliente INTO v_andres FROM cliente WHERE numero_documento = '1020304050';
SELECT id_cliente INTO v_laura  FROM cliente WHERE numero_documento = '1030405060';
SELECT id_cliente INTO v_camila FROM cliente WHERE numero_documento = '1040506070';
SELECT id_cliente INTO v_diego  FROM cliente WHERE numero_documento = '1050607080';
SELECT id_empleado INTO v_asesor FROM empleado WHERE numero_documento = '1010000002';
SELECT id_sede INTO v_sede_chap FROM sede WHERE nombre_sede = 'Sede Chapinero';
SELECT id_sede INTO v_sede_apto FROM sede WHERE nombre_sede = 'Sede Aeropuerto';
SELECT id_sede INTO v_sede_pob  FROM sede WHERE nombre_sede = 'Sede El Poblado';
SELECT id_vehiculo INTO v_abc FROM vehiculo WHERE placa = 'ABC123';
SELECT id_vehiculo INTO v_def FROM vehiculo WHERE placa = 'DEF456';
SELECT id_vehiculo INTO v_ghi FROM vehiculo WHERE placa = 'GHI789';
SELECT id_vehiculo INTO v_stu FROM vehiculo WHERE placa = 'STU901';
SELECT id_vehiculo INTO v_vwx FROM vehiculo WHERE placa = 'VWX234';
SELECT array_agg(id_seguro) INTO v_seguros FROM seguro WHERE nombre_seguro IN ('Total', 'Asistencia en ruta');

RAISE NOTICE '--- 1. Cotización: Económico 90.000 + seguros 35.000 + 8.000, 3 días = 399.000';
    v_costo := fn_costo_reserva(v_abc, current_date, current_date + 2, v_seguros);
    ASSERT v_costo = 399000, format('Costo esperado 399000, fue %s', v_costo);
    PERFORM pg_temp.esperar_error(format('SELECT fn_costo_reserva(%s, current_date + 3, current_date, NULL)', v_abc),
                                  'fecha de fin no puede ser anterior');

    RAISE NOTICE '--- 2. Disponibilidad antes de reservar';
    ASSERT EXISTS (SELECT 1 FROM fn_vehiculos_disponibles(current_date, current_date + 2, NULL, v_sede_chap) WHERE id_vehiculo = v_abc),
           'ABC123 debería estar disponible';

    RAISE NOTICE '--- 3. Crear reserva (PENDIENTE, costo pactado y seguros congelados)';
    v_res := fn_crear_reserva(v_andres, v_abc, v_asesor, v_sede_chap, v_sede_apto,
                              current_date, current_date + 2, v_seguros);
SELECT estado_reserva, costo_total INTO v_estado, v_costo FROM reserva WHERE id_reserva = v_res;
ASSERT v_estado = 'PENDIENTE' AND v_costo = 399000, 'La reserva debe quedar PENDIENTE con costo 399000';
    ASSERT (SELECT SUM(costo_contratado) FROM seg_res WHERE reserva_id_reserva = v_res) = 129000,
           'Los seguros contratados deben sumar (35000+8000)*3 = 129000';
ASSERT (SELECT COUNT(*) FROM auditoria_reserva WHERE id_reserva = v_res) = 1, 'Debe haber 1 fila de auditoría';
ASSERT NOT EXISTS (SELECT 1 FROM fn_vehiculos_disponibles(current_date, current_date + 2, NULL, NULL) WHERE id_vehiculo = v_abc),
           'ABC123 ya no debe aparecer como disponible';

    RAISE NOTICE '--- 4. Reglas de creación';
    PERFORM pg_temp.esperar_error(format('SELECT fn_crear_reserva(%s,%s,%s,%s,%s,current_date+1,current_date+1,NULL)', v_laura, v_abc, v_asesor, v_sede_chap, v_sede_chap),
                                  'ya tiene una reserva activa que se cruza');
    PERFORM pg_temp.esperar_error(format('SELECT fn_crear_reserva(%s,%s,%s,%s,%s,current_date+5,current_date+6,NULL)', v_diego, v_def, v_asesor, v_sede_chap, v_sede_chap),
                                  'no tiene licencia de conducción');
    PERFORM pg_temp.esperar_error(format('SELECT fn_crear_reserva(%s,%s,%s,%s,%s,current_date+8,current_date+12,NULL)', v_laura, v_def, v_asesor, v_sede_chap, v_sede_chap),
                                  'La licencia del cliente vence el');
    PERFORM pg_temp.esperar_error(format('SELECT fn_crear_reserva(%s,%s,%s,%s,%s,current_date-1,current_date+1,NULL)', v_andres, v_def, v_asesor, v_sede_chap, v_sede_chap),
                                  'fecha de inicio no puede ser pasada');
    PERFORM pg_temp.esperar_error(format('SELECT fn_crear_reserva(%s,%s,999999,%s,%s,current_date+5,current_date+6,NULL)', v_andres, v_def, v_sede_chap, v_sede_chap),
                                  'No existe un asesor activo');
    PERFORM pg_temp.esperar_error(format('SELECT fn_crear_reserva(%s,%s,%s,%s,%s,current_date+5,current_date+6,ARRAY[999999]::bigint[])', v_andres, v_def, v_asesor, v_sede_chap, v_sede_chap),
                                  'seguros indicados no existe');

    RAISE NOTICE '--- 5. Pagos: límites y confirmación';
    PERFORM pg_temp.esperar_error(format('CALL sp_registrar_pago(%s, 500000, ''TARJETA'', ''APROBADO'')', v_res), 'excede el saldo pendiente');
    PERFORM pg_temp.esperar_error(format('CALL sp_registrar_pago(%s, 1000, ''TARJETA'', ''REEMBOLSADO'')', v_res), 'APROBADO o RECHAZADO');
    PERFORM pg_temp.esperar_error(format('CALL sp_iniciar_reserva(%s)', v_res), 'Solo se puede iniciar una reserva CONFIRMADA');
CALL sp_registrar_pago(v_res, 200000, 'TARJETA', 'APROBADO');
SELECT estado_reserva INTO v_estado FROM reserva WHERE id_reserva = v_res;
ASSERT v_estado = 'CONFIRMADA', 'Un pago aprobado debe confirmar la reserva';
    ASSERT fn_saldo_pendiente(v_res) = 199000, 'Saldo esperado 199000';

    RAISE NOTICE '--- 6. Entrega: EN_CURSO deja el vehículo ALQUILADO';
CALL sp_iniciar_reserva(v_res);
ASSERT (SELECT estado FROM vehiculo WHERE id_vehiculo = v_abc) = 'ALQUILADO', 'El trigger debe dejar el vehículo ALQUILADO';
PERFORM pg_temp.esperar_error(format('CALL sp_cancelar_reserva(%s)', v_res), 'Solo se puede cancelar una reserva PENDIENTE o CONFIRMADA');
    PERFORM pg_temp.esperar_error(format('CALL sp_actualizar_estado_vehiculo(%s, ''EN_MANTENIMIENTO'')', v_abc), 'está alquilado');
    PERFORM pg_temp.esperar_error(format('CALL sp_actualizar_estado_vehiculo(%s, ''ALQUILADO'')', v_def), 'solo se asigna al iniciar una reserva');

    RAISE NOTICE '--- 7. Devolución: exige saldo en cero y mueve el vehículo a la sede de recibo';
    PERFORM pg_temp.esperar_error(format('CALL sp_finalizar_reserva(%s)', v_res), 'saldo pendiente');
CALL sp_registrar_pago(v_res, 199000, 'EFECTIVO', 'APROBADO');
CALL sp_finalizar_reserva(v_res);
SELECT estado, sede_id_sede INTO v_estado, v_sede FROM vehiculo WHERE id_vehiculo = v_abc;
ASSERT v_estado = 'DISPONIBLE' AND v_sede = v_sede_apto, 'El vehículo debe quedar DISPONIBLE en la sede de recibo';
    ASSERT (SELECT COUNT(*) FROM auditoria_reserva WHERE id_reserva = v_res) = 4, 'Auditoría: crear, confirmar, iniciar, finalizar';
PERFORM pg_temp.esperar_error(format('UPDATE reserva SET estado_reserva = ''PENDIENTE'' WHERE id_reserva = %s', v_res),
                                  'Transición de reserva no permitida');
    PERFORM pg_temp.esperar_error(format('CALL sp_registrar_pago(%s, 1000, ''TARJETA'', ''APROBADO'')', v_res), 'FINALIZADA');

    RAISE NOTICE '--- 8. Cancelación reembolsa los pagos aprobados';
    v_res2 := fn_crear_reserva(v_andres, v_ghi, v_asesor, v_sede_chap, v_sede_chap, current_date + 20, current_date + 22, NULL);
CALL sp_registrar_pago(v_res2, 100000, 'TARJETA', 'APROBADO');
CALL sp_cancelar_reserva(v_res2);
ASSERT (SELECT estado_reserva FROM reserva WHERE id_reserva = v_res2) = 'CANCELADA', 'Debe quedar CANCELADA';
ASSERT (SELECT estado_pago FROM pago WHERE reserva_id_reserva = v_res2) = 'REEMBOLSADO', 'El pago debe quedar REEMBOLSADO';
v_res3 := fn_crear_reserva(v_camila, v_ghi, v_asesor, v_sede_chap, v_sede_chap, current_date + 20, current_date + 22, NULL);

    RAISE NOTICE '--- 9. Pago rechazado sobre reserva PENDIENTE la cancela';
    v_res4 := fn_crear_reserva(v_camila, v_stu, v_asesor, v_sede_pob, v_sede_pob, current_date + 30, current_date + 31, NULL);
CALL sp_registrar_pago(v_res4, 50000, 'TARJETA', 'RECHAZADO');
ASSERT (SELECT estado_reserva FROM reserva WHERE id_reserva = v_res4) = 'CANCELADA', 'Debe quedar CANCELADA';
ASSERT (SELECT estado FROM vehiculo WHERE id_vehiculo = v_stu) = 'DISPONIBLE', 'El vehículo sigue DISPONIBLE';

RAISE NOTICE '--- 10. Mantenimiento bloquea reservas';
CALL sp_actualizar_estado_vehiculo(v_def, 'EN_MANTENIMIENTO');
PERFORM pg_temp.esperar_error(format('SELECT fn_crear_reserva(%s,%s,%s,%s,%s,current_date+5,current_date+6,NULL)', v_andres, v_def, v_asesor, v_sede_chap, v_sede_chap),
                                  'está en mantenimiento');
    ASSERT NOT EXISTS (SELECT 1 FROM fn_vehiculos_disponibles(current_date + 5, current_date + 6, NULL, NULL) WHERE id_vehiculo = v_def),
           'Un vehículo en mantenimiento no debe aparecer como disponible';
CALL sp_actualizar_estado_vehiculo(v_def, 'DISPONIBLE');

RAISE NOTICE '--- 11. La restricción EXCLUDE frena el solapamiento aunque se escriba directo (carrera entre asesores)';
    v_res := fn_crear_reserva(v_andres, v_vwx, v_asesor, v_sede_pob, v_sede_pob, current_date + 40, current_date + 45, NULL);
    PERFORM pg_temp.esperar_error(
        format('INSERT INTO reserva (fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe) VALUES (current_date+43, current_date+47, 1, %s, %s, %s, %s, %s)',
               v_andres, v_vwx, v_asesor, v_sede_pob, v_sede_pob),
        'ex_reserva_sin_solapamiento');

    RAISE NOTICE '--- 12. Registro de clientes, licencias y vehículos';
    PERFORM pg_temp.esperar_error('CALL sp_registrar_cliente(''1020304050'', ''X'', NULL, ''Y'', NULL, ''otro@correo.com'', NULL, crypt(''Clave123*'', gen_salt(''bf'', 10)))', 'Ya existe un cliente con el documento');
    PERFORM pg_temp.esperar_error('CALL sp_registrar_cliente(''999'', ''X'', NULL, ''Y'', NULL, ''ANDRES.GOMEZ@correo.com'', NULL, crypt(''Clave123*'', gen_salt(''bf'', 10)))', 'Ya existe un cliente con el correo');
    PERFORM pg_temp.esperar_error(format('CALL sp_registrar_licencia(%s, ''LIC-9999'', ''B1'', current_date-10, current_date+100)', v_andres), 'ya tiene una licencia');
CALL sp_registrar_cliente('777', 'Nuevo', NULL, 'Cliente', NULL, 'nuevo@correo.com', NULL, crypt('Clave123*', gen_salt('bf', 10)));
PERFORM pg_temp.esperar_error(format('CALL sp_registrar_licencia(%s, ''LIC-V'', ''B1'', current_date-400, current_date-5)', (SELECT id_cliente FROM cliente WHERE numero_documento = '777')), 'vencida');
    PERFORM pg_temp.esperar_error(format('CALL sp_registrar_vehiculo(''ABC123'', ''X'', 2020, 0, NULL, 1, %s)', v_sede_chap), 'Ya existe un vehículo con la placa');
    -- Se llenan los cupos que queden libres en El Poblado (no depende de cuántos vehículos haya cargados).
FOR v_n IN 1..((SELECT capacidad_estacionamiento FROM sede WHERE id_sede = v_sede_pob)
                   - (SELECT count(*) FROM vehiculo WHERE sede_id_sede = v_sede_pob)) LOOP
        CALL sp_registrar_vehiculo('ZZZ00' || v_n, 'Genérico', 2022, 100, 'Gris', 1, v_sede_pob);
END LOOP;
    PERFORM pg_temp.esperar_error(format('CALL sp_registrar_vehiculo(''ZZZ009'', ''Genérico'', 2022, 100, ''Gris'', 1, %s)', v_sede_pob), 'no tiene cupo');

    RAISE NOTICE '--- 13. Especialización de empleado: disjunta y total';
    PERFORM pg_temp.esperar_error(format('INSERT INTO tecnico (id_empleado, especialidad) VALUES (%s, ''Frenos'')', v_asesor), 'ya pertenece a otro subtipo');
    PERFORM pg_temp.esperar_error(
        format('INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, correo, password_hash, sede_id_sede) VALUES (''HUERFANO'', ''A'', ''B'', current_date, ''huerfano@alquiler.com'', crypt(''x'', gen_salt(''bf'', 4)), %s); SET CONSTRAINTS trg_empleado_total IMMEDIATE', v_sede_chap),
        'especialización total');

    RAISE NOTICE '=== TODAS LAS PRUEBAS FUNCIONALES PASARON ===';
END $$;

-- ---------- Seguridad por roles
SET LOCAL ROLE rol_asesor;
DO $$
BEGIN
    PERFORM count(*) FROM vehiculo;
BEGIN
EXECUTE 'DELETE FROM vehiculo';
RAISE EXCEPTION 'El asesor pudo borrar vehículos';
EXCEPTION WHEN insufficient_privilege THEN
        RAISE NOTICE 'OK  asesor sin DELETE sobre vehiculo: %', SQLERRM;
END;
BEGIN
EXECUTE 'CALL sp_registrar_vehiculo(''SEC001'', ''X'', 2020, 0, NULL, 1, 1)';
RAISE EXCEPTION 'El asesor pudo registrar un vehículo';
EXCEPTION WHEN insufficient_privilege THEN
        RAISE NOTICE 'OK  asesor sin EXECUTE de sp_registrar_vehiculo: %', SQLERRM;
END;
BEGIN
EXECUTE 'SELECT * FROM auditoria_reserva';
RAISE EXCEPTION 'El asesor pudo leer la auditoría';
EXCEPTION WHEN insufficient_privilege THEN
        RAISE NOTICE 'OK  asesor sin acceso a auditoria_reserva: %', SQLERRM;
END;
CALL sp_registrar_cliente('888', 'Prueba', NULL, 'Rol', NULL, 'prueba.rol@correo.com', NULL, crypt('Clave123*', gen_salt('bf', 10)));
RAISE NOTICE 'OK  asesor registró un cliente solo a través del procedimiento';
END $$;
RESET ROLE;

SET LOCAL ROLE rol_tecnico;
DO $$
DECLARE
v_id BIGINT;
BEGIN
BEGIN
EXECUTE 'SELECT * FROM cliente';
RAISE EXCEPTION 'El técnico pudo leer clientes';
EXCEPTION WHEN insufficient_privilege THEN
        RAISE NOTICE 'OK  técnico sin acceso a cliente: %', SQLERRM;
END;
SELECT id_vehiculo INTO v_id FROM vehiculo WHERE placa = 'DEF456';
CALL sp_actualizar_estado_vehiculo(v_id, 'EN_MANTENIMIENTO');
RAISE NOTICE 'OK  técnico puso un vehículo en mantenimiento';
END $$;
RESET ROLE;

-- Demostración explícita de ROLLBACK: nada de lo anterior queda guardado.
ROLLBACK;
SELECT 'Tras el ROLLBACK quedan' AS resultado, count(*) AS reservas, (SELECT count(*) FROM cliente) AS clientes FROM reserva;


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
        RAISE NOTICE '--- 6b. fn_registrar_penalizacion y cambio de estado';
    DECLARE
v_pen  BIGINT;
        v_pen2 BIGINT;
        v_pend BIGINT;
BEGIN
        v_pen := fn_registrar_penalizacion(v_res, 'Golpe en el parachoques', 200000, '0123456789abcdef01234567');
        ASSERT (SELECT estado_cargo FROM penalizacion WHERE id_penalizacion = v_pen) = 'PENDIENTE', 'Nace PENDIENTE';
ASSERT (SELECT id_inspeccion_mongo FROM penalizacion WHERE id_penalizacion = v_pen) = '0123456789abcdef01234567',
               'Debe guardar el id de la inspección';

PERFORM pg_temp.esperar_error(
            format('SELECT fn_registrar_penalizacion(%s, ''X'', 0, NULL)', v_res), 'monto de la penalización');
        PERFORM pg_temp.esperar_error(
            format('SELECT fn_registrar_penalizacion(%s, ''   '', 1000, NULL)', v_res), 'concepto');
        PERFORM pg_temp.esperar_error(
            format('SELECT fn_registrar_penalizacion(%s, ''X'', 1000, ''abc'')', v_res), 'identificador de inspección');
        PERFORM pg_temp.esperar_error(
            'SELECT fn_registrar_penalizacion(999999, ''X'', 1000, NULL)', 'No existe la reserva');

        -- Una reserva PENDIENTE no se penaliza.
        v_pend := fn_crear_reserva(v_andres, v_veh1, v_asesor, v_sede1, v_sede1, current_date + 10, current_date + 11, NULL);
        PERFORM pg_temp.esperar_error(
            format('SELECT fn_registrar_penalizacion(%s, ''X'', 1000, NULL)', v_pend), 'EN_CURSO o FINALIZADA');

        -- Cambios de estado.
        v_pen2 := fn_registrar_penalizacion(v_res, 'Tapicería manchada', 80000, NULL);
        PERFORM pg_temp.esperar_error(
            format('CALL sp_cambiar_estado_penalizacion(%s, ''PENDIENTE'')', v_pen2), 'Estado no válido');
        PERFORM pg_temp.esperar_error(
            format('UPDATE penalizacion SET monto = 1 WHERE id_penalizacion = %s', v_pen2), 'solo se puede modificar el estado');
CALL sp_cambiar_estado_penalizacion(v_pen2, 'PAGADA');
ASSERT (SELECT estado_cargo FROM penalizacion WHERE id_penalizacion = v_pen2) = 'PAGADA', 'Debe quedar PAGADA';
PERFORM pg_temp.esperar_error(
            format('CALL sp_cambiar_estado_penalizacion(%s, ''ANULADA'')', v_pen2), 'no se puede cambiar');
END;

    RAISE NOTICE '--- 7. Estado en catálogos';
    ASSERT (SELECT estado FROM sede WHERE nombre_sede = 'Sede Chapinero') = 'ACTIVO', 'Las sedes nuevas nacen ACTIVO';
PERFORM pg_temp.esperar_error('UPDATE categoria SET estado = ''BORRADA''', 'ck_categoria_estado');

    RAISE NOTICE '--- 8. sp_cambiar_clave';
CALL sp_cambiar_clave('CLIENTE', v_andres, crypt('Nueva12345', gen_salt('bf', 10)), TRUE);
ASSERT (SELECT debe_cambiar_clave FROM cliente WHERE id_cliente = v_andres), 'Debe quedar marcada como temporal';
CALL sp_cambiar_clave('CLIENTE', v_andres, crypt('Nueva12345', gen_salt('bf', 10)), FALSE);
ASSERT NOT (SELECT debe_cambiar_clave FROM cliente WHERE id_cliente = v_andres), 'Ya no debe ser temporal';
    PERFORM pg_temp.esperar_error('CALL sp_cambiar_clave(''ROBOT'', 1, crypt(''x'', gen_salt(''bf'', 4)), FALSE)', 'Tipo de cuenta no válido');
    PERFORM pg_temp.esperar_error('CALL sp_cambiar_clave(''CLIENTE'', 999999, crypt(''x'', gen_salt(''bf'', 4)), FALSE)', 'No existe la cuenta');
    PERFORM pg_temp.esperar_error('CALL sp_cambiar_clave(''CLIENTE'', 1, ''abc'', FALSE)', 'hash BCrypt');

    RAISE NOTICE '--- 9. Catálogos inactivos';
    DECLARE
v_seg BIGINT;
        v_tmp BIGINT;
BEGIN
        -- Seguro inactivo: no se cotiza ni se contrata.
SELECT id_seguro INTO v_seg FROM seguro ORDER BY id_seguro LIMIT 1;
UPDATE seguro SET estado = 'INACTIVO' WHERE id_seguro = v_seg;
PERFORM pg_temp.esperar_error(
            format('SELECT fn_costo_reserva(%s, current_date+10, current_date+11, ARRAY[%s]::bigint[])', v_veh1, v_seg),
            'seguros indicados está inactivo');

        -- Sede con vehículos no se puede desactivar.
        PERFORM pg_temp.esperar_error(
            format('UPDATE sede SET estado = ''INACTIVO'' WHERE id_sede = %s', v_sede1),
            'tiene vehículos asignados');

        -- Sede vacía sí; pero ya no admite vehículos nuevos.
INSERT INTO sede (nombre_sede, ciudad, direccion, capacidad_estacionamiento)
VALUES ('Sede Temporal', 'Bogotá', 'Cl 1 # 1-1', 2) RETURNING id_sede INTO v_tmp;
UPDATE sede SET estado = 'INACTIVO' WHERE id_sede = v_tmp;
PERFORM pg_temp.esperar_error(
            format('CALL sp_registrar_vehiculo(''TMP001'', ''X'', 2020, 0, NULL, 1, %s)', v_tmp),
            'está inactiva');

        -- Categoría inactiva: sus vehículos dejan de ofrecerse y de cotizarse.
        ASSERT EXISTS (SELECT 1 FROM fn_vehiculos_disponibles(current_date + 30, current_date + 31, NULL, NULL)
                        WHERE id_vehiculo = v_veh1), 'Antes de desactivar debe aparecer';
UPDATE categoria SET estado = 'INACTIVO'
WHERE id_categoria = (SELECT categoria_id_categoria FROM vehiculo WHERE id_vehiculo = v_veh1);
ASSERT NOT EXISTS (SELECT 1 FROM fn_vehiculos_disponibles(current_date + 30, current_date + 31, NULL, NULL)
                            WHERE id_vehiculo = v_veh1), 'Con la categoría inactiva no debe aparecer';
        PERFORM pg_temp.esperar_error(
            format('SELECT fn_costo_reserva(%s, current_date+30, current_date+31, NULL)', v_veh1),
            'categoría del vehículo');
END;

    RAISE NOTICE '--- 10. sp_actualizar_kilometraje_vehiculo';
    DECLARE
v_km INTEGER;
BEGIN
SELECT kilometraje INTO v_km FROM vehiculo WHERE id_vehiculo = v_veh1;
CALL sp_actualizar_kilometraje_vehiculo(v_veh1, v_km + 150);
ASSERT (SELECT kilometraje FROM vehiculo WHERE id_vehiculo = v_veh1) = v_km + 150, 'Debe subir el kilometraje';
PERFORM pg_temp.esperar_error(
            format('CALL sp_actualizar_kilometraje_vehiculo(%s, %s)', v_veh1, v_km), 'menor que el actual');
        PERFORM pg_temp.esperar_error(
            'CALL sp_actualizar_kilometraje_vehiculo(999999, 10)', 'No existe el vehículo');
END;
    RAISE NOTICE '=== PRUEBAS DE FASE 2 PASARON ===';
END $$;

ROLLBACK;

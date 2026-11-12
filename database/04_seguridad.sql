
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

-- Punto de partida: nadie (PUBLIC) tiene nada sobre nuestras tablas ni rutinas.
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;
DO $$
DECLARE
r RECORD;
BEGIN
    -- Solo rutinas propias
FOR r IN
SELECT p.oid::regprocedure::text AS firma, p.prokind
FROM pg_proc p
WHERE p.pronamespace = 'public'::regnamespace
           AND p.prokind IN ('f', 'p')
           AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = p.oid AND d.deptype = 'e')
    LOOP
        EXECUTE format('REVOKE ALL ON %s %s FROM PUBLIC',
                       CASE r.prokind WHEN 'p' THEN 'PROCEDURE' ELSE 'FUNCTION' END, r.firma);
END LOOP;
END $$;

GRANT USAGE ON SCHEMA public TO rol_asesor, rol_tecnico, rol_administrador;

-- ---------- ASESOR: atiende clientes y gestiona reservas y pagos ----------
GRANT SELECT ON sede, categoria, seguro, cliente, licencia, vehiculo, reserva, pago, seg_res, penalizacion TO rol_asesor;
GRANT EXECUTE ON FUNCTION  fn_costo_reserva(BIGINT, DATE, DATE, BIGINT[])                         TO rol_asesor;
GRANT EXECUTE ON FUNCTION  fn_saldo_pendiente(BIGINT)                                             TO rol_asesor;
GRANT EXECUTE ON FUNCTION  fn_vehiculos_disponibles(DATE, DATE, BIGINT, BIGINT)                   TO rol_asesor;
GRANT EXECUTE ON FUNCTION  fn_crear_reserva(BIGINT, BIGINT, BIGINT, BIGINT, BIGINT, DATE, DATE, BIGINT[]) TO rol_asesor;
GRANT EXECUTE ON PROCEDURE sp_registrar_cliente(VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR) TO rol_asesor;
GRANT EXECUTE ON PROCEDURE sp_registrar_licencia(BIGINT, VARCHAR, VARCHAR, DATE, DATE)            TO rol_asesor;
GRANT EXECUTE ON PROCEDURE sp_registrar_pago(BIGINT, NUMERIC, VARCHAR, VARCHAR)                   TO rol_asesor;
GRANT EXECUTE ON PROCEDURE sp_iniciar_reserva(BIGINT)                                             TO rol_asesor;
GRANT EXECUTE ON PROCEDURE sp_iniciar_reserva(BIGINT, BIGINT)                                     TO rol_asesor;
GRANT EXECUTE ON FUNCTION  fn_registrar_penalizacion(BIGINT, VARCHAR, NUMERIC, VARCHAR)           TO rol_asesor;
GRANT EXECUTE ON PROCEDURE sp_finalizar_reserva(BIGINT)                                           TO rol_asesor;
GRANT EXECUTE ON PROCEDURE sp_cancelar_reserva(BIGINT)                                            TO rol_asesor;

-- ---------- TÉCNICO: consulta vehículos y los pone/saca de mantenimiento ----------
GRANT SELECT ON sede, categoria, vehiculo TO rol_tecnico;
GRANT EXECUTE ON PROCEDURE sp_actualizar_estado_vehiculo(BIGINT, VARCHAR) TO rol_tecnico;

-- ---------- Todos los roles del personal: cambiar clave y actualizar el kilometraje ----------
GRANT EXECUTE ON PROCEDURE sp_cambiar_clave(VARCHAR, BIGINT, VARCHAR, BOOLEAN)  TO rol_asesor, rol_tecnico, rol_administrador;
GRANT EXECUTE ON PROCEDURE sp_actualizar_kilometraje_vehiculo(BIGINT, INTEGER)  TO rol_asesor, rol_tecnico, rol_administrador;

-- ---------- ADMINISTRADOR: todo, incluida la auditoría ----------
GRANT ALL ON ALL TABLES IN SCHEMA public TO rol_administrador;
DO $$
DECLARE
r RECORD;
BEGIN
FOR r IN
SELECT p.oid::regprocedure::text AS firma, p.prokind
FROM pg_proc p
WHERE p.pronamespace = 'public'::regnamespace
           AND p.prokind IN ('f', 'p')
           AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = p.oid AND d.deptype = 'e')
    LOOP
        EXECUTE format('GRANT EXECUTE ON %s %s TO rol_administrador',
                       CASE r.prokind WHEN 'p' THEN 'PROCEDURE' ELSE 'FUNCTION' END, r.firma);
END LOOP;
END $$;

-- Los enlaces de recuperación de contraseña los maneja solo el usuario del backend (el dueño):
-- ni siquiera el administrador necesita ver esa tabla.
REVOKE ALL ON token_recuperacion FROM rol_administrador;

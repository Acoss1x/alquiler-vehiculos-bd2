-- =====================================================================
-- 04_datos_prueba.sql : datos de ejemplo. Corre DESPUÉS de 01, 02 y 03.
-- Se usan los procedimientos (no INSERT directos) donde existen, para
-- ejercitar también las validaciones. Las fechas son relativas a hoy.
-- =====================================================================

-- ---------- Catálogos ----------
INSERT INTO sede (nombre_sede, ciudad, direccion, capacidad_estacionamiento) VALUES
    ('Sede Chapinero',   'Bogotá',   'Cra 13 # 60-25',   10),
    ('Sede Aeropuerto',  'Bogotá',   'Av. El Dorado # 103-9', 8),
    ('Sede El Poblado',  'Medellín', 'Cl 10 # 43E-31',   6);

INSERT INTO categoria (nombre_categoria, capacidad_pasajeros, tarifa_diaria, tipo_transmision) VALUES
    ('Económico',  4,  90000.00, 'MANUAL'),
    ('Compacto',   5, 120000.00, 'AUTOMATICA'),
    ('SUV',        7, 220000.00, 'AUTOMATICA'),
    ('Van',       12, 300000.00, 'MANUAL');

INSERT INTO seguro (nombre_seguro, descripcion, costo_diario) VALUES
    ('Básico',               'Daños a terceros',                      15000.00),
    ('Total',                'Cobertura total con deducible bajo',    35000.00),
    ('Asistencia en ruta',   'Grúa y asistencia mecánica 24/7',        8000.00);

-- ---------- Empleados (empleado + subtipo en la MISMA transacción: la
-- especialización total se verifica al COMMIT) ----------
BEGIN;
WITH e AS (
    INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, sede_id_sede)
    VALUES ('1010000001', 'Marta', 'Ríos', DATE '2022-03-01', 'ACTIVO', 'marta.rios@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)),
            (SELECT id_sede FROM sede WHERE nombre_sede = 'Sede Chapinero'))
    RETURNING id_empleado)
INSERT INTO administrador (id_empleado, nivel_acceso, bono_gerencial)
SELECT id_empleado, 'TOTAL', 500000.00 FROM e;

WITH e AS (
    INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, sede_id_sede)
    VALUES ('1010000002', 'Camilo', 'Pardo', DATE '2023-01-15', 'ACTIVO', 'camilo.pardo@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)),
            (SELECT id_sede FROM sede WHERE nombre_sede = 'Sede Chapinero'))
    RETURNING id_empleado)
INSERT INTO asesor (id_empleado, mostrador_asignado) SELECT id_empleado, 1 FROM e;

WITH e AS (
    INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, sede_id_sede)
    VALUES ('1010000003', 'Valentina', 'Mora', DATE '2023-06-10', 'ACTIVO', 'valentina.mora@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)),
            (SELECT id_sede FROM sede WHERE nombre_sede = 'Sede Aeropuerto'))
    RETURNING id_empleado)
INSERT INTO asesor (id_empleado, mostrador_asignado) SELECT id_empleado, 2 FROM e;

WITH e AS (
    INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, sede_id_sede)
    VALUES ('1010000004', 'Julián', 'Cortés', DATE '2021-09-20', 'ACTIVO', 'julian.cortes@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)),
            (SELECT id_sede FROM sede WHERE nombre_sede = 'Sede Chapinero'))
    RETURNING id_empleado)
INSERT INTO tecnico (id_empleado, especialidad) SELECT id_empleado, 'Frenos y suspensión' FROM e;

WITH e AS (
    INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, sede_id_sede)
    VALUES ('1010000005', 'Sofía', 'Lara', DATE '2024-02-05', 'ACTIVO', 'sofia.lara@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)),
            (SELECT id_sede FROM sede WHERE nombre_sede = 'Sede El Poblado'))
    RETURNING id_empleado)
INSERT INTO tecnico (id_empleado, especialidad) SELECT id_empleado, 'Motor y electrónica' FROM e;
COMMIT;

-- ---------- Clientes y licencias (vía procedimientos) ----------
CALL sp_registrar_cliente('1020304050', 'Andrés',  'Felipe', 'Gómez',   'Ruiz',   'andres.gomez@correo.com',  '3001112233', crypt('Clave123*', gen_salt('bf', 10)));
CALL sp_registrar_cliente('1030405060', 'Laura',   NULL,     'Peña',    NULL,     'laura.pena@correo.com',    '3012223344', crypt('Clave123*', gen_salt('bf', 10)));
CALL sp_registrar_cliente('1040506070', 'Camila',  'Andrea', 'Torres',  'Vega',   'camila.torres@correo.com', '3023334455', crypt('Clave123*', gen_salt('bf', 10)));
CALL sp_registrar_cliente('1050607080', 'Diego',   NULL,     'Herrera', NULL,     'diego.herrera@correo.com', NULL, crypt('Clave123*', gen_salt('bf', 10)));

-- (CALL no admite subconsultas como argumento: se usa un DO con un bucle.)
-- Laura (1030405060) tiene una licencia que vence pronto: sirve para probar que no
-- se reserva más allá de su vencimiento. Diego (1050607080) queda SIN licencia a
-- propósito, para probar que no puede reservar.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.id_cliente, l.numero, l.categoria, l.expedicion, l.vencimiento
          FROM (VALUES
                ('1020304050', 'LIC-0001', 'B1', current_date - 1500, current_date + 1500),
                ('1030405060', 'LIC-0002', 'B1', current_date - 900,  current_date + 10),
                ('1040506070', 'LIC-0003', 'C1', current_date - 700,  current_date + 2000)
               ) AS l (documento, numero, categoria, expedicion, vencimiento)
          JOIN cliente c ON c.numero_documento = l.documento
    LOOP
        CALL sp_registrar_licencia(r.id_cliente, r.numero, r.categoria, r.expedicion, r.vencimiento);
    END LOOP;
END $$;

-- ---------- Vehículos (vía procedimiento: valida cupo de la sede) ----------
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT v.placa, v.marca, v.anio, v.km, v.color, c.id_categoria, s.id_sede
          FROM (VALUES
                ('ABC123', 'Chevrolet Spark',  2022, 18000, 'Rojo',   'Económico', 'Sede Chapinero'),
                ('DEF456', 'Renault Logan',    2021, 42000, 'Blanco', 'Económico', 'Sede Chapinero'),
                ('GHI789', 'Mazda 2',          2023,  9000, 'Gris',   'Compacto',  'Sede Chapinero'),
                ('JKL012', 'Kia Sportage',     2023, 12000, 'Negro',  'SUV',       'Sede Aeropuerto'),
                ('MNO345', 'Toyota Fortuner',  2022, 30000, 'Plata',  'SUV',       'Sede Aeropuerto'),
                ('PQR678', 'Hyundai H1',       2020, 65000, 'Blanco', 'Van',       'Sede El Poblado'),
                ('STU901', 'Mazda 3',          2024,  3000, 'Azul',   'Compacto',  'Sede El Poblado'),
                ('VWX234', 'Renault Duster',   2022, 27000, 'Verde',  'SUV',       'Sede El Poblado')
               ) AS v (placa, marca, anio, km, color, categoria, sede)
          JOIN categoria c ON c.nombre_categoria = v.categoria
          JOIN sede s      ON s.nombre_sede      = v.sede
    LOOP
        CALL sp_registrar_vehiculo(r.placa, r.marca, r.anio, r.km, r.color, r.id_categoria, r.id_sede);
    END LOOP;
END $$;

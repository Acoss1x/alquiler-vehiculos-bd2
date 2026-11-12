
\set ON_ERROR_STOP on
BEGIN;

TRUNCATE TABLE auditoria_reserva, penalizacion, pago, seg_res, reserva, vehiculo, licencia,
               cliente, administrador, asesor, tecnico, empleado, seguro, categoria, sede
    RESTART IDENTITY CASCADE;

-- ---------- Catálogos ----------
INSERT INTO sede (nombre_sede, ciudad, direccion, capacidad_estacionamiento) VALUES
                                                                                 ('Sede Chapinero', 'Bogotá', 'Cra 13 # 60-25', 10),
                                                                                 ('Sede Aeropuerto', 'Bogotá', 'Av. El Dorado # 103-9', 8),
                                                                                 ('Sede El Poblado', 'Medellín', 'Cl 10 # 43E-31', 6),
                                                                                 ('Sede Bocagrande', 'Cartagena', 'Cra 2 # 11-52', 5),
                                                                                 ('Sede Usaquén', 'Bogotá', 'Cra 7 # 119-14', 4);
INSERT INTO categoria (nombre_categoria, capacidad_pasajeros, tarifa_diaria, tipo_transmision) VALUES
                                                                                                   ('Económico', 4, 90000.00, 'MANUAL'),
                                                                                                   ('Compacto', 5, 120000.00, 'AUTOMATICA'),
                                                                                                   ('SUV', 7, 220000.00, 'AUTOMATICA'),
                                                                                                   ('Van', 12, 300000.00, 'MANUAL'),
                                                                                                   ('Premium', 5, 380000.00, 'AUTOMATICA'),
                                                                                                   ('Pickup', 5, 260000.00, 'MANUAL'),
                                                                                                   ('Deportivo', 2, 520000.00, 'AUTOMATICA');
INSERT INTO seguro (nombre_seguro, descripcion, costo_diario) VALUES
                                                                  ('Básico', 'Daños a terceros', 15000.00),
                                                                  ('Total', 'Cobertura total con deducible bajo', 35000.00),
                                                                  ('Asistencia en ruta', 'Grúa y asistencia mecánica 24/7', 8000.00),
                                                                  ('Conductor adicional', 'Autoriza a un segundo conductor registrado', 12000.00),
                                                                  ('Llantas y vidrios', 'Cobertura de llantas, rines y vidrios', 10000.00),
                                                                  ('Premium Plus (descontinuado)', 'Producto retirado del catálogo; se conserva el histórico', 45000.00);

-- ---------- Empleados (empleado + subtipo en la misma transacción) ----------
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000001', 'Marta', 'Ríos', DATE '2022-03-01', 'ACTIVO', 'marta.rios@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 1)
    RETURNING id_empleado)
INSERT INTO administrador (id_empleado, nivel_acceso, bono_gerencial) SELECT id_empleado, 'TOTAL', 500000.00 FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000002', 'Camilo', 'Pardo', DATE '2023-01-15', 'ACTIVO', 'camilo.pardo@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 1)
    RETURNING id_empleado)
INSERT INTO asesor (id_empleado, mostrador_asignado) SELECT id_empleado, 1 FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000003', 'Valentina', 'Mora', DATE '2023-06-10', 'ACTIVO', 'valentina.mora@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 2)
    RETURNING id_empleado)
INSERT INTO asesor (id_empleado, mostrador_asignado) SELECT id_empleado, 2 FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000004', 'Julián', 'Cortés', DATE '2021-09-20', 'ACTIVO', 'julian.cortes@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 1)
    RETURNING id_empleado)
INSERT INTO tecnico (id_empleado, especialidad) SELECT id_empleado, 'Frenos y suspensión' FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000005', 'Sofía', 'Lara', DATE '2024-02-05', 'ACTIVO', 'sofia.lara@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 3)
    RETURNING id_empleado)
INSERT INTO tecnico (id_empleado, especialidad) SELECT id_empleado, 'Motor y electrónica' FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000006', 'Ricardo', 'Salazar', DATE '2022-08-16', 'ACTIVO', 'ricardo.salazar@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 3)
    RETURNING id_empleado)
INSERT INTO administrador (id_empleado, nivel_acceso, bono_gerencial) SELECT id_empleado, 'REGIONAL', NULL FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000007', 'Daniela', 'Restrepo', DATE '2024-05-06', 'ACTIVO', 'daniela.restrepo@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 3)
    RETURNING id_empleado)
INSERT INTO asesor (id_empleado, mostrador_asignado) SELECT id_empleado, 1 FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000008', 'Mateo', 'Giraldo', DATE '2024-07-01', 'ACTIVO', 'mateo.giraldo@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 4)
    RETURNING id_empleado)
INSERT INTO asesor (id_empleado, mostrador_asignado) SELECT id_empleado, 1 FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000009', 'Paola', 'Duarte', current_date - 3, 'ACTIVO', 'paola.duarte@alquiler.com', crypt('Temporal123*', gen_salt('bf', 10)), TRUE, 2)
    RETURNING id_empleado)
INSERT INTO asesor (id_empleado, mostrador_asignado) SELECT id_empleado, 3 FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000010', 'Esteban', 'Cárdenas', DATE '2023-02-13', 'ACTIVO', 'esteban.cardenas@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 2)
    RETURNING id_empleado)
INSERT INTO tecnico (id_empleado, especialidad) SELECT id_empleado, 'Electricidad y diagnóstico' FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000011', 'Andrea', 'Salcedo', DATE '2024-09-02', 'ACTIVO', 'andrea.salcedo@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 4)
    RETURNING id_empleado)
INSERT INTO tecnico (id_empleado, especialidad) SELECT id_empleado, 'Latonería y pintura' FROM e;
WITH e AS (INSERT INTO empleado (numero_documento, primer_nombre, primer_apellido, fecha_contratacion, estado, correo, password_hash, debe_cambiar_clave, sede_id_sede)
VALUES ('1010000012', 'Hernán', 'Quintero', DATE '2020-04-20', 'INACTIVO', 'hernan.quintero@alquiler.com', crypt('Clave123*', gen_salt('bf', 10)), FALSE, 5)
    RETURNING id_empleado)
INSERT INTO asesor (id_empleado, mostrador_asignado) SELECT id_empleado, 3 FROM e;

-- ---------- Clientes y licencias ----------
INSERT INTO cliente (numero_documento, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, correo, telefono, estado, password_hash, debe_cambiar_clave) VALUES
                                                                                                                                                                          ('1020304050', 'Andrés', 'Felipe', 'Gómez', 'Ruiz', 'andres.gomez@correo.com', '3001112233', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1030405060', 'Laura', NULL, 'Peña', NULL, 'laura.pena@correo.com', '3012223344', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1040506070', 'Camila', 'Andrea', 'Torres', 'Vega', 'camila.torres@correo.com', '3023334455', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1050607080', 'Diego', NULL, 'Herrera', NULL, 'diego.herrera@correo.com', NULL, 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1061728394', 'Juliana', NULL, 'Ocampo', 'Salazar', 'juliana.ocampo@correo.com', '3105557788', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1072839405', 'Sebastián', NULL, 'Arango', 'Londoño', 'sebastian.arango@correo.com', '3114448899', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1083940516', 'Mariana', NULL, 'Cifuentes', 'Duque', 'mariana.cifuentes@correo.com', '3126669900', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1094051627', 'Felipe', NULL, 'Rojas', 'Castaño', 'felipe.rojas@correo.com', '3137771122', 'BLOQUEADO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1005162738', 'Natalia', NULL, 'Bermúdez', 'Prieto', 'natalia.bermudez@correo.com', '3148882233', 'INACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1016273849', 'Carlos', 'Eduardo', 'Mejía', 'Gil', 'carlos.mejia@correo.com', '3159993344', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1027384950', 'Isabela', NULL, 'Vargas', 'Roldán', 'isabela.vargas@correo.com', '3161114455', 'ACTIVO', crypt('Temporal123*', gen_salt('bf', 10)), TRUE),
                                                                                                                                                                          ('1038495061', 'Tomás', NULL, 'Gutiérrez', 'Mora', 'tomas.gutierrez@correo.com', '3172225566', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1049506172', 'Valeria', NULL, 'Núñez', 'Cardona', 'valeria.nunez@correo.com', '3183336677', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1050617283', 'Jorge', 'Iván', 'Pineda', 'Soto', 'jorge.pineda@correo.com', '3194447788', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1061728305', 'Sara', 'Lucía', 'Ortiz', 'Henao', 'sara.ortiz@correo.com', '3205558899', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1072839416', 'Miguel', 'Ángel', 'Rincón', 'Parra', 'miguel.rincon@correo.com', '3216669900', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE),
                                                                                                                                                                          ('1083940527', 'Paula', 'Andrea', 'Zapata', 'Ríos', 'paula.zapata@correo.com', '3227770011', 'ACTIVO', crypt('Clave123*', gen_salt('bf', 10)), FALSE);
INSERT INTO licencia (numero_licencia, categoria_licencia, fecha_expedicion, fecha_vencimiento, cliente_id_cliente) VALUES
                                                                                                                        ('LIC-0001', 'B1', current_date - 1500, current_date + 1500, 1),
                                                                                                                        ('LIC-0002', 'B1', current_date - 900, current_date + 10, 2),
                                                                                                                        ('LIC-0003', 'C1', current_date - 700, current_date + 2000, 3),
                                                                                                                        ('LIC-0004', 'B1', current_date - 2200, current_date + 900, 5),
                                                                                                                        ('LIC-0005', 'B1', current_date - 400, current_date + 2900, 6),
                                                                                                                        ('LIC-0006', 'B1', current_date - 300, current_date + 3300, 7),
                                                                                                                        ('LIC-0007', 'B2', current_date - 1800, current_date + 700, 8),
                                                                                                                        ('LIC-0008', 'B1', current_date - 3000, current_date + 400, 9),
                                                                                                                        ('LIC-0009', 'B1', current_date - 3650, current_date - 20, 10),
                                                                                                                        ('LIC-0010', 'B1', current_date - 500, current_date + 1300, 11),
                                                                                                                        ('LIC-0011', 'C2', current_date - 2600, current_date + 600, 12),
                                                                                                                        ('LIC-0012', 'B1', current_date - 1000, current_date + 1800, 13),
                                                                                                                        ('LIC-0013', 'B1', current_date - 1400, current_date + 1000, 14),
                                                                                                                        ('LIC-0014', 'B1', current_date - 800, current_date + 2100, 15),
                                                                                                                        ('LIC-0015', 'B1', current_date - 1900, current_date + 1500, 16),
                                                                                                                        ('LIC-0016', 'B1', current_date - 200, current_date + 3400, 17);

-- ---------- Flota (nacen DISPONIBLE; los estados finales se aplican al terminar) ----------
INSERT INTO vehiculo (placa, marca, modelo_anio, kilometraje, estado, color, categoria_id_categoria, sede_id_sede) VALUES
                                                                                                                       ('ABC123', 'Chevrolet Spark', 2022, 26400, 'DISPONIBLE', 'Rojo', 1, 1),
                                                                                                                       ('DEF456', 'Renault Logan', 2021, 52900, 'DISPONIBLE', 'Blanco', 1, 1),
                                                                                                                       ('GHI789', 'Mazda 2', 2023, 16800, 'DISPONIBLE', 'Gris', 2, 1),
                                                                                                                       ('JKL012', 'Kia Sportage', 2023, 21500, 'DISPONIBLE', 'Negro', 3, 2),
                                                                                                                       ('MNO345', 'Toyota Fortuner', 2022, 33800, 'DISPONIBLE', 'Plata', 3, 2),
                                                                                                                       ('PQR678', 'Hyundai H1', 2020, 68400, 'DISPONIBLE', 'Blanco', 4, 3),
                                                                                                                       ('STU901', 'Mazda 3', 2024, 6900, 'DISPONIBLE', 'Azul', 2, 3),
                                                                                                                       ('VWX234', 'Renault Duster', 2022, 31200, 'DISPONIBLE', 'Verde', 3, 3),
                                                                                                                       ('HJK321', 'Kia Picanto', 2024, 8200, 'DISPONIBLE', 'Amarillo', 1, 1),
                                                                                                                       ('LMN654', 'Renault Sandero', 2022, 33500, 'DISPONIBLE', 'Gris', 1, 2),
                                                                                                                       ('NPQ987', 'Toyota Corolla', 2023, 17600, 'DISPONIBLE', 'Blanco', 2, 2),
                                                                                                                       ('RST246', 'Mazda CX-30', 2024, 8900, 'DISPONIBLE', 'Rojo', 3, 1),
                                                                                                                       ('TUV135', 'Chevrolet Tracker', 2023, 16100, 'DISPONIBLE', 'Azul', 3, 3),
                                                                                                                       ('WXY468', 'Toyota Hilux', 2022, 29400, 'DISPONIBLE', 'Gris', 6, 3),
                                                                                                                       ('ZAB579', 'Mercedes-Benz Clase C', 2023, 14200, 'DISPONIBLE', 'Negro', 5, 2),
                                                                                                                       ('CDE680', 'BMW Serie 3', 2022, 25800, 'DISPONIBLE', 'Blanco', 5, 1),
                                                                                                                       ('FGH791', 'Renault Master', 2021, 51700, 'DISPONIBLE', 'Blanco', 4, 4),
                                                                                                                       ('IJK802', 'Mazda 2 Sedán', 2022, 27900, 'DISPONIBLE', 'Rojo', 2, 4),
                                                                                                                       ('LMP913', 'Chevrolet Onix', 2023, 15900, 'DISPONIBLE', 'Plata', 1, 4),
                                                                                                                       ('NQR024', 'Hyundai Tucson', 2023, 13700, 'DISPONIBLE', 'Negro', 3, 4),
                                                                                                                       ('STV135', 'Nissan Kicks', 2022, 38200, 'DISPONIBLE', 'Naranja', 3, 1),
                                                                                                                       ('UWX246', 'Renault Kwid', 2020, 74500, 'DISPONIBLE', 'Blanco', 1, 2),
                                                                                                                       ('YZA357', 'Chevrolet Captiva', 2019, 61800, 'DISPONIBLE', 'Gris', 3, 1),
                                                                                                                       ('BCD468', 'Kia Rio', 2018, 83600, 'DISPONIBLE', 'Blanco', 1, 5),
                                                                                                                       ('EFG579', 'Porsche 911', 2022, 9800, 'DISPONIBLE', 'Rojo', 7, 1);

-- ---------- Reservas (en orden cronológico; cada una recorre sus transiciones reales) ----------

-- R1 [A20] Tomás Gutiérrez · BCD468 · FINALIZADA · Sede cerrada, asesor retirado y vehículo vendido (histórico)
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 157)::timestamp + interval '15 hours 15 minutes', 'PENDIENTE', current_date - 150, current_date - 146, 525000.00, 12, 24, 12, 5, 5);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 1, 75000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 156, 525000.00, 'TARJETA', 'APROBADO', 1);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 1;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 1;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 1;

-- R2 [A19] Andrés Gómez · EFG579 · FINALIZADA · Usa un seguro y una categoría hoy inactivos (histórico)
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 145)::timestamp + interval '9 hours 15 minutes', 'PENDIENTE', current_date - 140, current_date - 137, 2260000.00, 1, 25, 2, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (6, 2, 180000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 144, 2260000.00, 'TARJETA', 'APROBADO', 2);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 2;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 2;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 2;

-- R3 [A1] Andrés Gómez · ABC123 · FINALIZADA · Alquiler sin novedades
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 132)::timestamp + interval '9 hours 35 minutes', 'PENDIENTE', current_date - 120, current_date - 117, 420000.00, 1, 1, 2, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 3, 60000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 131, 420000.00, 'TARJETA', 'APROBADO', 3);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 3;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 3;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 3;

-- R4 [A2] Camila Torres · GHI789 · FINALIZADA · Pago en dos abonos; rayón leve con penalización pagada
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 110)::timestamp + interval '10 hours 25 minutes', 'PENDIENTE', current_date - 100, current_date - 96, 775000.00, 3, 3, 2, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 4, 175000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 109, 387000.00, 'TARJETA', 'APROBADO', 4);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 105, 388000.00, 'PSE', 'APROBADO', 4);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 4;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 4;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 4;

-- R5 [A3] Andrés Gómez · JKL012 · FINALIZADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 98)::timestamp + interval '16 hours 5 minutes', 'PENDIENTE', current_date - 90, current_date - 87, 972000.00, 1, 4, 3, 2, 2);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 5, 60000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (3, 5, 32000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 97, 972000.00, 'PSE', 'APROBADO', 5);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 5;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 5;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 5;

-- R6 [A4] Tomás Gutiérrez · MNO345 · FINALIZADA · Pago en tres abonos; abolladura moderada con penalización pagada
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 85)::timestamp + interval '13 hours 35 minutes', 'PENDIENTE', current_date - 80, current_date - 74, 1869000.00, 12, 5, 3, 2, 2);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 6, 245000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (4, 6, 84000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 84, 747000.00, 'TARJETA', 'APROBADO', 6);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 80, 560000.00, 'EFECTIVO', 'APROBADO', 6);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 80, 562000.00, 'TARJETA', 'APROBADO', 6);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 6;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 6;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 6;

-- R7 [A5] Felipe Rojas · STU901 · FINALIZADA · Daño grave: penalización PENDIENTE y cliente BLOQUEADO
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 78)::timestamp + interval '8 hours 5 minutes', 'PENDIENTE', current_date - 70, current_date - 66, 775000.00, 8, 7, 7, 3, 3);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 7, 175000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 77, 775000.00, 'TARJETA', 'APROBADO', 7);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 7;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 7;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 7;

-- R8 [A6] Juliana Ocampo · PQR678 · FINALIZADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 76)::timestamp + interval '13 hours 15 minutes', 'PENDIENTE', current_date - 65, current_date - 60, 1890000.00, 5, 6, 7, 3, 3);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 8, 90000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 75, 1890000.00, 'EFECTIVO', 'APROBADO', 8);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 8;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 8;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 8;

-- R9 [A7] Sebastián Arango · VWX234 · FINALIZADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 66)::timestamp + interval '18 hours 35 minutes', 'PENDIENTE', current_date - 55, current_date - 52, 1020000.00, 6, 8, 7, 3, 3);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 9, 140000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 65, 1020000.00, 'TARJETA', 'APROBADO', 9);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 9;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 9;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 9;

-- R10 [A8] Natalia Bermúdez · DEF456 · FINALIZADA · Devolución en otra sede (Chapinero -> Aeropuerto)
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 59)::timestamp + interval '9 hours 45 minutes', 'PENDIENTE', current_date - 50, current_date - 47, 420000.00, 9, 2, 2, 1, 2);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 10, 60000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 58, 420000.00, 'PSE', 'APROBADO', 10);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 10;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 10;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 10;

-- R11 [A10] Carlos Mejía · LMN654 · FINALIZADA · Cliente cuya licencia hoy está vencida
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 56)::timestamp + interval '13 hours 25 minutes', 'PENDIENTE', current_date - 45, current_date - 42, 392000.00, 10, 10, 3, 2, 2);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (3, 11, 32000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 55, 392000.00, 'EFECTIVO', 'APROBADO', 11);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 11;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 11;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 11;

-- R12 [A9] Tomás Gutiérrez · NPQ987 · FINALIZADA · Devuelto con medio tanque: penalización por combustible
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 50)::timestamp + interval '10 hours 45 minutes', 'PENDIENTE', current_date - 45, current_date - 43, 405000.00, 12, 11, 3, 2, 2);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 12, 45000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 49, 405000.00, 'TARJETA', 'APROBADO', 12);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 12;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 12;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 12;

-- R13 [A11] Andrés Gómez · RST246 · FINALIZADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 48)::timestamp + interval '13 hours 25 minutes', 'PENDIENTE', current_date - 40, current_date - 37, 1060000.00, 1, 12, 2, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 13, 140000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (5, 13, 40000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 47, 1060000.00, 'TARJETA', 'APROBADO', 13);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 13;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 13;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 13;

-- R14 [A12] Camila Torres · ZAB579 · FINALIZADA · Premium con devolución en otra sede (Aeropuerto -> Chapinero)
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 48)::timestamp + interval '15 hours 15 minutes', 'PENDIENTE', current_date - 36, current_date - 32, 2125000.00, 3, 15, 3, 2, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 14, 175000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (5, 14, 50000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 47, 1062000.00, 'TARJETA', 'APROBADO', 14);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 42, 1063000.00, 'TARJETA', 'APROBADO', 14);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 14;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 14;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 14;

-- R15 [A15] Sara Ortiz · NQR024 · FINALIZADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 42)::timestamp + interval '18 hours 5 minutes', 'PENDIENTE', current_date - 33, current_date - 29, 1140000.00, 15, 20, 8, 4, 4);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (3, 15, 40000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 41, 1140000.00, 'TARJETA', 'APROBADO', 15);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 15;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 15;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 15;

-- R16 [A13] Valeria Núñez · IJK802 · FINALIZADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 36)::timestamp + interval '13 hours 5 minutes', 'PENDIENTE', current_date - 30, current_date - 27, 540000.00, 13, 18, 8, 4, 4);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 16, 60000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 35, 540000.00, 'TARJETA', 'APROBADO', 16);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 16;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 16;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 16;

-- R17 [A16] Andrés Gómez · CDE680 · FINALIZADA · Devolución tardía: recargo pendiente
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 33)::timestamp + interval '9 hours 5 minutes', 'PENDIENTE', current_date - 28, current_date - 26, 1245000.00, 1, 16, 2, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 17, 105000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 32, 1245000.00, 'TARJETA', 'APROBADO', 17);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 17;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 17;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 17;

-- R18 [A14] Jorge Pineda · LMP913 · FINALIZADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 37)::timestamp + interval '9 hours 15 minutes', 'PENDIENTE', current_date - 25, current_date - 23, 375000.00, 14, 19, 8, 4, 4);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 18, 105000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 36, 375000.00, 'PSE', 'APROBADO', 18);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 18;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 18;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 18;

-- R19 [A17] Mariana Cifuentes · HJK321 · FINALIZADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 32)::timestamp + interval '9 hours 35 minutes', 'PENDIENTE', current_date - 22, current_date - 20, 315000.00, 7, 9, 2, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 19, 45000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 31, 315000.00, 'EFECTIVO', 'APROBADO', 19);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 19;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 19;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 19;

-- R20 [A18] Tomás Gutiérrez · FGH791 · FINALIZADA · Rayón leve: penalización ANULADA por cortesía
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 32)::timestamp + interval '11 hours 15 minutes', 'PENDIENTE', current_date - 21, current_date - 17, 1635000.00, 12, 17, 8, 4, 4);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 20, 75000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (4, 20, 60000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 31, 1635000.00, 'TARJETA', 'APROBADO', 20);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 20;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 20;
UPDATE reserva SET estado_reserva = 'FINALIZADA' WHERE id_reserva = 20;

-- R21 [C1] Valeria Núñez · ABC123 · CANCELADA · Pagada y luego cancelada: pago REEMBOLSADO
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 21)::timestamp + interval '11 hours 35 minutes', 'PENDIENTE', current_date - 15, current_date - 12, 360000.00, 13, 1, NULL, 1, 1);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 20, 360000.00, 'TARJETA', 'APROBADO', 21);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 21;
UPDATE reserva SET estado_reserva = 'CANCELADA' WHERE id_reserva = 21;
UPDATE pago SET estado_pago = 'REEMBOLSADO' WHERE reserva_id_reserva = 21 AND estado_pago = 'APROBADO';

-- R22 [C3] Mariana Cifuentes · GHI789 · CANCELADA · Pre-reserva que el cliente canceló sin pagar
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 17)::timestamp + interval '14 hours 5 minutes', 'PENDIENTE', current_date - 12, current_date - 10, 360000.00, 7, 3, NULL, 1, 1);
UPDATE reserva SET estado_reserva = 'CANCELADA' WHERE id_reserva = 22;

-- R23 [C2] Jorge Pineda · MNO345 · CANCELADA · Pago rechazado: se cancela de inmediato (rollback)
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 15)::timestamp + interval '13 hours 25 minutes', 'PENDIENTE', current_date - 10, current_date - 8, 765000.00, 14, 5, NULL, 2, 2);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 23, 105000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 15, 765000.00, 'TARJETA', 'RECHAZADO', 23);
UPDATE reserva SET estado_reserva = 'CANCELADA' WHERE id_reserva = 23;

-- R24 [E3] Miguel Rincón · VWX234 · EN_CURSO · TERMINA HOY: demo de devolución (inspección + penalización + finalizar)
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 11)::timestamp + interval '18 hours 15 minutes', 'PENDIENTE', current_date - 5, current_date, 1590000.00, 16, 8, 7, 3, 3);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 24, 210000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (5, 24, 60000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 10, 1590000.00, 'PSE', 'APROBADO', 24);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 24;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 24;

-- R25 [E1] Sebastián Arango · JKL012 · EN_CURSO · Pagado completo; entrega registrada
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 10)::timestamp + interval '18 hours 15 minutes', 'PENDIENTE', current_date - 3, current_date + 2, 1578000.00, 6, 4, 3, 2, 2);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 25, 210000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (3, 25, 48000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 9, 1578000.00, 'TARJETA', 'APROBADO', 25);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 25;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 25;

-- R26 [E2] Sara Ortiz · TUV135 · EN_CURSO · Abono del 50%: no se puede finalizar hasta pagar el saldo
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 6)::timestamp + interval '18 hours 5 minutes', 'PENDIENTE', current_date - 2, current_date + 3, 1410000.00, 15, 13, 7, 3, 3);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 26, 90000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 5, 705000.00, 'EFECTIVO', 'APROBADO', 26);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 26;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 26;

-- R27 [E4] Juliana Ocampo · NQR024 · EN_CURSO
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 5)::timestamp + interval '14 hours 25 minutes', 'PENDIENTE', current_date - 1, current_date + 4, 1368000.00, 5, 20, 8, 4, 4);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (3, 27, 48000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 4, 1368000.00, 'TARJETA', 'APROBADO', 27);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 27;
UPDATE reserva SET estado_reserva = 'EN_CURSO' WHERE id_reserva = 27;

-- R28 [K1] Andrés Gómez · GHI789 · CONFIRMADA · EMPIEZA HOY y sin asesor: demo de entrega (inspección + iniciar alquiler)
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 7)::timestamp + interval '17 hours 5 minutes', 'PENDIENTE', current_date, current_date + 3, 620000.00, 1, 3, NULL, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 28, 140000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 2, 620000.00, 'TARJETA', 'APROBADO', 28);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 28;

-- R29 [P1] Mariana Cifuentes · LMN654 · PENDIENTE · Para pagar en vivo (confirma al aprobar el pago)
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 9)::timestamp + interval '10 hours 25 minutes', 'PENDIENTE', current_date + 1, current_date + 3, 315000.00, 7, 10, 3, 2, 2);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 29, 45000.00);

-- R30 [K2] Camila Torres · RST246 · CONFIRMADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 1)::timestamp + interval '11 hours 45 minutes', 'PENDIENTE', current_date + 2, current_date + 6, 1150000.00, 3, 12, NULL, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (5, 30, 50000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 1, 1150000.00, 'PSE', 'APROBADO', 30);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 30;

-- R31 [K6] Jorge Pineda · NPQ987 · CONFIRMADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 1)::timestamp + interval '8 hours 35 minutes', 'PENDIENTE', current_date + 3, current_date + 5, 360000.00, 14, 11, NULL, 2, 2);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date, 360000.00, 'PSE', 'APROBADO', 31);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 31;

-- R32 [K3] Laura Peña · HJK321 · CONFIRMADA · Licencia de la cliente vence en +10 días: no admite reservas más largas
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 1)::timestamp + interval '13 hours 35 minutes', 'PENDIENTE', current_date + 5, current_date + 9, 525000.00, 2, 9, 2, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 32, 75000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date, 525000.00, 'EFECTIVO', 'APROBADO', 32);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 32;

-- R33 [K5] Valeria Núñez · IJK802 · CONFIRMADA
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 2)::timestamp + interval '13 hours 5 minutes', 'PENDIENTE', current_date + 7, current_date + 10, 620000.00, 13, 18, 8, 4, 4);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 33, 140000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date - 1, 620000.00, 'TARJETA', 'APROBADO', 33);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 33;

-- R34 [K4] Tomás Gutiérrez · ZAB579 · CONFIRMADA · Abono del 40%
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 1)::timestamp + interval '10 hours 5 minutes', 'PENDIENTE', current_date + 10, current_date + 14, 2125000.00, 12, 15, NULL, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 34, 175000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (5, 34, 50000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date, 850000.00, 'TARJETA', 'APROBADO', 34);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 34;

-- R35 [K7] Andrés Gómez · MNO345 · CONFIRMADA · Devolución en otra sede (Aeropuerto -> Chapinero)
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 1)::timestamp + interval '15 hours 35 minutes', 'PENDIENTE', current_date + 12, current_date + 16, 1275000.00, 1, 5, 3, 2, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 35, 175000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date, 637000.00, 'TARJETA', 'APROBADO', 35);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 35;

-- R36 [C4] Sara Ortiz · TUV135 · CANCELADA · Reserva futura cancelada con reembolso
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 1)::timestamp + interval '15 hours 5 minutes', 'PENDIENTE', current_date + 20, current_date + 22, 705000.00, 15, 13, NULL, 3, 3);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 36, 45000.00);
INSERT INTO pago (fecha_pago, monto, metodo_pago, estado_pago, reserva_id_reserva) VALUES (current_date, 705000.00, 'PSE', 'APROBADO', 36);
UPDATE reserva SET estado_reserva = 'CONFIRMADA' WHERE id_reserva = 36;
UPDATE reserva SET estado_reserva = 'CANCELADA' WHERE id_reserva = 36;
UPDATE pago SET estado_pago = 'REEMBOLSADO' WHERE reserva_id_reserva = 36 AND estado_pago = 'APROBADO';

-- R37 [P2] Andrés Gómez · CDE680 · PENDIENTE
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 1)::timestamp + interval '15 hours 25 minutes', 'PENDIENTE', current_date + 20, current_date + 24, 2125000.00, 1, 16, NULL, 1, 1);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (2, 37, 175000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (5, 37, 50000.00);

-- R38 [P3] Sebastián Arango · PQR678 · PENDIENTE
INSERT INTO reserva (fecha_creacion, estado_reserva, fecha_inicio, fecha_fin, costo_total, cliente_id_cliente, vehiculo_id_vehiculo, asesor_id_empleado, sede_entrega, sede_recibe)
VALUES ((current_date - 1)::timestamp + interval '15 hours 15 minutes', 'PENDIENTE', current_date + 30, current_date + 33, 1292000.00, 6, 6, NULL, 3, 3);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (1, 38, 60000.00);
INSERT INTO seg_res (seguro_id_seguro, reserva_id_reserva, costo_contratado) VALUES (3, 38, 32000.00);

-- ---------- Penalizaciones (enlazadas a la inspección de MongoDB que documenta el daño) ----------
INSERT INTO penalizacion (fecha_cargo, concepto, monto, estado_cargo, id_inspeccion_mongo, reserva_id_reserva) VALUES (current_date - 96, 'Rayón en puerta trasera derecha', 180000.00, 'PAGADA', '5e0d0000000000000000000b', 4);
INSERT INTO penalizacion (fecha_cargo, concepto, monto, estado_cargo, id_inspeccion_mongo, reserva_id_reserva) VALUES (current_date - 74, 'Abolladura en guardabarros y espejo retrovisor izquierdo', 650000.00, 'PAGADA', '5e0d00000000000000000013', 6);
INSERT INTO penalizacion (fecha_cargo, concepto, monto, estado_cargo, id_inspeccion_mongo, reserva_id_reserva) VALUES (current_date - 66, 'Parabrisas, parachoques y faro delantero dañados', 1200000.00, 'PENDIENTE', '5e0d00000000000000000015', 7);
INSERT INTO penalizacion (fecha_cargo, concepto, monto, estado_cargo, id_inspeccion_mongo, reserva_id_reserva) VALUES (current_date - 17, 'Rayón en el costado izquierdo', 90000.00, 'ANULADA', '5e0d0000000000000000003d', 20);
INSERT INTO penalizacion (fecha_cargo, concepto, monto, estado_cargo, id_inspeccion_mongo, reserva_id_reserva) VALUES (current_date - 43, 'Combustible faltante en la devolución', 45000.00, 'PAGADA', '5e0d00000000000000000024', 12);
INSERT INTO penalizacion (fecha_cargo, concepto, monto, estado_cargo, id_inspeccion_mongo, reserva_id_reserva) VALUES (current_date - 26, 'Recargo por devolución tardía (4 horas)', 60000.00, 'PENDIENTE', NULL, 17);

-- ---------- Estado final de flota, catálogos y personal ----------
UPDATE vehiculo SET estado = 'EN_MANTENIMIENTO' WHERE id_vehiculo IN (21, 22);
UPDATE vehiculo SET estado = 'BAJA' WHERE id_vehiculo IN (23);
UPDATE vehiculo SET estado = 'VENDIDO' WHERE id_vehiculo IN (24);
UPDATE seguro SET estado = 'INACTIVO' WHERE id_seguro IN (6);
UPDATE categoria SET estado = 'INACTIVO' WHERE id_categoria IN (7);
UPDATE sede SET estado = 'INACTIVO' WHERE id_sede IN (5);

-- ---------- Auditoría con fechas coherentes (el trigger la creó con la hora de carga) ----------
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 157)::timestamp + interval '15 hours 15 minutes' WHERE id_reserva = 1 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 156)::timestamp + interval '20 hours 12 minutes' WHERE id_reserva = 1 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 150)::timestamp + interval '10 hours 33 minutes' WHERE id_reserva = 1 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 146)::timestamp + interval '17 hours 4 minutes' WHERE id_reserva = 1 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 145)::timestamp + interval '9 hours 15 minutes' WHERE id_reserva = 2 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 144)::timestamp + interval '15 hours 42 minutes' WHERE id_reserva = 2 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 140)::timestamp + interval '10 hours 13 minutes' WHERE id_reserva = 2 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 137)::timestamp + interval '16 hours 4 minutes' WHERE id_reserva = 2 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 132)::timestamp + interval '9 hours 35 minutes' WHERE id_reserva = 3 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 131)::timestamp + interval '18 hours 2 minutes' WHERE id_reserva = 3 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 120)::timestamp + interval '9 hours 23 minutes' WHERE id_reserva = 3 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 117)::timestamp + interval '16 hours 14 minutes' WHERE id_reserva = 3 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 110)::timestamp + interval '10 hours 25 minutes' WHERE id_reserva = 4 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 109)::timestamp + interval '13 hours 22 minutes' WHERE id_reserva = 4 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 100)::timestamp + interval '10 hours 33 minutes' WHERE id_reserva = 4 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 96)::timestamp + interval '17 hours 14 minutes' WHERE id_reserva = 4 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 98)::timestamp + interval '16 hours 5 minutes' WHERE id_reserva = 5 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 97)::timestamp + interval '13 hours 42 minutes' WHERE id_reserva = 5 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 90)::timestamp + interval '8 hours 13 minutes' WHERE id_reserva = 5 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 87)::timestamp + interval '16 hours 44 minutes' WHERE id_reserva = 5 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 85)::timestamp + interval '13 hours 35 minutes' WHERE id_reserva = 6 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 84)::timestamp + interval '11 hours 2 minutes' WHERE id_reserva = 6 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 80)::timestamp + interval '9 hours 43 minutes' WHERE id_reserva = 6 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 74)::timestamp + interval '16 hours 44 minutes' WHERE id_reserva = 6 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 78)::timestamp + interval '8 hours 5 minutes' WHERE id_reserva = 7 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 77)::timestamp + interval '20 hours 32 minutes' WHERE id_reserva = 7 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 70)::timestamp + interval '9 hours 3 minutes' WHERE id_reserva = 7 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 66)::timestamp + interval '17 hours 14 minutes' WHERE id_reserva = 7 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 76)::timestamp + interval '13 hours 15 minutes' WHERE id_reserva = 8 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 75)::timestamp + interval '20 hours 22 minutes' WHERE id_reserva = 8 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 65)::timestamp + interval '8 hours 53 minutes' WHERE id_reserva = 8 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 60)::timestamp + interval '17 hours 34 minutes' WHERE id_reserva = 8 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 66)::timestamp + interval '18 hours 35 minutes' WHERE id_reserva = 9 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 65)::timestamp + interval '9 hours 32 minutes' WHERE id_reserva = 9 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 55)::timestamp + interval '8 hours 53 minutes' WHERE id_reserva = 9 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 52)::timestamp + interval '17 hours 14 minutes' WHERE id_reserva = 9 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 59)::timestamp + interval '9 hours 45 minutes' WHERE id_reserva = 10 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 58)::timestamp + interval '18 hours 32 minutes' WHERE id_reserva = 10 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 50)::timestamp + interval '9 hours 3 minutes' WHERE id_reserva = 10 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 47)::timestamp + interval '18 hours 24 minutes' WHERE id_reserva = 10 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 56)::timestamp + interval '13 hours 25 minutes' WHERE id_reserva = 11 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 55)::timestamp + interval '15 hours 12 minutes' WHERE id_reserva = 11 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 45)::timestamp + interval '8 hours 23 minutes' WHERE id_reserva = 11 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 42)::timestamp + interval '17 hours 34 minutes' WHERE id_reserva = 11 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 50)::timestamp + interval '10 hours 45 minutes' WHERE id_reserva = 12 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 49)::timestamp + interval '10 hours 22 minutes' WHERE id_reserva = 12 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 45)::timestamp + interval '8 hours 43 minutes' WHERE id_reserva = 12 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 43)::timestamp + interval '18 hours 24 minutes' WHERE id_reserva = 12 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 48)::timestamp + interval '13 hours 25 minutes' WHERE id_reserva = 13 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 47)::timestamp + interval '16 hours 32 minutes' WHERE id_reserva = 13 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 40)::timestamp + interval '8 hours 3 minutes' WHERE id_reserva = 13 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 37)::timestamp + interval '16 hours 34 minutes' WHERE id_reserva = 13 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 48)::timestamp + interval '15 hours 15 minutes' WHERE id_reserva = 14 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 47)::timestamp + interval '11 hours 42 minutes' WHERE id_reserva = 14 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 36)::timestamp + interval '10 hours 3 minutes' WHERE id_reserva = 14 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 32)::timestamp + interval '16 hours 4 minutes' WHERE id_reserva = 14 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 42)::timestamp + interval '18 hours 5 minutes' WHERE id_reserva = 15 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 41)::timestamp + interval '13 hours 42 minutes' WHERE id_reserva = 15 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 33)::timestamp + interval '9 hours 3 minutes' WHERE id_reserva = 15 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 29)::timestamp + interval '18 hours 4 minutes' WHERE id_reserva = 15 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 36)::timestamp + interval '13 hours 5 minutes' WHERE id_reserva = 16 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 35)::timestamp + interval '17 hours 12 minutes' WHERE id_reserva = 16 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 30)::timestamp + interval '8 hours 43 minutes' WHERE id_reserva = 16 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 27)::timestamp + interval '18 hours 14 minutes' WHERE id_reserva = 16 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 33)::timestamp + interval '9 hours 5 minutes' WHERE id_reserva = 17 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 32)::timestamp + interval '10 hours 42 minutes' WHERE id_reserva = 17 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 28)::timestamp + interval '10 hours 33 minutes' WHERE id_reserva = 17 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 26)::timestamp + interval '18 hours 4 minutes' WHERE id_reserva = 17 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 37)::timestamp + interval '9 hours 15 minutes' WHERE id_reserva = 18 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 36)::timestamp + interval '14 hours 42 minutes' WHERE id_reserva = 18 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 25)::timestamp + interval '10 hours 33 minutes' WHERE id_reserva = 18 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 23)::timestamp + interval '18 hours 54 minutes' WHERE id_reserva = 18 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 32)::timestamp + interval '9 hours 35 minutes' WHERE id_reserva = 19 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 31)::timestamp + interval '16 hours 22 minutes' WHERE id_reserva = 19 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 22)::timestamp + interval '8 hours 13 minutes' WHERE id_reserva = 19 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 20)::timestamp + interval '16 hours 24 minutes' WHERE id_reserva = 19 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 32)::timestamp + interval '11 hours 15 minutes' WHERE id_reserva = 20 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 31)::timestamp + interval '18 hours 22 minutes' WHERE id_reserva = 20 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 21)::timestamp + interval '10 hours 33 minutes' WHERE id_reserva = 20 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 17)::timestamp + interval '16 hours 34 minutes' WHERE id_reserva = 20 AND estado_nuevo = 'FINALIZADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 21)::timestamp + interval '11 hours 35 minutes' WHERE id_reserva = 21 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 20)::timestamp + interval '10 hours 2 minutes' WHERE id_reserva = 21 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 16)::timestamp + interval '11 hours 30 minutes' WHERE id_reserva = 21 AND estado_nuevo = 'CANCELADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 17)::timestamp + interval '14 hours 5 minutes' WHERE id_reserva = 22 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 14)::timestamp + interval '11 hours 30 minutes' WHERE id_reserva = 22 AND estado_nuevo = 'CANCELADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 15)::timestamp + interval '13 hours 25 minutes' WHERE id_reserva = 23 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 15)::timestamp + interval '11 hours 30 minutes' WHERE id_reserva = 23 AND estado_nuevo = 'CANCELADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 11)::timestamp + interval '18 hours 15 minutes' WHERE id_reserva = 24 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 10)::timestamp + interval '11 hours 22 minutes' WHERE id_reserva = 24 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 5)::timestamp + interval '8 hours 3 minutes' WHERE id_reserva = 24 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 10)::timestamp + interval '18 hours 15 minutes' WHERE id_reserva = 25 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 9)::timestamp + interval '14 hours 42 minutes' WHERE id_reserva = 25 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 3)::timestamp + interval '8 hours 43 minutes' WHERE id_reserva = 25 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 6)::timestamp + interval '18 hours 5 minutes' WHERE id_reserva = 26 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 5)::timestamp + interval '12 hours 32 minutes' WHERE id_reserva = 26 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 2)::timestamp + interval '9 hours 53 minutes' WHERE id_reserva = 26 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 5)::timestamp + interval '14 hours 25 minutes' WHERE id_reserva = 27 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 4)::timestamp + interval '16 hours 2 minutes' WHERE id_reserva = 27 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '10 hours 13 minutes' WHERE id_reserva = 27 AND estado_nuevo = 'EN_CURSO';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 7)::timestamp + interval '17 hours 5 minutes' WHERE id_reserva = 28 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 2)::timestamp + interval '12 hours 32 minutes' WHERE id_reserva = 28 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 9)::timestamp + interval '10 hours 25 minutes' WHERE id_reserva = 29 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '11 hours 45 minutes' WHERE id_reserva = 30 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '20 hours 22 minutes' WHERE id_reserva = 30 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '8 hours 35 minutes' WHERE id_reserva = 31 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = now() - interval '3 hours' WHERE id_reserva = 31 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '13 hours 35 minutes' WHERE id_reserva = 32 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = now() - interval '3 hours' WHERE id_reserva = 32 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 2)::timestamp + interval '13 hours 5 minutes' WHERE id_reserva = 33 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '12 hours 12 minutes' WHERE id_reserva = 33 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '10 hours 5 minutes' WHERE id_reserva = 34 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = now() - interval '3 hours' WHERE id_reserva = 34 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '15 hours 35 minutes' WHERE id_reserva = 35 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = now() - interval '3 hours' WHERE id_reserva = 35 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '15 hours 5 minutes' WHERE id_reserva = 36 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = now() - interval '3 hours' WHERE id_reserva = 36 AND estado_nuevo = 'CONFIRMADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '11 hours 30 minutes' WHERE id_reserva = 36 AND estado_nuevo = 'CANCELADA';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '15 hours 25 minutes' WHERE id_reserva = 37 AND estado_nuevo = 'PENDIENTE';
UPDATE auditoria_reserva SET fecha_cambio = (current_date - 1)::timestamp + interval '15 hours 15 minutes' WHERE id_reserva = 38 AND estado_nuevo = 'PENDIENTE';

-- ---------- Verificaciones (si algo no cuadra, se revierte todo) ----------
DO $$
BEGIN
    ASSERT (SELECT count(*) FROM reserva) = 38, 'Se esperaban 38 reservas';
ASSERT (SELECT count(*) FROM (VALUES
                                  (1, 'BCD468', 'FINALIZADA'),
                                  (2, 'EFG579', 'FINALIZADA'),
                                  (3, 'ABC123', 'FINALIZADA'),
                                  (4, 'GHI789', 'FINALIZADA'),
                                  (5, 'JKL012', 'FINALIZADA'),
                                  (6, 'MNO345', 'FINALIZADA'),
                                  (7, 'STU901', 'FINALIZADA'),
                                  (8, 'PQR678', 'FINALIZADA'),
                                  (9, 'VWX234', 'FINALIZADA'),
                                  (10, 'DEF456', 'FINALIZADA'),
                                  (11, 'LMN654', 'FINALIZADA'),
                                  (12, 'NPQ987', 'FINALIZADA'),
                                  (13, 'RST246', 'FINALIZADA'),
                                  (14, 'ZAB579', 'FINALIZADA'),
                                  (15, 'NQR024', 'FINALIZADA'),
                                  (16, 'IJK802', 'FINALIZADA'),
                                  (17, 'CDE680', 'FINALIZADA'),
                                  (18, 'LMP913', 'FINALIZADA'),
                                  (19, 'HJK321', 'FINALIZADA'),
                                  (20, 'FGH791', 'FINALIZADA'),
                                  (21, 'ABC123', 'CANCELADA'),
                                  (22, 'GHI789', 'CANCELADA'),
                                  (23, 'MNO345', 'CANCELADA'),
                                  (24, 'VWX234', 'EN_CURSO'),
                                  (25, 'JKL012', 'EN_CURSO'),
                                  (26, 'TUV135', 'EN_CURSO'),
                                  (27, 'NQR024', 'EN_CURSO'),
                                  (28, 'GHI789', 'CONFIRMADA'),
                                  (29, 'LMN654', 'PENDIENTE'),
                                  (30, 'RST246', 'CONFIRMADA'),
                                  (31, 'NPQ987', 'CONFIRMADA'),
                                  (32, 'HJK321', 'CONFIRMADA'),
                                  (33, 'IJK802', 'CONFIRMADA'),
                                  (34, 'ZAB579', 'CONFIRMADA'),
                                  (35, 'MNO345', 'CONFIRMADA'),
                                  (36, 'TUV135', 'CANCELADA'),
                                  (37, 'CDE680', 'PENDIENTE'),
                                  (38, 'PQR678', 'PENDIENTE')
                             ) AS e (id, placa, estado)
                                 JOIN reserva r ON r.id_reserva = e.id
                                 JOIN vehiculo v ON v.id_vehiculo = r.vehiculo_id_vehiculo
        WHERE v.placa = e.placa AND r.estado_reserva = e.estado) = 38,
        'Los ids de reserva no coinciden con el script de MongoDB';
ASSERT (SELECT count(*) FROM vehiculo WHERE estado = 'ALQUILADO') = 4, 'Vehículos ALQUILADO: se esperaban 4';
ASSERT (SELECT count(*) FROM vehiculo WHERE estado = 'EN_MANTENIMIENTO') = 2, 'Vehículos EN_MANTENIMIENTO: se esperaban 2';
ASSERT (SELECT count(*) FROM vehiculo WHERE estado = 'BAJA') = 1, 'Vehículos BAJA: se esperaban 1';
ASSERT (SELECT count(*) FROM vehiculo WHERE estado = 'VENDIDO') = 1, 'Vehículos VENDIDO: se esperaban 1';
ASSERT (SELECT count(*) FROM vehiculo WHERE estado = 'DISPONIBLE') = 17, 'Vehículos DISPONIBLE: se esperaban 17';
ASSERT (SELECT count(*) FROM empleado) = 12, 'Empleados';
ASSERT (SELECT count(*) FROM cliente) = 17, 'Clientes';
ASSERT (SELECT count(*) FROM reserva WHERE estado_reserva = 'FINALIZADA' AND fn_saldo_pendiente(id_reserva) <> 0) = 0,
        'Hay reservas finalizadas con saldo pendiente';
ASSERT (SELECT count(*) FROM reserva WHERE estado_reserva IN ('CONFIRMADA', 'EN_CURSO') AND fn_saldo_pendiente(id_reserva) >= costo_total) = 0,
        'Hay reservas confirmadas sin ningún pago aprobado';
ASSERT (SELECT count(*) FROM auditoria_reserva WHERE fecha_cambio > now()) = 0, 'Auditoría con fechas futuras';
ASSERT (SELECT count(*) FROM (SELECT sede_id_sede FROM vehiculo GROUP BY 1 HAVING count(*) > (SELECT capacidad_estacionamiento FROM sede WHERE id_sede = sede_id_sede)) x) = 0,
        'Una sede supera su capacidad de estacionamiento';
END $$;

COMMIT;

-- Resumen de lo cargado
SELECT 'sedes' AS tabla, count(*) AS filas FROM sede
UNION ALL SELECT 'categorias', count(*) FROM categoria
UNION ALL SELECT 'seguros', count(*) FROM seguro
UNION ALL SELECT 'empleados', count(*) FROM empleado
UNION ALL SELECT 'clientes', count(*) FROM cliente
UNION ALL SELECT 'licencias', count(*) FROM licencia
UNION ALL SELECT 'vehiculos', count(*) FROM vehiculo
UNION ALL SELECT 'reservas', count(*) FROM reserva
UNION ALL SELECT 'pagos', count(*) FROM pago
UNION ALL SELECT 'seguros contratados', count(*) FROM seg_res
UNION ALL SELECT 'penalizaciones', count(*) FROM penalizacion
UNION ALL SELECT 'auditoria', count(*) FROM auditoria_reserva;

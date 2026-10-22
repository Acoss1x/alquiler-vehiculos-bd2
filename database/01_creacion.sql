-- =====================================================================
-- Proyecto BD2 - Alquiler de vehículos (PostgreSQL 14+)
-- 01_creacion.sql : tablas, restricciones, índices
--
-- Convenciones: identificadores en minúscula y SIN comillas, para coincidir con
-- los nombres que genera Hibernate (CLIENTE_id_cliente -> cliente_id_cliente).
-- Ids BIGINT (Long en Java), dinero NUMERIC(10,2), fecha_creacion TIMESTAMP.
-- =====================================================================

-- Necesaria para la restricción anti-solapamiento de reservas (EXCLUDE ... gist).
-- Es una extensión "trusted": la puede crear el dueño de la base de datos.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------- Catálogos ----------
CREATE TABLE sede (
    id_sede                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_sede               VARCHAR(100) NOT NULL,
    ciudad                    VARCHAR(50)  NOT NULL,
    direccion                 VARCHAR(150) NOT NULL,
    capacidad_estacionamiento INTEGER      NOT NULL CHECK (capacidad_estacionamiento > 0)
);

CREATE TABLE categoria (
    id_categoria        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_categoria    VARCHAR(50)   NOT NULL UNIQUE,
    capacidad_pasajeros INTEGER       NOT NULL CHECK (capacidad_pasajeros > 0),
    tarifa_diaria       NUMERIC(10,2) NOT NULL CHECK (tarifa_diaria > 0),
    tipo_transmision    VARCHAR(20)   NOT NULL CHECK (tipo_transmision IN ('MANUAL', 'AUTOMATICA'))
);

CREATE TABLE seguro (
    id_seguro     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_seguro VARCHAR(100)  NOT NULL UNIQUE,
    descripcion   VARCHAR(255),
    costo_diario  NUMERIC(10,2) NOT NULL CHECK (costo_diario >= 0)
);

-- ---------- Clientes ----------
CREATE TABLE cliente (
    id_cliente       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_documento VARCHAR(20)  NOT NULL UNIQUE,
    primer_nombre    VARCHAR(50)  NOT NULL,
    segundo_nombre   VARCHAR(50),
    primer_apellido  VARCHAR(50)  NOT NULL,
    segundo_apellido VARCHAR(50),
    correo           VARCHAR(100) NOT NULL UNIQUE CHECK (correo LIKE '%_@_%'),
    telefono         VARCHAR(20),
    estado           VARCHAR(15)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

-- Relación 1:1 con cliente (UNIQUE sobre la FK).
CREATE TABLE licencia (
    id_licencia        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_licencia    VARCHAR(50) NOT NULL UNIQUE,
    categoria_licencia VARCHAR(5)  NOT NULL,
    fecha_expedicion   DATE        NOT NULL,
    fecha_vencimiento  DATE        NOT NULL,
    cliente_id_cliente BIGINT      NOT NULL UNIQUE REFERENCES cliente (id_cliente),
    CONSTRAINT ck_licencia_fechas CHECK (fecha_vencimiento > fecha_expedicion)
);

-- ---------- Empleados (especialización total y disjunta, tabla por entidad) ----------
CREATE TABLE empleado (
    id_empleado        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_documento   VARCHAR(20)  NOT NULL UNIQUE,
    primer_nombre      VARCHAR(50)  NOT NULL,
    primer_apellido    VARCHAR(50)  NOT NULL,
    fecha_contratacion DATE         NOT NULL,
    estado             VARCHAR(15)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    sede_id_sede       BIGINT       NOT NULL REFERENCES sede (id_sede)
);

CREATE TABLE administrador (
    id_empleado     BIGINT PRIMARY KEY REFERENCES empleado (id_empleado) ON DELETE CASCADE,
    nivel_acceso    VARCHAR(30)   NOT NULL,
    bono_gerencial  NUMERIC(10,2) CHECK (bono_gerencial >= 0)
);

CREATE TABLE asesor (
    id_empleado        BIGINT PRIMARY KEY REFERENCES empleado (id_empleado) ON DELETE CASCADE,
    mostrador_asignado INTEGER NOT NULL CHECK (mostrador_asignado > 0)
);

CREATE TABLE tecnico (
    id_empleado  BIGINT PRIMARY KEY REFERENCES empleado (id_empleado) ON DELETE CASCADE,
    especialidad VARCHAR(50) NOT NULL
);

-- ---------- Vehículos ----------
CREATE TABLE vehiculo (
    id_vehiculo            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    placa                  VARCHAR(10) NOT NULL UNIQUE,
    marca                  VARCHAR(50) NOT NULL,
    modelo_anio            INTEGER     NOT NULL CHECK (modelo_anio BETWEEN 1950 AND 2100),
    kilometraje            INTEGER     NOT NULL CHECK (kilometraje >= 0),
    estado                 VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
                           CHECK (estado IN ('DISPONIBLE', 'ALQUILADO', 'EN_MANTENIMIENTO')),
    color                  VARCHAR(30),
    categoria_id_categoria BIGINT      NOT NULL REFERENCES categoria (id_categoria),
    sede_id_sede           BIGINT      NOT NULL REFERENCES sede (id_sede)
);

-- ---------- Reservas y pagos ----------
CREATE TABLE reserva (
    id_reserva           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_creacion       TIMESTAMP     NOT NULL DEFAULT now(),
    estado_reserva       VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE'
                         CHECK (estado_reserva IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA')),
    fecha_inicio         DATE          NOT NULL,
    fecha_fin            DATE          NOT NULL,
    costo_total          NUMERIC(10,2) CHECK (costo_total >= 0),   -- precio pactado al crear la reserva
    cliente_id_cliente   BIGINT        NOT NULL REFERENCES cliente (id_cliente),
    vehiculo_id_vehiculo BIGINT        NOT NULL REFERENCES vehiculo (id_vehiculo),
    asesor_id_empleado   BIGINT        NOT NULL REFERENCES asesor (id_empleado),
    sede_entrega         BIGINT        NOT NULL REFERENCES sede (id_sede),
    sede_recibe          BIGINT        NOT NULL REFERENCES sede (id_sede),
    CONSTRAINT ck_reserva_fechas CHECK (fecha_fin >= fecha_inicio),
    -- Un vehículo no puede tener dos reservas activas con fechas que se crucen.
    -- Es la defensa final contra la condición de carrera entre dos asesores.
    CONSTRAINT ex_reserva_sin_solapamiento EXCLUDE USING gist (
        vehiculo_id_vehiculo WITH =,
        daterange(fecha_inicio, fecha_fin, '[]') WITH &&
    ) WHERE (estado_reserva IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO'))
);

-- Relación 1:N con reserva (abonos, saldo, reembolsos).
CREATE TABLE pago (
    id_pago            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_pago         DATE          NOT NULL DEFAULT current_date,
    monto              NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    metodo_pago        VARCHAR(30)   NOT NULL,
    estado_pago        VARCHAR(20)   NOT NULL CHECK (estado_pago IN ('APROBADO', 'RECHAZADO', 'REEMBOLSADO')),
    reserva_id_reserva BIGINT        NOT NULL REFERENCES reserva (id_reserva)
);

-- Tabla intermedia de la relación N:M. costo_contratado = costo_diario x días, congelado al reservar.
CREATE TABLE seg_res (
    seguro_id_seguro   BIGINT        NOT NULL REFERENCES seguro (id_seguro),
    reserva_id_reserva BIGINT        NOT NULL REFERENCES reserva (id_reserva),
    costo_contratado   NUMERIC(10,2) NOT NULL CHECK (costo_contratado >= 0),
    CONSTRAINT seg_res_pk PRIMARY KEY (seguro_id_seguro, reserva_id_reserva)
);

-- ---------- Auditoría (la llena un trigger) ----------
CREATE TABLE auditoria_reserva (
    id_auditoria    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_reserva      BIGINT       NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20)  NOT NULL,
    usuario_bd      VARCHAR(63)  NOT NULL DEFAULT session_user,
    fecha_cambio    TIMESTAMP    NOT NULL DEFAULT now()
);

-- ---------- Índices de apoyo (las FK no se indexan solas en PostgreSQL) ----------
CREATE INDEX ix_vehiculo_sede_estado ON vehiculo (sede_id_sede, estado);
CREATE INDEX ix_vehiculo_categoria   ON vehiculo (categoria_id_categoria);
CREATE INDEX ix_reserva_cliente      ON reserva (cliente_id_cliente);
CREATE INDEX ix_reserva_asesor       ON reserva (asesor_id_empleado);
CREATE INDEX ix_reserva_estado       ON reserva (estado_reserva);
CREATE INDEX ix_pago_reserva         ON pago (reserva_id_reserva);
CREATE INDEX ix_seg_res_reserva      ON seg_res (reserva_id_reserva);
CREATE INDEX ix_empleado_sede        ON empleado (sede_id_sede);
CREATE INDEX ix_auditoria_reserva    ON auditoria_reserva (id_reserva);

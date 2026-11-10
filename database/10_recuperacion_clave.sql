-- =====================================================================
-- 10_recuperacion_clave.sql : enlaces de un solo uso para que cada persona
-- restablezca su propia contraseña por correo. Corre DESPUÉS de 09. Se puede repetir.
--
-- Nunca se guarda el token: solo su hash SHA-256. El enlace vence y solo sirve una vez.
-- =====================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS token_recuperacion (
    id_token         BIGSERIAL   PRIMARY KEY,
    tipo             VARCHAR(10) NOT NULL,
    id_cuenta        BIGINT      NOT NULL,
    token_hash       CHAR(64)    NOT NULL,
    fecha_creacion   TIMESTAMP   NOT NULL DEFAULT now(),
    fecha_expiracion TIMESTAMP   NOT NULL,
    usado            BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_token_tipo      CHECK (tipo IN ('CLIENTE', 'EMPLEADO')),
    CONSTRAINT uq_token_hash      UNIQUE (token_hash),
    CONSTRAINT ck_token_vigencia  CHECK (fecha_expiracion > fecha_creacion)
);

CREATE INDEX IF NOT EXISTS idx_token_cuenta ON token_recuperacion (tipo, id_cuenta);

-- Solo el dueño (el usuario del backend) toca esta tabla.
REVOKE ALL ON token_recuperacion FROM PUBLIC;

COMMIT;

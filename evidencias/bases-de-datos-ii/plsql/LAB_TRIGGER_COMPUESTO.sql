--------------------------------------------------------------------------------
-- UNIVERSIDAD EL BOSQUE — INGENIERIA DE SISTEMAS
-- Asignatura: Bases de Datos 2
-- LABORATORIO PL/SQL — TRIGGERS: DE ORA-04091 AL COMPOUND TRIGGER
--
-- INTEGRANTES: 1) Daniel Felipe Acosta Rozo
--              2) Santiago Muñoz Villate
--              3) Nataly Manrique Vera
--

--------------------------------------------------------------------------------

SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF


-- Preparacion

BEGIN
  FOR r IN (SELECT table_name FROM user_tables
             WHERE table_name IN ('LABT_RECALCULO','LABT_LINEA','LABT_PEDIDO')) LOOP
    EXECUTE IMMEDIATE 'DROP TABLE ' || r.table_name || ' CASCADE CONSTRAINTS PURGE';
  END LOOP;
END;
/

CREATE TABLE labt_pedido (
  id_pedido  NUMBER        CONSTRAINT pk_labt_pedido PRIMARY KEY,
  cliente    VARCHAR2(40)  NOT NULL,
  total      NUMBER(14,2)  DEFAULT 0 NOT NULL
);

CREATE TABLE labt_linea (
  id_pedido  NUMBER        NOT NULL CONSTRAINT fk_labt_linea_pedido REFERENCES labt_pedido,
  linea      NUMBER(4)     NOT NULL,
  cantidad   NUMBER(8)     NOT NULL CONSTRAINT ck_labt_linea_cant   CHECK (cantidad > 0),
  precio     NUMBER(12,2)  NOT NULL CONSTRAINT ck_labt_linea_precio CHECK (precio > 0),
  CONSTRAINT pk_labt_linea PRIMARY KEY (id_pedido, linea)
);


CREATE TABLE labt_recalculo (
  id_pedido  NUMBER     NOT NULL,
  fecha      TIMESTAMP  DEFAULT SYSTIMESTAMP NOT NULL
);

INSERT INTO labt_pedido VALUES (1, 'Constructora Andes',  130000);
INSERT INTO labt_pedido VALUES (2, 'Electricos del Norte', 30000);

INSERT INTO labt_linea VALUES (1, 1, 2, 50000);   
INSERT INTO labt_linea VALUES (1, 2, 1, 30000);   
INSERT INTO labt_linea VALUES (2, 1, 3, 10000);   
COMMIT;


-- El intento que falla con ORA-04091

CREATE OR REPLACE TRIGGER tgr_a_iud_labt_linea
AFTER INSERT OR UPDATE OR DELETE ON labt_linea
FOR EACH ROW
BEGIN
  UPDATE labt_pedido
     SET total = (SELECT NVL(SUM(cantidad * precio), 0)
                    FROM labt_linea                      
                   WHERE id_pedido = NVL(:NEW.id_pedido, :OLD.id_pedido))
   WHERE id_pedido = NVL(:NEW.id_pedido, :OLD.id_pedido);
END;
/


DECLARE
  e_mutante EXCEPTION;
  PRAGMA EXCEPTION_INIT(e_mutante, -4091);
  v_total   NUMBER;
  v_cant    NUMBER;
BEGIN
  BEGIN
    UPDATE labt_linea SET cantidad = 5 WHERE id_pedido = 1 AND linea = 1;
    DBMS_OUTPUT.PUT_LINE('[FALLA] el UPDATE debio fallar con ORA-04091');
  EXCEPTION
    WHEN e_mutante THEN
      DBMS_OUTPUT.PUT_LINE('[OK]    ORA-04091 capturado por nombre (e_mutante)');
      DBMS_OUTPUT.PUT_LINE('        ' || SUBSTR(SQLERRM, 1, 90));
  END;

  SELECT total INTO v_total FROM labt_pedido WHERE id_pedido = 1;
  SELECT cantidad INTO v_cant FROM labt_linea WHERE id_pedido = 1 AND linea = 1;
  DBMS_OUTPUT.PUT_LINE(CASE WHEN v_total = 130000 AND v_cant = 2 THEN '[OK]' ELSE '[FALLA]' END
                       || '    la sentencia se revirtio completa (total=' || v_total ||
                       ', cantidad=' || v_cant || ')');
END;
/

-- Compound trigger (solucion)
DROP TRIGGER tgr_a_iud_labt_linea;

CREATE OR REPLACE TRIGGER tgr_a_iud_labt_linea_total
FOR INSERT OR UPDATE OR DELETE ON labt_linea
COMPOUND TRIGGER

  TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
  v_ids t_ids;  

  AFTER EACH ROW IS
  BEGIN
   
    IF :NEW.id_pedido IS NOT NULL THEN v_ids(:NEW.id_pedido) := 1; END IF;
    IF :OLD.id_pedido IS NOT NULL THEN v_ids(:OLD.id_pedido) := 1; END IF;
  END AFTER EACH ROW;

  AFTER STATEMENT IS
    v_id PLS_INTEGER;
  BEGIN
    v_id := v_ids.FIRST;
    WHILE v_id IS NOT NULL LOOP
      UPDATE labt_pedido
         SET total = (SELECT NVL(SUM(cantidad * precio), 0)
                        FROM labt_linea
                       WHERE id_pedido = v_id)
       WHERE id_pedido = v_id;

      INSERT INTO labt_recalculo (id_pedido) VALUES (v_id);
      

      v_id := v_ids.NEXT(v_id);
    END LOOP;
  END AFTER STATEMENT;

END tgr_a_iud_labt_linea_total;
/
SHOW ERRORS TRIGGER tgr_a_iud_labt_linea_total

--Pruebas 
DECLARE
  v_ok    PLS_INTEGER := 0;
  v_falla PLS_INTEGER := 0;
  v_r0    NUMBER;
  v_r1    NUMBER;

  PROCEDURE sp_chk(p_caso VARCHAR2, p_cond BOOLEAN) IS
  BEGIN
    IF NVL(p_cond, FALSE) THEN
      v_ok := v_ok + 1;     DBMS_OUTPUT.PUT_LINE('[OK]    ' || p_caso);
    ELSE
      v_falla := v_falla + 1; DBMS_OUTPUT.PUT_LINE('[FALLA] ' || p_caso);
    END IF;
  END sp_chk;

  FUNCTION fn_total(p_id NUMBER) RETURN NUMBER IS
    v_total NUMBER;
  BEGIN
    SELECT total INTO v_total FROM labt_pedido WHERE id_pedido = p_id;
    RETURN v_total;
  END fn_total;

  FUNCTION fn_recalculos RETURN NUMBER IS
    v_cantidad NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_cantidad FROM labt_recalculo;
    RETURN v_cantidad;
  END fn_recalculos;
BEGIN

  v_r0 := fn_recalculos;
  UPDATE labt_linea SET cantidad = 5 WHERE id_pedido = 1 AND linea = 1;
  sp_chk('T1a total pedido 1 = 5*50000 + 30000 = 280000', fn_total(1) = 280000);
  sp_chk('T1b 1 fila afectada -> 1 recalculo',            fn_recalculos - v_r0 = 1);

  v_r0 := fn_recalculos;
  INSERT INTO labt_linea (id_pedido, linea, cantidad, precio)
  SELECT 1, 3, 1, 20000 FROM DUAL
  UNION ALL
  SELECT 2, 2, 4,  5000 FROM DUAL;
  sp_chk('T2a total pedido 1 = 280000 + 20000 = 300000', fn_total(1) = 300000);
  sp_chk('T2b total pedido 2 = 30000 + 20000  =  50000', fn_total(2) = 50000);
  sp_chk('T2c 2 filas, 2 pedidos -> 2 recalculos',       fn_recalculos - v_r0 = 2);

  v_r0 := fn_recalculos;
  UPDATE labt_linea SET precio = precio * 2 WHERE id_pedido = 1;
  sp_chk('T3a total pedido 1 duplicado = 600000',        fn_total(1) = 600000);
  sp_chk('T3b 3 filas -> 1 solo recalculo (no 3)',       fn_recalculos - v_r0 = 1);


  v_r0 := fn_recalculos;
  DELETE FROM labt_linea WHERE id_pedido = 2;
  sp_chk('T4a pedido sin lineas -> total 0',             fn_total(2) = 0);
  sp_chk('T4b 2 filas borradas -> 1 recalculo',          fn_recalculos - v_r0 = 1);

  v_r0 := fn_recalculos;
  UPDATE labt_linea SET id_pedido = 2 WHERE id_pedido = 1 AND linea = 3;  
  sp_chk('T5a pedido 1 pierde la linea: 600000 - 40000 = 560000', fn_total(1) = 560000);
  sp_chk('T5b pedido 2 gana la linea: 40000',                     fn_total(2) = 40000);
  sp_chk('T5c 1 fila, 2 pedidos afectados -> 2 recalculos',       fn_recalculos - v_r0 = 2);

  ROLLBACK;

  DBMS_OUTPUT.PUT_LINE('RESUMEN: ' || v_ok || ' OK, ' || v_falla || ' FALLA');

END;
/
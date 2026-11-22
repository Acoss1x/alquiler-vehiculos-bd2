-- =====================================================================
-- Bases de Datos 2 · Taller integrador — Banco Andino
-- 01_sesion_A.sql        >>>  CONEXIÓN A  <<<   (cajero A)
--
-- NO ejecute este archivo de corrido. Cada bloque dice PASO n:
-- ejecútelo con Ctrl+Enter cuando le toque, alternando con la conexión B.
--
-- ANTES DE EMPEZAR, verifique:
--   1. Autocommit APAGADO
--      Herramientas > Preferencias > Base de datos > Avanzado > Autoconfirmar
--      (debe estar DESMARCADO)
--   2. Dos CONEXIONES distintas, no dos hojas sobre la misma conexión.
-- =====================================================================

SELECT 'CONEXIÓN A' AS ventana, SYS_CONTEXT('USERENV','SID') AS sid FROM dual;


-- #####################################################################
-- EJERCICIO 1.1 — El retiro que se pierde (actualización perdida)
--
-- Escenario: el mismo cliente retira por el cajero automático mientras
-- su empresa le hace un débito automático. Las dos operaciones leen el
-- saldo, restan en memoria y escriben el resultado. Es lo que hace la
-- mayoría del código que ustedes van a encontrar en producción.
--
-- Cuenta 1001, saldo inicial 1.000.000.
-- #####################################################################

-- PASO 1  (A)  Estado inicial
SELECT id_cuenta, saldo, saldo_retenido, version
  FROM cuenta WHERE id_cuenta = 1001;
--El saldo es 1000000 
--> Anote el SALDO. Debe ser 1.000.000.

-- PASO 2  (A)  "El cajero" leyó 1.000.000 y calculó 1.000.000 - 300.000
UPDATE cuenta SET saldo = 700000 WHERE id_cuenta = 1001;
--  NO confirme todavía.

-- ... vaya a la conexión B, PASO 3 ...

-- PASO 5  (A)  Confirmo mi retiro de 300.000
COMMIT;

-- ... conexión B, PASO 6 ...

-- PASO 8  (A)  ¿Cuánto saldo quedó?
SELECT id_cuenta, saldo FROM cuenta WHERE id_cuenta = 1001;
--
--  PREGUNTA 1.1 — Salieron 300.000 (A) más 200.000 (B) = 500.000 de
--  1.000.000. Deberían quedar 500.000. ¿Cuántos quedan? ¿Quién "ganó"?
--  ¿Hubo algún error, alguna advertencia, algún registro de que algo
--  salió mal?
--
--Quedaron 800.000. La operación de B fue la que “ganó”, porque fue la última en escribir el saldo y 
-- dejó el valor en 800.000, sobrescribiendo el cambio que había hecho A
-- No apareció ningún error, advertencia ni mensaje que indicara que algo había salido mal
-- para Oracle las dos actualizaciones se ejecutaron correctamente.
--
--  PREGUNTA 1.2 — El bloqueo de fila SÍ funcionó: B esperó a que A
--  confirmara. Explique entonces por qué el banco perdió dinero de todos
--  modos.
--
--El bloqueo de fila sí funcionó porque B tuvo que esperar a que A hiciera COMMIT. Sin embargo, 
--B ya había calculado su nuevo saldo usando el valor anterior de 1.000.000. Cuando A confirmó
--y B pudo continuar, B escribió directamente 800.000, sin tener en cuenta que A 
--ya había descontado 300.000. Por eso el bloqueo evitó que escribieran al mismo tiempo
-- pero no evitó que B trabajara con un dato desactualizado.
--
--  PREGUNTA 1.3 — En un marketplace este error vende una unidad de más.
--  En un banco, ¿de quién es la plata que se acaba de crear de la nada,
--  y quién la va a reclamar?
--
--La diferencia de 300.000 termina siendo una pérdida para el banco, porque la cuenta quedó mostrando más dinero del que realmente debería tener. 
-- El cliente verá ese saldo como disponible y puede reclamarlo o retirarlo, aunque contablemente ya se hayan realizado las dos operaciones
-- Por eso el banco sería quien tendría que asumir el descuadre.

-- #####################################################################
-- EJERCICIO 1.2 — Defensa pesimista: SELECT ... FOR UPDATE
-- #####################################################################

-- PASO 1  (A)  Dejo la cuenta en un valor conocido
UPDATE cuenta SET saldo = 1000000, version = 0 WHERE id_cuenta = 1001;
COMMIT;

-- PASO 2  (A)  Leo Y bloqueo en la misma operación
SELECT id_cuenta, saldo
  FROM cuenta
 WHERE id_cuenta = 1001
   FOR UPDATE;

-- ... conexión B, PASO 3 ...

-- PASO 5  (A)  Descuento mis 300.000 y libero
UPDATE cuenta SET saldo = saldo - 300000 WHERE id_cuenta = 1001;
COMMIT;

-- ... conexión B, PASO 6 ...

-- PASO 8  (A)  Resultado
SELECT id_cuenta, saldo FROM cuenta WHERE id_cuenta = 1001;
--
--  PREGUNTA 1.4 — Ahora sí quedan 500.000. ¿Qué dos cosas cambiaron
--  respecto al ejercicio anterior? (pista: una es el FOR UPDATE, la otra
--  está dentro del UPDATE mismo)
--
--Cambiaron dos cosas. Primero, se utilizó SELECT ... FOR UPDATE para bloquear la fila antes de trabajar con ella,
--evitando que otra transacción pudiera modificarla .  Luego , el UPDATE se realizó utilizando saldo = saldo - valor 
-- en lugar de escribir un saldo calculado anteriormente. De esta manera, la segunda transacción trabaja sobre el saldo
-- actualizado por la primera y el resultado final es correcto: $500.000. 
--
--  PREGUNTA 1.5 — ¿Cuál fue el precio que se pagó por esta seguridad?
--  Piense en una cuenta empresarial de recaudo que recibe cientos de
--  consignaciones por minuto.
--
--El precio fue la espera producida por la contención. Si una cuenta empresarial recibe cientos de consignaciones por minuto, 
--muchas transacciones pueden intentar modificar la misma fila al mismo tiempo.Solo una puede mantener el bloqueo y las
--demás deben esperar a que se libere. Esto puede aumentar los tiempos de respuesta y disminuir el rendimiento,
-- aunque a cambio se mantiene la consistencia del saldo.
--
-- #####################################################################
-- EJERCICIO 1.3 — Defensa optimista: columna de versión
--
-- Nadie bloquea nada. El choque se detecta al escribir.
-- #####################################################################

-- PASO 1  (A)  Reinicio
UPDATE cuenta SET saldo = 1000000, version = 0 WHERE id_cuenta = 1001;
COMMIT;

-- PASO 2  (A)  Leo el saldo y la versión.  NO bloqueo nada.
SELECT id_cuenta, saldo, version FROM cuenta WHERE id_cuenta = 1001;
--> Anote la versión que ve. Debe ser 0.

-- ... conexión B, PASO 3: lee lo mismo y escribe PRIMERO ...

-- PASO 5  (A)  Intento escribir con la versión que leí
UPDATE cuenta
   SET saldo   = saldo - 300000,
       version = version + 1
 WHERE id_cuenta = 1001
   AND version   = 0;              -- <-- la versión que leí en el PASO 2
COMMIT;
--
--  Mire el mensaje: ¿cuántas filas actualizó? 1 fila se actualizo

-- PASO 6  (A)
SELECT id_cuenta, saldo, version FROM cuenta WHERE id_cuenta = 1001;
--
--  PREGUNTA 1.6 — Su UPDATE afectó 0 filas. ¿Qué debe hacer la
--  aplicación en ese momento, y qué NO debe hacer nunca?
--
--Cuando el UPDATE afecta 0 filas, la aplicación debe detectar que ocurrió un conflicto de concurrencia, 
-- hacer ROLLBACK o cancelar la operación, volver a consultar los datos actuales y, si corresponde, reintentar la operación.
--
--Lo que nunca debe hacer es ignorar que se actualizaron 0 filas y continuar como si la operación hubiera sido exitosa, 
--porque el saldo no se habría modificado correctamente.
--
--  PREGUNTA 1.7 — Compare las dos defensas. ¿En qué producto del banco
--  usaría la pesimista y en cuál la optimista? Justifique con el nivel
--  de contención esperado, no con opiniones.
--
--La defensa pesimista (SELECT ... FOR UPDATE) la usaría en una cuenta empresarial de recaudo que recibe cientos de consignaciones por minuto, porque se espera un nivel
--alto de contención sobre la misma fila. En este caso, es preferible bloquear el registro y hacer que las demás transacciones esperen, evitando conflictos de actualización.
--
--La defensa optimista (control mediante version) la usaría en un producto donde se espera baja contención, especialmente si hay muchas lecturas y pocas
--modificaciones simultáneas. En este caso, no es necesario bloquear desde el principio
--se permite trabajar y solo se detecta el conflicto cuando otra transacción ya cambió la versión.

-- #####################################################################
-- EJERCICIO 1.4 — Repartir la cola de compensación: SKIP LOCKED
--
-- Dos procesos de compensación toman transferencias de la misma cola.
-- No pueden procesar la misma dos veces, y no pueden quedarse esperando
-- el uno al otro: la ventana de compensación se cierra a una hora fija.
-- #####################################################################

-- PASO 1  (A)  Tomo 5 tareas y las marco como mías
SELECT id_tarea, id_transferencia
  FROM tarea_compensacion
 WHERE estado = 'PENDIENTE'
   AND ROWNUM <= 5
   FOR UPDATE SKIP LOCKED;
--  Nota de sintaxis: aquí se usa ROWNUM y no FETCH FIRST ... ROWS ONLY.
--  Oracle implementa FETCH FIRST como una vista en línea, y combinarlo
--  con FOR UPDATE devuelve ORA-02014. Para una cola de trabajo ROWNUM es
--  además lo correcto: no importa CUÁLES cinco tareas toma, importa que
--  sean cinco libres y que nadie más las tenga.
--> Anote los id_tarea que le tocaron.  NO confirme todavía.

-- ... conexión B, PASO 2: pide 5 tareas y recibe OTRAS distintas ...

-- PASO 3  (A)  Marco las mías como compensadas y libero
UPDATE tarea_compensacion
   SET estado = 'COMPENSADA', tomada_por = 'PROCESO_A', fecha_toma = SYSTIMESTAMP
 WHERE id_tarea IN (1,2,3,4,5);      -- <-- reemplace por los ids que le tocaron
COMMIT;

-- PASO 5  (A)  Verificación conjunta
SELECT tomada_por, COUNT(*) AS tareas
  FROM tarea_compensacion WHERE estado = 'COMPENSADA'
 GROUP BY tomada_por;
--
--  PREGUNTA 1.8 — ¿Qué habría pasado sin SKIP LOCKED? Describa los dos
--  comportamientos posibles según se use FOR UPDATE simple o NOWAIT.
--
--Sin SKIP LOCKED, si se utilizara FOR UPDATE simple, una segunda sesión que intentara tomar una tarea que ya está bloqueada tendría
--que esperar hasta que la primera sesión liberara el bloqueo mediante COMMIT o ROLLBACK.
--
--Si se utilizara FOR UPDATE NOWAIT, la segunda sesión no esperaría, sino que recibiría inmediatamente un error porque la fila está bloqueada. 
--
--Con SKIP LOCKED, en cambio, la segunda sesión puede saltar las tareas bloqueadas y tomar otras disponibles, sin esperar ni generar el error de NOWAIT.
--
--
--  PREGUNTA 1.9 — En una compensación real, procesar dos veces la misma
--  transferencia significa mover la plata dos veces. ¿Qué le garantiza
--  SKIP LOCKED que no le garantizaría un reparto hecho en la aplicación
--  con un "WHERE MOD(id,2) = 0" para un proceso y "= 1" para el otro?
--
--SKIP LOCKED garantiza que, cuando una sesión toma una tarea mediante FOR UPDATE, la fila queda bloqueada en la base de datos y
--otra sesión que intente seleccionar tareas con SKIP LOCKED la saltará en lugar de tomarla simultáneamente.
--De esta forma, la propia base de datos coordina a los procesos concurrentes.
--
--En cambio, un reparto realizado en la aplicación mediante WHERE MOD(id,2) = 0 y WHERE MOD(id,2) = 1
-- solamente divide las filas según su número; no crea un bloqueo en la base de datos. Por lo tanto, no proporciona la misma protección frente a accesos concurrentes.
--
--Por eso, SKIP LOCKED es una defensa de concurrencia basada en el bloqueo real de Oracle, 
--mientras que el reparto con MOD es solamente una estrategia lógica de distribución del trabajo.

-- #####################################################################
-- EJERCICIO 1.5 — El bloqueo que nadie espera: FK sin índice
--
-- NOTIFICACION.ID_TRANSFERENCIA es llave foránea hacia TRANSFERENCIA
-- y NO está indexada.
-- #####################################################################

-- PASO 1  (A)  Registro la notificación de la transferencia 900001.
--   NO confirmo.
INSERT INTO notificacion (id_notificacion, id_transferencia, canal, enviada)
VALUES (9000001, 900001, 'SMS', 'S');

-- ... conexión B, PASO 2: intenta borrar la transferencia 900005, que no
--     tiene ninguna notificación ni ningún movimiento, y se queda esperando ...

-- PASO 3  (A)  Mientras B espera, mire el bloqueo.
--   TX = bloqueo de fila.  TM = bloqueo de TABLA.
SELECT l.sid, l.type,
       DECODE(l.lmode, 0,'ninguno',1,'null',2,'RS',3,'RX',4,'S',5,'SRX',6,'X') AS tiene,
       DECODE(l.request,0,'-',1,'null',2,'RS',3,'RX',4,'S',5,'SRX',6,'X')      AS pide
  FROM v$lock l JOIN v$session s ON s.sid = l.sid
 WHERE s.username = USER AND l.type IN ('TX','TM');
--> Si no tiene permisos sobre V$, el docente lo proyecta.

-- PASO 4  (A)  Libero
COMMIT;

-- PASO 6  (A)  Creo el índice que faltaba
CREATE INDEX ix_notif_transferencia ON notificacion (id_transferencia);

-- PASO 7  (A)  Repitan los pasos 1 y 2. Ahora B NO se bloquea.
--
--  PREGUNTA 1.10 — Explique por qué Oracle toma un bloqueo de TABLA sobre
--  el hijo cuando se borra un padre y la FK no está indexada.
--
--Cuando se elimina un registro padre, Oracle debe comprobar que no existan registros hijos que lo referencien mediante la llave foránea. Si la FK del hijo no está indexada, 
--Oracle no tiene un índice que le permita localizar rápidamente esos registros y puede necesitar tomar un bloqueo 
--más amplio sobre la tabla hija para garantizar la integridad referencial.
--
--Por eso, una FK sin índice puede provocar bloqueos inesperados cuando se eliminan o actualizan registros de la tabla padre, especialmente cuando existe concurrencia. 
--
--
--  PREGUNTA 1.11 — Revise el esquema completo del Banco Andino. ¿Qué
--  OTRAS llaves foráneas están sin índice? ¿Cuáles de ellas son realmente
--  un problema en este negocio y cuáles no? Justifique pensando en qué
--  se borra y qué no se borra nunca en un banco.
--
--Además de NOTIFICACION.ID_TRANSFERENCIA, las llaves foráneas que inicialmente están sin índice son: 
--CATALOGO_PRODUCTO.ID_PADRE 
--CUENTA.ID_CLIENTE 
--CUENTA.ID_PRODUCTO 
--CUENTA.ID_SUCURSAL 
--TRANSFERENCIA.CUENTA_ORIGEN 
--TRANSFERENCIA.CUENTA_DESTINO 
--MOVIMIENTO.ID_CUENTA 
--MOVIMIENTO.ID_TRANSFERENCIA no se incluye porque el esquema ya lo deja indexado mediante IX_MOV_TRANSFERENCIA. 
--
--Las FK que requieren mayor atención son TRANSFERENCIA.CUENTA_ORIGEN, TRANSFERENCIA.CUENTA_DESTINO y MOVIMIENTO.ID_CUENTA, porque están
--relacionadas directamente con las operaciones transaccionales del banco y pueden participar en escenarios de alta concurrencia.
--
--Las FK de CUENTA hacia CLIENTE, CATALOGO_PRODUCTO y SUCURSAL, y la FK CATALOGO_PRODUCTO.ID_PADRE, representan un problema menor para este caso 
--específico, porque en la operación normal de un banco no se están borrando continuamente clientes, productos, sucursales o elementos del catálogo. El riesgo de 
--bloqueo por DELETE del padre depende principalmente de que realmente se realicen borrados o actualizaciones de claves padre bajo concurrencia.
--
--Por lo tanto, no se debe concluir que toda FK sin índice necesita obligatoriamente un índice. 
--La decisión debe basarse en el patrón real de operaciones: qué padres se borran o actualizan, con qué frecuencia y con qué nivel de concurrencia.
--
-- #####################################################################
-- EJERCICIO 1.6 — Interbloqueo: las dos transferencias cruzadas
--
-- Este es el caso clásico de la banca, y no es hipotético: ocurre todos
-- los días. Dos clientes se transfieren plata mutuamente al mismo tiempo.
-- #####################################################################

-- Antes de empezar, deje las dos cuentas en un valor conocido
UPDATE cuenta SET saldo = 1000000, version = 0 WHERE id_cuenta IN (1001, 1002);
COMMIT;

-- PASO 1  (A)  Transferencia de la 1001 a la 1002: primero debito el origen
UPDATE cuenta SET saldo = saldo - 50000 WHERE id_cuenta = 1001;

-- ... conexión B, PASO 2: hace la transferencia inversa, 1002 -> 1001,
--     y debita PRIMERO la 1002 ...

-- PASO 3  (A)  Ahora acredito el destino: la cuenta 1002, que B ya tiene
UPDATE cuenta SET saldo = saldo + 50000 WHERE id_cuenta = 1002;
--> Me quedo esperando.

-- ... conexión B, PASO 4: pide la 1001, que yo tengo -> ciclo cerrado ...

--> En segundos, UNA de las dos recibe:
--     ORA-00060: deadlock detected while waiting for resource

ROLLBACK;
--
--  PREGUNTA 1.12 — Oracle abortó UNA sentencia, no la transacción
--  completa. ¿En qué estado queda la sesión víctima y qué debe hacer?
--  ¿Qué pasa si la aplicación asume que Oracle deshizo todo?
--
--La sesión víctima que recibe ORA-00060 queda con la sentencia que provocó el deadlock abortada, pero Oracle no necesariamente deshace toda la transacción. Por 
--eso, la aplicación debe detectar el error y realizar el ROLLBACK correspondiente para limpiar la transacción y liberar los bloqueos.
--
--Si la aplicación asume que Oracle deshizo toda la transacción, podría continuar creyendo que ninguna operación anterior se realizó, cuando en realidad puede haber 
--cambios pendientes. Esto puede dejar la operación de negocio en un estado inconsistente. Por eso, después de un ORA-00060, la aplicación debe manejar 
--explícitamente el error y decidir si hace rollback y reintenta la operación.
--
--  PREGUNTA 1.13 — Escriba la regla de diseño, en una frase, que habría
--  hecho imposible este interbloqueo. Después dígala en términos de
--  código: ¿qué línea concreta hay que agregar antes de los dos UPDATE?
--
--Todas las transacciones deben adquirir los bloqueos de los recursos en un orden determinístico y consistente. Por ejemplo, si se van a modificar las cuentas 1001 y 
--1002, todas las transacciones deben bloquear primero la cuenta 1001 y después la 1002
--
--En código, antes de los dos UPDATE se debe agregar: 

--SELECT id_cuenta, saldo 
--FROM cuenta 
--WHERE id_cuenta IN (1001, 1002) 
--ORDER BY id_cuenta 
--FOR UPDATE; 
--
--De esta forma, ambas sesiones intentan adquirir los bloqueos en el mismo orden y se evita el ciclo de espera que produce el deadlock. 
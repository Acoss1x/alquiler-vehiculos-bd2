#!/usr/bin/env bash
# Crea la base de datos completa en el orden correcto y corre las pruebas.
#
# ORDEN (importa): 04 y 05 dependen de lo que agrega 07, por eso van después de 07.
#   01 tablas -> 02 funciones/procedimientos -> 03 triggers -> 07 cambios de fase 2 ->
#   04 datos de prueba -> 05 roles -> 09 cambios del backend -> 06 y 08 pruebas
#
# Uso (la base debe existir y estar VACÍA; para repetir: dropdb y createdb primero):
#   PGHOST=192.168.56.10 PGUSER=admindb PGDATABASE=alquiler_vehiculos ./ejecutar_todo.sh
set -euo pipefail
cd "$(dirname "$0")"
export PGOPTIONS='-c client_min_messages=warning'

ORDEN=(01_creacion 02_funciones_procedimientos 03_triggers 07_cambios_fase2 \
       04_datos_prueba 05_seguridad 09_cambios_backend 06_pruebas 08_pruebas_fase2)

for f in "${ORDEN[@]}"; do
    printf '>>> %-32s' "$f.sql"
    psql -v ON_ERROR_STOP=1 -q -f "$f.sql" > /dev/null
    echo "OK"
done
echo "Base de datos lista y pruebas superadas."

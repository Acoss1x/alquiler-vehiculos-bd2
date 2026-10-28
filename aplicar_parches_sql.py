#!/usr/bin/env python3
"""
Parcha 04_datos_prueba.sql, 05_seguridad.sql y 06_pruebas.sql para que
coincidan con el esquema nuevo (07_cambios_fase2.sql).

Uso (desde la raíz del repo):
    python aplicar_parches_sql.py            # modifica los archivos en database/
    python aplicar_parches_sql.py --simular  # solo muestra qué cambiaría

Cada reemplazo se verifica: si el texto esperado no aparece el número de veces
previsto, el script se detiene SIN escribir nada. Se puede repetir sin dañar
nada (lo que ya está parchado se omite).
"""
import re
import sys
from pathlib import Path

DB = Path("database")
CLAVE = "crypt('Clave123*', gen_salt('bf', 10))"
CLAVE_ESC = "crypt(''Clave123*'', gen_salt(''bf'', 10))"   # dentro de un string SQL

EMPLEADOS = {
    "1010000001": "marta.rios",
    "1010000002": "camilo.pardo",
    "1010000003": "valentina.mora",
    "1010000004": "julian.cortes",
    "1010000005": "sofia.lara",
}


def leer(nombre):
    with open(DB / nombre, encoding="utf-8", newline="") as f:
        return f.read()


def exigir(nombre, que, n, esperado):
    if n != esperado:
        sys.exit(f"ERROR en {nombre}: '{que}' aparece {n} vez/veces y se esperaba {esperado}. "
                 f"No se escribió nada.")


def parchar_llamadas(texto):
    """Agrega el hash a las líneas 'CALL sp_registrar_cliente(...);' (8.º parámetro)."""
    lineas = texto.split("\n")
    n = 0
    for i, linea in enumerate(lineas):
        cuerpo = linea.rstrip("\r")
        fin = linea[len(cuerpo):]
        if (cuerpo.lstrip().startswith("CALL sp_registrar_cliente(")
                and cuerpo.rstrip().endswith(");") and "crypt(" not in cuerpo):
            lineas[i] = cuerpo.rstrip()[:-2] + ", " + CLAVE + ");" + fin
            n += 1
    return "\n".join(lineas), n


def parche_04(t):
    # Empleados: agregar correo y password_hash.
    cambios = 0
    if "correo, password_hash, sede_id_sede" not in t:
        t, n = re.subn(r"fecha_contratacion, estado, sede_id_sede\)",
                       "fecha_contratacion, estado, correo, password_hash, sede_id_sede)", t)
        exigir("04", "lista de columnas de empleado", n, 5)
        cambios += n
        for doc, correo in EMPLEADOS.items():
            patron = re.compile(r"(VALUES \('%s', '[^']*', '[^']*', DATE '[^']*', 'ACTIVO',)" % doc)
            t, n = patron.subn(r"\1 '%s@alquiler.com', %s," % (correo, CLAVE), t)
            exigir("04", f"empleado {doc}", n, 1)
            cambios += n
    # Clientes: hash como 8.º parámetro.
    t, n = parchar_llamadas(t)
    if n:
        exigir("04", "CALL sp_registrar_cliente", n, 4)
    return t, cambios + n


def parche_05(t):
    if re.search(r"sp_registrar_cliente\((?:VARCHAR, ){7}VARCHAR\)", t):
        return t, 0
    patron = re.compile(r"sp_registrar_cliente\((?:VARCHAR, ){6}VARCHAR\)")
    t, n = patron.subn("sp_registrar_cliente(" + ", ".join(["VARCHAR"] * 8) + ")", t)
    exigir("05", "GRANT de sp_registrar_cliente", n, 1)
    return t, n


def parche_06(t):
    total = 0
    # Llamadas dentro de strings (esperar_error): hash como último argumento.
    for correo in ("''otro@correo.com'', NULL)'", "''ANDRES.GOMEZ@correo.com'', NULL)'"):
        nuevo = correo[:-2] + ", " + CLAVE_ESC + ")'"
        if nuevo in t:
            continue
        n = t.count(correo)
        exigir("06", correo, n, 1)
        t = t.replace(correo, nuevo)
        total += n
    # Llamadas directas (777 y 888).
    t, n = parchar_llamadas(t)
    if n:
        exigir("06", "CALL sp_registrar_cliente directo", n, 2)
    total += n
    # Empleado huérfano: ahora exige correo y password_hash.
    viejo = ("fecha_contratacion, sede_id_sede) VALUES (''HUERFANO'', ''A'', ''B'', current_date, %s)")
    nuevo = ("fecha_contratacion, correo, password_hash, sede_id_sede) VALUES (''HUERFANO'', ''A'', ''B'', "
             "current_date, ''huerfano@alquiler.com'', crypt(''x'', gen_salt(''bf'', 4)), %s)")
    if nuevo not in t:
        exigir("06", "empleado HUERFANO", t.count(viejo), 1)
        t = t.replace(viejo, nuevo)
        total += 1
    return t, total


def main():
    simular = "--simular" in sys.argv
    if not DB.is_dir():
        sys.exit("Ejecuta el script desde la raíz del repo (debe existir la carpeta database/).")
    resultados = {}
    for nombre, funcion in (("04_datos_prueba.sql", parche_04),
                            ("05_seguridad.sql", parche_05),
                            ("06_pruebas.sql", parche_06)):
        original = leer(nombre)
        nuevo, cambios = funcion(original)
        resultados[nombre] = (nuevo, cambios)
    for nombre, (nuevo, cambios) in resultados.items():
        print(f"{nombre}: {cambios} cambio(s)" + ("" if cambios else "  (ya estaba parchado)"))
        if cambios and not simular:
            with open(DB / nombre, "w", encoding="utf-8", newline="") as f:
                f.write(nuevo)
    if simular:
        print("Modo simulación: no se escribió nada.")


if __name__ == "__main__":
    main()

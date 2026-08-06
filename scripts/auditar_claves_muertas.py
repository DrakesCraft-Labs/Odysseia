#!/usr/bin/env python3
"""Compara las claves del config.yml de Odysseia con las que el codigo realmente lee.

Una clave escrita en el config que el codigo no lee nunca es configuracion muerta: alguien la puso
esperando un efecto que no ocurre. El caso tipico es un nombre mal escrito o renombrado en el
codigo sin tocar el archivo, y no da ningun error: Bukkit devuelve el valor por defecto y sigue.

Uso: claves_muertas.py <ruta al repo Odysseia> <config.yml desplegado>
"""
import re
import sys
from pathlib import Path

import yaml

repo = Path(sys.argv[1])
config_desplegado = Path(sys.argv[2])

# getBoolean("a.b"), getString("a.b", ...), getConfigurationSection("a.b"), get("a.b")
LECTURA = re.compile(
    r'get(?:Boolean|String|Int|Long|Double|StringList|IntegerList|ConfigurationSection|Keys|List|)\s*\(\s*"([^"]+)"')

leidas = set()
for fuente in repo.rglob("src/main/java/**/*.java"):
    texto = fuente.read_text(encoding="utf-8", errors="replace")
    for m in LECTURA.finditer(texto):
        ruta = m.group(1)
        # Solo interesan las que parecen rutas de config, no mensajes ni ids sueltos.
        if re.fullmatch(r"[a-z0-9][a-z0-9._-]*", ruta):
            leidas.add(ruta)

def ramas(nodo, prefijo=""):
    if isinstance(nodo, dict):
        for clave, valor in nodo.items():
            ruta = f"{prefijo}.{clave}" if prefijo else str(clave)
            yield ruta
            yield from ramas(valor, ruta)

with config_desplegado.open(encoding="utf-8", errors="replace") as fichero:
    datos = yaml.safe_load(fichero)

presentes = set(ramas(datos))

def leida(ruta):
    """La clave cuenta como viva si el codigo la nombra, o nombra a un ancestro suyo (se puede leer
    una seccion entera con getConfigurationSection), o nombra a alguna de sus hijas (leer
    'reinicio.segundos' mantiene viva a 'reinicio')."""
    partes = ruta.split(".")
    for corte in range(len(partes), 0, -1):
        if ".".join(partes[:corte]) in leidas:
            return True
    prefijo = ruta + "."
    return any(l.startswith(prefijo) for l in leidas)

# Solo se reportan las hojas de primer y segundo nivel: si una seccion entera esta muerta, no hace
# falta listar sus 40 hijas.
muertas = sorted(r for r in presentes if not leida(r) and r.count(".") <= 1)
# Y se quita lo que sea hija de otra ya reportada.
final = [r for r in muertas if not any(r.startswith(o + ".") for o in muertas if o != r)]

print(f"claves leidas por el codigo: {len(leidas)}")
print(f"claves presentes en el config desplegado: {len(presentes)}")
print(f"\nSecciones/claves de primer o segundo nivel que el codigo NUNCA lee ({len(final)}):\n")
for ruta in final:
    print("   ", ruta)

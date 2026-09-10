# Manifiesto verificable de lotes SAORI

Odysseia puede anunciar un lote como verificado únicamente cuando encuentra
`release-manifest.json` y valida su firma HMAC-SHA256 con el contenido privado de
`release-manifest.secret`. Los nombres se pueden cambiar bajo `discord` en
`config.yml`, pero siempre se resuelven dentro del directorio de datos de
Odysseia.

La integración debe escribir un archivo temporal, hacer `fsync` y renombrarlo
atómicamente al nombre final. Odysseia nunca genera ni modifica el manifiesto.
El secreto tiene que contener al menos 32 bytes y no debe entrar al repositorio.

## Contrato v1

```json
{
  "payload": {
    "schema": 1,
    "releaseId": "saori-2026.09.10-001",
    "state": "VERIFIED",
    "playerSummary": "Las máquinas vuelven a procesar lotes completos.",
    "technicalDetails": [
      {
        "ticket": 123,
        "commit": "abcdef1",
        "artifact": "Example.jar",
        "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "backupVerified": true,
        "validation": "Pruebas locales y salud postarranque correctas"
      }
    ],
    "health": {
      "status": "HEALTHY",
      "summary": "Arranque y comprobaciones funcionales correctos"
    }
  },
  "signature": "HMAC_SHA256_HEX_DEL_PAYLOAD_CANONICO"
}
```

La representación canónica conserva ese orden fijo de campos, no agrega
espacios y conserva el orden de `technicalDetails`. La firma cubre solamente el
objeto `payload`, serializado en UTF-8, y se escribe como 64 caracteres
hexadecimales en minúscula. El productor debe implementar el mismo orden que
`ReleaseManifest.canonical`.

No se admiten campos adicionales. El contrato no tiene campos para jugador,
UUID, mundo, coordenadas, texto de chat ni rutas de respaldo. `artifact` es sólo
el nombre base del JAR; del respaldo se publica exclusivamente el booleano de
verificación. Los textos también se rechazan si contienen UUID, IP, coordenadas,
rutas privadas, webhooks o caracteres de control.

Los estados distintos de `VERIFIED`, incluidas preparaciones `STAGED` y
reversiones `ROLLED_BACK`, no se publican. El último `releaseId` encolado queda
registrado atómicamente en caché para deduplicar reinicios. Si no hay manifiesto
publicable, el escaneo anterior de JAR se conserva únicamente como observación
de un delta binario y jamás afirma que un parche fue aplicado o verificado.

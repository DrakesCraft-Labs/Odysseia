# Motor de Compras de Odysseia

Estado: diseno validado el 2026-07-10. No modifica paquetes Tebex.

## Hechos verificados

- Tebex admite `{transaction}` en Console Commands; es el identificador unico de
  la transaccion. `{transaction_id}` no es la variable documentada.
- Tebex vuelve a ejecutar comandos al reenviarlos. La entrega debe ser idempotente.
- Existen 23 paquetes activos en el store y todos estan mapeados por el portal.
- El `StoreManager` actual no es apto como motor transaccional: consulta una cola
  JSON, ejecuta comandos opacos, confirma al final y no persiste acciones ni
  reintentos por accion.
- La configuracion fuente y la desplegada de Odysseia no son iguales. No se debe
  migrar ninguna recompensa hasta comparar el archivo activo por SFTP/Pterodactyl.

Fuentes: [Tebex Commands](https://docs.tebex.io/creators/command-management/an-introduction-to-commands),
[Tebex Plugin API](https://docs.tebex.io/plugin/endpoints/command-queue).

## Arquitectura propuesta

`Tebex package command -> odysseiapurchase deliver {username} <product-id> {transaction}`

El comando solo acepta consola o `odysseia.purchase.admin`. `product-id` se
valida contra un registro cerrado y `transaction` es obligatorio. El motor se
divide en:

- `PurchaseCommand`: entrada validada y comandos administrativos.
- `ProductRegistry`: productos estables y acciones tipadas.
- `DeliveryRepository`: SQLite local con migraciones.
- `PurchaseService`: idempotencia, estados y coordinacion.
- `DeliveryExecutor`: ejecuta solo acciones pendientes.
- `PlayerJoinDeliveryListener`: retoma acciones que necesitan jugador online.
- `PurchaseAuditService`: auditoria y anuncio final unico.

No se acepta una cadena arbitraria de consola como producto. Las integraciones
preferidas son LuckPerms API y Vault; comandos externos quedan como accion
controlada y con parametros cerrados.

## Persistencia e idempotencia

Clave unica: `provider = TEBEX` + `transaction_id` + `product_id`. Una compra
con varios paquetes genera una entrega por producto, sin confundir dos compras
legitimas iguales. Tablas iniciales:

- `purchase_deliveries`: id, provider, transaction_id, player_name, player_uuid,
  product_id, estado, intentos, ultimo_error, recibido_en, actualizado_en,
  completado_en.
- `purchase_actions`: delivery_id, posicion, tipo, estado, intentos, ultimo_error,
  inicio_en, completado_en.
- `purchase_audit`: delivery_id, actor, evento, detalle seguro, creado_en.

Estados: `RECEIVED`, `WAITING_FOR_PLAYER`, `PROCESSING`,
`PARTIALLY_DELIVERED`, `COMPLETED`, `FAILED_RETRYABLE`,
`FAILED_MANUAL_REVIEW`, `REFUNDED`, `CHARGEBACK`.

Un indice unico impide una segunda entrega antes de ejecutar acciones. Solo se
reanuda una accion pendiente o reintentable; dinero, rango e items ya marcados
como completos nunca se repiten. El anuncio global y Discord ocurren una vez,
despues de completar las acciones obligatorias.

## Catalogo a migrar

Automatizables con validacion de configuracion activa:

- VIP: `vip_hercules`, `vip_hestia`, `vip_hermes`, `vip_hefesto`,
  `vip_artemisa`, `vip_afrodita`, `vip_zeus`.
- Roles: `role_minero`, `role_cazador`, `role_constructor`, `role_lenador`,
  `role_alquimista`, `role_nomada`.
- Economia: `dragmas_saco`, `dragmas_cofre`, `dragmas_anfora`.
- Paquetes: `kit_hermes`, `kit_zeus`, `protection_177`, `protection_481`,
  `economy_premium`, `sfmaster_1h`, `sfmaster_24h`.

No vender desde Tebex: `custom_kit`, `custom_slimefun`, `guild_pack`; ya fueron
retirados del catalogo publico. Los productos in-game tampoco entran al motor.

Cada rango temporal debe usar LuckPerms con acumulacion de 30 dias y rechazar
una degradacion de rango superior. Antes de codificar sus acciones se compararan
grupos, kits, ProtectionStones y montos contra el `config.yml` activo.

## Offline, refunds y chargebacks

Las acciones offline (LuckPerms y economia si su proveedor lo soporta) se hacen
al recibir la compra. Kits, inventario y mensajes esperan al join. Un join no
repite acciones completadas.

Refund y chargeback entran mediante comandos de evento de Tebex y/o webhook
firmado, por ejemplo:

`odysseiapurchase refund {username} <product-id> {transaction}`

Los rangos temporales y pases son revocables. Moneda, kits reclamados e items
consumibles pasan a revision manual: nunca se eliminan automaticamente.

## Administracion y pruebas

Comandos propuestos: `status`, `pending`, `retry`, `history`, `review`,
`complete`, `cancel`, `validate` y `dry-run` bajo `odysseia.purchase.admin`.

Pruebas requeridas: nueva compra, reenvio Tebex, recompra legitima, offline,
reinicio, fallo intermedio, rango superior, dependencia ausente, refund,
chargeback y anuncio unico. Se usara la herramienta de pago manual de Tebex,
nunca pagos reales.

## Migracion de panel

1. Respaldar comandos actuales por paquete desde el panel y verificar el
   `config.yml` activo.
2. Compilar, desplegar en staging y ejecutar `dry-run` por cada producto.
3. Cambiar un paquete por vez a un unico comando `deliver` con `{transaction}`.
4. Probar con el pago manual de Tebex, comprobar la fila SQLite y la idempotencia.
5. Mantener los comandos previos en el respaldo hasta cerrar la migracion.

No se cambia el panel sin aprobacion explicita de Jack.

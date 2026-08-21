# 🏰 Propuesta de Modalidad Clásica — Documento de Diseño y Arquitectura (Isaac & Jack)

**Fecha:** 21 de Agosto de 2026  
**Modalidad:** Survival Clásico (Semi-Vanilla RPG / Towny)  
**Objetivo:** Crear una alternativa inmersiva, balanceada y semi-vanilla al Survival Slimefun técnico, enfocada en comunidad, crafteos escalonados, ciudades y economía dinámica.

---

## 🧭 1. Visión y Pilares de Jugabilidad

1. **Escala de Poder Controlada (Semi-Vanilla Peligroso):**
   * El daño ambiental, caídas y monstruos deben representar un reto real.
   * Sin ítems absurdamente inmortales que rompan la curva de supervivencia.
   * Granjas vanilla de hierro, comida y aldeanos **100% funcionales y valiosas**.

2. **Cadenas de Crafteo Escalonadas (Tiers Progresivos):**
   * Los mejores equipamientos no se obtienen de inmediato; requieren una cadena de materiales previos.
   * *Ejemplo de progresión de armaduras:*
     $$\text{Hierro Vanilla} \longrightarrow \text{Acero} \longrightarrow \text{Acero con Cuero Tratado} \longrightarrow \text{Malla Reforzada} \longrightarrow \text{Titanio}$$
   * El equipo endgame representa horas de trabajo y materiales acumulados, haciendo que el PvP y las recompensas tengan alto valor.

---

## 🏛️ 2. Sistema de Ciudades y Terrenos (Towny)

* **Fundación de Ciudades:** `/t new <nombre>` con costo de entrada (aprox. ₯20,000).
* **Reclamo por Chunks:** Reclamos mediante `/t claim` en el chunk donde está parado el jugador.
* **Escala por Residentes:** Más residentes permiten más chunks reclamables.
* **Bonus Chunks Comprables:**
  * Para alcaldes que prefieren jugar en solitario o con grupos reducidos, se habilita la compra de tickets/bonus de terreno por **₯5,000** cada uno (`/t buy bonus`).
* **Mundos y Reinicios:**
  * **Overworld:** 100% persistente y protegido durante toda la vida del servidor.
  * **Nether y End:** Mundos de recursos con reinicios periódicos programados (semanal / quincenal) para evitar el agotamiento de recursos naturales.

---

## 💎 3. Encantamientos y Crafteos Personalizados

* **Encantamientos Especiales:**
  * Sistema basado en esferas / pergaminos adquiribles con puntos de experiencia (XP) para personalizar armas y herramientas.
* **Herramienta de Crafteos (CustomCrafting / GUI In-Game):**
  * Uso de interfaz gráfica (`/create recipe` o editor in-game) para crear recetas en mesas de crafteo, hornos, ahumadores y yunques.
  * Visualizador para jugadores con `/recipes` o `/crafts` para consultar árboles de materiales.

---

## 📈 4. Economía y Mercado Dual

1. **Subastas entre Jugadores (`/ah`):**
   * Mercado libre donde los jugadores ponen sus propios precios (`/ah sell <precio>`).
   * Permite identificar qué objetos tienen mayor demanda en la comunidad.
2. **Tienda del Administrador Dinámica (`/shop`):**
   * **Oferta y Demanda Real:**
     * Si los jugadores compran masivamente un bloque (ej. lana azul), el precio unitario **sube**.
     * Si los jugadores saturan el mercado vendiendo montones de un material (ej. diamantes), el precio de venta **baja**.
   * Garantiza stock infinito para construcción básica mientras regula la inflación económica.

---

## 🛠️ 5. Plan de Trabajo y Entorno para Isaac

* **Mundo de Desarrollo (`dev_clasico`):**
  * Creación de un mundo aislado en modo creativo para que Isaac pueda diseñar físicamente las recetas en marcos de ítems, testear los tiers y registrar las recetas en el plugin sin interferencias.
* **Permisos:**
  * Asignación de rango / permisos de editor de crafteos en el mundo de staging.

---

*Documento guardado para revisión y desarrollo conjunto.*

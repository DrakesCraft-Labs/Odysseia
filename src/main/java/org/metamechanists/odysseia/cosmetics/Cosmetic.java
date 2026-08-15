package org.metamechanists.odysseia.cosmetics;

import java.util.List;
import java.util.Locale;

/**
 * Catalogo de cosmeticos. Cada entrada declara a que rango pertenece, para que el permiso y lo
 * que anuncia la tienda no se puedan separar.
 *
 * @param id     identificador usado en el comando y en el permiso
 * @param nombre nombre visible
 * @param rango  rango a partir del cual se entrega, solo informativo para el menu
 */
public record Cosmetic(String id, String nombre, String rango) {

    /**
     * Rango de los cosmeticos que todavia no tienen permiso asignado en LuckPerms. Existen en el
     * catalogo para poder repartirlos sin recompilar, pero hoy nadie los alcanza.
     */
    public static final String SIN_ASIGNAR = "Sin asignar";

    /** Auras: particulas permanentes alrededor del jugador. */
    public static final List<Cosmetic> AURAS = List.of(
            new Cosmetic("flame", "Llama", "Hércules"),
            new Cosmetic("lightning", "Tormenta", "Thor"),
            new Cosmetic("soul", "Ánima", "Anubis"),
            new Cosmetic("water", "Marea", "Poseidón"),
            new Cosmetic("titan", "Titán", "Titanes"),
            new Cosmetic("caos", "Caos", "Titán Caos"),
            new Cosmetic("sparkle", "Destello", SIN_ASIGNAR),
            new Cosmetic("ember", "Brasa", SIN_ASIGNAR),
            new Cosmetic("forest", "Espesura", SIN_ASIGNAR),
            new Cosmetic("heart", "Encanto", SIN_ASIGNAR),
            new Cosmetic("sand", "Arena", SIN_ASIGNAR),
            new Cosmetic("abyss", "Abismo", SIN_ASIGNAR),
            new Cosmetic("solar", "Solar", "Titán Hiperión"),
            new Cosmetic("void", "Vacío", SIN_ASIGNAR),
            new Cosmetic("singularidad", "Singularidad", "Titán Caos"),
            new Cosmetic("staff", "Cetro", "Staff"),
            // ── Con forma: geometria de verdad, no una nube sobre la cabeza ──
            new Cosmetic("alas", "Alas Doradas", "Hermes"),
            new Cosmetic("alas_moradas", "Alas de Ocaso", "Anubis"),
            new Cosmetic("halo", "Halo", "Hestia"),
            new Cosmetic("halo_morado", "Halo de Ocaso", "Afrodita"),
            new Cosmetic("cola", "Cola Espectral", "Artemisa"),
            new Cosmetic("orbita", "Luciérnagas", "Zeus"),
            new Cosmetic("orbita_dorada", "Órbita Dorada", "Titanes"),
            new Cosmetic("star", "★ Star", "Staff"),
            new Cosmetic("papa", "Corona del Rey Papa", "Trueque de la Papa"));

    /** Rastros: particulas que quedan al caminar. */
    public static final List<Cosmetic> RASTROS = List.of(
            new Cosmetic("sparkle", "Chispas", "Hércules"),
            new Cosmetic("dragon", "Aliento", "Hermes"),
            new Cosmetic("portal", "Portal", "Artemisa"),
            new Cosmetic("heart", "Corazones", "Afrodita"),
            new Cosmetic("note", "Notas", SIN_ASIGNAR),
            new Cosmetic("lava", "Lava", SIN_ASIGNAR),
            new Cosmetic("leaf", "Hojas", SIN_ASIGNAR),
            new Cosmetic("cloud", "Nube", SIN_ASIGNAR),
            new Cosmetic("snow", "Escarcha", SIN_ASIGNAR),
            new Cosmetic("bubble", "Burbujas", SIN_ASIGNAR),
            new Cosmetic("rune", "Runas", SIN_ASIGNAR),
            new Cosmetic("staff", "Autoridad", "Staff"),
            new Cosmetic("star", "★ Star", "Staff"),
            new Cosmetic("dorado", "Polvo Dorado", "Titanes"),
            new Cosmetic("morado", "Polvo de Ocaso", "Titán Caos"),
            new Cosmetic("residuo", "Imágenes Residuales", "Titán Caos"),
            new Cosmetic("plumas", "Plumas", "Hermes"));

    /** Efectos de muerte: se disparan una vez al morir. */
    public static final List<Cosmetic> MUERTES = List.of(
            new Cosmetic("totem", "Tótem", "Hestia"),
            new Cosmetic("lightning", "Rayo", "Hefesto"),
            new Cosmetic("explosion", "Explosión", "Zeus"),
            new Cosmetic("smoke", "Humo", SIN_ASIGNAR),
            new Cosmetic("souls", "Almas", SIN_ASIGNAR),
            new Cosmetic("implosion", "Implosión", SIN_ASIGNAR),
            new Cosmetic("staff", "Retirada", "Staff"));

    public static List<Cosmetic> of(String tipo) {
        return switch (tipo.toLowerCase(Locale.ROOT)) {
            case "aura" -> AURAS;
            case "rastro", "trail" -> RASTROS;
            case "muerte", "death" -> MUERTES;
            default -> List.of();
        };
    }

    /** Clave canonica del tipo, para permisos y almacenamiento. */
    public static String tipoCanonico(String tipo) {
        return switch (tipo.toLowerCase(Locale.ROOT)) {
            case "aura" -> "aura";
            case "rastro", "trail" -> "trail";
            case "muerte", "death" -> "death";
            default -> "";
        };
    }

    public String permiso(String tipoCanonico) {
        return "drakes.cosmetics." + tipoCanonico + "." + id;
    }
}

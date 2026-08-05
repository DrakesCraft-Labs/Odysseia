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

    /** Auras: particulas permanentes alrededor del jugador. */
    public static final List<Cosmetic> AURAS = List.of(
            new Cosmetic("flame", "Llama", "Hércules"),
            new Cosmetic("sparkle", "Destello", "Hestia"),
            new Cosmetic("water", "Marea", "Hermes"),
            new Cosmetic("ember", "Brasa", "Hefesto"),
            new Cosmetic("forest", "Espesura", "Artemisa"),
            new Cosmetic("heart", "Encanto", "Afrodita"),
            new Cosmetic("lightning", "Tormenta", "Zeus"),
            new Cosmetic("soul", "Ánima", "Thor"),
            new Cosmetic("sand", "Arena", "Anubis"),
            new Cosmetic("abyss", "Abismo", "Poseidón"),
            new Cosmetic("titan", "Titán", "Titanes"),
            new Cosmetic("solar", "Solar", "Titán Hiperión"),
            new Cosmetic("void", "Vacío", "Titán Cronos"),
            new Cosmetic("caos", "Caos", "Titán Caos"),
            new Cosmetic("staff", "Cetro", "Staff"));

    /** Rastros: particulas que quedan al caminar. */
    public static final List<Cosmetic> RASTROS = List.of(
            new Cosmetic("sparkle", "Chispas", "Hércules"),
            new Cosmetic("heart", "Corazones", "Hestia"),
            new Cosmetic("note", "Notas", "Hermes"),
            new Cosmetic("lava", "Lava", "Hefesto"),
            new Cosmetic("leaf", "Hojas", "Artemisa"),
            new Cosmetic("cloud", "Nube", "Afrodita"),
            new Cosmetic("portal", "Portal", "Zeus"),
            new Cosmetic("dragon", "Aliento", "Thor"),
            new Cosmetic("snow", "Escarcha", "Anubis"),
            new Cosmetic("bubble", "Burbujas", "Poseidón"),
            new Cosmetic("rune", "Runas", "Titanes"),
            new Cosmetic("staff", "Autoridad", "Staff"));

    /** Efectos de muerte: se disparan una vez al morir. */
    public static final List<Cosmetic> MUERTES = List.of(
            new Cosmetic("smoke", "Humo", "Hércules"),
            new Cosmetic("totem", "Tótem", "Hermes"),
            new Cosmetic("explosion", "Explosión", "Hefesto"),
            new Cosmetic("lightning", "Rayo", "Zeus"),
            new Cosmetic("souls", "Almas", "Anubis"),
            new Cosmetic("implosion", "Implosión", "Titanes"),
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

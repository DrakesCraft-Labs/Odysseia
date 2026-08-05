package org.metamechanists.odysseia.boss;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Da a cada jefe un aspecto acorde a lo que representa.
 *
 * La entidad real sigue siendo la que conviene mecanicamente --un esqueleto apunta con arco, un
 * ravager embiste--, pero lo que ve el jugador es otra cosa. Asi Artemisa deja de parecer un
 * esqueleto con nombre dorado.
 *
 * Se apoya en LibsDisguises por reflexion: si el plugin no esta, el jefe sale con su aspecto
 * normal y no pasa nada mas.
 */
public final class BossDisguise {

    private static Boolean disponible;

    private BossDisguise() {
    }

    private static boolean disponible() {
        if (disponible == null) {
            disponible = Bukkit.getPluginManager().getPlugin("LibsDisguises") != null;
        }
        return disponible;
    }

    /**
     * Aplica un disfraz de mob al jefe.
     *
     * @param entidad el jefe
     * @param tipo    nombre del tipo de entidad a mostrar, por ejemplo {@code WITHER_SKELETON}
     * @param escala  multiplicador de tamano; 1.0 deja el original
     */
    public static void aplicar(LivingEntity entidad, String tipo, double escala) {
        if (entidad == null || tipo == null || !disponible()) return;
        try {
            Class<?> tipoDisfraz = Class.forName("me.libraryaddict.disguise.disguisetypes.DisguiseType");
            Class<?> mobDisfraz = Class.forName("me.libraryaddict.disguise.disguisetypes.MobDisguise");
            Class<?> api = Class.forName("me.libraryaddict.disguise.DisguiseAPI");

            Object valor = tipoDisfraz.getMethod("valueOf", String.class)
                    .invoke(null, tipo.toUpperCase(java.util.Locale.ROOT));
            Object disfraz = mobDisfraz.getConstructor(tipoDisfraz).newInstance(valor);

            api.getMethod("disguiseEntity", org.bukkit.entity.Entity.class,
                            Class.forName("me.libraryaddict.disguise.disguisetypes.Disguise"))
                    .invoke(null, entidad, disfraz);

            if (escala != 1.0D) {
                var atributo = entidad.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                if (atributo != null) atributo.setBaseValue(escala);
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            // Un disfraz que falla no debe impedir la pelea: se registra y el jefe sale normal.
            Bukkit.getLogger().log(Level.FINE,
                    "[Bosses] No se pudo disfrazar a " + entidad.getType() + " de " + tipo, error);
        }
    }

    /** Quita el disfraz. Se llama al limpiar para no dejar entidades disfrazadas colgando. */
    public static void quitar(LivingEntity entidad) {
        if (entidad == null || !disponible()) return;
        try {
            Class<?> api = Class.forName("me.libraryaddict.disguise.DisguiseAPI");
            Method metodo = api.getMethod("undisguiseToAll", org.bukkit.entity.Entity.class);
            metodo.invoke(null, entidad);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // nada que hacer
        }
    }
}

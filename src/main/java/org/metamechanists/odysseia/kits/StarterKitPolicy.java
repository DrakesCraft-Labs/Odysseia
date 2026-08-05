package org.metamechanists.odysseia.kits;

/** Pure eligibility rules for automatic first-join kit delivery. */
public final class StarterKitPolicy {
    private StarterKitPolicy() {
    }

    public static boolean shouldEnroll(boolean hasPlayedBefore, boolean pending,
                                       boolean delivered, boolean claimed) {
        return !delivered && !claimed && (pending || !hasPlayedBefore);
    }

    /**
     * Elegibilidad del kit de una modalidad distinta de la base.
     *
     * SkyBlock y OneBlock empiezan de cero, pero el kit inicial se entregaba una sola vez por
     * cuenta al entrar al servidor: quien ya lo habia recibido en Survival llegaba a la isla sin
     * nada. Aqui la primera visita a cada modalidad cuenta por separado, y {@code hasPlayedBefore}
     * deja de importar porque lo que se mide es la entrada a esa modalidad, no al servidor.
     */
    public static boolean shouldEnrollInModality(boolean pending, boolean delivered, boolean claimed) {
        return !delivered && !claimed;
    }
}

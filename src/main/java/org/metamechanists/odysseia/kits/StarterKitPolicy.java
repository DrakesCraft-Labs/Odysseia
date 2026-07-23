package org.metamechanists.odysseia.kits;

/** Pure eligibility rules for automatic first-join kit delivery. */
public final class StarterKitPolicy {
    private StarterKitPolicy() {
    }

    public static boolean shouldEnroll(boolean hasPlayedBefore, boolean pending,
                                       boolean delivered, boolean claimed) {
        return !delivered && !claimed && (pending || !hasPlayedBefore);
    }
}

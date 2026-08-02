package org.metamechanists.odysseia.listeners;

/** Pure policy so shared-network limits remain regression-testable. */
final class AntiAltPolicy {
    private AntiAltPolicy() { }

    static boolean shouldReject(int activeOrPendingConnections, int maximumConnections) {
        return activeOrPendingConnections >= Math.max(1, maximumConnections);
    }
}

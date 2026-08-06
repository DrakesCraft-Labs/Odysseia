package org.metamechanists.odysseia.purchase;

/**
 * Decide cuando conviene pedirle a Tebex que revise entregas pendientes.
 *
 * Tebex solo entrega en su ciclo de sondeo, asi que quien compra y entra justo despues de un ciclo
 * espera el siguiente completo. A Rojo le toco esperar 14 minutos con el producto ya pagado.
 * {@code tebex forcecheck} lo resuelve al instante, pero llamarlo en cada entrada golpearia la API
 * de Tebex sin motivo.
 *
 * Esta clase es la parte de la decision que no toca Bukkit, para poder comprobarla sin levantar un
 * servidor. Hace dos cosas:
 *
 *   - **Agrupa**: si diez personas entran a la vez, se dispara una sola revision, no diez.
 *   - **Espacia**: nunca dos revisiones separadas por menos del enfriamiento configurado.
 */
public final class ForcecheckThrottle {

    private final long cooldownMillis;
    /**
     * Arranca en 0, no en {@link Long#MIN_VALUE}: restarle un instante actual a MIN_VALUE desborda
     * y devuelve un positivo enorme, con lo que la primera revision se programaba para dentro de
     * millones de años. Con 0 la resta siempre sale negativa y la primera entrada dispara ya.
     */
    private long proximoPermitido;
    private boolean pendiente;

    public ForcecheckThrottle(long cooldownMillis) {
        this.cooldownMillis = Math.max(0, cooldownMillis);
    }

    /**
     * Registra que alguien entro al servidor.
     *
     * @return milisegundos que hay que esperar antes de lanzar la revision, o {@code -1} si ya hay
     *         una programada y esta entrada se aprovecha de ella.
     */
    public synchronized long registrarEntrada(long ahora) {
        if (pendiente) return -1;
        pendiente = true;
        return Math.max(0, proximoPermitido - ahora);
    }

    /** Marca que la revision ya se lanzo y arranca el enfriamiento. */
    public synchronized void registrarDisparo(long ahora) {
        pendiente = false;
        proximoPermitido = ahora + cooldownMillis;
    }

    /** Solo para pruebas y diagnostico: si hay una revision esperando su turno. */
    public synchronized boolean hayPendiente() {
        return pendiente;
    }
}

package org.metamechanists.odysseia.events.model;

public interface ActiveEvent {
    EventType getEventType();
    default String getName() { return getStatusSummary(); }
    void start();
    void tick();
    void stop(String reason);
    default void stop() { stop("Finalizado"); }
    boolean isActive();
    long getTimeRemainingSeconds();
    String getStatusSummary();
}

package org.metamechanists.odysseia.purchase;

public record ActionResult(Status status, String detail) {
    public enum Status { COMPLETED, WAITING_FOR_PLAYER, RETRYABLE_FAILURE, MANUAL_REVIEW, SKIPPED }
    public static ActionResult completed(String detail) { return new ActionResult(Status.COMPLETED, detail); }
    public static ActionResult waiting(String detail) { return new ActionResult(Status.WAITING_FOR_PLAYER, detail); }
    public static ActionResult retryable(String detail) { return new ActionResult(Status.RETRYABLE_FAILURE, detail); }
    public static ActionResult manual(String detail) { return new ActionResult(Status.MANUAL_REVIEW, detail); }
    public static ActionResult skipped(String detail) { return new ActionResult(Status.SKIPPED, detail); }
}

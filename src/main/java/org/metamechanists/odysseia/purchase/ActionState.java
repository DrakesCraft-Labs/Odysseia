package org.metamechanists.odysseia.purchase;

public enum ActionState {
    PENDING, WAITING_FOR_PLAYER, PROCESSING, COMPLETED, FAILED_RETRYABLE,
    FAILED_MANUAL_REVIEW, REVOKED, SKIPPED
}

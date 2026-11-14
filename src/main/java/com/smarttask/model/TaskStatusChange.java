package com.smarttask.model;

import java.time.LocalDateTime;

/**
 * Records a single status transition for Stack undo and LinkedList history.
 */
public class TaskStatusChange {

    private final Long taskId;
    private final Long userId;
    private final Status previousStatus;
    private final Status newStatus;
    private final LocalDateTime changedAt;

    public TaskStatusChange(Long taskId, Long userId, Status previousStatus, Status newStatus) {
        this.taskId = taskId;
        this.userId = userId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedAt = LocalDateTime.now();
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public Status getPreviousStatus() {
        return previousStatus;
    }

    public Status getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}

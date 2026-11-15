package com.smarttask.algorithms;

import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.Urgency;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Calculates deadline urgency and combined importance score
 * so high-priority / near-deadline tasks appear first.
 */
public final class TaskPriorityAlgorithm {

    private TaskPriorityAlgorithm() {
    }

    public static Urgency calculateUrgency(Task task) {
        if (task.getStatus() == Status.COMPLETED) {
            return Urgency.NONE;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueAt = task.getDeadline().atTime(LocalTime.of(23, 59));

        if (dueAt.isBefore(now)) {
            return Urgency.OVERDUE;
        }

        long hoursLeft = ChronoUnit.HOURS.between(now, dueAt);
        if (hoursLeft <= 24) {
            return Urgency.URGENT;
        }
        if (hoursLeft <= 72) {
            return Urgency.UPCOMING;
        }
        return Urgency.NORMAL;
    }

    public static String buildReminder(Task task) {
        Urgency urgency = calculateUrgency(task);
        if (urgency == Urgency.NONE || urgency == Urgency.NORMAL) {
            return null;
        }

        String title = task.getTitle();
        if (urgency == Urgency.OVERDUE) {
            return "Reminder: Your \"" + title + "\" task is overdue.";
        }
        if (task.getDeadline().equals(LocalDate.now().plusDays(1))) {
            return "Reminder: Your \"" + title + "\" task is due tomorrow.";
        }
        if (urgency == Urgency.URGENT) {
            return "Reminder: Your \"" + title + "\" task is due within 24 hours.";
        }
        return "Reminder: Your \"" + title + "\" task is due in a few days.";
    }

    /**
     * Higher score means the task should appear earlier.
     * Combines deadline urgency with user-assigned priority.
     */
    public static int importanceScore(Task task) {
        Urgency urgency = calculateUrgency(task);
        int urgencyScore;
        if (urgency == Urgency.OVERDUE) {
            urgencyScore = 400;
        } else if (urgency == Urgency.URGENT) {
            urgencyScore = 300;
        } else if (urgency == Urgency.UPCOMING) {
            urgencyScore = 200;
        } else if (urgency == Urgency.NORMAL) {
            urgencyScore = 100;
        } else {
            urgencyScore = 0;
        }

        int priorityScore;
        if (task.getPriority() == Priority.HIGH) {
            priorityScore = 30;
        } else if (task.getPriority() == Priority.MEDIUM) {
            priorityScore = 20;
        } else {
            priorityScore = 10;
        }
        return urgencyScore + priorityScore;
    }
}

package com.smarttask.model;

/**
 * One weekly or monthly bar-group for completed vs overdue charts.
 */
public class ReportPeriodBucket {

    private final String label;
    private final int completed;
    private final int overdue;
    private final int completedHeight;
    private final int overdueHeight;

    public ReportPeriodBucket(String label, int completed, int overdue,
                              int completedHeight, int overdueHeight) {
        this.label = label;
        this.completed = completed;
        this.overdue = overdue;
        this.completedHeight = completedHeight;
        this.overdueHeight = overdueHeight;
    }

    public String getLabel() {
        return label;
    }

    public int getCompleted() {
        return completed;
    }

    public int getOverdue() {
        return overdue;
    }

    public int getCompletedHeight() {
        return completedHeight;
    }

    public int getOverdueHeight() {
        return overdueHeight;
    }
}

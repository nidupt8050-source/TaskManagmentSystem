package com.smarttask.model;

public class DashboardStats {

    private final long totalTasks;
    private final long completedTasks;
    private final long pendingTasks;
    private final long inProgressTasks;
    private final long overdueTasks;
    private final long highPriorityTasks;
    private final int completionPercentage;

    public DashboardStats(long totalTasks, long completedTasks, long pendingTasks,
                          long inProgressTasks, long overdueTasks, long highPriorityTasks,
                          int completionPercentage) {
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.pendingTasks = pendingTasks;
        this.inProgressTasks = inProgressTasks;
        this.overdueTasks = overdueTasks;
        this.highPriorityTasks = highPriorityTasks;
        this.completionPercentage = completionPercentage;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public long getPendingTasks() {
        return pendingTasks;
    }

    public long getInProgressTasks() {
        return inProgressTasks;
    }

    public long getOverdueTasks() {
        return overdueTasks;
    }

    public long getHighPriorityTasks() {
        return highPriorityTasks;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }
}

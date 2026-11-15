package com.smarttask.model;

import com.smarttask.datastructures.DynamicArray;

public class ProductivityReport {

    private final String range;
    private final String rangeLabel;
    private final int totalTasks;
    private final int totalCompleted;
    private final int totalOverdue;
    private final int completionRate;
    private final DynamicArray<ReportPeriodBucket> timeline;
    private final DynamicArray<PriorityBreakdown> priorityBreakdown;
    private final String priorityGradient;
    private final DynamicArray<ProgressTrendItem> aimTrends;
    private final DynamicArray<ProgressTrendItem> goalTrends;

    public ProductivityReport(String range,
                              String rangeLabel,
                              int totalTasks,
                              int totalCompleted,
                              int totalOverdue,
                              int completionRate,
                              DynamicArray<ReportPeriodBucket> timeline,
                              DynamicArray<PriorityBreakdown> priorityBreakdown,
                              String priorityGradient,
                              DynamicArray<ProgressTrendItem> aimTrends,
                              DynamicArray<ProgressTrendItem> goalTrends) {
        this.range = range;
        this.rangeLabel = rangeLabel;
        this.totalTasks = totalTasks;
        this.totalCompleted = totalCompleted;
        this.totalOverdue = totalOverdue;
        this.completionRate = completionRate;
        this.timeline = timeline;
        this.priorityBreakdown = priorityBreakdown;
        this.priorityGradient = priorityGradient;
        this.aimTrends = aimTrends;
        this.goalTrends = goalTrends;
    }

    public String getRange() {
        return range;
    }

    public String getRangeLabel() {
        return rangeLabel;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getTotalCompleted() {
        return totalCompleted;
    }

    public int getTotalOverdue() {
        return totalOverdue;
    }

    public int getCompletionRate() {
        return completionRate;
    }

    public DynamicArray<ReportPeriodBucket> getTimeline() {
        return timeline;
    }

    public DynamicArray<PriorityBreakdown> getPriorityBreakdown() {
        return priorityBreakdown;
    }

    public String getPriorityGradient() {
        return priorityGradient;
    }

    public DynamicArray<ProgressTrendItem> getAimTrends() {
        return aimTrends;
    }

    public DynamicArray<ProgressTrendItem> getGoalTrends() {
        return goalTrends;
    }
}

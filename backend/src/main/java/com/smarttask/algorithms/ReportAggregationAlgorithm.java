package com.smarttask.algorithms;

import com.smarttask.datastructures.DynamicArray;
import com.smarttask.model.Priority;
import com.smarttask.model.PriorityBreakdown;
import com.smarttask.model.ReportPeriodBucket;
import com.smarttask.model.Status;
import com.smarttask.model.Task;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Aggregates task data into productivity report series.
 * Uses linear scans over DynamicArray (no library collections).
 */
public final class ReportAggregationAlgorithm {

    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private ReportAggregationAlgorithm() {
    }

    /**
     * Last 7 calendar days: completed (by completion date) vs overdue (deadline missed).
     */
    public static DynamicArray<ReportPeriodBucket> weeklyCompletedVsOverdue(
            DynamicArray<Task> tasks, LocalDate today) {
        DynamicArray<ReportPeriodBucket> buckets = new DynamicArray<>();
        LocalDate start = today.minusDays(6);
        int[] completed = new int[7];
        int[] overdue = new int[7];

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            LocalDate completedOn = completionDate(task);
            if (completedOn != null && !completedOn.isBefore(start) && !completedOn.isAfter(today)) {
                int index = (int) (completedOn.toEpochDay() - start.toEpochDay());
                if (index >= 0 && index < 7) {
                    completed[index]++;
                }
            }
            LocalDate deadline = task.getDeadline();
            if (deadline != null && !deadline.isBefore(start) && !deadline.isAfter(today) && isOverdue(task, today)) {
                int index = (int) (deadline.toEpochDay() - start.toEpochDay());
                if (index >= 0 && index < 7) {
                    overdue[index]++;
                }
            }
        }

        int max = 1;
        for (int i = 0; i < 7; i++) {
            max = Math.max(max, Math.max(completed[i], overdue[i]));
        }
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            buckets.add(new ReportPeriodBucket(
                    DAY_LABEL.format(day),
                    completed[i],
                    overdue[i],
                    height(completed[i], max),
                    height(overdue[i], max)
            ));
        }
        return buckets;
    }

    /**
     * Last 6 calendar months: completed vs overdue by month.
     */
    public static DynamicArray<ReportPeriodBucket> monthlyCompletedVsOverdue(
            DynamicArray<Task> tasks, LocalDate today) {
        DynamicArray<ReportPeriodBucket> buckets = new DynamicArray<>();
        YearMonth end = YearMonth.from(today);
        YearMonth start = end.minusMonths(5);
        int[] completed = new int[6];
        int[] overdue = new int[6];

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            LocalDate completedOn = completionDate(task);
            if (completedOn != null) {
                int index = monthIndex(YearMonth.from(completedOn), start);
                if (index >= 0) {
                    completed[index]++;
                }
            }
            LocalDate deadline = task.getDeadline();
            if (deadline != null && isOverdue(task, today)) {
                int index = monthIndex(YearMonth.from(deadline), start);
                if (index >= 0) {
                    overdue[index]++;
                }
            }
        }

        int max = 1;
        for (int i = 0; i < 6; i++) {
            max = Math.max(max, Math.max(completed[i], overdue[i]));
        }
        for (int i = 0; i < 6; i++) {
            YearMonth month = start.plusMonths(i);
            buckets.add(new ReportPeriodBucket(
                    MONTH_LABEL.format(month.atDay(1)),
                    completed[i],
                    overdue[i],
                    height(completed[i], max),
                    height(overdue[i], max)
            ));
        }
        return buckets;
    }

    public static DynamicArray<PriorityBreakdown> priorityBreakdown(DynamicArray<Task> tasks) {
        int high = 0;
        int medium = 0;
        int low = 0;
        for (int i = 0; i < tasks.size(); i++) {
            Priority priority = tasks.get(i).getPriority();
            if (priority == Priority.HIGH) {
                high++;
            } else if (priority == Priority.MEDIUM) {
                medium++;
            } else {
                low++;
            }
        }
        int total = Math.max(1, high + medium + low);
        DynamicArray<PriorityBreakdown> result = new DynamicArray<>();
        result.add(new PriorityBreakdown("high", Priority.HIGH.getLabel(), high, ProgressAlgorithm.percentage(high, total)));
        result.add(new PriorityBreakdown("medium", Priority.MEDIUM.getLabel(), medium, ProgressAlgorithm.percentage(medium, total)));
        result.add(new PriorityBreakdown("low", Priority.LOW.getLabel(), low, ProgressAlgorithm.percentage(low, total)));
        return result;
    }

    /** CSS conic-gradient stops for the priority donut. */
    public static String priorityConicGradient(DynamicArray<PriorityBreakdown> slices) {
        if (slices == null || slices.isEmpty()) {
            return "conic-gradient(#d7e4ed 0% 100%)";
        }
        int total = 0;
        for (int i = 0; i < slices.size(); i++) {
            total += slices.get(i).getCount();
        }
        if (total <= 0) {
            return "conic-gradient(#d7e4ed 0% 100%)";
        }
        StringBuilder gradient = new StringBuilder("conic-gradient(");
        double cursor = 0;
        String[] colors = {"#d64545", "#ee8a3c", "#1f9d7a"};
        for (int i = 0; i < slices.size(); i++) {
            double share = (slices.get(i).getCount() * 100.0) / total;
            double next = cursor + share;
            if (i > 0) {
                gradient.append(", ");
            }
            String color = colors[i % colors.length];
            gradient.append(color).append(' ')
                    .append(formatPercent(cursor)).append("% ")
                    .append(formatPercent(next)).append('%');
            cursor = next;
        }
        gradient.append(')');
        return gradient.toString();
    }

    public static int sumCompleted(DynamicArray<ReportPeriodBucket> buckets) {
        int sum = 0;
        for (int i = 0; i < buckets.size(); i++) {
            sum += buckets.get(i).getCompleted();
        }
        return sum;
    }

    public static int sumOverdue(DynamicArray<ReportPeriodBucket> buckets) {
        int sum = 0;
        for (int i = 0; i < buckets.size(); i++) {
            sum += buckets.get(i).getOverdue();
        }
        return sum;
    }

    private static boolean isOverdue(Task task, LocalDate today) {
        LocalDate deadline = task.getDeadline();
        if (deadline == null || !deadline.isBefore(today)) {
            return false;
        }
        if (task.getStatus() != Status.COMPLETED) {
            return true;
        }
        LocalDate completedOn = completionDate(task);
        return completedOn != null && completedOn.isAfter(deadline);
    }

    private static LocalDate completionDate(Task task) {
        if (task.getStatus() != Status.COMPLETED) {
            return null;
        }
        if (task.getCompletedAt() != null) {
            return task.getCompletedAt().toLocalDate();
        }
        if (task.getCreatedAt() != null) {
            return task.getCreatedAt().toLocalDate();
        }
        return task.getDeadline();
    }

    private static int monthIndex(YearMonth value, YearMonth start) {
        int index = (value.getYear() - start.getYear()) * 12 + (value.getMonthValue() - start.getMonthValue());
        if (index < 0 || index > 5) {
            return -1;
        }
        return index;
    }

    private static int height(int value, int max) {
        if (value <= 0) {
            return 0;
        }
        return Math.max(8, (int) Math.round((value * 100.0) / max));
    }

    private static String formatPercent(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}

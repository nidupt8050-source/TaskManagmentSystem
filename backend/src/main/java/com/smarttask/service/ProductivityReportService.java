package com.smarttask.service;

import com.smarttask.algorithms.ProgressAlgorithm;
import com.smarttask.algorithms.ReportAggregationAlgorithm;
import com.smarttask.datastructures.DynamicArray;
import com.smarttask.model.Aim;
import com.smarttask.model.Goal;
import com.smarttask.model.PriorityBreakdown;
import com.smarttask.model.ProductivityReport;
import com.smarttask.model.ProgressTrendItem;
import com.smarttask.model.ReportPeriodBucket;
import com.smarttask.model.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class ProductivityReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Thimphu");

    private final TaskService taskService;
    private final AimService aimService;

    public ProductivityReportService(TaskService taskService, AimService aimService) {
        this.taskService = taskService;
        this.aimService = aimService;
    }

    public ProductivityReport buildReport(Long userId, String rangeParam) {
        String range = normalizeRange(rangeParam);
        LocalDate today = LocalDate.now(ZONE);
        DynamicArray<Task> tasks = taskService.getUserTasks(userId);

        DynamicArray<ReportPeriodBucket> timeline = "month".equals(range)
                ? ReportAggregationAlgorithm.monthlyCompletedVsOverdue(tasks, today)
                : ReportAggregationAlgorithm.weeklyCompletedVsOverdue(tasks, today);

        int totalCompleted = ReportAggregationAlgorithm.sumCompleted(timeline);
        int totalOverdue = ReportAggregationAlgorithm.sumOverdue(timeline);
        int completionRate = ProgressAlgorithm.percentage(totalCompleted, totalCompleted + totalOverdue);

        DynamicArray<PriorityBreakdown> priorities = ReportAggregationAlgorithm.priorityBreakdown(tasks);
        String gradient = ReportAggregationAlgorithm.priorityConicGradient(priorities);

        return new ProductivityReport(
                range,
                "month".equals(range) ? "Last 6 months" : "Last 7 days",
                tasks.size(),
                totalCompleted,
                totalOverdue,
                completionRate,
                timeline,
                priorities,
                gradient,
                buildAimTrends(userId),
                buildGoalTrends(userId)
        );
    }

    private DynamicArray<ProgressTrendItem> buildAimTrends(Long userId) {
        DynamicArray<Aim> aims = aimService.getAimsWithProgress(userId);
        DynamicArray<ProgressTrendItem> trends = new DynamicArray<>();
        for (int i = 0; i < aims.size(); i++) {
            Aim aim = aims.get(i);
            String subtitle = aim.getDeadline() == null ? "No deadline" : "Due " + aim.getDeadline();
            trends.add(new ProgressTrendItem(aim.getTitle(), subtitle, aim.getProgressPercent()));
        }
        sortByProgressDesc(trends);
        return trends;
    }

    private DynamicArray<ProgressTrendItem> buildGoalTrends(Long userId) {
        DynamicArray<Aim> aims = aimService.getAimsWithProgress(userId);
        DynamicArray<ProgressTrendItem> trends = new DynamicArray<>();
        for (int i = 0; i < aims.size(); i++) {
            Aim aim = aims.get(i);
            DynamicArray<Goal> goals = aimService.getGoals(aim.getId(), userId);
            for (int j = 0; j < goals.size(); j++) {
                Goal goal = goals.get(j);
                trends.add(new ProgressTrendItem(goal.getTitle(), "Aim: " + aim.getTitle(), goal.getProgressPercent()));
            }
        }
        sortByProgressDesc(trends);
        return trends;
    }

    /** Insertion-style descending sort by progress (custom DS only). */
    private void sortByProgressDesc(DynamicArray<ProgressTrendItem> items) {
        for (int i = 1; i < items.size(); i++) {
            ProgressTrendItem key = items.get(i);
            int j = i - 1;
            while (j >= 0 && items.get(j).getProgressPercent() < key.getProgressPercent()) {
                items.set(j + 1, items.get(j));
                j--;
            }
            items.set(j + 1, key);
        }
    }

    private String normalizeRange(String rangeParam) {
        if (rangeParam == null) {
            return "week";
        }
        String value = rangeParam.trim().toLowerCase(Locale.ROOT);
        return "month".equals(value) || "monthly".equals(value) ? "month" : "week";
    }
}

package com.smarttask.controller;

import com.smarttask.model.Task;
import com.smarttask.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import com.smarttask.model.DashboardStats;

@Controller
public class DashboardController {

    private final TaskService taskService;

    public DashboardController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        List<Task> tasks = taskService.getUserTasks(userId);

        model.addAttribute("currentPage", "dashboard");
        DashboardStats stats = taskService.buildStats(tasks);
        model.addAttribute("stats", stats);
        Map<java.time.LocalDate, Long> timeline = taskService.getDeadlineCounts(userId);
        model.addAttribute("deadlineCounts", timeline);
        model.addAttribute("timelineMax", Math.max(1L, timeline.values().stream().mapToLong(Long::longValue).max().orElse(0L)));
        Map<java.time.LocalDate, long[]> outcomes = taskService.getOutcomeTimeline(userId);
        long outcomeMax = Math.max(1L, outcomes.values().stream()
                .mapToLong(values -> Math.max(values[0], values[1])).max().orElse(0L));
        StringBuilder completedPoints = new StringBuilder();
        StringBuilder overduePoints = new StringBuilder();
        int index = 0;
        for (long[] values : outcomes.values()) {
            int x = 80 + index++ * 100;
            completedPoints.append(x).append(',').append(230 - values[0] * 170.0 / outcomeMax).append(' ');
            overduePoints.append(x).append(',').append(230 - values[1] * 170.0 / outcomeMax).append(' ');
        }
        model.addAttribute("outcomeTimeline", outcomes);
        model.addAttribute("outcomeMax", outcomeMax);
        model.addAttribute("completedPoints", completedPoints.toString());
        model.addAttribute("overduePoints", overduePoints.toString());
        model.addAttribute("statusCounts", taskService.getStatusCounts(userId));
        model.addAttribute("reminders", taskService.getReminders(tasks));
        model.addAttribute("attentionTasks", taskService.attentionViews(tasks, 6));
        model.addAttribute("upcomingDeadlines", taskService.toViews(taskService.earliestDeadlines(userId, 5)));
        return "dashboard";
    }
}

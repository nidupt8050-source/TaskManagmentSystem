package com.smarttask.controller;

import com.smarttask.datastructures.DynamicArray;
import com.smarttask.datastructures.TaskMemoryStore;
import com.smarttask.model.Task;
import com.smarttask.service.AimService;
import com.smarttask.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final TaskService taskService;
    private final AimService aimService;

    public DashboardController(TaskService taskService, AimService aimService) {
        this.taskService = taskService;
        this.aimService = aimService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        DynamicArray<Task> tasks = taskService.getUserTasks(userId);

        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("stats", taskService.buildStats(tasks));
        model.addAttribute("aims", aimService.getAimsWithProgress(userId));
        model.addAttribute("overallAimProgress", aimService.overallAimProgress(userId));

        DynamicArray<TaskMemoryStore.DeadlineCount> timeline = taskService.deadlineCounts(userId);
        long timelineMax = 1L;
        for (int i = 0; i < timeline.size(); i++) {
            timelineMax = Math.max(timelineMax, timeline.get(i).count());
        }
        model.addAttribute("deadlineCounts", timeline);
        model.addAttribute("timelineMax", timelineMax);
        model.addAttribute("statusCounts", taskService.statusCounts(userId));
        model.addAttribute("reminders", taskService.getReminders(tasks));
        model.addAttribute("attentionTasks", taskService.attentionViews(tasks, 6));
        model.addAttribute("upcomingDeadlines", taskService.toViews(taskService.earliestDeadlines(userId, 5)));
        return "dashboard";
    }
}

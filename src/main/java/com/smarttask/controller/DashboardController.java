package com.smarttask.controller;

import com.smarttask.model.Task;
import com.smarttask.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

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
        model.addAttribute("stats", taskService.buildStats(tasks));
        model.addAttribute("reminders", taskService.getReminders(tasks));
        model.addAttribute("attentionTasks", taskService.attentionViews(tasks, 6));
        model.addAttribute("upcomingDeadlines", taskService.toViews(taskService.earliestDeadlines(userId, 5)));
        return "dashboard";
    }
}

package com.smarttask.controller;

import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    public String listTasks(@RequestParam(required = false) Status status,
                            @RequestParam(required = false) Priority priority,
                            @RequestParam(required = false) String q,
                            @RequestParam(required = false, defaultValue = "importance") String sort,
                            HttpSession session,
                            Model model) {
        Long userId = (Long) session.getAttribute("userId");
        List<Task> tasks = taskService.queryTasks(userId, status, priority, q, sort);

        model.addAttribute("currentPage", "tasks");
        model.addAttribute("taskViews", taskService.toViews(tasks));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("statuses", Status.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("canUndo", taskService.canUndo(userId));
        return "tasks";
    }

    @GetMapping("/tasks/new")
    public String newTaskForm(Model model) {
        Task task = new Task();
        task.setDeadline(LocalDate.now().plusDays(1));
        model.addAttribute("currentPage", "new");
        model.addAttribute("task", task);
        model.addAttribute("formAction", "/tasks");
        model.addAttribute("pageTitle", "Create Task");
        model.addAttribute("submitLabel", "Save Task");
        addFormOptions(model);
        return "task-form";
    }

    @PostMapping("/tasks")
    public String createTask(@RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam Priority priority,
                             @RequestParam Status status,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Task title is required.");
            return "redirect:/tasks/new";
        }
        if (deadline == null) {
            redirectAttributes.addFlashAttribute("error", "Please choose a deadline.");
            return "redirect:/tasks/new";
        }

        taskService.createTask(userId, title, description, priority, status, deadline);
        redirectAttributes.addFlashAttribute("success", "Task created successfully.");
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/{id}")
    public String taskDetails(@PathVariable Long id, HttpSession session, Model model,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            Task task = taskService.getTask(id, userId);
            model.addAttribute("currentPage", "tasks");
            model.addAttribute("view", taskService.toView(task));
            model.addAttribute("statuses", Status.values());
            model.addAttribute("statusHistory", taskService.getStatusHistory(id));
            model.addAttribute("canUndo", taskService.canUndo(userId));
            return "task-details";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/tasks";
        }
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTaskForm(@PathVariable Long id, HttpSession session, Model model,
                               RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            Task task = taskService.getTask(id, userId);
            model.addAttribute("currentPage", "tasks");
            model.addAttribute("task", task);
            model.addAttribute("formAction", "/tasks/" + id);
            model.addAttribute("pageTitle", "Edit Task");
            model.addAttribute("submitLabel", "Update Task");
            addFormOptions(model);
            return "task-form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/tasks";
        }
    }

    @PostMapping("/tasks/{id}")
    public String updateTask(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam Priority priority,
                             @RequestParam Status status,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Task title is required.");
            return "redirect:/tasks/" + id + "/edit";
        }

        try {
            taskService.updateTask(id, userId, title, description, priority, status, deadline);
            redirectAttributes.addFlashAttribute("success", "Task updated successfully.");
            return "redirect:/tasks/" + id;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/tasks";
        }
    }

    @PostMapping("/tasks/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam Status status,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            taskService.changeStatus(id, userId, status);
            redirectAttributes.addFlashAttribute("success", "Task status updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/tasks/undo-status")
    public String undoStatus(HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            Task restored = taskService.undoLastStatusChange(userId);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Undid status change for \"" + restored.getTitle() + "\"."
            );
            return "redirect:/tasks/" + restored.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/tasks";
        }
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            taskService.deleteTask(id, userId);
            redirectAttributes.addFlashAttribute("success", "Task deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tasks";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", Status.values());
        model.addAttribute("today", LocalDate.now());
    }
}

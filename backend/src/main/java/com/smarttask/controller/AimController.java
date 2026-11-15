package com.smarttask.controller;

import com.smarttask.model.Aim;
import com.smarttask.model.ObjectiveStatus;
import com.smarttask.service.AimService;
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

@Controller
public class AimController {

    private final AimService aimService;

    public AimController(AimService aimService) {
        this.aimService = aimService;
    }

    @GetMapping("/aims")
    public String listAims(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        model.addAttribute("currentPage", "aims");
        model.addAttribute("aims", aimService.getAimsWithProgress(userId));
        model.addAttribute("overallProgress", aimService.overallAimProgress(userId));
        return "aims";
    }

    @GetMapping("/aims/new")
    public String newAim(Model model) {
        model.addAttribute("currentPage", "aims");
        model.addAttribute("aim", new Aim());
        model.addAttribute("pageTitle", "Create Aim");
        model.addAttribute("formAction", "/aims");
        model.addAttribute("submitLabel", "Save Aim");
        return "aim-form";
    }

    @PostMapping("/aims")
    public String createAim(@RequestParam String title,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aim title is required.");
            return "redirect:/aims/new";
        }
        aimService.createAim(userId, title, description, deadline);
        redirectAttributes.addFlashAttribute("success", "Aim created.");
        return "redirect:/aims";
    }

    @GetMapping("/aims/{id}")
    public String aimDetails(@PathVariable Long id, HttpSession session, Model model,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            model.addAttribute("currentPage", "aims");
            model.addAttribute("aim", aimService.getAim(id, userId));
            model.addAttribute("goals", aimService.getGoals(id, userId));
            return "aim-details";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/aims";
        }
    }

    @GetMapping("/aims/{id}/edit")
    public String editAim(@PathVariable Long id, HttpSession session, Model model,
                          RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            model.addAttribute("currentPage", "aims");
            model.addAttribute("aim", aimService.getAim(id, userId));
            model.addAttribute("pageTitle", "Edit Aim");
            model.addAttribute("formAction", "/aims/" + id);
            model.addAttribute("submitLabel", "Update Aim");
            return "aim-form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/aims";
        }
    }

    @PostMapping("/aims/{id}")
    public String updateAim(@PathVariable Long id,
                            @RequestParam String title,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aim title is required.");
            return "redirect:/aims/" + id + "/edit";
        }
        try {
            aimService.updateAim(id, userId, title, description, deadline);
            redirectAttributes.addFlashAttribute("success", "Aim updated.");
            return "redirect:/aims/" + id;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/aims";
        }
    }

    @PostMapping("/aims/{id}/delete")
    public String deleteAim(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            aimService.deleteAim(id, userId);
            redirectAttributes.addFlashAttribute("success", "Aim deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/aims";
    }

    @PostMapping("/aims/{aimId}/goals")
    public String createGoal(@PathVariable Long aimId,
                             @RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            aimService.createGoal(aimId, userId, title, description, deadline);
            redirectAttributes.addFlashAttribute("success", "Goal added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/aims/" + aimId;
    }

    @GetMapping("/goals/{id}")
    public String goalDetails(@PathVariable Long id, HttpSession session, Model model,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            model.addAttribute("currentPage", "aims");
            model.addAttribute("goal", aimService.getGoal(id, userId));
            model.addAttribute("objectives", aimService.getObjectives(id, userId));
            model.addAttribute("objectiveStatuses", ObjectiveStatus.values());
            return "goal-details";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/aims";
        }
    }

    @PostMapping("/goals/{goalId}/objectives")
    public String createObjective(@PathVariable Long goalId,
                                  @RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                                  @RequestParam(required = false, defaultValue = "ACTIVE") ObjectiveStatus status,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            aimService.createObjective(goalId, userId, title, description, deadline, status);
            redirectAttributes.addFlashAttribute("success", "Objective added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/goals/" + goalId;
    }

    @PostMapping("/objectives/{id}/status")
    public String changeObjectiveStatus(@PathVariable Long id,
                                        @RequestParam ObjectiveStatus status,
                                        @RequestParam Long goalId,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            aimService.changeObjectiveStatus(id, userId, status);
            redirectAttributes.addFlashAttribute("success", "Objective status updated. Goal and Aim progress refreshed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/goals/" + goalId;
    }

    @PostMapping("/goals/{id}/delete")
    public String deleteGoal(@PathVariable Long id,
                             @RequestParam Long aimId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            aimService.deleteGoal(id, userId);
            redirectAttributes.addFlashAttribute("success", "Goal deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/aims/" + aimId;
    }

    @PostMapping("/objectives/{id}/delete")
    public String deleteObjective(@PathVariable Long id,
                                  @RequestParam Long goalId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            aimService.deleteObjective(id, userId);
            redirectAttributes.addFlashAttribute("success", "Objective deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/goals/" + goalId;
    }
}

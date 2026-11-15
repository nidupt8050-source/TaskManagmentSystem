package com.smarttask.controller;

import com.smarttask.model.ProductivityReport;
import com.smarttask.service.ProductivityReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportController {

    private final ProductivityReportService reportService;

    public ReportController(ProductivityReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false, defaultValue = "week") String range,
                          HttpSession session,
                          Model model) {
        Long userId = (Long) session.getAttribute("userId");
        ProductivityReport report = reportService.buildReport(userId, range);

        model.addAttribute("currentPage", "reports");
        model.addAttribute("report", report);
        model.addAttribute("selectedRange", report.getRange());
        return "reports";
    }
}

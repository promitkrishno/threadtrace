package com.cse444.threadtrace.dashboard.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", "ThreadTrace");
        model.addAttribute("version", "1.0.0");
        return "dashboard";
    }
}

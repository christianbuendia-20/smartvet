package com.smartvet.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Redirects the legacy /dashboard route to / so IndexController handles role-based routing.
@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/";
    }
}

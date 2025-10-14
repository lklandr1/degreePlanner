package com.PlanInk.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController{

    @GetMapping("/")
    public String home() {
        return "login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/student")
    public String student() {
        return "student";
    }
    
    @GetMapping("/faculty")
    public String faculty() {
        return "faculty";
    }

    @GetMapping("/logout")
    public String logout() {
        // For now, just redirect to login page
        // In a real application, you would invalidate the session here
        return "redirect:/login";
    }

}
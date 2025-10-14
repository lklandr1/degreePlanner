package com.PlanInk.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StudentController{

    @GetMapping("/student")
    public String student() {
        return "studentPortal";
    }

    @GetMapping("/progress")
    public String progress() {
        return "studentProgress";
    }

    @GetMapping("/sandbox")
    public String sandbox() {
        return "studentSandbox";
    }

    @GetMapping("/classes")
    public String classes() {
        return "studentClasses";
    }

}
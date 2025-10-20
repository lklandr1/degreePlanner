package com.PlanInk.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StudentController{

    @GetMapping("/student")
    public String student() {
        return "student/studentPortal";
    }

    @GetMapping("/student/progress")
    public String progress() {
        return "student/studentProgress";
    }

    @GetMapping("/student/sandbox")
    public String sandbox() {
        return "student/studentSandbox";
    }

    @GetMapping("/student/classes")
    public String classes() {
        return "student/studentClasses";
    }

}
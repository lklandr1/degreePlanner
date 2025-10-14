package com.PlanInk.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdvisorController {

    @GetMapping("/advisor")
    public String advisor() {
        return "advisorPortal";
    }

    @GetMapping("/individualStudentAdvisor")
    public String individualStudentAdvisor() {
        return "individualStudentAdvisor";
    }

    @GetMapping("/changesAdvisor")
    public String changesAdvisor() {
        return "changesAdvisor";
    }
}

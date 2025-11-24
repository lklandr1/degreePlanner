package com.PlanInk.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdvisorController {

    @GetMapping("/advisor")
    public String advisor() {
        return "advisor/advisorPortal";
    }

    @GetMapping("/advisor/curriculumUpdates")
    public String changesAdvisor() {
        return "advisor/advisorChanges";
    }

    @GetMapping("/advisor/courseReview")
    public String courseReview() {
        return "advisor/courseReview";
    }
}

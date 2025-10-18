package com.PlanInk.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FacultyController {

    @GetMapping("/faculty")
    public String faculty() {
        return "faculty/facultyPortal";
    }

    @GetMapping("/faculty/courseRequirements")
    public String courseRequirements() {
        return "faculty/courseRequirements";
    }

    @GetMapping("/faculty/editRequirements")
    public String editRequirement() {
        return "faculty/editRequirement";
    }

    @GetMapping("/faculty/editCourse")
    public String editCourse() {
        return "faculty/editCourse";
    }
}

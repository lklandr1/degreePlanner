package com.PlanInk.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FacultyController {

    @GetMapping("/faculty")
    public String faculty() {
        return "facultyPortal";
    }

    @GetMapping("/courseRequirements")
    public String courseRequirements() {
        return "courseRequirements";
    }

    @GetMapping("/editRequirement")
    public String editRequirement() {
        return "editRequirement";
    }

    @GetMapping("/editCourse")
    public String editCourse() {
        return "editCourse";
    }
}

package com.PlanInk.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FacultyController {

    @GetMapping("/faculty")
    public String faculty() {
        return "faculty/facultyPortal";
    }

    @GetMapping("/faculty/courseRequirements")
    public String courseRequirements(@RequestParam(name = "major", required = false) String majorName, Model model) {
        if (majorName != null) {
            model.addAttribute("majorName", majorName);
        }
        return "faculty/courseRequirements";
    }

    @GetMapping("/faculty/editRequirements")
    public String editRequirement(@RequestParam(name = "major", required = false, defaultValue = "Major") String majorName, Model model) {
        model.addAttribute("majorName", majorName);
        return "faculty/editRequirement";
    }

    @GetMapping("/faculty/editCourse")
    public String editCourse(@RequestParam(name = "course", required = false, defaultValue = "Course") String courseName, 
                            @RequestParam(name = "major", required = false) String majorName, 
                            Model model) {
        String courseTitle;
        if (courseName.contains(" - ")) {
            courseTitle = courseName.split(" - ", 2)[1];
        } else {
            courseTitle = courseName;
        }

        model.addAttribute("courseName", courseTitle);
        if (majorName != null) {
            model.addAttribute("majorName", majorName);
        }
        return "faculty/editCourse";
    }

    @GetMapping("/faculty/changelog")
    public String changelog() {
        return "faculty/changelog";
    }
}

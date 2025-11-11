package com.PlanInk.mvc.signup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class SignupRequest {
    @NotBlank
    private String role; // "student", "advisor", or "faculty"

    // only used when role == "student"
    @Valid
    private StudentDto student;

    // For advisor/faculty we accept a generic map or you can add typed DTOs later
    // private AdvisorDto advisor;
    // private FacultyDto faculty;

    // getters & setters
}

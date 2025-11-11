package com.PlanInk.mvc.signup;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles displaying and processing signup form for
 * students, advisors, and faculty users.
 */
@Controller
@RequestMapping("/signup")
public class SignupController {

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;

    public SignupController(FirebaseAuth firebaseAuth, Firestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    // Renders signup.html
    @GetMapping
    public String showSignupForm(Model model) {
        return "signup";
    }

    // Handles POST submission
    @PostMapping
    @ResponseBody
    public ResponseEntity<?> handleSignup(@RequestParam Map<String, String> params) {
        try {
            String userType = params.get("userType");

            if (userType == null || userType.isBlank()) {
                return ResponseEntity.badRequest().body("Missing userType");
            }

            // 1️⃣ Create Firebase Auth user
            CreateRequest request = new CreateRequest()
                    .setEmail(params.get("email"))
                    .setPassword(params.get("password"))
                    .setDisplayName(params.get("name"));

            UserRecord userRecord = firebaseAuth.createUser(request);
            String uid = userRecord.getUid();

            // 2️⃣ Prepare Firestore document based on type
            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("name", params.get("name"));
            data.put("email", params.get("email"));
            data.put("createdAt", System.currentTimeMillis());
            String successDetail = null;

            switch (userType.toLowerCase()) {
                case "student" -> {
                    data.put("studentID", params.get("studentID"));
                    data.put("majorID", params.get("majorID"));
                    data.put("advisorID", params.get("advisorID"));
                    firestore.collection("students").document(uid).set(data);
                }
                case "advisor" -> {
                    String pin = params.get("advisorPin");
                    if (!"1111".equals(pin)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid advisor PIN.");
                    }
                    String name = params.get("name");
                    String sanitized = name == null ? "" : name.toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (sanitized.isBlank()) {
                        sanitized = "advisor";
                    }
                    int randomSuffix = ThreadLocalRandom.current().nextInt(100, 1000);
                    String advisorId = sanitized + randomSuffix;
                    data.put("advisorID", advisorId);
                    firestore.collection("advisors").document(uid).set(data);
                    successDetail = "Advisor ID: " + advisorId;
                }
                case "faculty" -> {
                    String pin = params.get("facultyPin");
                    if (!"2222".equals(pin)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid faculty PIN.");
                    }
                    String name = params.get("name");
                    String sanitized = name == null ? "" : name.toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (sanitized.isBlank()) {
                        sanitized = "faculty";
                    }
                    int randomSuffix = ThreadLocalRandom.current().nextInt(100, 1000);
                    String facultyId = sanitized + randomSuffix;
                    data.put("facultyID", facultyId);
                    firestore.collection("faculty").document(uid).set(data);
                    successDetail = "Faculty ID: " + facultyId;
                }
                default -> {
                    return ResponseEntity.badRequest().body("Invalid userType: " + userType);
                }
            }

            // 3️⃣ Return success message
            String message = "✅ " + userType + " created successfully!";
            if (successDetail != null) {
                message += " " + successDetail;
            }
            return ResponseEntity.ok(message);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Signup failed: " + e.getMessage());
        }
    }
}

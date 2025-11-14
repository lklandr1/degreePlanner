package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/secure/students")
public class StudentDataController {

    private final Firestore firestore;

    public StudentDataController(Firestore firestore) {
        this.firestore = firestore;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyData(HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated student.");
        }

        ApiFuture<DocumentSnapshot> studentFuture =
                firestore.collection("students").document(uid).get();
        DocumentSnapshot studentDoc = studentFuture.get(5, TimeUnit.SECONDS);

        if (!studentDoc.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Student profile not found.");
        }

        Map<String, Object> studentData = new HashMap<>(studentDoc.getData() != null ? studentDoc.getData() : Map.of());
        studentData.put("id", studentDoc.getId());
        
        return ResponseEntity.ok(studentData);
    }
}


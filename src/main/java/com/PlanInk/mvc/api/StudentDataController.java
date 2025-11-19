package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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

    @PutMapping("/me/sandbox")
    public ResponseEntity<?> saveSandbox(HttpSession session, @RequestBody Map<String, Object> sandboxData) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated student.");
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("sandbox", sandboxData);

        ApiFuture<WriteResult> writeResult = firestore.collection("students").document(uid).update(updateData);
        writeResult.get(5, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sandbox saved successfully");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/submitForReview")
    public ResponseEntity<?> submitForReview(HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated student.");
        }

        // Set submission status to "pending"
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("scheduleSubmissionStatus", "pending");
        updateData.put("scheduleSubmittedAt", com.google.cloud.Timestamp.now());

        ApiFuture<WriteResult> writeResult = firestore.collection("students").document(uid).update(updateData);
        writeResult.get(5, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Schedule submitted for review successfully");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/approvedSchedule")
    public ResponseEntity<?> getApprovedSchedule(HttpSession session) throws Exception {
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

        @SuppressWarnings("unchecked")
        Map<String, Object> approvedSchedule = (Map<String, Object>) studentDoc.get("approvedSchedule");
        
        Map<String, Object> response = new HashMap<>();
        response.put("approvedSchedule", approvedSchedule);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/comments/{commentIndex}")
    public ResponseEntity<?> resolveComment(
            @PathVariable int commentIndex,
            HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated student.");
        }

        ApiFuture<DocumentSnapshot> studentFuture = firestore.collection("students").document(uid).get();
        DocumentSnapshot studentDoc = studentFuture.get(5, TimeUnit.SECONDS);

        if (!studentDoc.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Student profile not found.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> comments = (List<Map<String, Object>>) studentDoc.get("comments");
        
        if (comments == null || commentIndex < 0 || commentIndex >= comments.size()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid comment index.");
        }

        // Remove the comment at the specified index
        comments.remove(commentIndex);

        // Update the comments array in Firestore
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("comments", comments);

        ApiFuture<WriteResult> writeResult = firestore.collection("students").document(uid).update(updateData);
        writeResult.get(5, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Comment resolved successfully");
        
        return ResponseEntity.ok(response);
    }
}


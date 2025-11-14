package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/secure/advisors")
public class AdvisorStudentsController {

    private final Firestore firestore;

    public AdvisorStudentsController(Firestore firestore) {
        this.firestore = firestore;
    }

    @GetMapping("/me/students")
    public ResponseEntity<?> listMyStudents(HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated advisor.");
        }

        ApiFuture<DocumentSnapshot> advisorFuture =
                firestore.collection("advisors").document(uid).get();
        DocumentSnapshot advisorDoc = advisorFuture.get(5, TimeUnit.SECONDS);

        if (!advisorDoc.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Advisor profile not found.");
        }

        String advisorId = advisorDoc.getString("advisorID");
        if (advisorId == null || advisorId.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        ApiFuture<QuerySnapshot> studentsFuture = firestore.collection("students")
                .whereEqualTo("advisorID", advisorId)
                .get();
        List<QueryDocumentSnapshot> studentDocs = studentsFuture.get(5, TimeUnit.SECONDS).getDocuments();

        List<Map<String, Object>> students = new ArrayList<>();
        for (QueryDocumentSnapshot doc : studentDocs) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", doc.getId());
            row.put("studentID", doc.getString("studentID"));
            row.put("name", doc.getString("name"));
            row.put("email", doc.getString("email"));
            row.put("majorID", doc.getString("majorID"));
            
            // Include graduationDate if it exists
            @SuppressWarnings("unchecked")
            Map<String, Object> graduationDate = (Map<String, Object>) doc.get("graduationDate");
            if (graduationDate != null) {
                row.put("graduationDate", graduationDate);
            }
            
            students.add(row);
        }

        return ResponseEntity.ok(students);
    }
}


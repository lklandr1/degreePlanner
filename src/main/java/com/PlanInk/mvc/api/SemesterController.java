package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class SemesterController {

    private final Firestore firestore;

    public SemesterController(Firestore firestore) {
        this.firestore = firestore;
    }

    @GetMapping("/currentYearSemester")
    public ResponseEntity<?> getCurrentYearSemester() {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("currentYearSemester").get();
            QuerySnapshot snapshot = future.get(5, TimeUnit.SECONDS);
            
            if (snapshot.isEmpty()) {
                // Return a helpful error response instead of throwing
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No currentYearSemester document found in Firebase");
                error.put("message", "Please ensure the 'currentYearSemester' collection exists in Firebase with at least one document containing 'year' and 'semester' fields.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            // Get the first document (assuming there's only one)
            QueryDocumentSnapshot document = snapshot.getDocuments().get(0);
            Map<String, Object> data = new HashMap<>(document.getData() != null ? document.getData() : Map.of());
            data.put("id", document.getId());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            // Log the exception for debugging
            System.err.println("Error fetching currentYearSemester: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch currentYearSemester");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}


package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/majors")
public class MajorsController {

    private final Firestore firestore;

    public MajorsController(Firestore firestore) {
        this.firestore = firestore;
    }

    @GetMapping
    public List<Map<String, Object>> listMajors() throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection("majors").get();
        List<QueryDocumentSnapshot> documents = future.get(5, TimeUnit.SECONDS).getDocuments();

        List<Map<String, Object>> majors = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            Map<String, Object> data = new HashMap<>(document.getData() != null ? document.getData() : Map.of());
            data.putIfAbsent("majorID", document.getId());
            data.put("id", document.getId());
            majors.add(data);
        }
        return majors;
    }

    @PostMapping("/requirements")
    public Map<String, Object> updateRequirements(@RequestBody Map<String, Object> payload) throws Exception {
        String majorName = (String) payload.get("major");
        List<String> courses = (List<String>) payload.get("courses");

        if (majorName == null || courses == null) {
            throw new IllegalArgumentException("Missing 'major' or 'courses' in request");
        }

        // Find the major by name and update its reqs.courses array
        ApiFuture<QuerySnapshot> future = firestore.collection("majors")
                .whereEqualTo("name", majorName)
                .get();
        
        List<QueryDocumentSnapshot> documents = future.get(5, TimeUnit.SECONDS).getDocuments();
        
        if (documents.isEmpty()) {
            throw new NoSuchElementException("Major not found: " + majorName);
        }

        // Update the first matching major
        String docId = documents.get(0).getId();
        Map<String, Object> updateData = new HashMap<>();
        
        // Create nested update for reqs.courses
        Map<String, Object> reqs = new HashMap<>();
        reqs.put("courses", courses);
        updateData.put("reqs", reqs);

        ApiFuture<WriteResult> writeResult = firestore.collection("majors").document(docId).update(updateData);
        writeResult.get(5, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Requirements updated successfully");
        response.put("majorId", docId);
        return response;
    }
}


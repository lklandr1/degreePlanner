package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.PlanInk.models.ChangeLog;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/changelog")
public class ChangeLogController {

    /**
     * Record a new changelog entry
     * POST /api/changelog/record
     * Body: {
     *   "changeType": "major" or "course",
     *   "description": "System-generated description",
     *   "notes": "User notes",
     *   "facultyEmail": "faculty@example.com"
     * }
     */
    @PostMapping("/record")
    public Map<String, Object> recordChange(@RequestBody Map<String, Object> request) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        
        String changeType = (String) request.get("changeType");
        String description = (String) request.get("description");
        String notes = (String) request.get("notes");
        String facultyEmail = (String) request.get("facultyEmail");

        // Validate required fields
        if (changeType == null || !changeType.matches("major|course")) {
            throw new IllegalArgumentException("changeType must be 'major' or 'course'");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("description is required");
        }

        // Create changelog entry
        ChangeLog changeLog = new ChangeLog(
            changeType,
            Instant.now().toString(),
            description,
            notes != null ? notes : "",
            facultyEmail != null ? facultyEmail : "unknown"
        );

        // Add to Firestore changeLog collection
        DocumentReference docRef = db.collection("changeLog").document();
        ApiFuture<WriteResult> future = docRef.set(changeLog.toMap());
        WriteResult result = future.get(15, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", docRef.getId());
        response.put("message", "Changelog entry recorded successfully");
        response.put("timestamp", result.getUpdateTime().toString());
        return response;
    }

    /**
     * Get all changelog entries
     * GET /api/changelog/all?limit=50
     */
    @GetMapping("/all")
    public List<Map<String, Object>> getAllChanges(@RequestParam(defaultValue = "100") int limit) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection("changeLog")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit)
            .get();
        
        List<QueryDocumentSnapshot> docs = future.get(15, TimeUnit.SECONDS).getDocuments();
        List<Map<String, Object>> changes = new ArrayList<>();

        for (QueryDocumentSnapshot doc : docs) {
            Map<String, Object> data = new HashMap<>(doc.getData());
            data.put("id", doc.getId());
            changes.add(data);
        }

        return changes;
    }

    /**
     * Get changelog entries filtered by changeType
     * GET /api/changelog/filter?type=course&limit=50
     */
    @GetMapping("/filter")
    public List<Map<String, Object>> getChangesByType(
            @RequestParam String type,
            @RequestParam(defaultValue = "100") int limit) throws Exception {
        
        if (!type.matches("major|course")) {
            throw new IllegalArgumentException("type must be 'major' or 'course'");
        }

        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection("changeLog")
            .whereEqualTo("changeType", type)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit)
            .get();

        List<QueryDocumentSnapshot> docs = future.get(15, TimeUnit.SECONDS).getDocuments();
        List<Map<String, Object>> changes = new ArrayList<>();

        for (QueryDocumentSnapshot doc : docs) {
            Map<String, Object> data = new HashMap<>(doc.getData());
            data.put("id", doc.getId());
            changes.add(data);
        }

        return changes;
    }

    /**
     * Get changelog entries by faculty member
     * GET /api/changelog/faculty?email=faculty@example.com&limit=50
     */
    @GetMapping("/faculty")
    public List<Map<String, Object>> getChangesByFaculty(
            @RequestParam String email,
            @RequestParam(defaultValue = "100") int limit) throws Exception {
        
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection("changeLog")
            .whereEqualTo("facultyEmail", email)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit)
            .get();

        List<QueryDocumentSnapshot> docs = future.get(15, TimeUnit.SECONDS).getDocuments();
        List<Map<String, Object>> changes = new ArrayList<>();

        for (QueryDocumentSnapshot doc : docs) {
            Map<String, Object> data = new HashMap<>(doc.getData());
            data.put("id", doc.getId());
            changes.add(data);
        }

        return changes;
    }

    /**
     * Get a single changelog entry by ID
     * GET /api/changelog/{id}
     */
    @GetMapping("/{id}")
    public Map<String, Object> getChangeById(@PathVariable String id) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot snap = db.collection("changeLog")
            .document(id)
            .get()
            .get(15, TimeUnit.SECONDS);

        if (!snap.exists()) {
            throw new NoSuchElementException("Changelog entry not found: " + id);
        }

        Map<String, Object> data = new HashMap<>(snap.getData());
        data.put("id", snap.getId());
        return data;
    }

    /**
     * Delete a changelog entry (admin/faculty only)
     * DELETE /api/changelog/{id}
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteChange(@PathVariable String id) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<WriteResult> future = db.collection("changeLog").document(id).delete();
        WriteResult result = future.get(15, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Changelog entry deleted successfully");
        response.put("timestamp", result.getUpdateTime().toString());
        return response;
    }

    /**
     * Update changelog notes (faculty can update their own notes)
     * PUT /api/changelog/{id}/notes
     */
    @PutMapping("/{id}/notes")
    public Map<String, Object> updateNotes(
            @PathVariable String id,
            @RequestBody Map<String, Object> request) throws Exception {
        
        Firestore db = FirestoreClient.getFirestore();
        String notes = (String) request.get("notes");

        if (notes == null) {
            throw new IllegalArgumentException("notes field is required");
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("notes", notes);

        ApiFuture<WriteResult> future = db.collection("changeLog")
            .document(id)
            .update(updates);
        
        WriteResult result = future.get(15, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Changelog notes updated successfully");
        response.put("timestamp", result.getUpdateTime().toString());
        return response;
    }
}

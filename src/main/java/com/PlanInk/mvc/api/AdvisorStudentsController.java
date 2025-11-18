package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/students/{studentId}/completedCourses")
    public ResponseEntity<?> updateCompletedCourses(
            @PathVariable String studentId,
            @RequestBody Map<String, Object> payload,
            HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated advisor.");
        }

        // Verify advisor has access to this student
        ApiFuture<DocumentSnapshot> studentFuture = firestore.collection("students").document(studentId).get();
        DocumentSnapshot studentDoc = studentFuture.get(5, TimeUnit.SECONDS);
        
        if (!studentDoc.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Student not found.");
        }

        // Verify advisor is assigned to this student
        ApiFuture<DocumentSnapshot> advisorFuture = firestore.collection("advisors").document(uid).get();
        DocumentSnapshot advisorDoc = advisorFuture.get(5, TimeUnit.SECONDS);
        String advisorId = advisorDoc.getString("advisorID");
        String studentAdvisorId = studentDoc.getString("advisorID");
        
        if (advisorId == null || !advisorId.equals(studentAdvisorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have access to this student.");
        }

        @SuppressWarnings("unchecked")
        List<String> newCourses = (List<String>) payload.get("courses");
        if (newCourses == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Courses list is required.");
        }

        // Get existing completed courses
        @SuppressWarnings("unchecked")
        List<String> existingCourses = (List<String>) studentDoc.get("completedCourses");
        if (existingCourses == null) {
            existingCourses = new ArrayList<>();
        }

        // Merge new courses with existing ones (avoid duplicates)
        List<String> updatedCourses = new ArrayList<>(existingCourses);
        for (String course : newCourses) {
            if (!updatedCourses.contains(course)) {
                updatedCourses.add(course);
            }
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("completedCourses", updatedCourses);

        ApiFuture<WriteResult> writeResult = firestore.collection("students").document(studentId).update(updateData);
        writeResult.get(5, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Completed courses updated successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/students/{studentId}/completedCourses/{courseId}")
    public ResponseEntity<?> removeCompletedCourse(
            @PathVariable String studentId,
            @PathVariable String courseId,
            HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated advisor.");
        }

        // Verify advisor has access to this student
        ApiFuture<DocumentSnapshot> studentFuture = firestore.collection("students").document(studentId).get();
        DocumentSnapshot studentDoc = studentFuture.get(5, TimeUnit.SECONDS);
        
        if (!studentDoc.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Student not found.");
        }

        // Verify advisor is assigned to this student
        ApiFuture<DocumentSnapshot> advisorFuture = firestore.collection("advisors").document(uid).get();
        DocumentSnapshot advisorDoc = advisorFuture.get(5, TimeUnit.SECONDS);
        String advisorId = advisorDoc.getString("advisorID");
        String studentAdvisorId = studentDoc.getString("advisorID");
        
        if (advisorId == null || !advisorId.equals(studentAdvisorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have access to this student.");
        }

        // Get existing completed courses
        @SuppressWarnings("unchecked")
        List<String> existingCourses = (List<String>) studentDoc.get("completedCourses");
        if (existingCourses == null) {
            existingCourses = new ArrayList<>();
        }

        // Remove the course if it exists
        if (!existingCourses.contains(courseId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Course not found in completed courses.");
        }

        List<String> updatedCourses = new ArrayList<>(existingCourses);
        updatedCourses.remove(courseId);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("completedCourses", updatedCourses);

        ApiFuture<WriteResult> writeResult = firestore.collection("students").document(studentId).update(updateData);
        writeResult.get(5, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Course removed successfully");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/students/{studentId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable String studentId,
            @RequestBody Map<String, Object> payload,
            HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated advisor.");
        }

        // Verify advisor has access to this student
        ApiFuture<DocumentSnapshot> studentFuture = firestore.collection("students").document(studentId).get();
        DocumentSnapshot studentDoc = studentFuture.get(5, TimeUnit.SECONDS);
        
        if (!studentDoc.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Student not found.");
        }

        // Verify advisor is assigned to this student
        ApiFuture<DocumentSnapshot> advisorFuture = firestore.collection("advisors").document(uid).get();
        DocumentSnapshot advisorDoc = advisorFuture.get(5, TimeUnit.SECONDS);
        String advisorId = advisorDoc.getString("advisorID");
        String studentAdvisorId = studentDoc.getString("advisorID");
        
        if (advisorId == null || !advisorId.equals(studentAdvisorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have access to this student.");
        }

        String comment = (String) payload.get("comment");
        if (comment == null || comment.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Comment is required.");
        }

        // Create comment object with timestamp
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("comment", comment);
        commentData.put("advisorId", advisorId);
        commentData.put("advisorName", advisorDoc.getString("name"));
        commentData.put("timestamp", Timestamp.now());

        // Add comment to student's comments array
        ApiFuture<WriteResult> writeResult = firestore.collection("students").document(studentId)
                .update("comments", FieldValue.arrayUnion(commentData));
        writeResult.get(5, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Comment added successfully");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/students/{studentId}/data")
    public ResponseEntity<?> getStudentData(
            @PathVariable String studentId,
            HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated advisor.");
        }

        // Verify advisor has access to this student
        ApiFuture<DocumentSnapshot> studentFuture = firestore.collection("students").document(studentId).get();
        DocumentSnapshot studentDoc = studentFuture.get(5, TimeUnit.SECONDS);
        
        if (!studentDoc.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Student not found.");
        }

        // Verify advisor is assigned to this student
        ApiFuture<DocumentSnapshot> advisorFuture = firestore.collection("advisors").document(uid).get();
        DocumentSnapshot advisorDoc = advisorFuture.get(5, TimeUnit.SECONDS);
        String advisorId = advisorDoc.getString("advisorID");
        String studentAdvisorId = studentDoc.getString("advisorID");
        
        if (advisorId == null || !advisorId.equals(studentAdvisorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have access to this student.");
        }

        // Get student data
        Map<String, Object> studentData = new HashMap<>(studentDoc.getData() != null ? studentDoc.getData() : Map.of());
        
        // Return only relevant data (comments and completed courses)
        Map<String, Object> response = new HashMap<>();
        response.put("comments", studentData.get("comments"));
        response.put("completedCourses", studentData.get("completedCourses"));
        
        return ResponseEntity.ok(response);
    }
}


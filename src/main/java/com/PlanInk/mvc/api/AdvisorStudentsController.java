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
            
            // Include schedule submission status
            String submissionStatus = doc.getString("scheduleSubmissionStatus");
            row.put("scheduleSubmissionStatus", submissionStatus != null ? submissionStatus : "none");
            
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

    @GetMapping("/students/{studentId}/submittedSchedule")
    public ResponseEntity<?> getSubmittedSchedule(
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

        // Check if schedule is submitted for review
        String submissionStatus = studentDoc.getString("scheduleSubmissionStatus");
        if (!"pending".equals(submissionStatus)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No schedule submitted for review.");
        }

        // Get sandbox data
        @SuppressWarnings("unchecked")
        Map<String, Object> sandbox = (Map<String, Object>) studentDoc.get("sandbox");
        
        Map<String, Object> response = new HashMap<>();
        response.put("sandbox", sandbox);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/students/{studentId}/acceptSchedule")
    public ResponseEntity<?> acceptSchedule(
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

        // Check if schedule is submitted for review
        String submissionStatus = studentDoc.getString("scheduleSubmissionStatus");
        if (!"pending".equals(submissionStatus)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No schedule submitted for review.");
        }

        // Get sandbox data and copy to approvedSchedule
        @SuppressWarnings("unchecked")
        Map<String, Object> sandbox = (Map<String, Object>) studentDoc.get("sandbox");
        
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("approvedSchedule", sandbox);
        updateData.put("scheduleSubmissionStatus", "accepted");
        updateData.put("scheduleApprovedAt", Timestamp.now());

        ApiFuture<WriteResult> writeResult = firestore.collection("students").document(studentId).update(updateData);
        writeResult.get(5, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Schedule accepted successfully");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/students/{studentId}/rejectSchedule")
    public ResponseEntity<?> rejectSchedule(
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

        // Check if schedule is submitted for review
        String submissionStatus = studentDoc.getString("scheduleSubmissionStatus");
        if (!"pending".equals(submissionStatus)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No schedule submitted for review.");
        }

        String comment = (String) payload.get("comment");
        if (comment == null || comment.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Comment is required for rejection.");
        }

        // Create comment object with timestamp
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("comment", "Schedule Rejected: " + comment);
        commentData.put("advisorId", advisorId);
        commentData.put("advisorName", advisorDoc.getString("name"));
        commentData.put("timestamp", Timestamp.now());

        // Update student: add comment, clear submission status
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("scheduleSubmissionStatus", "rejected");
        updateData.put("scheduleRejectedAt", Timestamp.now());

        // Add comment and update status in one transaction
        ApiFuture<WriteResult> writeResult = firestore.collection("students").document(studentId)
                .update(updateData);
        writeResult.get(5, TimeUnit.SECONDS);

        // Add comment separately
        ApiFuture<WriteResult> commentResult = firestore.collection("students").document(studentId)
                .update("comments", FieldValue.arrayUnion(commentData));
        commentResult.get(5, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Schedule rejected and comment sent");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/studentsNeedingReview")
    public ResponseEntity<?> getStudentsNeedingReview(HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null || uid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No authenticated advisor.");
        }

        // Verify advisor exists
        ApiFuture<DocumentSnapshot> advisorFuture = firestore.collection("advisors").document(uid).get();
        DocumentSnapshot advisorDoc = advisorFuture.get(5, TimeUnit.SECONDS);
        
        if (!advisorDoc.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Advisor profile not found.");
        }

        String advisorId = advisorDoc.getString("advisorID");
        if (advisorId == null || advisorId.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        // Get current semester to determine previous semester
        ApiFuture<QuerySnapshot> currentSemesterFuture = firestore.collection("currentYearSemester").get();
        QuerySnapshot currentSemesterSnapshot = currentSemesterFuture.get(5, TimeUnit.SECONDS);
        
        if (currentSemesterSnapshot.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Current semester not found.");
        }
        
        QueryDocumentSnapshot currentSemesterDoc = currentSemesterSnapshot.getDocuments().get(0);
        String currentSemester = currentSemesterDoc.getString("semester");
        Object yearObj = currentSemesterDoc.get("year");
        
        if (currentSemester == null || yearObj == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid semester data.");
        }
        
        // Handle year as either Number or String
        int currentYear;
        if (yearObj instanceof Number) {
            currentYear = ((Number) yearObj).intValue();
        } else if (yearObj instanceof String) {
            try {
                currentYear = Integer.parseInt((String) yearObj);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid year format in database: " + yearObj);
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Year must be a number or numeric string, got: " + yearObj.getClass().getSimpleName());
        }
        String normalizedCurrentSemester = normalizeSemester(currentSemester);
        
        // Calculate previous semester
        String previousSemester;
        int previousYear;
        
        if ("Spring".equalsIgnoreCase(normalizedCurrentSemester)) {
            previousSemester = "Fall";
            previousYear = currentYear;
        } else {
            previousSemester = "Spring";
            previousYear = currentYear - 1;
        }

        // Get all students assigned to this advisor
        ApiFuture<QuerySnapshot> studentsFuture = firestore.collection("students")
                .whereEqualTo("advisorID", advisorId)
                .get();
        List<QueryDocumentSnapshot> studentDocs = studentsFuture.get(10, TimeUnit.SECONDS).getDocuments();

        List<Map<String, Object>> studentsNeedingReview = new ArrayList<>();
        
        for (QueryDocumentSnapshot studentDoc : studentDocs) {
            @SuppressWarnings("unchecked")
            Map<String, Object> approvedSchedule = (Map<String, Object>) studentDoc.get("approvedSchedule");
            
            if (approvedSchedule == null) {
                continue;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> semesters = (List<Map<String, Object>>) approvedSchedule.get("semesters");
            
            if (semesters == null) {
                continue;
            }
            
            // Find courses in the previous semester
            List<String> coursesInPreviousSemester = new ArrayList<>();
            for (Map<String, Object> sem : semesters) {
                String semSemester = normalizeSemester((String) sem.get("semester"));
                Object semYearObj = sem.get("year");
                
                // Handle year as either Number or String
                int semYear = 0;
                if (semYearObj instanceof Number) {
                    semYear = ((Number) semYearObj).intValue();
                } else if (semYearObj instanceof String) {
                    try {
                        semYear = Integer.parseInt((String) semYearObj);
                    } catch (NumberFormatException e) {
                        // Skip this semester if year is invalid
                        continue;
                    }
                }
                
                if (semSemester != null && semSemester.equalsIgnoreCase(previousSemester) && semYear == previousYear) {
                    @SuppressWarnings("unchecked")
                    List<String> courses = (List<String>) sem.get("courses");
                    if (courses != null && !courses.isEmpty()) {
                        coursesInPreviousSemester.addAll(courses);
                    }
                }
            }
            
            if (!coursesInPreviousSemester.isEmpty()) {
                // Get existing completed courses to mark which are already completed
                @SuppressWarnings("unchecked")
                List<String> completedCourses = (List<String>) studentDoc.get("completedCourses");
                if (completedCourses == null) {
                    completedCourses = new ArrayList<>();
                }
                
                Map<String, Object> studentInfo = new HashMap<>();
                studentInfo.put("studentId", studentDoc.getId());
                studentInfo.put("studentName", studentDoc.getString("name"));
                studentInfo.put("studentID", studentDoc.getString("studentID"));
                studentInfo.put("courses", coursesInPreviousSemester);
                studentInfo.put("completedCourses", completedCourses);
                studentInfo.put("semester", previousSemester);
                studentInfo.put("year", previousYear);
                studentsNeedingReview.add(studentInfo);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("students", studentsNeedingReview);
        response.put("previousSemester", previousSemester);
        response.put("previousYear", previousYear);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/students/{studentId}/submitCourses")
    public ResponseEntity<?> submitCourses(
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
        List<String> coursesToSubmit = (List<String>) payload.get("courses");
        if (coursesToSubmit == null) {
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
        for (String course : coursesToSubmit) {
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
        response.put("message", "Courses submitted successfully");
        response.put("submittedCount", coursesToSubmit.size());
        
        return ResponseEntity.ok(response);
    }
    
    private String normalizeSemester(String semester) {
        if (semester == null) return "Fall";
        String normalized = semester.substring(0, 1).toUpperCase() + semester.substring(1).toLowerCase();
        return normalized.equals("Fall") || normalized.equals("Spring") ? normalized : "Fall";
    }
}


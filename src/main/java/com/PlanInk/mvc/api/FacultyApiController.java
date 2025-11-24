package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/secure/faculty")
public class FacultyApiController {

    private final Firestore firestore;

    public FacultyApiController(Firestore firestore) {
        this.firestore = firestore;
    }

    @PostMapping("/advanceSemester")
    public ResponseEntity<?> advanceSemester(HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        String role = (String) session.getAttribute("role");
        
        if (uid == null || uid.isBlank() || !"FACULTY".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: Faculty access required.");
        }

        try {
            // Get current semester
            ApiFuture<QuerySnapshot> currentSemesterFuture = firestore.collection("currentYearSemester").get();
            QuerySnapshot currentSemesterSnapshot = currentSemesterFuture.get(5, TimeUnit.SECONDS);
            
            if (currentSemesterSnapshot.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Current semester not found in database.");
            }
            
            QueryDocumentSnapshot currentSemesterDoc = currentSemesterSnapshot.getDocuments().get(0);
            String currentSemester = currentSemesterDoc.getString("semester");
            Object yearObj = currentSemesterDoc.get("year");
            
            if (currentSemester == null || yearObj == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid semester data in database.");
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
            String normalizedSemester = normalizeSemester(currentSemester);
            
            // Calculate new semester
            String newSemester;
            int newYear;
            
            if ("Fall".equalsIgnoreCase(normalizedSemester)) {
                newSemester = "Spring";
                newYear = currentYear + 1;
            } else {
                newSemester = "Fall";
                newYear = currentYear;
            }
            
            // Find all students with courses in the current semester
            List<Map<String, Object>> studentsToReview = findStudentsWithCoursesInSemester(normalizedSemester, currentYear);
            
            // Update currentYearSemester
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("semester", newSemester);
            updateData.put("year", newYear);
            
            ApiFuture<WriteResult> writeResult = firestore.collection("currentYearSemester")
                    .document(currentSemesterDoc.getId())
                    .update(updateData);
            writeResult.get(5, TimeUnit.SECONDS);
            
            // Remove the completed semester from all students' approvedSchedules
            removeSemesterFromStudentSchedules(normalizedSemester, currentYear);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("oldSemester", normalizedSemester);
            response.put("oldYear", currentYear);
            response.put("newSemester", newSemester);
            response.put("newYear", newYear);
            response.put("studentsToReview", studentsToReview);
            response.put("message", "Semester advanced successfully. " + studentsToReview.size() + 
                    " student(s) have courses that need advisor review.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error advancing semester: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to advance semester");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    private List<Map<String, Object>> findStudentsWithCoursesInSemester(String semester, int year) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // Get all students
        ApiFuture<QuerySnapshot> studentsFuture = firestore.collection("students").get();
        QuerySnapshot studentsSnapshot = studentsFuture.get(10, TimeUnit.SECONDS);
        
        for (QueryDocumentSnapshot studentDoc : studentsSnapshot.getDocuments()) {
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
            
            // Find courses in the current semester
            List<String> coursesInSemester = new ArrayList<>();
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
                
                if (semSemester != null && semSemester.equalsIgnoreCase(semester) && semYear == year) {
                    @SuppressWarnings("unchecked")
                    List<String> courses = (List<String>) sem.get("courses");
                    if (courses != null && !courses.isEmpty()) {
                        coursesInSemester.addAll(courses);
                    }
                }
            }
            
            if (!coursesInSemester.isEmpty()) {
                Map<String, Object> studentInfo = new HashMap<>();
                studentInfo.put("studentId", studentDoc.getId());
                studentInfo.put("studentName", studentDoc.getString("name"));
                studentInfo.put("studentID", studentDoc.getString("studentID"));
                studentInfo.put("advisorID", studentDoc.getString("advisorID"));
                studentInfo.put("courses", coursesInSemester);
                result.add(studentInfo);
            }
        }
        
        return result;
    }
    
    private void removeSemesterFromStudentSchedules(String semester, int year) throws Exception {
        // Get all students
        ApiFuture<QuerySnapshot> studentsFuture = firestore.collection("students").get();
        QuerySnapshot studentsSnapshot = studentsFuture.get(10, TimeUnit.SECONDS);
        
        int updatedCount = 0;
        
        for (QueryDocumentSnapshot studentDoc : studentsSnapshot.getDocuments()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> approvedSchedule = (Map<String, Object>) studentDoc.get("approvedSchedule");
            
            if (approvedSchedule == null) {
                continue;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> semesters = (List<Map<String, Object>>) approvedSchedule.get("semesters");
            
            if (semesters == null || semesters.isEmpty()) {
                continue;
            }
            
            // Find and remove the matching semester
            boolean found = false;
            List<Map<String, Object>> updatedSemesters = new ArrayList<>();
            
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
                        // Keep this semester if year is invalid (don't remove it)
                        updatedSemesters.add(sem);
                        continue;
                    }
                } else {
                    // Keep this semester if year is not a recognized type
                    updatedSemesters.add(sem);
                    continue;
                }
                
                // Skip the semester that matches the completed one
                if (semSemester != null && semSemester.equalsIgnoreCase(semester) && semYear == year) {
                    found = true;
                    // Don't add this semester to the updated list
                } else {
                    // Keep this semester
                    updatedSemesters.add(sem);
                }
            }
            
            // Only update if we found and removed a semester
            if (found) {
                Map<String, Object> updatedSchedule = new HashMap<>(approvedSchedule);
                updatedSchedule.put("semesters", updatedSemesters);
                
                Map<String, Object> studentUpdateData = new HashMap<>();
                studentUpdateData.put("approvedSchedule", updatedSchedule);
                
                ApiFuture<WriteResult> updateResult = firestore.collection("students")
                        .document(studentDoc.getId())
                        .update(studentUpdateData);
                updateResult.get(5, TimeUnit.SECONDS);
                updatedCount++;
            }
        }
        
        System.out.println("Removed semester " + semester + " " + year + " from " + updatedCount + " student(s) approved schedules.");
    }
    
    private String normalizeSemester(String semester) {
        if (semester == null) return "Fall";
        String normalized = semester.substring(0, 1).toUpperCase() + semester.substring(1).toLowerCase();
        return normalized.equals("Fall") || normalized.equals("Spring") ? normalized : "Fall";
    }
}


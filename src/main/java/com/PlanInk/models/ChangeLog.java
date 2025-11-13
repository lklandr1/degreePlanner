package com.PlanInk.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a changelog entry in Firestore.
 * Records changes made to courses or majors by faculty members.
 */
public class ChangeLog {
    private String id;
    private String changeType;        // "major" or "course"
    private String date;              // ISO 8601 timestamp
    private String description;       // System-generated description of changes
    private String notes;             // User-provided explanation of changes
    private String facultyEmail;      // Email of faculty member who made the change

    public ChangeLog() {
    }

    public ChangeLog(String changeType, String date, String description, String notes, String facultyEmail) {
        this.changeType = changeType;
        this.date = date;
        this.description = description;
        this.notes = notes;
        this.facultyEmail = facultyEmail;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFacultyEmail() {
        return facultyEmail;
    }

    public void setFacultyEmail(String facultyEmail) {
        this.facultyEmail = facultyEmail;
    }

    /**
     * Convert ChangeLog to Firestore-compatible Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("changeType", this.changeType);
        map.put("date", this.date);
        map.put("description", this.description);
        map.put("notes", this.notes);
        map.put("facultyEmail", this.facultyEmail);
        return map;
    }

    /**
     * Create ChangeLog from Firestore Map
     */
    public static ChangeLog fromMap(Map<String, Object> map) {
        ChangeLog changeLog = new ChangeLog();
        if (map.containsKey("changeType")) {
            changeLog.setChangeType((String) map.get("changeType"));
        }
        if (map.containsKey("date")) {
            changeLog.setDate((String) map.get("date"));
        }
        if (map.containsKey("description")) {
            changeLog.setDescription((String) map.get("description"));
        }
        if (map.containsKey("notes")) {
            changeLog.setNotes((String) map.get("notes"));
        }
        if (map.containsKey("facultyEmail")) {
            changeLog.setFacultyEmail((String) map.get("facultyEmail"));
        }
        return changeLog;
    }
}

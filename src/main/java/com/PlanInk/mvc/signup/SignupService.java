package com.PlanInk.mvc.signup;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class SignupService {

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;

    public SignupService(FirebaseAuth firebaseAuth, Firestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    public String signupStudent(StudentDto dto) throws Exception {

        CreateRequest request = new CreateRequest()
                .setEmail(dto.getEmail())
                .setPassword(dto.getPassword())
                .setDisplayName(dto.getName());

        UserRecord userRecord = firebaseAuth.createUser(request);
        String uid = userRecord.getUid();

        // 2) prepare Firestore document (don't store password)
        Map<String, Object> doc = new HashMap<>();
        doc.put("studentID", dto.getStudentID());
        doc.put("email", dto.getEmail());
        doc.put("majorID", dto.getMajorID());
        doc.put("name", dto.getName());
        doc.put("advisorID", dto.getAdvisorID());
        doc.put("authUid", uid);
        doc.put("createdAt", System.currentTimeMillis());

        // 3) save to 'students' collection using uid as document id
        ApiFuture<WriteResult> writeResult = firestore.collection("students").document(uid).set(doc);
        try {
            WriteResult result = writeResult.get();
            return uid;
        } catch (InterruptedException | ExecutionException e) {
            // If Firestore write fails, consider deleting the created Auth user to avoid orphaned auth accounts
            try {
                firebaseAuth.deleteUser(uid);
            } catch (Exception ignored) {}
            throw new Exception("Failed to write student document: " + e.getMessage(), e);
        }
    }

    // Basic generic handlers for advisor/faculty (stub implementations to be extended later)
//    public String signupAdvisor(Map<String, Object> advisorData) throws Exception {
//        // Minimal example: must contain email & password & name
//        String email = (String) advisorData.get("email");
//        String password = (String) advisorData.get("password");
//        String name = (String) advisorData.get("name");
//        if (email == null || password == null || name == null) {
//            throw new IllegalArgumentException("advisor must include email, password, and name");
//        }
//
//        CreateRequest request = CreateRequest.builder()
//                .setEmail(email)
//                .setPassword(password)
//                .setDisplayName(name)
//                .build();
//
//        UserRecord userRecord;
//        try {
//            userRecord = firebaseAuth.createUser(request);
//        } catch (FirebaseAuthException e) {
//            throw new Exception("Failed to create auth user: " + e.getMessage(), e);
//        }
//
//        String uid = userRecord.getUid();
//        advisorData.remove("password"); // don't store
//        advisorData.put("authUid", uid);
//        advisorData.put("createdAt", System.currentTimeMillis());
//
//        ApiFuture<WriteResult> writeResult = firestore.collection("advisors").document(uid).set(advisorData);
//        try {
//            writeResult.get();
//            return uid;
//        } catch (Exception e) {
//            try { firebaseAuth.deleteUser(uid); } catch (Exception ignored) {}
//            throw new Exception("Failed to write advisor document: " + e.getMessage(), e);
//        }
//    }
//
//    public String signupFaculty(Map<String, Object> facultyData) throws Exception {
//        // implement similar to advisor
//        String email = (String) facultyData.get("email");
//        String password = (String) facultyData.get("password");
//        String name = (String) facultyData.get("name");
//        if (email == null || password == null || name == null) {
//            throw new IllegalArgumentException("faculty must include email, password, and name");
//        }
//
//        CreateRequest request = CreateRequest.builder()
//                .setEmail(email)
//                .setPassword(password)
//                .setDisplayName(name)
//                .build();
//
//        UserRecord userRecord;
//        try {
//            userRecord = firebaseAuth.createUser(request);
//        } catch (FirebaseAuthException e) {
//            throw new Exception("Failed to create auth user: " + e.getMessage(), e);
//        }
//
//        String uid = userRecord.getUid();
//        facultyData.remove("password");
//        facultyData.put("authUid", uid);
//        facultyData.put("createdAt", System.currentTimeMillis());
//
//        ApiFuture<WriteResult> writeResult = firestore.collection("faculty").document(uid).set(facultyData);
//        try {
//            writeResult.get();
//            return uid;
//        } catch (Exception e) {
//            try { firebaseAuth.deleteUser(uid); } catch (Exception ignored) {}
//            throw new Exception("Failed to write faculty document: " + e.getMessage(), e);
//        }
//    }
}

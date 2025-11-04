package com.PlanInk.mvc.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.api.core.ApiFuture;
import com.google.firebase.cloud.FirestoreClient;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@RestController
public class FirebaseTestController {

    @GetMapping("/firestore/test-write")
    public Map<String,Object> firestoreWrite() throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference doc = db.collection("test").document("spring");

        Map<String,Object> payload = new HashMap<>();
        payload.put("hello","world");
        payload.put("ts", Instant.now().toString());

        ApiFuture<WriteResult> fut = doc.set(payload);
        WriteResult wr = fut.get(5, TimeUnit.SECONDS);

        Map<String,Object> out = new HashMap<>();
        out.put("wrote", payload);
        out.put("docPath", "test/spring");
        out.put("updateTime", wr.getUpdateTime().toString());
        return out;
    }

    @GetMapping("/firestore/test")
    public ResponseEntity<?> firestoreRead() throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference doc = db.collection("test").document("spring");

        ApiFuture<DocumentSnapshot> fut = doc.get();
        DocumentSnapshot snap = fut.get(5, TimeUnit.SECONDS);

        if (!snap.exists()) {
            return ResponseEntity.ok(Map.of("exists", false));
        }
        return ResponseEntity.ok(snap.getData());
    }
}

package com.PlanInk.mvc.api;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class DevSeedController {
    @PostMapping("/dev/seed-catalog")
    public Map<String,Object> seed() throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        db.collection("catalog").document("MATH101").set(Map.of(
            "code","MATH101","title","Calculus I",
            "description","Limits, derivatives, and applications.","credits",4
        )).get();

        db.collection("catalog").document("CS101").set(Map.of(
            "code","CS101","title","Intro to Computer Science",
            "description","Programming fundamentals in Java.","credits",3
        )).get();

        db.collection("catalog").document("ENG201").set(Map.of(
            "code","ENG201","title","Academic Writing",
            "description","Composition, rhetoric, and research writing.","credits",3
        )).get();

        return Map.of("seeded", true, "count", 3);
    }
}

package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/advisors")
public class AdvisorsController {

    private final Firestore firestore;

    public AdvisorsController(Firestore firestore) {
        this.firestore = firestore;
    }

    @GetMapping
    public List<Map<String, Object>> listAdvisors() throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection("advisors").get();
        List<QueryDocumentSnapshot> documents = future.get(5, TimeUnit.SECONDS).getDocuments();

        List<Map<String, Object>> advisors = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            Map<String, Object> data = new HashMap<>(document.getData() != null ? document.getData() : Map.of());
            data.putIfAbsent("advisorID", document.getId());
            data.put("id", document.getId());
            advisors.add(data);
        }
        return advisors;
    }
}


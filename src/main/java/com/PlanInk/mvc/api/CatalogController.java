package com.PlanInk.mvc.api;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
public class CatalogController {

    // GET /catalog  -> list first N courses (default 50)
    @GetMapping("/catalog")
    public List<Map<String,Object>> listCatalog(@RequestParam(defaultValue = "50") int limit) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> fut = db.collection("catalog").limit(limit).get();
        List<QueryDocumentSnapshot> docs = fut.get(5, TimeUnit.SECONDS).getDocuments();

        List<Map<String,Object>> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : docs) {
            Map<String,Object> m = new HashMap<>(d.getData());
            m.put("id", d.getId());
            out.add(m);
        }
        return out;
    }

    // GET /courses/search?q=math  -> naive text contains search (until we add indexes)
    @GetMapping("/courses/search")
    public List<Map<String,Object>> searchCourses(@RequestParam String q,
                                                  @RequestParam(defaultValue = "50") int limit) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> fut = db.collection("catalog").limit(200).get();
        List<QueryDocumentSnapshot> docs = fut.get(5, TimeUnit.SECONDS).getDocuments();

        String needle = q.toLowerCase();
        List<Map<String,Object>> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : docs) {
            Map<String,Object> data = d.getData();
            String hay = (
                    String.valueOf(data.getOrDefault("title","")) + " " +
                    String.valueOf(data.getOrDefault("code","")) + " " +
                    String.valueOf(data.getOrDefault("description",""))
            ).toLowerCase();
            if (hay.contains(needle)) {
                Map<String,Object> m = new HashMap<>(data);
                m.put("id", d.getId());
                out.add(m);
                if (out.size() >= limit) break;
            }
        }
        return out;
    }

    // GET /courses/{id} -> fetch a single course by doc id
    @GetMapping("/courses/{id}")
    public Map<String,Object> getCourse(@PathVariable String id) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot snap = db.collection("catalog").document(id).get().get(5, TimeUnit.SECONDS);
        if (!snap.exists()) throw new NoSuchElementException("Not found: " + id);
        Map<String,Object> m = new HashMap<>(snap.getData());
        m.put("id", snap.getId());
        return m;
    }

    // GET /coreCourses -> list core courses from coreCourses collection
    @GetMapping("/coreCourses")
    public List<Map<String,Object>> listCoreCourses(@RequestParam(defaultValue = "200") int limit) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> fut = db.collection("coreCourses").limit(limit).get();
        List<QueryDocumentSnapshot> docs = fut.get(5, TimeUnit.SECONDS).getDocuments();

        List<Map<String,Object>> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : docs) {
            Map<String,Object> m = new HashMap<>(d.getData());
            m.put("id", d.getId());
            out.add(m);
        }
        return out;
    }

    // GET /api/courses -> list all courses from courses collection
    @GetMapping("/api/courses")
    public List<Map<String,Object>> listAllCourses(@RequestParam(defaultValue = "1000") int limit) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> fut = db.collection("courses").limit(limit).get();
        List<QueryDocumentSnapshot> docs = fut.get(5, TimeUnit.SECONDS).getDocuments();

        List<Map<String,Object>> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : docs) {
            Map<String,Object> m = new HashMap<>(d.getData());
            m.put("id", d.getId());
            out.add(m);
        }
        return out;
    }

    // GET /api/requirements/{id} -> fetch a requirements document from the requirements collection
    @GetMapping("/api/requirements/{id}")
    public Map<String,Object> getRequirements(@PathVariable String id) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot snap = db.collection("requirements").document(id).get().get(5, TimeUnit.SECONDS);
        if (!snap.exists()) throw new NoSuchElementException("Requirements not found: " + id);
        Map<String,Object> m = new HashMap<>(snap.getData());
        m.put("id", snap.getId());
        return m;
    }
}

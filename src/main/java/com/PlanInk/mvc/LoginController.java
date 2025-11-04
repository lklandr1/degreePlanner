package com.PlanInk.mvc;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class LoginController{

    @GetMapping("/")
    public String home() {
        return "login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String userType,
            HttpSession session) throws ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection(userType)
                .whereEqualTo("email", username)
                .whereEqualTo("password", password) // replace with hashed compare later
                .get();

        if (!future.get().isEmpty()) {
            // store minimal info in session
            session.setAttribute("currentUser", username);
            session.setAttribute("role", userType.toUpperCase());
            return "redirect:/" + userType.replace("ts", "t").replace("rs", "r");
        } else {
            return "redirect:/login?error=invalidCredentials";
        }
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Invalidate the current user session
        session.invalidate();

        // Redirect to login page
        return "redirect:/login";
    }


}
package com.PlanInk.mvc;

import com.PlanInk.mvc.auth.FirebaseAuthRestService;
import com.PlanInk.mvc.auth.FirebaseLoginException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Controller
public class LoginController{

    private final FirebaseAuthRestService firebaseAuthRestService;
    private final Firestore firestore;

    public LoginController(FirebaseAuthRestService firebaseAuthRestService, Firestore firestore) {
        this.firebaseAuthRestService = firebaseAuthRestService;
        this.firestore = firestore;
    }

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
            HttpSession session) {

        String normalizedCollection = normalizeCollection(userType);
        if (normalizedCollection == null) {
            return "redirect:/login?error=invalidRole";
        }

        try {
            var signInResponse = firebaseAuthRestService
                    .signInWithEmailAndPassword(username, password);
            String uid = signInResponse.getLocalId();

            ApiFuture<DocumentSnapshot> future =
                    firestore.collection(normalizedCollection).document(uid).get();
            DocumentSnapshot document = future.get(10, TimeUnit.SECONDS);

            if (!document.exists()) {
                return "redirect:/login?error=profileMissing";
            }

            session.setAttribute("currentUser", username);
            session.setAttribute("role", normalizedCollection.toUpperCase());
            session.setAttribute("uid", uid);

            return "redirect:/" + resolveRedirect(normalizedCollection);

        } catch (FirebaseLoginException e) {
            String suffix = e.isInvalidCredentials() ? "invalidCredentials" : "authError";
            return "redirect:/login?error=" + suffix;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "redirect:/login?error=authError";
        } catch (ExecutionException | TimeoutException e) {
            return "redirect:/login?error=authError";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Invalidate the current user session
        session.invalidate();

        // Redirect to login page
        return "redirect:/login";
    }

    private String normalizeCollection(String userType) {
        if (userType == null) {
            return null;
        }
        return switch (userType.toLowerCase()) {
            case "students" -> "students";
            case "faculty" -> "faculty";
            case "advisors" -> "advisors";
            default -> null;
        };
    }

    private String resolveRedirect(String collection) {
        return switch (collection) {
            case "students" -> "student";
            case "faculty" -> "faculty";
            case "advisors" -> "advisor";
            default -> "login";
        };
    }

}
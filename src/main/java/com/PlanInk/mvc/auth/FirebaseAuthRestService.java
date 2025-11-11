package com.PlanInk.mvc.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
@Service
public class FirebaseAuthRestService {

    private static final String SIGN_IN_ENDPOINT =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=%s";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public FirebaseAuthRestService() {
        this.restTemplate = new RestTemplate();
        String key = System.getenv("FIREBASE_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Missing Firebase Web API key. Set FIREBASE_API_KEY environment variable.");
        }
        this.apiKey = key;
    }

    public SignInResponse signInWithEmailAndPassword(String email, String password)
            throws FirebaseLoginException {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password must be provided.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("returnSecureToken", true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        String url = String.format(SIGN_IN_ENDPOINT, apiKey);

        try {
            ResponseEntity<SignInResponse> response =
                    restTemplate.postForEntity(url, request, SignInResponse.class);

            SignInResponse body = response.getBody();
            if (body == null || body.getLocalId() == null || body.getLocalId().isBlank()) {
                throw new FirebaseLoginException("Firebase response missing localId.", true);
            }
            return body;

        } catch (HttpClientErrorException e) {
            throw new FirebaseLoginException(resolveErrorMessage(e), true, e);
        } catch (RestClientException e) {
            throw new FirebaseLoginException("Failed to reach Firebase Authentication service.", e);
        }
    }

    private String resolveErrorMessage(HttpClientErrorException e) {
        String statusText = e.getStatusText();
        if (statusText != null && !statusText.isBlank()) {
            return "Firebase rejected credentials: " + statusText;
        }
        return "Firebase rejected credentials.";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignInResponse {
        private String idToken;
        private String refreshToken;
        private String localId;

        public String getIdToken() {
            return idToken;
        }

        public void setIdToken(String idToken) {
            this.idToken = idToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getLocalId() {
            return localId;
        }

        public void setLocalId(String localId) {
            this.localId = localId;
        }
    }
}


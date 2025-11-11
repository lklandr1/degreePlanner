package com.PlanInk.mvc.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase already initialized, reusing existing app");
                return FirebaseApp.getInstance();
            }

            GoogleCredentials creds;
            String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");

            if (credPath != null && Files.exists(Path.of(credPath))) {
                try (InputStream is = new FileInputStream(credPath)) {
                    creds = GoogleCredentials.fromStream(is);
                }
                log.info("Loaded Firebase credentials from env var: {}", credPath);
            } else {
                try (InputStream is = getClass().getClassLoader()
                        .getResourceAsStream("firebase-service-account.json")) {
                    if (is == null) {
                        throw new IllegalStateException(
                                "Missing firebase-service-account.json and GOOGLE_APPLICATION_CREDENTIALS not set"
                        );
                    }
                    creds = GoogleCredentials.fromStream(is);
                }
                log.info("Loaded Firebase credentials from classpath resource");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(creds)
                    .setDatabaseUrl("https://degreeplanner-2e548-default-rtdb.firebaseio.com")
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("✅ Firebase initialized successfully");
            return app;

        } catch (Exception e) {
            log.error("❌ Failed to initialize Firebase", e);
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    // 🔹 Add this: exposes FirebaseAuth as a Spring Bean
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    // 🔹 Optional: expose Firestore if you plan to use it
    @Bean
    public Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app);
    }
}

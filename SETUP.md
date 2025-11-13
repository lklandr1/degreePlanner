# Setup Instructions

## Firebase API Key Configuration

To avoid setting environment variables manually, you can use a local properties file.

### Option 1: Using application-local.properties (Recommended)

1. Copy the example file:
   ```bash
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```

2. Edit `src/main/resources/application-local.properties` and replace `YOUR_FIREBASE_WEB_API_KEY_HERE` with your actual Firebase Web API Key.

3. The file is automatically loaded by Spring Boot via LocalPropertiesConfig (no profile setup needed).

4. **Important**: This file is git-ignored, so your API key won't be committed to the repository.

### Option 2: Using Environment Variable (Fallback)

If you prefer using environment variables, you can still set:
```powershell
$Env:FIREBASE_API_KEY = "your-api-key-here"
```

### Option 3: Using Spring Profiles

1. Create `src/main/resources/application-local.properties` with your API key.

2. Set the profile in `application.properties`:
   ```properties
   spring.profiles.active=local
   ```

   Or set it when running:
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=local
   ```

## Getting Your Firebase Web API Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings (gear icon)
4. Scroll down to "Your apps" section
5. Copy the "Web API Key" value

## Priority Order

The application checks for the Firebase API key in this order:
1. `firebase.api-key` in `application-local.properties`
2. `FIREBASE_API_KEY` environment variable
3. If neither is found, the application will fail to start with a clear error message


# Deployment Guide

## Building the Application

Before deploying, build the executable JAR file:

```bash
mvn clean package
```

This will create a JAR file in the `target/` directory named `degreePlanner-0.0.1-SNAPSHOT.jar`.

## Files to Deploy

When moving files to your server via SSH, you need to copy:

1. **JAR File**: `target/degreePlanner-0.0.1-SNAPSHOT.jar`
   - This is the executable application

2. **Firebase Service Account JSON**: `src/main/resources/firebase-service-account.json`
   - Required for Firebase authentication
   - **IMPORTANT**: This file contains sensitive credentials and should be kept secure

3. **Firebase API Key** (choose one):
   - Option A: Create `application-local.properties` on the server with:
     ```
     firebase.api-key=YOUR_FIREBASE_WEB_API_KEY_HERE
     ```
   - Option B: Set environment variable `FIREBASE_API_KEY` on the server

## Server Requirements

- Java 17 or higher
- Port 3005 available (or change `server.port` in `application.properties`)

## Running on the Server

### Option 1: Direct Java Execution

```bash
java -jar degreePlanner-0.0.1-SNAPSHOT.jar
```

### Option 2: With Firebase Service Account from Environment Variable

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-service-account.json
java -jar degreePlanner-0.0.1-SNAPSHOT.jar
```

### Option 3: With Firebase API Key from Environment Variable

```bash
export FIREBASE_API_KEY=your-api-key-here
java -jar degreePlanner-0.0.1-SNAPSHOT.jar
```

### Option 4: Run in Background (nohup)

```bash
nohup java -jar degreePlanner-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

## Verifying Deployment

Once the application is running, you can verify it's working by:

1. Checking the logs for startup messages
2. Accessing the health endpoint: `http://your-server:3005/api/health`
3. Accessing the application: `http://your-server:3005/`

## Port Configuration

The application is configured to run on port **3005** by default (set in `application.properties`).

To change the port, either:
- Edit `server.port` in `application.properties` before building
- Override at runtime: `java -jar degreePlanner-0.0.1-SNAPSHOT.jar --server.port=8080`
- Set environment variable: `SERVER_PORT=8080`

## Troubleshooting

### Common Issues

- **Port already in use**: Change the port or stop the process using port 3005
- **Firebase errors**: Ensure `firebase-service-account.json` is accessible and `FIREBASE_API_KEY` is set
- **Java version**: Ensure Java 17+ is installed (`java -version`)

### SSL/HTTPS Error: "Invalid character found in method name"

If you see this error:
```
java.lang.IllegalArgumentException: Invalid character found in method name [0x160x030x01...]
```

**This means something is trying to connect via HTTPS to an HTTP-only port.**

**Solutions:**

1. **Access via HTTP (not HTTPS)**: 
   - Use `http://your-server:3005` (not `https://`)
   - The application runs on HTTP by default

2. **If using a reverse proxy (nginx/Apache)**:
   
   **Common Issue**: The proxy is forwarding HTTPS to HTTPS instead of HTTP.
   
   **Check your reverse proxy configuration:**
   
   **For nginx** - Make sure you're using `http://` (not `https://`) in proxy_pass:
   ```nginx
   server {
       listen 443 ssl;
       server_name your-domain.com;
       
       # SSL certificate configuration here...
       
       location / {
           # ✅ CORRECT: Forward HTTP to backend
           proxy_pass http://localhost:3005;
           
           # ❌ WRONG: This would cause the error
           # proxy_pass https://localhost:3005;
           
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           proxy_set_header X-Forwarded-Host $host;
           proxy_set_header X-Forwarded-Port $server_port;
       }
   }
   ```
   
   **For Apache** - Make sure ProxyPass uses `http://`:
   ```apache
   <VirtualHost *:443>
       ServerName your-domain.com
       SSLEngine on
       # SSL certificate configuration...
       
       # ✅ CORRECT: Forward HTTP to backend
       ProxyPass / http://localhost:3005/
       ProxyPassReverse / http://localhost:3005/
       
       # ❌ WRONG: This would cause the error
       # ProxyPass / https://localhost:3005/
       
       ProxyPreserveHost On
       RequestHeader set X-Forwarded-Proto "https"
   </VirtualHost>
   ```
   
   **Key Points:**
   - The reverse proxy should **terminate SSL** (handle HTTPS)
   - The proxy should forward **HTTP** (not HTTPS) to `localhost:3005`
   - Use `http://localhost:3005` in proxy_pass, never `https://`
   - After changing proxy config, restart the proxy service

3. **If you need HTTPS directly on the application**:
   - You'll need to configure SSL certificates in Spring Boot
   - Add to `application.properties`:
     ```properties
     server.ssl.key-store=classpath:keystore.p12
     server.ssl.key-store-password=your-password
     server.ssl.key-store-type=PKCS12
     server.ssl.key-alias=tomcat
     ```

4. **Health checks or monitoring tools**:
   - Ensure any automated tools are using HTTP, not HTTPS


# Reverse Proxy Configuration Checklist

If you're getting the SSL error "Invalid character found in method name" with a reverse proxy, check these items:

## Critical Configuration Issues

### ✅ CORRECT Configuration

The reverse proxy should:
1. **Listen on HTTPS** (port 443) for incoming connections
2. **Terminate SSL** (handle the HTTPS encryption)
3. **Forward HTTP** (not HTTPS) to the backend application on port 3005

### ❌ Common Mistakes

1. **Using `https://` in proxy_pass**:
   ```nginx
   # ❌ WRONG - This causes the SSL error
   proxy_pass https://localhost:3005;
   
   # ✅ CORRECT - Forward HTTP to backend
   proxy_pass http://localhost:3005;
   ```

2. **Not terminating SSL properly** - The proxy must handle SSL, not forward it

3. **Wrong port** - Make sure the proxy forwards to port 3005 (or whatever port the app uses)

## nginx Configuration Example

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    # SSL Certificate Configuration
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    # ✅ Forward HTTP (not HTTPS) to backend
    location / {
        proxy_pass http://localhost:3005;
        
        # Important headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        
        # WebSocket support (if needed)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## Apache Configuration Example

```apache
<VirtualHost *:443>
    ServerName your-domain.com
    
    SSLEngine on
    SSLCertificateFile /path/to/cert.pem
    SSLCertificateKeyFile /path/to/key.pem
    
    # ✅ Forward HTTP (not HTTPS) to backend
    ProxyPass / http://localhost:3005/
    ProxyPassReverse / http://localhost:3005/
    
    ProxyPreserveHost On
    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-Host "your-domain.com"
</VirtualHost>
```

## Verification Steps

1. **Check the proxy configuration file**:
   - Look for `proxy_pass` (nginx) or `ProxyPass` (Apache)
   - Verify it uses `http://localhost:3005` (not `https://`)

2. **Test direct connection** (bypass proxy):
   ```bash
   curl http://localhost:3005/api/health
   ```
   This should work if the app is running.

3. **Check proxy logs**:
   - nginx: `/var/log/nginx/error.log`
   - Apache: `/var/log/apache2/error.log`

4. **Restart the proxy** after making changes:
   ```bash
   # nginx
   sudo systemctl restart nginx
   
   # Apache
   sudo systemctl restart apache2
   ```

## What the Error Means

The error `Invalid character found in method name [0x160x030x01...]` means:
- The backend application (Spring Boot) is receiving an HTTPS/SSL handshake
- But it's configured to only accept HTTP
- This happens when the proxy forwards HTTPS instead of HTTP

## Quick Fix

If you have access to the proxy configuration, change:
- `proxy_pass https://localhost:3005;` → `proxy_pass http://localhost:3005;`
- `ProxyPass / https://localhost:3005/` → `ProxyPass / http://localhost:3005/`

Then restart the proxy service.


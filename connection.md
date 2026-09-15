# The Dedox Ngrok Connection Guide

This guide explains how to start The Dedox backend and connect the Android app and web app through ngrok.

## 1. Start the Backend

Open a terminal:

```bash
cd /home/Frutus/Projects/legit/legit-backend
```

Start MongoDB first:

```bash
docker compose up -d
```

Then start the Ktor backend:

```bash
./gradlew run
```

By default, the backend runs at:

```text
http://localhost:8080
```

Check that it is working:

```bash
curl http://localhost:8080/api/v1/gateway/ping
```

Expected response:

```json
{
  "success": true,
  "message": "pong"
}
```

## 2. Start Ngrok

Open another terminal and expose the backend:

```bash
ngrok http 8080
```

Ngrok will show a forwarding URL like:

```text
https://abcd-1234.ngrok-free.app
```

Use the HTTPS ngrok URL everywhere outside the backend terminal.

## 3. Connect the Android App

Update this file:

```text
/home/Frutus/Projects/legit/Legit-App/app/src/main/java/com/example/vaultkey/data/EnvironmentConfig.kt
```

Set `baseUrl` to your current ngrok URL:

```kotlin
object EnvironmentConfig {
    var baseUrl: String = "https://abcd-1234.ngrok-free.app"
}
```

Replace `https://abcd-1234.ngrok-free.app` with the actual URL printed by ngrok.

Important: free ngrok URLs usually change every time you restart ngrok. When the URL changes, update `EnvironmentConfig.kt` and rebuild/reinstall the Android app.

The Android app sends requests through:

```text
Legit-App/app/src/main/java/com/example/vaultkey/data/LegitApi.kt
```

## 4. Connect the Web App

Open a terminal:

```bash
cd /home/Frutus/Projects/legit/legit-web-new
```

Create or update `.env.local`:

```bash
NEXT_PUBLIC_API_URL=https://abcd-1234.ngrok-free.app
API_URL=https://abcd-1234.ngrok-free.app
```

Replace the URL with your actual ngrok forwarding URL.

Then start the web app:

```bash
npm run dev
```

The web frontend usually runs at:

```text
http://localhost:3000
```

If the web dev server was already running, restart it after changing `.env.local`.

The web app uses this API helper:

```text
legit-web-new/src/lib/api.ts
```

It reads:

```ts
const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
```

The proxy route also forwards API calls to the backend from:

```text
legit-web-new/src/app/api/proxy/[...path]/route.ts
```

## 5. Common URLs

Backend local URL:

```text
http://localhost:8080
```

Ngrok public backend URL:

```text
https://abcd-1234.ngrok-free.app
```

Android app backend URL:

```text
https://abcd-1234.ngrok-free.app
```

Web app backend URL:

```text
https://abcd-1234.ngrok-free.app
```

Local web frontend:

```text
http://localhost:3000
```

Backend ping through ngrok:

```text
https://abcd-1234.ngrok-free.app/api/v1/gateway/ping
```

## 6. Quick Ngrok Flow

1. Start MongoDB:

```bash
cd /home/Frutus/Projects/legit/legit-backend
docker compose up -d
```

2. Start backend:

```bash
./gradlew run
```

3. Start ngrok:

```bash
ngrok http 8080
```

4. Copy the HTTPS forwarding URL from ngrok.

5. Put that URL in Android:

```text
Legit-App/app/src/main/java/com/example/vaultkey/data/EnvironmentConfig.kt
```

6. Put that URL in web:

```text
legit-web-new/.env.local
```

7. Test ngrok:

```bash
curl https://abcd-1234.ngrok-free.app/api/v1/gateway/ping
```

Expected:

```json
{
  "success": true,
  "message": "pong"
}
```

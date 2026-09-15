# The Dedox

The Dedox is a secure document verification platform built around one core idea: documents stay inside the vault, while service providers receive only verification results.

Instead of sending Aadhaar, PAN, certificates, or other documents to every company that asks for them, The Dedox lets a requester create a verification contract. The user reviews what is being requested, approves or rejects it, and the backend performs verification internally. The requester receives a PASS, FAIL, or PARTIAL result with proof metadata, not the original document data.

## Project Structure

```text
legit/
├── Legit-App/          # Android app built with Kotlin, Jetpack Compose, Firebase, CameraX, and TFLite
├── legit-backend/      # Ktor backend with MongoDB, JWT auth, encrypted document storage, and verification pipeline
├── legit-web-new/      # Next.js web frontend for landing, auth, dashboard, KYC, and demo flows
├── connection.md       # Local/ngrok connection guide for backend, Android, and web
└── readme.md           # Top-level project overview
```

## What The System Does

The Dedox has three main surfaces:

- Android app: the user-facing mobile vault for signup, login, document screens, profile, camera/scanning flows, AI/threat detection, and verification approvals.
- Backend API: the main source of truth for users, sessions, encrypted documents, verification contracts, disposable keys, proof generation, and gateway health endpoints.
- Web app: a Next.js frontend for the public experience, login, dashboard, KYC flow, demos, and service-provider-style interactions.

The backend exposes the API used by both clients. For full endpoint details, see [legit-backend/API.md](legit-backend/API.md).

## Core Verification Flow

```text
1. Service provider creates a verification contract.
2. User sees what is requested and why.
3. User approves or rejects the request.
4. Approval creates a short-lived disposable key.
5. Service provider executes verification with that key.
6. Backend checks documents internally.
7. Backend returns verification status and proof.
8. The key is burned and cannot be reused.
```

The important privacy boundary is that the requester does not receive the user's raw document files or full private document fields.

## Backend

Location:

```bash
cd legit-backend
```

Main technologies:

- Kotlin
- Ktor
- MongoDB
- JWT authentication
- AES-256-GCM document encryption
- Firebase Admin SDK for push notification support
- Gradle wrapper

Important backend files:

- `src/main/kotlin/com/sonusid/legit/Application.kt` wires the app, services, database, CORS, gateway, and cleanup job.
- `src/main/resources/application.yaml` contains local port, JWT, MongoDB, and Legit secret configuration.
- `src/main/kotlin/com/sonusid/legit/routes/` contains auth, document, and pipeline routes.
- `src/main/kotlin/com/sonusid/legit/services/` contains user, document, and Firebase services.
- `src/main/kotlin/com/sonusid/legit/pipeline/DataPipelineService.kt` contains the verification contract pipeline.

Start MongoDB:

```bash
docker compose up -d
```

Run the backend:

```bash
./gradlew run
```

The backend starts on:

```text
http://localhost:8080
```

Health check:

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

Useful backend docs:

- [legit-backend/README.md](legit-backend/README.md)
- [legit-backend/API.md](legit-backend/API.md)

## Android App

Location:

```bash
cd Legit-App
```

Main technologies:

- Kotlin
- Android Jetpack Compose
- Material 3
- Navigation Compose
- Ktor Android client
- Firebase Messaging
- CameraX and ML Kit barcode scanning
- TensorFlow Lite threat detection model

Important Android files:

- `app/src/main/java/com/example/vaultkey/MainActivity.kt` starts the app.
- `app/src/main/java/com/example/vaultkey/navigations/Navigator.kt` defines navigation.
- `app/src/main/java/com/example/vaultkey/screens/` contains the main app screens.
- `app/src/main/java/com/example/vaultkey/components/` contains reusable Compose UI pieces.
- `app/src/main/java/com/example/vaultkey/data/LegitApi.kt` contains backend API calls.
- `app/src/main/java/com/example/vaultkey/data/EnvironmentConfig.kt` controls the backend base URL.
- `app/src/main/assets/shieldnet_model.tflite` is the bundled TFLite model.

Run tests/build from the Android project:

```bash
./gradlew test
./gradlew assembleDebug
```

For an Android emulator talking to a local backend, debug builds are configured for:

```text
http://10.0.2.2:8080
```

If you are using a physical device or ngrok, update:

```text
Legit-App/app/src/main/java/com/example/vaultkey/data/EnvironmentConfig.kt
```

More Android connection notes are in [Legit-App/BACKEND_INTEGRATION.md](Legit-App/BACKEND_INTEGRATION.md).

## Web App

Location:

```bash
cd legit-web-new
```

Main technologies:

- Next.js
- React
- TypeScript
- Tailwind CSS
- Three.js / React Three Fiber
- Framer Motion
- Lucide icons

Install dependencies:

```bash
npm install
```

Run the web app:

```bash
npm run dev
```

The web frontend normally starts on:

```text
http://localhost:3000
```

Useful scripts:

```bash
npm run dev
npm run build
npm run start
npm run lint
```

Important web files:

- `src/app/page.tsx` is the main landing page.
- `src/app/login/page.tsx` is the login flow.
- `src/app/dashboard/page.tsx` is the dashboard.
- `src/app/kyc/page.tsx` is the KYC flow.
- `src/app/demo/page.tsx` contains demo interactions.
- `src/app/api/proxy/[...path]/route.ts` proxies API requests.
- `src/lib/api.ts` contains the frontend API helper.

To point the web app at a backend URL, create or update:

```text
legit-web-new/.env.local
```

Example:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
API_URL=http://localhost:8080
```

Restart the web dev server after changing `.env.local`.

## Containerization (Docker Compose)

You can run the entire stack (MongoDB, Backend, and Website) with a single command from the project root:

```bash
docker compose up --build
```

This will:
1. Build the Ktor backend (using a multi-stage JDK 25 environment).
2. Build the Next.js website (using a multi-stage Node 20 standalone configuration).
3. Spin up MongoDB, Backend, and Website, mapping them to the following ports:
   - **Website**: [http://localhost:3000](http://localhost:3000)
   - **Backend API**: [http://localhost:8080](http://localhost:8080)
   - **MongoDB**: `localhost:27017`

To run in the background (detached mode):

```bash
docker compose up -d
```

To stop all services:

```bash
docker compose down
```

## Running Everything Locally (Without Docker)

From the repository root:

```bash
cd legit-backend
docker compose up -d
./gradlew run
```

In a second terminal:

```bash
cd legit-web-new
npm install
npm run dev
```

For Android, open `Legit-App` in Android Studio or build from the terminal:

```bash
cd Legit-App
./gradlew assembleDebug
```

## Using Ngrok

Use ngrok when the Android app or another external client needs to reach the local backend.

```bash
ngrok http 8080
```

Then copy the HTTPS forwarding URL into:

- Android: `Legit-App/app/src/main/java/com/example/vaultkey/data/EnvironmentConfig.kt`
- Web: `legit-web-new/.env.local`

Full ngrok setup is documented in [connection.md](connection.md).

## Main API Areas

The backend groups functionality under these route families:

- `/api/v1/auth` for register, login, refresh, and logout.
- `/api/v1/user` for profile, session, and password management.
- `/api/v1/documents` for encrypted document vault operations.
- `/api/v1/pipeline` for verification contracts, approvals, disposable-key verification, and results.
- `/api/v1/gateway` for ping, health, API metadata, endpoint discovery, and uptime.

Read [legit-backend/API.md](legit-backend/API.md) for request and response examples.

## Configuration Notes

Local backend configuration lives in:

```text
legit-backend/src/main/resources/application.yaml
```

The checked-in values are development defaults. Before production use, replace:

- JWT secret
- encryption secret
- pipeline secret
- MongoDB URI
- production backend URL
- Android release `LEGIT_BASE_URL`
- Firebase configuration and credentials

Do not ship production builds with local placeholder secrets or debug signing.

## Testing Checklist

Basic manual flow:

```text
1. Start MongoDB.
2. Start the Ktor backend.
3. Confirm /api/v1/gateway/ping returns pong.
4. Start the web app or Android app.
5. Register or log in.
6. Upload or view documents.
7. Create a verification contract.
8. Approve or reject the contract as the user.
9. Execute verification as the requester.
10. Confirm only verification results are returned.
```

Useful automated commands:

```bash
# Backend
cd legit-backend
./gradlew test

# Android
cd Legit-App
./gradlew test

# Web
cd legit-web-new
npm run lint
npm run build
```

## Current Docs

- [connection.md](connection.md): how to connect backend, Android, and web through ngrok.
- [legit-backend/README.md](legit-backend/README.md): backend architecture and quick start.
- [legit-backend/API.md](legit-backend/API.md): complete API reference.
- [Legit-App/BACKEND_INTEGRATION.md](Legit-App/BACKEND_INTEGRATION.md): Android backend integration notes.
- [legit-web-new/README.md](legit-web-new/README.md): default Next.js project notes.


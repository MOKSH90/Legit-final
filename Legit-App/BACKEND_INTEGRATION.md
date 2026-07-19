## Backend integration & test guide

- **Base URLs**
  - Debug builds use `http://10.0.2.2:8080` (Android emulator → local Ktor backend).
  - Release builds are configured for `https://api.legit.example.com` – update this to your real production URL if different.

- **Auth flow**
  - Signup: `POST /api/v1/auth/register`
  - Login: `POST /api/v1/auth/login`
  - Protected calls send `Authorization: Bearer <token>` from the stored session.

- **Core test flow (manual)**
  1. Start the Ktor backend on `localhost:8080`.
  2. Run a **debug build** of the app in the emulator.
  3. Signup a new user, then log in.
  4. Open **Profile**, confirm data loads, and update full name/phone.
  5. Open **Documents**, upload at least one document, and verify it appears in the list.
  6. Trigger a pending contract on the backend (for this user), then:
     - Open **Verification** screen and/or **Verification bottom sheet** on Home.
     - Approve and reject contracts and confirm status changes server-side.

- **Logs**
  - `BuildConfig.ENABLE_HTTP_LOGS` controls Ktor client logging:
    - `true` in debug for full request/response logs.
    - `false` in release to avoid noisy logs.


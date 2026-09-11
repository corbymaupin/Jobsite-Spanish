# Jobsite Spanish — native Android (Kotlin + Jetpack Compose)

**Package id:** `com.corbymaupin.jobsitespanish`  
**Version:** `1.0.0` (`versionCode` 1)  
**No WebView / Capacitor / Cordova / TWA** — pure Compose UI.

## Monetization

- **Paid app at $1.99** (one-time) — set in **Google Play Console**, not in this project.
- **Do not** add the Play Billing Library for unlocks or IAP.
- Classmates get free copies via **Play Console paid-app promo codes** (up to ~500/quarter). Corby/Ada distribute codes outside the app.

## Feedback

Bottom nav → **Feedback**: category (Bug / Idea / Praise), message, optional email → opens a prefilled mail to `james.corby.maupin@gmail.com` with subject `[Jobsite Spanish Feedback]` (includes app version + Android SDK). Also **Copy feedback text**. Ada reviews that Gmail inbox. Nothing is transmitted until the user sends the email.

## Open in Android Studio

1. **File → Open** → select this `android/` folder (not the repo root).
2. Let Gradle Sync finish (SDK 35 / JDK 17+ recommended).
3. Run on an emulator or device (**minSdk 26**).

Android Studio writes `local.properties` with your `sdk.dir` (gitignored).

## Command-line builds

### Debug APK

```bash
cd android
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

### Release AAB (Play upload)

Corby creates an upload keystore (**do not invent or commit passwords**):

```bash
keytool -genkey -v -keystore jobsite-spanish-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias jobsite-spanish
```

Then either:

- Android Studio → **Build → Generate Signed Bundle / APK**, or
- Put paths/passwords in a local `keystore.properties` (gitignored) and configure `signingConfigs` if you prefer CLI.

```bash
cd android
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

Without Corby’s keystore, use the Studio wizard or treat CLI output as a local smoke artifact only — Play needs a real upload signature. Full Play steps: repo root `PLAY_RELEASE.md`.

## What’s included

- `app/src/main/assets/terms.json` — 387 cards (`id` = web `cardKey` = `en|es`)
- Study / Listen / Browse / Stats / **Feedback**
- Leitner SRS + session rotation (~30/70 new/review, graduate after 2 correct)
- DataStore progress (on-device) + Android `TextToSpeech`
- Adaptive launcher icons generated from repo `icons/icon-512.png`
- No `INTERNET` permission (offline study)

Web PWA files at the repo root remain as reference/demo only.

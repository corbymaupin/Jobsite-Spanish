# Jobsite Spanish — Google Play release guide (native Android)

**Repo today:** https://github.com/corbymaupin/Jobsite-Spanish  
**Live web demo (reference only):** https://corbymaupin.github.io/Jobsite-Spanish/

## Product decisions (Corby / Ada)

| Decision | Detail |
|---|---|
| Stack | **Native Kotlin + Jetpack Compose only** (`android/`). No WebView / TWA / Capacitor / Cordova. |
| Package | `com.corbymaupin.jobsitespanish` |
| Monetization | **Paid app · $3.99** one-time (set in Play Console, **not** in code). **Do not** add Play Billing Library for this. |
| Classmates | Free via **Play Console paid-app promo codes** (up to **500 / quarter**). Ada/Corby distribute codes; no in-app unlock. |
| Feedback | In-app **Feedback** tab → prefilled email to `james.corby.maupin@gmail.com` (subject `[Jobsite Spanish Feedback]`) + copy-text button. **Ada absorbs feedback from that Gmail inbox.** |
| Version (first release) | `versionName 1.0.0` / `versionCode 1` |
| Network | Offline-only study. **No `INTERNET` permission** (fine for Play). Feedback leaves the device only when the user sends email. |

---

## What the repo is today

Native Play code lives in **`android/`** — Kotlin + Jetpack Compose. Open that folder in Android Studio (Sync → Run). See `android/README.md`.

The repo root remains the **web PWA** (`index.html`, `manifest.json`, `sw.js`, icons) as behavior/content reference and demo. **TWA / Capacitor / Cordova / WebView wraps stay retired.**

Layout:

```
android/
  app/src/main/java/com/corbymaupin/jobsitespanish/
    ui/   Study, Listen, Browse, Stats, Feedback
    data/ TermsRepository, ProgressStore (DataStore)
    srs/  Leitner + session builder
    tts/  Speech (TextToSpeech)
  app/src/main/assets/terms.json
```

---

## Build release AAB (`bundleRelease`)

### Debug (CI / smoke)

```bash
cd android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Signed release AAB (what Play wants)

1. **Corby creates a keystore** (once). Do **not** invent or commit passwords.

```bash
keytool -genkey -v -keystore jobsite-spanish-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias jobsite-spanish
```

Store the `.jks` and passwords in a password manager — **never commit** them to git.

2. Create `android/keystore.properties` (gitignored) locally:

```properties
storeFile=/absolute/path/to/jobsite-spanish-upload.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=jobsite-spanish
keyPassword=YOUR_KEY_PASSWORD
```

3. Wire signing in Android Studio (**Build → Generate Signed Bundle / APK**) **or** add a `signingConfigs` block in `app/build.gradle.kts` that reads `keystore.properties` (optional; Studio UI is enough for v1).

4. Build:

```bash
cd android
./gradlew bundleRelease
# AAB: app/build/outputs/bundle/release/app-release.aab
```

Without a keystore, `bundleRelease` may produce an **unsigned** or debug-signed artifact depending on config — **Play requires a properly signed upload key** (or Play App Signing enrollment with an upload key Corby controls).

5. Upload `app-release.aab` to Play Console → **Internal testing** first, then production.

### Unsigned / debug-signing note

If you only need a local AAB smoke check and have no keystore yet:

```bash
cd android
./gradlew bundleRelease
```

Inspect `app/build/outputs/bundle/release/`. For Play upload, Corby must still create the upload keystore and sign (or use Android Studio’s signed-bundle wizard).

---

## Play Console checklist

1. Google Play developer account ($25) — **Corby**
2. Create app → package `com.corbymaupin.jobsitespanish`
3. **Pricing:** Paid · **$3.99** USD (and other countries as needed)
4. **Promo codes:** Generate paid-app promo codes for classmates (quota ~500/quarter)
5. Upload signed AAB; enable **Play App Signing**
6. Listing copy — `store-assets/LISTING.md` (developer name: **Applied Solutions Lab**)
7. Icon / feature graphic / **native** screenshots (re-shoot from Compose UI)
8. Data safety: no data collected / on-device only; optional feedback email only when user sends
9. Privacy policy URL: https://corbymaupin.github.io/Jobsite-Spanish/privacy.html
10. Content rating questionnaire

---

## Feature parity checklist (web → native)

- [x] 387 cards / 13 trades from assets
- [x] Study / Listen / Browse / Stats
- [x] Leitner SRS + session rotation (scaffold)
- [x] Offline-only runtime (no INTERNET)
- [x] Feedback screen (email + copy)
- [x] Adaptive icons from `icons/icon-512.png`
- [x] Privacy wording for on-device app storage
- [ ] Bilingual study-home toggle (optional parity)
- [ ] Final native store screenshots

---

## Feedback ops (Ada)

Users tap **Feedback** → choose Bug / Idea / Praise → write a message (optional reply email) → **Email feedback** opens a mail client to `james.corby.maupin@gmail.com` with subject `[Jobsite Spanish Feedback]` and body including app version + Android SDK. **Copy feedback text** is available if no mail app. Ada triages that Gmail inbox; no backend or Play Billing required.

---

## Retired (do not use for Play)

- PWABuilder / Bubblewrap / TWA
- Capacitor / Cordova / PhoneGap / any WebView shell
- Play Billing Library (paid app is Console-priced; classmates use promo codes)
- Committing keystores or passwords

---

## Out of scope (this release)

- Law-enforcement Spanish app (separate worker)
- Actually uploading to Play Console (Corby)
- Piddly UI polish

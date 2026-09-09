# Jobsite Spanish — Google Play release guide (native Android)

**Repo today:** https://github.com/corbymaupin/Jobsite-Spanish  
**Live web demo (reference only):** https://corbymaupin.github.io/Jobsite-Spanish/

## What the repo is today

Native Play code lives in **`android/`** — Kotlin + Jetpack Compose, package id `com.corbymaupin.jobsitespanish`. Open that folder in Android Studio (Sync → Run). See `android/README.md`.

The repo root remains the **web PWA** (`index.html`, `manifest.json`, `sw.js`, icons) as behavior/content reference and demo. **TWA / Capacitor / Cordova / WebView wraps stay retired** — Play ships from the native Compose app only.


---

## Recommended native approach (least rewrite, clearly native)

**Primary recommendation: Kotlin + Jetpack Compose**

Why this over other options:

| Option | Verdict |
|---|---|
| **Kotlin + Jetpack Compose** | **Chosen.** Real Android UI toolkit, Play-native tooling, on-device `TextToSpeech`, Room/DataStore for progress. Unambiguous “native,” no WebView. |
| Flutter (Dart) | Fine backup if we want iOS later from one codebase. Still a full UI rewrite; not less work for Android-only. |
| React Native | Native views, but JS-centric; easier to confuse with “web stack.” Skip unless Corby prefers JS. |
| TWA / Capacitor / Cordova | **Disallowed** for this Play release. |

The web app’s hard parts are **data + rules**, not a huge UI surface. Those port cleanly:

1. **Export `SEED_TERMS`** (387 cards × trade/region/en/es) → `assets/terms.json` (or Kotlin `terms` module).
2. **Port Leitner SRS + session rotation** (boxes, due dates, 30/70 new/review flow, streak) → domain layer in Kotlin.
3. **Port screens** — Study, Listen, Browse, Stats — in Compose (same IA as the web app).
4. **Replace `speechSynthesis`** → Android `TextToSpeech` (Spanish voice).
5. **Replace `localStorage`** → DataStore or Room (progress + streak only; terms stay in assets).
6. **Reuse store art** already in `store-assets/` and `icons/` (regenerate adaptive icon mipmaps from `icon-512.png`).

Layout (scaffolded under `android/`):

```
android/
  app/src/main/java/com/corbymaupin/jobsitespanish/
    ui/   Study, Listen, Browse, Stats
    data/ TermsRepository, ProgressStore
    srs/  Leitner + session builder
    tts/  Speech (TextToSpeech)
  app/src/main/assets/terms.json
```

**Application id:** `com.corbymaupin.jobsitespanish`  
**First version:** `versionName 1.0.0` / `versionCode 1`  
**Min SDK:** 26 (Android 8) is enough for TTS + DataStore; target / compile SDK = current Play requirement (PWABuilder no longer applies — use Android Studio defaults for 2026 Play target SDK).

---

## Feature parity checklist (from web → native)

- [x] 387 cards / 13 trades loaded from assets
- [x] Study: flip card, correct/incorrect, Leitner scheduling (scaffold)
- [x] Session new-card rotation (~30/70 vs reviews; graduate after 2 correct) (scaffold)
- [x] Category / trade practice mode (scaffold)
- [x] Listen mode (hands-free queue, does not move Leitner boxes) (basic)
- [x] Browse deck by trade
- [x] Stats: streak, totals, box gauge
- [x] Offline-only runtime (no network required after install)
- [ ] Bilingual study-home toggle (if keeping web parity)
- [ ] Privacy policy URL (can keep hosted `privacy.html`; update wording from “browser localStorage” → “on-device app storage”)

---

## Play Console (unchanged needs)

Still required from Corby:

1. Google Play developer account ($25)
2. Upload key / Play App Signing (Android Studio / Play Console — **not** PWABuilder)
3. Listing copy — draft remains in `store-assets/LISTING.md` (minor edits OK)
4. Icon, feature graphic, screenshots — existing assets are usable; re-capture screenshots from the **native** UI before submit
5. Data safety: no data collected / on-device only
6. Privacy policy URL (update text for native storage)

---

## What’s done vs what needs Corby

| Item | Status |
|---|---|
| Web reference app + vocabulary | Done (this repo) |
| Store listing draft | Done — `store-assets/LISTING.md` |
| Feature graphic + old screenshots | Done (re-shoot after native UI) |
| Privacy page (web wording) | Done — needs native wording tweak |
| Native Android project (Compose) | **Not started** |
| `terms.json` export from `SEED_TERMS` | **Not started** |
| Signed `.aab` from Android Studio | Needs native project + Corby keystore |
| Play developer account | **Corby** |
| Final store screenshots from native builds | After UI exists |
| Approve package id / display name | **Corby** (default above) |

---

## Retired (do not use for Play)

- PWABuilder / Bubblewrap / TWA
- Capacitor / Cordova / PhoneGap / any WebView shell around `index.html`
- `.well-known/assetlinks.json` for chrome-less TWA (only relevant to the retired path; template may remain but is not the Play plan)

---

## Next engineering steps

1. Scaffold Kotlin Compose app with package `com.corbymaupin.jobsitespanish`.
2. Extract `SEED_TERMS` → `terms.json` and load in-app.
3. Implement progress store + Leitner domain to match web behavior.
4. Build Study → Listen → Browse → Stats.
5. Wire Android TTS (es).
6. Debug/release signing, Play internal testing track, then production.

Out of scope until Jobsite Play-ready: law-enforcement Spanish native port (same engine + different `terms.json` later).

# Jobsite Spanish — Google Play release guide

**Repo:** https://github.com/corbymaupin/Jobsite-Spanish  
**Live PWA:** https://corbymaupin.github.io/Jobsite-Spanish/  
**Approach:** Trusted Web Activity (TWA) via [PWABuilder](https://www.pwabuilder.com/) — wrap the hosted PWA; no Kotlin rewrite.

Recommended Android application id: `com.corbymaupin.jobsitespanish`

---

## What’s already done in this repo

| Item | Status |
|---|---|
| Single-file offline app (`index.html`) | Done |
| PWA manifest (`manifest.json`) | Done — name Jobsite Spanish, standalone, portrait |
| Service worker (`sw.js`) | Done — precaches shell for installability |
| Icons 192 + 512 (`icons/`) | Done (512×512 + 192×192) |
| Feature graphic 1024×500 | Done — `store-assets/feature-graphic.png` |
| Phone screenshots (5) | Done — 860×1688 under `store-assets/screenshots/` |
| Privacy policy page | Done — `privacy.html` (on-device only, no network) |
| Privacy contact email | Done — set in `privacy.html` |
| GitHub Pages (HTTPS) | Done — deploys from `main` `/` |
| Store listing draft copy | Done — `store-assets/LISTING.md` |
| Digital Asset Links template | Done — `.well-known/assetlinks.json.TEMPLATE` |
| Real `assetlinks.json` with signing cert | **Blocked** — needs SHA-256 from signing key |
| Signed `.aab` | **Blocked** — needs PWABuilder + keystore |
| Play Console listing / submit | **Blocked** — needs $25 developer account |

---

## What Corby must do (human / account)

1. **Create Google Play developer account** ($25 one-time) after class when ready.
2. **Generate Android package in PWABuilder**
   - URL: `https://corbymaupin.github.io/Jobsite-Spanish/`
   - Package ID: `com.corbymaupin.jobsitespanish`
   - App name: Jobsite Spanish
   - Generate signing key (or upload your own) — **save the keystore + passwords offline**
   - Download the signed `.aab` (or signed APK for sideload test)
3. **Publish Digital Asset Links**
   - PWABuilder shows the `assetlinks.json` content (includes your cert SHA-256)
   - Copy that JSON to `.well-known/assetlinks.json` in this repo (replace the TEMPLATE)
   - Push to `main` so Pages serves:  
     `https://corbymaupin.github.io/Jobsite-Spanish/.well-known/assetlinks.json`
   - Verify with Google’s statement list tool before relying on chrome-less mode
4. **Play Console**
   - Create app → upload `.aab`
   - Paste listing from `store-assets/LISTING.md`
   - Upload icon (use `icons/icon-512.png`), feature graphic, screenshots
   - Privacy policy URL: `https://corbymaupin.github.io/Jobsite-Spanish/privacy.html`
   - Data safety: no data collected
   - Content rating questionnaire
   - Target API / Play policy: PWABuilder’s current Android package targets a Play-compliant SDK — accept defaults unless Console warns
5. **Smoke-test on a phone**
   - Install internal testing track first
   - Confirm full-screen (no browser URL bar) = asset links OK
   - Confirm Study / Listen / Browse / Stats + offline after first open

---

## Versioning

| Layer | Where | Notes |
|---|---|---|
| Web / cache | `sw.js` → `CACHE_NAME` | Bump when `index.html`, manifest, or icons change |
| Android | PWABuilder / `build.gradle` versionCode + versionName | Increment every Play upload |
| npm | `package.json` version | Dev/tests only; not what Play reads |

Suggested first Play upload: `versionName 1.0.0`, `versionCode 1`.

---

## Target SDK / policy basics (TWA)

- Ship whatever current PWABuilder Android package defaults to (stays Play-compliant).
- No ads, no analytics, no accounts → Data safety is simple (“no data collected”).
- Privacy policy required even for no-collection apps — already hosted.
- Declare that the app uses text-to-speech / microphone? TTS is on-device speech **output** only; no mic permission expected for this app.
- Export compliance / encryption: standard HTTPS for first load only; no custom crypto.

---

## Quick PWABuilder checklist

- [ ] Manifest validates (name, icons, `display: standalone`, `start_url`)
- [ ] Service worker controls start URL
- [ ] HTTPS GitHub Pages URL loads offline after first visit
- [ ] Package id set to `com.corbymaupin.jobsitespanish`
- [ ] Signing key stored safely (not in git)
- [ ] `assetlinks.json` live on Pages with correct SHA-256
- [ ] Internal test install is chrome-less

---

## Out of scope for this prep

- Law-enforcement Spanish packaging (separate follow-up; clone this checklist)
- Kotlin / Flutter rewrite
- Monetization / IAP / ads

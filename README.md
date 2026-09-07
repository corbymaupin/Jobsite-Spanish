# Jobsite Spanish

Construction-Spanish vocabulary trainer for English-speaking supervisors
running Texas crews. 387 cards across 13 trades, Leitner spaced repetition
with session-based new-word rotation, hands-free Listen mode, and offline
text-to-speech.

The whole app is **`index.html`**. No framework, no bundler, no npm, no build
step, no CDN. Open it from disk with the network off and it works. It is
deployed straight from the repo root by GitHub Pages.

`manifest.json`, `sw.js`, and `icons/` are the one deliberate exception —
static PWA-install plumbing with no logic of their own, needed because a
service worker can't be registered from a blob: URL and Android's app-store
packaging (Trusted Web Activity, see below) needs a real, fetchable
manifest. `privacy.html` and `store-assets/` support the Play Store listing
and aren't linked from the app itself.

## Publishing to Google Play

This ships as a [Trusted Web Activity](https://developer.chrome.com/docs/android/trusted-web-activity/)
via [PWABuilder](https://www.pwabuilder.com/) — no native code, no Android
Studio. Point PWABuilder at the deployed GitHub Pages URL, generate the
signed `.aab` under "Publish → Android," then add the `assetlinks.json`
content it gives you to `.well-known/assetlinks.json` in this repo (not
present yet — its content depends on the signing key generated during that
step) so the installed app opens full-screen instead of with a browser bar.
`store-assets/` holds the feature graphic and icon for the Play Console
listing; `privacy.html` is the required privacy policy URL, truthfully
short given this app makes no network calls and stores everything only in
`localStorage`.

## Running the tests

The test harness is the only thing that needs Node. It drives the real
`index.html` in a headless browser — it never reads the source and decides it
looks right.

```
npm install
node test/acceptance.js     # 50 assertions: data, scheduler, UI, Listen mode
node test/shots.js          # screenshots of every screen at 390x844
node test/smoke.js          # quick sanity check, no assertions
```

Screenshots land in `test/screenshots/` (gitignored). If Chromium isn't where
`test/acceptance.js` expects it, change the `CHROME` constant at the top.

## Where things are in index.html

| Section | What it holds |
|---|---|
| 1 | `SEED_TERMS` — the vocabulary, the only place card text exists |
| 2 | Dates, as `"YYYY-MM-DD"` strings and never `Date` objects |
| 3 | Card identity — why a card is not its array index |
| 4 | Leitner boxes and intervals (long-term mastery, tracked in the background) |
| 5 | The deck, the save file, and the v2 migration |
| 6 | Queries over the deck; streak |
| 7 | Speech and voice selection |
| 8 | Session-based new-word rotation — no daily quota; new cards flow into a session at roughly a 30/70 ratio against reviews and graduate out after 2 correct answers, replaced from the pool in real time |
| 9–13 | Session state, tab shell, Study, Browse, Stats |
| 14 | Listen mode, and the three browser bugs it works around |
| 15 | Wiring and boot — manifest link is in `<head>`; this section registers `sw.js` |

`window.jobsite` in the browser console exposes the deck, the save state and
the current session for poking at.

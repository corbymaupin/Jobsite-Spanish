// Screenshots at 390x844 (iPhone 14) of every screen, so layout bugs that
// are invisible in source have somewhere to show up.
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const CHROME = '/opt/pw-browsers/chromium-1194/chrome-linux/chrome';
const FILE = 'file://' + path.resolve(__dirname, '..', 'index.html');
const OUT = path.resolve(__dirname, 'screenshots');

// Headless Chromium ships no speech voices at all. Fake the two a real
// phone has so the screenshots show the normal state, not the "install a
// Spanish voice" warning.
const FAKE_VOICES = () => {
  const voices = [
    { name: 'Paulina', lang: 'es-MX', default: false, localService: true, voiceURI: 'Paulina' },
    { name: 'Samantha', lang: 'en-US', default: true, localService: true, voiceURI: 'Samantha' }
  ];
  const fake = {
    getVoices: () => voices,
    speak(u) { setTimeout(() => { if (u.onstart) u.onstart({}); if (u.onend) u.onend({}); }, 5); },
    cancel() {}, pause() {}, resume() {},
    speaking: false, paused: false, pending: false,
    addEventListener() {}, removeEventListener() {}
  };
  Object.defineProperty(window, 'speechSynthesis', { value: fake, configurable: true });
};

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const b = await chromium.launch({ executablePath: CHROME });
  const p = await b.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
  await p.addInitScript(FAKE_VOICES);

  // Day one: nothing introduced.
  await p.goto(FILE);
  await p.waitForTimeout(200);
  await p.screenshot({ path: OUT + '/01-study-day-one.png' });

  // Now seed a few weeks of realistic progress so the charts have shape.
  await p.evaluate(() => {
    const deck = window.jobsite.deck();
    const today = window.jobsite.todayKey();
    const spread = [1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5];
    const progress = {};
    deck.slice(0, 96).forEach((c, i) => {
      const box = spread[i % spread.length];
      progress[c.key] = {
        box, introduced: true,
        nextReview: window.jobsite.addDays(today, i % 7 === 0 ? 0 : (i % 5) + 1)
      };
    });
    localStorage.setItem(window.jobsite.STORAGE_KEY, JSON.stringify({
      v: 3, progress,
      streak: { last: today, days: 11 },
      listen: { order: 'es-en', gap: 3000, repeat: false, scope: 'learning' },
      session: null
    }));
  });
  await p.reload();
  await p.waitForTimeout(200);
  await p.screenshot({ path: OUT + '/02-study.png' });

  await p.click('[data-tab="listen"]');  await p.waitForTimeout(150);
  await p.screenshot({ path: OUT + '/03-listen.png', fullPage: true });

  await p.click('[data-tab="browse"]');  await p.waitForTimeout(150);
  await p.fill('#searchBox', 'esling');
  await p.waitForTimeout(120);
  await p.screenshot({ path: OUT + '/04-browse-search.png' });
  await p.fill('#searchBox', '');
  await p.waitForTimeout(120);
  await p.screenshot({ path: OUT + '/05-browse.png' });

  await p.click('[data-tab="stats"]');   await p.waitForTimeout(150);
  await p.screenshot({ path: OUT + '/06-stats.png', fullPage: true });

  // Mid-session, both faces of the card.
  await p.click('[data-tab="study"]');   await p.waitForTimeout(150);
  await p.click('#startBtn');            await p.waitForTimeout(200);
  await p.screenshot({ path: OUT + '/07-session-prompt.png' });
  await p.click('#flipBtn');             await p.waitForTimeout(350);
  await p.screenshot({ path: OUT + '/08-session-answer.png' });
  // A long phrase, to check the type ramps down instead of overflowing.
  await p.evaluate(() => {
    const d = window.jobsite.deck();
    const long = d.find(c => c.en === "There's lightning. Come down.");
    document.getElementById('promptText').textContent = long.en;
    document.getElementById('promptText').className = 'term is-long';
    document.getElementById('answerText').textContent = long.es;
    document.getElementById('answerText').className = 'term is-long';
    document.getElementById('tradeTag').textContent = long.trade;
    document.getElementById('regionalNote').textContent = 'Los relámpagos is the flash, los rayos is the strike';
    document.getElementById('regionalNote').hidden = false;
  });
  await p.waitForTimeout(120);
  await p.screenshot({ path: OUT + '/09-session-long-phrase.png' });

  await p.click('#gotItBtn'); await p.waitForTimeout(200);
  await p.evaluate(() => {
    let guard = 0;
    while (window.jobsite.session().currentKey && guard++ < 400) {
      document.getElementById('flipBtn').click();
      document.getElementById(guard % 5 === 0 ? 'missedBtn' : 'gotItBtn').click();
      const done = document.getElementById('studyDone');
      if (done && !done.hidden) break;
    }
  });
  await p.waitForTimeout(250);
  await p.screenshot({ path: OUT + '/10-session-done.png' });

  console.log('wrote', fs.readdirSync(OUT).join(', '));
  await b.close();
})();

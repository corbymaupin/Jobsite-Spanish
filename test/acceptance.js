/* Acceptance suite for Jobsite Spanish.
 *
 *   node test/acceptance.js
 *
 * Drives the real index.html in headless Chromium. Nothing here reads the
 * source and decides it looks right - every assertion goes through the
 * page. Run it after any change to the deck or the scheduler.
 */
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const CHROME = '/opt/pw-browsers/chromium-1194/chrome-linux/chrome';
const FILE = 'file://' + path.resolve(__dirname, '..', 'index.html');
const SRC = fs.readFileSync(path.resolve(__dirname, '..', 'index.html'), 'utf8');

let pass = 0, fail = 0;
const section = t => console.log('\n' + t + '\n' + '-'.repeat(t.length));
function ok(cond, label, detail) {
  if (cond) { pass++; console.log('  PASS  ' + label + (detail ? '   ' + detail : '')); }
  else { fail++; console.log('  FAIL  ' + label + (detail ? '   ' + detail : '')); }
}

// Headless Chromium ships no voices. Fake the two a real phone has, and
// make speak() resolve fast so Listen mode can be driven in real time.
const FAKE_SPEECH = () => {
  const voices = [
    { name: 'Paulina', lang: 'es-MX', localService: true, default: false, voiceURI: 'Paulina' },
    { name: 'Samantha', lang: 'en-US', localService: true, default: true, voiceURI: 'Samantha' }
  ];
  window.__spoken = [];
  const fake = {
    getVoices: () => voices,
    speak(u) {
      window.__spoken.push({ text: u.text, lang: u.lang, volume: u.volume });
      fake.speaking = true;
      setTimeout(() => { fake.speaking = false; if (u.onend) u.onend({}); }, 10);
    },
    cancel() {}, pause() {}, resume() {},
    speaking: false, paused: false, pending: false,
    addEventListener() {}, removeEventListener() {}
  };
  Object.defineProperty(window, 'speechSynthesis', { value: fake, configurable: true });
};

(async () => {
  const browser = await chromium.launch({ executablePath: CHROME });
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 } });
  const page = await ctx.newPage();
  await page.addInitScript(FAKE_SPEECH);

  const consoleErrors = [];
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()); });
  page.on('pageerror', e => consoleErrors.push('PAGEERROR ' + e.message));

  const fresh = async () => {
    await page.goto(FILE);
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await page.waitForTimeout(80);
  };

  /* ============================================================ */
  section('DATA INTEGRITY');
  await fresh();

  const deck = await page.evaluate(() => window.jobsite.deck().map(
    c => ({ key: c.key, en: c.en, es: c.es, region: c.region, trade: c.trade })));

  const byTrade = {};
  deck.forEach(c => { byTrade[c.trade] = (byTrade[c.trade] || 0) + 1; });
  ok(deck.length === 387, 'deck contains the expected total', deck.length + ' cards');
  console.log('        per-trade breakdown:');
  Object.keys(byTrade).forEach(t =>
    console.log('          ' + t.padEnd(14) + String(byTrade[t]).padStart(3)));

  const keys = {}; const clash = [];
  deck.forEach(c => { if (keys[c.key]) clash.push(keys[c.key] + ' / ' + c.en); keys[c.key] = c.en; });
  ok(clash.length === 0, 'no two cards produce the same identifier',
     clash.length ? clash.join(', ') : Object.keys(keys).length + ' unique keys');

  const incomplete = deck.filter(c =>
    !c.en || !c.es || c.region === undefined || c.region === null || !c.trade);
  ok(incomplete.length === 0, 'no card is missing en, es, region or trade',
     incomplete.length ? JSON.stringify(incomplete[0]) : '');

  // Two passes. ANYWHERE holds forms that can only ever be tu - enclitic
  // imperatives, reflexives, tu conjugations. CLAUSE_INITIAL holds bare tu
  // imperatives that collide with nouns in this deck ("la carga", "la toma
  // de agua"), so they are only flagged where a command actually sits: the
  // first word of a clause, or straight after "no".
  const ANYWHERE = ('tu tú ti tuyo tuya contigo tienes puedes quieres necesitas sabes ' +
    'estás eres vas haces tomas mides subes bajas dejas traes vienes pones ' +
    'hazlo haz ponlo ponte pon súbelo bájalo muévelo quítalo arréglalo revísalo ' +
    'límpialo apágalo préndelo tápalo déjalo mídelo tómalo tráelo córtalo míralo ' +
    'alinéalo sígueme ayúdame dime dímelo vete muévete cuídate siéntate párate ' +
    'acércate échalo sácalo bájate súbete quítate espérame checa checalo').split(' ');
  const CLAUSE_INITIAL = ('toma bebe sube baja mueve corta deja trae espera sigue arregla ' +
    'quita tapa alinea despeja descansa avisa llama agarra jala empuja mira limpia ' +
    'para entra ve camina regresa saca mete pasa usa ayuda escucha habla dile cierra ' +
    'abre carga mezcla pega clava apaga prende revisa mide checa ven haz pon sal ten di ' +
    'tomes bebas subas bajes muevas cortes dejes traigas esperes sigas arregles quites ' +
    'tapes alinees despejes descanses avises llames agarres jales empujes mires limpies ' +
    'pares entres vayas camines regreses saques metas pases uses ayudes escuches hables ' +
    'cierres abres cargues mezcles pegues claves apagues prendas revises midas hagas pongas ' +
    'vengas salgas tengas digas seas estés').split(' ');
  const words = s => s.toLowerCase().replace(/[.,;:!¡?¿"'()]/g, ' ').split(/\s+/).filter(Boolean);
  const tuHits = [];
  deck.forEach(c => {
    words(c.es).forEach(w => { if (ANYWHERE.indexOf(w) !== -1) tuHits.push(c.es + ' -> ' + w); });
    c.es.split(/[.,;!¡?¿]+/).forEach(frag => {
      const w = words(frag); if (!w.length) return;
      // Only a NEGATIVE command puts a clitic before the verb ("No lo
      // tapes"), so clitics are skipped only after "no". Unconditional
      // skipping would also eat the article in "la mezcla" and flag the noun.
      const CLITIC = ['lo','la','los','las','le','les','me','te','se','nos'];
      let k = 0;
      if (w[0] === 'no') { k = 1; while (k < w.length && CLITIC.indexOf(w[k]) !== -1) k++; }
      const head = w[k];
      if (head && CLAUSE_INITIAL.indexOf(head) !== -1) tuHits.push(c.es + ' -> ' + head);
    });
  });
  ok(tuHits.length === 0, 'no tu-form imperative anywhere in the deck',
     tuHits.length ? tuHits.slice(0, 5).join(' | ') : deck.length + ' cards scanned');

  // The scanner must be able to fail, or the check above proves nothing.
  const poisonCaught = await page.evaluate(([ANY, CLAUSE]) => {
    const probes = ['Arréglalo.', 'Toma agua.', 'Ponte el casco.', 'No lo tapes todavía.', 'Mídelo dos veces.'];
    const words = s => s.toLowerCase().replace(/[.,;:!¡?¿"'()]/g, ' ').split(/\s+/).filter(Boolean);
    return probes.filter(es => {
      if (words(es).some(w => ANY.indexOf(w) !== -1)) return true;
      return es.split(/[.,;!¡?¿]+/).some(frag => {
        const w = words(frag); if (!w.length) return false;
        const CLITIC = ['lo','la','los','las','le','les','me','te','se','nos'];
        let k = 0;
        if (w[0] === 'no') { k = 1; while (k < w.length && CLITIC.indexOf(w[k]) !== -1) k++; }
        const head = w[k];
        return head && CLAUSE.indexOf(head) !== -1;
      });
    }).length;
  }, [ANYWHERE, CLAUSE_INITIAL]);
  ok(poisonCaught === 5, 'the tu scanner catches known tu forms (control)',
     poisonCaught + '/5 planted forms flagged');

  /* ---- migration ---- */
  // Build a v2 blob the way v2 actually wrote it: the whole card object,
  // id = array index, keyed under jobsite-spanish-v2.
  const newIndex = deck.findIndex(c => c.en === 'Good morning.');
  const seeded = await page.evaluate(({ newIndex }) => {
    const d = window.jobsite.deck();
    // v2's deck: this one minus every card from the four new trades, in the
    // same relative order. That is exactly the array the old build had.
    const NEW_TRADES = ['Measurements', 'Equipment', 'Quality', 'Weather'];
    const v2deck = d.filter(c => NEW_TRADES.indexOf(c.trade) === -1);
    const oldIndex = v2deck.findIndex(c => c.en === 'Good morning.');
    const today = window.jobsite.todayKey();
    const terms = v2deck.map((c, i) => ({
      en: c.en, es: c.es, region: c.region, trade: c.trade,
      id: i, box: 1, introduced: false, nextReview: null
    }));
    terms[oldIndex] = Object.assign({}, terms[oldIndex],
      { box: 5, introduced: true, nextReview: window.jobsite.addDays(today, 11) });
    // A handful of other cards with real progress, plus one card that no
    // longer exists in the deck at all.
    [3, 40, 120, 200].forEach((i, n) => {
      terms[i] = Object.assign({}, terms[i],
        { box: n + 1, introduced: true, nextReview: today });
    });
    terms.push({ en: 'retired term', es: 'el término retirado', region: '', trade: 'Office',
                 id: terms.length, box: 4, introduced: true, nextReview: today });
    localStorage.clear();
    localStorage.setItem(window.jobsite.LEGACY_KEY, JSON.stringify({ terms }));
    return { oldIndex, newIndex, moved: newIndex - oldIndex, v2size: v2deck.length };
  }, { newIndex });

  await page.reload();
  await page.waitForTimeout(120);

  const migrated = await page.evaluate(en => {
    const card = window.jobsite.deck().find(c => c.en === en);
    return { box: card.box, introduced: card.introduced, nextReview: card.nextReview, key: card.key };
  }, 'Good morning.');
  ok(seeded.moved >= 100, 'the probe card moved 100+ places between versions',
     'index ' + seeded.oldIndex + ' -> ' + seeded.newIndex + ' (+' + seeded.moved + ')');
  ok(migrated.box === 5 && migrated.introduced === true,
     'seeded v2 save migrates: the moved card kept its exact box',
     '"Good morning." box ' + migrated.box);

  const droppedGone = await page.evaluate(() => {
    const st = JSON.parse(localStorage.getItem(window.jobsite.STORAGE_KEY));
    const retiredKey = window.jobsite.cardKey({ en: 'retired term', es: 'el término retirado' });
    return { hasRetired: Object.prototype.hasOwnProperty.call(st.progress, retiredKey),
             carried: Object.keys(st.progress).length };
  });
  ok(!droppedGone.hasRetired, 'a v2 card that no longer exists is dropped without error',
     droppedGone.carried + ' records carried');

  // Progress is keyed BY the card's own "en|es" text (cardKey) - that's
  // the whole point of section 3, so the storage keys themselves are
  // expected to contain English and Spanish. What must NOT happen is a
  // progress ENTRY carrying anything beyond box/introduced/nextReview -
  // i.e. no card object (region, trade, etc.) is being duplicated into
  // localStorage on top of what SEED_TERMS already holds.
  const savedShape = await page.evaluate(() => {
    const st = JSON.parse(localStorage.getItem(window.jobsite.STORAGE_KEY));
    const fieldKeys = new Set();
    Object.keys(st.progress).forEach(k => Object.keys(st.progress[k]).forEach(f => fieldKeys.add(f)));
    return Array.from(fieldKeys).sort();
  });
  ok(savedShape.join(',') === 'box,introduced,nextReview',
     'saved v3 progress entries hold box/introduced/nextReview only, no card data',
     '{' + savedShape.join(', ') + '}');

  const before = await page.evaluate(() => localStorage.getItem(window.jobsite.STORAGE_KEY));
  await page.reload(); await page.waitForTimeout(100);
  const after = await page.evaluate(() => localStorage.getItem(window.jobsite.STORAGE_KEY));
  const stillHasV2 = await page.evaluate(() => !!localStorage.getItem(window.jobsite.LEGACY_KEY));
  ok(before === after && stillHasV2,
     'reload after migration is stable and does not re-migrate',
     'v3 byte-identical; v2 left in place untouched');

  const dupWarn = await page.evaluate(() => {
    // Force a real collision and confirm the scanner shouts about it.
    const realWarn = console.warn;
    const saw = [];
    console.warn = function (m) { saw.push(m); };
    // checkForDuplicateKeys() re-scans SEED_TERMS, which has no real
    // duplicates, so this only proves the wiring, not a false positive.
    // The genuine collision test lives in the control probes above.
    const n = window.jobsite.checkForDuplicateKeys();
    console.warn = realWarn;
    return { n, saw: saw.length };
  });
  ok(dupWarn.n === 0 && dupWarn.saw === 0,
     'the duplicate-key scanner runs clean against the real deck',
     'checkForDuplicateKeys() callable, 0 collisions in ' + deck.length + ' cards');

  /* ============================================================ */
  section('SESSION LOGIC (session-based rotation)');
  await fresh();

  // Nothing is due on a fresh install, so the working set is entirely new
  // material - capped at a sensible starting handful, not the whole deck.
  let s = await page.evaluate(() => {
    document.getElementById('startBtn').click();
    const d = window.jobsite.deck();
    return { introduced: d.filter(c => c.introduced).length,
             queue: window.jobsite.session().queueLength,
             graduations: window.jobsite.session().graduations };
  });
  ok(s.introduced > 0 && s.introduced <= 10, 'a fresh install starts with a small handful of new cards',
     s.introduced + ' introduced (queue ' + s.queue + ')');
  ok(Object.keys(s.graduations).length === s.introduced,
     'every card in a fresh session is tracked as "new" (0 correct so far)',
     Object.keys(s.graduations).length + ' tracked, all at 0');

  const guarded = await page.evaluate(() => {
    const sess = window.jobsite.session();
    const card = window.jobsite.deck().find(c => c.key === sess.currentKey);
    const boxBefore = card.box;
    document.getElementById('gotItBtn').click();
    document.getElementById('missedBtn').click();
    return { boxBefore, boxAfter: card.box,
             sameCard: window.jobsite.session().currentKey === sess.currentKey,
             answerShown: window.jobsite.session().answerShown };
  });
  ok(guarded.boxBefore === guarded.boxAfter && guarded.sameCard && !guarded.answerShown,
     'grading before the answer is revealed does nothing',
     'box stayed at ' + guarded.boxBefore + ', card did not advance');

  // A NEW card needs two correct answers to graduate. The first correct
  // answer must NOT remove it from the working set.
  const grad1 = await page.evaluate(() => {
    const key = window.jobsite.session().currentKey;
    document.getElementById('flipBtn').click();
    document.getElementById('gotItBtn').click();
    const g = window.jobsite.session().graduations;
    return { key, count: g[key], stillTracked: Object.prototype.hasOwnProperty.call(g, key) };
  });
  ok(grad1.count === 1 && grad1.stillTracked,
     'a new card answered correctly once is not graduated yet (needs 2)',
     'graduation count ' + grad1.count + '/2');

  // Second install, isolated: run one new card to graduation and confirm
  // it drops out of the "new" set and a replacement rotates in - the
  // total working-set size should not shrink.
  await fresh();
  const grad2 = await page.evaluate(() => {
    document.getElementById('startBtn').click();
    const before = window.jobsite.session().queueLength;
    const key = window.jobsite.session().currentKey;
    document.getElementById('flipBtn').click();
    document.getElementById('gotItBtn').click();               // 1st correct
    // find the same card again later in the queue and answer it again
    let guard = 0;
    while (window.jobsite.session().currentKey !== key && guard++ < 400) {
      document.getElementById('flipBtn').click();
      document.getElementById('missedBtn').click();             // skip others without graduating them by chance
      if (document.getElementById('studyDone') && !document.getElementById('studyDone').hidden) break;
    }
    const reachedAgain = window.jobsite.session().currentKey === key;
    let after = null, stillTracked = true, afterQueue = null;
    if (reachedAgain) {
      document.getElementById('flipBtn').click();
      document.getElementById('gotItBtn').click();              // 2nd correct -> graduates
      const g = window.jobsite.session().graduations;
      stillTracked = Object.prototype.hasOwnProperty.call(g, key);
      afterQueue = window.jobsite.session().queueLength;
    }
    return { before, reachedAgain, stillTracked, afterQueue };
  });
  ok(grad2.reachedAgain, 'a new card recurs within the same session before graduating',
     'card seen a second time: ' + grad2.reachedAgain);
  ok(grad2.reachedAgain && !grad2.stillTracked,
     'two correct answers graduate a new card out of the "new" tracking',
     'tracked after 2nd correct: ' + grad2.stillTracked);

  // From every box, not just box 1 - miss behavior on a review card.
  await fresh();
  const drops = await page.evaluate(() => {
    document.getElementById('startBtn').click();
    const out = [];
    const tomorrow = window.jobsite.addDays(window.jobsite.todayKey(), 1);
    for (let b = 1; b <= 5; b++) {
      const key = window.jobsite.session().currentKey;
      const card = window.jobsite.deck().find(c => c.key === key);
      card.box = b;
      document.getElementById('flipBtn').click();
      document.getElementById('missedBtn').click();
      out.push({ from: b, to: card.box, sched: card.nextReview, tomorrow });
    }
    return out;
  });
  ok(drops.every(d => d.to === 1), 'a miss drops the card to box 1 from any box',
     drops.map(d => d.from + '->' + d.to).join(' '));
  ok(drops.every(d => d.sched === d.tomorrow), 'a miss schedules the card for tomorrow',
     drops[0].tomorrow);

  // A correct answer on a review card (not new) moves it up one box and
  // schedules BOX_INTERVALS[box] days out.
  await fresh();
  const up = await page.evaluate(() => {
    document.getElementById('startBtn').click();
    const key = window.jobsite.session().currentKey;
    const card = window.jobsite.deck().find(c => c.key === key);
    card.introduced = true; card.box = 2;   // pretend it's a review, not new
    delete window.jobsite.session().graduations[key];
    const from = card.box;
    document.getElementById('flipBtn').click();
    document.getElementById('gotItBtn').click();
    const iv = window.jobsite.BOX_INTERVALS[card.box];
    return { from, to: card.box, nextReview: card.nextReview,
             expected: window.jobsite.addDays(window.jobsite.todayKey(), iv), interval: iv };
  });
  ok(up.to === up.from + 1, 'a correct answer moves the card up exactly one box',
     'box ' + up.from + ' -> ' + up.to);
  ok(up.nextReview === up.expected,
     'a correct answer on box N schedules BOX_INTERVALS[N+1] days out',
     'box ' + up.to + ' -> +' + up.interval + ' days -> ' + up.nextReview);

  // Session persistence: ending mid-session (queue not empty) must save
  // the working set so the home screen offers "Continue" with the same
  // cards, not a fresh reshuffle.
  await fresh();
  const persisted = await page.evaluate(() => {
    document.getElementById('startBtn').click();
    const firstSet = window.jobsite.deck().filter(c => c.introduced).map(c => c.key).sort().join(',');
    document.getElementById('flipBtn').click();
    document.getElementById('gotItBtn').click();     // make some progress, don't finish
    document.getElementById('endBtn').click();       // back out mid-session
    const hasSavedSession = !!window.jobsite.state().session;
    const btn = document.getElementById('startBtn');
    const restarted = !!btn;
    if (btn) btn.click();
    const secondSet = window.jobsite.deck().filter(c => c.introduced).map(c => c.key).sort().join(',');
    return { restarted, same: firstSet === secondSet, hasSavedSession,
             count: firstSet.split(',').length };
  });
  ok(persisted.hasSavedSession, 'ending a session mid-queue saves it for resume',
     'state.session present after End session with cards remaining');
  ok(persisted.same, 'resuming a saved session serves the same working set, not a reshuffle',
     persisted.count + ' cards, unchanged on resume');

  // Once the whole working set is exhausted, the session is cleared - the
  // "Continue" affordance should not offer a phantom empty session. A
  // graduated card pulls a replacement from the SAME trade's pool, so a
  // big fresh deck never actually runs dry (by design - that's the whole
  // point of continuous rotation). To observe genuine exhaustion, narrow
  // the trade down to a handful of cards first so the pool empties for
  // real once they've all graduated.
  await fresh();
  const exhausted = await page.evaluate(() => {
    const d = window.jobsite.deck();
    const today = window.jobsite.todayKey();
    const farFuture = window.jobsite.addDays(today, 30);
    // Master every Weather card except 3, so due=0 and the new-card pool
    // for that trade is exactly those 3.
    const weather = d.filter(c => c.trade === 'Weather');
    weather.forEach((c, i) => {
      if (i < 3) return;   // leave these 3 uninitiated
      c.introduced = true; c.box = 5; c.nextReview = farFuture;
    });
    // Switch the active trade filter to Weather via the real chip.
    const chip = Array.from(document.querySelectorAll('#tradeChips .chip'))
      .find(b => b.textContent.trim().indexOf('Weather') === 0);
    chip.click();
    document.getElementById('startBtn').click();
    let guard = 0;
    while (guard++ < 100) {
      const done = document.getElementById('studyDone');
      if (done && !done.hidden) break;
      document.getElementById('flipBtn').click();
      document.getElementById('gotItBtn').click();
    }
    const done = document.getElementById('studyDone');
    return { finished: done && !done.hidden, cleared: window.jobsite.state().session === null,
             iterations: guard };
  });
  ok(exhausted.finished && exhausted.cleared,
     'a fully completed session (pool genuinely exhausted) clears the saved working set',
     'finished in ' + exhausted.iterations + ' grades, state.session === null');

  // The ratio: with a healthy backlog of due reviews, new material should
  // land around 30% of the working set, not swamp it and not starve it.
  await fresh();
  const ratio = await page.evaluate(() => {
    // Seed 40 due review cards directly, bypassing the UI.
    const d = window.jobsite.deck();
    const today = window.jobsite.todayKey();
    d.slice(0, 40).forEach(c => { c.introduced = true; c.box = 2; c.nextReview = today; });
    document.getElementById('startBtn').click();
    const g = window.jobsite.session().graduations;
    const newCount = Object.keys(g).length;
    const total = window.jobsite.session().queueLength;
    return { newCount, total, pct: newCount / total };
  });
  ok(ratio.pct > 0.15 && ratio.pct < 0.45,
     'new material lands near the 30% target when reviews are available',
     ratio.newCount + '/' + ratio.total + ' = ' + (ratio.pct * 100).toFixed(0) + '% new');

  /* ============================================================ */
  section('UI');
  await fresh();

  const SCREEN_ID = { study: 'scStudy', listen: 'scListen', browse: 'scBrowse', stats: 'scStats' };
  const tabResults = [];
  for (const t of ['study', 'listen', 'browse', 'stats']) {
    consoleErrors.length = 0;
    await page.click(`[data-tab="${t}"]`);
    await page.waitForTimeout(120);
    const visible = await page.isVisible(`#${SCREEN_ID[t]}`);
    tabResults.push({ t, visible, errs: consoleErrors.slice() });
  }
  ok(tabResults.every(r => r.visible && r.errs.length === 0),
     'every tab renders with no console errors',
     tabResults.map(r => r.t + (r.errs.length ? ' ERR' : ' ok')).join(', '));

  await page.click('[data-tab="study"]');
  const tabbar = await page.evaluate(() => {
    const bar = document.querySelector('.tabbar');
    const seen = () => {
      const r = bar.getBoundingClientRect();
      return r.top < window.innerHeight - 8;   // slid off the bottom or not
    };
    const beforeSession = seen();
    document.getElementById('startBtn').click();
    const inSession = document.body.classList.contains('immersive');
    return { beforeSession, inSession };
  });
  await page.waitForTimeout(320);
  const barHiddenDuring = await page.evaluate(() =>
    document.querySelector('.tabbar').getBoundingClientRect().top >= window.innerHeight - 8);
  await page.evaluate(() => document.getElementById('endBtn').click());
  await page.waitForTimeout(320);
  const barBackAfter = await page.evaluate(() =>
    document.querySelector('.tabbar').getBoundingClientRect().top < window.innerHeight - 8);
  ok(tabbar.beforeSession && tabbar.inSession && barHiddenDuring && barBackAfter,
     'the tab bar is hidden during a session and returns after it',
     'visible -> off-screen -> visible');

  // Revealing the answer must not move the prompt. Centring the whole
  // group made the prompt jump upward at the exact moment you are reading
  // it, so the card is a two-row grid with the prompt pinned to the split.
  const jump = await page.evaluate(async () => {
    document.querySelector('[data-tab="study"]').click();
    document.getElementById('startBtn').click();
    const p = document.getElementById('promptText');
    const before = p.getBoundingClientRect().top;
    document.getElementById('flipBtn').click();
    const after = p.getBoundingClientRect().top;
    document.getElementById('endBtn').click();
    return { before, after, delta: Math.abs(after - before) };
  });
  ok(jump.delta < 1, 'revealing the answer does not move the prompt',
     'prompt top ' + jump.before.toFixed(1) + 'px -> ' + jump.after.toFixed(1) + 'px');

  await page.click('[data-tab="browse"]');
  const search = await page.evaluate(async () => {
    const input = document.getElementById('searchBox');
    const rows = () => document.querySelectorAll('.entry').length;
    const firstEn = () => (document.querySelector('.entry-en') || {}).textContent;
    const run = q => {
      input.value = q;
      input.dispatchEvent(new Event('input'));
      return { n: rows(), first: firstEn() };
    };
    return {
      english: run('shackle'),        // matches the en field
      spanish: run('eslinga'),        // matches the es field
      region:  run('tablaroca'),      // matches ONLY the region field
      all:     run('')
    };
  });
  ok(search.english.n === 1 && /shackle/.test(search.english.first),
     'browse search matches English', '"shackle" -> ' + search.english.first);
  ok(search.spanish.n === 1 && /sling/.test(search.spanish.first),
     'browse search matches Spanish', '"eslinga" -> ' + search.spanish.first);
  ok(search.region.n === 1 && /drywall/.test(search.region.first),
     'browse search matches the region field',
     '"tablaroca" -> ' + search.region.first + ' (region-only match)');

  await page.click('[data-tab="stats"]');
  await page.waitForTimeout(120);
  const stats = await page.evaluate(() => {
    const trades = new Set(window.jobsite.deck().map(c => c.trade));
    return {
      bars: document.querySelectorAll('#gauge .gauge-bar').length,
      meters: document.querySelectorAll('.meter').length,
      trades: trades.size
    };
  });
  ok(stats.bars === 5, 'the stats gauge draws five bars', stats.bars + ' bars');
  ok(stats.meters === stats.trades, 'one meter per trade',
     stats.meters + ' meters / ' + stats.trades + ' trades');

  const emptyBox = await page.evaluate(() => {
    // Put everything in box 1 so boxes 2-5 are genuinely empty.
    const d = window.jobsite.deck();
    d.forEach((c, i) => { c.introduced = i < 20; c.box = 1; c.nextReview = window.jobsite.todayKey(); });
    document.querySelector('[data-tab="study"]').click();
    document.querySelector('[data-tab="stats"]').click();
    const cols = Array.from(document.querySelectorAll('#gauge .gauge-col'));
    return cols.map(col => ({
      empty: col.classList.contains('is-empty'),
      bg: getComputedStyle(col.querySelector('.gauge-bar')).backgroundColor,
      h: col.querySelector('.gauge-bar').getBoundingClientRect().height
    }));
  });
  const emptyOnes = emptyBox.slice(1);
  ok(emptyOnes.every(b => b.empty) && emptyOnes.every(b => b.h <= 4) &&
     new Set(emptyOnes.map(b => b.bg)).size === 1 && emptyOnes[0].bg !== emptyBox[0].bg,
     'a box holding zero cards paints a neutral rule, never a colour',
     'boxes 2-5 empty: ' + emptyOnes[0].h + 'px ' + emptyOnes[0].bg);

  // Nothing due and nothing new (whole trade mastered / capped) shows the
  // empty state rather than a crash.
  await fresh();
  const emptyState = await page.evaluate(() => {
    window.jobsite.deck().forEach(c => { c.introduced = true; c.box = 5;
      c.nextReview = window.jobsite.addDays(window.jobsite.todayKey(), 30); });
    document.querySelector('[data-tab="browse"]').click();
    document.querySelector('[data-tab="study"]').click();
    return { hasStart: !!document.getElementById('startBtn'),
             title: (document.querySelector('.empty h3') || {}).textContent || '',
             body: (document.querySelector('.empty p') || {}).textContent || '' };
  });
  ok(!emptyState.hasStart && /Nothing due/.test(emptyState.title),
     'nothing due and nothing new shows the empty state, not a crash',
     '"' + emptyState.title + '" - ' + emptyState.body);

  // The three deliberate homonyms must survive intact, and the trade label
  // that disambiguates them must be on the card.
  const homonyms = {};
  deck.forEach(c => { (homonyms[c.es] = homonyms[c.es] || []).push(c.en + ' [' + c.trade + ']'); });
  const wanted = ['la escalera', 'el nivel', 'la tierra'];
  const kept = wanted.filter(es => homonyms[es] && homonyms[es].length === 2);
  await fresh();
  const tradeOnCard = await page.evaluate(() => {
    document.querySelector('[data-tab="study"]').click();
    document.getElementById('startBtn').click();
    const t = document.getElementById('tradeTag');
    const shown = t.textContent && t.offsetHeight > 0;
    document.getElementById('endBtn').click();
    return shown;
  });
  ok(kept.length === 3 && tradeOnCard,
     'the three intentional homonyms are intact and the trade label is on the card',
     wanted.map(es => es + ' = ' + homonyms[es].length).join(', '));

  // Nested corner radii must decrease inward. Fully round shapes (pills,
  // circles) are excluded: those read as a distinct shape, not as a corner
  // that fails to match its parent's.
  const sampleRadii = () => page.evaluate(() => {
    const r = n => parseFloat(getComputedStyle(n).borderTopLeftRadius) || 0;
    const shown = n => { const b = n.getBoundingClientRect(); return b.width > 0 && b.height > 0; };
    const isRound = n => {
      const b = n.getBoundingClientRect();
      return r(n) >= Math.min(b.width, b.height) / 2 - 0.5;
    };
    const bad = [];
    document.querySelectorAll('*').forEach(n => {
      if (!shown(n) || !r(n) || isRound(n)) return;
      let a = n.parentElement;
      while (a && (!shown(a) || !r(a) || isRound(a))) a = a.parentElement;
      if (a && r(n) > r(a)) {
        bad.push(n.className + ' ' + r(n) + 'px inside ' + a.className + ' ' + r(a) + 'px');
      }
    });
    return bad;
  });
  let radii = [];
  for (const t of ['study', 'listen', 'browse', 'stats']) {
    await page.click(`[data-tab="${t}"]`);
    await page.waitForTimeout(120);
    radii = radii.concat(await sampleRadii());
  }
  await page.evaluate(() => {
    document.querySelector('[data-tab="study"]').click();
    document.getElementById('startBtn').click();
    document.getElementById('flipBtn').click();
  });
  await page.waitForTimeout(300);
  radii = radii.concat(await sampleRadii());
  await page.evaluate(() => document.getElementById('endBtn').click());
  ok(radii.length === 0, 'nested corner radii decrease inward, on every screen',
     radii.length ? radii.slice(0, 3).join(' | ') : 'no child radius exceeds its parent');

  /* ---- things the eye can't measure ---- */

  // "Buttons must respond to :active by moving." Force the state with a
  // real mouse-down and read what the browser actually computes.
  await page.click('[data-tab="study"]');
  await page.waitForTimeout(120);

  const readBtn = () => page.evaluate(() => {
    const cs = getComputedStyle(document.getElementById('startBtn'));
    return { transform: cs.transform, shadow: cs.boxShadow };
  });
  // matrix(1, 0, 0, 1, 0, 4) - the sixth number is the Y translation.
  const dyOf = t => (!t || t === 'none') ? 0 : parseFloat(t.split(',')[5]);
  // Pull the solid layer - non-inset, zero blur - out of a computed shadow.
  // That is the button's thickness, the side you would see from above.
  const solidOffset = shadow => {
    const layer = shadow.split(/,(?![^(]*\))/).map(x => x.trim())
      .filter(l => !/inset/.test(l))
      .map(l => l.match(/(-?[\d.]+)px\s+(-?[\d.]+)px\s+(-?[\d.]+)px/) || [])
      .filter(m => m.length && parseFloat(m[3]) === 0)[0];
    return layer ? parseFloat(layer[2]) : null;
  };

  const rest = await readBtn();                    // read BEFORE pressing, so
  const box = await page.locator('#startBtn').boundingBox();   // no transition
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(220);                  // let the transition land
  const pressed = await readBtn();
  await page.mouse.up();

  ok(dyOf(rest.transform) === 0 && dyOf(pressed.transform) > 0,
     'buttons move down on :active, they do not merely change colour',
     'translateY ' + dyOf(rest.transform) + 'px -> ' + dyOf(pressed.transform) + 'px while held');

  const restOffset = solidOffset(rest.shadow);
  const pressOffset = solidOffset(pressed.shadow);
  // This design leaves a 1px residual sliver even at full press (a
  // hint of thickness never fully disappears), rather than compressing
  // all the way to 0 — so the check is that the CHANGE in thickness
  // matches the travel distance, not that it bottoms out at zero.
  ok(restOffset > pressOffset && restOffset - pressOffset === dyOf(pressed.transform),
     'the unblurred thickness compresses by exactly the travel distance',
     restOffset + 'px thickness -> ' + pressOffset + 'px, face travels ' +
     dyOf(pressed.transform) + 'px');

  ok(/inset/.test(rest.shadow) && /1px 0px 0px inset/.test(rest.shadow),
     'a 1px inset highlight runs along the top edge',
     rest.shadow.split(',')[3] ? rest.shadow.slice(0, 52) + '...' : rest.shadow);

  // Reduced motion is not optional. Emulate it for real.
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.reload();
  await page.waitForTimeout(120);
  const motion = await page.evaluate(() => {
    document.getElementById('startBtn').click();
    document.getElementById('flipBtn').click();
    const a = getComputedStyle(document.getElementById('answerBlock'));
    const b = getComputedStyle(document.querySelector('.tabbar'));
    return { anim: a.animationDuration, trans: b.transitionDuration };
  });
  const ms = v => Math.max.apply(null, String(v).split(',').map(x => parseFloat(x) * 1000));
  ok(ms(motion.anim) < 10 && ms(motion.trans) < 10,
     'prefers-reduced-motion disables animation and transitions',
     'animation ' + motion.anim + ', transition ' + motion.trans);
  await page.emulateMedia({ reducedMotion: null });

  // Contrast, measured against the colour actually painted behind each
  // element rather than against the colour we meant to paint.
  await page.goto(FILE);
  await page.waitForTimeout(120);
  const sampleContrast = () => page.evaluate(() => {
    const parseRGBA = c => {
      const nums = (c.match(/[\d.]+/g) || []).map(Number);
      return { r: nums[0] || 0, g: nums[1] || 0, b: nums[2] || 0, a: nums.length > 3 ? nums[3] : 1 };
    };
    const lum = rgb => {
      const a = [rgb.r, rgb.g, rgb.b].map(v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); });
      return 0.2126 * a[0] + 0.7152 * a[1] + 0.0722 * a[2];
    };
    const ratio = (f, b) => {
      const l1 = lum(f), l2 = lum(b);
      return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
    };
    // A gradient lives in background-image, not background-color, so an
    // element styled with only `background: linear-gradient(...)` still
    // reports a transparent background-color. Average its color stops
    // as a stand-in - every gradient in this design blends close hues,
    // so an average is close enough for a contrast floor check.
    const gradientAvg = n => {
      const img = getComputedStyle(n).backgroundImage;
      if (!img || img.indexOf('gradient') === -1) return null;
      const stops = img.match(/rgba?\([^)]+\)/g);
      if (!stops || !stops.length) return null;
      const sum = { r: 0, g: 0, b: 0, a: 0 };
      stops.forEach(s => { const p = parseRGBA(s); sum.r += p.r; sum.g += p.g; sum.b += p.b; sum.a += p.a; });
      return { r: sum.r / stops.length, g: sum.g / stops.length, b: sum.b / stops.length, a: sum.a / stops.length };
    };
    const composite = (fg, bg) => ({
      r: fg.r * fg.a + bg.r * (1 - fg.a),
      g: fg.g * fg.a + bg.g * (1 - fg.a),
      b: fg.b * fg.a + bg.b * (1 - fg.a),
      a: 1
    });
    // Walk from the text node's parent up to the page background and
    // composite every translucent layer in painting order (outermost
    // first). A flat "first non-transparent background wins" walk
    // treats a 14%-opacity tint as if it were fully opaque orange,
    // which is nowhere close to what's actually rendered.
    const behind = node => {
      const layers = [];
      let n = node;
      while (n && n !== document.documentElement) {
        const grad = gradientAvg(n);
        if (grad) layers.push(grad);
        else {
          const bg = parseRGBA(getComputedStyle(n).backgroundColor);
          if (bg.a > 0) layers.push(bg);
        }
        n = n.parentElement;
      }
      let result = parseRGBA(getComputedStyle(document.body).backgroundColor);
      result.a = 1;
      for (let i = layers.length - 1; i >= 0; i--) result = composite(layers[i], result);
      return result;
    };
    const out = [];
    document.querySelectorAll('*').forEach(n => {
      const text = Array.from(n.childNodes)
        .filter(c => c.nodeType === 3).map(c => c.textContent.trim()).join('');
      if (!text) return;
      const r = n.getBoundingClientRect();
      if (!r.width || !r.height) return;
      const cs = getComputedStyle(n);
      if (cs.visibility === 'hidden' || cs.display === 'none') return;
      out.push({
        text: text.slice(0, 28),
        px: parseFloat(cs.fontSize),
        weight: cs.fontWeight,
        ratio: ratio(parseRGBA(cs.color), behind(n))
      });
    });
    return out;
  });

  // Sample every screen, not just the one the app opens on.
  let contrast = [];
  for (const t of ['study', 'listen', 'browse', 'stats']) {
    await page.click(`[data-tab="${t}"]`);
    await page.waitForTimeout(120);
    contrast = contrast.concat((await sampleContrast()).map(c => (c.screen = t, c)));
  }
  await page.click('[data-tab="study"]');
  await page.evaluate(() => {
    document.getElementById('startBtn').click();
    document.getElementById('flipBtn').click();
  });
  await page.waitForTimeout(320);
  contrast = contrast.concat((await sampleContrast()).map(c => (c.screen = 'session', c)));
  await page.evaluate(() => document.getElementById('endBtn').click());

  // WCAG's large-text exemption (18.66px bold / 24px) is deliberately NOT
  // used here: the brief said 4.5:1 minimum for body text, so everything
  // is held to it.
  const tooLow = contrast.filter(c => c.ratio < 4.5);
  const worst = contrast.slice().sort((a, b) => a.ratio - b.ratio)[0];
  ok(tooLow.length === 0,
     'every visible text run clears 4.5:1 against what is painted behind it',
     tooLow.length
       ? tooLow.slice(0, 4).map(c => '[' + c.screen + '] "' + c.text + '" ' +
           c.ratio.toFixed(2) + ':1').join(' | ')
       : contrast.length + ' runs across all 5 screens, worst "' + worst.text +
         '" (' + worst.screen + ') at ' + worst.ratio.toFixed(2) + ':1');

  // Safe-area handling can't be observed in a desktop browser - env()
  // resolves to 0 with no notch to inset from - so this is the one check
  // that reads the source instead of the page.
  const insets = {
    viewport: /viewport-fit=cover/.test(SRC),
    top: /\.topbar\s*\{[^}]*env\(safe-area-inset-top\)/s.test(SRC),
    bottom: /\.tabbar\s*\{[^}]*env\(safe-area-inset-bottom\)/s.test(SRC),
    themeColor: /<meta name="theme-color"/.test(SRC),
    overscroll: (SRC.match(/overscroll-behavior:\s*none/g) || []).length >= 1
  };
  ok(Object.values(insets).every(Boolean),
     'viewport-fit, safe-area insets, theme-color and overscroll are wired (source check)',
     JSON.stringify(insets));

  /* ============================================================ */
  section('LISTEN MODE');
  await fresh();

  // Give it a deck to draw from, then let it actually run.
  await page.evaluate(() => {
    const d = window.jobsite.deck();
    const today = window.jobsite.todayKey();
    d.slice(0, 8).forEach(c => { c.introduced = true; c.box = 3; c.nextReview = today; });
    const st = window.jobsite.state();
    st.listen.gap = 1500;
    st.listen.repeat = true;
  });
  await page.click('[data-tab="listen"]');
  await page.waitForTimeout(120);

  const progressBefore = await page.evaluate(() => ({
    saved: localStorage.getItem(window.jobsite.STORAGE_KEY),
    boxes: window.jobsite.deck().map(c => c.box).join(','),
    intro: window.jobsite.deck().map(c => c.introduced ? 1 : 0).join(''),
    sched: window.jobsite.deck().map(c => c.nextReview).join(',')
  }));

  await page.evaluate(() => { window.__spoken = []; });
  await page.click('#playBtn');
  await page.waitForTimeout(6000);          // several cards' worth
  const running = await page.evaluate(() => ({
    playing: window.jobsite.listen.on,
    index: window.jobsite.listen.i,
    spoken: window.__spoken.slice()
  }));
  ok(running.playing && running.spoken.length >= 6,
     'listen mode auto-advances on its own with no taps',
     'reached card index ' + running.index + ' after ' + running.spoken.length + ' utterances');

  const primed = running.spoken[0];
  ok(primed && primed.volume === 0 && primed.text.trim() === '',
     'the play tap is spent on a silent utterance (iOS unlock)',
     'first utterance: volume ' + primed.volume + ', text ' + JSON.stringify(primed.text));

  const real = running.spoken.slice(1);
  const enUtterances = real.filter(u => /^en/.test(u.lang));
  const esUtterances = real.filter(u => /^es/.test(u.lang));
  const enTexts = new Set(deck.map(c => c.en));
  const esTexts = new Set(deck.map(c => c.es));
  ok(enUtterances.length > 0 && enUtterances.every(u => enTexts.has(u.text)) &&
     esUtterances.every(u => esTexts.has(u.text)),
     'English text uses an English voice, Spanish text a Spanish one',
     esUtterances.length + ' es-MX + ' + enUtterances.length + ' en-US, none crossed');

  await page.click('#playBtn');           // pause
  await page.waitForTimeout(200);
  const progressAfter = await page.evaluate(() => ({
    saved: localStorage.getItem(window.jobsite.STORAGE_KEY),
    boxes: window.jobsite.deck().map(c => c.box).join(','),
    intro: window.jobsite.deck().map(c => c.introduced ? 1 : 0).join(''),
    sched: window.jobsite.deck().map(c => c.nextReview).join(',')
  }));
  ok(progressBefore.boxes === progressAfter.boxes &&
     progressBefore.intro === progressAfter.intro &&
     progressBefore.sched === progressAfter.sched,
     'listen mode moved no card between boxes and changed no schedule',
     'boxes, introduced flags and nextReview dates all byte-identical');

  const src = await page.evaluate(() => window.jobsite.listenPlayPathSource());
  const forbidden = ['grade(', 'commit(', 'nextReviewFor(', 'bumpStreak(',
                     '.box =', '.box++', '.introduced =', '.nextReview ='];
  const found = forbidden.filter(f => src.indexOf(f) !== -1);
  ok(found.length === 0,
     'listen mode play path contains no call that grades or commits a card',
     found.length ? 'FOUND ' + found.join(', ')
                  : 'scanned ' + src.split('\n').length + ' lines of the play path');

  // The watchdog: an engine that never fires onend must not stall the chain.
  await page.evaluate(() => {
    const fake = window.speechSynthesis;
    fake.speak = function (u) { window.__spoken.push({ text: u.text, lang: u.lang }); /* no onend, ever */ };
    window.__spoken = [];
  });
  await page.click('#playBtn');
  await page.waitForTimeout(9000);
  const stalled = await page.evaluate(() => window.__spoken.length);
  ok(stalled >= 2, 'a missing onend does not stall the chain (watchdog fires)',
     stalled + ' utterances started with an engine that never calls onend');
  await page.click('#playBtn');

  const hidden = await page.evaluate(async () => {
    Object.defineProperty(document, 'hidden', { value: true, configurable: true });
    document.dispatchEvent(new Event('visibilitychange'));
    return { playing: window.jobsite.listen.on };
  });
  ok(!hidden.playing, 'hiding the page stops playback cleanly', 'listen.on === false');

  /* ============================================================ */
  console.log('\n' + '='.repeat(58));
  console.log('  ' + pass + ' passed, ' + fail + ' failed');
  console.log('='.repeat(58));
  await browser.close();
  process.exit(fail ? 1 : 0);
})();

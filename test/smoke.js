const { chromium } = require('playwright');
const path = require('path');
(async () => {
  const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome' });
  const p = await b.newPage({ viewport: { width: 390, height: 844 } });
  const errs = [];
  p.on('console', m => { if (m.type() === 'error') errs.push(m.text()); });
  p.on('pageerror', e => errs.push('PAGEERROR ' + e.message));
  await p.goto('file://' + path.resolve('index.html'));
  await p.waitForTimeout(300);
  console.log('deck size:', await p.evaluate(() => window.jobsite.deck().length));
  console.log('tabs:', await p.$$eval('.tabbar__btn', n => n.map(x => x.textContent.trim())));
  console.log('errors:', errs);
  await b.close();
})();

const { chromium } = require(require('child_process').execSync('npm root -g').toString().trim() + '/playwright');
const fs = require('fs');
(async () => {
  const [json, out, sV, dbg] = process.argv.slice(2);
  const art = JSON.parse(fs.readFileSync(json, 'utf8'));
  const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome' }).catch(() => chromium.launch());
  const p = await b.newPage({ viewport: { width: 720, height: 810 } });
  await p.setContent('<body style="margin:0;background:#05070a"><canvas id=c width=720 height=810></canvas></body>');
  await p.addScriptTag({ path: __dirname + '/renderer.js' });
  await p.evaluate(([art, sV, dbg]) => { const c = document.getElementById('c').getContext('2d'); render(c, art, 7.2, { sV: +sV, sA: 0, debug: !!+dbg }); }, [art, sV || 0, dbg || 0]);
  await p.screenshot({ path: out });
  await b.close();
})();

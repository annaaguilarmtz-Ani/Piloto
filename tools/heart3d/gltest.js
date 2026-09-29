const { chromium } = require(require('child_process').execSync('npm root -g').toString().trim() + '/playwright');
(async () => {
  const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome', args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist', '--enable-webgl'] });
  const p = await b.newPage();
  await p.setContent('<canvas id=c width=64 height=64></canvas>');
  const r = await p.evaluate(() => { const gl = document.getElementById('c').getContext('webgl2'); return gl ? gl.getParameter(gl.VERSION) + ' | ' + gl.getParameter(gl.MAX_TEXTURE_SIZE) : 'no webgl2'; });
  console.log(r); await b.close();
})();

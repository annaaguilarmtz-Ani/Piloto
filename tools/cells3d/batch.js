// Genera todos los sprites en app/src/main/assets/cells (WebP con alfa)
const fs = require('fs');
const prelude = require('./prelude.js');
const sprites = require('./sprites.js');
const { open, draw } = require('./render.js');
const OUT = __dirname + '/../../app/src/main/assets/cells';
fs.mkdirSync(OUT, { recursive: true });
const jobs = [
  ['mito', 'mito1', null], ['mito', 'mito2', [-0.35, 0.5, -0.35]],
  ['nucleus', 'nucleus'], ['chloro', 'chloro1'], ['chloro', 'chloro2', [-0.4, -0.4, -0.25]], ['golgi', 'golgi'], ['er', 'er'], ['ser', 'ser'], ['lyso', 'lyso'], ['ribo', 'ribo'],
  ['centriole', 'centriole'], ['wbc', 'wbc'], ['lymph', 'lymph'], ['plt', 'plt'], ['bacterium', 'bact'], ['nucleoid', 'nucleoid'], ['plasmid', 'plasmid'], ['pearl', 'pearl'],
  ['rbc', 'rbc0', [0, 0, 0]], ['rbc', 'rbc1', [0.55, 0, 0.3]], ['rbc', 'rbc2', [0.95, 0, 0.6]], ['rbc', 'rbc3', [1.25, 0, 0.9]], ['rbc', 'rbc4', [1.5, 0, 1.2]]
];
const only = process.argv.slice(2);
(async () => {
  for (const [sp, name, rot] of jobs) {
    if (only.length && !only.includes(name)) continue;
    const s = sprites[sp];
    const { b, p } = await open(s.w, s.h);
    const url = await draw(p, prelude + s.glsl, s.w, s.h, rot || s.rot, s.half, 0, s.tint || [0.6, 0.2, 0.1], s.step || 0.75, s.exp || 0.95);
    // reconvertir a WebP con alfa
    const webp = await p.evaluate(async (u) => { const im = new Image(); im.src = u; await im.decode(); const c = document.createElement('canvas'); c.width = im.width; c.height = im.height; const x = c.getContext('2d'); x.drawImage(im, 0, 0); return c.toDataURL('image/webp', 0.9); }, url);
    fs.writeFileSync(`${OUT}/${name}.webp`, Buffer.from(webp.split(',')[1], 'base64'));
    console.log(name); await b.close();
  }
})().catch(e => { console.error(e.message.slice(0, 1500)); process.exit(1); });

const { P, vessel, ellipse, L } = require('./common');
const { Art, C, rad, lin, spline, tubePoly, rnd, r2 } = L;

function inPoly(pts, x, y) { let c = false; for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) { if (((pts[i][1] > y) !== (pts[j][1] > y)) && (x < (pts[j][0] - pts[i][0]) * (y - pts[i][1]) / (pts[j][1] - pts[i][1]) + pts[i][0])) c = !c; } return c; }
function bbox(pts) { let x0 = 1e9, x1 = -1e9, y0 = 1e9, y1 = -1e9; for (const p of pts) { x0 = Math.min(x0, p[0]); x1 = Math.max(x1, p[0]); y0 = Math.min(y0, p[1]); y1 = Math.max(y1, p[1]); } return [x0, y0, x1, y1]; }
function blot(pts, r) { const n = 9, a = rnd() * 6.28, o = []; for (let i = 0; i < n; i++) { const t = i / n * 6.28, k = r * (0.7 + rnd() * 0.6); o.push([pts[0] + Math.cos(t + a) * k * 1.4, pts[1] + Math.sin(t + a) * k]); } return o; }

// órgano musculoso con volumen, textura, fibras y brillos
function organ(a, g, ctrl, opt) {
  const poly = spline(ctrl, true, 8), bb = bbox(poly), c = L.centroid(poly), R = Math.max(bb[2] - bb[0], bb[3] - bb[1]) / 2;
  const [c0, c1, c2] = opt.cols || ['#C24A48', '#8A2A2E', '#3A1014'];
  for (const [w, al] of [[3, 0.10], [2, 0.16], [1, 0.22]]) a.poly(g, poly, C('#000000', 0), [C('#000000', al), w]);
  a.poly(g, poly, rad(c[0] + (opt.hx || -0.35) * R, c[1] + (opt.hy || -0.45) * R, R * 1.55, [[0, C(c0)], [0.5, C(c1)], [1, C(c2)]]), [C('#2A0508', 0.7), 0.6]);
  // manchas (variación de tejido)
  for (let k = 0; k < (opt.blots || 40); k++) { const x = bb[0] + rnd() * (bb[2] - bb[0]), y = bb[1] + rnd() * (bb[3] - bb[1]); if (!inPoly(poly, x, y)) continue; const dark = rnd() < 0.5; a.poly(g, blot([x, y], 1 + rnd() * 3.2), dark ? C('#3A0A10', 0.16) : C('#E48A80', 0.14), null); }
  // fibras que siguen la curvatura base -> ápex
  const guide = opt.guide; // [start, end] ; fibras curvas entre puntos interpolados
  if (guide) {
    const segs = [];
    for (let k = 0; k < (opt.fibers || 40); k++) {
      const u = rnd(), off = (rnd() - 0.5) * 2 * (opt.spread || 12);
      const pts = []; for (let i = 0; i <= 14; i++) { const t = i / 14; const p = L.lp(guide[0], guide[1], t); const bend = Math.sin(t * Math.PI) * (opt.bend || 5) * (u - 0.5) * 2; pts.push([p[0] + off * (1 - 0.4 * t) + bend, p[1] + (rnd() - 0.5) * 0.4]); }
      const kept = pts.filter(p => inPoly(poly, p[0], p[1]));
      for (let i = 0; i + 1 < kept.length; i++) segs.push(kept[i][0], kept[i][1], kept[i + 1][0], kept[i + 1][1]);
    }
    a.lines(g, segs, C('#2A0508', 0.22), 0.3);
    const seg2 = segs.map((v, i) => v + (i % 2 ? 0.35 : -0.25));
    a.lines(g, seg2, C('#F0A090', 0.16), 0.25);
  }
  if (opt.sheen) for (const sh of opt.sheen) { const pts = spline(sh, false, 8); a.strip(g, pts, C('#FFFFFF', 0.10), 6); a.strip(g, pts, C('#FFFFFF', 0.16), 3.4); a.strip(g, pts, C('#FFFFFF', 0.30), 1.6); a.strip(g, pts, C('#FFFFFF', 0.6), 0.7); }
  return poly;
}
// vaso brillante con extremo cortado
function vess(a, name, cl, w, cols, cap) {
  const d = spline(cl, false, 10);
  a.tube('static', d.map(p => [p[0] + 1.2, p[1] + 1.8]), w * 1.06, [C('#000000', 0.18), C('#000000', 0.22), C('#000000', 0.25), C('#000000', 0.25)], [0, 0]);
  a.tube('static', d, w, cols.map(k => k[0] === '#' ? C(k) : k), [-0.16, -0.2]);
  a.region(name, tubePoly(d, w));
  if (cap !== false) { const e = d[d.length - 1], p = d[d.length - 2]; const ang = Math.atan2(e[1] - p[1], e[0] - p[0]); a.poly('static', ellipse(e[0], e[1], w * 0.5, w * 0.3, ang + Math.PI / 2, 16), rad(e[0], e[1], w * 0.5, [[0, C('#1A0508')], [1, C('#5A1A20')]]), [C('#E08A88', 0.7), 0.5]); }
  return d;
}
const ART = ['#7A2C34', '#B8484F', '#E2837C', '#F6BDB0'];
const VEIN = ['#4E2A5A', '#8A4E7C', '#B47AA0', '#DBA9C6'];

module.exports = function () {
  L.reseed(5);
  const a = new Art();
  // vasos posteriores
  vess(a, 'Vena cava superior', [[25, 0], [25, 14], [27, 30], [30, 44]], 10, VEIN, false);
  vess(a, 'Aorta', [[50, 56], [50, 44], [53, 32], [61, 23], [72, 19], [82, 23], [89, 32], [93, 42]], 12, ART);
  vess(a, 'Aorta', [[61, 24], [58, 15], [56, 7]], 4.6, ART); vess(a, 'Aorta', [[69, 20], [69, 12], [70, 4]], 4.4, ART); vess(a, 'Aorta', [[77, 21], [80, 13], [83, 6]], 4.2, ART);
  // orejuelas y ventrículos
  organ(a, 'static', [[15, 46], [21, 39], [31, 38], [38, 43], [38, 54], [28, 58], [18, 55]], { cols: ['#B4504E', '#8A2E32', '#40141A'], blots: 22, guide: [[18, 40], [34, 56]], fibers: 22, spread: 6, bend: 2 });
  a.route; // (sin flujo interior)
  organ(a, 'RV.wall', [[19, 62], [26, 53], [38, 51], [47, 56], [50, 70], [53, 88], [55, 105], [46, 103], [34, 93], [24, 80], [18, 70]], { cols: ['#B84A48', '#7C1F28', '#26060B'], blots: 70, guide: [[34, 52], [50, 104]], fibers: 55, spread: 14, bend: 4, sheen: [[[24, 66], [28, 58], [36, 54]]] });
  organ(a, 'LV.wall', [[50, 56], [64, 50], [79, 53], [88, 66], [90, 83], [81, 98], [67, 107], [55, 107], [48, 96], [46, 80], [47, 66]], { cols: ['#B23F3E', '#78191F', '#22050A'], blots: 90, guide: [[62, 52], [56, 106]], fibers: 70, spread: 20, bend: 6, sheen: [[[58, 60], [70, 56], [82, 64]], [[80, 74], [82, 84], [76, 94]]] });
  vess(a, 'Aorta', [[50, 60], [50, 48]], 12, ART, false);
  organ(a, 'static', [[58, 45], [66, 41], [73, 45], [72, 53], [65, 56], [58, 53]], { cols: ['#B4504E', '#8A2E32', '#40141A'], blots: 14, guide: [[60, 42], [70, 55]], fibers: 14, spread: 4, bend: 2 });
  // grasa y tejido conectivo pálido en la base
  for (const cap of [[[26, 54], [40, 50], [50, 56], [56, 54], [64, 50], [76, 52], [70, 58], [58, 62], [48, 64], [36, 62]]]) {
    const poly = spline(cap, true, 8); a.poly('static', poly, rad(50, 56, 30, [[0, C('#F4DCA0', 0.5)], [0.6, C('#E8B888', 0.35)], [1, C('#E8B888', 0)]]), null);
    for (let k = 0; k < 40; k++) { const x = 26 + rnd() * 50, y = 50 + rnd() * 14; if (inPoly(poly, x, y)) a.poly('static', blot([x, y], 1 + rnd() * 2), C(rnd() < 0.5 ? '#F6E6B8' : '#E8C98A', 0.32), null); }
  }
  // grasa y coronarias
  const fatLine = (pts, w) => { const d = spline(pts, false, 8); for (let i = 0; i < d.length; i++) { const p = d[i]; if (i % 2 === 0) a.poly('static', ellipse(p[0] + (rnd() - 0.5) * 1.6, p[1] + (rnd() - 0.5) * 1.6, w * (0.35 + rnd() * 0.35), w * (0.3 + rnd() * 0.3), rnd() * 3, 10), rad(p[0], p[1], w * 0.7, [[0, C('#F7E7AE', 0.85)], [1, C('#D9B25E', 0.7)]]), null); } };
  const cor = (name, pts, w, cols, key) => { const d = spline(pts, false, 8); a.tube('static', d, w, cols.map(k => k[0] === '#' ? C(k) : k), [-0.15, -0.2]); a.region(name, tubePoly(d, Math.max(w, 2.6))); return d; };
  const CORA = ['#6A1A22', '#B3313C', '#E25858', '#F49A96'], CORV = ['#3E2A50', '#6E4C86', '#9A78B0', '#C0A0D0'];
  const lad = [[56, 54], [53, 64], [51, 76], [52, 90], [54, 103]];
  const rca = [[47, 55], [39, 54], [29, 58], [21, 65], [18, 76], [22, 88], [32, 98]];
  fatLine(lad, 2.6); fatLine(rca, 3.2);
  cor('Arteria coronaria', lad, 1.5, CORA); cor('Arteria coronaria', rca, 1.6, CORA);
  cor('Arteria coronaria', [[52, 66], [60, 68], [68, 76], [72, 88]], 1.0, CORA); cor('Arteria coronaria', [[51, 80], [58, 84], [63, 94]], 0.9, CORA);
  cor('Arteria coronaria', [[26, 60], [30, 70], [34, 82]], 0.8, CORA);
  cor('Vena cardiaca', [[57, 56], [55, 68], [54, 84], [56, 100]], 1.0, CORV); cor('Vena cardiaca', [[66, 56], [76, 66], [80, 80]], 0.8, CORV);
  // tronco pulmonar delante, con rama cortada
  vess(a, 'Tronco pulmonar', [[43, 62], [45, 52], [52, 44], [63, 40], [76, 40], [88, 44]], 12.5, ['#6A2A44', '#A8586C', '#D48A94', '#F0BCC0']);
  // rutas de pulso en coronarias y etiquetas
  const f = (...v) => v;
  a.route(true, f(56, 54, 0, 53, 64, 0, 51, 76, 0, 52, 90, 0, 54, 103, 0)); a.route(true, f(47, 55, 0, 39, 54, 0, 29, 58, 0, 21, 65, 0, 18, 76, 0, 22, 88, 0, 32, 98, 0));
  a.chambers.push({ id: 'RV', px: 36, py: 82, type: 'v', cav: 0, wall: 0.045, inn: 0 }, { id: 'LV', px: 68, py: 82, type: 'v', cav: 0, wall: 0.06, inn: 0 });
  const lab = (key, text, x, y, left) => a.labels.push({ key, text, x, y, left });
  lab('Vena cava superior', 'V. cava superior', 25, 12, true); lab('Aurícula derecha', 'Orejuela derecha', 24, 48, true); lab('Ventrículo derecho', 'Ventrículo derecho', 30, 74, true);
  lab('Arteria coronaria', 'A. coronaria derecha', 20, 76, true);
  lab('Aorta', 'Arco aórtico', 74, 19, false); lab('Tronco pulmonar', 'Tronco pulmonar', 70, 40, false); lab('Aurícula izquierda', 'Orejuela izquierda', 66, 50, false);
  lab('Ventrículo izquierdo', 'Ventrículo izquierdo', 80, 78, false); lab('Arteria coronaria', 'A. descendente anterior', 53, 92, true);
  a.region('Aurícula derecha', [[15, 46], [21, 39], [31, 38], [38, 43], [38, 54], [28, 58], [18, 55]]);
  a.region('Aurícula izquierda', [[58, 45], [66, 41], [73, 45], [72, 53], [65, 56], [58, 53]]);
  a.region('Ventrículo derecho', [[19, 62], [26, 53], [38, 51], [47, 56], [50, 70], [53, 88], [55, 105], [46, 103], [34, 93], [24, 80], [18, 70]]);
  a.region('Ventrículo izquierdo', [[50, 56], [64, 50], [79, 53], [88, 66], [90, 83], [81, 98], [67, 107], [55, 107], [48, 96], [46, 80], [47, 66]]);
  return a;
};
if (require.main === module) require('fs').writeFileSync('/tmp/exterior.json', module.exports().json());

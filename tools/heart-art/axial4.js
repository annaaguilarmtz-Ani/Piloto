const { P, chamber, papillary, channel, ellipse, roundVessel, L } = require('./common');
const { Art, C, rad, lin, spline, rnd } = L;
module.exports = function () {
  L.reseed(21);
  const a = new Art();
  const V = (name, x, y, dx, dy, gap, semi, extra) => a.valves.push(Object.assign({ name, x, y, dx, dy, gap, semi }, extra));
  // pared torácica, pulmones (con textura), esternón, columna, aorta descendente, esófago
  a.poly('static', ellipse(50, 56, 60, 64, 0, 40), rad(50, 50, 70, [[0, C('#2A1A1A', 0.0)], [1, C('#5A3A30', 0.35)]]), [C('#A08070', 0.3), 0.6]);
  for (const [cx, rx] of [[9, 17], [92, 17]]) {
    a.poly('static', ellipse(cx, 58, rx, 42, 0, 36), rad(cx, 58, 45, [[0, C('#B8C8D8', 0.20)], [1, C('#6A88A8', 0.34)]]), [C('#9FB8D0', 0.35), 0.5]);
    const d = []; for (let i = 0; i < 90; i++) { const x = cx + (rnd() - 0.5) * rx * 1.7, y = 58 + (rnd() - 0.5) * 78; d.push(x, y, x + rnd() * 1.6, y + rnd() * 1.2); }
    a.lines('static', d, C('#DDE8F4', 0.16), 0.3);
  }
  a.poly('static', spline([[34, 4], [50, 1], [66, 4], [66, 8], [50, 6], [34, 8]], true, 6), lin(0, 1, 0, 8, [[0, C('#EADFC6')], [1, C('#A89870')]]), [C('#5A4A30', 0.6), 0.4]);
  a.poly('static', ellipse(50, 106, 10, 6.5, 0, 24), lin(0, 100, 0, 112, [[0, C('#E8DCC0')], [1, C('#9A8A62')]]), [C('#4A3A20', 0.6), 0.4]);
  a.poly('static', spline([[47, 100], [50, 96], [53, 100]], true, 4), C('#0B0F14', 0.9), null);
  roundVessel(a, 'AOd', 'Aorta descendente', 66, 94, 5.2, 5.2, 0, 'art', true);
  a.poly('static', ellipse(53, 90, 3.6, 2.4, 0, 16), rad(53, 90, 4, [[0, C('#D9A0A0')], [1, C('#8E4E52')]]), [C('#4A1A1E', 0.6), 0.3]);
  // cámaras
  chamber(a, 'LA', [[36, 74], [47, 69], [59, 73], [65, 82], [59, 92], [45, 95], [35, 88]], [2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5], { oxy: true, type: 'a', trab: 3, cavK: 0.14, wallK: 0.03, sheen: [0.55, 0.85] });
  chamber(a, 'RA', [[13, 52], [18, 44], [28, 46], [35, 58], [34, 72], [24, 79], [14, 72], [11, 62]], [2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5], { oxy: false, type: 'a', trab: 12, cavK: 0.14, wallK: 0.03, sheen: [0.6, 0.9] });
  chamber(a, 'RV', [[26, 28], [40, 20], [56, 22], [64, 32], [60, 44], [48, 48], [36, 46], [28, 38]], [3, 3, 3, 3, 3, 3, 3, 3], { oxy: false, type: 'v', trab: 22, cavK: 0.16, wallK: 0.05, sheen: [0.0, 0.3] });
  chamber(a, 'LV', [[58, 34], [70, 30], [84, 38], [91, 54], [89, 70], [79, 80], [67, 80], [59, 68], [55, 50]], [6, 9, 10, 10, 10, 9, 8, 7, 6], { oxy: true, type: 'v', trab: 22, cavK: 0.22, wallK: 0.06, sheen: [0.3, 0.6] });
  channel(a, 'static', [[28, 44], [38, 48], [37, 55], [27, 53]], false);
  channel(a, 'static', [[55, 76], [65, 68], [70, 74], [60, 82]], true);
  papillary(a, 'LV.in', [86, 66], [79, 58], 3.4); papillary(a, 'LV.in', [72, 77], [72, 66], 3.0);
  papillary(a, 'RV.in', [50, 24], [48, 34], 2.4);
  V('Válvula tricúspide', 33, 49, 0.25, -1, 6.5, false, { chordae: [[48, 34]] });
  V('Válvula mitral', 62, 74, 0.7, -0.7, 7.5, false, { chordae: [[79, 58], [72, 66]] });
  const f = (...v) => v, cat = (...x) => [].concat(...x);
  const dr = f(20, 70, 4, 25, 60, 1, 33, 49, 1, 40, 38, 1, 50, 32, 2, 40, 27, 2, 30, 25, 0);
  const lr = f(46, 86, 4, 54, 80, 1, 62, 74, 1, 70, 66, 1, 78, 54, 2, 68, 42, 2, 62, 36, 0);
  a.route(false, dr); a.route(false, dr); a.route(true, lr); a.route(true, lr);
  const lab = (key, text, x, y, left) => a.labels.push({ key, text, x, y, left });
  lab('Esternón', 'Esternón', 50, 4, true); lab('Ventrículo derecho', 'Ventrículo derecho', 42, 32, true); lab('Válvula tricúspide', 'V. tricúspide', 33, 49, true);
  lab('Aurícula derecha', 'Aurícula derecha', 22, 64, true); lab('Columna vertebral', 'Columna', 50, 106, true);
  lab('Tabique interventricular', 'Tabique', 60, 46, false); lab('Ventrículo izquierdo', 'Ventrículo izquierdo', 78, 56, false); lab('Válvula mitral', 'V. mitral', 62, 74, false);
  lab('Aurícula izquierda', 'Aurícula izquierda', 52, 84, false); lab('Aorta descendente', 'Aorta descendente', 66, 94, false); lab('Músculo papilar', 'M. papilar', 72, 72, false);
  a.region('Tabique interventricular', [[54, 36], [62, 34], [64, 46], [58, 58], [54, 50]]);
  a.region('Esternón', [[34, 0], [66, 0], [66, 9], [34, 9]]); a.region('Columna vertebral', [[40, 99], [60, 99], [60, 112], [40, 112]]);
  a.region('Músculo papilar', [[68, 62], [76, 62], [76, 80], [68, 80]]);
  return a;
};
if (require.main === module) require('fs').writeFileSync('/tmp/axial4.json', module.exports().json());

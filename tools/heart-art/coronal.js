const { P, chamber, vessel, fat, ring, papillary, channel, L } = require('./common');
const { Art, C, lin, rad, spline, tubePoly } = L;

module.exports = function () {
  L.reseed(7);
  const a = new Art();
  const V = (name, x, y, dx, dy, gap, semi, extra) => a.valves.push(Object.assign({ name, x, y, dx, dy, gap, semi }, extra));
  // contexto: pulmones difuminados
  a.poly('static', spline([[0, 14], [10, 22], [16, 44], [14, 72], [6, 96], [0, 100]], true, 8), rad(4, 56, 40, [[0, C('#8FB2D8', 0.14)], [1, C('#8FB2D8', 0)]]), null);
  a.poly('static', spline([[100, 14], [92, 22], [98, 60], [100, 100]], true, 8), rad(100, 56, 40, [[0, C('#8FB2D8', 0.12)], [1, C('#8FB2D8', 0)]]), null);
  // --- atrás
  vessel(a, 'Venas pulmonares', [[99, 42], [88, 44], [84, 46]], 5, 'pv');
  vessel(a, 'Venas pulmonares', [[99, 55], [90, 53], [86, 52]], 5, 'pv');
  vessel(a, 'Vena cava superior', [[23, 0], [24, 14], [26, 28], [28, 42]], 9, 'vein');
  vessel(a, 'Vena cava inferior', [[23, 112], [24, 96], [26, 82], [28, 70]], 9.5, 'vein');
  chamber(a, 'LA', [[60, 40], [70, 32], [84, 31], [94, 38], [97, 48], [90, 57], [78, 59], [66, 55], [60, 47]], [2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5], { oxy: true, type: 'a', trab: 5, cavK: 0.14, wallK: 0.03, sheen: [0.05, 0.3] });
  // orejuela izquierda
  a.poly('LA.wall', spline([[62, 40], [56, 38], [52, 42], [53, 48], [59, 50]], true, 6), rad(55, 44, 6, [[0, C('#B5504F')], [1, C('#6A1E24')]]), [C('#2A0508', 0.7), 0.5]);
  chamber(a, 'RA', [[15, 52], [17, 42], [26, 36], [37, 37], [43, 45], [44, 58], [42, 70], [32, 75], [21, 70], [15, 62]], [2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5], { oxy: false, type: 'a', trab: 12, cavK: 0.14, wallK: 0.03, sheen: [0.6, 0.85] });
  vessel(a, 'Aorta', [[60, 62], [58, 52], [56, 42], [57, 30], [62, 19], [72, 13], [82, 17], [88, 28], [90, 44], [90, 70], [91, 112]], 10.5, 'art');
  vessel(a, 'Aorta', [[64, 17], [62, 9], [60, 3]], 4.4, 'art');
  vessel(a, 'Aorta', [[71, 13], [71, 7], [71, 1]], 4, 'art');
  vessel(a, 'Aorta', [[80, 15], [83, 9], [86, 3]], 4, 'art');
  // --- ventrículos
  chamber(a, 'RV', [[27, 72], [38, 65], [47, 60], [54, 64], [58, 78], [64, 94], [70, 106], [58, 107], [44, 100], [33, 90], [27, 80]], [3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3], { oxy: false, type: 'v', trab: 22, cavK: 0.16, wallK: 0.05, sheen: [0.55, 0.85] });
  chamber(a, 'LV', [[50, 62], [60, 54], [74, 50], [88, 54], [96, 66], [97, 82], [92, 96], [82, 106], [72, 108], [64, 100], [58, 86], [52, 72]], [6, 8, 8, 9, 10, 10, 9, 8, 5, 5, 6, 6], { oxy: true, type: 'v', trab: 26, cavK: 0.22, wallK: 0.06, sheen: [0.05, 0.3] });
  // aros fibrosos y orificios
  channel(a, 'static', [[64, 56], [82, 55], [83, 63], [65, 64]], true);
  channel(a, 'static', [[29, 67], [42, 64], [44, 70], [32, 72]], false);
  channel(a, 'static', [[54, 60], [64, 58], [66, 52], [56, 52], [53, 56]], true);
  // músculos papilares
  papillary(a, 'LV.in', [66, 99], [68, 81], 3.6); papillary(a, 'LV.in', [78, 98], [76, 80], 3.2);
  papillary(a, 'RV.in', [43, 97], [44, 81], 2.8); papillary(a, 'RV.in', [52, 100], [50, 84], 2.4);
  // grasa y coronarias
  const cor = (name, pts, w) => { fat(a, pts, w * 2.6); a.tube('static', spline(pts, false, 8), w, [C('#7A1E26'), C('#C0343E'), C('#E85A5A'), C('#F08A8A')]); };
  cor('RCA', [[54, 56], [46, 60], [37, 66], [31, 76], [34, 92], [46, 103]], 1.1);
  cor('LAD', [[62, 55], [58, 66], [60, 80], [65, 95], [72, 106]], 1.2);
  cor('LCx', [[63, 55], [72, 53], [84, 52], [93, 58]], 1.0);
  // tronco pulmonar delante
  vessel(a, 'Tronco pulmonar', [[49, 64], [47, 54], [46, 44], [46, 36]], 9, 'pa');
  vessel(a, 'Arteria pulmonar', [[46, 37], [38, 33], [26, 31], [8, 34]], 6, 'pa');
  vessel(a, 'Arteria pulmonar', [[46, 37], [56, 32], [72, 30], [96, 36]], 6, 'pa');
  V('Válvula tricúspide', 35, 67, 0.15, 1, 6.5, false, { chordae: [[44, 81], [50, 84]] });
  V('Válvula mitral', 73, 59.5, 0, 1, 9, false, { chordae: [[68, 81], [76, 80]] });
  V('Válvula pulmonar', 48, 60, 0, -1, 4.5, true);
  V('Válvula aórtica', 60, 60, 0, -1, 4.5, true);
  const f = (...v) => v, cat = (...x) => [].concat(...x);
  const svc = f(23, 0, 0, 24, 14, 0, 26, 28, 0, 27, 44, 4);
  const ivc = f(23, 110, 0, 24, 96, 0, 26, 82, 0, 27, 68, 0, 28, 58, 4);
  const mid = f(31, 57, 1, 35, 67, 1, 40, 78, 1, 50, 90, 2, 52, 78, 2, 48, 62, 2, 47, 50, 0, 46, 40, 0);
  const tr = f(38, 34, 0, 26, 32, 0, 8, 34, 0), tl = f(56, 33, 0, 72, 31, 0, 96, 36, 0);
  const pv1 = f(99, 42, 0, 88, 44, 0, 80, 46, 4), pv2 = f(99, 55, 0, 90, 53, 0, 80, 50, 4);
  const midO = f(76, 52, 1, 73, 60, 1, 72, 68, 1, 73, 84, 1, 73, 90, 2, 66, 74, 2, 60, 64, 2, 59, 54, 0, 58, 46, 0, 57, 34, 0, 62, 21, 0, 72, 15, 0, 82, 19, 0, 88, 30, 0, 90, 46, 0, 90, 70, 0, 91, 108, 0);
  a.route(false, cat(svc, mid, tr)); a.route(false, cat(svc, mid, tl)); a.route(false, cat(ivc, mid, tr)); a.route(false, cat(ivc, mid, tl));
  a.route(true, cat(pv1, midO)); a.route(true, cat(pv2, midO));
  const lab = (key, text, x, y, left) => a.labels.push({ key, text, x, y, left });
  lab('Vena cava superior', 'V. cava superior', 24, 18, true); lab('Tronco pulmonar', 'Tronco pulmonar', 46, 46, true);
  lab('Aurícula derecha', 'Aurícula derecha', 24, 52, true); lab('Válvula tricúspide', 'V. tricúspide', 35, 67, true);
  lab('Ventrículo derecho', 'Ventrículo derecho', 40, 88, true); lab('Vena cava inferior', 'V. cava inferior', 24, 100, true);
  lab('Aorta', 'Arco aórtico', 80, 15, false); lab('Arteria pulmonar', 'A. pulmonar', 86, 33, false);
  lab('Venas pulmonares', 'Venas pulmonares', 95, 44, false); lab('Aurícula izquierda', 'Aurícula izquierda', 80, 44, false);
  lab('Válvula mitral', 'V. mitral', 73, 60, false); lab('Válvula aórtica', 'V. aórtica', 60, 60, false);
  lab('Ventrículo izquierdo', 'Ventrículo izquierdo', 84, 84, false); lab('Aorta descendente', 'Aorta descendente', 90, 100, false);
  lab('Músculo papilar', 'M. papilar / cuerdas', 68, 90, false); lab('Tabique interventricular', 'Tabique', 56, 88, true);
  a.region('Tabique interventricular', [[53, 66], [58, 63], [62, 80], [68, 100], [63, 103], [57, 90]].flat ? [[53, 66], [58, 63], [62, 80], [68, 100], [63, 103], [57, 90]] : []);
  a.region('Músculo papilar', [[65, 80], [70, 80], [70, 98], [65, 99]]); a.region('Músculo papilar', [[75, 79], [79, 79], [79, 98], [75, 98]]);
  const cd = (pts, start, dur) => a.conds.push({ pts, start, dur });
  cd(f(34, 42, 38, 52, 43, 62, 45, 66), 0, 0.08); cd(f(34, 42, 46, 42, 58, 44, 68, 44, 78, 46), 0, 0.09);
  cd(f(45, 66, 48, 70, 53, 76), 0.15, 0.03); cd(f(53, 76, 56, 90, 60, 101), 0.18, 0.04); cd(f(55, 76, 61, 90, 67, 101, 80, 98, 90, 84), 0.18, 0.06);
  a.nodes.push({ key: 'Nodo sinusal', x: 34, y: 42, t0: 0 }, { key: 'Nodo auriculoventricular', x: 45, y: 66, t0: 0.08 }, { key: 'Haz de His y Purkinje', x: 53, y: 76, t0: 0.16 });
  return a;
};
if (require.main === module) require('fs').writeFileSync('/tmp/coronal.json', module.exports().json());

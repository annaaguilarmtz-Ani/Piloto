const { P, ellipse, roundVessel, L } = require('./common');
const { Art, C, rad, lin, spline, rnd, tubePoly } = L;
module.exports = function () {
  L.reseed(31);
  const a = new Art();
  a.poly('static', ellipse(50, 56, 60, 64, 0, 40), rad(50, 50, 70, [[0, C('#2A1A1A', 0.0)], [1, C('#5A3A30', 0.35)]]), [C('#A08070', 0.3), 0.6]);
  for (const [cx, rx] of [[13, 19], [88, 19]]) {
    a.poly('static', ellipse(cx, 58, rx, 44, 0, 36), rad(cx, 58, 46, [[0, C('#B8C8D8', 0.20)], [1, C('#6A88A8', 0.34)]]), [C('#9FB8D0', 0.35), 0.5]);
    const d = []; for (let i = 0; i < 120; i++) { const x = cx + (rnd() - 0.5) * rx * 1.8, y = 58 + (rnd() - 0.5) * 84; d.push(x, y, x + rnd() * 1.6, y + rnd() * 1.2); }
    a.lines('static', d, C('#DDE8F4', 0.16), 0.3);
  }
  a.poly('static', spline([[34, 4], [50, 1], [66, 4], [66, 8], [50, 6], [34, 8]], true, 6), lin(0, 1, 0, 8, [[0, C('#EADFC6')], [1, C('#A89870')]]), [C('#5A4A30', 0.6), 0.4]);
  a.poly('static', ellipse(50, 98, 11, 7.5, 0, 24), lin(0, 90, 0, 106, [[0, C('#E8DCC0')], [1, C('#9A8A62')]]), [C('#4A3A20', 0.6), 0.4]);
  a.poly('static', ellipse(50, 108, 4, 3, 0, 12), C('#0B0F14', 0.9), null);
  // tráquea y bronquios principales (aire)
  const air = (pts, w) => { const d = spline(pts, false, 8); a.tube('static', d, w, [C('#3A2A2A'), C('#0E1216'), C('#05070A'), C('#05070A')]); };
  air([[50, 58], [50, 68], [50, 74]], 8);
  air([[50, 72], [40, 78], [28, 82]], 5); air([[50, 72], [60, 78], [74, 82]], 5);
  a.region('Tráquea', tubePoly(spline([[50, 58], [50, 68], [50, 74]], false, 8), 8));
  // estructuras vasculares
  roundVessel(a, 'RPA', 'Arteria pulmonar', 34, 60, 11, 4.2, -0.35, 'pa', false);
  roundVessel(a, 'LPA', 'Arteria pulmonar', 68, 62, 10, 4.2, 0.35, 'pa', false);
  roundVessel(a, 'AOd', 'Aorta descendente', 66, 82, 6, 6, 0, 'art', true);
  roundVessel(a, 'SVC', 'Vena cava superior', 28, 42, 7, 7, 0, 'vein', false);
  roundVessel(a, 'AOa', 'Aorta', 46, 44, 11, 11, 0, 'art', false);
  roundVessel(a, 'PT', 'Tronco pulmonar', 62, 30, 13, 8.5, 0.35, 'pa', false);
  a.poly('static', ellipse(56, 76, 3.6, 2.4, 0, 16), rad(56, 76, 4, [[0, C('#D9A0A0')], [1, C('#8E4E52')]]), [C('#4A1A1E', 0.6), 0.3]);
  const lab = (key, text, x, y, left) => a.labels.push({ key, text, x, y, left });
  lab('Esternón', 'Esternón', 50, 4, true); lab('Vena cava superior', 'V. cava superior', 26, 42, true); lab('Aorta', 'Aorta ascendente', 40, 46, true);
  lab('Arteria pulmonar', 'A. pulmonar der.', 30, 60, true); lab('Tráquea', 'Tráquea', 50, 62, true); lab('Columna vertebral', 'Columna', 50, 100, true);
  lab('Tronco pulmonar', 'Tronco pulmonar', 66, 28, false); lab('Arteria pulmonar', 'A. pulmonar izq.', 70, 62, false); lab('Aorta descendente', 'Aorta descendente', 68, 84, false);
  a.region('Esternón', [[34, 0], [66, 0], [66, 9], [34, 9]]); a.region('Columna vertebral', [[38, 90], [62, 90], [62, 108], [38, 108]]);
  return a;
};
if (require.main === module) require('fs').writeFileSync('/tmp/axialhigh.json', module.exports().json());

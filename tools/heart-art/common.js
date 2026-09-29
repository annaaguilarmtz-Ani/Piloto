const L = require('./lib');
const { spline, inner, tubePoly, C, lin, rad, rnd, r2 } = L;

// Paleta
const P = {
  wall: ['#B5504F', '#8E2F33', '#5A171D'],
  oxy: ['#D2343E', '#9C1B26', '#560A12'],
  deoxy: ['#5A70CC', '#34479A', '#161F55'],
  art: '#C46A70', vein: '#7C7AB8', artD: '#8F3E46', veinD: '#4E4C8C'
};

// Cámara cardiaca: pared (miocardio) + fibras + cavidad + trabéculas
function chamber(art, id, ctrl, thick, opts) {
  const o = Object.assign({ oxy: true, type: 'v', trab: 0, fibers: 26, cavK: 0.2, wallK: 0.05, innK: 0.1, seg: 8, hi: [-0.4, -0.5] }, opts);
  const { outer, inn, c } = inner(ctrl, thick, o.seg);
  const R = Math.max(...outer.map(p => Math.hypot(p[0] - c[0], p[1] - c[1])));
  const wallFill = rad(c[0] + o.hi[0] * R, c[1] + o.hi[1] * R, R * 1.5, [[0, C(P.wall[0])], [0.55, C(P.wall[1])], [1, C(P.wall[2])]]);
  for (const [w, al] of [[3.2, 0.10], [2.2, 0.14], [1.2, 0.2]]) art.poly(id + '.wall', outer, C('#000000', 0), [C('#000000', al), w]);
  art.poly(id + '.wall', outer, wallFill, [C('#2A0508', 0.7), 0.6]);
  if (o.sheen) { const n = outer.length, s0 = Math.floor(o.sheen[0] * n), s1 = Math.floor(o.sheen[1] * n); const pts = []; for (let i = s0; i <= s1; i++) { const p = outer[i % n]; const dx = c[0] - p[0], dy = c[1] - p[1], d = Math.hypot(dx, dy) || 1; pts.push([p[0] + dx / d * 1.3, p[1] + dy / d * 1.3]); } art.strip(id + '.wall', pts, C('#FFFFFF', 0.22), 1.1); art.strip(id + '.wall', pts, C('#FFFFFF', 0.16), 2.4); }
  // fibras musculares siguiendo el contorno
  const segs = [];
  for (let k = 0; k < o.fibers; k++) {
    const u0 = Math.floor(rnd() * outer.length), len = 4 + Math.floor(rnd() * 8), m = 0.15 + rnd() * 0.7;
    for (let j = 0; j < len; j++) {
      const a = (u0 + j) % outer.length, b = (u0 + j + 1) % outer.length;
      const pa = [outer[a][0] * (1 - m) + inn[a][0] * m, outer[a][1] * (1 - m) + inn[a][1] * m];
      const pb = [outer[b][0] * (1 - m) + inn[b][0] * m, outer[b][1] * (1 - m) + inn[b][1] * m];
      segs.push(pa[0], pa[1], pb[0], pb[1]);
    }
  }
  art.lines(id + '.wall', segs, C('#E8867C', 0.16), 0.28);
  const segs2 = [];
  for (let k = 0; k < o.fibers; k++) {
    const u0 = Math.floor(rnd() * outer.length), len = 3 + Math.floor(rnd() * 7), m = 0.1 + rnd() * 0.8;
    for (let j = 0; j < len; j++) {
      const a = (u0 + j) % outer.length, b = (u0 + j + 1) % outer.length;
      segs2.push(outer[a][0] * (1 - m) + inn[a][0] * m, outer[a][1] * (1 - m) + inn[a][1] * m, outer[b][0] * (1 - m) + inn[b][0] * m, outer[b][1] * (1 - m) + inn[b][1] * m);
    }
  }
  art.lines(id + '.wall', segs2, C('#2A0508', 0.22), 0.3);
  const bl = o.oxy ? P.oxy : P.deoxy;
  const cavFill = rad(c[0] + o.hi[0] * R * 0.5, c[1] + o.hi[1] * R * 0.5, R * 0.9, [[0, C(bl[0])], [0.5, C(bl[1])], [1, C(bl[2])]]);
  art.poly(id + '.cav', inn, cavFill, [C('#F0A8A8', 0.45), 0.5]);
  // trabéculas / músculos pectíneos
  for (let k = 0; k < o.trab; k++) {
    let i = Math.floor(rnd() * inn.length);
    for (let tries = 0; tries < 6 && o.type === 'v' && inn[i][1] < c[1] - R * 0.1; tries++) i = Math.floor(rnd() * inn.length);
    const p = inn[i];
    const dx = c[0] - p[0], dy = c[1] - p[1], d = Math.hypot(dx, dy) || 1;
    const off = 0.4 + rnd() * 1.0, rr = 0.45 + rnd() * 0.75;
    const cx = p[0] + dx / d * off, cy = p[1] + dy / d * off, ang = Math.atan2(dy, dx);
    const pts = []; for (let a = 0; a < 10; a++) { const t = a / 10 * Math.PI * 2; pts.push([cx + Math.cos(t) * rr * 1.6 * Math.cos(ang) - Math.sin(t) * rr * Math.sin(ang), cy + Math.cos(t) * rr * 1.6 * Math.sin(ang) + Math.sin(t) * rr * Math.cos(ang)]); }
    art.poly(id + '.cav', pts, C('#7A2429', 0.85), [C('#2A0508', 0.5), 0.25]);
  }
  art.wallRegion('Miocardio', outer);
  art.chambers.push({ id, px: r2(c[0]), py: r2(c[1]), type: o.type, cav: o.cavK, wall: o.wallK, inn: o.innK });
  art.region(id === 'RA' ? 'Aurícula derecha' : id === 'RV' ? 'Ventrículo derecho' : id === 'LA' ? 'Aurícula izquierda' : 'Ventrículo izquierdo', inn);
  return { outer, inn, c };
}

// Vaso: pared + luz con volumen
function vessel(art, name, cl, w, kind, g) {
  if (typeof w === 'function') w = w(0.5);
  const dense = spline(cl, false, 10);
  const cols = kind === 'art' ? [C(P.artD), C(P.art), C(P.oxy[1]), C(P.oxy[0])]
    : kind === 'pa' ? [C(P.veinD), C(P.vein), C(P.deoxy[1]), C(P.deoxy[0])]
    : kind === 'pv' ? [C(P.veinD), C(P.vein), C(P.oxy[1]), C(P.oxy[0])]
    : [C(P.veinD), C(P.vein), C(P.deoxy[1]), C(P.deoxy[0])];
  art.tube(g || 'static', dense, w, cols);
  art.region(name, tubePoly(dense, typeof w === 'function' ? (t) => w(t) : w));
  return dense;
}

const fat = (art, pts, w) => art.tube('static', spline(pts, false, 8), w, [C('#C9A45A', 0.0), C('#D8B86A', 0.22), C('#E8D08A', 0.25), C('#F2E2A8', 0.14)]);
const ring = (art, a, b, w) => art.tube('static', spline([a, [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2 + 0.0], b], false, 6), w || 1.0, [C('#6A4A34', 0.8), C('#A88C64', 0.8), C('#D0BF9A', 0.7), C('#EFE4C8', 0.5)], [-0.1, -0.2]);
const papillary = (art, g, base, tip, w) => art.tube(g, spline([base, [(base[0] + tip[0]) / 2 + 0.6, (base[1] + tip[1]) / 2], tip], false, 6), w, [C('#3E0C11'), C('#7E2A2E'), C('#B04A4A'), C('#D67A70')]);
// conducto de sangre entre dos cavidades (orificio valvular)
const channel = (art, g, pts, oxy) => { const bl = oxy ? P.oxy : P.deoxy; const cc = L.centroid(pts); pts = spline(pts, true, 6); art.poly(g, pts, rad(cc[0], cc[1], 8, [[0, C(bl[0])], [0.6, C(bl[1])], [1, C(bl[2])]]), null); };
function ellipse(cx, cy, rx, ry, rot, n) { n = n || 28; const o = [], cr = Math.cos(rot || 0), sr = Math.sin(rot || 0); for (let i = 0; i < n; i++) { const t = i / n * Math.PI * 2, x = Math.cos(t) * rx, y = Math.sin(t) * ry; o.push([cx + x * cr - y * sr, cy + x * sr + y * cr]); } return o; }
// vaso en corte transversal (con pulso)
function roundVessel(art, id, name, cx, cy, rx, ry, rot, kind, out) {
  const cols = kind === 'art' ? [P.artD, P.art, P.oxy] : kind === 'pv' ? [P.veinD, P.vein, P.oxy] : [P.veinD, P.vein, P.deoxy];
  const lum = kind === 'art' || kind === 'pv' ? P.oxy : P.deoxy;
  art.poly(id + '.wall', ellipse(cx, cy, rx + 1.6, ry + 1.6, rot), rad(cx - rx * 0.3, cy - ry * 0.3, Math.max(rx, ry) * 2, [[0, C(cols[1])], [1, C(cols[0])]]), [C('#2A0508', 0.5), 0.4]);
  art.poly(id + '.cav', ellipse(cx, cy, rx, ry, rot), rad(cx - rx * 0.2, cy - ry * 0.2, Math.max(rx, ry) * 1.1, [[0, C(lum[0])], [0.6, C(lum[1])], [1, C(lum[2])]]), [C('#F0A8A8', 0.35), 0.4]);
  art.chambers.push({ id, px: cx, py: cy, type: 'p', cav: -0.06, wall: -0.03, inn: 0 });
  art.region(name, ellipse(cx, cy, rx + 1.6, ry + 1.6, rot));
  art.flows.push({ name, cx, cy, rx, ry, rot: rot || 0, oxy: kind !== 'vein' && kind !== 'pa', out: !!out });
}
module.exports = { ellipse, roundVessel, P, chamber, vessel, fat, ring, papillary, channel, L };

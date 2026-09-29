// Utilidades para generar el arte anatómico (coordenadas en un lienzo de 100 x 112 unidades).
let seed = 12345;
function rnd() { seed |= 0; seed = seed + 0x6D2B79F5 | 0; let t = Math.imul(seed ^ seed >>> 15, 1 | seed); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }
function reseed(s) { seed = s; }
const r2 = (v) => Math.round(v * 100) / 100;

// Catmull-Rom -> polilínea densa. pts: [[x,y],...]
function spline(pts, closed, seg = 10) {
  const n = pts.length, out = [];
  const get = (i) => closed ? pts[(i + n) % n] : pts[Math.max(0, Math.min(n - 1, i))];
  const last = closed ? n : n - 1;
  for (let i = 0; i < last; i++) {
    const p0 = get(i - 1), p1 = get(i), p2 = get(i + 1), p3 = get(i + 2);
    for (let s = 0; s < seg; s++) {
      const t = s / seg, t2 = t * t, t3 = t2 * t;
      out.push([
        0.5 * ((2 * p1[0]) + (-p0[0] + p2[0]) * t + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3),
        0.5 * ((2 * p1[1]) + (-p0[1] + p2[1]) * t + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3)]);
    }
  }
  if (!closed) out.push(pts[n - 1]);
  return out;
}
const flat = (pts) => { const o = []; for (const p of pts) { o.push(r2(p[0]), r2(p[1])); } return o; };
function centroid(pts) { let x = 0, y = 0; for (const p of pts) { x += p[0]; y += p[1]; } return [x / pts.length, y / pts.length]; }
const lerp = (a, b, t) => a + (b - a) * t;
const lp = (a, b, t) => [lerp(a[0], b[0], t), lerp(a[1], b[1], t)];

// Contorno interior a partir del exterior con grosor de pared variable (thick por punto de control).
function inner(outerCtrl, thick, seg = 10, closed = true) {
  const outer = spline(outerCtrl, closed, seg);
  const th = spline(thick.map((t, i) => [i, t]), closed, seg).map(p => p[1]);
  const c = centroid(outer);
  const inn = outer.map((p, i) => {
    const dx = p[0] - c[0], dy = p[1] - c[1], d = Math.hypot(dx, dy) || 1;
    const k = Math.max(0.15, 1 - (th[i % th.length] || 2) / d);
    return [c[0] + dx * k, c[1] + dy * k];
  });
  return { outer, inn, c };
}

// Tubo: polígono alrededor de una línea central con ancho w (número o función t->ancho)
function tubePoly(cl, w) {
  const L = [], R = [];
  for (let i = 0; i < cl.length; i++) {
    const a = cl[Math.max(0, i - 1)], b = cl[Math.min(cl.length - 1, i + 1)];
    let dx = b[0] - a[0], dy = b[1] - a[1]; const d = Math.hypot(dx, dy) || 1; dx /= d; dy /= d;
    const ww = (typeof w === 'function' ? w(i / (cl.length - 1)) : w) / 2;
    L.push([cl[i][0] - dy * ww, cl[i][1] + dx * ww]); R.push([cl[i][0] + dy * ww, cl[i][1] - dx * ww]);
  }
  return L.concat(R.reverse());
}

// Colores: 'AARRGGBB'
const C = (hex, a) => { hex = hex.replace('#', ''); const al = a === undefined ? 'FF' : Math.round(a * 255).toString(16).padStart(2, '0').toUpperCase(); return (al + hex).toUpperCase(); };
const lin = (x0, y0, x1, y1, stops) => ({ lin: [x0, y0, x1, y1, stops] });
const rad = (cx, cy, r, stops) => ({ rad: [cx, cy, r, stops] });

class Art {
  constructor() { this.layers = []; this.regions = []; this.valves = []; this.routes = []; this.labels = []; this.conds = []; this.nodes = []; this.chambers = []; this.walls = []; this.flows = []; }
  poly(g, pts, fill, stroke, extra) { this.layers.push(Object.assign({ t: 'poly', g, pts: flat(pts), fill, stroke }, extra || {})); }
  tube(g, cl, w, cols, off) { this.layers.push({ t: 'tube', g, pts: flat(cl), w, cols, off: off || [-0.12, -0.12] }); }
  lines(g, segs, c, w) { this.layers.push({ t: 'lines', g, pts: segs.map(v => r2(v)), c, w }); }
  strip(g, pts, c, w) { this.layers.push({ t: 'strip', g, pts: flat(pts), c, w }); }
  wallRegion(name, pts) { this.walls.push({ name, pts: flat(pts) }); }
  route(oxy, pts) { // suaviza la ruta conservando el modo de cada tramo
    const n = pts.length / 3, ctrl = [], modes = [];
    for (let i = 0; i < n; i++) { ctrl.push([pts[i * 3], pts[i * 3 + 1]]); modes.push(pts[i * 3 + 2]); }
    const seg = 5, dense = spline(ctrl, false, seg), out = [];
    for (let i = 0; i < dense.length; i++) { const k = Math.min(n - 1, Math.floor(i / seg)); out.push(r2(dense[i][0]), r2(dense[i][1]), modes[k]); }
    this.routes.push({ oxy, pts: out });
  }
  region(name, pts) { this.regions.push({ name, pts: flat(pts) }); }
  json() { return JSON.stringify({ layers: this.layers, regions: this.regions, valves: this.valves, routes: this.routes, labels: this.labels, conds: this.conds, nodes: this.nodes, chambers: this.chambers, walls: this.walls, flows: this.flows }); }
}
module.exports = { rnd, reseed, spline, flat, centroid, lerp, lp, inner, tubePoly, C, lin, rad, Art, r2 };

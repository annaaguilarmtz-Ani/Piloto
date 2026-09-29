// Renderizador de referencia (canvas 2D). Kotlin replica esta misma semántica.
function argb(h) { const a = parseInt(h.slice(0, 2), 16) / 255, r = parseInt(h.slice(2, 4), 16), g = parseInt(h.slice(4, 6), 16), b = parseInt(h.slice(6, 8), 16); return `rgba(${r},${g},${b},${a.toFixed(3)})`; }
function mkFill(ctx, f) {
  if (typeof f === 'string') return argb(f);
  if (f.lin) { const [x0, y0, x1, y1, st] = f.lin; const g = ctx.createLinearGradient(x0, y0, x1, y1); st.forEach(([o, c]) => g.addColorStop(o, argb(c))); return g; }
  const [cx, cy, r, st] = f.rad; const g = ctx.createRadialGradient(cx, cy, 0, cx, cy, r); st.forEach(([o, c]) => g.addColorStop(o, argb(c))); return g;
}
function pathOf(ctx, pts, close) { ctx.beginPath(); ctx.moveTo(pts[0], pts[1]); for (let i = 2; i < pts.length; i += 2) ctx.lineTo(pts[i], pts[i + 1]); if (close) ctx.closePath(); }
function render(ctx, art, s, opts) {
  opts = opts || {};
  const chm = {}; art.chambers.forEach(c => chm[c.id] = c);
  ctx.save(); ctx.scale(s, s); ctx.lineJoin = 'round'; ctx.lineCap = 'round';
  for (const L of art.layers) {
    ctx.save();
    const dot = L.g.indexOf('.');
    if (dot > 0) {
      const c = chm[L.g.slice(0, dot)];
      if (c) {
        const kind = L.g.slice(dot + 1);
        const sys = c.type === 'v' ? (opts.sV || 0) : c.type === 'p' ? (opts.pulse || 0) : (opts.sA || 0);
        const amt = (kind === 'cav' ? c.cav : kind === 'in' ? c.inn : c.wall) * sys;
        ctx.translate(c.px, c.py); ctx.scale(1 - amt, 1 - amt); ctx.translate(-c.px, -c.py);
      }
    }
    if (L.t === 'poly') {
      pathOf(ctx, L.pts, true); ctx.fillStyle = mkFill(ctx, L.fill); ctx.fill();
      if (L.stroke) { ctx.strokeStyle = argb(L.stroke[0]); ctx.lineWidth = L.stroke[1]; ctx.stroke(); }
    } else if (L.t === 'tube') {
      const ratios = [1, 0.78, 0.5, 0.2];
      L.cols.forEach((c, i) => { ctx.save(); ctx.translate(L.off[0] * L.w * (1 - ratios[i]), L.off[1] * L.w * (1 - ratios[i])); pathOf(ctx, L.pts, false); ctx.strokeStyle = argb(c); ctx.lineWidth = L.w * ratios[i]; ctx.stroke(); ctx.restore(); });
    } else if (L.t === 'lines') {
      ctx.strokeStyle = argb(L.c); ctx.lineWidth = L.w; ctx.beginPath();
      for (let i = 0; i + 3 < L.pts.length; i += 4) { ctx.moveTo(L.pts[i], L.pts[i + 1]); ctx.lineTo(L.pts[i + 2], L.pts[i + 3]); }
      ctx.stroke();
    } else if (L.t === 'strip') { // polilínea abierta con trazo
      pathOf(ctx, L.pts, false); ctx.strokeStyle = argb(L.c); ctx.lineWidth = L.w; ctx.stroke();
    }
    ctx.restore();
  }
  if (opts.debug) {
    ctx.lineWidth = 0.4;
    art.routes.forEach(r => { ctx.strokeStyle = r.oxy ? 'yellow' : 'cyan'; ctx.beginPath(); for (let i = 0; i < r.pts.length; i += 3) i ? ctx.lineTo(r.pts[i], r.pts[i + 1]) : ctx.moveTo(r.pts[i], r.pts[i + 1]); ctx.stroke(); });
    art.valves.forEach(v => { ctx.fillStyle = 'lime'; ctx.beginPath(); ctx.arc(v.x, v.y, 1, 0, 7); ctx.fill(); });
  }
  ctx.restore();
}
if (typeof module !== 'undefined') module.exports = { render };

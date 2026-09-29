// node render.js <sprite> <out.png> [w h rotx roty rotz seed]
const { chromium } = require(require('child_process').execSync('npm root -g').toString().trim() + '/playwright');
const fs = require('fs');
const prelude = require('./prelude.js');
const sprites = require('./sprites.js');
async function open(w, h) {
  const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome', args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const p = await b.newPage({ viewport: { width: w, height: h } });
  await p.setContent(`<canvas id=c width=${w} height=${h}></canvas>`);
  return { b, p };
}
async function draw(p, frag, w, h, rot, half, seed, tint) {
  return p.evaluate(([frag, w, h, rot, half, seed, tint]) => {
    const c = document.getElementById('c'); const gl = c.getContext('webgl2', { premultipliedAlpha: true, preserveDrawingBuffer: true });
    const mk = (t, s) => { const sh = gl.createShader(t); gl.shaderSource(sh, s); gl.compileShader(sh); if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(sh)); return sh; };
    const pr = gl.createProgram(); gl.attachShader(pr, mk(gl.VERTEX_SHADER, '#version 300 es\nin vec2 a; void main(){ gl_Position=vec4(a,0.,1.); }')); gl.attachShader(pr, mk(gl.FRAGMENT_SHADER, frag));
    gl.linkProgram(pr); if (!gl.getProgramParameter(pr, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(pr)); gl.useProgram(pr);
    const buf = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buf); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
    const loc = gl.getAttribLocation(pr, 'a'); gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
    gl.uniform2f(gl.getUniformLocation(pr, 'uRes'), w, h); gl.uniform3f(gl.getUniformLocation(pr, 'uRot'), ...rot);
    gl.uniform1f(gl.getUniformLocation(pr, 'uHalf'), half); gl.uniform1f(gl.getUniformLocation(pr, 'uSeed'), seed); gl.uniform3f(gl.getUniformLocation(pr, 'uTint'), ...tint);
    gl.viewport(0, 0, w, h); gl.clearColor(0, 0, 0, 0); gl.clear(gl.COLOR_BUFFER_BIT); gl.drawArrays(gl.TRIANGLES, 0, 3); gl.finish();
    return c.toDataURL('image/png');
  }, [frag, w, h, rot, half, seed, tint]);
}
module.exports = { open, draw };
if (require.main === module) (async () => {
  const [name, out, W, H, rx, ry, rz, seed] = process.argv.slice(2);
  const s = sprites[name]; if (!s) throw new Error('sprite desconocido ' + name);
  const w = +W || s.w || 384, h = +H || s.h || 384;
  const { b, p } = await open(w, h);
  const url = await draw(p, prelude + s.glsl, w, h, [+(rx ?? s.rot[0]), +(ry ?? s.rot[1]), +(rz ?? s.rot[2])], s.half, +seed || 0, s.tint || [0.6, 0.2, 0.1]);
  fs.writeFileSync(out, Buffer.from(url.split(',')[1], 'base64')); await b.close();
})().catch(e => { console.error(e.message.slice(0, 2000)); process.exit(1); });

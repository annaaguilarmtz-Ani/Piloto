// Genera los fotogramas del corazón exterior (5 giros x 16 fases del latido) en app/src/main/assets/heart3d
const { chromium } = require(require('child_process').execSync('npm root -g').toString().trim() + '/playwright');
const fs = require('fs');
const frag = require('./shader.js');
const OUT = __dirname + '/../../app/src/main/assets/heart3d';
fs.mkdirSync(OUT, { recursive: true });
const W = +process.env.W || 800, H = +process.env.H || 889, OW = 640, OH = 711;
const yaws = (process.env.YAWS || '-0.6,-0.3,0,0.3,0.6').split(',').map(Number);
const N = +process.env.NF || 16;
const bump = (p, a, b) => (p >= a && p <= b) ? Math.sin(Math.PI * (p - a) / (b - a)) : 0;
(async () => {
  const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome', args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const p = await b.newPage({ viewport: { width: W, height: H } });
  await p.setContent(`<canvas id=c width=${W} height=${H}></canvas><canvas id=o width=${OW} height=${OH}></canvas>`);
  await p.evaluate(([frag, W, H]) => {
    const c = document.getElementById('c'); const gl = c.getContext('webgl2', { premultipliedAlpha: true, preserveDrawingBuffer: true });
    const mk = (t, s) => { const sh = gl.createShader(t); gl.shaderSource(sh, s); gl.compileShader(sh); if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(sh)); return sh; };
    const pr = gl.createProgram(); gl.attachShader(pr, mk(gl.VERTEX_SHADER, '#version 300 es\nin vec2 a; void main(){ gl_Position=vec4(a,0.,1.); }')); gl.attachShader(pr, mk(gl.FRAGMENT_SHADER, frag));
    gl.linkProgram(pr); gl.useProgram(pr);
    const buf = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buf); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
    const loc = gl.getAttribLocation(pr, 'a'); gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
    gl.uniform2f(gl.getUniformLocation(pr, 'uRes'), W, H);
    window.R = (yaw, beat, pulse) => {
      gl.uniform1f(gl.getUniformLocation(pr, 'uYaw'), yaw); gl.uniform1f(gl.getUniformLocation(pr, 'uBeat'), beat); gl.uniform1f(gl.getUniformLocation(pr, 'uPulse'), pulse);
      gl.viewport(0, 0, W, H); gl.drawArrays(gl.TRIANGLES, 0, 3); gl.finish();
      const o = document.getElementById('o'), ctx = o.getContext('2d'); ctx.fillStyle = '#000'; ctx.fillRect(0, 0, o.width, o.height); ctx.imageSmoothingQuality = 'high'; ctx.drawImage(c, 0, 0, o.width, o.height);
      return o.toDataURL('image/webp', 0.86);
    };
  }, [frag, W, H]);
  for (let yi = 0; yi < yaws.length; yi++) for (let f = 0; f < N; f++) {
    const ph = f / N, beat = bump(ph, 0.18, 0.48), pulse = bump(ph - 0.05 < 0 ? ph - 0.05 + 1 : ph - 0.05, 0.18, 0.48);
    const t0 = Date.now();
    const url = await p.evaluate(([y, be, pu]) => window.R(y, be, pu), [yaws[yi], beat, pulse]);
    fs.writeFileSync(`${OUT}/y${yi}_f${f}.webp`, Buffer.from(url.split(',')[1], 'base64'));
    console.log(`y${yi} f${f}`, Date.now() - t0, 'ms');
  }
  await b.close();
})().catch(e => { console.error(e.message.slice(0, 1500)); process.exit(1); });

const { chromium } = require(require('child_process').execSync('npm root -g').toString().trim() + '/playwright');
const fs = require('fs');
const frag = require('./shader.js');
// uso: node render.js out.png yaw beat pulse [w h]
(async () => {
  const [out, yaw, beat, pulse, W, H] = process.argv.slice(2);
  const w = +W || 360, h = +H || 400;
  const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome', args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const p = await b.newPage({ viewport: { width: w, height: h } });
  p.on('console', m => console.log('page:', m.text()));
  await p.setContent(`<body style="margin:0;background:#101418"><canvas id=c width=${w} height=${h}></canvas></body>`);
  const res = await p.evaluate(([frag, yaw, beat, pulse, w, h]) => {
    const c = document.getElementById('c'); const gl = c.getContext('webgl2', { premultipliedAlpha: true, preserveDrawingBuffer: true });
    const mk = (t, s) => { const sh = gl.createShader(t); gl.shaderSource(sh, s); gl.compileShader(sh); if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(sh)); return sh; };
    const pr = gl.createProgram();
    gl.attachShader(pr, mk(gl.VERTEX_SHADER, '#version 300 es\nin vec2 a; void main(){ gl_Position=vec4(a,0.,1.); }'));
    gl.attachShader(pr, mk(gl.FRAGMENT_SHADER, frag));
    gl.linkProgram(pr); if (!gl.getProgramParameter(pr, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(pr));
    gl.useProgram(pr);
    const buf = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buf); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
    const loc = gl.getAttribLocation(pr, 'a'); gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
    gl.uniform2f(gl.getUniformLocation(pr, 'uRes'), w, h); gl.uniform1f(gl.getUniformLocation(pr, 'uYaw'), +yaw);
    gl.uniform1f(gl.getUniformLocation(pr, 'uBeat'), +beat); gl.uniform1f(gl.getUniformLocation(pr, 'uPulse'), +pulse);
    const t0 = performance.now(); gl.viewport(0, 0, w, h); gl.drawArrays(gl.TRIANGLES, 0, 3); gl.finish();
    return (performance.now() - t0).toFixed(0) + 'ms';
  }, [frag, yaw || 0, beat || 0, pulse || 0, w, h]);
  console.log('render', res);
  await p.screenshot({ path: out, omitBackground: false });
  await b.close();
})().catch(e => { console.error(e.message.slice(0, 1500)); process.exit(1); });

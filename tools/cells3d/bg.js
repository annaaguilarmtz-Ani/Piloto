// Fondos realistas de las células: node bg.js  -> app/src/main/assets/cells/bg_*.webp
const fs = require('fs');
const { chromium } = require(require('child_process').execSync('npm root -g').toString().trim() + '/playwright');
const OUT = __dirname + '/../../app/src/main/assets/cells';
const W = +process.env.BW || 810, H = +process.env.BH || 1800;
const frag = `#version 300 es
precision highp float;
uniform vec2 uRes; uniform int uMode;
out vec4 fragColor;
float hash(vec2 p){ p=fract(p*vec2(123.34,456.21)); p+=dot(p,p+45.32); return fract(p.x*p.y); }
float noise(vec2 x){ vec2 i=floor(x), f=fract(x); f=f*f*(3.0-2.0*f); return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y); }
float fbm(vec2 p){ float s=0.,a=.5; for(int i=0;i<6;i++){ s+=a*noise(p); p=p*2.02+vec2(1.7,9.2); a*=.5;} return s; }
float ridge(vec2 p){ return 1.0-abs(2.0*fbm(p)-1.0); }
float worley(vec2 p, out vec2 cid){ vec2 i=floor(p), f=fract(p); float d=1e9; for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++){ vec2 g=vec2(x,y); vec2 o=vec2(hash(i+g),hash(i+g+7.7)); vec2 r=g+o-f; float dd=dot(r,r); if(dd<d){d=dd;cid=i+g;} } return sqrt(d); }
float sdRR(vec2 p, vec2 b, float r){ vec2 q=abs(p)-b+r; return length(max(q,0.0))+min(max(q.x,q.y),0.0)-r; }
vec3 lit(vec3 base, float h){ // relieve por derivadas
  vec2 g=vec2(dFdx(h),dFdy(h))*uRes.y*0.5; vec3 n=normalize(vec3(-g,1.0)); vec3 L=normalize(vec3(-0.5,0.6,0.7));
  float d=clamp(dot(n,L)*0.6+0.55,0.0,1.4); float s=pow(max(dot(reflect(-L,n),vec3(0,0,1)),0.0),24.0);
  return base*d+vec3(0.9,0.85,0.8)*s*0.25; }
vec3 eukaryote(vec2 uv){
  vec2 c=vec2(0.5,0.46); vec2 ab=vec2(0.47,0.40)*vec2(1.0,uRes.y/uRes.x*0.0+1.0);
  vec2 q=(uv-c)/vec2(0.47,0.40)*vec2(1.0,1.0); float e=length(q);
  float wob=0.012*(fbm(uv*6.0)-0.5); e+=wob;
  if(e>1.25){ return vec3(0.0); }
  float h=fbm(uv*vec2(5.0,9.0)*1.3)*0.5+0.5*fbm(uv*30.0)*0.2;
  vec3 cyto=mix(vec3(0.13,0.34,0.42),vec3(0.06,0.16,0.24),smoothstep(0.0,1.0,e));
  cyto*=0.8+0.5*fbm(uv*vec2(4.0,7.0)+3.0);
  // citoesqueleto: filamentos tenues
  float f1=ridge(uv*vec2(3.0,5.0)+vec2(2.0,1.0)); float f2=ridge(uv*vec2(6.0,3.0)+9.0);
  cyto+=vec3(0.35,0.55,0.60)*(smoothstep(0.93,0.995,f1)*0.10+smoothstep(0.94,0.995,f2)*0.08);
  // gránulos finos
  cyto+=vec3(0.5,0.6,0.7)*smoothstep(0.86,0.95,noise(uv*220.0))*0.06;
  vec3 col=lit(cyto,h*0.03);
  col*=mix(1.0,0.45,smoothstep(0.7,1.0,e));
  // membrana plasmática: doble capa
  float m1=smoothstep(0.045,0.0,abs(e-0.985)), m2=smoothstep(0.03,0.0,abs(e-0.945));
  col=mix(col,vec3(0.98,0.78,0.36),m1*0.95); col=mix(col,vec3(0.90,0.55,0.20),m2*0.8);
  col+=vec3(1.0,0.85,0.5)*smoothstep(0.03,0.0,abs(e-0.985))*0.35*smoothstep(0.35,0.8,fbm(uv*40.0));
  // halo exterior
  if(e>1.0) col=mix(vec3(0.0),vec3(0.55,0.42,0.15)*exp(-(e-1.0)*45.0),step(1.0,e))*0.7+col*step(e,1.0);
  return col;
}
vec3 plant(vec2 uv){
  vec2 c=vec2(0.5,0.46); vec2 hs=vec2(0.46,0.40); float d=sdRR(uv-c,hs,0.045);
  // pared celular (banda de 0.03) y lámina media
  float wallIn=-0.030, mid=0.012;
  if(d>mid+0.01) return vec3(0.0);
  vec3 col;
  float fib=ridge(vec2(uv.x*220.0+uv.y*60.0,uv.y*26.0))*0.5+ridge(vec2(uv.x*24.0,uv.y*200.0-uv.x*60.0))*0.5;
  if(d>0.0){ col=mix(vec3(0.85,0.82,0.55),vec3(0.95,0.92,0.68),fbm(uv*60.0)); }
  else if(d>wallIn){ col=mix(vec3(0.34,0.50,0.16),vec3(0.55,0.72,0.28),fib)*(0.85+0.3*fbm(uv*30.0)); col=lit(col,fib*0.02);
    col=mix(col,vec3(0.9,0.75,0.35),smoothstep(0.004,0.0,abs(d-wallIn+0.004))*0.9); }
  else {
    vec3 cy=mix(vec3(0.16,0.42,0.30),vec3(0.08,0.24,0.18),smoothstep(-0.2,0.0,d)); cy*=0.75+0.5*fbm(uv*vec2(5.0,8.0));
    cy+=vec3(0.4,0.7,0.5)*smoothstep(0.86,0.95,noise(uv*200.0))*0.05; col=cy;
    // vacuola
    vec2 vc=vec2(0.5,0.53); vec2 vh=vec2(0.31,0.263); float vd=sdRR(uv-vc,vh,0.10);
    if(vd<0.0){
      vec3 vcol=mix(vec3(0.16,0.52,0.66),vec3(0.06,0.28,0.42),smoothstep(-0.2,0.0,vd));
      float caus=ridge(uv*vec2(7.0,11.0)+vec2(0.0,0.0)); vcol+=vec3(0.5,0.8,0.9)*smoothstep(0.90,0.99,caus)*0.13;
      vcol*=0.85+0.3*fbm(uv*vec2(4.0,6.0)+5.0); col=vcol;
    }
    col=mix(col,vec3(0.55,0.85,0.95),smoothstep(0.006,0.0,abs(vd))*0.9);
    col=mix(col,vec3(0.90,0.72,0.30),smoothstep(0.004,0.0,abs(d+0.022))*0.9);
  }
  return col;
}
vec3 prokaryote(vec2 uv){
  vec2 c=vec2(0.5,0.44); vec2 q=(uv-c)/vec2(0.44,0.36); float e=length(q)+0.010*(fbm(uv*8.0)-0.5);
  vec3 col=vec3(0.0);
  // cápsula difusa
  float cap=smoothstep(1.085,1.0,e); col=mix(col,vec3(0.10,0.30,0.20)*(0.8+0.4*fbm(uv*30.0)),cap*0.75*smoothstep(1.0,1.08,e+0.0));
  if(e<1.09){
    vec3 cy=mix(vec3(0.15,0.42,0.42),vec3(0.06,0.20,0.24),smoothstep(0.0,1.0,e)); cy*=0.8+0.5*fbm(uv*vec2(5.0,8.0));
    float gran=smoothstep(0.80,0.92,noise(uv*260.0)); cy+=vec3(0.55,0.45,0.75)*gran*0.10;
    col=cy; col*=mix(1.0,0.5,smoothstep(0.78,1.0,e));
    float wall=smoothstep(0.045,0.0,abs(e-0.985)); float fib=ridge(uv*vec2(140.0,60.0)+fbm(uv*30.0)*4.0);
    col=mix(col,mix(vec3(0.28,0.55,0.38),vec3(0.55,0.82,0.55),fib),wall*0.95);
    float mem=smoothstep(0.02,0.0,abs(e-0.94)); col=mix(col,vec3(0.95,0.75,0.35),mem*0.9);
    float cw=smoothstep(0.02,0.0,abs(e-1.06)); col=mix(col,vec3(0.35,0.75,0.55),cw*0.45);
  }
  return col;
}
vec3 blood(vec2 uv){
  float wallW=0.09; float xl=uv.x, xr=1.0-uv.x; float edge=min(xl,xr);
  vec3 col;
  if(edge<wallW){
    vec2 cid; vec2 wp=vec2(edge*9.0/wallW*0.9, uv.y*13.0); float wd=worley(wp+vec2(0.0,floor(uv.x*2.0)*3.0),cid);
    float bord=smoothstep(0.05,0.0,abs(wd-0.42))*0.0+smoothstep(0.42,0.5,wd);
    col=mix(vec3(0.42,0.10,0.12),vec3(0.62,0.22,0.20),fbm(uv*vec2(20.0,40.0)));
    col=mix(col,vec3(0.16,0.03,0.05),bord*0.7);
    float nuc=smoothstep(0.24,0.12,wd); col=mix(col,vec3(0.30,0.08,0.14),nuc*0.8);
    float h=(1.0-wd)*0.02+fbm(uv*60.0)*0.01; col=lit(col,h);
    col*=mix(0.35,1.0,smoothstep(0.0,wallW,edge));
    col+=vec3(0.9,0.5,0.4)*smoothstep(wallW,wallW*0.7,edge)*0.12;
  } else {
    float t=(edge-wallW)/(0.5-wallW);
    col=mix(vec3(0.55,0.09,0.11),vec3(0.75,0.14,0.15),t); col*=0.8+0.4*fbm(uv*vec2(6.0,10.0)+2.0);
    col+=vec3(0.7,0.3,0.25)*smoothstep(0.88,0.98,ridge(uv*vec2(4.0,9.0)))*0.06;
  }
  col*=1.0-0.35*length(uv-0.5);
  return col;
}
vec4 scope(vec2 uv){
  vec2 q=(uv-0.5)*vec2(1.0,0.62)*2.0; float r=length(q);
  float vig=smoothstep(0.72,1.25,r)*0.9;
  float rim=smoothstep(0.02,0.0,abs(r-1.02))*0.10;
  float grain=(hash(floor(uv*uRes*0.5))-0.5);
  float a=vig+abs(grain)*0.09;
  vec3 col=vec3(0.0)+vec3(0.9,0.95,1.0)*max(grain,0.0)*0.9;
  // polvo y fibras
  float dust=0.0; for(int i=0;i<40;i++){ float fi=float(i); vec2 c=vec2(hash(vec2(fi,1.0)),hash(vec2(fi,2.0))); float sz=0.0015+0.004*hash(vec2(fi,3.0)); dust=max(dust,smoothstep(sz,sz*0.3,length((uv-c)*vec2(1.0,uRes.y/uRes.x)))*0.35); }
  for(int i=0;i<8;i++){ float fi=float(i)+50.0; vec2 c=vec2(hash(vec2(fi,1.0)),hash(vec2(fi,2.0))); vec2 d=uv-c; float ang=hash(vec2(fi,4.0))*6.28; d=mat2(cos(ang),-sin(ang),sin(ang),cos(ang))*d; d.y+=0.02*sin(d.x*40.0); float f=smoothstep(0.0012,0.0002,abs(d.y*uRes.y/uRes.x))*smoothstep(0.05,0.03,abs(d.x)); dust=max(dust,f*0.4); }
  a=clamp(a+dust,0.0,1.0);
  float glare=exp(-length((uv-vec2(0.22,0.14))*vec2(1.0,0.5))*7.0)*0.10;
  vec3 c2=mix(col,vec3(1.0,0.97,0.9),glare*3.0+rim*4.0);
  float aa=clamp(a+glare+rim,0.0,1.0);
  return vec4(c2*aa,aa);
}
void main(){
  vec2 uv=gl_FragCoord.xy/uRes; uv.y=1.0-uv.y;
  if(uMode==4){ fragColor=scope(uv); return; }
  vec3 c= uMode==0?eukaryote(uv): uMode==1?plant(uv): uMode==2?prokaryote(uv): blood(uv);
  c=pow(clamp(c,0.0,1.0),vec3(0.95));
  fragColor=vec4(c,1.0);
}`;
(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome', args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const p = await b.newPage({ viewport: { width: W, height: H } });
  await p.setContent(`<canvas id=c width=${W} height=${H}></canvas>`);
  const names = ['euk', 'plant', 'prok', 'blood', 'scope'];
  for (let m = 0; m < 5; m++) {
    const url = await p.evaluate(([frag, m, W, H]) => {
      const c = document.getElementById('c'); const gl = c.getContext('webgl2', { preserveDrawingBuffer: true, premultipliedAlpha: true });
      const mk = (t, s) => { const sh = gl.createShader(t); gl.shaderSource(sh, s); gl.compileShader(sh); if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(sh)); return sh; };
      const pr = gl.createProgram(); gl.attachShader(pr, mk(gl.VERTEX_SHADER, '#version 300 es\nin vec2 a; void main(){ gl_Position=vec4(a,0.,1.); }')); gl.attachShader(pr, mk(gl.FRAGMENT_SHADER, frag));
      gl.linkProgram(pr); if (!gl.getProgramParameter(pr, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(pr)); gl.useProgram(pr);
      const buf = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buf); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
      const loc = gl.getAttribLocation(pr, 'a'); gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
      gl.uniform2f(gl.getUniformLocation(pr, 'uRes'), W, H); gl.uniform1i(gl.getUniformLocation(pr, 'uMode'), m);
      gl.viewport(0, 0, W, H); gl.clearColor(0,0,0,0); gl.clear(gl.COLOR_BUFFER_BIT); gl.drawArrays(gl.TRIANGLES, 0, 3); gl.finish();
      return c.toDataURL('image/webp', 0.9);
    }, [frag, m, W, H]);
    fs.writeFileSync(`${OUT}/bg_${names[m]}.webp`, Buffer.from(url.split(',')[1], 'base64'));
    if (process.env.PNG) { const png = await p.evaluate(() => document.getElementById('c').toDataURL('image/png')); fs.writeFileSync(`/tmp/bg_${names[m]}.png`, Buffer.from(png.split(',')[1], 'base64')); }
    console.log('bg', names[m]);
  }
  await b.close();
})().catch(e => { console.error(e.message.slice(0, 1500)); process.exit(1); });

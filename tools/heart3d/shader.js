// Fragment shader: corazón humano por ray-marching de campos de distancia (SDF) con iluminación húmeda.
const curves = {
  LAD: [[0.10, 0.42], [0.06, 0.18], [0.05, -0.15], [0.08, -0.55], [0.15, -0.95], [0.22, -1.22]],
  RCA: [[-0.10, 0.46], [-0.34, 0.46], [-0.58, 0.32], [-0.72, 0.02], [-0.72, -0.38], [-0.58, -0.75], [-0.38, -1.02]],
  B1: [[0.08, 0.14], [0.30, 0.06], [0.50, -0.24], [0.56, -0.6]],
  B2: [[0.06, -0.28], [0.28, -0.48], [0.42, -0.8]],
  B3: [[0.10, -0.62], [0.30, -0.9], [0.34, -1.1]],
  B4: [[-0.72, -0.1], [-0.55, -0.15], [-0.4, -0.35]],
  B5: [[-0.72, -0.4], [-0.5, -0.5], [-0.3, -0.7]],
  V1: [[0.20, -1.16], [0.14, -0.7], [0.10, -0.2], [0.14, 0.2], [0.30, 0.42], [0.52, 0.58]],
  V2: [[-0.34, -0.98], [-0.5, -0.7], [-0.6, -0.3], [-0.56, 0.06]],
  V3: [[0.28, -0.5], [0.44, -0.3], [0.62, 0.0]],
};
function smooth(pts, n) {
  const out = [], m = pts.length, g = (i) => pts[Math.max(0, Math.min(m - 1, i))];
  for (let i = 0; i < m - 1; i++) for (let s = 0; s < n; s++) { const t = s / n, t2 = t * t, t3 = t2 * t, a = g(i - 1), b = g(i), c = g(i + 1), d = g(i + 2);
    out.push([0, 1].map(k => 0.5 * (2 * b[k] + (-a[k] + c[k]) * t + (2 * a[k] - 5 * b[k] + 4 * c[k] - d[k]) * t2 + (-a[k] + 3 * b[k] - 3 * c[k] + d[k]) * t3))); }
  out.push(pts[m - 1]); return out;
}
function polyFn(name, pts) {
  pts = smooth(pts, 4);
  let s = `float d_${name}(vec2 p){ float d=1e9; vec2 a,b,pa,ba; float h;\n`;
  for (let i = 0; i + 1 < pts.length; i++) s += `a=vec2(${pts[i][0]},${pts[i][1]}); b=vec2(${pts[i + 1][0]},${pts[i + 1][1]}); pa=p-a; ba=b-a; h=clamp(dot(pa,ba)/dot(ba,ba),0.,1.); d=min(d,length(pa-ba*h));\n`;
  return s + 'return d; }\n';
}
const curveGLSL = Object.entries(curves).map(([k, v]) => polyFn(k, v)).join('\n');

module.exports = `#version 300 es
precision highp float;
uniform vec2 uRes; uniform float uYaw, uBeat, uPulse;
out vec4 fragColor;
mat2 rot(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}
float hash(vec3 p){ p=fract(p*0.3183099+.1); p*=17.0; return fract(p.x*p.y*p.z*(p.x+p.y+p.z)); }
float noise(vec3 x){ vec3 i=floor(x), f=fract(x); f=f*f*(3.0-2.0*f);
  return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x), mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
             mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x), mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z); }
float fbm(vec3 p){ float s=0.,a=.5; for(int i=0;i<5;i++){ s+=a*noise(p); p=p*2.03+vec3(1.7,9.2,3.1); a*=.5;} return s; }
float smin(float a,float b,float k){ float h=max(k-abs(a-b),0.0)/k; return min(a,b)-h*h*k*0.25; }
float sdE(vec3 p, vec3 r){ float k0=length(p/r); float k1=length(p/(r*r)); return k0*(k0-1.0)/k1; }
float sdC(vec3 p, vec3 a, vec3 b, float ra, float rb){ vec3 pa=p-a, ba=b-a; float h=clamp(dot(pa,ba)/dot(ba,ba),0.,1.); return length(pa-ba*h)-mix(ra,rb,h); }
${curveGLSL}
// hueco en el extremo cortado de un vaso
float openEnd(float d, vec3 p, vec3 e, vec3 dir, float r){ return max(d, -sdC(p, e-dir*0.06, e+dir*0.4, r*0.62, r*0.62)); }

// Devuelve (distancia, material): 1 ventrículo, 2 aurícula, 3 aorta, 4 pulmonar, 5 cava
vec2 mapBase(vec3 p){
  vec3 c=vec3(0.1,-0.3,0.0);
  float k=1.0-0.055*uBeat;
  vec3 pv=c+(p-c)/k; pv.xz=rot(0.05*uBeat)*pv.xz;
  // Una sola masa ventricular cónica (ápex único) + abombamiento del ventrículo derecho
  vec3 q=pv-vec3(0.10,-0.30,-0.05); q.xy=rot(0.22)*q.xy; q.xz*=1.0+1.25*smoothstep(0.1,-1.05,q.y);
  float lv=sdE(q,vec3(0.80,1.05,0.56))*0.72;
  vec3 r=pv-vec3(-0.36,0.02,0.30); r.xy=rot(-0.16)*r.xy; r.xz*=1.0+1.4*smoothstep(0.0,-0.6,r.y);
  float rv=sdE(r,vec3(0.42,0.60,0.34))*0.75;
  float vent=smin(lv,rv,0.22)*k;
  float ra=sdE(p-vec3(-0.55,0.45,0.02),vec3(0.42,0.42,0.36));
  vec3 a1=p-vec3(-0.30,0.68,0.40); a1.xy=rot(0.6)*a1.xy; float raa=sdE(a1,vec3(0.34,0.20,0.17));
  float la=sdE(p-vec3(0.38,0.52,-0.38),vec3(0.5,0.36,0.3));
  vec3 a2=p-vec3(0.64,0.58,0.14); a2.xy=rot(-0.5)*a2.xy; float laa=sdE(a2,vec3(0.27,0.17,0.13));
  float atr=smin(smin(ra,raa,0.10),smin(la,laa,0.10),0.10);
  // aorta
  float pu=1.0+0.05*uPulse;
  vec3 A0=vec3(0.12,0.30,-0.02),A1=vec3(0.08,0.72,-0.08),A2=vec3(0.10,1.02,-0.14),A3=vec3(0.30,1.26,-0.20),A4=vec3(0.62,1.24,-0.28),A5=vec3(0.90,1.0,-0.38);
  float ao=min(min(sdC(p,A0,A1,0.20*pu,0.19*pu),sdC(p,A1,A2,0.19*pu,0.18*pu)),min(min(sdC(p,A2,A3,0.18*pu,0.18*pu),sdC(p,A3,A4,0.18*pu,0.175*pu)),sdC(p,A4,A5,0.175*pu,0.17*pu)));
  vec3 h1a=vec3(0.18,1.24,-0.19),h1b=vec3(0.12,1.78,-0.2),h2a=vec3(0.36,1.28,-0.22),h2b=vec3(0.38,1.82,-0.24),h3a=vec3(0.54,1.26,-0.27),h3b=vec3(0.66,1.74,-0.29);
  float hv=min(min(sdC(p,h1a,h1b,0.075,0.07),sdC(p,h2a,h2b,0.07,0.065)),sdC(p,h3a,h3b,0.065,0.06));
  float aoAll=smin(ao,hv,0.05);
  aoAll=openEnd(aoAll,p,A5,normalize(A5-A4),0.17);
  aoAll=openEnd(aoAll,p,h1b,normalize(h1b-h1a),0.07); aoAll=openEnd(aoAll,p,h2b,normalize(h2b-h2a),0.065); aoAll=openEnd(aoAll,p,h3b,normalize(h3b-h3a),0.06);
  // tronco pulmonar
  vec3 T0=vec3(-0.12,0.32,0.36),T1=vec3(-0.02,0.66,0.44),T2=vec3(0.22,0.88,0.42),T3=vec3(0.55,0.95,0.32),T4=vec3(0.92,0.88,0.14);
  float pt=min(min(sdC(p,T0,T1,0.21,0.20),sdC(p,T1,T2,0.20,0.19)),min(sdC(p,T2,T3,0.19,0.17),sdC(p,T3,T4,0.17,0.16)));
  pt=openEnd(pt,p,T4,normalize(T4-T3),0.16);
  // cava superior
  vec3 S0=vec3(-0.70,0.55,0.0),S1=vec3(-0.72,1.10,-0.02),S2=vec3(-0.74,1.68,-0.02);
  float sv=min(sdC(p,S0,S1,0.17,0.16),sdC(p,S1,S2,0.16,0.16)); sv=openEnd(sv,p,S2,normalize(S2-S1),0.16);
  float body=smin(smin(vent,atr,0.10),smin(min(pt,aoAll),sv,0.05),0.06);
  float best=vent; float id=1.0;
  if(atr<best){best=atr;id=2.0;} if(aoAll<best){best=aoAll;id=3.0;} if(pt<best){best=pt;id=4.0;} if(sv<best){best=sv;id=5.0;}
  return vec2(body,id);
}
float fatMask(vec3 p){
  float f=0.0;
  f+=exp(-pow(d_LAD(p.xy)/0.06,2.0))*0.9+exp(-pow(d_RCA(p.xy)/0.07,2.0))*0.9;
  f+=0.5*(exp(-pow(d_V1(p.xy)/0.06,2.0))+exp(-pow(d_B1(p.xy)/0.05,2.0)));
  float base=smoothstep(-0.05,0.5,p.y)*(1.0-smoothstep(0.55,0.95,p.y));
  f+=base*(0.15+0.7*fbm(p*3.0+vec3(3.0)));
  return clamp(f*(0.4+0.8*fbm(p*7.0)),0.0,1.0);
}
float artMask(vec3 p){
  float w=0.020;
  float d=min(min(d_LAD(p.xy),d_RCA(p.xy)),min(min(d_B1(p.xy),d_B2(p.xy)),min(d_B3(p.xy),min(d_B4(p.xy),d_B5(p.xy)))));
  return 1.0-smoothstep(w*0.5,w,d);
}
float veinMask(vec3 p){ float w=0.016; float d=min(d_V1(p.xy),min(d_V2(p.xy),d_V3(p.xy))); return 1.0-smoothstep(w*0.5,w,d); }
float mapFull(vec3 p, out float id){
  vec2 m=mapBase(p); id=m.y; float d=m.x;
  if(d<0.25 && (m.y<2.5)){
    vec3 fq=vec3(p.x*14.0+p.y*6.0,p.y*2.6-p.x*3.0,p.z*14.0); float fibers=1.0-abs(2.0*noise(fq)-1.0);
    d+=0.007*(fbm(p*5.0)-0.5)+0.010*(fibers-0.5)+0.004*(noise(p*60.0)-0.5);
    float gr=exp(-pow(d_LAD(p.xy)/0.035,2.0))+0.8*exp(-pow(d_RCA(p.xy)/0.04,2.0)); d+=0.022*gr*step(-0.1,p.z);
    float fat=fatMask(p); d-=0.030*fat*(0.6+0.6*noise(p*20.0));
    d-=0.012*artMask(p)+0.008*veinMask(p);
  } else if(d<0.25){ d+=0.006*(fbm(p*9.0)-0.5); }
  return d;
}
float mapD(vec3 p){ return mapBase(p).x; }
vec3 calcN(vec3 p){ float id; vec2 e=vec2(0.0035,0.0); return normalize(vec3(mapFull(p+e.xyy,id)-mapFull(p-e.xyy,id), mapFull(p+e.yxy,id)-mapFull(p-e.yxy,id), mapFull(p+e.yyx,id)-mapFull(p-e.yyx,id))); }
float softShadow(vec3 ro, vec3 rd){ float res=1.0,t=0.04; for(int i=0;i<28;i++){ float h=mapD(ro+rd*t); res=min(res,10.0*h/t); t+=clamp(h,0.03,0.25); if(res<0.001||t>3.0)break;} return clamp(res,0.0,1.0); }
float calcAO(vec3 p, vec3 n){ float o=0.0,s=1.0; for(int i=0;i<5;i++){ float h=0.03+0.14*float(i)/4.0; o+=(h-mapD(p+n*h))*s; s*=0.72;} return clamp(1.0-2.2*o,0.0,1.0); }
vec3 env(vec3 r){
  float top=smoothstep(-0.2,1.0,r.y);
  vec3 c=mix(vec3(0.03,0.02,0.02),vec3(0.35,0.32,0.30),top);
  // cajas de luz (softboxes)
  c+=vec3(3.2,3.0,2.8)*smoothstep(0.86,0.93,dot(r,normalize(vec3(-0.5,0.7,0.7))));
  c+=vec3(1.0,1.15,1.4)*smoothstep(0.88,0.95,dot(r,normalize(vec3(0.8,0.2,0.5))));
  c+=vec3(1.3,0.8,0.6)*smoothstep(0.90,0.96,dot(r,normalize(vec3(0.0,-0.6,0.8))));
  return c;
}
vec3 shade(vec3 p, vec3 rd, float id){
  vec3 n=calcN(p);
  float fat=(id<2.5)?fatMask(p):0.0;
  float art=(id<2.5)?artMask(p):0.0, vein=(id<2.5)?veinMask(p):0.0;
  float mott=fbm(p*4.0+2.0), fine=fbm(p*22.0);
  vec3 alb;
  if(id<1.5) alb=mix(vec3(0.20,0.022,0.028),vec3(0.42,0.075,0.07),smoothstep(0.35,0.8,mott));
  else if(id<2.5) alb=mix(vec3(0.26,0.035,0.045),vec3(0.46,0.11,0.10),smoothstep(0.3,0.8,mott));
  else if(id<3.5) alb=mix(vec3(0.50,0.17,0.15),vec3(0.80,0.52,0.44),smoothstep(0.25,0.75,mott));
  else if(id<4.5) alb=mix(vec3(0.38,0.10,0.14),vec3(0.60,0.24,0.26),smoothstep(0.3,0.8,mott));
  else alb=mix(vec3(0.26,0.07,0.15),vec3(0.46,0.18,0.30),smoothstep(0.3,0.8,mott));
  alb*=0.8+0.4*fine;
  float net=pow(1.0-abs(2.0*noise(p*34.0+3.0)-1.0),9.0); float net2=pow(1.0-abs(2.0*fbm(p*9.0+5.0)*1.0-1.0),10.0);
  if(id<2.5){ alb=mix(alb,vec3(0.55,0.05,0.06),net*0.45); alb=mix(alb,vec3(0.09,0.01,0.015),net2*0.6); }
  alb*=mix(0.5,1.0,smoothstep(-1.35,0.25,p.y));
  vec3 fatC=mix(vec3(0.74,0.50,0.20),vec3(0.90,0.72,0.42),fbm(p*10.0));
  alb=mix(alb,fatC,smoothstep(0.35,0.7,fat)*0.92);
  alb=mix(alb,vec3(0.62,0.03,0.03),art*0.9);
  alb=mix(alb,vec3(0.24,0.12,0.36),vein*0.85);
  float wet=0.5+0.5*fbm(p*5.0+7.0);
  float ao=calcAO(p,n);
  vec3 L1=normalize(vec3(-0.5,0.75,0.75)); vec3 L2=normalize(vec3(0.85,0.1,0.5)); vec3 L3=normalize(vec3(-0.2,-0.7,0.6));
  float sh=softShadow(p+n*0.02,L1);
  float d1=clamp((dot(n,L1)+0.35)/1.35,0.0,1.0), d2=clamp((dot(n,L2)+0.2)/1.2,0.0,1.0), d3=clamp(dot(n,L3),0.0,1.0);
  vec3 sss=vec3(0.55,0.06,0.04)*pow(1.0-abs(dot(n,-rd)),2.0)*0.6;
  vec3 col=alb*(vec3(1.1,1.0,0.95)*d1*(0.2+0.8*sh)*1.0+vec3(0.30,0.36,0.50)*d2*0.35+vec3(0.55,0.35,0.25)*d3*0.18+vec3(0.05,0.04,0.045));
  col+=alb*sss;
  col*=mix(0.45,1.0,ao);
  vec3 nm=normalize(n+0.14*(vec3(noise(p*45.0),noise(p*45.0+9.0),noise(p*45.0+17.0))-0.5)); vec3 refl=reflect(rd,nm);
  float fres=0.04+0.96*pow(1.0-clamp(dot(n,-rd),0.0,1.0),5.0);
  float gloss=(id<2.5)?mix(0.15,1.0,wet*wet)*(1.0-0.8*smoothstep(0.3,0.65,fat)):mix(0.3,0.8,wet);
  vec3 spec=env(refl)*(0.12+fres*0.9)*gloss*mix(0.5,1.0,ao);
  vec3 hv=normalize(L1-rd); spec+=vec3(1.0,0.95,0.9)*pow(max(dot(nm,hv),0.0),90.0)*1.1*gloss*sh;
  col+=spec;
  col+=vec3(0.5,0.12,0.10)*pow(1.0-clamp(dot(n,-rd),0.0,1.0),3.0)*0.35;
  return col;
}
vec3 aces(vec3 x){ return clamp((x*(2.51*x+0.03))/(x*(2.43*x+0.59)+0.14),0.0,1.0); }
void main(){
  vec2 uv=(gl_FragCoord.xy-0.5*uRes)/uRes.y;
  float halfH=1.72; vec2 sc=vec2(uv.x*2.0*halfH-0.05, uv.y*2.0*halfH+0.22);
  vec3 ro=vec3(sc,5.0), rd=vec3(0.0,0.0,-1.0);
  // perspectiva suave
  vec3 fo=vec3(0.0,0.3,0.0); ro=vec3(sc*1.0,5.0); rd=normalize(vec3(sc*0.0,-1.0)+vec3((sc.x)*0.02,(sc.y-0.3)*0.02,0.0));
  mat2 ry=rot(uYaw);
  float t=3.0, mind=1e9, id=1.0; bool hit=false; vec3 p;
  for(int i=0;i<140;i++){
    p=ro+rd*t; vec3 q=p; q.xz=ry*q.xz;
    vec2 m=mapBase(q); mind=min(mind,m.x);
    if(m.x<0.0015){hit=true; id=m.y; break;}
    t+=m.x*0.8; if(t>8.0)break;
  }
  float px=2.0*2.0*halfH/uRes.y;
  float alpha=hit?1.0:(1.0-smoothstep(0.0,px*1.2,mind));
  if(alpha<=0.001){ fragColor=vec4(0.0); return; }
  vec3 q=p; q.xz=ry*q.xz;
  // afinar sobre el campo con detalle
  for(int i=0;i<4;i++){ float dd=mapFull(q,id); q+=rd*ry[0][0]*0.0; vec3 ddir=rd; ddir.xz=ry*ddir.xz; q+=ddir*dd*0.7; }
  vec3 rdl=rd; rdl.xz=ry*rdl.xz;
  vec3 col=shade(q,rdl,id);
  col=aces(col*1.05); col=pow(col,vec3(1.0/2.2));
  fragColor=vec4(col*alpha,alpha);
}
`;

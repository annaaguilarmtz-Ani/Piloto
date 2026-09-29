const S = {};
S.mito = { w: 512, h: 384, half: 0.9, rot: [-0.55, 0.0, 0.25], tint: [0.9, 0.3, 0.1], glsl: `
float mapD(vec3 q, out float id){
  float R=0.52, L=0.82;
  float outerSolid=sdC(q,vec3(-L,0,0),vec3(L,0,0),R,R*0.98);
  float d=abs(outerSolid+0.03)-0.03; id=1.0;
  float innerSolid=sdC(q,vec3(-L+0.03,0,0),vec3(L-0.03,0,0),R-0.09,R-0.09);
  float ish=abs(innerSolid+0.018)-0.018;
  float cr=1e9;
  for(int i=0;i<6;i++){ float xi=-0.66+float(i)*0.265; float sg=(i%2==0)?1.0:-1.0;
    vec3 pp=q-vec3(xi,sg*0.245,0.0); pp.x+=0.022*sin(q.y*10.0+float(i)*1.3+q.z*4.0);
    float pl=sdBox(pp,vec3(0.026,0.20,0.62))-0.008; cr=min(cr,max(pl,innerSolid+0.01)); }
  if(ish<d){d=ish;id=2.0;} if(cr<d){d=cr;id=3.0;}
  // ribosomas y ADN mitocondrial en la matriz
  float g=1e9; for(int i=0;i<9;i++){ float fi=float(i); vec3 c=vec3(-0.6+fi*0.15, 0.12*sin(fi*2.3), -0.25+0.07*cos(fi*1.7)); g=min(g,length(q-c)-0.03); }
  float dna=sdTorus((q-vec3(0.05,0.0,-0.28)).xzy,vec2(0.11,0.017));
  if(g<d){d=g;id=4.0;} if(dna<d){d=dna;id=5.0;}
  return max(d, q.z-0.035);
}
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){
  float m=fbm(p*6.0); gloss=0.7; sss=0.5;
  if(id<1.5){ gloss=0.85; return mix(vec3(0.70,0.24,0.08),vec3(0.90,0.42,0.16),m); }
  if(id<2.5){ gloss=0.6; return mix(vec3(0.80,0.42,0.22),vec3(0.92,0.58,0.32),m); }
  if(id<3.5){ gloss=0.55; return mix(vec3(0.88,0.50,0.28),vec3(0.98,0.68,0.42),m); }
  if(id<4.5){ gloss=0.4; return vec3(0.35,0.10,0.10); }
  gloss=0.8; return vec3(0.95,0.95,0.85);
}` };
S.nucleus = { w: 512, h: 512, half: 1.05, rot: [-0.35, 0.3, 0.0], tint: [0.4, 0.3, 0.9], glsl: `
float mapD(vec3 q, out float id){
  float R=0.9;
  vec3 n=normalize(q+1e-4);
  float pat=sin(n.x*15.0)*sin(n.y*15.0+1.0)*sin(n.z*15.0+2.0);
  float l=length(q);
  float sh1=abs(l-R+0.03)-0.03; sh1=max(sh1,(pat-0.72)*0.07);
  float sh2=abs(l-(R-0.10)+0.015)-0.018; sh2=max(sh2,(pat-0.72)*0.07);
  float d=sh1; id=1.0; if(sh2<d){d=sh2;id=2.0;}
  float st=abs(fbm(q*3.0+3.0)-0.5)-0.028; st=max(st,l-(R-0.16));
  st=min(st, max(abs(fbm(q*4.2+9.0)-0.5)-0.022, l-(R-0.16)));
  if(st<d){d=st;id=3.0;}
  float nu=length(q-vec3(0.22,-0.18,-0.12))-0.27+0.03*(fbm(q*9.0)-0.5); if(nu<d){d=nu;id=4.0;}
  return max(d,q.z-0.04);
}
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){
  float m=fbm(p*5.0); gloss=0.7; sss=0.6;
  if(id<1.5){ gloss=0.85; return mix(vec3(0.30,0.34,0.80),vec3(0.50,0.55,0.95),m); }
  if(id<2.5){ gloss=0.7; return mix(vec3(0.40,0.45,0.85),vec3(0.62,0.66,1.0),m); }
  if(id<3.5){ gloss=0.5; return mix(vec3(0.55,0.42,0.90),vec3(0.75,0.62,1.0),m); }
  gloss=0.4; return mix(vec3(0.35,0.14,0.50),vec3(0.55,0.28,0.70),m);
}` };
S.chloro = { w: 512, h: 384, half: 0.75, rot: [-0.5, 0.0, 0.2], tint: [0.2, 0.9, 0.3], glsl: `
float mapD(vec3 q, out float id){
  vec3 R=vec3(1.0,0.52,0.52);
  float e=sdE(q,R);
  float d=abs(e+0.03)-0.03; id=1.0;
  float e2=sdE(q,R-vec3(0.06));
  float m2=abs(e2+0.012)-0.012; if(m2<d){d=m2;id=2.0;}
  // grana: pilas de discos
  float gr=1e9;
  for(int i=0;i<4;i++){ float fi=float(i); vec3 c=vec3(-0.55+fi*0.36, -0.12+0.08*sin(fi*2.1), 0.05*cos(fi*1.3)-0.05);
    vec3 pp=q-c; float yy=pp.y+0.02; float cell=floor(yy/0.075+0.5); float ly=yy-cell*0.075; float cl=clamp(cell,-3.0,3.0);
    float disc=sdCyl(vec3(pp.x,ly,pp.z),0.022,0.17-0.006*abs(cl)); if(abs(cell)>3.0) disc=1e9; gr=min(gr,disc-0.006); }
  // lamelas del estroma
  float lam=1e9; for(int i=0;i<3;i++){ float fi=float(i); vec3 c=vec3(-0.37+fi*0.36,0.10*sin(fi*3.0),0.0); lam=min(lam,sdBox(q-c,vec3(0.19,0.008,0.05))); }
  if(lam<gr) gr=lam;
  gr=max(gr,e2+0.02);
  if(gr<d){d=gr;id=3.0;}
  float st=sdE(q-vec3(0.55,0.05,-0.15),vec3(0.19,0.13,0.12)); if(st<d){d=st;id=4.0;}
  float pg=1e9; for(int i=0;i<6;i++){ float fi=float(i); pg=min(pg,length(q-vec3(0.05+0.1*fi*0.6-0.2,-0.33+0.04*sin(fi),0.2-0.08*fi))-0.035); }
  if(pg<d){d=pg;id=5.0;}
  return max(d,q.z-0.03);
}
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){
  float m=fbm(p*6.0); gloss=0.7; sss=0.6;
  if(id<1.5){ gloss=0.85; return mix(vec3(0.14,0.50,0.20),vec3(0.28,0.70,0.32),m); }
  if(id<2.5){ gloss=0.7; return mix(vec3(0.22,0.60,0.26),vec3(0.42,0.80,0.40),m); }
  if(id<3.5){ gloss=0.5; return mix(vec3(0.12,0.62,0.22),vec3(0.30,0.85,0.35),m); }
  if(id<4.5){ gloss=0.8; return vec3(0.92,0.92,0.86); }
  gloss=0.5; return vec3(0.30,0.32,0.10);
}` };

S.golgi = { w: 512, h: 384, half: 0.95, rot: [-0.75, 0.25, 0.0], tint: [0.3, 0.8, 0.5], glsl: `
float mapD(vec3 q, out float id){
  float d=1e9; id=1.0;
  for(int i=0;i<6;i++){ float fi=float(i);
    vec3 p=q; p.y+=0.32*p.x*p.x-0.05*p.z*p.z;
    float w=0.82-0.05*fi, z=0.46-0.035*fi;
    float pl=sdBox(p-vec3(0.0,-0.5+fi*0.19,0.0),vec3(w,0.026,z))-0.02;
    float holes=fbm(vec3(q.x*7.0,q.z*7.0,fi*3.0)); pl=max(pl,(holes-0.60)*0.09);
    pl=max(pl, -(length(vec2(p.x/w,p.z/z))-0.0)*0.0-1.0+length(vec2(p.x/w,p.z/z))*1.0+ -0.0);
    if(pl<d){d=pl;id=1.0+fi;} }
  float v=1e9; for(int i=0;i<10;i++){ float fi=float(i); vec3 c=vec3(-0.95+fi*0.21, -0.55+0.9*fract(fi*0.618), 0.62*sin(fi*2.4)); v=min(v,length(q-c)-0.06-0.02*fract(fi*0.37)); }
  if(v<d){d=v;id=8.0;}
  return d;
}
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){
  float m=fbm(p*7.0); gloss=0.75; sss=0.6;
  if(id>7.5) return vec3(0.95,0.72,0.30);
  float t=(id-1.0)/5.0;
  return mix(mix(vec3(0.20,0.72,0.55),vec3(0.55,0.80,0.35),t*2.0),mix(vec3(0.55,0.80,0.35),vec3(0.92,0.55,0.20),clamp(t*2.0-1.0,0.0,1.0)),step(0.5,t))*(0.85+0.3*m);
}` };
S.er = { w: 512, h: 384, half: 1.0, rot: [-0.85, 0.3, 0.0], tint: [0.2, 0.6, 0.8], glsl: `
float mapD(vec3 q, out float id){
  float d=1e9; id=1.0; float rb=1e9;
  for(int i=0;i<3;i++){ float fi=float(i); float y=-0.45+fi*0.42;
    float wave=0.07*sin(q.x*5.0+q.z*3.0+fi*2.0)+0.03*sin(q.z*9.0+fi);
    float sheet=abs(q.y-y-wave)-0.028; sheet=max(sheet,max(abs(q.x)-0.95,abs(q.z)-0.6));
    sheet=max(sheet, (fbm(vec3(q.xz*6.0,fi))-0.66)*0.08);
    if(sheet<d){d=sheet;id=1.0;}
    vec2 cell=floor(q.xz/0.10+0.5); float hh=hash(vec3(cell,fi));
    vec2 c2=(cell+ (vec2(hash(vec3(cell,fi+5.0)),hash(vec3(cell,fi+9.0)))-0.5)*0.4)*0.10;
    float wv=0.07*sin(c2.x*5.0+c2.y*3.0+fi*2.0)+0.03*sin(c2.y*9.0+fi);
    float r=length(q-vec3(c2.x,y+wv+0.05,c2.y))-0.034; if(hh>0.3&&abs(c2.x)<0.9&&abs(c2.y)<0.55) rb=min(rb,r); }
  if(rb<d){d=rb;id=2.0;}
  return d;
}
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){
  float m=fbm(p*6.0); gloss=0.7; sss=0.5;
  if(id<1.5){ return mix(vec3(0.15,0.55,0.62),vec3(0.30,0.75,0.80),m); }
  gloss=0.6; return mix(vec3(0.55,0.35,0.85),vec3(0.75,0.55,1.0),m);
}` };
S.ser = { w: 512, h: 384, half: 1.0, rot: [-0.6, 0.2, 0.0], tint: [0.2, 0.6, 0.8], glsl: `
float tube(vec3 q, vec3 a, vec3 b, vec3 c){ return min(sdC(q,a,b,0.09,0.09),sdC(q,b,c,0.09,0.09)); }
float mapD(vec3 q, out float id){
  float d=1e9; id=1.0;
  d=min(d,tube(q,vec3(-0.9,-0.2,0.0),vec3(-0.4,0.25,0.2),vec3(0.1,-0.1,-0.1)));
  d=min(d,tube(q,vec3(0.1,-0.1,-0.1),vec3(0.55,0.3,0.0),vec3(0.95,0.0,0.15)));
  d=min(d,tube(q,vec3(-0.4,0.25,0.2),vec3(-0.2,0.6,-0.2),vec3(0.3,0.55,0.1)));
  d=min(d,tube(q,vec3(0.1,-0.1,-0.1),vec3(-0.1,-0.5,0.15),vec3(0.4,-0.7,-0.1)));
  d=min(d,tube(q,vec3(-0.9,-0.2,0.0),vec3(-0.7,-0.6,0.2),vec3(-0.2,-0.75,0.0)));
  d=smin(d,d,0.1);
  return d+0.01*(fbm(q*10.0)-0.5);
}
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ float m=fbm(p*6.0); gloss=0.8; sss=0.6; return mix(vec3(0.16,0.58,0.64),vec3(0.35,0.80,0.82),m); }` };
S.lyso = { w: 384, h: 384, half: 1.0, rot: [-0.3, 0.2, 0.0], tint: [0.9, 0.5, 0.1], glsl: `
float mapD(vec3 q, out float id){
  float R=0.85; float l=length(q);
  float d=abs(l-R+0.03)-0.03; id=1.0;
  float en=1e9; for(int i=0;i<14;i++){ float fi=float(i); vec3 c=0.55*vec3(sin(fi*2.3),cos(fi*1.7),sin(fi*3.1+1.0))*(0.5+0.5*fract(fi*0.71)); en=min(en,length(q-c)-0.07); }
  if(en<d){d=en;id=2.0;}
  float deb=sdE(q-vec3(-0.1,-0.05,-0.1),vec3(0.28,0.16,0.14))+0.02*(fbm(q*8.0)-0.5); if(deb<d){d=deb;id=3.0;}
  return max(d,q.z-0.1);
}
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ float m=fbm(p*6.0); gloss=0.8; sss=0.6;
  if(id<1.5) return mix(vec3(0.62,0.35,0.10),vec3(0.85,0.55,0.20),m);
  if(id<2.5) return vec3(0.95,0.62,0.20); return vec3(0.30,0.45,0.15); }` };
S.ribo = { w: 128, h: 128, half: 0.9, rot: [-0.3, 0.4, 0.0], tint: [0.6, 0.3, 0.9], glsl: `
float mapD(vec3 q, out float id){
  float big=sdE(q-vec3(0.0,0.22,0.0),vec3(0.62,0.42,0.45))+0.03*(fbm(q*7.0)-0.5); id=1.0;
  float sm=sdE(q-vec3(0.0,-0.28,0.0),vec3(0.52,0.22,0.36))+0.03*(fbm(q*7.0+3.0)-0.5);
  float d=smin(big,sm,0.06); if(sm<big) id=2.0; return d; }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.6; sss=0.5; float m=fbm(p*8.0);
  if(id<1.5) return mix(vec3(0.55,0.35,0.85),vec3(0.75,0.55,1.0),m); return mix(vec3(0.40,0.28,0.75),vec3(0.60,0.45,0.95),m); }` };
S.centriole = { w: 384, h: 384, half: 1.0, rot: [0.35, -0.5, 0.15], tint: [0.3, 0.4, 0.9], glsl: `
float cent(vec3 q){ float d=1e9;
  for(int i=0;i<9;i++){ float a=float(i)*0.698132; vec3 c=vec3(0.0,cos(a),sin(a))*0.22;
    for(int k=0;k<3;k++){ vec3 o=vec3(0.0,-sin(a),cos(a))*(float(k)-1.0)*0.055; d=min(d,sdC(q,c+o+vec3(-0.5,0,0),c+o+vec3(0.5,0,0),0.028,0.028)); } }
  return d; }
float mapD(vec3 q, out float id){ id=1.0;
  float a=cent(q-vec3(-0.15,0.25,0.0));
  vec3 p2=q-vec3(0.35,-0.35,0.0); p2=vec3(p2.y,p2.x,p2.z); float b=cent(p2);
  return min(a,b); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.75; sss=0.5; return mix(vec3(0.35,0.42,0.85),vec3(0.60,0.68,1.0),fbm(p*8.0)); }` };
S.rbc = { w: 192, h: 192, half: 1.0, rot: [0.0, 0.0, 0.0], tint: [0.9, 0.1, 0.1], glsl: `
float mapD(vec3 q, out float id){ id=1.0;
  float R=0.86; float r=length(q.xz); float s=min(r/R,1.0);
  float t=R*sqrt(max(1.0-s*s,0.0))*(0.207+2.003*s*s-1.123*s*s*s*s)*0.5;
  float dr=r-R, dy=abs(q.y)-t;
  float d=length(vec2(max(dr,0.0),max(dy,0.0)))+min(max(dr,dy),0.0);
  return d*0.7-0.02+0.004*(fbm(q*10.0)-0.5); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.55; sss=1.0; float m=fbm(p*5.0);
  float dimple=1.0-smoothstep(0.0,0.55,length(p.xz)); return mix(vec3(0.72,0.07,0.09),vec3(0.86,0.16,0.16),m)*(1.0-0.3*dimple); }` };
S.wbc = { w: 256, h: 256, half: 1.05, rot: [-0.3, 0.3, 0.0], tint: [0.9, 0.7, 0.6], glsl: `
float mapD(vec3 q, out float id){ id=1.0;
  return length(q)-0.85+0.05*fbm(q*4.5)+0.025*noise(q*16.0)-0.03; }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.55; sss=0.8; float m=fbm(p*7.0);
  vec3 cream=mix(vec3(0.86,0.80,0.68),vec3(0.95,0.90,0.80),m);
  float nd=1e9; for(int i=0;i<4;i++){ float a=float(i)*1.9+0.4; vec3 c=vec3(cos(a)*0.32,sin(a*1.3)*0.3,0.18+0.2*sin(a)); nd=min(nd,length(p-c)); }
  vec3 nuc=vec3(0.42,0.28,0.68); float k=smoothstep(0.62,0.32,nd);
  float gr=smoothstep(0.55,0.75,noise(p*30.0)); return mix(cream,nuc,k*0.85)*(1.0-0.12*gr); }` };
S.lymph = { w: 256, h: 256, half: 1.05, rot: [-0.3, 0.3, 0.0], tint: [0.9, 0.7, 0.6], glsl: `
float mapD(vec3 q, out float id){ id=1.0; return length(q)-0.72+0.04*fbm(q*4.5)+0.02*noise(q*16.0); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.55; sss=0.8; float m=fbm(p*7.0);
  vec3 cream=mix(vec3(0.70,0.78,0.86),vec3(0.82,0.88,0.94),m);
  float k=smoothstep(0.62,0.20,length(p-vec3(0.05,0.0,0.15))); return mix(cream,vec3(0.32,0.24,0.62),k*0.9); }` };
S.plt = { w: 128, h: 128, half: 1.0, rot: [-0.4, 0.3, 0.0], tint: [0.9, 0.6, 0.8], glsl: `
float mapD(vec3 q, out float id){ id=1.0; return sdE(q,vec3(0.85,0.36,0.6))*0.8+0.02*(fbm(q*9.0)-0.5); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.6; sss=0.8; float g=smoothstep(0.55,0.7,noise(p*22.0));
  return mix(vec3(0.85,0.60,0.80),vec3(0.55,0.30,0.55),g); }` };
S.bacterium = { w: 256, h: 128, half: 0.75, rot: [-0.3, 0.0, 0.0], tint: [0.4, 0.9, 0.3], glsl: `
float mapD(vec3 q, out float id){ id=1.0; return sdC(q,vec3(-0.45,0,0),vec3(0.45,0,0),0.25,0.25)+0.012*(noise(q*22.0)-0.5); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.8; sss=0.7; return mix(vec3(0.35,0.72,0.20),vec3(0.55,0.88,0.35),fbm(p*8.0)); }` };
module.exports = S;

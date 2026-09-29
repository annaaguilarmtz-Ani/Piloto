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
S.nucleus = { exp: 0.8, w: 512, h: 512, half: 1.05, rot: [-0.35, 0.3, 0.0], tint: [0.4, 0.3, 0.9], glsl: `
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
  if(id<3.5){ gloss=0.5; return mix(vec3(0.42,0.30,0.78),vec3(0.62,0.48,0.95),m); }
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

S.golgi = { step: 0.5, exp: 0.85, w: 512, h: 384, half: 0.95, rot: [-0.75, 0.25, 0.0], tint: [0.3, 0.8, 0.5], glsl: `
float mapD(vec3 q, out float id){
  float d=1e9; id=1.0;
  for(int i=0;i<6;i++){ float fi=float(i);
    vec3 p=q; p.y+=0.22*p.x*p.x-0.03*p.z*p.z;
    float w=0.82-0.05*fi, z=0.46-0.035*fi;
    float pl=(sdBox(p-vec3(0.0,-0.5+fi*0.19,0.0),vec3(w,0.026,z))-0.02)*0.7;
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
S.er = { step: 0.5, exp: 0.85, w: 512, h: 384, half: 1.0, rot: [-0.85, 0.3, 0.0], tint: [0.2, 0.6, 0.8], glsl: `
float mapD(vec3 q, out float id){
  float d=1e9; id=1.0; float rb=1e9;
  for(int i=0;i<3;i++){ float fi=float(i); float y=-0.45+fi*0.42;
    float wave=0.07*sin(q.x*5.0+q.z*3.0+fi*2.0)+0.03*sin(q.z*9.0+fi);
    float sheet=(abs(q.y-y-wave)-0.028)*0.6; sheet=max(sheet,max(abs(q.x)-0.95,abs(q.z)-0.6));
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
S.ser = { step: 0.6, w: 512, h: 384, half: 1.0, rot: [-0.6, 0.2, 0.0], tint: [0.2, 0.6, 0.8], glsl: `
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
S.rbc = { exp: 0.8, w: 192, h: 192, half: 1.0, rot: [0.0, 0.0, 0.0], tint: [0.9, 0.1, 0.1], glsl: `
float mapD(vec3 q, out float id){ id=1.0;
  float R=0.86; float r=length(q.xy); float s=min(r/R,1.0);
  float t=R*sqrt(max(1.0-s*s,0.0))*(0.207+2.003*s*s-1.123*s*s*s*s)*0.42;
  float dr=r-R, dy=abs(q.z)-t;
  float d=length(vec2(max(dr,0.0),max(dy,0.0)))+min(max(dr,dy),0.0);
  return d*0.7-0.02+0.004*(fbm(q*10.0)-0.5); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.55; sss=1.0; float m=fbm(p*5.0);
  float dimple=1.0-smoothstep(0.0,0.55,length(p.xy)); return mix(vec3(0.50,0.02,0.05),vec3(0.72,0.07,0.09),m)*(1.0-0.35*dimple); }` };
S.wbc = { exp: 0.72, w: 256, h: 256, half: 1.05, rot: [-0.3, 0.3, 0.0], tint: [0.9, 0.7, 0.6], glsl: `
float mapD(vec3 q, out float id){ id=1.0;
  return length(q)-0.85+0.05*fbm(q*4.5)+0.025*noise(q*16.0)-0.03; }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.55; sss=0.8; float m=fbm(p*7.0);
  vec3 cream=mix(vec3(0.78,0.70,0.56),vec3(0.90,0.82,0.68),m);
  float nd=1e9; for(int i=0;i<4;i++){ float a=float(i)*1.9+0.4; vec3 c=vec3(cos(a)*0.42,sin(a*1.3)*0.38,0.55); nd=min(nd,length(p-c)); }
  vec3 nuc=vec3(0.38,0.24,0.66); float k=smoothstep(0.50,0.22,nd);
  float gr=smoothstep(0.55,0.75,noise(p*30.0)); return mix(cream,nuc,k*0.85)*(1.0-0.12*gr); }` };
S.lymph = { exp: 0.62, w: 256, h: 256, half: 1.05, rot: [-0.3, 0.3, 0.0], tint: [0.9, 0.7, 0.6], glsl: `
float mapD(vec3 q, out float id){ id=1.0; return length(q)-0.72+0.04*fbm(q*4.5)+0.02*noise(q*16.0); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.55; sss=0.8; float m=fbm(p*7.0);
  vec3 cream=mix(vec3(0.70,0.78,0.86),vec3(0.82,0.88,0.94),m);
  float k=smoothstep(0.62,0.20,length(p-vec3(0.05,0.0,0.15))); return mix(cream,vec3(0.32,0.24,0.62),k*0.9); }` };
S.plt = { exp: 0.75, w: 128, h: 128, half: 1.0, rot: [-0.4, 0.3, 0.0], tint: [0.9, 0.6, 0.8], glsl: `
float mapD(vec3 q, out float id){ id=1.0; return sdE(q,vec3(0.85,0.36,0.6))*0.8+0.02*(fbm(q*9.0)-0.5); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.6; sss=0.8; float g=smoothstep(0.55,0.7,noise(p*22.0));
  return mix(vec3(0.85,0.60,0.80),vec3(0.55,0.30,0.55),g); }` };
S.bacterium = { exp: 0.8, w: 256, h: 128, half: 0.75, rot: [-0.3, 0.0, 0.0], tint: [0.4, 0.9, 0.3], glsl: `
float mapD(vec3 q, out float id){ id=1.0; return sdC(q,vec3(-0.45,0,0),vec3(0.45,0,0),0.25,0.25)+0.012*(noise(q*22.0)-0.5); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.8; sss=0.7; return mix(vec3(0.35,0.72,0.20),vec3(0.55,0.88,0.35),fbm(p*8.0)); }` };

S.nucleoid = { step: 0.6, exp: 0.85, w: 512, h: 512, half: 1.2, rot: [0.3, 0.2, 0.0], tint: [0.9, 0.7, 0.2], glsl: `
vec3 curveP(float t, float k){ float a=t*6.2831853;
  float r=1.0+0.22*sin(5.0*a+k)+0.12*sin(9.0*a-k*1.7); 
  return vec3(cos(a)*r*0.85+0.1*sin(3.0*a+k), sin(a)*r*0.75, 0.30*sin(3.0*a+1.0+k)+0.15*sin(7.0*a)); }
float mapD(vec3 q, out float id){ float d=1e9; id=1.0;
  for(int k=0;k<2;k++){ vec3 prev=curveP(0.0,float(k)*1.7);
    for(int i=1;i<=110;i++){ vec3 cur=curveP(float(i)/110.0,float(k)*1.7); float dd=sdC(q,prev,cur,0.045,0.045); if(dd<d){d=dd; id=1.0+float(k);} prev=cur; } }
  return d; }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.8; sss=0.8; float m=fbm(p*9.0);
  if(id<1.5) return mix(vec3(0.95,0.72,0.20),vec3(1.0,0.86,0.40),m); return mix(vec3(0.95,0.50,0.15),vec3(1.0,0.66,0.28),m); }` };
S.plasmid = { w: 128, h: 128, half: 0.9, rot: [0.9, 0.3, 0.0], tint: [1.0, 0.4, 0.5], glsl: `
float mapD(vec3 q, out float id){ id=1.0; vec3 p=q.xzy; float t=sdTorus(p,vec2(0.55,0.09)); return t+0.02*sin(atan(p.z,p.x)*14.0)*0.5; }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.85; sss=0.8; return mix(vec3(0.95,0.40,0.55),vec3(1.0,0.60,0.72),fbm(p*8.0)); }` };
S.pearl = { w: 128, h: 128, half: 1.0, rot: [0.0, 0.0, 0.0], tint: [0.8, 0.8, 0.9], glsl: `
float mapD(vec3 q, out float id){ id=1.0; return length(q)-0.8+0.02*fbm(q*6.0); }
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ gloss=0.9; sss=0.6; return mix(vec3(0.82,0.86,0.90),vec3(0.95,0.96,0.98),fbm(p*5.0)); }` };

S.membrane = { step: 0.6, exp: 0.78, w: 1200, h: 760, half: 1.25, rot: [0.15, 0.0, 0.0], tint: [0.9, 0.6, 0.2], glsl: `
const float SP=0.085;
float lipidLayer(vec3 q, float top, float off, out float id){
  vec2 c=floor(q.xz/SP+0.5+off); vec2 cc=c-off;
  vec2 j=(vec2(hash(vec3(c,1.0+top)),hash(vec3(c,2.0+top)))-0.5)*0.03; vec2 pc=cc*SP+j;
  float wob=hash(vec3(c,3.0))*0.045; float sgn=top>0.0?1.0:-1.0;
  vec3 hp=vec3(pc.x,sgn*(0.36+wob),pc.y);
  float head=length(q-hp)-0.041;
  vec3 e1=vec3(pc.x+0.014,sgn*0.02,pc.y+0.012), e2=vec3(pc.x+0.048,sgn*0.02,pc.y-0.012);
  float t1=sdC(q,hp+vec3(-0.012,0,0),e1,0.011,0.009), t2=sdC(q,hp+vec3(0.014,0,0),e2,0.011,0.009);
  id=(head<min(t1,t2))?1.0:2.0; return min(head,min(t1,t2)); }
// proteínas
float pump(vec3 p){ // Na+/K+ ATPasa
  float b=sdC(p,vec3(0,-0.4,0),vec3(0,0.42,0),0.30,0.24);
  float cy=sdE(p-vec3(0.0,-0.68,0.0),vec3(0.44,0.30,0.40));
  float ex=sdE(p-vec3(0.0,0.55,0.0),vec3(0.30,0.16,0.28));
  float be=sdC(p,vec3(0.30,0.0,0.0),vec3(0.30,0.6,0.0),0.09,0.11);
  float d=smin(smin(b,cy,0.1),smin(ex,be,0.05),0.05);
  float cav=sdC(p,vec3(0.0,-0.85,0.0),vec3(0.0,0.05,0.0),0.10,0.075);
  return max(d,-cav); }
float chan(vec3 p){ float r=0.15+0.5*p.y*p.y; float d=max(length(p.xz)-r,abs(p.y)-0.66)*0.8;
  return max(d,-(length(p.xz)-0.055)); }
float sglt(vec3 p){ float b=sdC(p,vec3(0,-0.55,0),vec3(0,0.55,0),0.27,0.27);
  float l1=sdE(p-vec3(-0.12,0.0,0.0),vec3(0.16,0.62,0.22)); float d=smin(b,sdE(p-vec3(0.0,-0.62,0.0),vec3(0.3,0.2,0.28)),0.08);
  float cav=sdC(p,vec3(0.02,-0.75,0.0),vec3(0.02,0.62,0.0),0.075,0.075); return max(d,-cav); }
float aqp(vec3 p){ float d=1e9; for(int i=0;i<4;i++){ vec2 o=vec2(i%2==0?-0.135:0.135,i<2?-0.11:0.11); vec3 pp=p-vec3(o.x,0.0,o.y);
  float t=max(length(pp.xz)-0.115,abs(pp.y)-0.52); t=max(t,-(length(pp.xz)-0.035)); d=min(d,t); } return d; }
float glyco(vec3 p, out float sid){ sid=0.0;
  float rod=sdC(p,vec3(0,0.3,0),vec3(0,0.85,0),0.05,0.045); float d=rod;
  for(int i=0;i<3;i++){ float fi=float(i); vec3 b=vec3(0.13*(fi-1.0),0.95+0.06*fi,0.05*(fi-1.0)); float s=length(p-b)-0.055; if(s<d){d=s;sid=1.0;} d=min(d,sdC(p,vec3(0,0.82,0),b,0.02,0.02)); }
  return d; }
float mapD(vec3 q, out float id){
  float d=1e9; id=1.0;
  float pumpX=-1.2, chX=-0.4, sgX=0.42, aqX=1.22, glX=1.75;
  float dp=pump(q-vec3(pumpX,0.0,0.0)); float dc=chan(q-vec3(chX,0.0,0.0)); float ds=sglt(q-vec3(sgX,0.0,0.0)); float da=aqp(q-vec3(aqX,0.0,0.0));
  float gid; float dg=glyco(q-vec3(glX,0.0,0.0),gid);
  float dprot=min(min(dp,dc),min(ds,da));
  float idl1,idl2; float l1=lipidLayer(q,1.0,0.0,idl1), l2=lipidLayer(q,-1.0,0.5,idl2);
  float dl=min(l1,l2); dl=max(dl,-(dprot-0.05));
  if(dl<d){d=dl;id=(l1<l2?idl1:idl2);}
  float bump=0.018*(fbm(q*5.0)-0.5);
  dp+=bump; dc+=bump; ds+=bump; da+=bump*0.5;
  if(dp<d){d=dp;id=3.0;} if(dc<d){d=dc;id=4.0;} if(ds<d){d=ds;id=5.0;} if(da<d){d=da;id=6.0;}
  if(dg<d){d=dg;id=7.0+gid;}
  // colesterol
  vec2 cc=floor(q.xz/0.36+0.5); float hh=hash(vec3(cc,7.0)); vec2 cp=cc*0.36+(vec2(hash(vec3(cc,8.0)),hash(vec3(cc,9.0)))-0.5)*0.1;
  float sd=(hh>0.55)?sdC(q,vec3(cp.x,-0.28+0.1*hh,cp.y),vec3(cp.x,0.28-0.1*hh,cp.y),0.028,0.024):1e9; sd=max(sd,-(dprot-0.06));
  if(sd<d){d=sd;id=9.0;}
  return max(d,q.z-0.02);
}
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss){ float m=fbm(p*7.0); gloss=0.7; sss=0.5;
  if(id<1.5) return mix(vec3(0.95,0.72,0.28),vec3(1.0,0.86,0.45),m);
  if(id<2.5) return mix(vec3(0.80,0.50,0.18),vec3(0.92,0.66,0.30),m);
  if(id<3.5){ float h=smoothstep(-0.9,0.6,p.y); return mix(vec3(0.38,0.10,0.55),vec3(0.75,0.14,0.36),h)*(0.7+0.5*m); }
  if(id<4.5) return mix(vec3(0.06,0.38,0.46),vec3(0.14,0.62,0.66),m);
  if(id<5.5) return mix(vec3(0.14,0.48,0.16),vec3(0.36,0.72,0.26),m);
  if(id<6.5) return mix(vec3(0.18,0.24,0.68),vec3(0.36,0.44,0.90),m);
  if(id<7.5) return vec3(0.85,0.30,0.55);
  if(id<8.5) return vec3(0.98,0.85,0.30);
  return vec3(0.95,0.45,0.15); }` };
module.exports = S;

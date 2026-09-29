// Utilidades GLSL comunes para renderizar orgánulos y células con ray-marching.
module.exports = `#version 300 es
precision highp float;
uniform vec2 uRes; uniform vec3 uRot; uniform float uHalf; uniform float uSeed; uniform float uStep; uniform float uExp; uniform vec3 uTint;
out vec4 fragColor;
mat2 rot2(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}
float hash(vec3 p){ p=fract(p*0.3183099+.1+uSeed*0.013); p*=17.0; return fract(p.x*p.y*p.z*(p.x+p.y+p.z)); }
float noise(vec3 x){ vec3 i=floor(x), f=fract(x); f=f*f*(3.0-2.0*f);
  return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x), mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
             mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x), mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z); }
float fbm(vec3 p){ float s=0.,a=.5; for(int i=0;i<5;i++){ s+=a*noise(p); p=p*2.03+vec3(1.7,9.2,3.1); a*=.5;} return s; }
float smin(float a,float b,float k){ float h=max(k-abs(a-b),0.0)/k; return min(a,b)-h*h*k*0.25; }
float smax(float a,float b,float k){ return -smin(-a,-b,k); }
float sdE(vec3 p, vec3 r){ float k0=length(p/r); float k1=length(p/(r*r)); return k0*(k0-1.0)/k1; }
float sdC(vec3 p, vec3 a, vec3 b, float ra, float rb){ vec3 pa=p-a, ba=b-a; float h=clamp(dot(pa,ba)/dot(ba,ba),0.,1.); return length(pa-ba*h)-mix(ra,rb,h); }
float sdBox(vec3 p, vec3 b){ vec3 q=abs(p)-b; return length(max(q,0.0))+min(max(q.x,max(q.y,q.z)),0.0); }
float sdTorus(vec3 p, vec2 t){ vec2 q=vec2(length(p.xz)-t.x,p.y); return length(q)-t.y; }
float sdCyl(vec3 p, float h, float r){ vec2 d=abs(vec2(length(p.xz),p.y))-vec2(r,h); return min(max(d.x,d.y),0.0)+length(max(d,0.0)); }
vec3 rotAll(vec3 p){ p.yz=rot2(uRot.x)*p.yz; p.xz=rot2(uRot.y)*p.xz; p.xy=rot2(uRot.z)*p.xy; return p; }
float mapD(vec3 q, out float id);
float dOnly(vec3 q){ float id; return mapD(q,id); }
vec3 calcN(vec3 p){ vec2 e=vec2(0.0025,0.0); return normalize(vec3(dOnly(p+e.xyy)-dOnly(p-e.xyy), dOnly(p+e.yxy)-dOnly(p-e.yxy), dOnly(p+e.yyx)-dOnly(p-e.yyx))); }
float calcAO(vec3 p, vec3 n){ float o=0.0,s=1.0; for(int i=0;i<5;i++){ float h=0.02+0.10*float(i)/4.0; o+=(h-dOnly(p+n*h))*s; s*=0.75;} return clamp(1.0-2.4*o,0.0,1.0); }
float softShadow(vec3 ro, vec3 rd){ float res=1.0,t=0.03; for(int i=0;i<24;i++){ float h=dOnly(ro+rd*t); res=min(res,9.0*h/t); t+=clamp(h,0.02,0.2); if(res<0.001||t>2.5)break;} return clamp(res,0.0,1.0); }
vec3 env(vec3 r){
  float top=smoothstep(-0.3,1.0,r.y);
  vec3 c=mix(vec3(0.03,0.03,0.04),vec3(0.30,0.32,0.36),top);
  c+=vec3(3.0,2.9,2.8)*smoothstep(0.86,0.93,dot(r,normalize(vec3(-0.5,0.7,0.7))));
  c+=vec3(1.0,1.2,1.5)*smoothstep(0.88,0.95,dot(r,normalize(vec3(0.8,0.2,0.5))));
  c+=vec3(1.4,0.9,0.7)*smoothstep(0.90,0.96,dot(r,normalize(vec3(0.0,-0.6,0.8))));
  return c;
}
vec3 aces(vec3 x){ return clamp((x*(2.51*x+0.03))/(x*(2.43*x+0.59)+0.14),0.0,1.0); }
// material: albedo, gloss (0..1), subsurface tint strength
vec3 albedoOf(float id, vec3 p, vec3 n, out float gloss, out float sss);
vec3 shade(vec3 p, vec3 rd, float id){
  vec3 n=calcN(p);
  float gloss, sss; vec3 alb=albedoOf(id,p,n,gloss,sss);
  float ao=calcAO(p,n);
  vec3 L1=normalize(vec3(-0.5,0.75,0.75)), L2=normalize(vec3(0.85,0.1,0.5)), L3=normalize(vec3(-0.2,-0.7,0.6));
  float sh=softShadow(p+n*0.02,L1);
  float d1=clamp((dot(n,L1)+0.3)/1.3,0.0,1.0), d2=clamp((dot(n,L2)+0.2)/1.2,0.0,1.0), d3=clamp(dot(n,L3),0.0,1.0);
  vec3 col=alb*(vec3(1.1,1.0,0.95)*d1*(0.25+0.75*sh)+vec3(0.30,0.38,0.55)*d2*0.4+vec3(0.55,0.35,0.30)*d3*0.2+vec3(0.06,0.06,0.07));
  float fres=0.04+0.96*pow(1.0-clamp(dot(n,-rd),0.0,1.0),5.0);
  col+=alb*uTint*sss*pow(1.0-abs(dot(n,-rd)),2.0)*0.9;
  col*=mix(0.35,1.0,ao);
  vec3 nm=normalize(n+0.06*(vec3(noise(p*40.0),noise(p*40.0+9.0),noise(p*40.0+17.0))-0.5)*(1.0-gloss*0.6));
  vec3 refl=reflect(rd,nm);
  col+=env(refl)*(0.10+fres*0.9)*gloss*mix(0.5,1.0,ao);
  vec3 hv=normalize(L1-rd); col+=vec3(1.0,0.95,0.9)*pow(max(dot(nm,hv),0.0),80.0)*1.2*gloss*sh;
  return col;
}
void main(){
  vec2 uv=(gl_FragCoord.xy-0.5*uRes)/uRes.y;
  vec2 sc=uv*2.0*uHalf;
  vec3 ro=vec3(sc,5.0), rd=vec3(0.0,0.0,-1.0);
  float t=2.0, mind=1e9, id=0.0; bool hit=false; vec3 p;
  for(int i=0;i<160;i++){
    p=ro+rd*t; float dd=mapD(rotAll(p),id); mind=min(mind,dd);
    if(dd<0.0012){hit=true;break;}
    t+=dd*uStep; if(t>8.0)break;
  }
  float px=2.0*2.0*uHalf/uRes.y;
  float alpha=hit?1.0:(1.0-smoothstep(0.0,px*1.2,mind));
  if(alpha<=0.001){ fragColor=vec4(0.0); return; }
  vec3 q=rotAll(p);
  // dirección del rayo en espacio local para reflejos coherentes
  vec3 rdl=rotAll(rd+vec3(0.0)); 
  vec3 col=shade(q,rdl,id);
  col=aces(col*uExp); col=pow(col,vec3(1.0/2.2));
  fragColor=vec4(col*alpha,alpha);
}
`;

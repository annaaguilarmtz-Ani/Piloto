// Genera los recursos JSON del corazón: node build.js
const fs = require('fs');
const out = __dirname + '/../../app/src/main/res/raw';
fs.mkdirSync(out, { recursive: true });
const planes = { heart_sagittal: './sagittal', heart_coronal: './coronal', heart_axial4: './axial4', heart_axialhigh: './axialhigh', heart_exterior: './exterior' };
for (const [name, mod] of Object.entries(planes)) {
  const j = require(mod)().json();
  fs.writeFileSync(`${out}/${name}.json`, j);
  console.log(name, (j.length / 1024).toFixed(0) + ' KB');
}

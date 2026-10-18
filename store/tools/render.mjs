// Composes Play Store frames (1080×1920) and the feature graphic (1024×500) with headless Chromium.
// usage: node render.mjs <captures/screenshots> <out dir>
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';

const [, , shots, outDir] = process.argv;
const here = path.dirname(new URL(import.meta.url).pathname);
const fonts = path.resolve(here, '../../app/src/main/res/font');
const frames = JSON.parse(fs.readFileSync(path.join(here, 'frames.json'), 'utf8'));
fs.mkdirSync(outDir, { recursive: true });

const face = (w, f) => `@font-face{font-family:J;font-weight:${w};src:url(file://${fonts}/plus_jakarta_sans_${f}.ttf)}`;
const css = `
${face(400, 'regular')}${face(600, 'semibold')}${face(700, 'bold')}${face(800, 'extrabold')}
*{margin:0;box-sizing:border-box}
body{width:1080px;height:1920px;overflow:hidden;font-family:J;position:relative}
.t{position:absolute;left:80px;right:80px;top:118px;text-align:center}
h1{font-weight:800;font-size:92px;line-height:1.02;letter-spacing:-.035em;text-wrap:balance}
p{margin-top:26px;font-weight:600;font-size:40px;opacity:.72;letter-spacing:-.01em}
.phone{position:absolute;left:50%;top:478px;width:648px;height:1432px;margin-left:-324px;border-radius:84px;
  background:#0b0b12;padding:16px;box-shadow:0 60px 90px -40px rgba(20,24,70,.55),0 0 0 3px rgba(255,255,255,.35) inset}
.phone img{width:100%;height:100%;border-radius:70px;display:block;object-fit:cover}
.b{position:absolute;border-radius:50%;border:5px solid rgba(255,255,255,.75);background:rgba(255,255,255,.25)}
`;
const bubbles = (dark) => [[70, 640, 58], [960, 760, 40], [120, 1200, 34], [930, 1420, 66], [60, 1700, 44], [990, 1100, 26]]
  .map(([x, y, s]) => `<div class="b" style="left:${x - s / 2}px;top:${y}px;width:${s}px;height:${s}px;${dark ? 'border-color:rgba(160,170,255,.5);background:rgba(120,130,255,.12)' : ''}"></div>`).join('');

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
for (const f of frames) {
  const img = path.join(shots, f.src + '.png');
  if (!fs.existsSync(img)) { console.warn('missing', img); continue; }
  const ink = f.dark ? '#ffffff' : '#1f2350';
  const html = `<html><head><style>${css}</style></head><body style="background:radial-gradient(120% 70% at 50% 0%,rgba(255,255,255,${f.dark ? '.06' : '.55'}),rgba(255,255,255,0) 60%),${f.bg};color:${ink}">
    ${bubbles(f.dark)}
    <div class="t"><h1>${f.title.replace(/(\S+)$/, `<span style="color:${f.accent}">$1</span>`)}</h1><p>${f.sub}</p></div>
    <div class="phone"><img src="file://${path.resolve(img)}"></div></body></html>`;
  fs.writeFileSync(path.join(outDir, '_page.html'), html);
  await page.goto('file://' + path.resolve(outDir, '_page.html'), { waitUntil: 'load' });
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: path.join(outDir, f.out + '.png') });
  console.log('wrote', f.out);
}

// Feature graphic: logo + tagline left, three mascots from the pool shot cropped right.
const pool = path.join(shots, '01_pool.png');
if (fs.existsSync(pool)) {
  await page.setViewportSize({ width: 1024, height: 500 });
  const fg = `<html><head><style>${css}
    body{width:1024px;height:500px;background:linear-gradient(135deg,#6e76f5,#4a52d8);color:#fff}
    .l{position:absolute;left:64px;top:112px;width:470px}
    .l h1{font-size:86px}.l p{font-size:30px;opacity:.9;margin-top:18px}
    .r{position:absolute;right:-20px;top:-40px;width:560px;height:580px;border-radius:0;overflow:hidden;
       -webkit-mask-image:linear-gradient(90deg,transparent,#000 9%)}
    .r img{position:absolute;width:600px;left:20px;top:-300px}
  </style></head><body>
    <div class="r"><img src="file://${path.resolve(pool)}"></div>
    <div class="l"><h1>Sippo</h1><p>The cute water tracker that cheers every sip</p></div>
  </body></html>`;
  fs.writeFileSync(path.join(outDir, '_page.html'), fg);
  await page.goto('file://' + path.resolve(outDir, '_page.html'), { waitUntil: 'load' });
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: path.join(outDir, 'feature_graphic.png') });
  console.log('wrote feature_graphic');
}
await browser.close();

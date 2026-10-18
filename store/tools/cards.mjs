// Renders video overlay cards: background + caption with a transparent hole where the screen video goes.
// usage: node cards.mjs <video.json> <out dir> <screenshot for outro>
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';

const [, , specPath, outDir, outroShot] = process.argv;
const here = path.dirname(new URL(import.meta.url).pathname);
const fonts = path.resolve(here, '../../app/src/main/res/font');
const spec = JSON.parse(fs.readFileSync(specPath, 'utf8'));
fs.mkdirSync(outDir, { recursive: true });
const face = (w, f) => `@font-face{font-family:J;font-weight:${w};src:url(file://${fonts}/plus_jakarta_sans_${f}.ttf)}`;

// Screen hole geometry (video is 540×1200, ratio 0.45).
export const LAYOUTS = {
  land: { w: 1920, h: 1080, hole: { x: 1236, y: 70, w: 423, h: 940 } },
  port: { w: 1080, h: 1920, hole: { x: 248, y: 520, w: 585, h: 1300 } },
};
fs.writeFileSync(path.join(outDir, 'layouts.json'), JSON.stringify(LAYOUTS));

const base = (L, bg, ink) => `${face(400,'regular')}${face(600,'semibold')}${face(800,'extrabold')}
*{margin:0;box-sizing:border-box}body{width:${L.w}px;height:${L.h}px;overflow:hidden;font-family:J;color:${ink};background:transparent}
.bg{position:absolute;inset:0;background:radial-gradient(90% 70% at 30% 10%,rgba(255,255,255,.45),rgba(255,255,255,0) 60%),${bg};
  -webkit-mask: linear-gradient(#000,#000) , url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='${L.w}' height='${L.h}'><rect x='${L.hole.x}' y='${L.hole.y}' width='${L.hole.w}' height='${L.hole.h}' rx='${Math.round(L.hole.w * .1)}' fill='black'/></svg>");
  -webkit-mask-composite: xor;mask-composite: exclude}
.bezel{position:absolute;left:${L.hole.x - 14}px;top:${L.hole.y - 14}px;width:${L.hole.w + 28}px;height:${L.hole.h + 28}px;
  border-radius:${Math.round(L.hole.w * .1) + 14}px;border:14px solid #0b0b12;box-shadow:0 50px 80px -30px rgba(20,24,70,.55)}
.pill{display:inline-flex;align-items:center;gap:10px;background:#1f2350;color:#ffc93d;font-weight:800;font-size:30px;padding:10px 22px;border-radius:40px;letter-spacing:.04em}
h1{font-weight:800;letter-spacing:-.035em;line-height:1.02;text-wrap:balance}p{font-weight:600;opacity:.72}
.b{position:absolute;border-radius:50%;border:5px solid rgba(255,255,255,.7);background:rgba(255,255,255,.22)}`;

const browser = await chromium.launch();
const page = await browser.newPage();
async function render(L, html, file) {
  await page.setViewportSize({ width: L.w, height: L.h });
  const tmp = path.join(outDir, '_page.html');
  fs.writeFileSync(tmp, html);
  await page.goto('file://' + path.resolve(tmp), { waitUntil: 'load' });
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: file, omitBackground: true });
}
const bubbles = (L) => (L.w > L.h ? [[90, 860, 50], [700, 920, 36], [1100, 120, 30], [1780, 980, 46], [1790, 160, 28]] : [[70, 1500, 50], [990, 600, 36], [960, 1700, 46], [80, 420, 30]])
  .map(([x, y, s]) => `<div class="b" style="left:${x}px;top:${y}px;width:${s}px;height:${s}px"></div>`).join('');

for (const [key, L] of Object.entries(LAYOUTS)) {
  const land = key === 'land';
  for (const [i, s] of spec.segments.entries()) {
    const ink = s.dark ? '#fff' : '#1f2350';
    const text = land
      ? `<div style="position:absolute;left:150px;top:0;bottom:0;width:900px;display:flex;flex-direction:column;justify-content:center;gap:28px">
           ${s.plus ? '<div><span class="pill">SIPPO+</span></div>' : ''}
           <h1 style="font-size:104px">${s.title}</h1><p style="font-size:44px">${s.sub}</p></div>`
      : `<div style="position:absolute;left:70px;right:70px;top:${s.plus ? 96 : 130}px;text-align:center;display:flex;flex-direction:column;gap:22px;align-items:center">
           ${s.plus ? '<span class="pill">SIPPO+</span>' : ''}
           <h1 style="font-size:84px">${s.title}</h1><p style="font-size:38px">${s.sub}</p></div>`;
    await render(L, `<html><head><style>${base(L, s.bg, ink)}</style></head><body><div class="bg"></div>${bubbles(L)}<div class="bezel"></div>${text}</body></html>`,
      path.join(outDir, `${key}_${String(i).padStart(2, '0')}.png`));
  }
  // Intro and outro are full cards (no hole).
  const card = (inner, bg) => `<html><head><style>${base(L, bg, '#fff')} .bg{-webkit-mask:none;mask:none}</style></head><body><div class="bg"></div>${bubbles(L)}
    <div style="position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;gap:26px;padding:80px">${inner}</div></body></html>`;
  const logo = `<div style="display:flex;align-items:center;gap:18px;font-weight:800;font-size:${land ? 150 : 130}px;letter-spacing:-.04em">
      <svg width="${land ? 120 : 104}" viewBox="0 0 24 24"><path fill="#fff" d="M12 2.5S5 10.2 5 14.8C5 18.8 8.1 21.5 12 21.5S19 18.8 19 14.8C19 10.2 12 2.5 12 2.5Z"/></svg>sippo</div>`;
  await render(L, card(`${logo}<p style="font-size:${land ? 54 : 50}px;opacity:.92">The cute water tracker that cheers every sip</p>`, 'linear-gradient(135deg,#6e76f5,#4a52d8)'), path.join(outDir, `${key}_intro.png`));
  const shot = outroShot && fs.existsSync(outroShot) ? `<img src="file://${path.resolve(outroShot)}" style="width:${land ? 760 : 900}px;margin:-10px 0 -20px;border-radius:36px;box-shadow:0 30px 60px -20px rgba(0,0,0,.35)">` : '';
  await render(L, card(`${logo}${shot}<p style="font-size:${land ? 50 : 46}px;opacity:.95">Free on Google Play · Sippo+ €2.99 once</p>
    <p style="font-size:${land ? 34 : 32}px;opacity:.75">No account · no ads · your data stays on your phone</p>`, 'linear-gradient(135deg,#6e76f5,#4a52d8)'), path.join(outDir, `${key}_outro.png`));
  console.log('cards', key);
}
await browser.close();

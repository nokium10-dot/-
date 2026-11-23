from pathlib import Path

p = Path("pip/src/main/assets/pip/index.html")
s = p.read_text(encoding="utf-8")

css = """
  * { -webkit-tap-highlight-color: rgba(0,0,0,0) !important; -webkit-touch-callout:none; user-select:none; -webkit-user-select:none; }
  html, body { width:100%; height:100%; min-height:0; overflow:hidden; background:#000 !important; overscroll-behavior:none; }
  body { display:block; background:#000 !important; }
  #view { position:absolute; inset:0; width:100%; height:100%; min-width:0; background:#000; touch-action:none; }
  #view canvas { display:block; width:100%; height:100%; outline:none; touch-action:none; -webkit-tap-highlight-color:rgba(0,0,0,0); }

  :root { --alt-accent:#91a7ff; --alt-accent2:#75e5d0; --alt-panel:rgba(11,13,25,.76); }
  body::after { content:""; position:fixed; inset:0; z-index:8; pointer-events:none;
    background:radial-gradient(ellipse at 50% 46%, transparent 36%, rgba(20,25,52,.18) 76%, rgba(0,0,0,.56) 100%); }
  #pip-alt-hud { position:fixed; z-index:12; top:max(14px,env(safe-area-inset-top)); left:50%;
    transform:translateX(-50%); display:flex; flex-direction:column; align-items:center; gap:3px;
    width:max-content; max-width:54vw; pointer-events:none; text-align:center; }
  #pip-alt-hud .brand { color:#f4f5ff; font:700 11px/1.2 system-ui,sans-serif; letter-spacing:3px;
    text-shadow:0 2px 12px #000; white-space:nowrap; }
  #pip-alt-hud .sub { color:#a9b3d5; font:9px/1.2 system-ui,sans-serif; letter-spacing:1.5px; }
  #pip-alt-hud .online { display:inline-flex; align-items:center; gap:5px; margin-top:4px;
    padding:4px 9px; border:1px solid #91a7ff35; border-radius:20px; background:#090c1bbd;
    color:#b9c8ff; font:9px system-ui,sans-serif; letter-spacing:1px; }
  #pip-alt-hud .online i { width:5px; height:5px; border-radius:50%; background:#75e5d0;
    box-shadow:0 0 9px #75e5d0; animation:alt-pulse 1.8s ease-in-out infinite; }
  @keyframes alt-pulse { 50% { opacity:.35; box-shadow:0 0 2px #75e5d0; } }
  #pip-alt-status { position:fixed; z-index:12; left:13px; bottom:calc(max(17px,env(safe-area-inset-bottom)) + 68px);
    display:flex; flex-direction:column; gap:3px; padding:10px 12px; border:1px solid #a4b4ff2b;
    border-radius:14px; background:linear-gradient(135deg,rgba(14,18,39,.88),rgba(6,8,18,.78));
    box-shadow:0 8px 26px #0008; pointer-events:none; backdrop-filter:blur(14px); }
  #pip-alt-status strong { color:#f1f3ff; font:650 10px system-ui,sans-serif; letter-spacing:1.2px; }
  #pip-alt-status span { color:#9ca9cb; font:9px system-ui,sans-serif; }
  #pip-alt-status .meter { width:78px; height:3px; border-radius:4px; margin-top:4px; background:#252b45; overflow:hidden; }
  #pip-alt-status .meter i { display:block; height:100%; width:82%; background:linear-gradient(90deg,var(--alt-accent),var(--alt-accent2)); border-radius:4px; }
  #pip-settings { background:radial-gradient(ellipse at 80% 0%,#171c3a 0%,#080910 48%,#030407 100%); }
  #pip-settings .settings-head h3 { letter-spacing:.3px; }
  #pip-settings .settings-card, #pip-mod-panel .mod-row { border-color:#879cff30; background:linear-gradient(145deg,#11162a,#090b13); }
  #pip-settings button, #pip-mod-panel button { transition:transform .16s ease,border-color .16s ease,background .16s ease; }
  #pip-settings button:active, #pip-mod-panel button:active { transform:scale(.97); }
  #pip-settings .swatch:focus-visible, #pip-settings button:focus-visible { outline:2px solid var(--alt-accent); outline-offset:2px; }
  #pip-actions-trigger { border-color:#91a7ff66; background:linear-gradient(145deg,#20294b,#101426);
    box-shadow:0 0 0 5px #91a7ff10,0 8px 30px #000c; }
  #pip-actions-sheet { border-color:#91a7ff44; background:linear-gradient(160deg,#11162b,#06070d 70%); }
  #pip-actions-sheet button { border-color:#879cff35; background:#171d34; }
  #pip-actions-sheet button:active { background:#27345b; }

  #hint, #panel { display:none !important; }
  #pip-settings-trigger {
    position:fixed; z-index:20; top:max(12px, env(safe-area-inset-top)); left:12px;
    width:44px; height:44px; border-radius:15px; border:1px solid #ffffff24;
    color:#fff; background:#101014; font-size:22px; display:grid; place-items:center;
    box-shadow:0 5px 24px #0008;
  }
  #pip-settings {
    display:none; position:fixed; z-index:30; inset:0; box-sizing:border-box;
    padding:calc(env(safe-area-inset-top) + 18px) 18px calc(env(safe-area-inset-bottom) + 20px);
    background:#000; color:#f4f4f8; font:15px system-ui,sans-serif; overflow-y:auto;
  }
  #pip-settings.open { display:block; animation:pip-settings-in 430ms cubic-bezier(.2,.9,.2,1.12) both; }
  #pip-settings.closing { display:block; pointer-events:none; animation:pip-settings-out 230ms cubic-bezier(.4,0,1,1) both; }
  @keyframes pip-settings-in {
    0% { opacity:0; transform:translateY(26px) scale(.965); filter:blur(8px); }
    68% { opacity:1; transform:translateY(-3px) scale(1.008); filter:blur(0); }
    100% { opacity:1; transform:translateY(0) scale(1); filter:blur(0); }
  }
  @keyframes pip-settings-out {
    0% { opacity:1; transform:translateY(0) scale(1); filter:blur(0); }
    100% { opacity:0; transform:translateY(18px) scale(.98); filter:blur(5px); }
  }
  @media (prefers-reduced-motion:reduce) {
    #pip-settings.open, #pip-settings.closing { animation-duration:1ms; }
  }
  #pip-settings .settings-head { display:flex; align-items:center; gap:14px; margin-bottom:28px; }
  #pip-settings h3 { margin:0; font-size:22px; font-weight:650; }
  #pip-settings .back { width:42px; height:42px; border-radius:14px; border:1px solid #292932; color:#fff; background:#101014; font-size:22px; }
  #pip-settings .section-title { color:#aeb2c2; margin:20px 0 12px; font-size:13px; }
  #pip-settings .case-colors { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }
  #pip-settings button { min-height:44px; border-radius:13px; border:1px solid #292932; color:#f4f4f8; background:#101014; }
  #pip-settings .swatch { display:flex; align-items:center; justify-content:flex-start; gap:9px; padding:0 12px; }
  #pip-settings .dot { width:17px; height:17px; border-radius:50%; border:1px solid #ffffff66; flex:none; }
  #pip-settings .assist, #pip-settings .reset-zoom { width:100%; margin-top:12px; }
  #pip-mod-repo { width:100%; margin-top:12px; }
  #pip-mod-panel { display:none; position:fixed; z-index:45; inset:0; box-sizing:border-box; padding:calc(env(safe-area-inset-top) + 18px) 16px calc(env(safe-area-inset-bottom) + 18px); background:#050507; color:#f4f4f8; font:14px system-ui,sans-serif; overflow-y:auto; }
  #pip-mod-panel.open { display:block; }
  #pip-mod-panel .mod-head { display:flex; align-items:center; gap:12px; margin-bottom:18px; }
  #pip-mod-panel h3 { margin:0; font-size:21px; }
  #pip-mod-panel .mod-row { padding:14px; margin:9px 0; border:1px solid #292932; border-radius:15px; background:#101014; }
  #pip-mod-panel .mod-row strong { display:block; margin-bottom:5px; }
  #pip-mod-panel .mod-row p { margin:0 0 10px; color:#aeb2c2; line-height:1.4; font-size:12px; }
  #pip-mod-panel .mod-toggle { min-height:36px; padding:0 14px; }
  #pip-mod-panel .mod-toggle.active { border-color:#70cfa6; background:#14251e; }
  #pip-mod-status { color:#969aaa; font-size:12px; margin:8px 0 14px; }
  #pip-mod-panel .mod-progress { height:5px; border-radius:8px; background:#292932; overflow:hidden; margin:9px 0 5px; display:none; }
  #pip-mod-panel .mod-progress.visible { display:block; }
  #pip-mod-panel .mod-progress i { display:block; width:0; height:100%; border-radius:8px; background:linear-gradient(90deg,#6a8cff,#72e0c0); transition:width .16s ease; }
  #pip-mod-panel .mod-progress-label { min-height:15px; color:#9da3b5; font-size:11px; margin-bottom:7px; }
  #pip-settings .hint { color:#777b89; font-size:12px; margin-top:18px; line-height:1.5; }
  #pip-settings .settings-card { padding:15px; border:1px solid #24242c; border-radius:17px; background:#08080b; }
  #pip-actions-trigger {
    position:fixed; z-index:21; left:50%; bottom:max(16px, env(safe-area-inset-bottom));
    transform:translateX(-50%); width:54px; height:54px; border-radius:19px;
    border:1px solid #ffffff2b; background:#15151b; color:#fff; font-size:25px;
    display:grid; place-items:center; box-shadow:0 6px 28px #000b;
  }
  #pip-actions-sheet {
    display:none; position:fixed; z-index:25; left:0; right:0; bottom:0;
    max-height:72vh; overflow-y:auto; box-sizing:border-box;
    padding:18px 16px calc(env(safe-area-inset-bottom) + 88px);
    border:1px solid #292932; border-bottom:0; border-radius:24px 24px 0 0;
    background:#08080b; color:#f4f4f8; font:14px system-ui,sans-serif;
    box-shadow:0 -14px 45px #000b;
  }
  #pip-actions-sheet.open { display:block; }
  #pip-actions-sheet .sheet-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; }
  #pip-actions-sheet h3 { margin:0; font-size:17px; }
  #pip-actions-close { width:36px; height:36px; border-radius:12px; font-size:21px; }
  #pip-actions-sheet .action-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:8px; }
  #pip-actions-sheet button { min-height:44px; border-radius:12px; border:1px solid #292932; background:#15151b; color:#f4f4f8; }
  #pip-actions-sheet .action-grid button { padding:8px 3px; font-size:12px; }
  #pip-actions-sheet .section-title { color:#aeb2c2; margin:18px 0 9px; font-size:12px; }
  #pip-moods-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:7px; }
  #pip-moods-grid button { padding:7px 4px; font-size:12px; min-width:0; }
  #pip-moods-grid button.cur { border-color:#a879ff; background:#21152f; }
  @media (max-width:380px) {
    #pip-actions-sheet .action-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
    #pip-moods-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
  }
"""
s = s.replace("</style>", css + "\n</style>", 1)

markup = """

<div id="pip-alt-hud" aria-hidden="true">
  <div class="brand">PIP / ALTVER</div>
  <div class="sub">DIGITAL COMPANION SYSTEM</div>
  <div class="online"><i></i> CORE ONLINE</div>
</div>
<div id="pip-alt-status" aria-hidden="true">
  <strong>ALTVER OS · READY</strong>
  <span>Связь с Пипом активна</span>
  <div class="meter"><i></i></div>
</div>
<button id="pip-settings-trigger" aria-label="Настройки" title="Настройки">⚙</button>
<section id="pip-settings" aria-label="Настройки Пипа">
  <div class="settings-head">
    <button class="back" id="pip-settings-back" aria-label="Назад">‹</button>
    <h3>Настройки</h3>
  </div>
  <div class="settings-card">
    <div>Цвет корпуса часов</div>
    <div class="section-title">Выберите оформление</div>
    <div class="case-colors">
      <button class="swatch" data-case="#c4c6cc" data-polish="#e0e1e5"><i class="dot" style="background:#c4c6cc"></i>Серый</button>
      <button class="swatch" data-case="#252833" data-polish="#555b6d"><i class="dot" style="background:#252833"></i>Графит</button>
      <button class="swatch" data-case="#17191e" data-polish="#454954"><i class="dot" style="background:#17191e"></i>Чёрный</button>
      <button class="swatch" data-case="#7139a8" data-polish="#b77af0"><i class="dot" style="background:#7139a8"></i>Фиолетовый</button>
      <button class="swatch" data-case="#126b68" data-polish="#56c8b9"><i class="dot" style="background:#126b68"></i>Бирюзовый</button>
      <button class="swatch" data-case="#9d244d" data-polish="#ed79a0"><i class="dot" style="background:#9d244d"></i>Розовый</button>
    </div>
  </div>
  <button id="pip-mod-repo">🧩 Репозиторий модов</button>
  <button class="assist" id="pip-assistant-role">Выбрать Пипа помощником</button>
  <button class="reset-zoom" id="pip-reset-zoom">Сбросить размер часов</button>
  <div class="hint">Сведите или разведите два пальца на самих часах, чтобы изменить их размер.</div>
</section>
<section id="pip-mod-panel" aria-label="Репозиторий модов">
  <div class="mod-head"><button class="back" id="pip-mod-back">‹</button><h3>Репозиторий модов</h3></div>
  <div id="pip-mod-status">Загрузка каталога…</div>
  <div id="pip-mod-list"></div>
</section>
<button id="pip-actions-trigger" aria-label="Действия и эмоции Пипа" title="Действия и эмоции">☻</button>
<section id="pip-actions-sheet" aria-label="Действия и эмоции Пипа">
  <div class="sheet-head"><h3>Пип</h3><button id="pip-actions-close" aria-label="Закрыть">×</button></div>
  <div class="section-title">Действия</div>
  <div class="action-grid">
    <button id="pip-action-feed">🍎<br>Покормить</button>
    <button id="pip-action-play">🎾<br>Поиграть</button>
    <button id="pip-action-pet">♡<br>Погладить</button>
    <button id="pip-action-poke">☝<br>Тык</button>
  </div>
  <div class="section-title">Эмоции</div>
  <div id="pip-moods-grid"></div>
</section>
"""
s = s.replace('<div id="panel">', markup + '\n<div id="panel">', 1)

# Keep the HTML canvas at full size; pinch zoom now changes the camera distance.
s = s.replace(
    '#view canvas { width:100%; height:100%; transform:scale(var(--pip-watch-scale, 1)); transform-origin:50% 50%; }',
    '#view canvas { display:block; width:100%; height:100%; outline:none; touch-action:none; }'
)

# Apply case colors and retain the simulator bridge.
needle = "window.sim = { M, tgt, cur, dev, advance };"
replacement = """window.setPipCaseColor = (bodyColor, trimColor) => {
  brushed.color.set(bodyColor);
  polished.color.set(trimColor);
  store.set('keypet.caseColor', bodyColor);
  store.set('keypet.trimColor', trimColor);
};
const savedBody = store.get('keypet.caseColor');
const savedTrim = store.get('keypet.trimColor');
if (savedBody && savedTrim) { brushed.color.set(savedBody); polished.color.set(savedTrim); }
window.sim = { M, tgt, cur, dev, advance, setCaseColor: window.setPipCaseColor };"""
if needle not in s:
    raise SystemExit("Could not find simulator export hook")
s = s.replace(needle, replacement, 1)

# Put pinch zoom on the 3D renderer: changing camDist resizes the physical watch model only.
needle = """const canvas = renderer.domElement;
canvas.addEventListener('pointerdown', e => {
  canvas.setPointerCapture(e.pointerId);"""
replacement = """const canvas = renderer.domElement;
let pinchStartDistance = 0, pinchStartCamDist = camDist, pinching = false;
const touchDistance = touches => Math.hypot(
  touches[0].clientX - touches[1].clientX,
  touches[0].clientY - touches[1].clientY
);
canvas.addEventListener('touchstart', e => {
  if (e.touches.length === 2) {
    e.preventDefault();
    pinching = true;
    pinchStartDistance = touchDistance(e.touches);
    pinchStartCamDist = camDist;
    if (touch) M._sim_touch(touch[0], touch[1], 0);
    touch = null;
    drag = null;
  }
}, { passive:false });
canvas.addEventListener('touchmove', e => {
  if (pinching && e.touches.length === 2) {
    e.preventDefault();
    const ratio = touchDistance(e.touches) / Math.max(1, pinchStartDistance);
    camDist = THREE.MathUtils.clamp(pinchStartCamDist / ratio, 90, 320);
  }
}, { passive:false });
const endPinch = e => {
  if (e.touches.length < 2) {
    pinching = false;
    pinchStartDistance = 0;
  }
};
canvas.addEventListener('touchend', endPinch, { passive:false });
canvas.addEventListener('touchcancel', endPinch, { passive:false });
canvas.addEventListener('contextmenu', e => e.preventDefault());
canvas.addEventListener('pip-reset-zoom', () => { camDist = 175; });
canvas.addEventListener('pointerdown', e => {
  if (pinching) return;
  canvas.setPointerCapture(e.pointerId);"""
if needle not in s:
    raise SystemExit("Could not find renderer pointer hook")
s = s.replace(needle, replacement, 1)
s = s.replace(
    "canvas.addEventListener('pointermove', e => {\n  if (touch && !drag) {",
    "canvas.addEventListener('pointermove', e => {\n  if (pinching) return;\n  if (touch && !drag) {",
    1
)

# Add each original simulator mood to the new bottom sheet as a working button.
needle = "  moodsEl.append(b);"
replacement = """  moodsEl.append(b);
  const pipMoodGrid = document.getElementById('pip-moods-grid');
  if (pipMoodGrid) {
    const moodButton = b.cloneNode(true);
    moodButton.onclick = () => M._sim_set_mood(i);
    pipMoodGrid.append(moodButton);
  }"""
if needle not in s:
    raise SystemExit("Could not find mood button creation")
s = s.replace(needle, replacement, 1)
s = s.replace(
    "for (const b of moodsEl.children) b.classList.toggle('cur', b.dataset.mood === st.mood);",
    "for (const b of document.querySelectorAll('#moods button, #pip-moods-grid button')) b.classList.toggle('cur', b.dataset.mood === st.mood);",
    1
)

# Wire the new bottom sheet to the same simulator actions as the original controls.
needle = "muteBtn.onclick = () => M._sim_volume(M._sim_get_volume() > 0 ? 0 : 0.8);"
replacement = """muteBtn.onclick = () => M._sim_volume(M._sim_get_volume() > 0 ? 0 : 0.8);

const pipSheet = document.getElementById('pip-actions-sheet');
document.getElementById('pip-actions-trigger').onclick = () => pipSheet.classList.toggle('open');
document.getElementById('pip-actions-close').onclick = () => pipSheet.classList.remove('open');
document.getElementById('pip-action-feed').onclick = () => M._sim_action(0);
document.getElementById('pip-action-play').onclick = () => M._sim_action(1);
document.getElementById('pip-action-poke').onclick = () => {
  M._sim_touch(233, 233, 1);
  setTimeout(() => M._sim_touch(233, 233, 0), 90);
};
document.getElementById('pip-action-pet').onclick = () => {
  M._sim_touch(150, 240, 1);
  let step = 0;
  const timer = setInterval(() => {
    step++;
    M._sim_touch(step % 2 ? 320 : 150, 240 + (step % 3) * 5, 1);
    if (step >= 16) {
      clearInterval(timer);
      M._sim_touch(150, 240, 0);
    }
  }, 65);
};
document.addEventListener('pointerdown', e => {
  if (!pipSheet.contains(e.target) && e.target.id !== 'pip-actions-trigger') {
    pipSheet.classList.remove('open');
  }
});"""
if needle not in s:
    raise SystemExit("Could not find audio controls hook")
s = s.replace(needle, replacement, 1)

footer = """
<script>
(() => {
  const trigger = document.getElementById('pip-settings-trigger');
  const panel = document.getElementById('pip-settings');
  const closeSettings = () => {
    if (!panel.classList.contains('open')) return;
    panel.classList.remove('open');
    panel.classList.add('closing');
    const finish = () => panel.classList.remove('closing');
    panel.addEventListener('animationend', finish, { once:true });
    setTimeout(finish, 270);
  };
  trigger.addEventListener('click', () => {
    panel.classList.remove('closing');
    panel.classList.add('open');
  });
  document.getElementById('pip-settings-back').addEventListener('click', closeSettings);
  document.querySelectorAll('[data-case]').forEach(button => {
    button.addEventListener('click', () => {
      if (window.sim && window.sim.setCaseColor) {
        window.sim.setCaseColor(button.dataset.case, button.dataset.polish);
      }
    });
  });
  document.getElementById('pip-assistant-role').addEventListener('click', () => {
    if (window.PipAndroid && window.PipAndroid.chooseAssistant) window.PipAndroid.chooseAssistant();
  });
  document.getElementById('pip-reset-zoom').addEventListener('click', () => {
    const canvas = document.querySelector('#view canvas');
    if (canvas) canvas.dispatchEvent(new CustomEvent('pip-reset-zoom'));
  });
})();
</script>
"""
loader = r"""
<script>
(() => {
  const MANIFEST='https://raw.githubusercontent.com/nokium10-dot/-/main/pip-mods/manifest.json';
  const BASE='https://raw.githubusercontent.com/nokium10-dot/-/main/pip-mods/';
  const panel=document.getElementById('pip-mod-panel');
  const list=document.getElementById('pip-mod-list');
  const status=document.getElementById('pip-mod-status');
  const readActive=()=>{try{const v=JSON.parse(localStorage.getItem('pip.mods.active')||'[]');return Array.isArray(v)?v.filter(x=>typeof x==='string'):[];}catch(_){return[];}};
  const active=new Set(readActive());
  const loaded=new Set(),cleanups=new Map(),registrations=new Map(),screenLayers=new Map();
  const persist=()=>{try{localStorage.setItem('pip.mods.active',JSON.stringify([...active]));}catch(_){status.textContent='Не удалось сохранить список модов на устройстве.';}};
  window.PipMods={
    has:id=>registrations.has(id),
    register:(id,factory)=>{if(typeof id==='string'&&typeof factory==='function')registrations.set(id,factory);},
    addScreenLayer:(id,draw)=>{if(typeof id==='string'&&typeof draw==='function')screenLayers.set(id,draw);},
    removeScreenLayer:id=>screenLayers.delete(id),
    drawScreen:(ctx,size,off,core)=>{ctx.save();ctx.beginPath();ctx.arc(off+size/2,off+size/2,size/2,0,Math.PI*2);ctx.clip();for(const [id,draw] of screenLayers){try{draw(ctx,size,off,core);}catch(e){console.warn('Pip mod render error:',id,e);}}ctx.restore();}
  };
  let mods=[];
  const rowFor=id=>list.querySelector('[data-mod="'+id+'"]');
  function setProgress(mod,pct,label){
    const row=rowFor(mod.id);if(!row)return;
    const bar=row.querySelector('.mod-progress'),fill=bar.querySelector('i'),caption=row.querySelector('.mod-progress-label');
    bar.classList.add('visible');fill.style.width=Math.max(0,Math.min(100,pct))+'%';caption.textContent=label||Math.round(pct)+'%';
  }
  function finishProgress(mod,label){
    setProgress(mod,100,label);
    const row=rowFor(mod.id);if(row){const bar=row.querySelector('.mod-progress');bar.classList.remove('visible');}
  }
  async function fetchTextWithProgress(url,onProgress){
    const response=await fetch(url,{cache:'no-store'});
    if(!response.ok)throw new Error('Сервер вернул ошибку '+response.status);
    const total=Number(response.headers.get('content-length'))||0;
    if(!response.body||!response.body.getReader){const text=await response.text();onProgress(76,'Загрузка завершена');return text;}
    const reader=response.body.getReader(),chunks=[];let received=0;
    while(true){
      const part=await reader.read();if(part.done)break;
      chunks.push(part.value);received+=part.value.length;
      const pct=total?Math.min(76,Math.round(received/total*76)):Math.min(70,12+Math.floor(received/2048)*4);
      onProgress(pct,'Скачивание · '+(total?Math.round(received/total*100)+'%':'получено '+Math.round(received/1024)+' КБ'));
    }
    const all=new Uint8Array(received);let offset=0;
    for(const chunk of chunks){all.set(chunk,offset);offset+=chunk.length;}
    onProgress(78,'Проверка файла');
    return new TextDecoder('utf-8').decode(all);
  }
  async function ensureScript(mod,onProgress){
    if(registrations.has(mod.id))return;
    if(!loaded.has(mod.id)){
      const source=await fetchTextWithProgress(BASE+encodeURIComponent(mod.entry),onProgress);
      onProgress(84,'Подготовка мода');
      const blob=new Blob([source+'\n//# sourceURL=pip-mod-'+mod.id+'.js'],{type:'text/javascript'});
      const objectUrl=URL.createObjectURL(blob);
      try{
        await new Promise((resolve,reject)=>{
          const script=document.createElement('script');script.async=true;script.src=objectUrl;
          script.onload=()=>{script.remove();resolve();};
          script.onerror=()=>{script.remove();reject(new Error('Не удалось запустить файл мода'));};
          document.head.append(script);
        });
      }finally{URL.revokeObjectURL(objectUrl);}
      loaded.add(mod.id);
    }
    if(!registrations.has(mod.id))throw new Error('Файл загружен, но мод не зарегистрировался');
  }
  function renderOne(mod){
    const row=rowFor(mod.id);if(!row)return;
    const button=row.querySelector('.mod-toggle');
    const enabled=active.has(mod.id);
    button.classList.toggle('active',enabled);
    button.textContent=enabled?'Установлен ✓':'Установить';
  }
  async function setMod(mod,on){
    if(on){
      await ensureScript(mod,(pct,label)=>setProgress(mod,pct,label));
      setProgress(mod,88,'Установка · подключение эффекта');
      const factory=registrations.get(mod.id);
      if(typeof factory!=='function')throw new Error('Не найден установщик мода');
      const cleanup=factory();
      if(typeof cleanup==='function')cleanups.set(mod.id,cleanup);
      active.add(mod.id);persist();
      setProgress(mod,100,'Готово · мод установлен');
    }else{
      setProgress(mod,35,'Отключение мода');
      const cleanup=cleanups.get(mod.id);
      if(cleanup){try{cleanup();}catch(e){console.warn('Pip mod cleanup error:',mod.id,e);}}
      cleanups.delete(mod.id);active.delete(mod.id);persist();
      setProgress(mod,100,'Мод отключён');
    }
    renderOne(mod);
  }
  function render(){
    list.replaceChildren();
    mods.forEach(mod=>{
      const row=document.createElement('div');row.className='mod-row';row.dataset.mod=mod.id;
      const title=document.createElement('strong');title.textContent=mod.name+' · '+mod.version;
      const desc=document.createElement('p');desc.textContent=mod.description;
      const progress=document.createElement('div');progress.className='mod-progress';
      const fill=document.createElement('i');progress.append(fill);
      const label=document.createElement('div');label.className='mod-progress-label';label.textContent=active.has(mod.id)?'Установлен на этом устройстве':'';
      const button=document.createElement('button');button.className='mod-toggle';button.textContent=active.has(mod.id)?'Установлен ✓':'Установить';
      button.onclick=async()=>{
        const turnOn=!active.has(mod.id);button.disabled=true;status.textContent=turnOn?'Загружаем и устанавливаем '+mod.name+'…':'Отключаем '+mod.name+'…';
        try{
          await setMod(mod,turnOn);
          status.textContent=turnOn?'Мод установлен. Его можно использовать вместе с другими.':'Мод отключён.';
          label.textContent=turnOn?'Установлено успешно':'Отключено';
        }catch(e){
          status.textContent='Не удалось установить '+mod.name+': '+(e&&e.message?e.message:'неизвестная ошибка');
          label.textContent='Ошибка установки · можно попробовать снова';
          console.error('Pip mod install failed:',mod.id,e);
          if(turnOn){const cleanup=cleanups.get(mod.id);if(cleanup){try{cleanup();}catch(_){ }cleanups.delete(mod.id);}active.delete(mod.id);persist();}
        }finally{button.disabled=false;renderOne(mod);}
      };
      row.append(title,desc,progress,label,button);list.append(row);
    });
  }
  document.getElementById('pip-mod-repo').onclick=async()=>{
    panel.classList.add('open');status.textContent='Подключаемся к репозиторию…';
    try{
      const response=await fetch(MANIFEST,{cache:'no-store'});
      if(!response.ok)throw new Error('Каталог недоступен ('+response.status+')');
      const data=await response.json();mods=Array.isArray(data.mods)?data.mods:[];
      render();status.textContent='Выберите моды. Установка показывает ход скачивания и подключения.';
      for(const mod of mods){
        if(active.has(mod.id)){
          try{
            await ensureScript(mod,(pct,label)=>setProgress(mod,pct,label));
            const cleanup=registrations.get(mod.id)();
            if(typeof cleanup==='function')cleanups.set(mod.id,cleanup);
            finishProgress(mod,'Активен');
          }catch(e){active.delete(mod.id);persist();status.textContent='Не удалось восстановить '+mod.name+'. Его можно установить заново.';}
        }
      }
    }catch(e){status.textContent='Не удалось загрузить каталог. Проверьте подключение и попробуйте снова.';console.warn('Pip mod repository error',e);}
  };
  document.getElementById('pip-mod-back').onclick=()=>panel.classList.remove('open');
  // Restore enabled mods quietly on every simulator start, without forcing the repository panel open.
  (async()=>{
    try{
      const response=await fetch(MANIFEST,{cache:'no-store'});if(!response.ok)return;
      const data=await response.json();mods=Array.isArray(data.mods)?data.mods:[];
      for(const mod of mods){
        if(!active.has(mod.id)||cleanups.has(mod.id))continue;
        try{
          await ensureScript(mod,()=>{});
          const factory=registrations.get(mod.id);
          if(typeof factory!=='function')continue;
          const cleanup=factory();
          if(typeof cleanup==='function')cleanups.set(mod.id,cleanup);
        }catch(e){console.warn('Could not restore Pip mod:',mod.id,e);}
      }
    }catch(e){console.warn('Pip mods restore unavailable',e);}
  })();
})();
</script>
"""
s = s.replace("</body>", footer + "\n" + loader + "\n</body>", 1)
screen_hook = "  sctx.fill('evenodd');"
screen_replacement = """  if (window.PipMods && window.PipMods.drawScreen) window.PipMods.drawScreen(sctx, S, OFF, M);
  sctx.fill('evenodd');"""
if screen_hook not in s:
    raise SystemExit("Could not find screen render hook")
s = s.replace(screen_hook, screen_replacement, 1)

core_hook = "M._sim_init((Math.random() * 2 ** 31) >>> 0);"
core_replacement = """M._sim_init((Math.random() * 2 ** 31) >>> 0);
window.PipCore = M;"""
if core_hook not in s:
    raise SystemExit("Could not find simulator init hook")
s = s.replace(core_hook, core_replacement, 1)

p.write_text(s, encoding="utf-8")

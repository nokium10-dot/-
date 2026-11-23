from pathlib import Path

p = Path("pip/src/main/assets/pip/index.html")
s = p.read_text(encoding="utf-8")

css = """
  * { -webkit-tap-highlight-color: rgba(0,0,0,0) !important; -webkit-touch-callout:none; user-select:none; -webkit-user-select:none; }
  html, body { width:100%; height:100%; min-height:0; overflow:hidden; background:#000 !important; overscroll-behavior:none; }
  body { display:block; background:#000 !important; }
  #view { position:absolute; inset:0; width:100%; height:100%; min-width:0; background:#000; touch-action:none; }
  #view canvas { display:block; width:100%; height:100%; outline:none; touch-action:none; -webkit-tap-highlight-color:rgba(0,0,0,0); }
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
  #pip-settings.open { display:block; }
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
  const closeSettings = () => panel.classList.remove('open');
  trigger.addEventListener('click', () => panel.classList.add('open'));
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
s = s.replace("
<script>
(() => {
  const MANIFEST='https://raw.githubusercontent.com/nokium10-dot/-/main/pip-mods/manifest.json';
  const BASE='https://raw.githubusercontent.com/nokium10-dot/-/main/pip-mods/';
  const panel=document.getElementById('pip-mod-panel'), list=document.getElementById('pip-mod-list'), status=document.getElementById('pip-mod-status');
  const active=new Set(JSON.parse(localStorage.getItem('pip.mods.active')||'[]'));
  const loaded=new Set(), cleanups=new Map(), registrations=new Map();
  window.PipMods={
    has:id=>registrations.has(id),
    register:(id,enable)=>registrations.set(id,enable)
  };
  const persist=()=>localStorage.setItem('pip.mods.active',JSON.stringify([...active]));
  async function ensureScript(mod){
    if(registrations.has(mod.id))return;
    if(!loaded.has(mod.id)){
      await new Promise((resolve,reject)=>{const script=document.createElement('script');script.src=BASE+mod.entry;script.onload=resolve;script.onerror=()=>reject(new Error('Не удалось загрузить '+mod.name));document.head.append(script);});
      loaded.add(mod.id);
    }
    if(!registrations.has(mod.id))throw new Error('Мод не зарегистрировал себя');
  }
  async function setMod(mod,on){
    if(on){await ensureScript(mod);const cleanup=registrations.get(mod.id)();if(typeof cleanup==='function')cleanups.set(mod.id,cleanup);active.add(mod.id);}
    else{const cleanup=cleanups.get(mod.id);if(cleanup)cleanup();cleanups.delete(mod.id);active.delete(mod.id);}
    persist();renderOne(mod);
  }
  let mods=[];
  function renderOne(mod){
    const row=list.querySelector('[data-mod="'+mod.id+'"]');if(!row)return;
    const b=row.querySelector('button');b.classList.toggle('active',active.has(mod.id));b.textContent=active.has(mod.id)?'Установлен ✓':'Установить';
  }
  function render(){
    list.replaceChildren();
    mods.forEach(mod=>{
      const row=document.createElement('div');row.className='mod-row';row.dataset.mod=mod.id;
      const title=document.createElement('strong');title.textContent=mod.name+' · '+mod.version;
      const desc=document.createElement('p');desc.textContent=mod.description;
      const b=document.createElement('button');b.className='mod-toggle';b.textContent=active.has(mod.id)?'Установлен ✓':'Установить';
      b.onclick=async()=>{b.disabled=true;try{await setMod(mod,!active.has(mod.id));status.textContent='Изменения сохранены на этом устройстве.';}catch(e){status.textContent=e.message;}finally{b.disabled=false;}};
      row.append(title,desc,b);list.append(row);
    });
  }
  document.getElementById('pip-mod-repo').onclick=async()=>{panel.classList.add('open');status.textContent='Загрузка каталога…';try{const r=await fetch(MANIFEST,{cache:'no-store'});if(!r.ok)throw new Error('Каталог недоступен');const data=await r.json();mods=data.mods||[];render();status.textContent='Выберите моды. Их можно включать одновременно.';for(const m of mods)if(active.has(m.id)){try{await ensureScript(m);const cleanup=registrations.get(m.id)();if(typeof cleanup==='function')cleanups.set(m.id,cleanup);}catch(e){active.delete(m.id);persist();status.textContent='Один из сохранённых модов не загрузился: '+e.message;}}}catch(e){status.textContent='Не удалось загрузить каталог. Проверьте интернет и откройте раздел снова.';}};
  document.getElementById('pip-mod-back').onclick=()=>panel.classList.remove('open');
})();
</script>\n</body>", footer + "\n</body>", 1)
p.write_text(s, encoding="utf-8")

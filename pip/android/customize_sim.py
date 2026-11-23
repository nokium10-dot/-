from pathlib import Path
import re

p = Path("pip/src/main/assets/pip/index.html")
s = p.read_text(encoding="utf-8")

css = """
  /* AMOLED black background and centered virtual watch. */
  html, body { width:100%; height:100%; min-height:0; overflow:hidden; background:#000 !important; }
  body { display:block; background:#000 !important; }
  #view { position:absolute; inset:0; width:100%; height:100%; min-width:0; background:#000; touch-action:pan-x pan-y; }
  #view canvas { width:100%; height:100%; transform:scale(var(--pip-watch-scale, 1)); transform-origin:50% 50%; }
  #hint, #panel { display:none !important; }
  #pip-settings-trigger {
    position:fixed; z-index:20; top:max(12px, env(safe-area-inset-top)); left:12px;
    width:44px; height:44px; border-radius:15px; border:1px solid #ffffff24;
    color:#fff; background:#101014; font-size:22px; display:grid; place-items:center;
    box-shadow:0 5px 24px #0008; -webkit-tap-highlight-color:transparent;
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
  #pip-settings .hint { color:#777b89; font-size:12px; margin-top:18px; line-height:1.5; }
  #pip-settings .settings-card { padding:15px; border:1px solid #24242c; border-radius:17px; background:#08080b; }
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
  <button class="assist" id="pip-assistant-role">Выбрать Пипа помощником</button>
  <button class="reset-zoom" id="pip-reset-zoom">Сбросить размер часов</button>
  <div class="hint">На главном экране можно менять размер часов, сводя или разводя два пальца. Игры и приложения открываются кнопками на корпусе часов: верхняя — меню / открыть, нижняя — назад.</div>
</section>
"""
s = s.replace('<div id="view">', '<div id="view">', 1)
s = s.replace('<div id="panel">', markup + '\n<div id="panel">', 1)

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

  const view = document.getElementById('view');
  const canvas = view && view.querySelector('canvas');
  let watchScale = Number(localStorage.getItem('keypet.watchScale') || 1);
  const applyScale = () => {
    document.documentElement.style.setProperty('--pip-watch-scale', String(watchScale));
    localStorage.setItem('keypet.watchScale', String(watchScale));
  };
  applyScale();
  let startDistance = 0;
  let startScale = watchScale;
  const distance = touches => {
    const dx = touches[0].clientX - touches[1].clientX;
    const dy = touches[0].clientY - touches[1].clientY;
    return Math.hypot(dx, dy);
  };
  view.addEventListener('touchstart', event => {
    if (event.touches.length === 2) {
      event.preventDefault();
      startDistance = distance(event.touches);
      startScale = watchScale;
    }
  }, { passive:false });
  view.addEventListener('touchmove', event => {
    if (event.touches.length === 2 && startDistance > 0) {
      event.preventDefault();
      watchScale = Math.max(0.55, Math.min(1.8, startScale * distance(event.touches) / startDistance));
      applyScale();
    }
  }, { passive:false });
  view.addEventListener('touchend', event => {
    if (event.touches.length < 2) startDistance = 0;
  }, { passive:false });
  document.getElementById('pip-reset-zoom').addEventListener('click', () => {
    watchScale = 1;
    applyScale();
  });
})();
</script>
"""
s = s.replace("</body>", footer + "\n</body>", 1)
p.write_text(s, encoding="utf-8")

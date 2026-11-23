from pathlib import Path
import re

p = Path("pip/src/main/assets/pip/index.html")
s = p.read_text(encoding="utf-8")

css = """
  /* Android: focus on the keychain itself; tools are operated through its physical side buttons. */
  html, body { width:100%; height:100%; min-height:0; overflow:hidden; }
  body { display:block; background:#08090d; }
  #view { position:absolute; inset:0; width:100%; height:100%; min-width:0; }
  #view canvas { width:100%; height:100%; }
  #hint, #panel { display:none !important; }
  #pip-settings-trigger {
    position:fixed; z-index:20; top:max(12px, env(safe-area-inset-top)); left:12px;
    width:44px; height:44px; border-radius:15px; border:1px solid #ffffff28;
    color:#fff; background:#171923eF; font-size:22px; display:grid; place-items:center;
    box-shadow:0 5px 24px #0008; -webkit-tap-highlight-color:transparent;
  }
  #pip-settings {
    display:none; position:fixed; z-index:19; top:max(64px, calc(env(safe-area-inset-top) + 60px));
    left:12px; width:min(280px, calc(100vw - 24px)); padding:16px;
    border:1px solid #ffffff22; border-radius:18px; background:#151720f5; color:#f4f4f8;
    box-shadow:0 12px 40px #000a; backdrop-filter:blur(16px); font:14px system-ui,sans-serif;
  }
  #pip-settings.open { display:block; }
  #pip-settings h3 { margin:0 0 12px; font-size:15px; }
  #pip-settings .case-colors { display:grid; grid-template-columns:repeat(3,1fr); gap:8px; }
  #pip-settings button { min-height:38px; border-radius:11px; }
  #pip-settings .swatch { display:flex; align-items:center; justify-content:center; gap:6px; }
  #pip-settings .dot { width:14px; height:14px; border-radius:50%; border:1px solid #ffffff66; }
  #pip-settings .assist { width:100%; margin-top:12px; }
  #pip-settings .hint { color:#aeb2c2; font-size:11px; margin-top:10px; line-height:1.4; }
"""
s = s.replace("</style>", css + "\n</style>", 1)

markup = """
<button id="pip-settings-trigger" aria-label="Настройки" title="Настройки">⚙</button>
<section id="pip-settings" aria-label="Настройки Пипа">
  <h3>Настройки</h3>
  <div style="margin-bottom:9px">Цвет корпуса часов</div>
  <div class="case-colors">
    <button class="swatch" data-case="#c4c6cc" data-polish="#e0e1e5"><i class="dot" style="background:#c4c6cc"></i>Серый</button>
    <button class="swatch" data-case="#252833" data-polish="#555b6d"><i class="dot" style="background:#252833"></i>Графит</button>
    <button class="swatch" data-case="#17191e" data-polish="#454954"><i class="dot" style="background:#17191e"></i>Чёрный</button>
    <button class="swatch" data-case="#7139a8" data-polish="#b77af0"><i class="dot" style="background:#7139a8"></i>Фиолет.</button>
    <button class="swatch" data-case="#126b68" data-polish="#56c8b9"><i class="dot" style="background:#126b68"></i>Бирюза</button>
    <button class="swatch" data-case="#9d244d" data-polish="#ed79a0"><i class="dot" style="background:#9d244d"></i>Розовый</button>
  </div>
  <button class="assist" id="pip-assistant-role">Выбрать Пипа помощником</button>
  <div class="hint">Игры и приложения открываются кнопками на корпусе часов: верхняя — меню / открыть, нижняя — назад.</div>
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
  trigger.addEventListener('click', () => panel.classList.toggle('open'));
  document.addEventListener('pointerdown', e => {
    if (!panel.contains(e.target) && e.target !== trigger) panel.classList.remove('open');
  });
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
})();
</script>
"""
s = s.replace("</body>", footer + "\n</body>", 1)
p.write_text(s, encoding="utf-8")

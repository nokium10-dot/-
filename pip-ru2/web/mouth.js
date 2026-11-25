/* Тумблер «Рот» во вкладке «Настройки» (рот встроен в сам рендерер Пипа, патч — папка mouth/) */
(() => {
  const page = document.getElementById('pg-look');
  if (!page) return;
  const st = document.createElement('style');
  st.textContent = `
.pr-switch { position: relative; display: flex; align-items: center; gap: 14px; padding: 14px; border-radius: 18px; background: var(--pr-card); border: 1px solid var(--pr-line); cursor: pointer; -webkit-tap-highlight-color: transparent; }
.pr-switch .txt { flex: 1; display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.pr-switch b { font-size: 16px; font-weight: 650; }
.pr-switch small { font-size: 13px; line-height: 1.35; color: var(--pr-dim); }
.pr-switch input { position: absolute; opacity: 0; width: 1px; height: 1px; pointer-events: none; }
.pr-switch .knob { position: relative; flex: none; width: 52px; height: 32px; border-radius: 16px; background: #2a3045; transition: background .35s var(--pr-ease-out); }
.pr-switch .knob::after { content: ''; position: absolute; left: 3px; top: 3px; width: 26px; height: 26px; border-radius: 50%; background: #fff; box-shadow: 0 2px 6px rgba(0,0,0,.45); transition: transform .45s var(--pr-ease-spring), width .2s ease; }
.pr-switch:active .knob::after { width: 31px; }
.pr-switch input:checked + .knob { background: var(--pr-acc); }
.pr-switch input:checked + .knob::after { transform: translateX(20px); }
.pr-switch:active input:checked + .knob::after { transform: translateX(15px); }
.pr-switch input:focus-visible + .knob { outline: 2px solid var(--pr-acc2); outline-offset: 3px; }`;
  document.head.appendChild(st);
  const box = document.createElement('div');
  box.innerHTML = '<h2>Лицо</h2><label class="pr-switch" id="pr-mouth-row"><span class="txt"><b>Рот</b>' +
    '<small>Белый рот под глазами: меняется с каждой эмоцией и открывается, когда Пип говорит</small></span>' +
    '<input type="checkbox" id="pr-mouth" checked><i class="knob" aria-hidden="true"></i></label>';
  const anchor = Array.from(page.querySelectorAll('h2')).find(h => h.textContent.trim() === 'Цвет корпуса');
  while (box.firstChild) page.insertBefore(box.firstChild, anchor || null);
  const cb = document.getElementById('pr-mouth');
  const sync = () => { const a = window.pipApi; if (a && a.mouth) cb.checked = !!a.mouth(); };
  cb.addEventListener('change', () => {
    const a = window.pipApi; if (a && a.mouth) a.mouth(cb.checked);
    try { const b = window.PipAndroid; if (b && b.haptic) b.haptic(); } catch (_) { /* нет вибро */ }
  });
  addEventListener('pip-mouth-sync', sync);
})();

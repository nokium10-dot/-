/* ===== Пип · мобильный интерфейс: логика (обычный скрипт, выполняется до модуля симулятора) ===== */
(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const reduce = () => matchMedia('(prefers-reduced-motion: reduce)').matches;
  const bridge = () => window.PipAndroid || null;
  const buzz = () => { try { const b = bridge(); if (b && b.haptic) b.haptic(); } catch (_) { /* нет вибро */ } };
  const EASE_OUT = 'cubic-bezier(.16, 1, .3, 1)';

  /* ---------- пружинная физика (в духе HyperSpring): настоящая затухающая пружина, выбранная в ключевые кадры ---------- */
  const springP = (t, k, c) => {                  // прогресс пружины 0→1 с перелётом, масса = 1
    const w = Math.sqrt(k), z = c / (2 * w), wd = w * Math.sqrt(Math.max(1e-4, 1 - z * z));
    return 1 - Math.exp(-z * w * t) * (Math.cos(wd * t) + (z * w / wd) * Math.sin(wd * t));
  };
  function springAnim(el, frame, o = {}) {
    const k = o.k || 190, c = o.c || 15, fill = o.fill || 'backwards';
    const dur = Math.min(1.3, 6.2 / (c / 2)), n = Math.max(12, Math.round(dur * 60)), kfs = [];
    for (let i = 0; i <= n; i++) { const t = dur * i / n; kfs.push(Object.assign(frame(springP(t, k, c), t), { offset: i / n })); }
    return el.animate(kfs, { duration: dur * 1000, delay: o.delay || 0, easing: 'linear', fill });
  }
  const clamp = (v, a, b) => Math.min(b, Math.max(a, v));

  /* ---------- ошибки запуска: показываем на заставке ---------- */
  const splashText = $('#pr-splash-text');
  const showFatal = (msg) => {
    if (document.documentElement.classList.contains('pip-ready')) return;
    splashText.className = 'err';
    splashText.textContent = 'Не удалось запустить Пипа: ' + msg;
  };
  addEventListener('error', e => showFatal(e.message || 'ошибка скрипта'));
  addEventListener('unhandledrejection', e => showFatal((e.reason && e.reason.message) || String(e.reason)));
  setTimeout(() => showFatal('превышено время ожидания (проверьте WebGL и обновите Android System WebView)'), 20000);

  /* ---------- перекладываем панель оригинального симулятора по вкладкам ---------- */
  const panel = $('#panel');
  const kids = panel ? Array.from(panel.children) : [];
  const head = kids.shift();                       // строка: экран Пипа + настроение
  const closeBtn = $('#pr-close');
  if (head) $('#pr-head').insertBefore(head, closeBtn);

  const groups = {};
  let cur = null;
  for (const el of kids) {
    if (el.tagName === 'H2') {
      cur = { nodes: [el] };
      groups[el.textContent.trim().split(/[\s(]/)[0]] = cur;
    } else if (cur) {
      cur.nodes.push(el);
    }
  }
  const rename = { 'iPhone': 'Эмуляция iPhone', 'Компьютер': 'Эмуляция компьютера', 'Датчики': 'Датчики',
    'Виртуальный': 'Виртуальный гироскоп', 'Жесты': 'Жесты', 'Кнопки': 'Кнопки корпуса', 'Тамагочи': 'Тамагочи' };
  for (const k in rename) if (groups[k]) groups[k].nodes[0].textContent = rename[k];
  const place = (page, key) => {
    if (!groups[key]) { console.warn('Нет раздела оригинала:', key); return; }
    groups[key].nodes.forEach(n => $('#pg-' + page).append(n));
  };
  place('care', 'Тамагочи'); place('care', 'Жесты');
  place('mood', 'Настроение');
  place('sound', 'Голос');
  place('ctl', 'Кнопки'); place('ctl', 'Виртуальный'); place('ctl', 'Датчики');
  place('link', 'iPhone'); place('link', 'Компьютер');
  if (panel) panel.remove();

  /* «Стиль» и справочные блоки — на вкладку «Настройки» */
  const look = $('#pg-look');
  if (groups['Стиль']) groups['Стиль'].nodes.forEach(n => look.append(n));

  const COMMANDS = [
    ['покорми', 'накормить Пипа'], ['поиграй', 'поиграть с ним'], ['погладь', 'погладить'],
    ['тык / встряхни', 'ткнуть или встряхнуть'], ['усни / проснись', 'уложить спать или разбудить'],
    ['радуйся, грусти, удивись, влюбись…', 'сменить эмоцию'], ['как дела', 'узнать, что он чувствует'],
    ['открой меню / назад', 'нажать кнопки брелока'], ['который час', 'время и дата'],
    ['включи звук / выключи звук', 'громкость Пипа'], ['что ты умеешь', 'список команд']];
  const h2 = (t) => { const e = document.createElement('h2'); e.textContent = t; return e; };
  look.append(h2('Голосовые команды'));
  const ul = document.createElement('ul'); ul.className = 'pr-cmds';
  for (const [c, d] of COMMANDS) { const li = document.createElement('li'); const b = document.createElement('b'); b.textContent = '«' + c + '»'; li.append(b, ' — ' + d); ul.append(li); }
  look.append(ul);
  look.append(h2('О приложении'));
  const about = document.createElement('p'); about.className = 'note'; about.id = 'pr-about';
  about.textContent = 'Пип — русская мобильная версия симулятора Pip-KeyPet (MIT, NovikovVisuals). Настоящие датчики и Bluetooth брелока на телефоне эмулируются.';
  look.append(about);

  /* ---------- уведомление ---------- */
  const toastEl = $('#pr-toast'); let toastT = 0;
  window.pipToast = (msg, ms = 2600) => {
    toastEl.textContent = msg; toastEl.classList.add('on');
    clearTimeout(toastT); toastT = setTimeout(() => toastEl.classList.remove('on'), ms);
  };

  /* ---------- подтверждение ---------- */
  const modal = $('#pr-modal'); let modalDone = null;
  const closeModal = (v) => { modal.classList.remove('on'); const d = modalDone; modalDone = null; if (d) d(v); };
  $('#pr-modal-yes').onclick = () => closeModal(true);
  $('#pr-modal-no').onclick = () => closeModal(false);
  modal.addEventListener('pointerdown', e => { if (e.target === modal) closeModal(false); });
  window.pipConfirm = (title, text) => new Promise(res => {
    $('#pr-modal-title').textContent = title; $('#pr-modal-text').textContent = text;
    modalDone = res; modal.classList.add('on'); buzz();
  });

  /* ---------- шторка с вкладками ---------- */
  const sheet = $('#pr-sheet'), backdrop = $('#pr-backdrop'), tabs = $('#pr-tabs'), ind = $('.pr-ind', tabs), pages = $('#pr-pages');
  const tabBtns = $$('button[data-tab]', tabs);
  pages.style.perspective = '900px'; pages.style.perspectiveOrigin = '50% 0';   // для наклона «шторы»
  const order = tabBtns.map(b => b.dataset.tab);
  let current = null, opened = false;

  function placeIndicator(btn, animate) {
    tabs.classList.toggle('no-anim', !animate);
    ind.style.width = btn.offsetWidth + 'px';
    ind.style.transform = 'translateX(' + btn.offsetLeft + 'px)';
    const left = btn.offsetLeft - (tabs.clientWidth - btn.offsetWidth) / 2;
    tabs.scrollTo({ left, behavior: animate && !reduce() ? 'smooth' : 'auto' });
  }

  function stagger(page) {
    if (reduce()) return;
    Array.from(page.children).forEach((el, i) => {
      el.animate(
        [{ opacity: 0, transform: 'translateY(18px) scale(.985)' }, { opacity: 1, transform: 'none' }],
        { duration: 520, delay: Math.min(i, 14) * 34 + 40, easing: EASE_OUT, fill: 'backwards' });
    });
  }

  function selectTab(name, instant) {
    if (!order.includes(name) || name === current) return;
    const next = $('#pg-' + name), prev = current ? $('#pg-' + current) : null;
    const dir = current ? Math.sign(order.indexOf(name) - order.indexOf(current)) || 1 : 1;
    tabBtns.forEach(b => { const on = b.dataset.tab === name; b.classList.toggle('on', on); b.setAttribute('aria-selected', on ? 'true' : 'false'); });
    const animate = !instant && !reduce();
    placeIndicator($('[data-tab="' + name + '"]', tabs), animate && !!current);
    current = name;
    const f = 0.75 + 0.25 * Math.min(3, Math.abs(order.indexOf(name) - order.indexOf(prev ? prev.id.slice(3) : name)) || 1);
    if (prev) {
      if (animate) {
        /* «штора»: уходящая страница наклоняется по скорости и размывается */
        prev.classList.add('leaving');
        const out = prev.animate(
          [{ opacity: 1, transform: 'none', filter: 'blur(0px)' },
           { opacity: 0, transform: 'translateX(' + (-dir * 46 * f) + 'px) rotateY(' + (dir * 22 * f) + 'deg) scale(.96)', filter: 'blur(7px)' }],
          { duration: 230, easing: 'cubic-bezier(.4, 0, 1, 1)', fill: 'forwards' });
        out.onfinish = () => { prev.classList.remove('active', 'leaving'); out.cancel(); };
      } else prev.classList.remove('active');
    }
    next.classList.add('active');
    pages.scrollTop = 0;
    if (animate) {
      springAnim(next, (p) => {
        const q = 1 - p;
        return { transform: 'translateX(' + (dir * 70 * f * q) + 'px) rotateY(' + (-dir * 20 * f * q) + 'deg) scale(' + (1 - .04 * q) + ')',
                 opacity: Math.min(1, p * 2.2), filter: 'blur(' + Math.max(0, q) * 8 + 'px)' };
      }, { k: 200, c: 16, delay: 90 });
      stagger(next);
    }
    if (!instant) buzz();
    if (name === 'look') refreshAssistant();
  }
  tabBtns.forEach(b => b.addEventListener('click', () => selectTab(b.dataset.tab)));

  let sheetAnim = null, origin = null;
  const setOrigin = (el) => {
    if (!el || !el.getBoundingClientRect) return;
    const r = el.getBoundingClientRect(); origin = { x: r.left + r.width / 2, y: r.top + r.height / 2 };
  };
  const killSheetAnim = () => { if (sheetAnim) { sheetAnim.onfinish = null; sheetAnim.cancel(); sheetAnim = null; } };
  const resetSheetStyle = () => { sheet.style.transition = ''; sheet.style.transform = ''; sheet.style.transformOrigin = ''; backdrop.style.transition = ''; backdrop.style.opacity = ''; };

  function openSheet(tab, from) {
    if (from) setOrigin(from);
    if (!opened) {
      opened = true;
      killSheetAnim(); resetSheetStyle();
      document.body.classList.add('pr-open');
      backdrop.classList.add('show');
      const genie = !reduce();
      if (genie) sheet.style.transition = 'none';
      sheet.classList.add('show');
      buzz();
      if (!current) selectTab(tab || 'care', true);
      else if (tab) selectTab(tab);
      if (genie) {
        /* «джинн»: окно вырастает из кнопки, которой его открыли, с пружинным перелётом */
        const r = sheet.getBoundingClientRect(), o = origin || { x: innerWidth / 2, y: innerHeight };
        sheet.style.transformOrigin = clamp(o.x - r.left, 0, r.width) + 'px ' + (o.y - r.top) + 'px';
        sheetAnim = springAnim(sheet, (p, t) => {
          const py = springP(t * 0.8, 170, 15);
          return { transform: 'scale(' + (0.1 + 0.9 * p) + ',' + Math.min(1.05, 0.03 + 0.97 * py) + ') skewX(' + ((1 - Math.min(1, p)) * -9) + 'deg)',
                   opacity: Math.min(1, p * 4) };
        }, { k: 170, c: 15, fill: 'none' });
        sheetAnim.onfinish = () => { sheetAnim = null; resetSheetStyle(); };
      }
      requestAnimationFrame(() => { const b = $('[data-tab="' + current + '"]', tabs); if (b) placeIndicator(b, false); });
      if (!reduce()) stagger($('#pg-' + current));
    } else if (tab) selectTab(tab);
  }

  function finishClose() {
    killSheetAnim(); resetSheetStyle();
    sheet.classList.remove('show');
  }
  /* mode: undefined — «втягиваем» окно обратно в кнопку; объект {dx,dy,rot} — окно отпущено после смахивания */
  function closeSheet(mode) {
    if (!opened) return;
    opened = false;
    document.body.classList.remove('pr-open');
    backdrop.style.transition = ''; backdrop.style.opacity = '';
    backdrop.classList.remove('show');
    const a = document.activeElement; if (a && a.blur) a.blur();
    killSheetAnim();
    if (reduce()) { resetSheetStyle(); sheet.classList.remove('show'); return; }
    sheet.style.transition = 'none';
    if (mode && typeof mode === 'object') {
      sheetAnim = sheet.animate(
        [{ transform: 'translate3d(' + mode.dx + 'px,' + mode.dy + 'px,0) rotate(' + mode.rot + 'deg)' },
         { transform: 'translate3d(' + (mode.dx * 1.8) + 'px,' + (sheet.offsetHeight * 1.1 + mode.dy) + 'px,0) rotate(' + (mode.rot * 1.8) + 'deg)' }],
        { duration: 320, easing: 'cubic-bezier(.5, 0, .75, 0)', fill: 'forwards' });
    } else {
      const r = sheet.getBoundingClientRect(), o = origin || { x: innerWidth / 2, y: innerHeight };
      sheet.style.transformOrigin = clamp(o.x - r.left, 0, r.width) + 'px ' + (o.y - r.top) + 'px';
      const kfs = [];
      for (let i = 0; i <= 24; i++) {
        const u = i / 24, e = u * u * u, ey = Math.min(1, e * 1.3);
        kfs.push({ offset: u, transform: 'scale(' + (1 - 0.9 * e) + ',' + (1 - 0.97 * ey) + ') skewX(' + (-9 * e) + 'deg)', opacity: 1 - clamp((u - .55) / .45, 0, 1) });
      }
      sheetAnim = sheet.animate(kfs, { duration: 340, easing: 'linear', fill: 'forwards' });
    }
    sheetAnim.onfinish = finishClose;
  }
  $('#pr-close').onclick = () => closeSheet();
  backdrop.addEventListener('pointerdown', () => closeSheet());
  addEventListener('keydown', e => { if (e.key === 'Escape') (modalDone ? closeModal(false) : closeSheet()); });

  /* «лампа»: окно висит на пальце и раскачивается; отпустили — пружинно возвращается или улетает вниз */
  let drag = null;
  const dragStart = (e) => {
    if (!opened || e.target.closest('button')) return;
    killSheetAnim(); resetSheetStyle();
    const r = sheet.getBoundingClientRect();
    drag = { x: e.clientX, y: e.clientY, t: performance.now(), lt: performance.now(), lx: e.clientX, vx: 0, dx: 0, dy: 0, rot: 0, el: e.currentTarget };
    drag.el.setPointerCapture(e.pointerId);
    sheet.style.transformOrigin = (e.clientX - r.left) + 'px ' + (e.clientY - r.top) + 'px';
    sheet.style.transition = 'none'; backdrop.style.transition = 'none';
  };
  const dragMove = (e) => {
    if (!drag) return;
    const now = performance.now(), dt = Math.max(1, now - drag.lt);
    drag.vx = drag.vx * 0.7 + ((e.clientX - drag.lx) / dt) * 0.3;
    drag.lt = now; drag.lx = e.clientX;
    drag.dx = (e.clientX - drag.x) * 0.45;
    drag.dy = Math.max(0, e.clientY - drag.y);
    drag.rot = clamp((e.clientX - drag.x) / 16 + drag.vx * 5, -11, 11);
    sheet.style.transform = 'translate3d(' + drag.dx + 'px,' + drag.dy + 'px,0) rotate(' + drag.rot + 'deg)';
    backdrop.style.opacity = String(Math.max(0, 1 - drag.dy / Math.max(1, sheet.offsetHeight * .8)));
  };
  const dragEnd = () => {
    if (!drag) return;
    const d = drag; drag = null;
    const v = d.dy / Math.max(1, performance.now() - d.t);
    const close = d.dy > sheet.offsetHeight * .22 || v > .7;
    backdrop.style.transition = '';
    if (close) { closeSheet({ dx: d.dx, dy: d.dy, rot: d.rot }); return; }
    backdrop.style.opacity = '';
    sheet.style.transform = '';
    if (reduce()) { sheet.style.transition = ''; sheet.style.transformOrigin = ''; return; }
    sheetAnim = springAnim(sheet, (p) => {
      const q = 1 - p;
      return { transform: 'translate3d(' + (d.dx * q) + 'px,' + (d.dy * q) + 'px,0) rotate(' + (d.rot * q) + 'deg)' };
    }, { k: 230, c: 11, fill: 'none' });
    sheetAnim.onfinish = () => { sheetAnim = null; resetSheetStyle(); };
  };
  for (const el of [$('#pr-grab'), $('#pr-head')]) {
    el.addEventListener('pointerdown', dragStart);
    el.addEventListener('pointermove', dragMove);
    el.addEventListener('pointerup', dragEnd);
    el.addEventListener('pointercancel', dragEnd);
  }

  window.pipUi = {
    open: openSheet,
    close: closeSheet,
    back: () => { if (modalDone) { closeModal(false); return true; } if (opened) { closeSheet(); return true; } return false; },
  };

  /* ---------- верх и док ---------- */
  const notReady = () => { window.pipToast('Пип ещё просыпается…'); };
  const api = () => window.pipApi || null;
  $('#pr-open-look').onclick = (e) => openSheet('look', e.currentTarget);
  $('#pr-pill').onclick = (e) => openSheet('mood', e.currentTarget);
  $('#pr-pill').addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') openSheet('mood', e.currentTarget); });
  $('#pr-mic').onclick = () => startVoice();
  $$('#pr-dock button').forEach(b => b.addEventListener('click', () => {
    const act = b.dataset.act;
    if (act === 'menu') { openSheet('care', b); return; }
    if (!api()) { notReady(); return; }
    b.classList.remove('pop'); void b.offsetWidth; b.classList.add('pop'); buzz();
    api()[act]();
  }));

  /* настроение в плашке сверху зеркалит оригинальное поле */
  const pillText = $('#pr-pill-text'), moodEl = $('#mood');
  if (moodEl) new MutationObserver(() => {
    const t = moodEl.textContent.trim();
    if (t && t !== '…' && pillText.textContent !== t) pillText.textContent = t;
  }).observe(moodEl, { childList: true, characterData: true, subtree: true });

  /* ---------- вкладка «Настройки» ---------- */
  $$('[data-case]').forEach(b => b.addEventListener('click', () => {
    if (window.setPipCaseColor) { window.setPipCaseColor(b.dataset.case, b.dataset.polish); buzz(); }
  }));
  $('#pr-reset-zoom').onclick = () => { if (api()) { api().resetZoom(); buzz(); } };

  function refreshAssistant() {
    const b = bridge(), box = $('#pr-assist-status'), txt = $('#pr-assist-text');
    const set = $('#pr-assist-set'), open = $('#pr-assist-open'), tryBtn = $('#pr-assist-try');
    if (!b || !b.isAssistant) {
      box.dataset.ok = '0'; txt.textContent = 'Выбор помощника доступен в приложении на телефоне';
      set.disabled = open.disabled = tryBtn.disabled = true; return;
    }
    let ok = false; try { ok = !!b.isAssistant(); } catch (_) { /* нет доступа */ }
    box.dataset.ok = ok ? '1' : '0';
    txt.textContent = ok ? 'Пип — ваш голосовой помощник' : 'Пип пока не выбран помощником';
    set.textContent = ok ? 'Пип выбран помощником ✓' : 'Сделать Пипа голосовым помощником';
    set.disabled = false; open.disabled = false; tryBtn.disabled = false;
  }
  window.pipRefreshAssistant = refreshAssistant;
  $('#pr-assist-set').onclick = () => { const b = bridge(); if (b) b.chooseAssistant(); };
  $('#pr-assist-open').onclick = () => { const b = bridge(); if (b) b.openAssistantSettings(); };
  $('#pr-assist-try').onclick = () => { closeSheet(); setTimeout(startVoice, 380); };
  try { const b = bridge(); if (b && b.version) about.textContent += ' Версия ' + b.version() + '.'; } catch (_) { /* без версии */ }

  /* ---------- голос: команды на русском ---------- */
  const hud = $('#pv'), hudTitle = $('#pv-title'), hudSub = $('#pv-sub'); let hudT = 0, moodT = 0;
  const showHud = (cls, title, sub) => {
    hud.className = 'pv on ' + cls; hudTitle.textContent = title; hudSub.textContent = sub || '';
  };
  const hideHud = (ms) => { clearTimeout(hudT); hudT = setTimeout(() => hud.classList.remove('on'), ms); };
  const norm = (t) => t.toLowerCase().replace(/ё/g, 'е').replace(/[^a-zа-я0-9 ]+/g, ' ').replace(/\s+/g, ' ').trim();
  const say = (t) => { try { const b = bridge(); if (b && b.speak) b.speak(t); } catch (_) { /* без озвучки */ } };
  const later = (fn, ms) => setTimeout(fn, ms);
  const MOODS = [
    [/радост|улыбн|веселис|весело/, 'happy', 'Ура, я рад!'], [/груст|грустн|плач/, 'sad', 'Мне грустно…'],
    [/разозл|злис|сердис|в ярост/, 'angry', 'Гр-р-р!'], [/удив/, 'surprised', 'Ого!'],
    [/влюб|обожаю|люблю тебя/, 'love', 'Я тоже тебя люблю!'], [/испуг|бойс|страшно|испугай/, 'afraid', 'Ой, страшно!'],
    [/смущ|засте|стесня/, 'shy', 'Ой, я стесняюсь…'], [/скуч/, 'bored', 'Скучно…'],
    [/закруж|головокруж/, 'dizzy', 'Ой, кружится голова!'], [/кивни|согласи/, 'yes', 'Да!'],
    [/покачай головой|откажи|скажи нет/, 'no', 'Нет!'], [/самодов|задавак|красуй/, 'smug', 'Я такой молодец.'],
    [/дерзк|нагл/, 'cheeky', 'Хе-хе!'], [/обидись|обиж/, 'hurt', 'Мне обидно…'],
    [/подозр|прищур/, 'suspicious', 'Хм… подозрительно.'], [/доволен|комфорт|расслабь/, 'content', 'Мне хорошо.'],
  ];
  const NEED_TEXT = { food: 'Я проголодался.', energy: 'Я хочу спать.', fun: 'Мне хочется поиграть.', love: 'Мне не хватает ласки.', walk: 'Хочу погулять.' };

  function describeState(a) {
    const st = a.status();
    let worst = null, min = 1;
    for (const k of Object.keys(NEED_TEXT)) if (st[k] < min) { min = st[k]; worst = k; }
    return 'Я чувствую: ' + a.ru(st.mood) + '. ' + (worst && min < 0.35 ? NEED_TEXT[worst] : 'У меня всё хорошо!');
  }

  function handle(raw) {
    const a = api(); const t = norm(raw);
    if (!a) return 'Подождите, я ещё просыпаюсь.';
    const has = (re) => re.test(t);
    if (has(/что ты умеешь|помощь|какие команды|помоги/)) return 'Скажи: покорми, поиграй, погладь, усни, как дела, открой меню, который час.';
    if (has(/как тебя зовут|кто ты/)) return 'Я Пип, твой питомец из брелока!';
    if (has(/покорм|накорм|кормить|дай (поесть|еды|покушать)|проголодал|есть хочешь/)) { a.feed(); return 'Ням-ням! Спасибо!'; }
    if (has(/поигра|давай играть|играть|развлек/)) { a.play(); return 'Давай играть!'; }
    if (has(/погладь|поглади|приласкай|обними|почеши/)) { a.pet(); return 'Мне приятно!'; }
    if (has(/ткни|потыкай|тык/)) { a.poke(); return 'Ой!'; }
    if (has(/встряхни|потряси|тряхни/)) { a.shake(); return 'Ого, трясёт!'; }
    if (has(/проснись|просыпайся|вставай|доброе утро/)) { a.mood('woke up'); later(a.auto, 2600); return 'Доброе утро!'; }
    if (has(/усни|засыпай|спокойной ночи|баиньки|иди спать|спи/)) { a.mood('asleep'); return 'Спокойной ночи… хр-р.'; }
    if (has(/открой меню|покажи меню|^меню$|^меню /)) { a.button(0); return 'Открываю меню.'; }
    if (has(/^назад|вернись|закрой|выход/)) { a.button(1); return 'Назад.'; }
    if (has(/который час|сколько времени|скажи время|^время$/)) return 'Сейчас ' + new Date().toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    if (has(/какое сегодня число|какой сегодня день|какая сегодня дата|какое число/)) return 'Сегодня ' + new Date().toLocaleDateString('ru-RU', { weekday: 'long', day: 'numeric', month: 'long' });
    if (has(/как дела|как ты|как настроение|как себя чувствуешь|что чувствуешь/)) return describeState(a);
    if (has(/выключи звук|без звука|замолчи|тише/)) { a.volume(0); return 'Молчу.'; }
    if (has(/включи звук|громче|со звуком/)) { a.volume(0.8); return 'Звук включён!'; }
    for (const [re, mood, reply] of MOODS) if (has(re) && a.mood(mood)) { clearTimeout(moodT); moodT = later(a.auto, 9000); return reply; }
    if (has(/привет|здравствуй|добрый день|добрый вечер|хай/)) { a.mood('happy'); moodT = later(a.auto, 5000); return 'Привет! Я Пип.'; }
    if (has(/спасибо|молодец|умница|хороший/)) { a.mood('love'); moodT = later(a.auto, 5000); return 'Мне так приятно!'; }
    return 'Не понял. Скажи «что ты умеешь».';
  }

  function startVoice() {
    const b = bridge();
    if (!b || !b.startVoice) { window.pipToast('Голосовые команды работают в приложении на телефоне'); return; }
    closeSheet(); b.startVoice();
  }

  window.pipVoice = {
    state(s, msg) {
      clearTimeout(hudT);
      const a = api();
      if (s === 'listening') {
        showHud('listening', 'Слушаю…', 'Скажите, например: «покорми» или «как дела»');
        if (a) { clearTimeout(moodT); a.mood('curious'); }
      } else if (s === 'hearing') {
        hud.classList.remove('listening'); hud.classList.add('hearing'); hudTitle.textContent = 'Слушаю…';
      } else if (s === 'thinking') {
        showHud('thinking', 'Думаю…', hudSub.textContent);
      } else if (s === 'error') {
        showHud('error', 'Не получилось', msg); hideHud(4800);
        if (a) a.auto();
      }
    },
    level(v) { hud.style.setProperty('--lvl', String(v)); },
    partial(text) { hudSub.textContent = '«' + text + '»'; },
    heard(text) {
      const a = api(); if (a) a.auto();
      const reply = handle(text);
      showHud('reply', 'Пип', reply); hudSub.textContent = reply;
      hudTitle.textContent = '«' + text + '»';
      say(reply); hideHud(5600);
    },
  };

  /* ---------- первое открытие: вкладка готова заранее ---------- */
  selectTab('care', true);
  window.addEventListener('resize', () => { const b = $('[data-tab="' + current + '"]', tabs); if (b && opened) placeIndicator(b, false); });
})();

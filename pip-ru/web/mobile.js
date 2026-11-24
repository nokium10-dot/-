/* ===== Пип · мобильный интерфейс: логика (обычный скрипт, выполняется до модуля симулятора) ===== */
(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const reduce = () => matchMedia('(prefers-reduced-motion: reduce)').matches;
  const bridge = () => window.PipAndroid || null;
  const buzz = () => { try { const b = bridge(); if (b && b.haptic) b.haptic(); } catch (_) { /* нет вибро */ } };
  const EASE_OUT = 'cubic-bezier(.16, 1, .3, 1)';

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
    if (prev) {
      if (animate) {
        prev.classList.add('leaving');
        const out = prev.animate(
          [{ opacity: 1, transform: 'none' }, { opacity: 0, transform: 'translateX(' + (-dir * 30) + 'px) scale(.985)' }],
          { duration: 200, easing: 'cubic-bezier(.4, 0, 1, 1)', fill: 'forwards' });
        out.onfinish = () => { prev.classList.remove('active', 'leaving'); out.cancel(); };
      } else prev.classList.remove('active');
    }
    next.classList.add('active');
    pages.scrollTop = 0;
    if (animate) {
      next.animate(
        [{ opacity: 0, transform: 'translateX(' + (dir * 38) + 'px)' }, { opacity: 1, transform: 'none' }],
        { duration: 460, easing: EASE_OUT });
      stagger(next);
    }
    if (!instant) buzz();
    if (name === 'look') refreshAssistant();
  }
  tabBtns.forEach(b => b.addEventListener('click', () => selectTab(b.dataset.tab)));

  function openSheet(tab) {
    if (!opened) {
      opened = true;
      document.body.classList.add('pr-open');
      backdrop.style.opacity = ''; backdrop.style.transition = '';
      sheet.style.transition = ''; sheet.style.transform = '';
      backdrop.classList.add('show');
      void sheet.offsetWidth;                       // фиксируем начальное состояние, чтобы сработала анимация
      sheet.classList.add('show');
      buzz();
      if (!current) selectTab(tab || 'care', true);
      else if (tab) selectTab(tab);
      requestAnimationFrame(() => { const b = $('[data-tab="' + current + '"]', tabs); if (b) placeIndicator(b, false); });
      if (!reduce()) stagger($('#pg-' + current));
    } else if (tab) selectTab(tab);
  }
  function closeSheet() {
    if (!opened) return;
    opened = false;
    document.body.classList.remove('pr-open');
    sheet.style.transition = ''; sheet.style.transform = '';
    backdrop.style.transition = ''; backdrop.style.opacity = '';
    sheet.classList.remove('show'); backdrop.classList.remove('show');
    const a = document.activeElement; if (a && a.blur) a.blur();
  }
  $('#pr-close').onclick = closeSheet;
  backdrop.addEventListener('pointerdown', closeSheet);
  addEventListener('keydown', e => { if (e.key === 'Escape') (modalDone ? closeModal(false) : closeSheet()); });

  /* смахивание шторки вниз за «ручку» и шапку */
  let drag = null;
  const dragStart = (e) => {
    if (!opened || e.target.closest('button')) return;
    drag = { y: e.clientY, t: performance.now(), dy: 0, el: e.currentTarget };
    drag.el.setPointerCapture(e.pointerId);
    sheet.style.transition = 'none'; backdrop.style.transition = 'none';
  };
  const dragMove = (e) => {
    if (!drag) return;
    drag.dy = Math.max(0, e.clientY - drag.y);
    sheet.style.transform = 'translate3d(0,' + drag.dy + 'px,0)';
    backdrop.style.opacity = String(Math.max(0, 1 - drag.dy / Math.max(1, sheet.offsetHeight * .8)));
  };
  const dragEnd = () => {
    if (!drag) return;
    const v = drag.dy / Math.max(1, performance.now() - drag.t);
    const close = drag.dy > sheet.offsetHeight * .22 || v > .7;
    drag = null;
    sheet.style.transition = ''; backdrop.style.transition = '';
    if (close) closeSheet();
    else { sheet.style.transform = ''; backdrop.style.opacity = ''; }
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
  $('#pr-open-look').onclick = () => openSheet('look');
  $('#pr-pill').onclick = () => openSheet('mood');
  $('#pr-pill').addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') openSheet('mood'); });
  $('#pr-mic').onclick = () => startVoice();
  $$('#pr-dock button').forEach(b => b.addEventListener('click', () => {
    const act = b.dataset.act;
    if (act === 'menu') { openSheet('care'); return; }
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

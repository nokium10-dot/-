#!/usr/bin/env python3
"""Собирает мобильный русский интерфейс Пипа из оригинального sim/web/index.html (Pip-KeyPet).

Оригинал не копируется целиком в репозиторий: на каждой сборке он берётся из GitHub и патчится.
Если исходник изменился и нужное место не найдено, сборка падает с понятной ошибкой.
"""
import argparse
import sys
from pathlib import Path

ap = argparse.ArgumentParser()
ap.add_argument("--src", required=True, help="оригинальный sim/web/index.html")
ap.add_argument("--web", default=str(Path(__file__).resolve().parent.parent / "web"), help="папка с mobile.*")
ap.add_argument("--out", required=True, help="куда записать готовый index.html")
args = ap.parse_args()

web = Path(args.web)
s = Path(args.src).read_text(encoding="utf-8")


def patch(old: str, new: str, label: str) -> None:
    global s
    if s.count(old) != 1:
        sys.exit(f"Не найдено место для патча «{label}» (найдено {s.count(old)} раз). "
                 f"Исходник Pip-KeyPet изменился — обновите pip-ru/tools/build_web.py.")
    s = s.replace(old, new, 1)


# --- страница ---
patch('<meta name="viewport" content="width=device-width, initial-scale=1">',
      '<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover">\n'
      '<meta name="theme-color" content="#050609">', "viewport")
patch("<title>KeyPet Simulator</title>", "<title>Пип</title>", "title")

# --- three.js берём из пакета, а не из CDN (приложение работает без интернета) ---
patch("https://cdn.jsdelivr.net/npm/three@0.170.0/build/three.module.js", "./three.module.js", "three")
patch("https://cdn.jsdelivr.net/npm/three@0.170.0/examples/jsm/", "./three/addons/", "three addons")

# --- мобильный интерфейс: стили, разметка, логика ---
css = (web / "mobile.css").read_text(encoding="utf-8")
html = (web / "mobile.html").read_text(encoding="utf-8")
js = (web / "mobile.js").read_text(encoding="utf-8")
patch("</style>", css + "\n</style>", "css")
patch('<script type="importmap">', html + "\n<script>\n" + js + "\n</script>\n\n" + '<script type="importmap">', "markup")

# --- 3D-сцена и ввод ---
patch("renderer.setPixelRatio(devicePixelRatio);",
      "renderer.setPixelRatio(Math.min(devicePixelRatio, 2)); /* телефоны с DPR 3+ не тянут */", "pixel ratio")

patch("let camDist = 175;", "let camDist = 175, fitScale = 1;", "camDist")
patch("  camera.aspect = w / h;\n  camera.updateProjectionMatrix();",
      "  camera.aspect = w / h;\n  camera.updateProjectionMatrix();\n"
      "  /* в портретной ориентации брелок должен целиком помещаться по ширине */\n"
      "  fitScale = Math.max(1, 70 / (2 * Math.tan(THREE.MathUtils.degToRad(13)) * camera.aspect) / 175);",
      "fit camera")
patch("camera.position.set(0, camDist * 0.16, camDist);",
      "const camD = camDist * fitScale;\n  camera.position.set(0, camD * 0.16, camD);", "camera position")

patch("const canvas = renderer.domElement;\ncanvas.addEventListener('pointerdown', e => {\n  canvas.setPointerCapture(e.pointerId);",
      """const canvas = renderer.domElement;
/* щипок двумя пальцами = масштаб брелока */
const ptrs = new Map();
let pinch = null;
const pdist = () => { const [a, b] = [...ptrs.values()]; return Math.hypot(a.x - b.x, a.y - b.y); };
canvas.addEventListener('contextmenu', e => e.preventDefault());
canvas.addEventListener('pointerdown', e => {
  ptrs.set(e.pointerId, { x: e.clientX, y: e.clientY });
  if (ptrs.size === 2) {
    if (touch) M._sim_touch(touch[0], touch[1], 0);
    if (held) releaseButton(held);
    touch = drag = null;
    pinch = { d: Math.max(1, pdist()), z: camDist };
    return;
  }
  if (ptrs.size > 2) return;
  canvas.setPointerCapture(e.pointerId);""", "pinch down")

patch("canvas.addEventListener('pointermove', e => {\n  if (touch && !drag) {",
      """canvas.addEventListener('pointermove', e => {
  if (ptrs.has(e.pointerId)) ptrs.set(e.pointerId, { x: e.clientX, y: e.clientY });
  if (pinch) {
    if (ptrs.size >= 2) camDist = Math.min(320, Math.max(90, pinch.z * pinch.d / Math.max(1, pdist())));
    return;
  }
  if (touch && !drag) {""", "pinch move")

patch("canvas.addEventListener('pointerup', release);\ncanvas.addEventListener('pointercancel', release);",
      """const endPtr = e => {
  ptrs.delete(e.pointerId);
  if (pinch) { if (ptrs.size < 2) pinch = null; return; }
  release();
};
canvas.addEventListener('pointerup', endPtr);
canvas.addEventListener('pointercancel', endPtr);""", "pinch up")

# --- подтверждение «Новый питомец» без системного confirm() ---
patch("document.getElementById('newpet').onclick = () => {\n  if (!confirm('Начать с новым Пипом? Текущий забудется.')) return;",
      "document.getElementById('newpet').onclick = async () => {\n"
      "  if (!(await window.pipConfirm('Начать с новым Пипом?', 'Текущий Пип будет забыт навсегда.'))) return;",
      "new pet confirm")

# --- мост между страницей, голосом и Android ---
patch("window.sim = { M, tgt, cur, dev, advance };", """window.setPipCaseColor = (bodyColor, trimColor) => {
  brushed.color.set(bodyColor);
  polished.color.set(trimColor);
  store.set('keypet.caseColor', bodyColor);
  store.set('keypet.trimColor', trimColor);
};
{
  const sb = store.get('keypet.caseColor'), st = store.get('keypet.trimColor');
  if (sb && st) { brushed.color.set(sb); polished.color.set(st); }
}
const moodIndex = name => {
  for (let i = 0; i < M._sim_mood_count(); i++) if (M.UTF8ToString(M._sim_mood_name(i)) === name) return i;
  return -1;
};
window.pipApi = {
  feed: () => M._sim_action(0),
  play: () => M._sim_action(1),
  pet: () => document.getElementById('pet').onclick(),
  poke: () => poke(),
  shake: () => { shakeLeft = 1.6; },
  button: id => pressButton(buttons[id]),
  mood: name => { const i = moodIndex(name); if (i < 0) return false; M._sim_set_mood(i); return true; },
  auto: () => M._sim_auto(1),
  volume: v => { startAudio(); M._sim_volume(v); },
  status: () => JSON.parse(M.UTF8ToString(M._sim_status())),
  ru: name => RU[name] || name,
  save: () => savePet(),
  resetZoom: () => { camDist = 175; },
};
window.sim = { M, tgt, cur, dev, advance };
document.documentElement.classList.add('pip-ready');
if (window.PipAndroid && window.PipAndroid.onReady) window.PipAndroid.onReady();""", "bridge")

out = Path(args.out)
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(s, encoding="utf-8")
print(f"ok: {out} ({len(s)//1024} КБ)")

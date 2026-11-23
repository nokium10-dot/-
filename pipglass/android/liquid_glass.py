from pathlib import Path

p = Path("pipglass/src/main/assets/pip/index.html")
s = p.read_text(encoding="utf-8")
css = r"""
/* Pip Glass — mobile Liquid Glass finish */
:root {
  --glass-bg: rgba(16, 21, 42, .54);
  --glass-edge: rgba(210, 224, 255, .24);
  --glass-highlight: rgba(255,255,255,.14);
  --glass-blue: #a9c4ff;
  --glass-mint: #82f0d9;
}
html, body { background:#050711 !important; }
#pip-alt-hud .online, #pip-alt-status, #pip-settings .settings-card,
#pip-settings button, #pip-mod-panel .mod-row, #pip-actions-trigger,
#pip-actions-sheet, #pip-actions-sheet button, #pip-settings .back,
#pip-mod-panel .back {
  background: linear-gradient(145deg, rgba(255,255,255,.13), rgba(255,255,255,.035)) !important;
  border: 1px solid var(--glass-edge) !important;
  box-shadow: inset 0 1px 0 var(--glass-highlight), 0 12px 36px rgba(0,0,0,.28) !important;
  -webkit-backdrop-filter: blur(24px) saturate(165%);
  backdrop-filter: blur(24px) saturate(165%);
}
#pip-alt-status { border-radius:18px !important; }
#pip-alt-status .meter i { background:linear-gradient(90deg,var(--glass-blue),var(--glass-mint)) !important; }
#pip-actions-trigger { border-radius:20px !important; color:#eaf1ff !important; }
#pip-actions-sheet { border-radius:28px 28px 0 0 !important; }
#pip-settings, #pip-mod-panel {
  background: radial-gradient(ellipse at 15% 0%, rgba(91,118,190,.28), transparent 42%),
              linear-gradient(155deg, rgba(14,19,39,.97), rgba(4,6,14,.98)) !important;
}
#pip-settings .settings-head h3, #pip-mod-panel h3 { color:#f5f7ff; letter-spacing:.2px; }
#pip-settings .section-title, #pip-actions-sheet .section-title { color:#b9c8e8; }
#pip-settings button:active, #pip-actions-sheet button:active { background:rgba(145,177,255,.25) !important; }
#pip-settings .swatch { border-radius:16px !important; }
#pip-alt-hud .brand { letter-spacing:3.5px; }
@media (max-width: 420px) {
  #pip-alt-status { left:10px; bottom:calc(max(14px,env(safe-area-inset-bottom)) + 66px); }
  #pip-alt-hud { top:max(12px,env(safe-area-inset-top)); }
  #pip-actions-sheet { padding-left:14px; padding-right:14px; }
}
"""
if "</style>" not in s:
    raise SystemExit("Style block not found in Pip simulator page")
s = s.replace("</style>", css + "\n</style>", 1)
p.write_text(s, encoding="utf-8")

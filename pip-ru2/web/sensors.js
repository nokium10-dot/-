/* Датчики телефона: Пип чувствует наклон и тряску. Акселерометр и гироскоп телефона идут прямо в прошивку Пипа,
   а 3D-брелок наклоняется вместе с телефоном. Положение «в руке» запоминается и медленно подстраивается. */
(() => {
  if (window.pipSensor) return;
  const G = 9.80665, UP = [0, 1, 0];
  const S = { on: true, raw: null, gyro: [0, 0, 0], at: 0, lp: null, base: null };
  try { if (localStorage.getItem('pip.sensors') === '0') S.on = false; } catch (_) { /* без хранилища */ }

  const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
  const unit = (v) => { const l = Math.hypot(v[0], v[1], v[2]) || 1; return [v[0] / l, v[1] / l, v[2] / l]; };
  /* поворот, переводящий направление u в w (формула Родрига) */
  function rotation(u, w) {
    let k = cross(u, w);
    const s = Math.hypot(k[0], k[1], k[2]), c = dot(u, w);
    if (s < 1e-6) {
      if (c > 0) return (v) => v;
      k = Math.abs(u[0]) < 0.9 ? cross(u, [1, 0, 0]) : cross(u, [0, 1, 0]);
    }
    k = unit(k);
    const ss = s < 1e-6 ? 0 : s;
    return (v) => {
      const kv = dot(k, v), kxv = cross(k, v);
      return [v[0] * c + kxv[0] * ss + k[0] * kv * (1 - c),
              v[1] * c + kxv[1] * ss + k[1] * kv * (1 - c),
              v[2] * c + kxv[2] * ss + k[2] * kv * (1 - c)];
    };
  }

  addEventListener('devicemotion', (e) => {
    const a = e.accelerationIncludingGravity;
    if (!a || a.x == null || a.y == null || a.z == null) return;
    S.raw = [a.x / G, a.y / G, a.z / G];
    const r = e.rotationRate;
    S.gyro = r && r.beta != null ? [r.beta, r.gamma || 0, r.alpha || 0] : [0, 0, 0]; /* °/с вокруг x, y, z */
    S.at = performance.now();
  });

  /* вызывается каждый кадр симуляции; null — датчиков нет или они выключены */
  function frame(dt) {
    if (!S.on || !S.raw || performance.now() - S.at > 600) return null;
    const raw = S.raw;
    const k = 1 - Math.exp(-dt * 6);
    S.lp = S.lp ? S.lp.map((x, i) => x + (raw[i] - x) * k) : raw.slice();
    const still = Math.abs(Math.hypot(raw[0], raw[1], raw[2]) - 1) < 0.15;
    const cur = unit(S.lp);
    if (!S.base) S.base = cur;
    else if (still) {
      const b = 1 - Math.exp(-dt / 10);
      S.base = unit(S.base.map((x, i) => x + (cur[i] - x) * b));
    }
    const R = rotation(S.base, UP);
    const acc = R(raw), gy = R(S.gyro), v = unit(R(S.lp));
    return {
      ax: acc[0], ay: acc[1], az: acc[2], gx: gy[0], gy: gy[1], gz: gy[2],
      pitch: Math.asin(Math.max(-1, Math.min(1, -v[2]))) * 0.95, roll: Math.atan2(v[0], v[1]), yaw: 0,
    };
  }

  window.pipSensor = {
    frame,
    enabled: (on) => {
      if (on === undefined) return S.on;
      S.on = !!on; S.lp = null; S.base = null;
      try { localStorage.setItem('pip.sensors', on ? '1' : '0'); } catch (_) { /* без хранилища */ }
      return S.on;
    },
  };

  /* тумблер во вкладке «Настройки» (только в основном приложении) */
  const after = document.getElementById('pr-mouth-row');
  if (after) {
    const row = document.createElement('label');
    row.className = 'pr-switch';
    row.innerHTML = '<span class="txt"><b>Датчики телефона</b>' +
      '<small>Пип наклоняется и поворачивается вместе с телефоном, а если телефон потрясти — трясётся и Пип</small></span>' +
      '<input type="checkbox" id="pr-sensors"><i class="knob" aria-hidden="true"></i>';
    row.style.marginTop = '10px';
    after.after(row);
    const cb = row.querySelector('input');
    cb.checked = S.on;
    cb.addEventListener('change', () => {
      window.pipSensor.enabled(cb.checked);
      try { const b = window.PipAndroid; if (b && b.haptic) b.haptic(); } catch (_) { /* нет вибро */ }
    });
  }
})();

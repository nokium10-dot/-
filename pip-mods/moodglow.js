(() => {
  const id='moodglow';
  if (!window.PipMods || window.PipMods.has(id)) return;
  window.PipMods.register(id, () => {
    const style=document.createElement('style');
    style.id='pipmod-moodglow';
    style.textContent='#view:after{content:"";position:absolute;inset:0;pointer-events:none;box-shadow:inset 0 0 34px var(--pip-mood-color,#75d9ff);opacity:.32}';
    document.head.append(style);
    let n=0;
    const colors=['#75d9ff','#ff83b5','#a78bfa','#8de6a2','#ffd27a','#f5f5ff'];
    const tick=()=>document.documentElement.style.setProperty('--pip-mood-color',colors[n++%colors.length]);
    tick();const timer=setInterval(tick,4200);
    return()=>{clearInterval(timer);style.remove();document.documentElement.style.removeProperty('--pip-mood-color');};
  });
})();
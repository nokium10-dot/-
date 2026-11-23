(() => {
 const id='moodglow'; if(!window.PipMods || window.PipMods.has(id)) return;
 window.PipMods.register(id, () => {
  const style=document.createElement('style');style.id='pipmod-moodglow';style.textContent='#view canvas{filter:drop-shadow(0 0 13px var(--pip-mood-color,#75d9ff))}';
  document.head.append(style);let n=0;const colors=['#75d9ff','#ff83b5','#a78bfa','#8de6a2','#ffd27a','#f5f5ff'];
  const tick=()=>{document.documentElement.style.setProperty('--pip-mood-color',colors[n++%colors.length])};
  const timer=setInterval(tick,2600);tick();
  return ()=>{clearInterval(timer);style.remove();document.documentElement.style.removeProperty('--pip-mood-color')};
 });
})();
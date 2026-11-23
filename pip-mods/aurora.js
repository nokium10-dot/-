(() => {
  const id = 'aurora';
  if (!window.PipMods || window.PipMods.has(id)) return;
  window.PipMods.register(id, () => {
    const layer = document.createElement('div');
    layer.id = 'pipmod-aurora';
    Object.assign(layer.style, {
      position:'fixed', inset:'7% 9% 12%', pointerEvents:'none', zIndex:'2',
      borderRadius:'48%', opacity:'.34',
      background:'conic-gradient(from 30deg, transparent 0 18%, #25d9c477 28%, #8274ff66 46%, transparent 58% 70%, #e96db766 84%, transparent 96%)',
      animation:'pipAurora 24s linear infinite'
    });
    const style=document.createElement('style');
    style.textContent='@keyframes pipAurora{to{transform:rotate(360deg)}}';
    document.head.append(style); document.body.append(layer);
    return () => { layer.remove(); style.remove(); };
  });
})();
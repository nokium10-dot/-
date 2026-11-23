(() => {
  const id='nightlight';
  if (!window.PipMods || window.PipMods.has(id)) return;
  window.PipMods.register(id, () => {
    const layer=document.createElement('div');layer.id='pipmod-nightlight';
    Object.assign(layer.style,{position:'fixed',inset:'0',pointerEvents:'none',zIndex:'4',background:'radial-gradient(ellipse at center,transparent 48%,rgba(255,174,112,.075) 100%)'});
    document.body.append(layer);return()=>layer.remove();
  });
})();
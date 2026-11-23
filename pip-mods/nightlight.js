(() => {
 const id='nightlight'; if(!window.PipMods || window.PipMods.has(id)) return;
 window.PipMods.register(id, () => {
  const layer=document.createElement('div');layer.id='pipmod-nightlight';
  Object.assign(layer.style,{position:'fixed',inset:'0',pointerEvents:'none',zIndex:'4',background:'radial-gradient(ellipse at center,transparent 45%,rgba(255,160,90,.10) 100%)',mixBlendMode:'screen'});
  document.body.append(layer);document.documentElement.style.setProperty('--pip-nightlight','1');
  return ()=>{layer.remove();document.documentElement.style.removeProperty('--pip-nightlight')};
 });
})();
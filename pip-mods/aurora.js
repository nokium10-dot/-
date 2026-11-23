(() => {
 const id='aurora'; if(!window.PipMods || window.PipMods.has(id)) return;
 window.PipMods.register(id, () => {
  const root=document.createElement('div'); root.id='pipmod-aurora';
  Object.assign(root.style,{position:'fixed',inset:'8% 8% 12%',pointerEvents:'none',zIndex:'2',borderRadius:'48%',filter:'blur(18px)',opacity:'.48',background:'conic-gradient(from 0deg,transparent,#28e6c5aa,#6b62ffaa,transparent,#e95fc8aa,transparent)',animation:'pipAurora 18s linear infinite'});
  const style=document.createElement('style'); style.textContent='@keyframes pipAurora{to{transform:rotate(360deg) scale(1.08)}}';
  document.head.append(style); document.body.append(root);
  return ()=>{root.remove();style.remove()};
 });
})();
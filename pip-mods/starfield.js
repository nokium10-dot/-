(() => {
  const id='starfield';
  if (!window.PipMods || window.PipMods.has(id)) return;
  window.PipMods.register(id, () => {
    const layer=document.createElement('div');layer.id='pipmod-stars';
    Object.assign(layer.style,{position:'fixed',inset:'0',pointerEvents:'none',zIndex:'1',opacity:'.42',backgroundImage:'radial-gradient(1px 1px at 12% 18%,#dcecff 98%,transparent),radial-gradient(1px 1px at 72% 28%,#dcecff 98%,transparent),radial-gradient(1.5px 1.5px at 38% 76%,#dcecff 98%,transparent),radial-gradient(1px 1px at 88% 82%,#dcecff 98%,transparent),radial-gradient(1px 1px at 56% 54%,#dcecff 98%,transparent)',backgroundSize:'260px 230px',animation:'pipStars 38s linear infinite'});
    const style=document.createElement('style');style.textContent='@keyframes pipStars{to{background-position:260px 230px}}';
    document.head.append(style);document.body.append(layer);
    return()=>{layer.remove();style.remove();};
  });
})();
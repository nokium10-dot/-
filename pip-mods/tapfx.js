(() => {
  const id='tapfx';
  if (!window.PipMods || window.PipMods.has(id)) return;
  window.PipMods.register(id, () => {
    const pending=new Set();
    const handler=e=>{
      if(e.target.closest('button,section'))return;
      const p=document.createElement('i');
      Object.assign(p.style,{position:'fixed',left:e.clientX+'px',top:e.clientY+'px',width:'7px',height:'7px',border:'1px solid #9be7ff',borderRadius:'50%',pointerEvents:'none',zIndex:'50',transform:'translate(-50%,-50%)',boxShadow:'0 0 8px #78dfff',transition:'width .45s ease,height .45s ease,opacity .45s ease'});
      document.body.append(p);pending.add(p);requestAnimationFrame(()=>{p.style.width='30px';p.style.height='30px';p.style.opacity='0';});
      setTimeout(()=>{p.remove();pending.delete(p);},480);
    };
    document.addEventListener('pointerdown',handler,{passive:true});
    return()=>{document.removeEventListener('pointerdown',handler);pending.forEach(p=>p.remove());pending.clear();};
  });
})();
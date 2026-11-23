(() => {
 const id='tapfx'; if(!window.PipMods || window.PipMods.has(id)) return;
 window.PipMods.register(id, () => {
  const handler=e=>{if(e.target.closest('button,section'))return;const p=document.createElement('i');Object.assign(p.style,{position:'fixed',left:e.clientX+'px',top:e.clientY+'px',width:'8px',height:'8px',border:'1px solid #9be7ff',borderRadius:'50%',pointerEvents:'none',zIndex:'50',transform:'translate(-50%,-50%)',boxShadow:'0 0 12px #78dfff',transition:'all .55s ease,opacity .55s'});document.body.append(p);requestAnimationFrame(()=>{p.style.width='38px';p.style.height='38px';p.style.opacity='0'});setTimeout(()=>p.remove(),600)};
  document.addEventListener('pointerdown',handler);
  return ()=>document.removeEventListener('pointerdown',handler);
 });
})();
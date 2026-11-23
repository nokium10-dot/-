(() => {
  const id='quickgestures';
  if(!window.PipMods||window.PipMods.has(id))return;
  window.PipMods.register(id,()=>{
    const core=window.PipCore;if(!core)throw new Error('Симулятор ещё загружается');
    const canvas=document.querySelector('#view canvas');
    if(!canvas)throw new Error('Экран часов не найден');
    let start=null,lastTap=0;
    const down=e=>{start={x:e.clientX,y:e.clientY,t:Date.now()};};
    const up=e=>{
      if(!start)return;
      const dx=e.clientX-start.x,dy=e.clientY-start.y,dt=Date.now()-start.t;
      if(Math.hypot(dx,dy)<18&&dt<350){
        const now=Date.now();
        if(now-lastTap<330){core._sim_button(0);lastTap=0;}
        else lastTap=now;
      }else if(Math.abs(dx)>75&&Math.abs(dx)>Math.abs(dy)*1.35){
        if(dx<0)core._sim_button(0);
        else {core._sim_button(1);setTimeout(()=>core._sim_button_up(1),90);}
      }
      start=null;
    };
    canvas.addEventListener('pointerdown',down,{passive:true});
    canvas.addEventListener('pointerup',up,{passive:true});
    return()=>{canvas.removeEventListener('pointerdown',down);canvas.removeEventListener('pointerup',up);};
  });
})();
(() => {
  const id='autotheme';
  if(!window.PipMods||window.PipMods.has(id))return;
  window.PipMods.register(id,()=>{
    const core=window.PipCore;if(!core)throw new Error('Симулятор ещё загружается');
    const original=core._sim_theme();let last=-1;
    const tick=()=>{
      const h=new Date().getHours();
      const theme=h>=22||h<7?0:(h>=7&&h<18?2:1);
      if(theme!==last){core._sim_set_theme(theme);last=theme;}
    };
    tick();const timer=setInterval(tick,60000);
    return()=>{clearInterval(timer);core._sim_set_theme(original);};
  });
})();
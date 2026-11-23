(() => {
  const id='smartcare';
  if(!window.PipMods||window.PipMods.has(id))return;
  window.PipMods.register(id,()=>{
    const core=window.PipCore;if(!core)throw new Error('Симулятор ещё загружается');
    let lastFood=0,lastPlay=0;
    const tick=()=>{
      let s;try{s=JSON.parse(core.UTF8ToString(core._sim_status()));}catch(_){return;}
      if(s.asleep)return;
      const now=Date.now();
      if(s.food<.16&&now-lastFood>45*60*1000){core._sim_action(0);lastFood=now;}
      if(s.fun<.13&&s.energy>.45&&now-lastPlay>60*60*1000){core._sim_action(1);lastPlay=now;}
    };
    const timer=setInterval(tick,30000);
    return()=>clearInterval(timer);
  });
})();
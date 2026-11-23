(() => {
  const id='moodbrain';
  if(!window.PipMods||window.PipMods.has(id))return;
  window.PipMods.register(id,()=>{
    const core=window.PipCore;if(!core)throw new Error('Симулятор ещё загружается');
    let last='',lastAt=0;
    const tick=()=>{
      let s;try{s=JSON.parse(core.UTF8ToString(core._sim_status()));}catch(_){return;}
      if(s.asleep)return;
      const now=Date.now();if(now-lastAt<18000)return;
      let mood=-1;
      if(s.energy<.22)mood=5;
      else if(s.fun<.2)mood=2;
      else if(s.love>.82&&s.fun>.65)mood=9;
      else if(s.fun>.82&&s.energy>.55)mood=1;
      else if(s.reaction==='surprised'||s.reaction==='bang')mood=4;
      if(mood>=0&&mood!==last){core._sim_set_mood(mood);last=mood;lastAt=now;}
    };
    const timer=setInterval(tick,2500);tick();
    return()=>clearInterval(timer);
  });
})();
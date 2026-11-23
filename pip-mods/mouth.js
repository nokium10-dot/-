(() => {
  const id='mouth';
  if(!window.PipMods||window.PipMods.has(id))return;
  window.PipMods.register(id,()=>{
    let current={open:.10,width:.62,curve:.2,round:.5,kind:'smile'};
    let from={...current},target={...current},changed=0,lastMood='neutral',lastTime=performance.now();
    const profiles={
      happy:{open:.18,width:.66,curve:.9,round:.55,kind:'smile'},
      love:{open:.19,width:.64,curve:1,round:.62,kind:'smile'},
      content:{open:.14,width:.62,curve:.72,round:.5,kind:'smile'},
      comfy:{open:.10,width:.59,curve:.52,round:.48,kind:'smile'},
      cheeky:{open:.16,width:.63,curve:1.05,round:.42,kind:'smile'},
      smug:{open:.08,width:.57,curve:.7,round:.34,kind:'smile'},
      sad:{open:.10,width:.60,curve:-.78,round:.4,kind:'frown'},
      gloomy:{open:.08,width:.58,curve:-.86,round:.38,kind:'frown'},
      lonely:{open:.075,width:.57,curve:-.7,round:.4,kind:'frown'},
      despondent:{open:.07,width:.56,curve:-.88,round:.38,kind:'frown'},
      hurt:{open:.13,width:.60,curve:-.48,round:.48,kind:'frown'},
      afraid:{open:.22,width:.27,curve:0,round:.9,kind:'oval'},
      surprised:{open:.32,width:.25,curve:0,round:1,kind:'oval'},
      shocked:{open:.36,width:.28,curve:0,round:1,kind:'oval'},
      amazed:{open:.27,width:.30,curve:0,round:.95,kind:'oval'},
      angry:{open:.11,width:.61,curve:-.12,round:.28,kind:'flat'},
      furious:{open:.13,width:.63,curve:-.2,round:.25,kind:'flat'},
      rage:{open:.15,width:.63,curve:-.24,round:.25,kind:'flat'},
      annoyed:{open:.09,width:.58,curve:-.12,round:.25,kind:'flat'},
      asleep:{open:.05,width:.55,curve:.18,round:.25,kind:'sleep'},
      sleepy:{open:.06,width:.57,curve:.2,round:.28,kind:'sleep'},
      tired:{open:.055,width:.56,curve:.12,round:.25,kind:'sleep'},
      curious:{open:.16,width:.25,curve:0,round:.85,kind:'oval'},
      puzzled:{open:.14,width:.27,curve:0,round:.82,kind:'oval'},
      skeptical:{open:.06,width:.37,curve:-.1,round:.3,kind:'flat'},
      stoned:{open:.1,width:.41,curve:.38,round:.58,kind:'smile'},
      pinkeye:{open:.05,width:.36,curve:-.2,round:.3,kind:'flat'},
      nauseous:{open:.07,width:.38,curve:-.4,round:.35,kind:'frown'},
      sick:{open:.06,width:.37,curve:-.5,round:.38,kind:'frown'},
      exhausted:{open:.025,width:.34,curve:.08,round:.25,kind:'sleep'},
      belligerent:{open:.1,width:.4,curve:-.25,round:.3,kind:'flat'},
      swearing:{open:.23,width:.34,curve:0,round:.9,kind:'oval'},
      indignant:{open:.1,width:.38,curve:-.22,round:.3,kind:'flat'},
      disgusted:{open:.06,width:.38,curve:-.42,round:.38,kind:'frown'},
      suspicious:{open:.045,width:.35,curve:-.12,round:.28,kind:'flat'},
      cynical:{open:.07,width:.4,curve:.46,round:.36,kind:'smile'},
      nervous:{open:.16,width:.27,curve:0,round:.9,kind:'oval'},
      dull:{open:.025,width:.34,curve:.08,round:.25,kind:'sleep'},
      shy:{open:.075,width:.39,curve:.58,round:.46,kind:'smile'},
      dismayed:{open:.12,width:.33,curve:-.58,round:.8,kind:'oval'},
      yes:{open:.13,width:.43,curve:.78,round:.48,kind:'smile'},
      no:{open:.06,width:.37,curve:-.2,round:.28,kind:'flat'},
      neutral:{open:.085,width:.60,curve:.18,round:.35,kind:'flat'}
    };

    const readStatus=core=>{
      try{return JSON.parse(core.UTF8ToString(core._sim_status()));}catch(_){return{};}
    };

    const draw=(ctx,size,off,core)=>{
      const now=performance.now(),dt=Math.min(.05,(now-lastTime)/1000);lastTime=now;
      const status=readStatus(core);
      // The native simulator reports "menu", "pip", or the active app ID.
      // Keep the mouth on the face only, never over the native menu/apps.
      if(status.screen && status.screen!=='pip')return;
      if(document.getElementById('pip-actions-sheet')?.classList.contains('open') ||
         document.getElementById('pip-settings')?.classList.contains('open') ||
         document.getElementById('pip-mod-panel')?.classList.contains('open')) return;

      const mood=status.mood||'neutral';
      if(mood!==lastMood){
        from={...current};target={...(profiles[mood]||profiles.neutral)};
        changed=now;lastMood=mood;
      }
      const p=Math.min(1,(now-changed)/420),ease=1-Math.pow(1-p,3);
      for(const k of ['open','width','curve','round'])current[k]=from[k]+(target[k]-from[k])*ease;
      current.kind=target.kind;

      // Subtle organic motion, like the softly wobbling white eye shapes.
      const energy=(mood==='happy'||mood==='love'||mood==='curious')?1:.45;
      const wobble=Math.sin(now/185)*.012*energy+Math.sin(now/337)*.006;
      const breathe=1+Math.sin(now/520)*.012;
      const cx=off+size*(.5+Math.sin(now/680)*.002);
      const cy=off+size*(.718+Math.sin(now/430)*.003);
      const w=size*current.width*breathe;
      const h=size*(.085+current.open*.28);
      const curve=current.curve+wobble*18;
      const round=Math.max(.18,Math.min(1,current.round));
      ctx.save();
      ctx.translate(cx,cy);
      ctx.rotate(Math.sin(now/720)*.008);
      ctx.scale(1+wobble,1-wobble*.35);
      ctx.globalAlpha=1;
      ctx.globalCompositeOperation='source-over';
      ctx.filter='none';
      ctx.fillStyle='rgb(255,255,255)';
      ctx.shadowColor='rgba(255,255,255,.12)';
      ctx.shadowBlur=size*.004;

      // Every expression is a single solid-white silhouette: no outline,
      // teeth, inner strokes, or coloured details.
      if(current.kind==='oval'){
        const ow=w*(.38+current.open*.35);
        const oh=h*(.78+current.open*.9);
        ctx.beginPath();
        ctx.ellipse(0,0,ow,oh,Math.sin(now/510)*.025,0,Math.PI*2);
        ctx.fill();
      }else if(current.kind==='sleep'){
        const sh=size*(.012+current.open*.035);
        ctx.beginPath();
        ctx.moveTo(-w*.46,0);
        ctx.bezierCurveTo(-w*.22,sh*1.3,w*.22,sh*1.3,w*.46,0);
        ctx.quadraticCurveTo(0,sh*.6,-w*.46,0);
        ctx.fill();
      }else{
        const depth=h*(.36+round*.42);
        const edge=h*(.08+round*.16);
        const sign=current.kind==='frown'?-1:1;
        const center=sign*depth;
        ctx.beginPath();
        ctx.moveTo(-w*.5,0);
        ctx.bezierCurveTo(-w*.34,center+edge,w*.34,center+edge,w*.5,0);
        ctx.bezierCurveTo(w*.34,center+edge+h*.22,-w*.34,center+edge+h*.22,-w*.5,0);
        ctx.closePath();
        ctx.fill();
      }
      ctx.restore();
    };
    window.PipMods.addScreenLayer(id,draw);
    return()=>window.PipMods.removeScreenLayer(id);
  });
})();

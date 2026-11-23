(() => {
  const id='mouth';
  if(!window.PipMods||window.PipMods.has(id))return;
  window.PipMods.register(id,()=>{
    let current={open:.12,width:.19,curve:.2,kind:'smile'},from={...current},target={...current},changed=0;
    let lastMood='neutral',lastTime=performance.now();
    const profiles={
      happy:{open:.24,width:.205,curve:.72,kind:'smile'},love:{open:.28,width:.205,curve:.82,kind:'smile'},
      content:{open:.18,width:.20,curve:.68,kind:'smile'},comfy:{open:.12,width:.19,curve:.48,kind:'smile'},
      cheeky:{open:.16,width:.195,curve:.82,kind:'smile'},smug:{open:.09,width:.18,curve:.7,kind:'smile'},
      sad:{open:.07,width:.18,curve:-.62,kind:'frown'},gloomy:{open:.05,width:.17,curve:-.68,kind:'frown'},
      lonely:{open:.05,width:.17,curve:-.55,kind:'frown'},despondent:{open:.04,width:.16,curve:-.7,kind:'frown'},
      hurt:{open:.12,width:.18,curve:-.35,kind:'frown'},afraid:{open:.26,width:.15,curve:0,kind:'oval'},
      surprised:{open:.38,width:.12,curve:0,kind:'oval'},shocked:{open:.42,width:.13,curve:0,kind:'oval'},
      amazed:{open:.32,width:.15,curve:0,kind:'oval'},angry:{open:.08,width:.18,curve:-.12,kind:'flat'},
      furious:{open:.12,width:.19,curve:-.2,kind:'flat'},rage:{open:.13,width:.19,curve:-.2,kind:'flat'},
      annoyed:{open:.05,width:.17,curve:-.12,kind:'flat'},asleep:{open:.025,width:.16,curve:.12,kind:'sleep'},
      sleepy:{open:.04,width:.17,curve:.18,kind:'sleep'},tired:{open:.035,width:.16,curve:.1,kind:'sleep'},
      curious:{open:.14,width:.13,curve:0,kind:'oval'},puzzled:{open:.13,width:.14,curve:0,kind:'oval'},
      skeptical:{open:.06,width:.16,curve:-.08,kind:'flat'},
      stoned:{open:.12,width:.18,curve:.35,kind:'smile'},pinkeye:{open:.05,width:.16,curve:-.2,kind:'flat'},
      nauseous:{open:.08,width:.16,curve:-.4,kind:'flat'},sick:{open:.06,width:.16,curve:-.45,kind:'frown'},
      exhausted:{open:.02,width:.15,curve:.08,kind:'sleep'},annoyed:{open:.05,width:.17,curve:-.12,kind:'flat'},
      belligerent:{open:.1,width:.18,curve:-.25,kind:'flat'},swearing:{open:.25,width:.17,curve:-.1,kind:'oval'},
      indignant:{open:.11,width:.17,curve:-.22,kind:'flat'},disgusted:{open:.06,width:.17,curve:-.38,kind:'frown'},
      suspicious:{open:.04,width:.15,curve:-.1,kind:'flat'},cynical:{open:.07,width:.18,curve:.42,kind:'smile'},
      nervous:{open:.18,width:.13,curve:0,kind:'oval'},dull:{open:.025,width:.15,curve:.08,kind:'sleep'},
      shy:{open:.08,width:.17,curve:.55,kind:'smile'},dismayed:{open:.14,width:.15,curve:-.5,kind:'frown'},
      yes:{open:.16,width:.19,curve:.65,kind:'smile'},no:{open:.06,width:.16,curve:-.2,kind:'flat'},
      neutral:{open:.045,width:.17,curve:.24,kind:'smile'}
    };
    const draw=(ctx,size,off,core)=>{
      const now=performance.now(),dt=Math.min(.05,(now-lastTime)/1000);lastTime=now;
      let mood='neutral';
      try{mood=JSON.parse(core.UTF8ToString(core._sim_status())).mood||mood;}catch(_){}
      if(mood!==lastMood){from={...current};target={...(profiles[mood]||profiles.neutral)};changed=now;lastMood=mood;}
      const p=Math.min(1,(now-changed)/360),ease=1-Math.pow(1-p,3);
      current.open=from.open+(target.open-from.open)*ease;
      current.width=from.width+(target.width-from.width)*ease;
      current.curve=from.curve+(target.curve-from.curve)*ease;
      current.kind=target.kind;
      const cx=off+size*.5,cy=off+size*.705,w=size*current.width,h=size*(.025+current.open*.10);
      const pulse=(mood==='love'||mood==='happy')?1+Math.sin(now/230)*.025:1;
      ctx.save();ctx.translate(cx,cy);ctx.scale(pulse,1);ctx.lineCap='round';ctx.lineJoin='round';
      ctx.shadowColor='rgba(18,4,12,.55)';ctx.shadowBlur=size*.008;
      ctx.strokeStyle='#6f3946';ctx.lineWidth=Math.max(2,size*.006);
      if(current.kind==='oval'){
        ctx.beginPath();ctx.ellipse(0,0,w*.34,h*.78,0,0,Math.PI*2);
        ctx.fillStyle='#542b39';ctx.fill();ctx.stroke();
        ctx.beginPath();ctx.ellipse(0,-h*.1,w*.22,h*.24,0,Math.PI,Math.PI*2);ctx.fillStyle='#f4a2ad';ctx.fill();
      }else if(current.kind==='sleep'){
        ctx.beginPath();ctx.moveTo(-w/2,0);ctx.quadraticCurveTo(0,h*.35,w/2,0);ctx.strokeStyle='#e8a0ae';ctx.lineWidth=Math.max(2,size*.008);ctx.stroke();
      }else if(current.kind==='flat'){
        ctx.beginPath();ctx.moveTo(-w/2,0);ctx.quadraticCurveTo(0,h*current.curve,w/2,0);ctx.strokeStyle='#e9a1ad';ctx.lineWidth=Math.max(2.5,size*.009);ctx.stroke();
        if(mood==='angry'||mood==='furious'||mood==='rage'){ctx.beginPath();ctx.moveTo(-w*.12,-h*.12);ctx.lineTo(-w*.02,h*.05);ctx.lineTo(w*.08,-h*.1);ctx.strokeStyle='#f3c1c5';ctx.lineWidth=Math.max(1.5,size*.004);ctx.stroke();}
      }else{
        ctx.beginPath();ctx.moveTo(-w/2,0);ctx.bezierCurveTo(-w*.27,h*(.85+current.curve),w*.27,h*(.85+current.curve),w/2,0);
        ctx.strokeStyle='#f0a1ad';ctx.lineWidth=Math.max(2.5,size*.009);ctx.stroke();
        if(current.open>.16){
          ctx.beginPath();ctx.moveTo(-w*.32,h*.12);ctx.quadraticCurveTo(0,h*(.9+current.open),w*.32,h*.12);ctx.closePath();
          ctx.fillStyle='#b94f69';ctx.fill();
          ctx.beginPath();ctx.moveTo(-w*.23,h*.17);ctx.quadraticCurveTo(0,h*.32,w*.23,h*.17);ctx.strokeStyle='#fff0ec';ctx.lineWidth=Math.max(1.5,size*.004);ctx.stroke();
        }
      }
      ctx.restore();
    };
    window.PipMods.addScreenLayer(id,draw);
    return()=>window.PipMods.removeScreenLayer(id);
  });
})();
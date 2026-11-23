(() => {
  const id='mouth';
  if (!window.PipMods || window.PipMods.has(id)) return;
  window.PipMods.register(id, () => {
    if (!window.PipMods.addScreenLayer) throw new Error('Рендерер экрана не готов');
    const draw=(ctx,size)=>{
      const cx=size/2,cy=size*.665;
      ctx.save();ctx.translate(cx,cy);ctx.lineCap='round';ctx.lineJoin='round';
      ctx.beginPath();ctx.moveTo(-25,-2);ctx.bezierCurveTo(-15,13,15,13,25,-2);ctx.quadraticCurveTo(0,24,-25,-2);ctx.closePath();
      ctx.fillStyle='#f18b9c';ctx.fill();
      ctx.beginPath();ctx.moveTo(-25,-2);ctx.bezierCurveTo(-14,15,14,15,25,-2);ctx.strokeStyle='#f4dce2';ctx.lineWidth=2.6;ctx.stroke();
      ctx.beginPath();ctx.moveTo(-12,9);ctx.quadraticCurveTo(0,14,12,9);ctx.strokeStyle='rgba(130,48,68,.62)';ctx.lineWidth=1.5;ctx.stroke();
      ctx.restore();
    };
    window.PipMods.addScreenLayer(id,draw);
    return()=>window.PipMods.removeScreenLayer&&window.PipMods.removeScreenLayer(id);
  });
})();
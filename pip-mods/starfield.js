(() => {
 const id='starfield'; if(!window.PipMods || window.PipMods.has(id)) return;
 window.PipMods.register(id, () => {
  const c=document.createElement('canvas'); c.id='pipmod-stars';
  Object.assign(c.style,{position:'fixed',inset:'0',width:'100%',height:'100%',pointerEvents:'none',zIndex:'1',opacity:'.65'});
  document.body.append(c); const ctx=c.getContext('2d'); let w=0,h=0,raf=0;
  const stars=Array.from({length:65},()=>({x:Math.random(),y:Math.random(),r:.4+Math.random()*1.2,v:.08+Math.random()*.22,a:.2+Math.random()*.65}));
  const resize=()=>{w=c.width=innerWidth*devicePixelRatio;h=c.height=innerHeight*devicePixelRatio}; resize(); addEventListener('resize',resize);
  const draw=()=>{ctx.clearRect(0,0,w,h);stars.forEach(s=>{s.y+=s.v/1000;if(s.y>1)s.y=0;ctx.globalAlpha=s.a;ctx.fillStyle='#d9e8ff';ctx.beginPath();ctx.arc(s.x*w,s.y*h,s.r*devicePixelRatio,0,Math.PI*2);ctx.fill()});raf=requestAnimationFrame(draw)};draw();
  return ()=>{cancelAnimationFrame(raf);removeEventListener('resize',resize);c.remove()};
 });
})();
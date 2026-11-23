(() => {
  const id='menulab';
  if(!window.PipMods||window.PipMods.has(id))return;
  window.PipMods.register(id,()=>{
    const core=window.PipCore;if(!core)throw new Error('Симулятор ещё загружается');
    const original=core._sim_menu_style();
    const styles=[0,1,2];let index=(original+1)%styles.length;
    core._sim_set_menu_style(styles[index]);
    const timer=setInterval(()=>{
      index=(index+1)%styles.length;
      core._sim_set_menu_style(styles[index]);
    },15*60*1000);
    return()=>{clearInterval(timer);core._sim_set_menu_style(original);};
  });
})();
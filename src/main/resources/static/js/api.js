/* api.js - wrapper fetch + toast system */
(function(global){
  async function request(method,url,body){
    try{
      const opts={method,credentials:'include',headers:{'Accept':'application/json'}};
      if(body!==undefined){opts.headers['Content-Type']='application/json';opts.body=JSON.stringify(body)}
      const res=await fetch(url,opts);
      if(res.status===204) return null;
      let data=null;
      try{data=await res.json()}catch(e){data=null}
      if(!res.ok){const msg=(data&&data.error) || (data&&data.message) || `Error HTTP ${res.status}`; const err=new Error(msg); err.status=res.status; err.data=data; throw err}
      return data;
    }catch(e){if(e instanceof TypeError) {const ne=new Error('Error de conexion con el servidor'); ne.status=0; throw ne} throw e}
  }

  const api={get:(u)=>request('GET',u),post:(u,b)=>request('POST',u,b),put:(u,b)=>request('PUT',u,b),del:(u)=>request('DELETE',u)};

  const toast=(function(){
    const container=document.getElementById('toast-container');
    function mk(type,msg,time){
      if(!container){alert(msg);return}
      const el=document.createElement('div');el.className=`toast ${type}`;el.textContent=msg;container.appendChild(el);
      setTimeout(()=>{el.remove()},time);
    }
    return {success:(m)=>mk('success',m,3500),error:(m)=>mk('error',m,4500),info:(m)=>mk('info',m,3500)}
  })();

  global.api=api;global.toast=toast;
})(window);
/* api.js - Placeholder Sprint 1 */
/* TODO: implementar en el Paso 5 */

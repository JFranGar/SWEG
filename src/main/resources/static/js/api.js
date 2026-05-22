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
      if(!res.ok){
        // Treat 401 from the session-check endpoint '/api/auth/me' as unauthenticated helper: return null
        if(res.status===401 && method==='GET' && url && url.indexOf('/api/auth/me')!==-1) return null;
        const msg = (data && data.error) || (data && data.message) || `Error HTTP ${res.status}`;
        throw {
          status: res.status,
          message: msg,
          fields: (data && data.fields) || {},
          data
        };
      }
      return data;
    }catch(e){if(e instanceof TypeError) {const ne=new Error('Error de conexion con el servidor'); ne.status=0; throw ne} throw e}
  }

  const api={get:(u)=>request('GET',u),post:(u,b)=>request('POST',u,b),put:(u,b)=>request('PUT',u,b),patch:(u,b)=>request('PATCH',u,b),del:(u)=>request('DELETE',u)};

  const toast=(function(){
    const container=document.getElementById('toast-container');
    function mk(type,msg,time){
      if(!container){alert(msg);return}
      const el=document.createElement('div');el.className=`toast ${type}`;el.textContent=msg;container.appendChild(el);
      setTimeout(()=>{el.remove()},time);
    }
    return {success:(m)=>mk('success',m,3500),error:(m)=>mk('error',m,4500),info:(m)=>mk('info',m,3500)}
  })();

  global.api = api;
  global.toast = toast;
})(window);

/**
 * Pinta errores por campo enviados por el backend (CA-HU02-04, CA-HU04-04).
 * Asume convención: input id="campo" + span id="err-campo".
 * Mapeo opcional para nombres distintos (ej. capacidadMaxima -> capacidad).
 */
window.pintarErroresCampo = function (err, mapeo = {}) {
  if (!err || !err.fields) return;
  Object.entries(err.fields).forEach(([campoBackend, msg]) => {
    const campoUi = mapeo[campoBackend] || campoBackend;
    const input = document.getElementById(campoUi);
    const span  = document.getElementById('err-' + campoUi);
    if (input) input.classList.add('error');
    if (span)  span.textContent = msg;
  });
};
/* end api.js */
/* api.js - Placeholder Sprint 1 */
/* TODO: implementar en el Paso 5 */

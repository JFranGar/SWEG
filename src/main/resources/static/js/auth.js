/* auth.js */
(function(){
  const form=document.getElementById('login-form');
  const correo=document.getElementById('correo');
  const contrasena=document.getElementById('contrasena');
  const errCorreo=document.getElementById('err-correo');
  const errPass=document.getElementById('err-contrasena');
  const btn=document.getElementById('btn-login');

  function limpiar(){[errCorreo,errPass].forEach(e=>e.textContent='');[correo,contrasena].forEach(i=>i.classList.remove('error'))}
  function validar(){let ok=true;limpiar();const re=/^[^\s@]+@[^\s@]+\.[^\s@]+$/; if(!correo.value.trim()){errCorreo.textContent='Correo obligatorio';correo.classList.add('error');ok=false}else if(!re.test(correo.value.trim())){errCorreo.textContent='Correo inválido';correo.classList.add('error');ok=false} if(!contrasena.value.trim()){errPass.textContent='Contraseña obligatoria';contrasena.classList.add('error');ok=false} return ok}

  async function onSubmit(e){e.preventDefault(); if(!validar()) return; const original=btn.innerHTML;btn.disabled=true;btn.innerHTML='<span class="spinner"></span> Ingresando...';
    try{
      const body={correo:correo.value.toLowerCase().trim(),contrasena:contrasena.value};
      const res=await api.post('/api/auth/login',body);
      toast.success('Bienvenido');
      if(res.rol==='ADMIN') location.href='/html/admin.html'; else if(res.rol==='CLIENTE') location.href='/html/cliente.html'; else if(res.rol==='RECEPCIONISTA') location.href='/html/recepcion.html'; else location.href='/html/login.html';
    }catch(err){
      if(err.status===401){toast.error('Credenciales no validas');correo.classList.add('error');contrasena.classList.add('error');errCorreo.textContent=' ';errPass.textContent=' ';}
      else if(err.status===423){toast.error(err.message)}
      else {toast.error(err.message||'Error')}
    }finally{btn.disabled=false;btn.innerHTML=original}
  }

  (async function init(){
    try{const me=await api.get('/api/auth/me'); if(me){if(me.rol==='ADMIN') return location.href='/html/admin.html'; if(me.rol==='CLIENTE') return location.href='/html/cliente.html'; if(me.rol==='RECEPCIONISTA') return location.href='/html/recepcion.html'}}catch(e){}
    form.addEventListener('submit',onSubmit);
  })();
})();
/* auth.js - Placeholder Sprint 1 */
/* TODO: implementar en el Paso 5 */

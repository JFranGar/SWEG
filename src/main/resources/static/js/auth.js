/* auth.js */
/**
 * HU01 - Inicio de Sesion.
 *
 * Decision de diseno (Informe Revision Sprint 1):
 *   El paso "Seleccionar rol" descrito en C51/C52/C61 NO existe en el sistema.
 *   El rol se determina en el SERVIDOR a partir de Usuario.rol y el cliente
 *   solo redirige al panel correspondiente. Esto es mas seguro
 *   (ISO 25010 - Confidencialidad) y mas KISS.
 */
(function(){
  const form=document.getElementById('login-form');
  const correo=document.getElementById('correo');
  const contrasena=document.getElementById('contrasena');
  const errCorreo=document.getElementById('err-correo');
  const errPass=document.getElementById('err-contrasena');
  const rolHidden = document.getElementById('rol-seleccionado');
  const roleCards = Array.from(document.querySelectorAll('.role-card'));
  const errRol = document.getElementById('err-rol');
  const btn=document.getElementById('btn-login');

  function limpiar(){[errCorreo,errPass].forEach(e=>e.textContent='');[correo,contrasena].forEach(i=>i.classList.remove('error'))}
  function validar(){let ok=true;limpiar();const re=/^[^\s@]+@[^\s@]+\.[^\s@]+$/; if(!correo.value.trim()){errCorreo.textContent='Correo obligatorio';correo.classList.add('error');ok=false}else if(!re.test(correo.value.trim())){errCorreo.textContent='Correo inválido';correo.classList.add('error');ok=false} if(!contrasena.value.trim()){errPass.textContent='Contraseña obligatoria';contrasena.classList.add('error');ok=false} return ok}

  function validarRol(){errRol.textContent=''; if(!rolHidden || !rolHidden.value){errRol.textContent='Debe seleccionar un rol'; return false} return true}

  async function onSubmit(e){e.preventDefault(); if(!validar()) return; const original=btn.innerHTML;btn.disabled=true;btn.innerHTML='<span class="spinner"></span> Ingresando...';
    try{
      if(!validarRol()) { toast.error('Seleccione un rol'); return; }
      const body={correo:correo.value.toLowerCase().trim(),contrasena:contrasena.value, rolSeleccionado: rolHidden.value};
      const res=await api.post('/api/auth/login',body);
      toast.success('Bienvenido');
      if(res.rol==='ADMIN') location.href='/html/admin.html'; else if(res.rol==='CLIENTE') location.href='/html/cliente.html'; else if(res.rol==='RECEPCIONISTA') location.href='/html/recepcionista.html'; else location.href='/html/login.html';
    }catch(err){
      if(err.status===400){
        try{pintarErroresCampo(err);}catch(e){}
        toast.error(err.message);
      } else if(err.status===401){
        toast.error('Credenciales no validas');
        correo.classList.add('error');
        contrasena.classList.add('error');
        errCorreo.textContent=' ';
        errPass.textContent=' ';
      } else if(err.status===403){
        // rol no coincide: resaltar cards
        try{const fields = err.fields || {}; roleCards.forEach(c=>c.classList.remove('active')); const sel = rolHidden && rolHidden.value; if(sel){ const card = document.querySelector('.role-card[data-rol="'+sel+'"]'); if(card) card.classList.add('active'); } }catch(e){}
        toast.error(err.message || 'Rol seleccionado no coincide');
      } else if(err.status===423){
        toast.error(err.message)
      } else {toast.error(err.message||'Error')}
    }finally{btn.disabled=false;btn.innerHTML=original}
  }

  (async function init(){
    // On the login page avoid calling /api/auth/me to prevent a visible 401 in Network tab.
    // The redirect-by-session check is useful on protected pages, but on login it only creates noise.

    // role card click handling
    roleCards.forEach(card=>card.addEventListener('click', ()=>{
      roleCards.forEach(c=>c.classList.remove('active'));
      card.classList.add('active');
      if(rolHidden) rolHidden.value = card.dataset.rol;
      if(errRol) errRol.textContent='';
    }));

    form.addEventListener('submit',onSubmit);
  })();
})();

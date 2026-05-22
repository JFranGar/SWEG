/* guards.js */
async function guard(rolesPermitidos){
  try{
    const me = await api.get('/api/auth/me');
    if(!me){ location.href='/html/login.html'; return null }
    if(!rolesPermitidos.includes(me.rol)){
      toast.error('Acceso denegado');
      setTimeout(()=>location.href='/html/login.html',800);
      return null;
    }
    const elCorreo=document.getElementById('user-correo'); if(elCorreo) elCorreo.textContent=me.correo;
    const elRol=document.getElementById('user-rol'); if(elRol) elRol.textContent=me.rol;
    const btnLogout=document.getElementById('btn-logout'); if(btnLogout) btnLogout.addEventListener('click',async()=>{await api.post('/api/auth/logout');location.href='/html/login.html'});
    return me;
  }catch(e){location.href='/html/login.html';return null}
}
/* guards.js - Placeholder Sprint 1 */
/* TODO: implementar en el Paso 5 */

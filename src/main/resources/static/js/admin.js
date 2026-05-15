/* admin.js */
(async function(){
  const me = await guard(['ADMIN']); if(!me) return;
  const tabla=document.getElementById('tabla-salas'); const form=document.getElementById('sala-form'); const idEl=document.getElementById('sala-id'); const nombre=document.getElementById('nombre'); const tipo=document.getElementById('tipo'); const capacidad=document.getElementById('capacidad'); const btnGuardar=document.getElementById('btn-guardar'); const btnCancelar=document.getElementById('btn-cancelar'); const errNombre=document.getElementById('err-nombre'); const errTipo=document.getElementById('err-tipo'); const errCap=document.getElementById('err-capacidad');

  function escapar(s){const d=document.createElement('div');d.textContent=s;return d.innerHTML}
  function limpiarErrores(){[errNombre,errTipo,errCap].forEach(e=>e.textContent='');[nombre,tipo,capacidad].forEach(i=>i.classList.remove('error'))}
  function modoCreacion(){idEl.value='';form.reset();document.getElementById('form-title').textContent='Nueva Sala';btnCancelar.style.display='none'}
  function modoEdicion(s){idEl.value=s.id;nombre.value=s.nombre;tipo.value=s.tipo;capacidad.value=s.capacidadMaxima;document.getElementById('form-title').textContent='Editar Sala #'+s.id;btnCancelar.style.display='inline-block';window.scrollTo({top:0,behavior:'smooth'})}

  async function cargarSalas(){try{const list=await api.get('/api/admin/salas'); tabla.innerHTML=''; if(!list.length){const tr=document.createElement('tr');tr.innerHTML='<td colspan="5" style="color:var(--text-muted)">Sin salas registradas</td>';tabla.appendChild(tr);return}
      list.forEach(s=>{const tr=document.createElement('tr');tr.innerHTML=`<td>${escapar(s.nombre)}</td><td>${escapar(s.tipo)}</td><td>${escapar(String(s.capacidadMaxima))}</td><td><span class="badge badge-active">${s.estado||'DISPONIBLE'}</span></td><td><button data-edit='${JSON.stringify(s)}' class='btn btn-secondary'>Editar</button> <button data-del='${s.id}' class='btn btn-danger'>Eliminar</button></td>`;tabla.appendChild(tr)})}catch(e){toast.error('Error cargando salas')}}

  function validar(){limpiarErrores();let ok=true; if(!nombre.value.trim()){errNombre.textContent='Nombre obligatorio';nombre.classList.add('error');ok=false} if(!tipo.value){errTipo.textContent='Tipo obligatorio';tipo.classList.add('error');ok=false} if(!capacidad.value || parseInt(capacidad.value)<=0){errCap.textContent='Capacidad inválida';capacidad.classList.add('error');ok=false} return ok}

  form.addEventListener('submit',async function(e){e.preventDefault(); if(!validar()) return; btnGuardar.disabled=true; const payload={nombre:nombre.value.trim(),tipo:tipo.value,capacidadMaxima:parseInt(capacidad.value)}; try{ if(idEl.value){await api.put('/api/admin/salas/'+idEl.value,payload); toast.success('Sala actualizada');} else {await api.post('/api/admin/salas',payload); toast.success('Sala creada');} modoCreacion(); await cargarSalas();}catch(err){ if(err.status===409){errNombre.textContent='Ya existe una sala con ese nombre';nombre.classList.add('error'); toast.error(err.message)} else {toast.error(err.message||'Error')} } finally{btnGuardar.disabled=false}})

  btnCancelar.addEventListener('click',modoCreacion);

  tabla.addEventListener('click',async function(e){const btn=e.target.closest('button'); if(!btn) return; if(btn.dataset.edit){const s=JSON.parse(btn.dataset.edit); modoEdicion(s)} else if(btn.dataset.del){ if(!confirm('Eliminar sala?')) return; try{await api.del('/api/admin/salas/'+btn.dataset.del); toast.success('Eliminada'); await cargarSalas()}catch(err){toast.error(err.message||'Error')}}});

  await cargarSalas();
})();
/* admin.js - Placeholder Sprint 1 */
/* TODO: implementar en el Paso 5 */

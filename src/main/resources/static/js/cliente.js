/* cliente.js */
(async function(){
  const me = await guard(['CLIENTE']); if(!me) return;
  const selSala=document.getElementById('sala'); const inpFecha=document.getElementById('fecha'); const horaInicio=document.getElementById('hora-inicio'); const horaFin=document.getElementById('hora-fin'); const btnReservar=document.getElementById('btn-reservar'); const tabla=document.getElementById('tabla-reservas'); const errSala=document.getElementById('err-sala'); const errFecha=document.getElementById('err-fecha'); const errInicio=document.getElementById('err-hora-inicio'); const errFin=document.getElementById('err-hora-fin');

  function limpiar(){[errSala,errFecha,errInicio,errFin].forEach(e=>e.textContent='');[selSala,inpFecha,horaInicio,horaFin].forEach(i=>i.classList.remove('error'))}
  function hoyMin(){const d=new Date();return d.toISOString().split('T')[0]}

  async function cargarSalas(){try{const list=await api.get('/api/cliente/reservas/salas-disponibles'); selSala.innerHTML=''; selSala.appendChild(new Option('Seleccione...','')); list.forEach(s=>selSala.appendChild(new Option(s.nombre,s.id)))}catch(e){toast.error('Error cargando salas')}}
  async function cargarMisReservas(){try{const list=await api.get('/api/cliente/reservas'); tabla.innerHTML=''; if(!list.length){const tr=document.createElement('tr');tr.innerHTML='<td colspan="5" style="color:var(--text-muted)">Sin reservas</td>';tabla.appendChild(tr);return} list.forEach(r=>{const tr=document.createElement('tr');tr.innerHTML=`<td>${r.sala.nombre}</td><td>${r.fecha}</td><td>${r.horaInicio}</td><td>${r.horaFin}</td><td><span class="badge badge-${r.estado.toLowerCase()}">${r.estado}</span></td>`;tabla.appendChild(tr)})}catch(e){toast.error('Error cargando reservas')}}

  function validar(){limpiar();let ok=true; if(!selSala.value){errSala.textContent='Seleccione una sala';selSala.classList.add('error');ok=false} if(!inpFecha.value){errFecha.textContent='Fecha obligatoria';inpFecha.classList.add('error');ok=false} else if(inpFecha.value < hoyMin()){errFecha.textContent='Fecha no puede ser anterior a hoy';inpFecha.classList.add('error');ok=false} if(!horaInicio.value){errInicio.textContent='Hora inicio obligatoria';horaInicio.classList.add('error');ok=false} if(!horaFin.value){errFin.textContent='Hora fin obligatoria';horaFin.classList.add('error');ok=false} if(horaInicio.value && horaFin.value && horaInicio.value>=horaFin.value){errInicio.textContent='Hora inicio debe ser anterior a hora fin';horaInicio.classList.add('error');ok=false} return ok}

  formSetup();
  async function formSetup(){inpFecha.min=hoyMin(); await cargarSalas(); await cargarMisReservas(); document.getElementById('reserva-form').addEventListener('submit',async function(e){e.preventDefault(); if(!validar()) return; btnReservar.disabled=true; try{const body={salaId:parseInt(selSala.value),fecha:inpFecha.value,horaInicio:horaInicio.value+':00',horaFin:horaFin.value+':00'}; const r=await api.post('/api/cliente/reservas',body); toast.success(r.mensaje||'Reserva creada'); document.getElementById('reserva-form').reset(); inpFecha.min=hoyMin(); await cargarMisReservas(); await cargarSalas(); }catch(err){ if(err.status===409){errSala.textContent='Horario no disponible';selSala.classList.add('error'); toast.error(err.message)} else {toast.error(err.message||'Error')} } finally{btnReservar.disabled=false}})}
})();
/* cliente.js - Placeholder Sprint 1 */
/* TODO: implementar en el Paso 5 */

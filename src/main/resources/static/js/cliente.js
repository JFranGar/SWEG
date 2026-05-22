/* cliente.js */
(async function(){
  const me = await guard(['CLIENTE']); if(!me) return;
  const selSala=document.getElementById('sala'); const inpFecha=document.getElementById('fecha'); const horaInicio=document.getElementById('hora-inicio'); const horaFin=document.getElementById('hora-fin'); const btnReservar=document.getElementById('btn-reservar'); const tabla=document.getElementById('tabla-reservas'); const errSala=document.getElementById('err-sala'); const errFecha=document.getElementById('err-fecha'); const errInicio=document.getElementById('err-hora-inicio'); const errFin=document.getElementById('err-hora-fin');
  const selSalaDispo = document.getElementById('sala-dispo'); const inpFechaDispo = document.getElementById('fecha-dispo'); const horaInicioDispo = document.getElementById('hora-inicio-dispo'); const horaFinDispo = document.getElementById('hora-fin-dispo'); const btnConsultar = document.getElementById('btn-consultar'); const resultadoDispo = document.getElementById('disponibilidad-result');

  function limpiar(){[errSala,errFecha,errInicio,errFin].forEach(e=>e.textContent='');[selSala,inpFecha,horaInicio,horaFin].forEach(i=>i.classList.remove('error'))}
  function hoyMin(){const d=new Date();return d.toISOString().split('T')[0]}

  async function cargarSalas(){try{const list=await api.get('/api/reservas/salas-disponibles'); selSala.innerHTML=''; selSala.appendChild(new Option('Seleccione...','')); list.forEach(s=>selSala.appendChild(new Option(s.nombre,s.id)))}catch(e){toast.error('Error cargando salas')}}
  async function cargarSalasParaDispo(){try{const list=await api.get('/api/reservas/salas-disponibles'); selSalaDispo.innerHTML=''; selSalaDispo.appendChild(new Option('Seleccione...','')); list.forEach(s=>selSalaDispo.appendChild(new Option(s.nombre,s.id)))}catch(e){toast.error('Error cargando salas')}}
  async function cargarMisReservas(){
    try{
      const list=await api.get('/api/reservas'); tabla.innerHTML='';
      if(!list.length){const tr=document.createElement('tr');tr.innerHTML='<td colspan="5" style="color:var(--text-muted)">Sin reservas</td>';tabla.appendChild(tr);return}
      list.forEach(r=>{
        const tr=document.createElement('tr');
        const acciones = (r.estado==='PENDIENTE' || r.estado==='CONFIRMADA') ? `<button data-cancel='${r.id}' class='btn btn-danger small'>Cancelar</button>` : '';
        tr.innerHTML=`<td>${r.sala.nombre}</td><td>${r.fecha}</td><td>${r.horaInicio}</td><td>${r.horaFin}</td><td><span class="badge badge-${r.estado.toLowerCase()}">${r.estado}</span></td><td>${acciones}</td>`;
        tabla.appendChild(tr)
      })
    }catch(e){toast.error('Error cargando reservas')}
  }

  function validar(){limpiar();let ok=true; if(!selSala.value){errSala.textContent='Seleccione una sala';selSala.classList.add('error');ok=false} if(!inpFecha.value){errFecha.textContent='Fecha obligatoria';inpFecha.classList.add('error');ok=false} else if(inpFecha.value < hoyMin()){errFecha.textContent='Fecha no puede ser anterior a hoy';inpFecha.classList.add('error');ok=false} if(!horaInicio.value){errInicio.textContent='Hora inicio obligatoria';horaInicio.classList.add('error');ok=false} if(!horaFin.value){errFin.textContent='Hora fin obligatoria';horaFin.classList.add('error');ok=false} if(horaInicio.value && horaFin.value && horaInicio.value>=horaFin.value){errInicio.textContent='Hora inicio debe ser anterior a hora fin';horaInicio.classList.add('error');ok=false} return ok}

  formSetup();
  async function formSetup(){
    inpFecha.min=hoyMin();
    await cargarSalas();
    await cargarSalasParaDispo();
    await cargarMisReservas();
    document.getElementById('reserva-form').addEventListener('submit',async function(e){
      e.preventDefault(); if(!validar()) return; btnReservar.disabled=true;
      try{
        const body={salaId:parseInt(selSala.value),fecha:inpFecha.value,horaInicio:horaInicio.value+':00',horaFin:horaFin.value+':00'};
        const r=await api.post('/api/reservas',body);
        toast.success(r.mensaje||'Reserva creada');
        document.getElementById('reserva-form').reset();
        inpFecha.min=hoyMin();
        await cargarMisReservas();
        await cargarSalas();
      }catch(err){
        if(err.status===400){
          try{pintarErroresCampo(err, {
            salaId: 'sala',
            horaInicio: 'hora-inicio',
            horaFin: 'hora-fin'
          });}catch(e){}
          toast.error(err.message);
        } else if(err.status===409){
          errSala.textContent='Horario no disponible';
          selSala.classList.add('error');
          toast.error(err.message)
        } else {toast.error(err.message||'Error')}
      } finally{btnReservar.disabled=false}
    })
    document.getElementById('disponibilidad-form').addEventListener('submit', async function(e){
      e.preventDefault(); // validar campos
      resultadoDispo.innerHTML=''; if(!selSalaDispo.value){document.getElementById('err-sala-dispo').textContent='Seleccione una sala'; return} if(!inpFechaDispo.value){document.getElementById('err-fecha-dispo').textContent='Fecha obligatoria'; return} if(!horaInicioDispo.value){document.getElementById('err-hora-inicio-dispo').textContent='Hora inicio obligatoria'; return} if(!horaFinDispo.value){document.getElementById('err-hora-fin-dispo').textContent='Hora fin obligatoria'; return} if(horaInicioDispo.value>=horaFinDispo.value){document.getElementById('err-hora-inicio-dispo').textContent='Hora inicio debe ser anterior a hora fin'; return}
      btnConsultar.disabled=true;
      try{
        const params = `?salaId=${selSalaDispo.value}&fecha=${inpFechaDispo.value}&horaInicio=${horaInicioDispo.value}&horaFin=${horaFinDispo.value}`;
        const r = await api.get('/api/reservas/disponibilidad' + params);
        // mostrar card
        resultadoDispo.innerHTML = '';
        const div = document.createElement('div'); div.className='availability-card ' + (r.disponible? 'available':'unavailable');
        div.innerHTML = `<strong>${r.disponible? 'Disponible':'No disponible'}</strong><div>${r.mensaje || ''}</div>`;
        if(r.disponible){ const btn = document.createElement('button'); btn.className='btn btn-primary'; btn.textContent='Reservar ahora'; btn.addEventListener('click', ()=>{ selSala.value = selSalaDispo.value; inpFecha.value = inpFechaDispo.value; horaInicio.value = horaInicioDispo.value; horaFin.value = horaFinDispo.value; window.scrollTo({top:0,behavior:'smooth'}); }); div.appendChild(btn); }
        resultadoDispo.appendChild(div);
      }catch(err){ if(err.status===400){ try{pintarErroresCampo(err,{ horaInicio: 'hora-inicio-dispo', horaFin: 'hora-fin-dispo' })}catch(e){}; toast.error(err.message)} else {toast.error(err.message||'Error')} } finally{btnConsultar.disabled=false}
    });
    // delegate for cancel buttons
    tabla.addEventListener('click', async function(e){ const btn = e.target.closest('button[data-cancel]'); if(!btn) return; const id = btn.dataset.cancel; if(!confirm('¿Está seguro de cancelar esta reserva? Esta acción no se puede deshacer.')) return; try{ await api.patch('/api/reservas/' + id + '/cancelar'); toast.success('Reserva cancelada'); await cargarMisReservas(); }catch(err){ toast.error(err.message||'Error') } });
  }
})();
/* cliente.js - Placeholder Sprint 1 */
/* TODO: implementar en el Paso 5 */

(async function(){
  const me = await guard(['RECEPCIONISTA','ADMIN']); if(!me) return;
  const form = document.getElementById('buscar-form'); const correo = document.getElementById('correo-cliente') || document.getElementById('correo'); const resultados = document.getElementById('resultados');
  form.addEventListener('submit', async function(e){
    e.preventDefault(); resultados.innerHTML=''; if(!correo.value){toast.error('Ingrese un correo'); return}
    try{
      const r = await api.get('/api/recepcion/buscar-reserva?correoCliente=' + encodeURIComponent(correo.value));
      if(!r.length){resultados.innerHTML = '<div style="color:var(--text-muted)">No se encontraron reservas</div>'; return}
      r.forEach(res=>{
        const card = document.createElement('div'); card.className='card';
        card.innerHTML = `<div><strong>${res.sala.nombre}</strong> - ${res.fecha} ${res.horaInicio} - ${res.horaFin}</div><div style="margin-top:8px">Estado: <span class="badge badge-${res.estado.toLowerCase()}">${res.estado}</span></div>`;
        const btn = document.createElement('button'); btn.className='btn btn-primary'; btn.textContent='Registrar ingreso'; btn.style.marginTop='8px'; btn.addEventListener('click', async ()=>{
          try{ await api.patch('/api/recepcion/reservas/' + res.id + '/ingreso'); toast.success('Ingreso registrado'); btn.disabled=true; card.querySelector('.badge').textContent='EN_USO'; }catch(err){ toast.error(err.message||'Error') }
        });
        card.appendChild(btn); resultados.appendChild(card);
      })
    }catch(err){ toast.error(err.message||'Error buscando reservas') }
  })
})();

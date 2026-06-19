(async function () {
  const me = await guard(['RECEPCIONISTA', 'ADMIN']);
  if (!me) return;

  /* ── Navegación ── */
  function mostrarSeccion(sec) {
    document.getElementById('section-buscar').style.display   = sec === 'buscar'  ? '' : 'none';
    document.getElementById('section-en-uso').style.display   = sec === 'en-uso'  ? '' : 'none';
    document.getElementById('nav-buscar').classList.toggle('active', sec === 'buscar');
    document.getElementById('nav-en-uso').classList.toggle('active', sec === 'en-uso');
    const titulos = { 'buscar': 'Buscar Reserva', 'en-uso': 'Salas en Uso' };
    document.getElementById('main-title').textContent = titulos[sec] || 'Recepción';
  }

  document.getElementById('nav-buscar').addEventListener('click', e => { e.preventDefault(); mostrarSeccion('buscar'); });
  document.getElementById('nav-en-uso').addEventListener('click', e => { e.preventDefault(); mostrarSeccion('en-uso'); cargarSalasEnUso(); });

  /* ══════════════════════════════
     MODAL SALIDA
  ══════════════════════════════ */
  const modalSalida = document.getElementById('modal-salida');
  let reservaIdSalida = null;

  document.getElementById('modal-salida-close').addEventListener('click', () => modalSalida.close());
  document.getElementById('btn-cancelar-salida').addEventListener('click', () => modalSalida.close());

  function abrirModalSalida(reservaId, nombreSala, horaInicio, horaFin) {
    reservaIdSalida = reservaId;
    document.getElementById('chk-limpieza').checked = false;
    document.getElementById('modal-salida-info').textContent =
      `Sala: ${nombreSala} | Horario: ${String(horaInicio).substring(0, 5)} – ${String(horaFin).substring(0, 5)}`;
    modalSalida.showModal();
  }

  document.getElementById('btn-confirmar-salida').addEventListener('click', async () => {
    if (!reservaIdSalida) return;
    const limpieza = document.getElementById('chk-limpieza').checked;
    const btn = document.getElementById('btn-confirmar-salida');
    btn.disabled = true;
    try {
      await api.patch(`/api/recepcion/reservas/${reservaIdSalida}/salida?limpieza=${limpieza}`);
      modalSalida.close();
      const msg = limpieza
        ? 'Salida registrada. Sala en proceso de limpieza.'
        : 'Salida registrada. Sala disponible para nuevas reservas.';
      toast.success(msg);
      cargarSalasEnUso();
    } catch (err) {
      toast.error(err.message || 'Error al registrar salida');
    } finally {
      btn.disabled = false;
      reservaIdSalida = null;
    }
  });

  /* ══════════════════════════════
     SECCIÓN BUSCAR POR CORREO
  ══════════════════════════════ */
  const form       = document.getElementById('buscar-form');
  const correoInp  = document.getElementById('correo-cliente');
  const resultados = document.getElementById('resultados');

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    resultados.innerHTML = '';
    if (!correoInp.value) { toast.error('Ingrese un correo'); return; }
    try {
      const reservas = await api.get('/api/recepcion/buscar-reserva?correoCliente=' + encodeURIComponent(correoInp.value));
      if (!reservas.length) {
        resultados.innerHTML = '<div style="color:var(--text-muted);padding:12px 0">No se encontraron reservas activas para ese correo</div>';
        return;
      }
      reservas.forEach(res => renderTarjetaReserva(res, resultados));
    } catch (err) {
      toast.error(err.message || 'Error buscando reservas');
    }
  });

  function renderTarjetaReserva(res, contenedor, onActualizacion) {
    const card = document.createElement('div');
    card.className = 'reserva-card';
    card.dataset.reservaId = res.id;

    const estadoBadge = `<span class="badge badge-${res.estado.toLowerCase()}">${res.estado}</span>`;
    const cliente = res.nombreCliente ? `<div style="font-size:12px;color:var(--text-muted)">${res.nombreCliente} — ${res.correoCliente || ''}</div>` : '';

    card.innerHTML = `
      <div class="reserva-card-header">
        <span class="reserva-card-sala">${res.sala.nombre}</span>
        ${estadoBadge}
      </div>
      ${cliente}
      <div class="reserva-card-info">${res.fecha} · ${String(res.horaInicio).substring(0,5)} – ${String(res.horaFin).substring(0,5)}</div>
      <div class="reserva-card-actions"></div>`;

    const actions = card.querySelector('.reserva-card-actions');

    if (res.estado === 'CONFIRMADA' || res.estado === 'PENDIENTE') {
      const btnIngreso = document.createElement('button');
      btnIngreso.className = 'btn btn-primary btn-sm';
      btnIngreso.textContent = 'Registrar Ingreso';
      btnIngreso.addEventListener('click', async () => {
        btnIngreso.disabled = true;
        try {
          await api.patch(`/api/recepcion/reservas/${res.id}/ingreso`);
          toast.success('Ingreso registrado correctamente');
          card.querySelector('.badge').className = 'badge badge-en_uso';
          card.querySelector('.badge').textContent = 'EN_USO';
          btnIngreso.remove();
          agregarBotonSalida(actions, res);
        } catch (err) {
          toast.error(err.message || 'Error al registrar ingreso');
          btnIngreso.disabled = false;
        }
      });
      actions.appendChild(btnIngreso);
    }

    if (res.estado === 'EN_USO') {
      agregarBotonSalida(actions, res);
    }

    contenedor.appendChild(card);
  }

  function agregarBotonSalida(actions, res) {
    const btnSalida = document.createElement('button');
    btnSalida.className = 'btn btn-secondary btn-sm';
    btnSalida.textContent = 'Registrar Salida';
    btnSalida.addEventListener('click', () => {
      abrirModalSalida(res.id, res.sala.nombre, res.horaInicio, res.horaFin);
    });
    actions.appendChild(btnSalida);
  }

  /* ══════════════════════════════
     SECCIÓN SALAS EN USO
  ══════════════════════════════ */
  async function cargarSalasEnUso() {
    const grid = document.getElementById('en-uso-grid');
    grid.innerHTML = '<div style="color:var(--text-muted);font-size:13px">Cargando...</div>';
    try {
      const reservas = await api.get('/api/recepcion/salas-en-uso') || [];
      grid.innerHTML = '';
      if (!reservas.length) {
        grid.innerHTML = '<div style="color:var(--text-muted)">No hay salas en uso en este momento</div>';
        return;
      }
      reservas.forEach(res => renderTarjetaReserva(res, grid, cargarSalasEnUso));
    } catch (err) {
      grid.innerHTML = '<div style="color:var(--text-muted)">Error cargando salas en uso</div>';
      toast.error(err.message || 'Error cargando salas en uso');
    }
  }

  document.getElementById('btn-refresh-uso').addEventListener('click', cargarSalasEnUso);
})();

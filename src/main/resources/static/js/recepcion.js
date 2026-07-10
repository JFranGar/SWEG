(async function () {
  const me = await guard(['RECEPCIONISTA', 'ADMIN']);
  if (!me) return;

  /* ── Fecha de hoy visible ── */
  const fechaEl = document.getElementById('fecha-hoy');
  if (fechaEl) {
    const ahora = new Date();
    fechaEl.textContent = ahora.toLocaleDateString('es-EC', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
  }

  /* ── Estado global ── */
  let todasSalas = [];
  let salaSeleccionadaId = null;
  let filtroActivo = 'TODAS';

  const ESTADO_LABEL = {
    DISPONIBLE: 'Disponible', RESERVADA: 'Con Reserva',
    EN_USO: 'En Uso', EN_LIMPIEZA: 'En Limpieza', MANTENIMIENTO: 'Mantenimiento'
  };
  const ESTADO_COLOR = {
    DISPONIBLE: '#00C851', RESERVADA: '#4488FF',
    EN_USO: '#FFA500', EN_LIMPIEZA: '#5599ff', MANTENIMIENTO: '#888'
  };

  /* ══════════════════════════════
     NAVEGACIÓN
  ══════════════════════════════ */
  const SECCIONES = ['dia', 'correo', 'accesos'];
  function mostrarSeccion(sec) {
    SECCIONES.forEach(s => {
      document.getElementById('section-' + s).style.display = s === sec ? '' : 'none';
      document.getElementById('nav-' + s).classList.toggle('active', s === sec);
    });
    const titulos = { dia: 'Vista del Día', correo: 'Buscar por Correo', accesos: 'Check-in / Check-out' };
    document.getElementById('main-title').textContent = titulos[sec] || 'Recepción';
  }

  document.getElementById('nav-dia').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('dia'); cargarPanelSalas();
  });
  document.getElementById('nav-correo').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('correo');
  });
  document.getElementById('nav-accesos').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('accesos'); cargarAccesos();
  });

  /* ══════════════════════════════
     PANEL SALAS — VISTA DEL DÍA
  ══════════════════════════════ */
  async function cargarPanelSalas() {
    try {
      todasSalas = await api.get('/api/recepcion/panel-salas') || [];
      renderListaSalas();
      if (salaSeleccionadaId) cargarDetalleSala(salaSeleccionadaId);
    } catch (e) {
      toast.error('Error cargando salas');
    }
  }

  function renderListaSalas() {
    const list = document.getElementById('sala-list');
    list.innerHTML = '';

    const filtradas = filtroActivo === 'TODAS'
      ? todasSalas
      : todasSalas.filter(s => s.estadoPanel === filtroActivo);

    document.getElementById('salas-count').textContent = `${filtradas.length} sala${filtradas.length !== 1 ? 's' : ''}`;

    if (!filtradas.length) {
      list.innerHTML = '<div class="sala-empty">Sin salas con este estado</div>';
      return;
    }

    filtradas.forEach(sala => {
      const item = document.createElement('div');
      item.className = 'sala-item' + (sala.id === salaSeleccionadaId ? ' selected' : '');
      item.dataset.salaId = sala.id;
      const color = ESTADO_COLOR[sala.estadoPanel] || '#888';
      item.innerHTML = `
        <div class="sala-item-header">
          <span class="sala-item-nombre">${esc(sala.nombre)}</span>
          <span class="badge badge-${sala.estadoPanel.toLowerCase()}" style="font-size:0.75rem;white-space:nowrap">
            ${ESTADO_LABEL[sala.estadoPanel] || sala.estadoPanel}
          </span>
        </div>
        <div class="sala-item-meta">${esc(sala.tipo)} · Cap. ${sala.capacidadMaxima}</div>`;
      item.style.borderLeft = `3px solid ${color}33`;
      item.addEventListener('click', () => seleccionarSala(sala));
      list.appendChild(item);
    });
  }

  function seleccionarSala(sala) {
    salaSeleccionadaId = sala.id;
    document.querySelectorAll('.sala-item').forEach(el =>
      el.classList.toggle('selected', Number(el.dataset.salaId) === sala.id));
    cargarDetalleSala(sala.id);
  }

  /* ── Detalle de sala ── */
  async function cargarDetalleSala(salaId) {
    const placeholder  = document.getElementById('detail-placeholder');
    const detailWrap   = document.getElementById('detail-content');
    const reservasList = document.getElementById('reservas-list');

    placeholder.style.display = 'none';
    detailWrap.style.display  = 'flex';
    reservasList.innerHTML    = '<div style="color:var(--text-muted);font-size:13px;padding:20px 0">Cargando reservas...</div>';

    const sala = todasSalas.find(s => s.id === salaId);
    if (!sala) return;

    document.getElementById('detail-nombre').textContent = sala.nombre;
    document.getElementById('detail-meta').textContent   = `${sala.tipo} · Capacidad: ${sala.capacidadMaxima} personas`;
    document.getElementById('detail-estado-badge').innerHTML =
      `<span class="badge badge-${sala.estadoPanel.toLowerCase()}">${ESTADO_LABEL[sala.estadoPanel] || sala.estadoPanel}</span>`;

    const btnDisponible = document.getElementById('btn-limpieza-disponible');
    btnDisponible.style.display = sala.estadoPanel === 'EN_LIMPIEZA' ? '' : 'none';
    btnDisponible.onclick = () => marcarDisponible(salaId);

    document.getElementById('btn-refresh-detail').onclick = () => {
      cargarPanelSalas();
    };

    try {
      const reservas = await api.get(`/api/recepcion/salas/${salaId}/reservas-hoy`);
      reservasList.innerHTML = '';

      if (!reservas || reservas.length === 0) {
        reservasList.innerHTML = '<div class="no-reservas">Sin reservas para hoy en esta sala</div>';
        return;
      }

      reservas.forEach(res => reservasList.appendChild(crearTarjetaReserva(res)));
    } catch (e) {
      reservasList.innerHTML = '<div class="no-reservas">Error cargando reservas</div>';
    }
  }

  function crearTarjetaReserva(res) {
    const card = document.createElement('div');
    card.className = 'reserva-item';
    card.dataset.reservaId = res.id;

    const hi = String(res.horaInicio).substring(0, 5);
    const hf = String(res.horaFin).substring(0, 5);
    const cliente = res.nombreCliente || '—';
    const correo  = res.correoCliente || '';

    card.innerHTML = `
      <div class="reserva-item-top">
        <span class="reserva-item-tiempo">${hi} – ${hf}</span>
        <span class="badge badge-${res.estado.toLowerCase()}">${estadoLabel(res.estado)}</span>
      </div>
      <div class="reserva-item-cliente">
        <strong>${esc(cliente)}</strong>${correo ? `<br><span style="font-size:11px">${esc(correo)}</span>` : ''}
      </div>
      <div class="reserva-item-btns" id="btns-${res.id}"></div>`;

    renderBotonesReserva(card.querySelector(`#btns-${res.id}`), res);
    return card;
  }

  /* Construye un Date a partir de la fecha + hora (HH:mm:ss) de la reserva. */
  function fechaHoraReserva(fecha, hora) {
    if (!fecha || !hora) return null;
    const d = new Date(`${fecha}T${String(hora).substring(0, 8)}`);
    return isNaN(d) ? null : d;
  }

  /* Botón deshabilitado por horario: no ejecuta la acción y explica cuándo se habilita. */
  function botonBloqueado(label, mensaje) {
    const btn = document.createElement('button');
    btn.className = 'btn btn-sm';
    btn.textContent = label;
    btn.title = mensaje;
    btn.style.opacity = '0.5';
    btn.style.cursor = 'not-allowed';
    btn.style.borderColor = 'var(--border-soft)';
    btn.addEventListener('click', () => toast.info(mensaje));
    return btn;
  }

  function hint(texto) {
    return `<span style="font-size:11px;color:var(--text-muted)">${texto}</span>`;
  }

  function renderBotonesReserva(container, res) {
    container.innerHTML = '';
    const ahora  = new Date();
    const inicio = fechaHoraReserva(res.fecha, res.horaInicio);
    const fin    = fechaHoraReserva(res.fecha, res.horaFin);
    const hhI = String(res.horaInicio).substring(0, 5);
    const hhF = String(res.horaFin).substring(0, 5);

    if (res.estado === 'CONFIRMADA' || res.estado === 'PENDIENTE') {
      // Check-in solo dentro del rango [horaInicio, horaFin].
      if (inicio && ahora < inicio) {
        container.appendChild(botonBloqueado(
          'Registrar Ingreso',
          `Aún no puede registrar ingreso. La reunión inicia a las ${hhI}.`));
      } else if (fin && ahora > fin) {
        container.innerHTML = hint('Fuera de horario de ingreso');
      } else {
        const btn = document.createElement('button');
        btn.className = 'btn btn-primary btn-sm';
        btn.textContent = 'Registrar Ingreso';
        btn.addEventListener('click', () => accionIngreso(res.id, btn));
        container.appendChild(btn);
      }
    } else if (res.estado === 'EN_USO') {
      // Check-out solo cuando terminó el rango horario elegido (ahora >= horaFin).
      if (fin && ahora < fin) {
        container.appendChild(botonBloqueado(
          'Registrar Salida',
          `Aún no puede registrar salida. La reunión termina a las ${hhF}.`));
      } else {
        const btn = document.createElement('button');
        btn.className = 'btn btn-secondary btn-sm';
        btn.textContent = 'Registrar Salida';
        btn.style.borderColor = 'rgba(255,165,0,0.4)';
        btn.style.color = '#FFA500';
        btn.addEventListener('click', () => abrirModalSalida(res));
        container.appendChild(btn);
      }
    } else if (res.estado === 'FINALIZADA') {
      container.innerHTML = hint('✓ Finalizada');
    } else if (res.estado === 'CANCELADA') {
      container.innerHTML = hint('Cancelada');
    }
  }

  async function accionIngreso(reservaId, btn) {
    btn.disabled = true;
    btn.textContent = 'Registrando...';
    try {
      await api.patch(`/api/recepcion/reservas/${reservaId}/ingreso`);
      toast.success('Ingreso registrado correctamente');
      await cargarPanelSalas();
    } catch (err) {
      toast.error(err.message || 'Error al registrar ingreso');
      btn.disabled = false;
      btn.textContent = 'Registrar Ingreso';
    }
  }

  async function marcarDisponible(salaId) {
    try {
      await api.patch(`/api/recepcion/salas/${salaId}/disponible`);
      toast.success('Sala marcada como disponible');
      await cargarPanelSalas();
    } catch (err) {
      toast.error(err.message || 'Error al cambiar estado');
    }
  }

  /* ── Filtros ── */
  document.getElementById('filter-chips').addEventListener('click', e => {
    const chip = e.target.closest('.chip');
    if (!chip) return;
    document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
    chip.classList.add('active');
    filtroActivo = chip.dataset.filtro;
    salaSeleccionadaId = null;
    document.getElementById('detail-placeholder').style.display = '';
    document.getElementById('detail-content').style.display     = 'none';
    renderListaSalas();
  });

  document.getElementById('btn-refresh').addEventListener('click', cargarPanelSalas);

  /* ══════════════════════════════
     MODAL SALIDA
  ══════════════════════════════ */
  const modalSalida = document.getElementById('modal-salida');
  let reservaEnSalida = null;

  document.getElementById('modal-salida-close').addEventListener('click',  () => modalSalida.close());
  document.getElementById('btn-cancelar-salida').addEventListener('click', () => modalSalida.close());

  document.getElementById('limpieza-toggle').addEventListener('click', e => {
    if (e.target.tagName !== 'INPUT') {
      const chk = document.getElementById('chk-limpieza');
      chk.checked = !chk.checked;
    }
  });

  function abrirModalSalida(res) {
    reservaEnSalida = res;
    document.getElementById('chk-limpieza').checked = false;
    document.getElementById('salida-sala-nombre').textContent  = res.sala?.nombre || '—';
    document.getElementById('salida-cliente-nombre').textContent = res.nombreCliente || res.correoCliente || '—';
    document.getElementById('salida-horario').textContent =
      `${String(res.horaInicio).substring(0,5)} – ${String(res.horaFin).substring(0,5)}`;
    modalSalida.showModal();
  }

  document.getElementById('btn-confirmar-salida').addEventListener('click', async () => {
    if (!reservaEnSalida) return;
    const btn = document.getElementById('btn-confirmar-salida');
    btn.disabled = true;
    btn.textContent = 'Procesando...';
    const limpieza = document.getElementById('chk-limpieza').checked;
    try {
      await api.patch(`/api/recepcion/reservas/${reservaEnSalida.id}/salida?limpieza=${limpieza}`);
      modalSalida.close();
      toast.success(limpieza
        ? 'Salida registrada — sala en proceso de limpieza'
        : 'Salida registrada — sala disponible');
      reservaEnSalida = null;
      await cargarPanelSalas();
    } catch (err) {
      toast.error(err.message || 'Error al registrar salida');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Confirmar salida';
    }
  });

  /* ══════════════════════════════
     BUSCAR POR CORREO
  ══════════════════════════════ */
  document.getElementById('buscar-form').addEventListener('submit', async function (e) {
    e.preventDefault();
    const correoInp = document.getElementById('correo-cliente');
    const errEl     = document.getElementById('err-correo');
    const resultados = document.getElementById('resultados-correo');
    errEl.textContent = '';
    resultados.innerHTML = '';

    if (!correoInp.value.trim()) {
      errEl.textContent = 'Ingrese un correo electrónico';
      correoInp.classList.add('error');
      return;
    }
    correoInp.classList.remove('error');
    resultados.innerHTML = '<div style="color:var(--text-muted);font-size:13px">Buscando...</div>';

    try {
      const reservas = await api.get('/api/recepcion/buscar-reserva?correoCliente=' + encodeURIComponent(correoInp.value.trim()));
      resultados.innerHTML = '';
      if (!reservas || reservas.length === 0) {
        resultados.innerHTML = `
          <div class="card" style="text-align:center;padding:28px">
            <div style="font-size:32px;margin-bottom:10px">📭</div>
            <div style="color:var(--text-muted)">No se encontraron reservas activas para ese correo</div>
          </div>`;
        return;
      }
      reservas.forEach(res => resultados.appendChild(crearTarjetaReserva(res)));
    } catch (err) {
      resultados.innerHTML = '';
      toast.error(err.message || 'Error buscando reservas');
    }
  });

  /* ══════════════════════════════
     HISTORIAL DE ACCESOS (check-in / check-out)
  ══════════════════════════════ */
  let listaAccesos = [], filtroAccesos = 'TODOS';

  document.getElementById('accesos-chips').addEventListener('click', e => {
    const chip = e.target.closest('.chip');
    if (!chip) return;
    document.querySelectorAll('#accesos-chips .chip').forEach(c => c.classList.remove('active'));
    chip.classList.add('active');
    filtroAccesos = chip.dataset.filtro;
    renderAccesos();
  });
  document.getElementById('btn-refresh-accesos').addEventListener('click', cargarAccesos);

  async function cargarAccesos() {
    const tbody = document.getElementById('tabla-accesos');
    tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted)">Cargando...</td></tr>';
    try {
      listaAccesos = await api.get('/api/recepcion/accesos') || [];
      renderAccesosResumen();
      renderAccesos();
    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted)">Error cargando historial</td></tr>';
    }
  }

  /* Tarjetas "Check-ins/Check-outs hoy" + gráfica de 7 días, calculadas desde los accesos. */
  function mismoDia(iso, ref) {
    const d = new Date(iso);
    return !isNaN(d) && d.getFullYear() === ref.getFullYear()
      && d.getMonth() === ref.getMonth() && d.getDate() === ref.getDate();
  }

  function renderAccesosResumen() {
    const hoy = new Date();
    let ciHoy = 0, coHoy = 0;
    listaAccesos.forEach(a => {
      if (mismoDia(a.fecha, hoy)) {
        if (a.tipo === 'CHECK_IN') ciHoy++; else if (a.tipo === 'CHECK_OUT') coHoy++;
      }
    });
    document.getElementById('stat-checkins-hoy').textContent = ciHoy;
    document.getElementById('stat-checkouts-hoy').textContent = coHoy;

    // Serie de los últimos 7 días
    const dias = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date(); d.setDate(d.getDate() - i);
      let cin = 0, cout = 0;
      listaAccesos.forEach(a => {
        if (mismoDia(a.fecha, d)) { if (a.tipo === 'CHECK_IN') cin++; else if (a.tipo === 'CHECK_OUT') cout++; }
      });
      dias.push({
        label: String(d.getDate()).padStart(2, '0') + '/' + String(d.getMonth() + 1).padStart(2, '0'),
        cin, cout
      });
    }
    const max = Math.max(1, ...dias.map(d => Math.max(d.cin, d.cout)));
    const cont = document.getElementById('accesos-chart');
    cont.innerHTML = '';
    dias.forEach(d => {
      const col = document.createElement('div');
      col.style.cssText = 'flex:1;display:flex;flex-direction:column;align-items:center;gap:6px';
      col.innerHTML = `
        <div style="flex:1;display:flex;align-items:flex-end;gap:4px;width:100%;justify-content:center">
          <div title="Check-in: ${d.cin}" style="width:14px;background:#00C851;border-radius:3px 3px 0 0;height:${d.cin / max * 100}%;min-height:${d.cin > 0 ? 4 : 0}px"></div>
          <div title="Check-out: ${d.cout}" style="width:14px;background:#FFA500;border-radius:3px 3px 0 0;height:${d.cout / max * 100}%;min-height:${d.cout > 0 ? 4 : 0}px"></div>
        </div>
        <div style="font-size:0.75rem;color:var(--text-muted);white-space:nowrap">${d.label}</div>`;
      cont.appendChild(col);
    });
  }

  function renderAccesos() {
    const tbody = document.getElementById('tabla-accesos');
    const filtrados = filtroAccesos === 'TODOS' ? listaAccesos : listaAccesos.filter(a => a.tipo === filtroAccesos);
    tbody.innerHTML = '';
    if (!filtrados.length) {
      tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted)">Sin accesos registrados</td></tr>';
      return;
    }
    filtrados.forEach(a => {
      const esIn = a.tipo === 'CHECK_IN';
      const hi = String(a.horaInicio || '').substring(0, 5);
      const hf = String(a.horaFin || '').substring(0, 5);
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><span class="badge badge-${esIn ? 'disponible' : 'en_uso'}">${esIn ? '→ Check-in' : '← Check-out'}</span></td>
        <td style="font-size:12px">${fechaHora(a.fecha)}</td>
        <td>${esc(a.salaNombre)}</td>
        <td style="font-size:12px">${esc(a.clienteNombre || a.clienteCorreo || '—')}</td>
        <td style="font-size:12px;color:var(--text-muted)">${hi} – ${hf}</td>
        <td style="font-size:12px;color:var(--text-muted)">${esc(a.operador || '—')}</td>`;
      tbody.appendChild(tr);
    });
  }

  /* ── Helpers ── */
  function esc(s) {
    const d = document.createElement('div');
    d.textContent = String(s ?? '');
    return d.innerHTML;
  }

  function fechaHora(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d)) return String(iso).replace('T', ' ').substring(0, 16);
    return d.toLocaleString('es-EC', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  function estadoLabel(estado) {
    const map = {
      CONFIRMADA: 'Confirmada', PENDIENTE: 'Pendiente',
      EN_USO: 'En Uso', FINALIZADA: 'Finalizada', CANCELADA: 'Cancelada'
    };
    return map[estado] || estado;
  }

  /* ── Init ── */
  await cargarPanelSalas();
})();

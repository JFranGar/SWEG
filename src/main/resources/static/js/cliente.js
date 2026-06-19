/* cliente.js */
(async function () {
  const me = await guard(['CLIENTE']);
  if (!me) return;

  /* ── Navegación ── */
  function mostrarSeccion(sec) {
    document.getElementById('section-reservar').style.display = sec === 'reservar' ? '' : 'none';
    document.getElementById('section-mis').style.display      = sec === 'mis'      ? '' : 'none';
    document.getElementById('nav-reservar').classList.toggle('active', sec === 'reservar');
    document.getElementById('nav-mis').classList.toggle('active',      sec === 'mis');
    if (sec === 'mis') cargarMisReservas(0);
  }
  document.getElementById('nav-reservar').addEventListener('click', e => { e.preventDefault(); mostrarSeccion('reservar'); });
  document.getElementById('nav-mis').addEventListener('click',      e => { e.preventDefault(); mostrarSeccion('mis'); });

  /* ── Refs formulario principal ── */
  const selSala       = document.getElementById('sala');
  const inpCantidad   = document.getElementById('cantidad-personas');
  const inpFecha      = document.getElementById('fecha');
  const horaInicio    = document.getElementById('hora-inicio');
  const horaFin       = document.getElementById('hora-fin');
  const btnReservar   = document.getElementById('btn-reservar');
  const errSala       = document.getElementById('err-sala');
  const errCantidad   = document.getElementById('err-cantidad');
  const errFecha      = document.getElementById('err-fecha');
  const errInicio     = document.getElementById('err-hora-inicio');
  const errFin        = document.getElementById('err-hora-fin');
  const capacityHint  = document.getElementById('capacity-hint');

  /* ── Refs Mis Reservas ── */
  const tablaBody  = document.getElementById('tabla-reservas');
  const btnPrev    = document.getElementById('btn-prev');
  const btnNext    = document.getElementById('btn-next');
  const pageInfo   = document.getElementById('page-info');
  let paginaActual = 0;
  const PAGE_SIZE  = 10;
  let reservasMap  = {};   // id → reserva completa (para editar)

  /* ── Refs Timeline principal ── */
  const tlWrap        = document.getElementById('tl-wrap');
  const tlPlaceholder = document.getElementById('tl-placeholder');
  const tlRowsEl      = document.getElementById('tl-rows');

  /* ── Refs Modal Editar ── */
  const modalEditar     = document.getElementById('modal-editar');
  const formEditar      = document.getElementById('editar-form');
  const editarId        = document.getElementById('editar-id');
  const editarSala      = document.getElementById('editar-sala');
  const editarCantidad  = document.getElementById('editar-cantidad');
  const editarFecha     = document.getElementById('editar-fecha');
  const editarInicio    = document.getElementById('editar-inicio');
  const editarFin       = document.getElementById('editar-fin');
  const errEditSala     = document.getElementById('err-editar-sala');
  const errEditCantidad = document.getElementById('err-editar-cantidad');
  const errEditFecha    = document.getElementById('err-editar-fecha');
  const errEditInicio   = document.getElementById('err-editar-inicio');
  const errEditFin      = document.getElementById('err-editar-fin');
  const editCapHint     = document.getElementById('edit-capacity-hint');

  /* ── Refs Modal Éxito ── */
  const modalExito = document.getElementById('modal-exito');
  let exitoCb = null;
  document.getElementById('modal-exito-ok').addEventListener('click', () => {
    modalExito.close();
    if (exitoCb) { exitoCb(); exitoCb = null; }
  });
  function mostrarExito(msg, cb) {
    document.getElementById('modal-exito-msg').textContent = msg;
    exitoCb = cb || null;
    modalExito.showModal();
  }

  document.getElementById('modal-editar-close').addEventListener('click', () => modalEditar.close());
  document.getElementById('btn-editar-cancelar').addEventListener('click', () => modalEditar.close());

  /* ── Caché de reglas ── */
  let reglasCache = [];

  /* ══════════════════════════════
     TIMELINE VERTICAL
  ══════════════════════════════ */
  let TL_H_INI = 7;
  let TL_H_FIN = 23;
  let tlSelStart = null;
  let tlSelEnd   = null;

  function parseMins(t) {
    if (Array.isArray(t)) return (t[0] || 0) * 60 + (t[1] || 0);
    const p = String(t).split(':').map(Number);
    return (p[0] || 0) * 60 + (p[1] || 0);
  }

  function actualizarRangoTimeline(reglas) {
    const aps = reglas.filter(r => r.tipo === 'APERTURA');
    if (!aps.length) { TL_H_INI = 7; TL_H_FIN = 23; return; }
    TL_H_INI = Math.min(...aps.map(r => Math.floor(parseMins(r.horaInicio) / 60)));
    TL_H_FIN = Math.max(...aps.map(r => Math.ceil (parseMins(r.horaFin)    / 60)));
    if (TL_H_FIN <= TL_H_INI) { TL_H_INI = 7; TL_H_FIN = 23; }
  }

  function slotToTime(idx) {
    const m = TL_H_INI * 60 + idx * 30;
    return String(Math.floor(m / 60)).padStart(2, '0') + ':' + String(m % 60).padStart(2, '0');
  }

  function clasificarSlot(idx, reservas, reglas, diaSemana, nowMins, isToday) {
    const sMin = TL_H_INI * 60 + idx * 30;
    const eMin = sMin + 30;
    if (isToday && eMin <= nowMins) return 'pasado';
    const aps = reglas.filter(r => r.tipo === 'APERTURA' && (r.diaSemana == null || r.diaSemana === diaSemana));
    if (aps.length) {
      const dentro = aps.some(r => sMin >= parseMins(r.horaInicio) && eMin <= parseMins(r.horaFin));
      if (!dentro) return 'cerrado';
    }
    if (reglas.some(r => {
      if (r.tipo !== 'BLOQUEO') return false;
      if (r.diaSemana != null && r.diaSemana !== diaSemana) return false;
      return sMin < parseMins(r.horaFin) && parseMins(r.horaInicio) < eMin;
    })) return 'bloqueado';
    if (reservas.some(r => sMin < parseMins(r.horaFin) && parseMins(r.horaInicio) < eMin)) return 'ocupado';
    return 'libre';
  }

  const ESTADO_LABEL = {
    libre: 'Libre', ocupado: 'Ocupado', bloqueado: 'Bloqueado', cerrado: 'Cerrado', pasado: 'Hora pasada'
  };

  function renderTimelineEn(containerEl, reservas, reglas, fecha, onClickSlot) {
    containerEl.innerHTML = '';
    const diaSemana = fecha ? (new Date(fecha + 'T00:00:00').getDay() || 7) : null;
    const hoy       = new Date().toISOString().split('T')[0];
    const isToday   = fecha === hoy;
    const now       = new Date();
    const nowMins   = now.getHours() * 60 + now.getMinutes();
    const TL_SLOTS  = (TL_H_FIN - TL_H_INI) * 2;

    for (let i = 0; i <= TL_SLOTS; i++) {
      const isEnd = i === TL_SLOTS;
      const row   = document.createElement('div');
      row.className = isEnd ? 'tl-row tl-row-end' : 'tl-row';

      const timeEl = document.createElement('div');
      timeEl.className   = 'tl-row-time';
      timeEl.textContent = slotToTime(i);
      row.appendChild(timeEl);

      if (!isEnd) {
        const estado = clasificarSlot(i, reservas, reglas, diaSemana, nowMins, isToday);
        const bar    = document.createElement('div');
        bar.className   = 'tl-row-bar ' + estado;
        bar.textContent = ESTADO_LABEL[estado] || estado;
        if (estado === 'libre' && onClickSlot) {
          bar.dataset.idx = i;
          bar.addEventListener('click', () => onClickSlot(i));
        }
        row.appendChild(bar);
      }
      containerEl.appendChild(row);
    }
  }

  function actualizarResaltado() {
    tlRowsEl.querySelectorAll('.tl-row-bar').forEach((bar, i) => {
      bar.classList.remove('sel-inicio', 'sel-fin', 'sel-rango');
      if (tlSelStart === null) return;
      const lo = tlSelEnd !== null ? Math.min(tlSelStart, tlSelEnd) : tlSelStart;
      const hi = tlSelEnd !== null ? Math.max(tlSelStart, tlSelEnd) : tlSelStart;
      if (i === lo) bar.classList.add('sel-inicio');
      else if (i === hi && tlSelEnd !== null) bar.classList.add('sel-fin');
      else if (i > lo && i < hi) bar.classList.add('sel-rango');
    });
  }

  function renderTimeline(reservas, reglas, fecha) {
    tlSelStart = null; tlSelEnd = null;
    renderTimelineEn(tlRowsEl, reservas, reglas, fecha, idx => {
      if (tlSelStart === null || tlSelEnd !== null) {
        tlSelStart = idx; tlSelEnd = null;
      } else {
        tlSelEnd = idx;
        const lo = Math.min(tlSelStart, tlSelEnd);
        const hi = Math.max(tlSelStart, tlSelEnd);
        tlSelStart = lo; tlSelEnd = hi;
        horaInicio.value = slotToTime(lo);
        horaFin.value    = slotToTime(hi + 1);
      }
      actualizarResaltado();
    });
    requestAnimationFrame(() => {
      const primer = tlRowsEl.querySelector('.tl-row-bar.libre, .tl-row-bar.ocupado, .tl-row-bar.bloqueado');
      if (primer) primer.scrollIntoView({ block: 'start', behavior: 'smooth' });
    });
  }

  async function cargarTimeline() {
    const salaId = selSala.value;
    const fecha  = inpFecha.value;
    tlWrap.style.display        = 'none';
    tlPlaceholder.style.display = 'block';
    tlPlaceholder.textContent   = 'Seleccione una sala y fecha para ver la disponibilidad.';
    if (!salaId || !fecha) return;
    try {
      const [reservas, reglas] = await Promise.all([
        api.get(`/api/reservas/horario-dia?salaId=${salaId}&fecha=${fecha}`),
        api.get('/api/reservas/reglas-activas').catch(() => [])
      ]);
      reglasCache = reglas || [];
      actualizarRangoTimeline(reglasCache);
      renderTimeline(reservas || [], reglasCache, fecha);
      tlPlaceholder.style.display = 'none';
      tlWrap.style.display        = 'block';
    } catch {
      tlPlaceholder.textContent = 'No se pudo cargar la disponibilidad.';
    }
  }

  selSala.addEventListener('change', () => { actualizarHintCapacidad(); cargarTimeline(); });
  inpFecha.addEventListener('change', cargarTimeline);

  /* ── Mini-timeline en modal editar ── */
  async function cargarTimelineEditar() {
    const salaId = editarSala.value;
    const fecha  = editarFecha.value;
    const sec    = document.getElementById('edit-tl-section');
    const ph     = document.getElementById('edit-tl-placeholder');
    const wrap   = document.getElementById('edit-tl-wrap');
    sec.style.display  = '';
    ph.style.display   = 'block';
    wrap.style.display = 'none';
    if (!salaId || !fecha) { ph.textContent = 'Selecciona sala y fecha para ver disponibilidad'; return; }
    ph.textContent = 'Cargando…';
    try {
      const [reservas, reglas] = await Promise.all([
        api.get(`/api/reservas/horario-dia?salaId=${salaId}&fecha=${fecha}`),
        api.get('/api/reservas/reglas-activas').catch(() => [])
      ]);
      reglasCache = reglas || [];
      actualizarRangoTimeline(reglasCache);
      renderTimelineEn(document.getElementById('edit-tl-rows'), reservas || [], reglasCache, fecha, null);
      ph.style.display   = 'none';
      wrap.style.display = 'block';
    } catch {
      ph.textContent = 'No se pudo cargar la disponibilidad';
    }
  }

  editarSala.addEventListener('change', () => { actualizarHintCapacidadEditar(); cargarTimelineEditar(); });
  editarFecha.addEventListener('change', cargarTimelineEditar);

  /* ══════════════════════════════
     SALAS + CAPACIDAD
  ══════════════════════════════ */
  let listaSalasCliente = [];

  async function cargarSalas() {
    try {
      listaSalasCliente = await api.get('/api/reservas/salas-disponibles') || [];
      [selSala, editarSala].forEach(sel => {
        sel.innerHTML = '';
        sel.appendChild(new Option('Seleccione...', ''));
        listaSalasCliente.forEach(s => sel.appendChild(new Option(s.nombre, s.id)));
      });
      actualizarHintCapacidad();
      actualizarHintCapacidadEditar();
    } catch { toast.error('Error cargando salas'); }
  }

  function salaSeleccionada(salaId) {
    return listaSalasCliente.find(s => String(s.id) === String(salaId)) || null;
  }

  function actualizarHintCapacidad() {
    const sala = salaSeleccionada(selSala.value);
    if (sala) {
      inpCantidad.max = sala.capacidadMaxima;
      capacityHint.textContent = `Capacidad máxima: ${sala.capacidadMaxima} personas`;
    } else {
      inpCantidad.removeAttribute('max');
      capacityHint.textContent = '';
    }
  }

  function actualizarHintCapacidadEditar() {
    const sala = salaSeleccionada(editarSala.value);
    if (sala) {
      editarCantidad.max = sala.capacidadMaxima;
      editCapHint.textContent = `Capacidad máxima: ${sala.capacidadMaxima} personas`;
    } else {
      editarCantidad.removeAttribute('max');
      editCapHint.textContent = '';
    }
  }

  /* ══════════════════════════════
     MIS RESERVAS
  ══════════════════════════════ */
  async function cargarMisReservas(page = 0) {
    try {
      const data = await api.get(`/api/reservas?page=${page}&size=${PAGE_SIZE}`);
      tablaBody.innerHTML = '';
      reservasMap = {};
      if (!data.content || data.content.length === 0) {
        tablaBody.innerHTML = '<tr><td colspan="7" style="color:var(--text-muted)">Sin reservas</td></tr>';
      } else {
        data.content.forEach(r => {
          reservasMap[r.id] = r;
          const editable = r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA';
          const acciones = editable
            ? `<button data-editar="${r.id}" class="btn btn-secondary" style="font-size:11px;padding:4px 10px">Editar</button>
               <button data-cancel="${r.id}"  class="btn btn-danger"    style="font-size:11px;padding:4px 10px">Cancelar</button>`
            : '';
          const personas = r.cantidadPersonas != null ? r.cantidadPersonas : '—';
          const tr = document.createElement('tr');
          tr.innerHTML = `
            <td>${r.sala.nombre}</td>
            <td>${r.fecha}</td>
            <td>${String(r.horaInicio).substring(0,5)}</td>
            <td>${String(r.horaFin).substring(0,5)}</td>
            <td style="text-align:center">${personas}</td>
            <td><span class="badge badge-${r.estado.toLowerCase()}">${r.estado}</span></td>
            <td>${acciones}</td>`;
          tablaBody.appendChild(tr);
        });
      }
      paginaActual         = data.number;
      btnPrev.disabled     = data.first;
      btnNext.disabled     = data.last;
      pageInfo.textContent = `Página ${data.number + 1} de ${Math.max(data.totalPages, 1)}`;
    } catch { toast.error('Error cargando reservas'); }
  }

  btnPrev.addEventListener('click', () => cargarMisReservas(paginaActual - 1));
  btnNext.addEventListener('click', () => cargarMisReservas(paginaActual + 1));

  tablaBody.addEventListener('click', async function (e) {
    const btn = e.target.closest('button');
    if (!btn) return;
    if (btn.dataset.cancel) {
      if (!confirm('¿Cancelar esta reserva? Esta acción no se puede deshacer.')) return;
      try {
        await api.patch('/api/reservas/' + btn.dataset.cancel + '/cancelar');
        mostrarExito('Reserva cancelada correctamente', () => cargarMisReservas(paginaActual));
      } catch (err) { toast.error(err.message || 'Error al cancelar'); }
    } else if (btn.dataset.editar) {
      abrirModalEditar(btn.dataset.editar);
    }
  });

  /* ══════════════════════════════
     MODAL EDITAR
  ══════════════════════════════ */
  function abrirModalEditar(id) {
    const r = reservasMap[id];
    if (!r) return;

    editarId.value       = id;
    editarFecha.value    = r.fecha;
    editarInicio.value   = String(r.horaInicio).substring(0, 5);
    editarFin.value      = String(r.horaFin).substring(0, 5);
    editarCantidad.value = r.cantidadPersonas != null ? r.cantidadPersonas : '';
    if (r.sala) editarSala.value = r.sala.id;
    editarFecha.min = hoyStr();

    actualizarHintCapacidadEditar();

    [errEditSala, errEditCantidad, errEditFecha, errEditInicio, errEditFin].forEach(e => e.textContent = '');
    [editarSala, editarCantidad, editarFecha, editarInicio, editarFin].forEach(i => i.classList.remove('error'));

    document.getElementById('edit-tl-section').style.display = 'none';
    modalEditar.showModal();
    if (editarSala.value && editarFecha.value) cargarTimelineEditar();
  }

  function validarEditar() {
    [errEditSala, errEditCantidad, errEditFecha, errEditInicio, errEditFin].forEach(e => e.textContent = '');
    [editarSala, editarCantidad, editarFecha, editarInicio, editarFin].forEach(i => i.classList.remove('error'));
    let ok = true;
    const hoy  = hoyStr();
    const sala = salaSeleccionada(editarSala.value);

    if (!editarSala.value) { errEditSala.textContent = 'Seleccione una sala'; editarSala.classList.add('error'); ok = false; }

    const cantVal = parseInt(editarCantidad.value);
    if (!editarCantidad.value || isNaN(cantVal) || cantVal < 1) {
      errEditCantidad.textContent = 'Mínimo 1 persona'; editarCantidad.classList.add('error'); ok = false;
    } else if (sala && cantVal > sala.capacidadMaxima) {
      errEditCantidad.textContent = `Supera la capacidad máxima (${sala.capacidadMaxima})`; editarCantidad.classList.add('error'); ok = false;
    }

    if (!editarFecha.value)  { errEditFecha.textContent  = 'Fecha obligatoria'; editarFecha.classList.add('error'); ok = false; }
    else if (editarFecha.value < hoy) { errEditFecha.textContent = 'No puede ser anterior a hoy'; editarFecha.classList.add('error'); ok = false; }
    if (!editarInicio.value) { errEditInicio.textContent = 'Hora inicio obligatoria'; editarInicio.classList.add('error'); ok = false; }
    if (!editarFin.value)    { errEditFin.textContent    = 'Hora fin obligatoria';    editarFin.classList.add('error');    ok = false; }
    if (editarInicio.value && editarFin.value && editarInicio.value >= editarFin.value) {
      errEditInicio.textContent = 'Hora inicio debe ser anterior a hora fin'; editarInicio.classList.add('error'); ok = false;
    }
    if (ok && reglasCache.length > 0 && editarFecha.value && editarInicio.value && editarFin.value) {
      const iniM = parseMins(editarInicio.value + ':00');
      const finM = parseMins(editarFin.value    + ':00');
      const dia  = new Date(editarFecha.value + 'T00:00:00').getDay() || 7;
      const aps  = reglasCache.filter(r => r.tipo === 'APERTURA' && (r.diaSemana == null || r.diaSemana === dia));
      if (aps.length) {
        const dentro = aps.some(r => iniM >= parseMins(r.horaInicio) && finM <= parseMins(r.horaFin));
        if (!dentro) { errEditInicio.textContent = 'Fuera del horario comercial permitido'; editarInicio.classList.add('error'); ok = false; }
      }
    }
    return ok;
  }

  formEditar.addEventListener('submit', async function (e) {
    e.preventDefault();
    if (!validarEditar()) return;
    const btn = document.getElementById('btn-editar-guardar');
    btn.disabled = true;
    try {
      await api.put('/api/reservas/' + editarId.value, {
        salaId:           parseInt(editarSala.value),
        cantidadPersonas: parseInt(editarCantidad.value),
        fecha:            editarFecha.value,
        horaInicio:       editarInicio.value + ':00',
        horaFin:          editarFin.value    + ':00'
      });
      modalEditar.close();
      mostrarExito('Reserva actualizada correctamente', () => cargarMisReservas(paginaActual));
    } catch (err) {
      if (err.status === 409) {
        const msg = err.message || 'Conflicto de horario o capacidad';
        if (msg.toLowerCase().includes('personas') || msg.toLowerCase().includes('capacidad')) {
          errEditCantidad.textContent = msg; editarCantidad.classList.add('error');
        } else {
          errEditInicio.textContent = msg; editarInicio.classList.add('error');
        }
        toast.error(msg);
      } else if (err.status === 400) {
        try { pintarErroresCampo(err, { salaId: 'editar-sala', cantidadPersonas: 'editar-cantidad', horaInicio: 'editar-inicio', horaFin: 'editar-fin', fecha: 'editar-fecha' }); } catch (_) {}
        toast.error(err.message);
      } else {
        toast.error(err.message || 'Error al actualizar la reserva');
      }
    } finally { btn.disabled = false; }
  });

  /* ══════════════════════════════
     FORMULARIO NUEVA RESERVA
  ══════════════════════════════ */
  function hoyStr() { return new Date().toISOString().split('T')[0]; }

  function limpiarErrores() {
    [errSala, errCantidad, errFecha, errInicio, errFin].forEach(e => e.textContent = '');
    [selSala, inpCantidad, inpFecha, horaInicio, horaFin].forEach(i => i.classList.remove('error'));
  }

  function validar() {
    limpiarErrores();
    let ok = true;
    const hoy  = hoyStr();
    const sala = salaSeleccionada(selSala.value);

    if (!selSala.value) { errSala.textContent = 'Seleccione una sala'; selSala.classList.add('error'); ok = false; }

    const cantVal = parseInt(inpCantidad.value);
    if (!inpCantidad.value || isNaN(cantVal) || cantVal < 1) {
      errCantidad.textContent = 'Mínimo 1 persona'; inpCantidad.classList.add('error'); ok = false;
    } else if (sala && cantVal > sala.capacidadMaxima) {
      errCantidad.textContent = `Supera la capacidad máxima (${sala.capacidadMaxima})`; inpCantidad.classList.add('error'); ok = false;
    }

    if (!inpFecha.value)   { errFecha.textContent  = 'Fecha obligatoria'; inpFecha.classList.add('error'); ok = false; }
    else if (inpFecha.value < hoy) { errFecha.textContent = 'No puede ser anterior a hoy'; inpFecha.classList.add('error'); ok = false; }
    if (!horaInicio.value) { errInicio.textContent = 'Hora inicio obligatoria'; horaInicio.classList.add('error'); ok = false; }
    if (!horaFin.value)    { errFin.textContent    = 'Hora fin obligatoria';    horaFin.classList.add('error');    ok = false; }
    if (horaInicio.value && horaFin.value && horaInicio.value >= horaFin.value) {
      errInicio.textContent = 'Hora inicio debe ser anterior a hora fin'; horaInicio.classList.add('error'); ok = false;
    }
    if (ok && reglasCache.length > 0 && inpFecha.value && horaInicio.value && horaFin.value) {
      const iniM = parseMins(horaInicio.value + ':00');
      const finM = parseMins(horaFin.value    + ':00');
      const dia  = new Date(inpFecha.value + 'T00:00:00').getDay() || 7;
      const aps  = reglasCache.filter(r => r.tipo === 'APERTURA' && (r.diaSemana == null || r.diaSemana === dia));
      if (aps.length) {
        const dentro = aps.some(r => iniM >= parseMins(r.horaInicio) && finM <= parseMins(r.horaFin));
        if (!dentro) { errInicio.textContent = 'Fuera del horario comercial permitido'; horaInicio.classList.add('error'); ok = false; }
      }
    }
    return ok;
  }

  document.getElementById('reserva-form').addEventListener('submit', async function (e) {
    e.preventDefault();
    if (!validar()) return;
    btnReservar.disabled = true;
    try {
      await api.post('/api/reservas', {
        salaId:           parseInt(selSala.value),
        cantidadPersonas: parseInt(inpCantidad.value),
        fecha:            inpFecha.value,
        horaInicio:       horaInicio.value + ':00',
        horaFin:          horaFin.value    + ':00'
      });
      mostrarExito('Reserva creada correctamente', () => {
        document.getElementById('reserva-form').reset();
        inpFecha.min            = hoyStr();
        capacityHint.textContent = '';
        tlWrap.style.display        = 'none';
        tlPlaceholder.style.display = 'block';
        tlPlaceholder.textContent   = 'Seleccione una sala y fecha para ver la disponibilidad.';
        cargarSalas();
      });
    } catch (err) {
      if (err.status === 409) {
        const msg = err.message || 'Conflicto';
        if (msg.toLowerCase().includes('personas') || msg.toLowerCase().includes('capacidad')) {
          errCantidad.textContent = msg; inpCantidad.classList.add('error');
        } else {
          errInicio.textContent = msg; horaInicio.classList.add('error');
        }
        toast.error(msg);
      } else if (err.status === 400) {
        try { pintarErroresCampo(err, { salaId: 'sala', cantidadPersonas: 'cantidad-personas', horaInicio: 'hora-inicio', horaFin: 'hora-fin' }); } catch (_) {}
        toast.error(err.message);
      } else {
        toast.error(err.message || 'Error al crear la reserva');
      }
    } finally { btnReservar.disabled = false; }
  });

  /* ── Init ── */
  inpFecha.min    = hoyStr();
  editarFecha.min = hoyStr();
  await cargarSalas();
})();

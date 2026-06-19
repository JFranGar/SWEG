/* cliente.js */
(async function () {
  const me = await guard(['CLIENTE']);
  if (!me) return;

  /* ── Navegación ── */
  function mostrarSeccion(sec) {
    document.getElementById('section-buscar-salas').style.display = sec === 'buscar-salas' ? '' : 'none';
    document.getElementById('section-reservar').style.display     = sec === 'reservar'     ? '' : 'none';
    document.getElementById('section-mis').style.display          = sec === 'mis'          ? '' : 'none';
    document.getElementById('nav-buscar-salas').classList.toggle('active', sec === 'buscar-salas');
    document.getElementById('nav-reservar').classList.toggle('active',     sec === 'reservar');
    document.getElementById('nav-mis').classList.toggle('active',          sec === 'mis');
    if (sec === 'mis') cargarMisReservas(0);
  }
  document.getElementById('nav-buscar-salas').addEventListener('click', e => { e.preventDefault(); mostrarSeccion('buscar-salas'); });
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
  let reservasMap  = {};

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

  /* ── Estado de la reserva que se está editando (para resaltado azul) ── */
  let editarOriginalInicio = null;
  let editarOriginalFin    = null;
  let editarOriginalSalaId = null;
  let editarOriginalFecha  = null;

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

  /* ── Refs Modal Confirmación ── */
  const modalConfirmar = document.getElementById('modal-confirmar');
  let confirmarCb = null;
  document.getElementById('modal-confirmar-si').addEventListener('click', () => {
    modalConfirmar.close();
    if (confirmarCb) { confirmarCb(); confirmarCb = null; }
  });
  document.getElementById('modal-confirmar-no').addEventListener('click', () => {
    modalConfirmar.close();
    confirmarCb = null;
  });
  function confirmar(msg, cb) {
    document.getElementById('modal-confirmar-msg').textContent = msg;
    confirmarCb = cb;
    modalConfirmar.showModal();
  }

  document.getElementById('modal-editar-close').addEventListener('click', () => modalEditar.close());
  document.getElementById('btn-editar-cancelar').addEventListener('click', () => modalEditar.close());

  /* ══════════════════════════════
     TIMELINE VERTICAL
     Horario fijo: 07:00 – 22:00
  ══════════════════════════════ */
  const TL_H_INI = 7;
  const TL_H_FIN = 22;
  let tlSelStart = null;
  let tlSelEnd   = null;

  function parseMins(t) {
    if (Array.isArray(t)) return (t[0] || 0) * 60 + (t[1] || 0);
    const p = String(t).split(':').map(Number);
    return (p[0] || 0) * 60 + (p[1] || 0);
  }

  function slotToTime(idx) {
    const m = TL_H_INI * 60 + idx * 30;
    return String(Math.floor(m / 60)).padStart(2, '0') + ':' + String(m % 60).padStart(2, '0');
  }

  /* Usa fecha local del dispositivo para evitar desfase UTC */
  function localDateStr() {
    const d = new Date();
    return d.getFullYear() + '-'
      + String(d.getMonth() + 1).padStart(2, '0') + '-'
      + String(d.getDate()).padStart(2, '0');
  }

  function clasificarSlot(idx, reservas, nowMins, isToday, reservaPropia = null) {
    const sMin = TL_H_INI * 60 + idx * 30;
    const eMin = sMin + 30;
    if (isToday && eMin <= nowMins) return 'pasado';
    if (reservaPropia) {
      const pIni = parseMins(reservaPropia.horaInicio);
      const pFin = parseMins(reservaPropia.horaFin);
      if (sMin >= pIni && eMin <= pFin) return 'propio';
    }
    if (reservas.some(r => sMin < parseMins(r.horaFin) && parseMins(r.horaInicio) < eMin)) return 'ocupado';
    return 'libre';
  }

  const ESTADO_LABEL = { libre: 'Libre', ocupado: 'Ocupado', pasado: 'Hora pasada', propio: 'Tu reserva actual' };

  function renderTimelineEn(containerEl, reservas, fecha, onClickSlot, reservaPropia = null) {
    containerEl.innerHTML = '';
    const hoy     = localDateStr();
    const isToday = fecha === hoy;
    const now     = new Date();
    const nowMins = now.getHours() * 60 + now.getMinutes();
    const TL_SLOTS = (TL_H_FIN - TL_H_INI) * 2;

    for (let i = 0; i <= TL_SLOTS; i++) {
      const isEnd = i === TL_SLOTS;
      const row   = document.createElement('div');
      row.className = isEnd ? 'tl-row tl-row-end' : 'tl-row';

      const timeEl = document.createElement('div');
      timeEl.className   = 'tl-row-time';
      timeEl.textContent = slotToTime(i);
      row.appendChild(timeEl);

      if (!isEnd) {
        const estado = clasificarSlot(i, reservas, nowMins, isToday, reservaPropia);
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

  function renderTimeline(reservas, fecha) {
    tlSelStart = null; tlSelEnd = null;
    renderTimelineEn(tlRowsEl, reservas, fecha, idx => {
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
      const primer = tlRowsEl.querySelector('.tl-row-bar.libre, .tl-row-bar.ocupado');
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
      const reservas = await api.get(`/api/reservas/horario-dia?salaId=${salaId}&fecha=${fecha}`);
      renderTimeline(reservas || [], fecha);
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
      const reservas = await api.get(`/api/reservas/horario-dia?salaId=${salaId}&fecha=${fecha}`);
      const mismoContexto = salaId === editarOriginalSalaId && fecha === editarOriginalFecha;
      const reservaPropia = (mismoContexto && editarOriginalInicio && editarOriginalFin)
        ? { horaInicio: editarOriginalInicio, horaFin: editarOriginalFin }
        : null;
      renderTimelineEn(document.getElementById('edit-tl-rows'), reservas || [], fecha, null, reservaPropia);
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
      const idReserva = btn.dataset.cancel;
      confirmar('¿Estás seguro de que deseas cancelar esta reserva? Esta acción no se puede deshacer.', async () => {
        try {
          await api.patch('/api/reservas/' + idReserva + '/cancelar');
          mostrarExito('Reserva cancelada correctamente', () => cargarMisReservas(paginaActual));
        } catch (err) { toast.error(err.message || 'Error al cancelar'); }
      });
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
    editarFecha.min = localDateStr();

    editarOriginalInicio = String(r.horaInicio).substring(0, 5);
    editarOriginalFin    = String(r.horaFin).substring(0, 5);
    editarOriginalSalaId = r.sala ? String(r.sala.id) : null;
    editarOriginalFecha  = r.fecha;

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
    const hoy  = localDateStr();
    const sala = salaSeleccionada(editarSala.value);

    if (!editarSala.value) { errEditSala.textContent = 'Seleccione una sala'; editarSala.classList.add('error'); ok = false; }

    const cantVal = parseInt(editarCantidad.value);
    if (!editarCantidad.value || isNaN(cantVal) || cantVal < 1) {
      errEditCantidad.textContent = 'Mínimo 1 persona'; editarCantidad.classList.add('error'); ok = false;
    } else if (sala && cantVal > sala.capacidadMaxima) {
      errEditCantidad.textContent = `Supera la capacidad máxima (${sala.capacidadMaxima})`; editarCantidad.classList.add('error'); ok = false;
    }

    if (!editarFecha.value) { errEditFecha.textContent = 'Fecha obligatoria'; editarFecha.classList.add('error'); ok = false; }
    else if (editarFecha.value < hoy) { errEditFecha.textContent = 'No puede ser anterior a hoy'; editarFecha.classList.add('error'); ok = false; }
    if (!editarInicio.value) { errEditInicio.textContent = 'Hora inicio obligatoria'; editarInicio.classList.add('error'); ok = false; }
    if (!editarFin.value)    { errEditFin.textContent    = 'Hora fin obligatoria';    editarFin.classList.add('error');    ok = false; }
    if (editarInicio.value && editarFin.value && editarInicio.value >= editarFin.value) {
      errEditInicio.textContent = 'Hora inicio debe ser anterior a hora fin'; editarInicio.classList.add('error'); ok = false;
    }
    if (ok && editarInicio.value && editarFin.value) {
      const iniM = parseMins(editarInicio.value + ':00');
      const finM = parseMins(editarFin.value    + ':00');
      if (iniM < TL_H_INI * 60 || finM > TL_H_FIN * 60) {
        errEditInicio.textContent = `El horario debe estar entre ${slotToTime(0)} y ${String(TL_H_FIN).padStart(2,'0')}:00`;
        editarInicio.classList.add('error'); ok = false;
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
  function limpiarErrores() {
    [errSala, errCantidad, errFecha, errInicio, errFin].forEach(e => e.textContent = '');
    [selSala, inpCantidad, inpFecha, horaInicio, horaFin].forEach(i => i.classList.remove('error'));
  }

  function validar() {
    limpiarErrores();
    let ok = true;
    const hoy  = localDateStr();
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
    if (ok && horaInicio.value && horaFin.value) {
      const iniM = parseMins(horaInicio.value + ':00');
      const finM = parseMins(horaFin.value    + ':00');
      if (iniM < TL_H_INI * 60 || finM > TL_H_FIN * 60) {
        errInicio.textContent = `El horario debe estar entre ${slotToTime(0)} y ${String(TL_H_FIN).padStart(2,'0')}:00`;
        horaInicio.classList.add('error'); ok = false;
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
        inpFecha.min            = localDateStr();
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

  /* ══════════════════════════════
     BUSCAR SALAS DISPONIBLES — HU03
  ══════════════════════════════ */
  const buscarSalasForm = document.getElementById('buscar-salas-form');
  const filtroFecha     = document.getElementById('filtro-fecha');
  const filtroInicio    = document.getElementById('filtro-inicio');
  const filtroFin       = document.getElementById('filtro-fin');
  const filtroTipo      = document.getElementById('filtro-tipo');
  const buscarResultados = document.getElementById('buscar-salas-resultados');

  filtroFecha.min = localDateStr();

  const TIPO_LABEL = { REUNION: 'Reunión', SEMINARIO: 'Seminario', TRABAJO: 'Trabajo' };

  function limpiarErroresBuscar() {
    ['err-filtro-fecha', 'err-filtro-inicio', 'err-filtro-fin'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.textContent = '';
    });
    ['filtro-fecha', 'filtro-inicio', 'filtro-fin'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.classList.remove('error');
    });
  }

  function validarBuscarSalas() {
    limpiarErroresBuscar();
    const hoy = localDateStr();
    let ok = true;

    if (!filtroFecha.value) {
      document.getElementById('err-filtro-fecha').textContent = 'La fecha es obligatoria';
      filtroFecha.classList.add('error'); ok = false;
    } else if (filtroFecha.value < hoy) {
      document.getElementById('err-filtro-fecha').textContent = 'No se permiten fechas pasadas';
      filtroFecha.classList.add('error'); ok = false;
    }
    if (!filtroInicio.value) {
      document.getElementById('err-filtro-inicio').textContent = 'Hora inicio obligatoria';
      filtroInicio.classList.add('error'); ok = false;
    }
    if (!filtroFin.value) {
      document.getElementById('err-filtro-fin').textContent = 'Hora fin obligatoria';
      filtroFin.classList.add('error'); ok = false;
    }
    if (filtroInicio.value && filtroFin.value && filtroInicio.value >= filtroFin.value) {
      document.getElementById('err-filtro-inicio').textContent = 'La hora inicio debe ser menor a la hora fin';
      filtroInicio.classList.add('error'); ok = false;
    }
    return ok;
  }

  buscarSalasForm.addEventListener('submit', async function (e) {
    e.preventDefault();
    if (!validarBuscarSalas()) return;

    buscarResultados.innerHTML = '<div style="color:var(--text-muted);font-size:13px">Buscando...</div>';
    const btn = buscarSalasForm.querySelector('button[type="submit"]');
    btn.disabled = true;

    try {
      const params = new URLSearchParams({
        fecha:      filtroFecha.value,
        horaInicio: filtroInicio.value + ':00',
        horaFin:    filtroFin.value    + ':00'
      });
      if (filtroTipo.value) params.set('tipo', filtroTipo.value);

      const salas = await api.get('/api/reservas/buscar-salas?' + params.toString());
      buscarResultados.innerHTML = '';

      if (!salas || salas.length === 0) {
        buscarResultados.innerHTML = `
          <div class="card" style="max-width:560px;text-align:center;padding:28px">
            <div style="font-size:32px;margin-bottom:10px">🔍</div>
            <div style="color:var(--text-muted);font-size:14px">
              No hay espacios disponibles para los filtros seleccionados.
            </div>
          </div>`;
        return;
      }

      const grid = document.createElement('div');
      grid.style.cssText = 'display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:14px;max-width:860px';
      salas.forEach(sala => {
        const card = document.createElement('div');
        card.className = 'card';
        card.style.cursor = 'pointer';
        card.innerHTML = `
          <div style="font-weight:700;font-size:14px;margin-bottom:4px">${sala.nombre}</div>
          <div style="font-size:11px;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.5px;margin-bottom:8px">${TIPO_LABEL[sala.tipo] || sala.tipo}</div>
          <span class="badge badge-disponible">Disponible</span>
          <div style="font-size:12px;color:var(--text-muted);margin-top:8px">Capacidad: ${sala.capacidadMaxima} personas</div>
          <button class="btn btn-primary full" style="margin-top:12px;font-size:12px">Reservar esta sala</button>`;
        card.querySelector('button').addEventListener('click', () => {
          selSala.value = sala.id;
          inpFecha.value = filtroFecha.value;
          horaInicio.value = filtroInicio.value;
          horaFin.value    = filtroFin.value;
          actualizarHintCapacidad();
          cargarTimeline();
          mostrarSeccion('reservar');
        });
        grid.appendChild(card);
      });
      buscarResultados.appendChild(grid);
    } catch (err) {
      buscarResultados.innerHTML = '';
      const msg = err.fields?.fecha || err.fields?.horaInicio || err.message || 'Error al buscar salas';
      if (err.fields?.fecha) {
        document.getElementById('err-filtro-fecha').textContent = err.fields.fecha;
        filtroFecha.classList.add('error');
      }
      toast.error(msg);
    } finally {
      btn.disabled = false;
    }
  });

  /* ── Init ── */
  inpFecha.min    = localDateStr();
  editarFecha.min = localDateStr();
  await cargarSalas();
})();

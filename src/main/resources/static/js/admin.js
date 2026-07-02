/* admin.js */
(async function () {
  const me = await guard(['ADMIN']);
  if (!me) return;

  const PAGE = 8;

  /* ── Modal éxito ── */
  const modalExitoAdmin = document.getElementById('modal-exito-admin');
  let exitoCbAdmin = null;
  document.getElementById('modal-exito-admin-ok').addEventListener('click', () => {
    modalExitoAdmin.close();
    if (exitoCbAdmin) { exitoCbAdmin(); exitoCbAdmin = null; }
  });
  function mostrarExito(msg, cb) {
    document.getElementById('modal-exito-admin-msg').textContent = msg;
    exitoCbAdmin = cb || null;
    modalExitoAdmin.showModal();
  }

  /* ── Modal confirmación ── */
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

  /* ── Estado de paginación ── */
  let listaSalas = [],    pagSalas = 0;
  let listaUsuarios = [], pagUsuarios = 0;

  /* ── Helpers ── */
  function esc(s) {
    const d = document.createElement('div');
    d.textContent = String(s ?? '');
    return d.innerHTML;
  }

  function actualizarPag(sec, pag, total) {
    const pages = Math.ceil(total / PAGE) || 1;
    document.getElementById('info-' + sec).textContent = `Página ${pag + 1} de ${pages}`;
    document.getElementById('prev-' + sec).disabled = pag === 0;
    document.getElementById('next-' + sec).disabled = pag >= pages - 1;
  }

  /* ── Navegación ── */
  const SECCIONES = ['dashboard', 'panel', 'salas', 'usuarios', 'invitaciones', 'accesos'];
  function mostrarSeccion(sec) {
    SECCIONES.forEach(s => {
      document.getElementById('section-' + s).style.display = s === sec ? '' : 'none';
      document.getElementById('nav-' + s).classList.toggle('active', s === sec);
    });
    const titulos = {
      dashboard: 'Dashboard', panel: 'Panel de Control', salas: 'Gestión de Salas',
      usuarios: 'Gestión de Usuarios', invitaciones: 'Invitaciones', accesos: 'Historial de Accesos'
    };
    document.getElementById('main-title').textContent = titulos[sec];
  }

  document.getElementById('nav-dashboard').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('dashboard'); cargarDashboard();
  });
  document.getElementById('nav-panel').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('panel'); cargarPanel();
  });
  document.getElementById('nav-salas').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('salas');
    if (!listaSalas.length) cargarSalas();
  });
  document.getElementById('nav-usuarios').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('usuarios');
    if (!listaUsuarios.length) cargarUsuarios();
  });
  document.getElementById('nav-invitaciones').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('invitaciones'); cargarInvitaciones();
  });
  document.getElementById('nav-accesos').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('accesos'); cargarAccesos();
  });

  /* ══════════════════════════════
     PANEL DE CONTROL — HU09
  ══════════════════════════════ */
  const ESTADO_LABEL_PANEL = {
    DISPONIBLE: 'Disponible', RESERVADA: 'Reservada', EN_USO: 'En Uso',
    EN_LIMPIEZA: 'En Limpieza', MANTENIMIENTO: 'Mantenimiento'
  };

  async function cargarPanel() {
    const grid    = document.getElementById('panel-grid');
    const resumen = document.getElementById('panel-resumen');
    grid.innerHTML    = '<div style="color:var(--text-muted);font-size:13px">Cargando...</div>';
    resumen.innerHTML = '';
    try {
      const salas = await api.get('/api/admin/salas/panel') || [];
      grid.innerHTML = '';
      if (!salas.length) {
        grid.innerHTML = '<div style="color:var(--text-muted)">No hay salas registradas</div>';
        return;
      }
      const conteos = {};
      salas.forEach(s => { conteos[s.estadoPanel] = (conteos[s.estadoPanel] || 0) + 1; });
      Object.entries(conteos).forEach(([estado, cnt]) => {
        const chip = document.createElement('span');
        chip.className = 'resumen-chip ' + estado.toLowerCase();
        chip.textContent = `${ESTADO_LABEL_PANEL[estado] || estado}: ${cnt}`;
        resumen.appendChild(chip);
      });
      salas.forEach(s => {
        const card = document.createElement('div');
        card.className = 'panel-card estado-' + s.estadoPanel.toLowerCase();
        card.innerHTML = `
          <div class="panel-card-nombre" title="${esc(s.nombre)}">${esc(s.nombre)}</div>
          <div class="panel-card-tipo">${esc(s.tipo)}</div>
          <span class="badge badge-${s.estadoPanel.toLowerCase()}">${ESTADO_LABEL_PANEL[s.estadoPanel] || s.estadoPanel}</span>
          <div class="panel-card-cap">Capacidad: ${esc(s.capacidadMaxima)} personas</div>`;
        grid.appendChild(card);
      });
    } catch (e) { grid.innerHTML = '<div style="color:var(--text-muted)">Error cargando panel</div>'; toast.error('Error cargando panel de control'); }
  }

  document.getElementById('btn-refresh-panel').addEventListener('click', cargarPanel);

  /* ══════════════════════════════
     SECCIÓN SALAS
  ══════════════════════════════ */
  const modalSala = document.getElementById('modal-sala');
  const formSala  = document.getElementById('sala-form');
  const salaId    = document.getElementById('sala-id');
  const sNombre   = document.getElementById('nombre');
  const sTipo     = document.getElementById('tipo');
  const sCap      = document.getElementById('capacidad');
  const errSNombre = document.getElementById('err-nombre');
  const errSTipo   = document.getElementById('err-tipo');
  const errSCap    = document.getElementById('err-capacidad');

  function abrirModalSala(sala = null) {
    formSala.reset();
    [errSNombre, errSTipo, errSCap].forEach(e => e.textContent = '');
    [sNombre, sTipo, sCap].forEach(i => i.classList.remove('error'));
    if (sala) {
      salaId.value  = sala.id;
      sNombre.value = sala.nombre;
      sTipo.value   = sala.tipo;
      sCap.value    = sala.capacidadMaxima;
      document.getElementById('modal-sala-title').textContent = 'Editar Sala';
    } else {
      salaId.value = '';
      document.getElementById('modal-sala-title').textContent = 'Nueva Sala';
    }
    modalSala.showModal();
  }

  document.getElementById('btn-nueva-sala').addEventListener('click', () => abrirModalSala());
  document.getElementById('modal-sala-close').addEventListener('click', () => modalSala.close());
  document.getElementById('btn-cancelar-sala').addEventListener('click', () => modalSala.close());

  function renderSalas() {
    const slice = listaSalas.slice(pagSalas * PAGE, (pagSalas + 1) * PAGE);
    const tbody = document.getElementById('tabla-salas');
    tbody.innerHTML = '';
    if (!slice.length) {
      tbody.innerHTML = '<tr><td colspan="5" style="color:var(--text-muted)">Sin salas registradas</td></tr>';
      actualizarPag('salas', 0, 0);
      return;
    }
    slice.forEach(s => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${esc(s.nombre)}</td>
        <td>${esc(s.tipo)}</td>
        <td>${esc(s.capacidadMaxima)}</td>
        <td><span class="badge badge-${(s.estado || 'disponible').toLowerCase()}">${esc(s.estado || 'DISPONIBLE')}</span></td>
        <td>
          <button data-sala-id="${s.id}" class="btn btn-secondary btn-sm">Editar</button>
          <button data-del-sala="${s.id}" class="btn btn-danger btn-sm">Eliminar</button>
        </td>`;
      tbody.appendChild(tr);
    });
    actualizarPag('salas', pagSalas, listaSalas.length);
  }

  document.getElementById('prev-salas').addEventListener('click', () => { if (pagSalas > 0) { pagSalas--; renderSalas(); } });
  document.getElementById('next-salas').addEventListener('click', () => { pagSalas++; renderSalas(); });

  async function cargarSalas() {
    try {
      listaSalas = await api.get('/api/admin/salas') || [];
      pagSalas = 0;
      renderSalas();
    } catch (e) { toast.error('Error cargando salas'); }
  }

  function validarSala() {
    [errSNombre, errSTipo, errSCap].forEach(e => e.textContent = '');
    [sNombre, sTipo, sCap].forEach(i => i.classList.remove('error'));
    let ok = true;
    if (!sNombre.value.trim()) { errSNombre.textContent = 'Nombre obligatorio'; sNombre.classList.add('error'); ok = false; }
    if (!sTipo.value) { errSTipo.textContent = 'Tipo obligatorio'; sTipo.classList.add('error'); ok = false; }
    if (!sCap.value || parseInt(sCap.value) <= 0) { errSCap.textContent = 'Capacidad inválida'; sCap.classList.add('error'); ok = false; }
    return ok;
  }

  formSala.addEventListener('submit', async function (e) {
    e.preventDefault();
    if (!validarSala()) return;
    const btn = document.getElementById('btn-guardar');
    btn.disabled = true;
    const payload = { nombre: sNombre.value.trim(), tipo: sTipo.value, capacidadMaxima: parseInt(sCap.value) };
    try {
      const msg = salaId.value ? 'Sala actualizada correctamente' : 'Sala creada correctamente';
      if (salaId.value) {
        await api.put('/api/admin/salas/' + salaId.value, payload);
      } else {
        await api.post('/api/admin/salas', payload);
      }
      modalSala.close();
      await cargarSalas();
      mostrarExito(msg);
    } catch (err) {
      if (err.status === 400) { try { pintarErroresCampo(err, { capacidadMaxima: 'capacidad' }); } catch (_) {} toast.error(err.message); }
      else if (err.status === 409) { errSNombre.textContent = 'Ya existe una sala con ese nombre'; sNombre.classList.add('error'); toast.error(err.message); }
      else toast.error(err.message || 'Error al guardar');
    } finally { btn.disabled = false; }
  });

  document.getElementById('tabla-salas').addEventListener('click', async function (e) {
    const btn = e.target.closest('button');
    if (!btn) return;
    if (btn.dataset.salaId) {
      const sala = listaSalas.find(s => String(s.id) === String(btn.dataset.salaId));
      if (sala) abrirModalSala(sala);
    } else if (btn.dataset.delSala) {
      const idSala = btn.dataset.delSala;
      confirmar('¿Estás seguro de que deseas eliminar esta sala? Esta acción no se puede deshacer.', async () => {
        try {
          await api.del('/api/admin/salas/' + idSala);
          await cargarSalas();
          mostrarExito('Sala eliminada correctamente');
        } catch (err) { toast.error(err.message || 'Error al eliminar'); }
      });
    }
  });

  /* ══════════════════════════════
     SECCIÓN USUARIOS
  ══════════════════════════════ */
  const modalUsuario = document.getElementById('modal-usuario');
  const formUsuario  = document.getElementById('usuario-form');
  const uId          = document.getElementById('usuario-id');
  const uNombre      = document.getElementById('u-nombre');
  const uCorreo      = document.getElementById('u-correo');
  const uPass        = document.getElementById('u-contrasena');
  const uRol         = document.getElementById('u-rol');
  const errUNombre   = document.getElementById('err-u-nombre');
  const errUCorreo   = document.getElementById('err-u-correo');
  const errUPass     = document.getElementById('err-u-contrasena');
  const errURol      = document.getElementById('err-u-rol');

  function abrirModalUsuario(u = null) {
    formUsuario.reset();
    [errUNombre, errUCorreo, errUPass, errURol].forEach(e => e.textContent = '');
    [uNombre, uCorreo, uPass, uRol].forEach(i => i.classList.remove('error'));
    if (u) {
      uId.value     = u.id;
      uNombre.value = u.nombre;
      uCorreo.value = u.correo;
      uRol.value    = u.rol;
      uPass.value   = '';
      document.getElementById('modal-usuario-title').textContent = 'Editar Usuario';
    } else {
      uId.value = '';
      document.getElementById('modal-usuario-title').textContent = 'Nuevo Usuario';
    }
    modalUsuario.showModal();
  }

  document.getElementById('btn-nuevo-usuario').addEventListener('click', () => abrirModalUsuario());
  document.getElementById('modal-usuario-close').addEventListener('click', () => modalUsuario.close());
  document.getElementById('btn-cancelar-usuario').addEventListener('click', () => modalUsuario.close());

  function renderUsuarios() {
    const slice = listaUsuarios.slice(pagUsuarios * PAGE, (pagUsuarios + 1) * PAGE);
    const tbody = document.getElementById('tabla-usuarios');
    tbody.innerHTML = '';
    if (!slice.length) {
      tbody.innerHTML = '<tr><td colspan="5" style="color:var(--text-muted)">Sin usuarios registrados</td></tr>';
      actualizarPag('usuarios', 0, 0);
      return;
    }
    slice.forEach(u => {
      const loginStr = u.ultimoLogin ? u.ultimoLogin.replace('T', ' ').substring(0, 16) : '—';
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${esc(u.nombre)}</td>
        <td>${esc(u.correo)}</td>
        <td><span class="badge badge-${u.rol.toLowerCase()}" style="font-size:11px">${esc(u.rol)}</span></td>
        <td style="font-size:12px;color:var(--text-muted)">${loginStr}</td>
        <td>
          <button data-usuario-id="${u.id}" class="btn btn-secondary btn-sm">Editar</button>
          <button data-del-usuario="${u.id}" class="btn btn-danger btn-sm">Eliminar</button>
        </td>`;
      tbody.appendChild(tr);
    });
    actualizarPag('usuarios', pagUsuarios, listaUsuarios.length);
  }

  document.getElementById('prev-usuarios').addEventListener('click', () => { if (pagUsuarios > 0) { pagUsuarios--; renderUsuarios(); } });
  document.getElementById('next-usuarios').addEventListener('click', () => { pagUsuarios++; renderUsuarios(); });

  async function cargarUsuarios() {
    try {
      listaUsuarios = await api.get('/api/admin/usuarios') || [];
      pagUsuarios = 0;
      renderUsuarios();
    } catch (e) { toast.error('Error cargando usuarios'); }
  }

  function validarUsuario(esNuevo) {
    [errUNombre, errUCorreo, errUPass, errURol].forEach(e => e.textContent = '');
    [uNombre, uCorreo, uPass, uRol].forEach(i => i.classList.remove('error'));
    let ok = true;
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!uNombre.value.trim()) { errUNombre.textContent = 'Nombre obligatorio'; uNombre.classList.add('error'); ok = false; }
    if (!uCorreo.value.trim()) { errUCorreo.textContent = 'Correo obligatorio'; uCorreo.classList.add('error'); ok = false; }
    else if (!re.test(uCorreo.value.trim())) { errUCorreo.textContent = 'Correo inválido'; uCorreo.classList.add('error'); ok = false; }
    if (esNuevo && !uPass.value.trim()) { errUPass.textContent = 'Contraseña obligatoria para nuevo usuario'; uPass.classList.add('error'); ok = false; }
    if (!uRol.value) { errURol.textContent = 'Rol obligatorio'; uRol.classList.add('error'); ok = false; }
    return ok;
  }

  formUsuario.addEventListener('submit', async function (e) {
    e.preventDefault();
    const esNuevo = !uId.value;
    if (!validarUsuario(esNuevo)) return;
    const btn = document.getElementById('btn-u-guardar');
    btn.disabled = true;
    const payload = { nombre: uNombre.value.trim(), correo: uCorreo.value.trim(), contrasena: uPass.value || null, rol: uRol.value };
    try {
      const msgU = uId.value ? 'Usuario actualizado correctamente' : 'Usuario creado correctamente';
      if (uId.value) {
        await api.put('/api/admin/usuarios/' + uId.value, payload);
      } else {
        await api.post('/api/admin/usuarios', payload);
      }
      modalUsuario.close();
      await cargarUsuarios();
      mostrarExito(msgU);
    } catch (err) {
      if (err.status === 400) { try { pintarErroresCampo(err, { nombre: 'u-nombre', correo: 'u-correo', rol: 'u-rol' }); } catch (_) {} toast.error(err.message); }
      else if (err.status === 409) { errUCorreo.textContent = 'Ya existe un usuario con ese correo'; uCorreo.classList.add('error'); toast.error(err.message); }
      else toast.error(err.message || 'Error al guardar');
    } finally { btn.disabled = false; }
  });

  document.getElementById('tabla-usuarios').addEventListener('click', async function (e) {
    const btn = e.target.closest('button');
    if (!btn) return;
    if (btn.dataset.usuarioId) {
      const u = listaUsuarios.find(x => String(x.id) === String(btn.dataset.usuarioId));
      if (u) abrirModalUsuario(u);
    } else if (btn.dataset.delUsuario) {
      const idUsuario = btn.dataset.delUsuario;
      confirmar('¿Estás seguro de que deseas eliminar este usuario? Esta acción no se puede deshacer.', async () => {
        try {
          await api.del('/api/admin/usuarios/' + idUsuario);
          await cargarUsuarios();
          mostrarExito('Usuario eliminado correctamente');
        } catch (err) { toast.error(err.message || 'No se pudo eliminar'); }
      });
    }
  });

  /* ══════════════════════════════
     DASHBOARD — tarjetas + gráficas
  ══════════════════════════════ */
  const PALETA = ['#FFD700', '#4488FF', '#00C851', '#FFA500', '#A78BFA', '#22D3EE', '#FF6B6B', '#9a9a9a'];
  const COLOR_ESTADO = {
    'Pendientes': '#9a9a9a', 'Confirmadas': '#4488FF', 'En uso': '#00C851',
    'Finalizadas': '#666', 'Canceladas': '#FF4444'
  };

  /* Tooltip flotante compartido */
  const tip = document.createElement('div');
  tip.className = 'chart-tip';
  document.body.appendChild(tip);
  function moverTip(e, html) {
    tip.innerHTML = html;
    tip.classList.add('show');
    const pad = 14;
    let x = e.clientX + pad, y = e.clientY + pad;
    const r = tip.getBoundingClientRect();
    if (x + r.width > window.innerWidth) x = e.clientX - r.width - pad;
    if (y + r.height > window.innerHeight) y = e.clientY - r.height - pad;
    tip.style.left = x + 'px';
    tip.style.top = y + 'px';
  }
  function ocultarTip() { tip.classList.remove('show'); }
  function bindTip(el, html) {
    el.addEventListener('mousemove', e => moverTip(e, html));
    el.addEventListener('mouseleave', ocultarTip);
  }

  const SVGNS = 'http://www.w3.org/2000/svg';
  function svg(tag, attrs) {
    const el = document.createElementNS(SVGNS, tag);
    for (const k in attrs) el.setAttribute(k, attrs[k]);
    return el;
  }
  function nuevoSvg(w, h) {
    const s = svg('svg', { viewBox: `0 0 ${w} ${h}`, preserveAspectRatio: 'xMidYMid meet', role: 'img' });
    return s;
  }
  function vacio(cont, msg) { cont.innerHTML = `<div class="chart-empty">${msg || 'Sin datos'}</div>`; }

  /* Gráfica de barras verticales */
  function barChart(cont, datos, opts = {}) {
    cont.innerHTML = '';
    if (!datos.length || datos.every(d => d.valor === 0)) return vacio(cont, opts.vacio);
    const W = opts.W || 340, H = opts.H || 210, pad = { t: 16, r: 12, b: 34, l: 30 };
    const s = nuevoSvg(W, H);
    const max = Math.max(...datos.map(d => d.valor), 1);
    const innerW = W - pad.l - pad.r, innerH = H - pad.t - pad.b;
    const bw = innerW / datos.length;
    // grid + ejes
    for (let g = 0; g <= 4; g++) {
      const y = pad.t + innerH * (g / 4);
      s.appendChild(svg('line', { x1: pad.l, y1: y, x2: W - pad.r, y2: y, class: 'chart-grid' }));
      s.appendChild(Object.assign(svg('text', { x: pad.l - 5, y: y + 3, 'text-anchor': 'end', class: 'chart-tick' }),
        { textContent: Math.round(max * (1 - g / 4)) }));
    }
    datos.forEach((d, i) => {
      const bh = d.valor / max * innerH;
      const x = pad.l + i * bw + bw * 0.18;
      const y = pad.t + innerH - bh;
      const w = bw * 0.64;
      const color = opts.color || (d.color || PALETA[i % PALETA.length]);
      const rect = svg('rect', { x, y, width: w, height: Math.max(bh, d.valor > 0 ? 2 : 0), rx: 3, fill: color, class: 'bar-rect' });
      bindTip(rect, `<div class="tip-label">${escSvg(d.etiqueta)}</div><div class="tip-val">${d.valor}</div>`);
      s.appendChild(rect);
      if (d.valor > 0) s.appendChild(Object.assign(svg('text', { x: x + w / 2, y: y - 4, 'text-anchor': 'middle', class: 'bar-val' }), { textContent: d.valor }));
      s.appendChild(Object.assign(svg('text', { x: x + w / 2, y: H - pad.b + 14, 'text-anchor': 'middle', class: 'chart-tick' }), { textContent: d.etiqueta }));
    });
    cont.appendChild(s);
  }

  /* Barras horizontales (top clientes) */
  function hBarChart(cont, datos, opts = {}) {
    cont.innerHTML = '';
    if (!datos.length || datos.every(d => d.valor === 0)) return vacio(cont, opts.vacio);
    const rowH = 34, W = 340, padL = 4, padR = 40, labelW = 110;
    const H = datos.length * rowH + 10;
    const s = nuevoSvg(W, H);
    const max = Math.max(...datos.map(d => d.valor), 1);
    const trackX = padL + labelW, trackW = W - trackX - padR;
    datos.forEach((d, i) => {
      const y = 5 + i * rowH;
      s.appendChild(Object.assign(svg('text', { x: padL, y: y + rowH / 2 + 1, class: 'chart-tick', 'dominant-baseline': 'middle' }),
        { textContent: recorta(d.etiqueta, 15) }));
      s.appendChild(svg('rect', { x: trackX, y: y + 6, width: trackW, height: rowH - 18, rx: 4, fill: 'rgba(255,255,255,0.04)' }));
      const bw = Math.max(d.valor / max * trackW, d.valor > 0 ? 4 : 0);
      const rect = svg('rect', { x: trackX, y: y + 6, width: bw, height: rowH - 18, rx: 4, fill: PALETA[i % PALETA.length], class: 'bar-rect' });
      bindTip(rect, `<div class="tip-label">${escSvg(d.etiqueta)}</div><div class="tip-val">${d.valor} reservas</div>`);
      s.appendChild(rect);
      s.appendChild(Object.assign(svg('text', { x: trackX + bw + 6, y: y + rowH / 2 + 1, class: 'bar-val', 'dominant-baseline': 'middle' }), { textContent: d.valor }));
    });
    cont.appendChild(s);
  }

  /* Donut con leyenda */
  function donutChart(cont, datos, opts = {}) {
    cont.innerHTML = '';
    const total = datos.reduce((a, d) => a + d.valor, 0);
    if (!total) return vacio(cont, opts.vacio);
    const W = 340, H = 200, cx = 100, cy = 100, r = 72, ir = 44;
    const s = nuevoSvg(W, H);
    let ang = -Math.PI / 2;
    datos.forEach((d, i) => {
      if (d.valor === 0) return;
      const frac = d.valor / total;
      const a2 = ang + frac * Math.PI * 2;
      const color = d.color || opts.colores?.[d.etiqueta] || PALETA[i % PALETA.length];
      const large = frac > 0.5 ? 1 : 0;
      const p = svg('path', {
        d: `M ${cx + r * Math.cos(ang)} ${cy + r * Math.sin(ang)}
            A ${r} ${r} 0 ${large} 1 ${cx + r * Math.cos(a2)} ${cy + r * Math.sin(a2)}
            L ${cx + ir * Math.cos(a2)} ${cy + ir * Math.sin(a2)}
            A ${ir} ${ir} 0 ${large} 0 ${cx + ir * Math.cos(ang)} ${cy + ir * Math.sin(ang)} Z`,
        fill: color, class: 'donut-seg'
      });
      bindTip(p, `<div class="tip-label">${escSvg(d.etiqueta)}</div><div class="tip-val">${d.valor} (${Math.round(frac * 100)}%)</div>`);
      s.appendChild(p);
      ang = a2;
    });
    s.appendChild(Object.assign(svg('text', { x: cx, y: cy - 4, 'text-anchor': 'middle', fill: '#f5f5f5', 'font-size': 24, 'font-weight': 800 }), { textContent: total }));
    s.appendChild(Object.assign(svg('text', { x: cx, y: cy + 14, 'text-anchor': 'middle', fill: '#9a9a9a', 'font-size': 10 }), { textContent: 'total' }));
    cont.appendChild(s);
    // leyenda
    const leg = document.createElement('div');
    leg.className = 'chart-legend';
    datos.forEach((d, i) => {
      const color = d.color || opts.colores?.[d.etiqueta] || PALETA[i % PALETA.length];
      const item = document.createElement('div');
      item.className = 'legend-item';
      item.innerHTML = `<span class="legend-dot" style="background:${color}"></span>${esc(d.etiqueta)} · <b style="color:var(--text-primary)">${d.valor}</b>`;
      leg.appendChild(item);
    });
    cont.appendChild(leg);
  }

  /* Barras agrupadas (check-in / check-out) */
  function groupedBarChart(cont, datos, series, opts = {}) {
    cont.innerHTML = '';
    const totalAll = datos.reduce((a, d) => a + series.reduce((x, k) => x + (d[k.key] || 0), 0), 0);
    if (!totalAll) return vacio(cont, opts.vacio);
    const W = 340, H = 220, pad = { t: 16, r: 12, b: 34, l: 30 };
    const s = nuevoSvg(W, H);
    const max = Math.max(...datos.flatMap(d => series.map(k => d[k.key] || 0)), 1);
    const innerW = W - pad.l - pad.r, innerH = H - pad.t - pad.b;
    const gw = innerW / datos.length;
    for (let g = 0; g <= 4; g++) {
      const y = pad.t + innerH * (g / 4);
      s.appendChild(svg('line', { x1: pad.l, y1: y, x2: W - pad.r, y2: y, class: 'chart-grid' }));
      s.appendChild(Object.assign(svg('text', { x: pad.l - 5, y: y + 3, 'text-anchor': 'end', class: 'chart-tick' }), { textContent: Math.round(max * (1 - g / 4)) }));
    }
    datos.forEach((d, i) => {
      const bw = gw * 0.7 / series.length;
      series.forEach((k, j) => {
        const val = d[k.key] || 0;
        const bh = val / max * innerH;
        const x = pad.l + i * gw + gw * 0.15 + j * bw;
        const y = pad.t + innerH - bh;
        const rect = svg('rect', { x, y, width: bw * 0.9, height: Math.max(bh, val > 0 ? 2 : 0), rx: 2, fill: k.color, class: 'bar-rect' });
        bindTip(rect, `<div class="tip-label">${escSvg(d.etiqueta)} · ${k.label}</div><div class="tip-val">${val}</div>`);
        s.appendChild(rect);
      });
      s.appendChild(Object.assign(svg('text', { x: pad.l + i * gw + gw / 2, y: H - pad.b + 14, 'text-anchor': 'middle', class: 'chart-tick' }), { textContent: d.etiqueta }));
    });
    cont.appendChild(s);
    const leg = document.createElement('div');
    leg.className = 'chart-legend';
    series.forEach(k => {
      const item = document.createElement('div');
      item.className = 'legend-item';
      item.innerHTML = `<span class="legend-dot" style="background:${k.color}"></span>${k.label}`;
      leg.appendChild(item);
    });
    cont.appendChild(leg);
  }

  /* Área/línea (reservas por día) */
  function areaChart(cont, datos, opts = {}) {
    cont.innerHTML = '';
    if (!datos.length || datos.every(d => d.valor === 0)) return vacio(cont, opts.vacio);
    const W = 340, H = 210, pad = { t: 16, r: 14, b: 30, l: 28 };
    const s = nuevoSvg(W, H);
    const max = Math.max(...datos.map(d => d.valor), 1);
    const innerW = W - pad.l - pad.r, innerH = H - pad.t - pad.b;
    const stepX = innerW / (datos.length - 1 || 1);
    const px = i => pad.l + i * stepX;
    const py = v => pad.t + innerH - (v / max * innerH);
    for (let g = 0; g <= 4; g++) {
      const y = pad.t + innerH * (g / 4);
      s.appendChild(svg('line', { x1: pad.l, y1: y, x2: W - pad.r, y2: y, class: 'chart-grid' }));
      s.appendChild(Object.assign(svg('text', { x: pad.l - 5, y: y + 3, 'text-anchor': 'end', class: 'chart-tick' }), { textContent: Math.round(max * (1 - g / 4)) }));
    }
    const linePts = datos.map((d, i) => `${px(i)},${py(d.valor)}`).join(' ');
    const fillPts = `${pad.l},${pad.t + innerH} ${linePts} ${px(datos.length - 1)},${pad.t + innerH}`;
    s.appendChild(svg('polygon', { points: fillPts, class: 'area-fill' }));
    s.appendChild(svg('polyline', { points: linePts, class: 'area-line' }));
    datos.forEach((d, i) => {
      const dot = svg('circle', { cx: px(i), cy: py(d.valor), r: 4, class: 'area-dot' });
      bindTip(dot, `<div class="tip-label">${escSvg(d.etiqueta)}</div><div class="tip-val">${d.valor} reservas</div>`);
      s.appendChild(dot);
      s.appendChild(Object.assign(svg('text', { x: px(i), y: H - pad.b + 14, 'text-anchor': 'middle', class: 'chart-tick' }), { textContent: d.etiqueta }));
    });
    cont.appendChild(s);
  }

  function tarjeta(label, valor, color, hint) {
    return `<div class="stat-card" style="--accent:${color}">
      <div class="stat-label">${label}</div>
      <div class="stat-value">${valor}</div>
      ${hint ? `<div class="stat-hint">${hint}</div>` : ''}
    </div>`;
  }

  async function cargarDashboard() {
    const cards = document.getElementById('dash-cards');
    cards.innerHTML = '<div style="color:var(--text-muted);font-size:13px">Cargando indicadores...</div>';
    try {
      const d = await api.get('/api/admin/dashboard');
      cards.innerHTML =
        tarjeta('Usuarios registrados', d.totalUsuarios, '#FFD700',
          `<span class="stat-mini"><b>${d.clientesActivos}</b> clientes · <b>${d.recepcionistasActivos}</b> recep. · <b>${d.administradoresActivos}</b> admins</span>`) +
        tarjeta('Reservas creadas hoy', d.reservasCreadasHoy, '#4488FF') +
        tarjeta('Check-ins hoy', d.checkinsHoy, '#00C851') +
        tarjeta('Check-outs hoy', d.checkoutsHoy, '#FFA500') +
        tarjeta('Espacios disponibles', d.espaciosDisponibles, '#00C851',
          `<span class="stat-mini"><b>${d.espaciosOcupados}</b> en uso · <b>${d.espaciosReservados}</b> reservados · <b>${d.totalSalas}</b> total</span>`) +
        tarjeta('Reservas en uso', d.reservasEnUso, '#00C851',
          `<span class="stat-mini"><b>${d.reservasPendientes}</b> pend. · <b>${d.reservasConfirmadas}</b> conf. · <b>${d.reservasFinalizadas}</b> fin. · <b>${d.reservasCanceladas}</b> canc.</span>`);

      areaChart(document.getElementById('chart-reservas-dia'), d.reservasPorDia);
      donutChart(document.getElementById('chart-estados'), d.reservasPorEstado, { colores: COLOR_ESTADO });
      barChart(document.getElementById('chart-tipos'), d.usoPorTipoSala, { vacio: 'Aún no hay reservas' });
      groupedBarChart(document.getElementById('chart-accesos'), d.accesosPorDia,
        [{ key: 'checkins', label: 'Check-in', color: '#00C851' }, { key: 'checkouts', label: 'Check-out', color: '#FFA500' }],
        { vacio: 'Sin accesos registrados' });
      hBarChart(document.getElementById('chart-clientes'), d.topClientes, { vacio: 'Aún no hay clientes con reservas' });
      barChart(document.getElementById('chart-horarios'), d.ocupacionPorHorario, { color: '#4488FF', W: 760, H: 230, vacio: 'Sin reservas' });
      renderActividad(d.actividadReciente);
    } catch (e) {
      cards.innerHTML = '<div style="color:var(--text-muted)">Error cargando el dashboard</div>';
      toast.error('Error cargando el dashboard');
    }
  }

  const ACT_ICON = { RESERVA: '📅', CHECK_IN: '→', CHECK_OUT: '←', CANCELACION: '✕' };
  const ACT_LABEL = { RESERVA: 'reserva', CHECK_IN: 'check_in', CHECK_OUT: 'check_out', CANCELACION: 'cancelacion' };
  function renderActividad(items) {
    const cont = document.getElementById('dash-actividad');
    if (!items || !items.length) { cont.innerHTML = '<div class="act-empty">Sin actividad reciente</div>'; return; }
    cont.innerHTML = '';
    items.forEach(it => {
      const row = document.createElement('div');
      row.className = 'actividad-item';
      row.innerHTML = `
        <div class="act-icon act-${ACT_LABEL[it.tipo] || 'reserva'}">${ACT_ICON[it.tipo] || '•'}</div>
        <div class="act-body">
          <div class="act-desc">${esc(it.descripcion)}</div>
          <div class="act-time">${fechaHora(it.fecha)}</div>
        </div>`;
      cont.appendChild(row);
    });
  }

  document.getElementById('btn-refresh-dashboard').addEventListener('click', cargarDashboard);

  /* ══════════════════════════════
     INVITACIONES — HU RBAC Híbrido
  ══════════════════════════════ */
  const modalInvitacion = document.getElementById('modal-invitacion');
  const formInvitacion = document.getElementById('invitacion-form');
  const invRol = document.getElementById('inv-rol');
  const invCorreo = document.getElementById('inv-correo');
  const invHoras = document.getElementById('inv-horas');
  const errInvRol = document.getElementById('err-inv-rol');
  const errInvCorreo = document.getElementById('err-inv-correo');
  const modalInvLink = document.getElementById('modal-invitacion-link');
  const invLinkInput = document.getElementById('inv-link-input');

  const INV_ESTADO_BADGE = { PENDIENTE: 'confirmada', USADA: 'en_uso', EXPIRADA: 'finalizada', REVOCADA: 'cancelada' };

  document.getElementById('btn-nueva-invitacion').addEventListener('click', () => {
    formInvitacion.reset();
    invHoras.value = 48;
    [errInvRol, errInvCorreo].forEach(e => e.textContent = '');
    [invRol, invCorreo].forEach(i => i.classList.remove('error'));
    modalInvitacion.showModal();
  });
  document.getElementById('modal-invitacion-close').addEventListener('click', () => modalInvitacion.close());
  document.getElementById('btn-cancelar-invitacion').addEventListener('click', () => modalInvitacion.close());
  document.getElementById('modal-inv-link-close').addEventListener('click', () => modalInvLink.close());
  document.getElementById('btn-cerrar-inv-link').addEventListener('click', () => modalInvLink.close());
  document.getElementById('btn-copiar-link').addEventListener('click', () => copiar(invLinkInput.value));

  function mostrarLink(ruta) {
    invLinkInput.value = location.origin + ruta;
    modalInvLink.showModal();
    invLinkInput.select();
  }

  async function copiar(texto) {
    try {
      await navigator.clipboard.writeText(texto);
      toast.success('Enlace copiado al portapapeles');
    } catch (e) {
      invLinkInput.select(); document.execCommand('copy');
      toast.info('Enlace seleccionado, use Ctrl+C');
    }
  }

  formInvitacion.addEventListener('submit', async e => {
    e.preventDefault();
    [errInvRol, errInvCorreo].forEach(x => x.textContent = '');
    [invRol, invCorreo].forEach(i => i.classList.remove('error'));
    let ok = true;
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!invRol.value) { errInvRol.textContent = 'Seleccione un rol'; invRol.classList.add('error'); ok = false; }
    if (!invCorreo.value.trim()) { errInvCorreo.textContent = 'Correo obligatorio'; invCorreo.classList.add('error'); ok = false; }
    else if (!re.test(invCorreo.value.trim())) { errInvCorreo.textContent = 'Correo inválido'; invCorreo.classList.add('error'); ok = false; }
    if (!ok) return;

    const btn = document.getElementById('btn-inv-guardar');
    btn.disabled = true;
    const payload = { rol: invRol.value, correo: invCorreo.value.trim(), horasValidez: parseInt(invHoras.value) || 48 };
    try {
      const inv = await api.post('/api/admin/invitaciones', payload);
      modalInvitacion.close();
      await cargarInvitaciones();
      mostrarLink(inv.rutaRegistro);
    } catch (err) {
      if (err.status === 400) { try { pintarErroresCampo(err, { rol: 'inv-rol', correo: 'inv-correo' }); } catch (_) {} toast.error(err.message); }
      else if (err.status === 409) { errInvCorreo.textContent = err.message; invCorreo.classList.add('error'); toast.error(err.message); }
      else toast.error(err.message || 'Error al generar invitación');
    } finally { btn.disabled = false; }
  });

  async function cargarInvitaciones() {
    const tbody = document.getElementById('tabla-invitaciones');
    tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted)">Cargando...</td></tr>';
    try {
      const lista = await api.get('/api/admin/invitaciones') || [];
      tbody.innerHTML = '';
      if (!lista.length) {
        tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted)">Aún no hay invitaciones emitidas</td></tr>';
        return;
      }
      lista.forEach(inv => {
        const tr = document.createElement('tr');
        const badge = INV_ESTADO_BADGE[inv.estado] || 'pendiente';
        const usada = inv.estado === 'USADA';
        const acciones = inv.estado === 'PENDIENTE'
          ? `<button data-copiar="${esc(inv.rutaRegistro)}" class="btn btn-secondary btn-sm">Copiar enlace</button>
             <button data-revocar="${inv.id}" class="btn btn-danger btn-sm">Revocar</button>`
          : (usada ? `<span style="font-size:11px;color:var(--text-muted)">${esc(inv.usuarioCreado || '—')}</span>` : '—');
        tr.innerHTML = `
          <td><span class="badge badge-${inv.rol.toLowerCase()}" style="font-size:11px">${esc(inv.rol)}</span></td>
          <td style="font-size:12px">${esc(inv.correo || '—')}</td>
          <td><span class="badge badge-${badge}">${esc(inv.estado)}</span></td>
          <td style="font-size:12px;color:var(--text-muted)">${esc(inv.creadaPor || '—')}</td>
          <td style="font-size:12px;color:var(--text-muted)">${fechaHora(inv.expiraEn)}</td>
          <td>${acciones}</td>`;
        tbody.appendChild(tr);
      });
    } catch (e) { tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted)">Error cargando invitaciones</td></tr>'; }
  }

  document.getElementById('tabla-invitaciones').addEventListener('click', async e => {
    const btn = e.target.closest('button');
    if (!btn) return;
    if (btn.dataset.copiar) {
      copiar(location.origin + btn.dataset.copiar);
    } else if (btn.dataset.revocar) {
      confirmar('¿Revocar esta invitación? El enlace dejará de funcionar.', async () => {
        try {
          await api.del('/api/admin/invitaciones/' + btn.dataset.revocar);
          await cargarInvitaciones();
          mostrarExito('Invitación revocada');
        } catch (err) { toast.error(err.message || 'No se pudo revocar'); }
      });
    }
  });

  /* ══════════════════════════════
     HISTORIAL DE ACCESOS (check-in/out)
  ══════════════════════════════ */
  let listaAccesos = [], pagAccesos = 0, filtroAccesos = 'TODOS';

  document.getElementById('accesos-chips').addEventListener('click', e => {
    const chip = e.target.closest('.chip');
    if (!chip) return;
    document.querySelectorAll('#accesos-chips .chip').forEach(c => c.classList.remove('active'));
    chip.classList.add('active');
    filtroAccesos = chip.dataset.filtro;
    pagAccesos = 0;
    renderAccesos();
  });
  document.getElementById('btn-refresh-accesos').addEventListener('click', cargarAccesos);
  document.getElementById('prev-accesos').addEventListener('click', () => { if (pagAccesos > 0) { pagAccesos--; renderAccesos(); } });
  document.getElementById('next-accesos').addEventListener('click', () => { pagAccesos++; renderAccesos(); });

  async function cargarAccesos() {
    const tbody = document.getElementById('tabla-accesos');
    tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted)">Cargando...</td></tr>';
    try {
      listaAccesos = await api.get('/api/admin/accesos') || [];
      pagAccesos = 0;
      renderAccesos();
    } catch (e) { tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted)">Error cargando historial</td></tr>'; }
  }

  function renderAccesos() {
    const tbody = document.getElementById('tabla-accesos');
    const filtrados = filtroAccesos === 'TODOS' ? listaAccesos : listaAccesos.filter(a => a.tipo === filtroAccesos);
    const slice = filtrados.slice(pagAccesos * PAGE, (pagAccesos + 1) * PAGE);
    tbody.innerHTML = '';
    if (!slice.length) {
      tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted)">Sin accesos registrados</td></tr>';
      actualizarPag('accesos', 0, 0);
      return;
    }
    slice.forEach(a => {
      const esIn = a.tipo === 'CHECK_IN';
      const tr = document.createElement('tr');
      const hi = String(a.horaInicio || '').substring(0, 5);
      const hf = String(a.horaFin || '').substring(0, 5);
      tr.innerHTML = `
        <td><span class="badge badge-${esIn ? 'disponible' : 'en_uso'}">${esIn ? '→ Check-in' : '← Check-out'}</span></td>
        <td style="font-size:12px">${fechaHora(a.fecha)}</td>
        <td>${esc(a.salaNombre)}</td>
        <td style="font-size:12px">${esc(a.clienteNombre || a.clienteCorreo || '—')}</td>
        <td style="font-size:12px;color:var(--text-muted)">${hi} – ${hf}</td>
        <td style="font-size:12px;color:var(--text-muted)">${esc(a.operador || '—')}</td>`;
      tbody.appendChild(tr);
    });
    actualizarPag('accesos', pagAccesos, filtrados.length);
  }

  /* ── Helpers compartidos ── */
  function escSvg(s) { return String(s ?? '').replace(/[<>&]/g, c => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;' }[c])); }
  function recorta(s, n) { s = String(s ?? ''); return s.length > n ? s.slice(0, n - 1) + '…' : s; }
  function fechaHora(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d)) return String(iso).replace('T', ' ').substring(0, 16);
    return d.toLocaleString('es-EC', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  /* ── Init ── */
  await cargarDashboard();
})();

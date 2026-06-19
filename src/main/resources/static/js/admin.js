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

  /* ── Estado de paginación ── */
  let listaSalas = [],    pagSalas = 0;
  let listaUsuarios = [], pagUsuarios = 0;
  let listaHorario = [],  pagHorario = 0;

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
  function mostrarSeccion(sec) {
    ['salas', 'usuarios', 'horario'].forEach(s => {
      document.getElementById('section-' + s).style.display = s === sec ? '' : 'none';
      document.getElementById('nav-' + s).classList.toggle('active', s === sec);
    });
    const titulos = { salas: 'Gestión de Salas', usuarios: 'Gestión de Usuarios', horario: 'Horario Comercial' };
    document.getElementById('main-title').textContent = titulos[sec];
  }

  document.getElementById('nav-salas').addEventListener('click', e => { e.preventDefault(); mostrarSeccion('salas'); });
  document.getElementById('nav-usuarios').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('usuarios');
    if (!listaUsuarios.length) cargarUsuarios();
  });
  document.getElementById('nav-horario').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('horario');
    if (!listaHorario.length) cargarHorario();
  });

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
      if (!confirm('¿Eliminar esta sala? Esta acción no se puede deshacer.')) return;
      try {
        await api.del('/api/admin/salas/' + btn.dataset.delSala);
        await cargarSalas();
        mostrarExito('Sala eliminada correctamente');
      } catch (err) { toast.error(err.message || 'Error al eliminar'); }
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
      if (!confirm('¿Eliminar usuario? Esta acción no se puede deshacer.')) return;
      try {
        await api.del('/api/admin/usuarios/' + btn.dataset.delUsuario);
        await cargarUsuarios();
        mostrarExito('Usuario eliminado correctamente');
      } catch (err) { toast.error(err.message || 'No se pudo eliminar'); }
    }
  });

  /* ══════════════════════════════
     SECCIÓN HORARIO COMERCIAL
  ══════════════════════════════ */
  const modalHorario = document.getElementById('modal-horario');
  const formHorario  = document.getElementById('horario-form');
  const hId    = document.getElementById('horario-id');
  const hTipo  = document.getElementById('h-tipo');
  const hIni   = document.getElementById('h-ini');
  const hFin   = document.getElementById('h-fin');
  const hDia   = document.getElementById('h-dia');
  const hDesc  = document.getElementById('h-desc');
  const hActivo = document.getElementById('h-activo');
  const errHTipo = document.getElementById('err-h-tipo');
  const errHIni  = document.getElementById('err-h-ini');
  const errHFin  = document.getElementById('err-h-fin');

  const DIAS_SEMANA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

  function horaStr(t) {
    if (!t) return '';
    if (Array.isArray(t)) return String(t[0]).padStart(2, '0') + ':' + String(t[1] || 0).padStart(2, '0');
    return String(t).substring(0, 5);
  }

  function abrirModalHorario(r = null) {
    formHorario.reset();
    [errHTipo, errHIni, errHFin].forEach(e => e.textContent = '');
    [hTipo, hIni, hFin].forEach(i => i.classList.remove('error'));
    hActivo.checked = true;
    if (r) {
      hId.value      = r.id;
      hTipo.value    = r.tipo;
      hIni.value     = horaStr(r.horaInicio);
      hFin.value     = horaStr(r.horaFin);
      hDia.value     = r.diaSemana != null ? String(r.diaSemana) : '';
      hDesc.value    = r.descripcion || '';
      hActivo.checked = r.activo;
      document.getElementById('modal-horario-title').textContent = 'Editar Regla';
    } else {
      hId.value = '';
      document.getElementById('modal-horario-title').textContent = 'Nueva Regla';
    }
    modalHorario.showModal();
  }

  document.getElementById('btn-nueva-regla').addEventListener('click', () => abrirModalHorario());
  document.getElementById('modal-horario-close').addEventListener('click', () => modalHorario.close());
  document.getElementById('btn-cancelar-horario').addEventListener('click', () => modalHorario.close());

  function renderHorario() {
    const slice = listaHorario.slice(pagHorario * PAGE, (pagHorario + 1) * PAGE);
    const tbody = document.getElementById('tabla-horario');
    tbody.innerHTML = '';
    if (!slice.length) {
      tbody.innerHTML = '<tr><td colspan="7" style="color:var(--text-muted)">Sin reglas registradas</td></tr>';
      actualizarPag('horario', 0, 0);
      return;
    }
    slice.forEach(r => {
      const diaStr   = r.diaSemana != null ? (DIAS_SEMANA[r.diaSemana] || r.diaSemana) : 'Todos';
      const activoBadge = r.activo
        ? '<span class="badge badge-disponible" style="font-size:11px">Activo</span>'
        : '<span class="badge badge-finalizada" style="font-size:11px">Inactivo</span>';
      const tipoBadge = r.tipo === 'APERTURA'
        ? '<span class="badge badge-confirmada" style="font-size:11px">APERTURA</span>'
        : '<span class="badge badge-cancelada" style="font-size:11px">BLOQUEO</span>';
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${tipoBadge}</td>
        <td style="font-size:12px">${esc(diaStr)}</td>
        <td>${esc(horaStr(r.horaInicio))}</td>
        <td>${esc(horaStr(r.horaFin))}</td>
        <td style="font-size:12px;color:var(--text-muted)">${r.descripcion ? esc(r.descripcion) : '—'}</td>
        <td>${activoBadge}</td>
        <td>
          <button data-horario-id="${r.id}" class="btn btn-secondary btn-sm">Editar</button>
          <button data-del-horario="${r.id}" class="btn btn-danger btn-sm">Eliminar</button>
        </td>`;
      tbody.appendChild(tr);
    });
    actualizarPag('horario', pagHorario, listaHorario.length);
  }

  document.getElementById('prev-horario').addEventListener('click', () => { if (pagHorario > 0) { pagHorario--; renderHorario(); } });
  document.getElementById('next-horario').addEventListener('click', () => { pagHorario++; renderHorario(); });

  async function cargarHorario() {
    try {
      listaHorario = await api.get('/api/admin/horario-reglas') || [];
      pagHorario = 0;
      renderHorario();
    } catch (e) { toast.error('Error cargando reglas'); }
  }

  function validarHorario() {
    [errHTipo, errHIni, errHFin].forEach(e => e.textContent = '');
    [hTipo, hIni, hFin].forEach(i => i.classList.remove('error'));
    let ok = true;
    if (!hTipo.value) { errHTipo.textContent = 'Tipo obligatorio'; hTipo.classList.add('error'); ok = false; }
    if (!hIni.value)  { errHIni.textContent  = 'Hora inicio obligatoria'; hIni.classList.add('error'); ok = false; }
    if (!hFin.value)  { errHFin.textContent  = 'Hora fin obligatoria'; hFin.classList.add('error'); ok = false; }
    if (hIni.value && hFin.value && hIni.value >= hFin.value) {
      errHIni.textContent = 'Hora inicio debe ser anterior a hora fin'; hIni.classList.add('error'); ok = false;
    }
    return ok;
  }

  formHorario.addEventListener('submit', async function (e) {
    e.preventDefault();
    if (!validarHorario()) return;
    const btn = document.getElementById('btn-h-guardar');
    btn.disabled = true;
    const payload = {
      tipo: hTipo.value,
      horaInicio: hIni.value + ':00',
      horaFin: hFin.value + ':00',
      diaSemana: hDia.value ? parseInt(hDia.value) : null,
      descripcion: hDesc.value.trim() || null,
      activo: hActivo.checked
    };
    try {
      const msgH = hId.value ? 'Regla de horario actualizada correctamente' : 'Regla de horario creada correctamente';
      if (hId.value) {
        await api.put('/api/admin/horario-reglas/' + hId.value, payload);
      } else {
        await api.post('/api/admin/horario-reglas', payload);
      }
      modalHorario.close();
      await cargarHorario();
      mostrarExito(msgH);
    } catch (err) { toast.error(err.message || 'Error al guardar la regla'); }
    finally { btn.disabled = false; }
  });

  document.getElementById('tabla-horario').addEventListener('click', async function (e) {
    const btn = e.target.closest('button');
    if (!btn) return;
    if (btn.dataset.horarioId) {
      const r = listaHorario.find(x => String(x.id) === String(btn.dataset.horarioId));
      if (r) abrirModalHorario(r);
    } else if (btn.dataset.delHorario) {
      if (!confirm('¿Eliminar esta regla de horario?')) return;
      try {
        await api.del('/api/admin/horario-reglas/' + btn.dataset.delHorario);
        await cargarHorario();
        mostrarExito('Regla de horario eliminada correctamente');
      } catch (err) { toast.error(err.message || 'Error al eliminar'); }
    }
  });

  /* ── Init ── */
  await cargarSalas();
})();

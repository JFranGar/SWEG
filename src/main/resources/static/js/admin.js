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
  function mostrarSeccion(sec) {
    ['panel', 'salas', 'usuarios'].forEach(s => {
      document.getElementById('section-' + s).style.display = s === sec ? '' : 'none';
      document.getElementById('nav-' + s).classList.toggle('active', s === sec);
    });
    const titulos = { panel: 'Panel de Control', salas: 'Gestión de Salas', usuarios: 'Gestión de Usuarios' };
    document.getElementById('main-title').textContent = titulos[sec];
  }

  document.getElementById('nav-panel').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('panel'); cargarPanel();
  });
  document.getElementById('nav-salas').addEventListener('click', e => { e.preventDefault(); mostrarSeccion('salas'); });
  document.getElementById('nav-usuarios').addEventListener('click', e => {
    e.preventDefault(); mostrarSeccion('usuarios');
    if (!listaUsuarios.length) cargarUsuarios();
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

  /* ── Init ── */
  await cargarSalas();
})();

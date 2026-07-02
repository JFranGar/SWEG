/* registro-invitacion.js — Completar registro de cuenta interna por invitación.
 *
 * El rol NO se muestra como editable ni se envía desde el frontend: proviene de
 * la invitación y lo asigna el backend tras validar el token (un solo uso + expiración).
 */
(function () {
  const params = new URLSearchParams(location.search);
  const token = params.get('token');

  const carga = document.getElementById('estado-carga');
  const bloqueInvalido = document.getElementById('bloque-invalido');
  const invalidoMsg = document.getElementById('invalido-msg');
  const form = document.getElementById('inv-form');
  const rolInvitado = document.getElementById('rol-invitado');
  const correo = document.getElementById('correo');
  const nombre = document.getElementById('nombre');
  const contrasena = document.getElementById('contrasena');
  const contrasena2 = document.getElementById('contrasena2');
  const errNombre = document.getElementById('err-nombre');
  const errPass = document.getElementById('err-contrasena');
  const errPass2 = document.getElementById('err-contrasena2');
  const btn = document.getElementById('btn-activar');

  const ROL_LABEL = { RECEPCIONISTA: 'Recepcionista', ADMIN: 'Administrador' };

  function mostrarInvalido(msg) {
    carga.style.display = 'none';
    form.style.display = 'none';
    invalidoMsg.textContent = msg;
    bloqueInvalido.style.display = 'block';
  }

  async function init() {
    if (!token) { mostrarInvalido('El enlace de invitación no es válido (falta el token).'); return; }
    try {
      const info = await api.get('/api/auth/invitacion/' + encodeURIComponent(token));
      carga.style.display = 'none';
      form.style.display = '';
      rolInvitado.textContent = ROL_LABEL[info.rol] || info.rol;
      correo.value = info.correo || '';
    } catch (err) {
      if (err.status === 404) mostrarInvalido('La invitación no existe.');
      else if (err.status === 410) mostrarInvalido('La invitación ya fue utilizada, revocada o expiró.');
      else mostrarInvalido(err.message || 'No se pudo validar la invitación.');
    }
  }

  function validar() {
    [errNombre, errPass, errPass2].forEach(e => e.textContent = '');
    [nombre, contrasena, contrasena2].forEach(i => i.classList.remove('error'));
    let ok = true;
    if (!nombre.value.trim()) { errNombre.textContent = 'Nombre obligatorio'; nombre.classList.add('error'); ok = false; }
    if (!contrasena.value) { errPass.textContent = 'Contraseña obligatoria'; contrasena.classList.add('error'); ok = false; }
    else if (contrasena.value.length < 4) { errPass.textContent = 'Mínimo 4 caracteres'; contrasena.classList.add('error'); ok = false; }
    if (contrasena2.value !== contrasena.value) { errPass2.textContent = 'Las contraseñas no coinciden'; contrasena2.classList.add('error'); ok = false; }
    return ok;
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();
    if (!validar()) return;
    const original = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Activando...';
    const body = { nombre: nombre.value.trim(), contrasena: contrasena.value };
    try {
      await api.post('/api/auth/invitacion/' + encodeURIComponent(token) + '/completar', body);
      toast.success('Cuenta activada. Ahora puedes iniciar sesión.');
      setTimeout(() => location.href = '/html/login.html', 1200);
    } catch (err) {
      if (err.status === 400) {
        try { pintarErroresCampo(err); } catch (_) {}
        toast.error(err.message || 'Revisa los campos');
      } else if (err.status === 410 || err.status === 404) {
        mostrarInvalido('La invitación ya no es válida.');
      } else if (err.status === 409) {
        toast.error(err.message || 'No se pudo completar el registro');
      } else {
        toast.error(err.message || 'Error al activar la cuenta');
      }
    } finally {
      btn.disabled = false;
      btn.innerHTML = original;
    }
  });

  init();
})();

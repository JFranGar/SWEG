/* registro.js — Auto-registro público de clientes (HU RBAC Híbrido).
 *
 * El rol se determina SIEMPRE en el backend (CLIENTE). El formulario público
 * jamás envía ni permite manipular el rol. Las cuentas internas se crean
 * exclusivamente por invitación.
 */
(function () {
  const form = document.getElementById('registro-form');
  const nombre = document.getElementById('nombre');
  const correo = document.getElementById('correo');
  const contrasena = document.getElementById('contrasena');
  const contrasena2 = document.getElementById('contrasena2');
  const errNombre = document.getElementById('err-nombre');
  const errCorreo = document.getElementById('err-correo');
  const errPass = document.getElementById('err-contrasena');
  const errPass2 = document.getElementById('err-contrasena2');
  const btn = document.getElementById('btn-registro');

  function limpiar() {
    [errNombre, errCorreo, errPass, errPass2].forEach(e => e.textContent = '');
    [nombre, correo, contrasena, contrasena2].forEach(i => i.classList.remove('error'));
  }

  function validar() {
    limpiar();
    let ok = true;
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!nombre.value.trim()) { errNombre.textContent = 'Nombre obligatorio'; nombre.classList.add('error'); ok = false; }
    if (!correo.value.trim()) { errCorreo.textContent = 'Correo obligatorio'; correo.classList.add('error'); ok = false; }
    else if (!re.test(correo.value.trim())) { errCorreo.textContent = 'Correo inválido'; correo.classList.add('error'); ok = false; }
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
    btn.innerHTML = '<span class="spinner"></span> Creando...';
    const body = { nombre: nombre.value.trim(), correo: correo.value.toLowerCase().trim(), contrasena: contrasena.value };
    try {
      await api.post('/api/auth/registro', body);
      toast.success('Cuenta creada. Ahora puedes iniciar sesión.');
      setTimeout(() => location.href = '/html/login.html', 1200);
    } catch (err) {
      if (err.status === 400) {
        try { pintarErroresCampo(err); } catch (_) {}
        toast.error(err.message || 'Revisa los campos');
      } else if (err.status === 409) {
        errCorreo.textContent = 'Ya existe una cuenta con ese correo';
        correo.classList.add('error');
        toast.error(err.message || 'El correo ya está registrado');
      } else {
        toast.error(err.message || 'Error al crear la cuenta');
      }
    } finally {
      btn.disabled = false;
      btn.innerHTML = original;
    }
  });
})();

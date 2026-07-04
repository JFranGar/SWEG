/* registro.js — Auto-registro público de clientes (HU RBAC Híbrido).
 *
 * El rol se determina SIEMPRE en el backend (CLIENTE). El formulario público
 * jamás envía ni permite manipular el rol. Las cuentas internas se crean
 * exclusivamente por invitación.
 *
 * Validaciones (CA-HU12-01 / CA-HU12-04):
 *  - Nombres y Apellidos: solo letras (tildes y ñ permitidas) y espacios.
 *  - Contraseña segura: mínimo 8 caracteres, mayúscula, minúscula y número.
 */
(function () {
  const form = document.getElementById('registro-form');
  const nombre = document.getElementById('nombre');
  const apellido = document.getElementById('apellido');
  const correo = document.getElementById('correo');
  const contrasena = document.getElementById('contrasena');
  const contrasena2 = document.getElementById('contrasena2');
  const errNombre = document.getElementById('err-nombre');
  const errApellido = document.getElementById('err-apellido');
  const errCorreo = document.getElementById('err-correo');
  const errPass = document.getElementById('err-contrasena');
  const errPass2 = document.getElementById('err-contrasena2');
  const btn = document.getElementById('btn-registro');

  const RE_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const RE_NOMBRE = /^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+(?: [A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$/;
  const RE_PASS = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;

  function limpiar() {
    [errNombre, errApellido, errCorreo, errPass, errPass2].forEach(e => e.textContent = '');
    [nombre, apellido, correo, contrasena, contrasena2].forEach(i => i.classList.remove('error'));
  }

  function validar() {
    limpiar();
    let ok = true;
    // Nombres
    if (!nombre.value.trim()) { errNombre.textContent = 'Nombres obligatorios'; nombre.classList.add('error'); ok = false; }
    else if (!RE_NOMBRE.test(nombre.value.trim())) { errNombre.textContent = 'Solo se permiten letras (sin números ni caracteres especiales)'; nombre.classList.add('error'); ok = false; }
    // Apellidos
    if (!apellido.value.trim()) { errApellido.textContent = 'Apellidos obligatorios'; apellido.classList.add('error'); ok = false; }
    else if (!RE_NOMBRE.test(apellido.value.trim())) { errApellido.textContent = 'Solo se permiten letras (sin números ni caracteres especiales)'; apellido.classList.add('error'); ok = false; }
    // Correo
    if (!correo.value.trim()) { errCorreo.textContent = 'Correo obligatorio'; correo.classList.add('error'); ok = false; }
    else if (!RE_CORREO.test(correo.value.trim())) { errCorreo.textContent = 'Correo inválido'; correo.classList.add('error'); ok = false; }
    // Contraseña segura
    if (!contrasena.value) { errPass.textContent = 'Contraseña obligatoria'; contrasena.classList.add('error'); ok = false; }
    else if (!RE_PASS.test(contrasena.value)) { errPass.textContent = 'Mínimo 8 caracteres, con mayúscula, minúscula y número'; contrasena.classList.add('error'); ok = false; }
    // Confirmación
    if (contrasena2.value !== contrasena.value) { errPass2.textContent = 'Las contraseñas no coinciden'; contrasena2.classList.add('error'); ok = false; }
    return ok;
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();
    if (!validar()) return;
    const original = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Creando...';
    const body = {
      nombre: nombre.value.trim(),
      apellido: apellido.value.trim(),
      correo: correo.value.toLowerCase().trim(),
      contrasena: contrasena.value
    };
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

# Dashboard del Administrador

## Como Administrador del coworking

Persona responsable de supervisar la operación del coworking y de tomar decisiones sobre su uso.

## Quiero un panel con tarjetas informativas, indicadores y gráficas interactivas del estado del sistema

Disponer de una vista central que resuma en tiempo real usuarios, reservas, accesos y ocupación de los espacios.

## Para conocer rápidamente lo que ocurre en la aplicación y tomar mejores decisiones

Interpretar de forma clara y ágil la situación del coworking sin revisar los datos manualmente.

## Criterios de aceptación

### Criterio de aceptación 1

Dado que inicio sesión como administrador y abro la sección Dashboard

Cuando el sistema carga la información

Entonces veo tarjetas con el total de usuarios registrados, los clientes, recepcionistas y administradores activos, las reservas del día por estado y los espacios disponibles, ocupados y reservados, todo calculado con datos reales.

### Criterio de aceptación 2

Dado que existen reservas y accesos registrados en el sistema

Cuando reviso las gráficas del dashboard

Entonces se muestran de forma interactiva las reservas por día, las reservas por estado, el uso de espacios por tipo, los check-ins y check-outs por fecha, los clientes con mayor actividad y la ocupación del coworking por horario.

### Criterio de aceptación 3

Dado que se ha generado actividad reciente como reservas, check-ins, check-outs o cancelaciones

Cuando presiono el botón de actualizar del dashboard

Entonces las tarjetas, las gráficas y la lista de actividad reciente se refrescan con la información más reciente del sistema.

# Check-in y Check-out

## Como Recepcionista del coworking

Persona encargada de controlar la llegada y la salida de los clientes en la recepción.

## Quiero registrar el check-in y el check-out de los clientes con una reserva válida

Marcar la entrada y la salida de cada cliente para reflejar la ocupación real de las salas.

## Para controlar el uso efectivo de los espacios y mantener la trazabilidad de los accesos

Asegurar que solo se ocupen salas con reservas válidas y contar con un historial confiable de accesos.

## Criterios de aceptación

### Criterio de aceptación 1

Dado que un cliente tiene una reserva confirmada dentro de su horario válido

Cuando registro el check-in de esa reserva

Entonces la reserva pasa al estado En uso y se guardan la fecha, la hora y el usuario responsable que realizó el registro.

### Criterio de aceptación 2

Dado que una reserva ya tiene un check-in registrado, o está cancelada, vencida o no existe

Cuando intento registrar el check-in nuevamente

Entonces el sistema rechaza la acción con un mensaje claro y no permite realizar un doble check-in.

### Criterio de aceptación 3

Dado que una reserva se encuentra En uso tras un check-in previo

Cuando registro el check-out de esa reserva

Entonces la reserva pasa al estado Finalizada, la sala se libera, el evento queda disponible en el historial de accesos para el administrador, y el sistema impide el check-out sin check-in previo o un doble check-out.

# Registro de cuentas con modelo híbrido RBAC

## Como Administrador del coworking

Persona responsable de habilitar el acceso al sistema y de proteger las cuentas internas sensibles.

## Quiero que los clientes se auto-registren y que las cuentas internas se creen solo por invitación segura

Permitir el auto-registro público de clientes y restringir la creación de recepcionistas y administradores a un flujo de invitación controlado.

## Para agilizar la captación de clientes manteniendo el control estricto sobre las cuentas internas

Facilitar el acceso de nuevos clientes sin exponer la creación de cuentas con privilegios elevados.

## Criterios de aceptación

### Criterio de aceptación 1

Dado que soy un visitante sin cuenta en la aplicación

Cuando completo el formulario público de registro con mis datos

Entonces el sistema crea mi cuenta con el rol Cliente asignado en el servidor, sin permitir elegir ni manipular el rol desde el formulario.

### Criterio de aceptación 2

Dado que soy un administrador activo y necesito crear una cuenta interna

Cuando emito una invitación seleccionando el rol Recepcionista o Administrador

Entonces el sistema genera una invitación segura, de un solo uso y con tiempo de expiración, y registra quién la creó y cuándo.

### Criterio de aceptación 3

Dado que recibo un enlace de invitación válido y vigente

Cuando completo mi registro a través de ese enlace

Entonces el sistema crea mi cuenta con el rol definido en la invitación y la marca como utilizada, impidiendo que se reutilice o que se use después de haber expirado.

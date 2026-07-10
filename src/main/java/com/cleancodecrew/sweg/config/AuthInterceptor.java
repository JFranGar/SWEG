package com.cleancodecrew.sweg.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * AuthInterceptor
 *
 * Propósito: Intercepta peticiones HTTP para validar sesión y roles.
 * Sprint 1 - HU1: Sesión y roles.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

	public static final String SESSION_USER_ID = "usuarioId";
	public static final String SESSION_ROL = "rol";
	public static final String SESSION_NOMBRE = "nombre";
	public static final String SESSION_CORREO = "correo";

	private static final String NO_AUTORIZADO = "No autorizado";
	private static final String LOG_ROL_INSUFICIENTE = "Request to {} rejected: role {} insufficient";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
		HttpSession session = request.getSession(false);
		String path = request.getRequestURI();

		if (session == null || session.getAttribute(SESSION_USER_ID) == null) {
			log.debug("Request to {} rejected: no session", path);
			writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado", path);
			return false;
		}

		Object rolObj = session.getAttribute(SESSION_ROL);
		String rol = rolObj != null ? String.valueOf(rolObj) : null;

		// Authorization by path prefix
		if (path.startsWith("/api/admin/") || path.startsWith("/api/salas")) {
			if (!"ADMIN".equals(rol)) {
				log.debug(LOG_ROL_INSUFICIENTE, path, rol);
				writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, NO_AUTORIZADO, path);
				return false;
			}
		} else if (path.startsWith("/api/recepcion/")) {
			// Allow both RECEPCIONISTA and ADMIN to access reception endpoints
			if (!"RECEPCIONISTA".equals(rol) && !"ADMIN".equals(rol)) {
				log.debug(LOG_ROL_INSUFICIENTE, path, rol);
				writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, NO_AUTORIZADO, path);
				return false;
			}
		} else if ((path.startsWith("/api/cliente/") || path.startsWith("/api/reservas")) && !"CLIENTE".equals(rol)) {
			log.debug(LOG_ROL_INSUFICIENTE, path, rol);
			writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, NO_AUTORIZADO, path);
			return false;
		}

		return true;
	}

	private void writeJsonError(HttpServletResponse response, int status, String error, String path) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json;charset=UTF-8");
		String json = String.format("{\"status\":%d,\"error\":\"%s\",\"path\":\"%s\"}", status, error, path);
		response.getWriter().write(json);
	}
}

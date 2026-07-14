package com.cleancodecrew.sweg.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del AuthInterceptor (HU1 - control de sesión y roles).
 * Se mockean request/response/session; no hay contexto Spring ni base de datos.
 */
class AuthInterceptorTest {

    private AuthInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter body;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new AuthInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
    }

    private void conSesion(String rol) {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthInterceptor.SESSION_USER_ID)).thenReturn(1L);
        when(session.getAttribute(AuthInterceptor.SESSION_ROL)).thenReturn(rol);
    }

    @Test
    @DisplayName("Sin sesión responde 401 y bloquea")
    void sinSesion_401() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/reservas");
        when(request.getSession(false)).thenReturn(null);

        boolean ok = interceptor.preHandle(request, response, new Object());

        assertFalse(ok);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(body.toString().contains("No autenticado"));
    }

    @Test
    @DisplayName("Sesión sin usuarioId responde 401")
    void sesionSinUsuario_401() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/reservas");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthInterceptor.SESSION_USER_ID)).thenReturn(null);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Ruta /api/admin/ exige rol ADMIN")
    void admin_ok() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/admin/usuarios");
        conSesion("ADMIN");
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    @DisplayName("Ruta /api/admin/ con rol no ADMIN responde 403")
    void admin_forbidden() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/admin/usuarios");
        conSesion("CLIENTE");
        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("Ruta /api/salas exige ADMIN")
    void salas_forbiddenParaCliente() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/salas");
        conSesion("RECEPCIONISTA");
        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("Ruta /api/recepcion/ permite RECEPCIONISTA y ADMIN")
    void recepcion_permitida() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/recepcion/buscar-reserva");
        conSesion("RECEPCIONISTA");
        assertTrue(interceptor.preHandle(request, response, new Object()));

        conSesion("ADMIN");
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    @DisplayName("Ruta /api/recepcion/ con CLIENTE responde 403")
    void recepcion_forbiddenParaCliente() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/recepcion/accesos");
        conSesion("CLIENTE");
        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("Ruta /api/reservas exige rol CLIENTE")
    void reservas_soloCliente() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/reservas");
        conSesion("CLIENTE");
        assertTrue(interceptor.preHandle(request, response, new Object()));

        when(request.getRequestURI()).thenReturn("/api/reservas");
        conSesion("ADMIN");
        assertFalse(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    @DisplayName("Ruta /api genérica autenticada pasa sin exigir rol específico")
    void rutaGenerica_pasa() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/otro-endpoint");
        conSesion("CLIENTE");
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }
}

package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del GlobalExceptionHandler: cada excepción se traduce al estado HTTP correcto.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/x");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400")
    void illegalArgument() {
        ResponseEntity<ApiError> resp = handler.handleIllegal(new IllegalArgumentException("mal"), req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("IllegalStateException -> 409")
    void illegalState() {
        assertEquals(HttpStatus.CONFLICT,
                handler.handleIllegalState(new IllegalStateException("estado"), req).getStatusCode());
    }

    @Test
    @DisplayName("DuplicadoException -> 409")
    void duplicado() {
        assertEquals(HttpStatus.CONFLICT,
                handler.handleDuplicado(new DuplicadoException("dup"), req).getStatusCode());
    }

    @Test
    @DisplayName("ConflictoException -> 409")
    void conflicto() {
        assertEquals(HttpStatus.CONFLICT,
                handler.handleConflicto(new ConflictoException("conf"), req).getStatusCode());
    }

    @Test
    @DisplayName("NoSuchElementException -> 404")
    void notFound() {
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleNotFound(new NoSuchElementException("no"), req).getStatusCode());
    }

    @Test
    @DisplayName("Exception genérica -> 500")
    void generica() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                handler.handleAny(new Exception("boom"), req).getStatusCode());
    }

    @Test
    @DisplayName("DataIntegrityViolationException -> 409")
    void dataIntegrity() {
        assertEquals(HttpStatus.CONFLICT,
                handler.handleDataIntegrity(new DataIntegrityViolationException("fk"), req).getStatusCode());
    }

    @Test
    @DisplayName("Validación de campos -> 400 con detalle por campo")
    void validacion() throws Exception {
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Dummy(), "dummy");
        br.addError(new FieldError("dummy", "nombre", "El nombre es obligatorio"));

        MethodParameter parametro = new MethodParameter(
                Dummy.class.getDeclaredMethod("metodo", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parametro, br);

        ResponseEntity<ApiError> resp = handler.handleValidation(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().getFields().containsKey("nombre"));
    }

    /** Clase auxiliar solo para construir el MethodParameter del test de validación. */
    static class Dummy {
        private String nombre;
        void metodo(String nombre) { this.nombre = nombre; }
        String getNombre() { return nombre; }
    }
}

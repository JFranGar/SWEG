package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		Map<String, String> campos = new LinkedHashMap<>();
		for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
			campos.putIfAbsent(fe.getField(), fe.getDefaultMessage());
		}

		String mensajeGeneral = campos.isEmpty() ? "Datos invalidos" : "Hay campos invalidos en el formulario";

		return ResponseEntity.badRequest()
				.body(ApiError.of(400, mensajeGeneral, req.getRequestURI(), campos));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegal(IllegalArgumentException ex, HttpServletRequest req) {
		ApiError err = ApiError.of(400, ex.getMessage(), req.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
		ApiError err = ApiError.of(409, ex.getMessage(), req.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
	}

	@ExceptionHandler(DuplicadoException.class)
	public ResponseEntity<ApiError> handleDuplicado(DuplicadoException ex, HttpServletRequest req) {
		ApiError err = ApiError.of(409, ex.getMessage(), req.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
	}

	@ExceptionHandler(ConflictoException.class)
	public ResponseEntity<ApiError> handleConflicto(ConflictoException ex, HttpServletRequest req) {
		ApiError err = ApiError.of(409, ex.getMessage(), req.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
	}

	@ExceptionHandler({NoSuchElementException.class, EntityNotFoundException.class})
	public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest req) {
		ApiError err = ApiError.of(404, ex.getMessage(), req.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {
		log.error("Unhandled exception processing request {}", req.getRequestURI(), ex);
		ApiError err = ApiError.of(500, "Error interno del servidor", req.getRequestURI());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
		log.warn("Data integrity violation for {}: {}", req.getRequestURI(), ex.getMessage());
		ApiError err = ApiError.of(409, "Operación inválida: existe restricción de integridad", req.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
	}
}

package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		String msg = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getDefaultMessage())
				.collect(Collectors.joining("; "));
		ApiError err = ApiError.of(400, msg, req.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegal(IllegalArgumentException ex, HttpServletRequest req) {
		ApiError err = ApiError.of(400, ex.getMessage(), req.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
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
}

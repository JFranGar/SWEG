package com.cleancodecrew.sweg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de humo de la clase de arranque.
 *
 * Es un test UNITARIO puro: no usa {@code @SpringBootTest} ni levanta el contexto
 * de Spring, por lo que no requiere base de datos. Verifica que la aplicación esté
 * correctamente anotada (auto-configuración y planificación de tareas habilitadas).
 */
class SwegApplicationTests {

	@Test
	@DisplayName("La clase principal declara @SpringBootApplication y @EnableScheduling")
	void metadatosDeArranque() {
		assertTrue(SwegApplication.class.isAnnotationPresent(SpringBootApplication.class),
				"Debe estar anotada con @SpringBootApplication");
		assertTrue(SwegApplication.class.isAnnotationPresent(EnableScheduling.class),
				"Debe habilitar la planificación de tareas (@EnableScheduling)");
	}

	@Test
	@DisplayName("La clase principal es instanciable")
	void claseInstanciable() {
		assertNotNull(new SwegApplication());
	}
}

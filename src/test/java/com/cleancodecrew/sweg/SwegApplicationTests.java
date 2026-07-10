package com.cleancodecrew.sweg;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class SwegApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
		// Verifica que el contexto de Spring se cargue con todos sus beans.
		assertNotNull(context, "El contexto de la aplicacion debe cargarse correctamente");
	}

}

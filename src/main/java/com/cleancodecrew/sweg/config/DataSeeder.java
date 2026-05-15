package com.cleancodecrew.sweg.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.model.Rol;

/**
 * DataSeeder
 *
 * Propósito: Sembrar usuarios demo iniciales en la base de datos.
 * Sprint 1 - Provee `admin`, `recep`, `cliente` demo.
 *
 * Nota: UsuarioRepository y Usuario se implementarán en el Paso 3.
 * Este seeder es defensivo y no fallará si aún no existen los métodos esperados.
 */
@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final UsuarioRepository usuarioRepository;
	private final PasswordHasher passwordHasher;

	public DataSeeder(ApplicationContext ctx, PasswordHasher passwordHasher) {
		this.passwordHasher = passwordHasher;
		UsuarioRepository repo = null;
		try {
			repo = ctx.getBean(UsuarioRepository.class);
		} catch (Exception e) {
			repo = null;
		}
		this.usuarioRepository = repo;
	}

	@Override
	public void run(String... args) {
		try {
			if (usuarioRepository == null) {
				log.debug("DataSeeder: usuarioRepository not available yet, skipping seeding.");
				return;
			}

			long count = usuarioRepository.count();
			if (count == 0L) {
				log.info("DataSeeder: repository empty — creating demo users (admin, recepcionista, cliente).");
				// create demo users with password 'demo'
				Usuario admin = Usuario.builder()
						.nombre("Administrador")
						.correo("admin@swgec.ec")
						.contrasenaHash(passwordHasher.hash("demo"))
						.rol(Rol.ADMIN)
						.build();
				Usuario recep = Usuario.builder()
						.nombre("Recepcion")
						.correo("recepcion@swgec.ec")
						.contrasenaHash(passwordHasher.hash("demo"))
						.rol(Rol.RECEPCIONISTA)
						.build();
				Usuario cliente = Usuario.builder()
						.nombre("Cliente Demo")
						.correo("cliente@swgec.ec")
						.contrasenaHash(passwordHasher.hash("demo"))
						.rol(Rol.CLIENTE)
						.build();
				usuarioRepository.save(admin);
				usuarioRepository.save(recep);
				usuarioRepository.save(cliente);
				log.info("DataSeeder: demo users created (password 'demo').");
			} else {
				log.debug("DataSeeder: repository contains {} entries, skipping demo users.", count);
			}
		} catch (Exception e) {
			log.warn("DataSeeder failed defensively: {}", e.getMessage());
		}
	}
}

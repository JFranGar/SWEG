package com.cleancodecrew.sweg.config;

import com.cleancodecrew.sweg.model.Rol;
import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;

    public DataSeeder(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher    = passwordHasher;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.debug("DataSeeder: usuarios existentes, omitiendo seed.");
            return;
        }
        log.info("DataSeeder: creando usuarios demo (admin, recepcionista, cliente). Contraseña: 'demo'");
        usuarioRepository.save(Usuario.builder()
                .nombre("Administrador").correo("admin@swgec.ec")
                .contrasenaHash(passwordHasher.hash("demo")).rol(Rol.ADMIN).build());
        usuarioRepository.save(Usuario.builder()
                .nombre("Recepcion").correo("recepcion@swgec.ec")
                .contrasenaHash(passwordHasher.hash("demo")).rol(Rol.RECEPCIONISTA).build());
        usuarioRepository.save(Usuario.builder()
                .nombre("Cliente Demo").correo("cliente@swgec.ec")
                .contrasenaHash(passwordHasher.hash("demo")).rol(Rol.CLIENTE).build());
        log.info("DataSeeder: seed completado.");
    }
}

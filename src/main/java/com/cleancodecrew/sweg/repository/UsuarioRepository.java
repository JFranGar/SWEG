package com.cleancodecrew.sweg.repository;

import com.cleancodecrew.sweg.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UsuarioRepository
 *
 * Repositorio JPA para la entidad `Usuario`.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	Optional<Usuario> findByCorreoIgnoreCase(String correo);
}


package com.cleancodecrew.sweg.repository;

import com.cleancodecrew.sweg.model.Invitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * InvitacionRepository
 *
 * Acceso a datos de las invitaciones del modelo hibrido RBAC.
 */
@Repository
public interface InvitacionRepository extends JpaRepository<Invitacion, Long> {

    /** CA-HU12-01: localiza una invitacion por su token seguro. */
    Optional<Invitacion> findByToken(String token);

    /** CA-HU12-02: listado para el panel de administracion, mas recientes primero. */
    List<Invitacion> findAllByOrderByCreadaEnDesc();
}

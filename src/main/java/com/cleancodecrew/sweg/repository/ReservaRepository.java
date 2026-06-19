package com.cleancodecrew.sweg.repository;

import com.cleancodecrew.sweg.model.EstadoReserva;
import com.cleancodecrew.sweg.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
	List<Reserva> findBySala_IdAndFechaAndEstadoNot(Long salaId, LocalDate fecha, EstadoReserva estado);
	List<Reserva> findByCliente_IdOrderByFechaDescHoraInicioDesc(Long clienteId);
	Page<Reserva> findByCliente_IdOrderByFechaDescHoraInicioDesc(Long clienteId, Pageable pageable);

	boolean existsByCliente_Id(Long clienteId);

	boolean existsBySala_Id(Long salaId);

	boolean existsBySala_IdAndEstadoNot(Long salaId, EstadoReserva estado);

	 @org.springframework.data.jpa.repository.Query("""
		 SELECT r FROM Reserva r
		 WHERE r.sala.id = :salaId
			AND r.fecha = :fecha
			AND r.estado <> com.cleancodecrew.sweg.model.EstadoReserva.CANCELADA
	 """)
	 List<Reserva> findActivasPorSalaYFecha(@org.springframework.data.repository.query.Param("salaId") Long salaId,
														 @org.springframework.data.repository.query.Param("fecha") LocalDate fecha);

	 @org.springframework.data.jpa.repository.Query("""
		 SELECT r FROM Reserva r
		 WHERE LOWER(r.cliente.correo) = :correo
			AND r.fecha = :fecha
			AND r.estado IN (
				com.cleancodecrew.sweg.model.EstadoReserva.PENDIENTE,
				com.cleancodecrew.sweg.model.EstadoReserva.CONFIRMADA
			)
		 ORDER BY r.horaInicio ASC
	 """)
	 List<Reserva> findReservasDelDiaPorCorreoCliente(@org.springframework.data.repository.query.Param("correo") String correo,
																	  @org.springframework.data.repository.query.Param("fecha") LocalDate fecha);

	@org.springframework.data.jpa.repository.Query("""
		SELECT r FROM Reserva r
		WHERE r.sala.id = :salaId
		  AND r.fecha = :fecha
		  AND r.estado NOT IN (
			  com.cleancodecrew.sweg.model.EstadoReserva.CANCELADA,
			  com.cleancodecrew.sweg.model.EstadoReserva.FINALIZADA
		  )
		ORDER BY r.horaInicio ASC
	""")
	List<Reserva> findOcupadasPorSalaYFecha(
			@org.springframework.data.repository.query.Param("salaId") Long salaId,
			@org.springframework.data.repository.query.Param("fecha") LocalDate fecha);

	@org.springframework.data.jpa.repository.Query("""
		SELECT r FROM Reserva r
		WHERE r.estado IN (
			com.cleancodecrew.sweg.model.EstadoReserva.CONFIRMADA,
			com.cleancodecrew.sweg.model.EstadoReserva.PENDIENTE,
			com.cleancodecrew.sweg.model.EstadoReserva.EN_USO
		)
		AND (r.fecha < :hoy OR (r.fecha = :hoy AND r.horaFin <= :horaActual))
	""")
	List<Reserva> findReservasVencidas(
			@org.springframework.data.repository.query.Param("hoy") LocalDate hoy,
			@org.springframework.data.repository.query.Param("horaActual") LocalTime horaActual);

	boolean existsBySala_IdAndEstado(Long salaId, EstadoReserva estado);

	/** Devuelve reservas activas de una sala/fecha excluyendo explícitamente un ID dado (uso en editar). */
	@org.springframework.data.jpa.repository.Query("""
		SELECT r FROM Reserva r
		WHERE r.sala.id = :salaId
		  AND r.fecha   = :fecha
		  AND r.estado <> com.cleancodecrew.sweg.model.EstadoReserva.CANCELADA
		  AND r.id     <> :excludeId
	""")
	List<Reserva> findOtrasActivasPorSalaYFecha(
		@org.springframework.data.repository.query.Param("salaId")    Long salaId,
		@org.springframework.data.repository.query.Param("fecha")     LocalDate fecha,
		@org.springframework.data.repository.query.Param("excludeId") Long excludeId
	);
}


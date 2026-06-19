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
				com.cleancodecrew.sweg.model.EstadoReserva.CONFIRMADA,
				com.cleancodecrew.sweg.model.EstadoReserva.EN_USO
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

	/** CA-HU03-01: IDs de salas con reservas que se solapan con el horario solicitado. */
	@org.springframework.data.jpa.repository.Query("""
		SELECT DISTINCT r.sala.id FROM Reserva r
		WHERE r.fecha = :fecha
		  AND r.estado NOT IN (
		      com.cleancodecrew.sweg.model.EstadoReserva.CANCELADA,
		      com.cleancodecrew.sweg.model.EstadoReserva.FINALIZADA
		  )
		  AND r.horaInicio < :horaFin
		  AND r.horaFin > :horaInicio
	""")
	List<Long> findSalaIdsConReservaEnHorario(
		@org.springframework.data.repository.query.Param("fecha")      LocalDate fecha,
		@org.springframework.data.repository.query.Param("horaInicio") LocalTime horaInicio,
		@org.springframework.data.repository.query.Param("horaFin")    LocalTime horaFin
	);

	/** CA-HU09-02: Reservas CONFIRMADA/PENDIENTE hoy (salas "Reservadas" en el panel). */
	@org.springframework.data.jpa.repository.Query("""
		SELECT r FROM Reserva r JOIN FETCH r.sala
		WHERE r.estado IN (
		    com.cleancodecrew.sweg.model.EstadoReserva.CONFIRMADA,
		    com.cleancodecrew.sweg.model.EstadoReserva.PENDIENTE
		)
		AND r.fecha = :hoy
	""")
	List<Reserva> findActivasHoy(@org.springframework.data.repository.query.Param("hoy") LocalDate hoy);

	/** CA-HU08-01: Todas las reservas actualmente EN_USO (para vista recepcionista). */
	@org.springframework.data.jpa.repository.Query("""
		SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.cliente
		WHERE r.estado = com.cleancodecrew.sweg.model.EstadoReserva.EN_USO
		ORDER BY r.sala.nombre ASC
	""")
	List<Reserva> findTodasEnUso();
}


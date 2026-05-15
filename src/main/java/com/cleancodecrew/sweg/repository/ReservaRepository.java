package com.cleancodecrew.sweg.repository;

import com.cleancodecrew.sweg.model.EstadoReserva;
import com.cleancodecrew.sweg.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
	List<Reserva> findBySala_IdAndFechaAndEstadoNot(Long salaId, LocalDate fecha, EstadoReserva estado);
	List<Reserva> findByCliente_IdOrderByFechaDescHoraInicioDesc(Long clienteId);
}


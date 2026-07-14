package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.PanelSalaResponse;
import com.cleancodecrew.sweg.dto.SalaRequest;
import com.cleancodecrew.sweg.model.EstadoSala;
import com.cleancodecrew.sweg.model.Reserva;
import com.cleancodecrew.sweg.model.Sala;
import com.cleancodecrew.sweg.model.TipoSala;
import com.cleancodecrew.sweg.repository.ReservaRepository;
import com.cleancodecrew.sweg.repository.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas del SalaController (HU2 gestión de salas + HU9 panel). Repositorios mockeados.
 */
class SalaControllerTest {

    private SalaRepository salaRepository;
    private ReservaRepository reservaRepository;
    private SalaController controller;

    @BeforeEach
    void setUp() {
        salaRepository = mock(SalaRepository.class);
        reservaRepository = mock(ReservaRepository.class);
        controller = new SalaController(salaRepository, reservaRepository);
    }

    private Sala sala(long id, String nombre, EstadoSala estado) {
        return Sala.builder().id(id).nombre(nombre).tipo(TipoSala.REUNION)
                .capacidadMaxima(4).estado(estado).build();
    }

    private SalaRequest req(String nombre) {
        SalaRequest r = new SalaRequest();
        r.setNombre(nombre);
        r.setTipo(TipoSala.REUNION);
        r.setCapacidadMaxima(4);
        return r;
    }

    @Test
    @DisplayName("listAll excluye las salas ELIMINADAS y ordena por nombre")
    void listAll_filtraYordena() {
        when(salaRepository.findAll()).thenReturn(List.of(
                sala(1, "Zeta", EstadoSala.DISPONIBLE),
                sala(2, "Alfa", EstadoSala.DISPONIBLE),
                sala(3, "Borrada", EstadoSala.ELIMINADA)));

        List<Sala> body = controller.listAll().getBody();

        assertEquals(2, body.size());
        assertEquals("Alfa", body.get(0).getNombre());
        assertEquals("Zeta", body.get(1).getNombre());
    }

    @Test
    @DisplayName("create con nombre nuevo persiste y responde 201")
    void create_ok() {
        when(salaRepository.findByNombreIgnoreCase("Sala A")).thenReturn(Optional.empty());
        when(salaRepository.save(any(Sala.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<Object> resp = controller.create(req("Sala A"));

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        verify(salaRepository).save(any(Sala.class));
    }

    @Test
    @DisplayName("create con nombre duplicado lanza DuplicadoException")
    void create_duplicado() {
        when(salaRepository.findByNombreIgnoreCase("Sala A"))
                .thenReturn(Optional.of(sala(1, "Sala A", EstadoSala.DISPONIBLE)));
        assertThrows(DuplicadoException.class, () -> controller.create(req("Sala A")));
        verify(salaRepository, never()).save(any());
    }

    @Test
    @DisplayName("update inexistente responde 404")
    void update_notFound() {
        when(salaRepository.findById(9L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.update(9L, req("X")).getStatusCode());
    }

    @Test
    @DisplayName("update con nombre en uso por otra sala lanza DuplicadoException")
    void update_duplicado() {
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala(1, "Sala A", EstadoSala.DISPONIBLE)));
        when(salaRepository.existsByNombreIgnoreCaseAndIdNot("Sala B", 1L)).thenReturn(true);
        assertThrows(DuplicadoException.class, () -> controller.update(1L, req("Sala B")));
    }

    @Test
    @DisplayName("update válido modifica y responde 200")
    void update_ok() {
        Sala s = sala(1, "Sala A", EstadoSala.DISPONIBLE);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(s));
        when(salaRepository.existsByNombreIgnoreCaseAndIdNot("Sala B", 1L)).thenReturn(false);
        when(salaRepository.save(any(Sala.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<Object> resp = controller.update(1L, req("Sala B"));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Sala B", s.getNombre());
    }

    @Test
    @DisplayName("delete aplica borrado lógico (ELIMINADA) y responde 204")
    void delete_ok() {
        Sala s = sala(1, "Sala A", EstadoSala.DISPONIBLE);
        when(salaRepository.findById(1L)).thenReturn(Optional.of(s));

        ResponseEntity<Object> resp = controller.delete(1L, null);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        assertEquals(EstadoSala.ELIMINADA, s.getEstado());
        verify(salaRepository).save(s);
    }

    @Test
    @DisplayName("delete inexistente responde 404")
    void delete_notFound() {
        when(salaRepository.findById(9L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.delete(9L, null).getStatusCode());
    }

    @Test
    @DisplayName("panel calcula el estado efectivo (EN_USO / RESERVADA / DISPONIBLE) y ordena")
    void panel_estadoEfectivo() {
        Sala ocupada = sala(1, "Beta", EstadoSala.OCUPADA);
        Sala reservada = sala(2, "Alfa", EstadoSala.DISPONIBLE);
        Sala libre = sala(3, "Gamma", EstadoSala.DISPONIBLE);

        Reserva reservaHoy = Reserva.builder().sala(reservada).build();
        when(reservaRepository.findActivasHoy(any(LocalDate.class))).thenReturn(List.of(reservaHoy));
        when(salaRepository.findAll()).thenReturn(List.of(ocupada, reservada, libre));

        List<PanelSalaResponse> body = controller.panel().getBody();

        assertEquals(3, body.size());
        assertEquals("Alfa", body.get(0).nombre());
        assertEquals("RESERVADA", body.get(0).estadoPanel());
        assertEquals("EN_USO", body.get(1).estadoPanel());   // Beta / OCUPADA
        assertEquals("DISPONIBLE", body.get(2).estadoPanel()); // Gamma
    }
}

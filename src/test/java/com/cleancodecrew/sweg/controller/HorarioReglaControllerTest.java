package com.cleancodecrew.sweg.controller;

import com.cleancodecrew.sweg.dto.HorarioReglaRequest;
import com.cleancodecrew.sweg.model.HorarioRegla;
import com.cleancodecrew.sweg.model.TipoRegla;
import com.cleancodecrew.sweg.repository.HorarioReglaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas del HorarioReglaController (reglas de horario del ADMIN).
 */
class HorarioReglaControllerTest {

    private HorarioReglaRepository repo;
    private HorarioReglaController controller;

    @BeforeEach
    void setUp() {
        repo = mock(HorarioReglaRepository.class);
        controller = new HorarioReglaController(repo);
    }

    private HorarioReglaRequest req() {
        HorarioReglaRequest r = new HorarioReglaRequest();
        r.setTipo(TipoRegla.APERTURA);
        r.setHoraInicio(LocalTime.of(7, 0));
        r.setHoraFin(LocalTime.of(22, 0));
        r.setDiaSemana(null);
        r.setDescripcion("Horario comercial");
        r.setActivo(true);
        return r;
    }

    private HorarioRegla regla() {
        return HorarioRegla.builder().id(1L).tipo(TipoRegla.APERTURA)
                .horaInicio(LocalTime.of(7, 0)).horaFin(LocalTime.of(22, 0)).activo(true).build();
    }

    @Test
    @DisplayName("listar responde 200")
    void listar() {
        when(repo.findAll()).thenReturn(List.of(regla()));
        assertEquals(HttpStatus.OK, controller.listar().getStatusCode());
    }

    @Test
    @DisplayName("crear responde 201")
    void crear() {
        when(repo.save(any(HorarioRegla.class))).thenAnswer(i -> {
            HorarioRegla r = i.getArgument(0);
            r.setId(1L);
            return r;
        });
        assertEquals(HttpStatus.CREATED, controller.crear(req()).getStatusCode());
    }

    @Test
    @DisplayName("actualizar existente responde 200")
    void actualizar_ok() {
        when(repo.findById(1L)).thenReturn(Optional.of(regla()));
        when(repo.save(any(HorarioRegla.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals(HttpStatus.OK, controller.actualizar(1L, req()).getStatusCode());
    }

    @Test
    @DisplayName("actualizar inexistente responde 404")
    void actualizar_notFound() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.actualizar(9L, req()).getStatusCode());
    }

    @Test
    @DisplayName("eliminar existente responde 200")
    void eliminar_ok() {
        when(repo.existsById(1L)).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.eliminar(1L).getStatusCode());
        verify(repo).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar inexistente responde 404")
    void eliminar_notFound() {
        when(repo.existsById(9L)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, controller.eliminar(9L).getStatusCode());
    }
}

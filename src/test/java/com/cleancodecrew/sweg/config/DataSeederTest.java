package com.cleancodecrew.sweg.config;

import com.cleancodecrew.sweg.model.Usuario;
import com.cleancodecrew.sweg.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas del DataSeeder (siembra de usuarios demo). Repositorio y hasher mockeados.
 */
@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordHasher passwordHasher;
    @InjectMocks private DataSeeder seeder;

    @Test
    @DisplayName("Si ya existen usuarios, no vuelve a sembrar")
    void omiteSiHayUsuarios() {
        when(usuarioRepository.count()).thenReturn(3L);

        seeder.run();

        verify(usuarioRepository, never()).save(any());
        verify(passwordHasher, never()).hash(anyString());
    }

    @Test
    @DisplayName("Con la tabla vacía crea los 3 usuarios demo con contraseña hasheada")
    void siembraCuandoVacio() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(passwordHasher.hash("demo")).thenReturn("salt:digest");

        seeder.run();

        verify(usuarioRepository, times(3)).save(any(Usuario.class));
        verify(passwordHasher, times(3)).hash("demo");
    }
}

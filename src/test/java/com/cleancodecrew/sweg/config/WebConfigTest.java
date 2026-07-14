package com.cleancodecrew.sweg.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas de WebConfig: registro del AuthInterceptor sobre /api/** excluyendo /api/auth/**.
 */
@ExtendWith(MockitoExtension.class)
class WebConfigTest {

    @Mock private AuthInterceptor authInterceptor;
    @Mock private InterceptorRegistry registry;
    @Mock private InterceptorRegistration registration;

    @Test
    @DisplayName("Registra el interceptor en /api/** y excluye /api/auth/**")
    void registraInterceptor() {
        when(registry.addInterceptor(authInterceptor)).thenReturn(registration);
        when(registration.addPathPatterns(any(String[].class))).thenReturn(registration);

        new WebConfig(authInterceptor).addInterceptors(registry);

        verify(registry).addInterceptor(authInterceptor);
        verify(registration).addPathPatterns("/api/**");
        verify(registration).excludePathPatterns("/api/auth/**");
    }
}

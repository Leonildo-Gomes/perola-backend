package cv.perola.modules.business.usecase.service;

import cv.perola.core.exception.ConflictException;
import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.business.domain.ServiceEntity;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.mapper.ServiceMapper;
import cv.perola.modules.business.repository.ServiceRepository;
import cv.perola.modules.business.service.ServiceAssignmentLookupService;
import cv.perola.modules.business.validator.service.ServiceStatusValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToggleServiceStatusUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ServiceAssignmentLookupService serviceAssignmentLookupService;

    @Mock
    private ServiceMapper serviceMapper;

    @Mock
    private ServiceStatusValidator serviceStatusValidator;

    @InjectMocks
    private ToggleServiceStatusUseCase useCase;

    @Test
    @DisplayName("Deve alternar status do serviço de ativo (true) para inativo (false) sem validação de conflito")
    void shouldToggleFromActiveToInactive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .name("Corte Masculino")
                .description("Corte com máquina e tesoura")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(800))
                .isActive(true)
                .build();

        ServiceResponse expectedResponse = new ServiceResponse(
                serviceId, businessId, UUID.randomUUID(), "barbershop", "Barbearia",
                "Corte Masculino", "Corte com máquina e tesoura",
                30, BigDecimal.valueOf(800), false, List.of(), Instant.now(), Instant.now()
        );

        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(service));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceAssignmentLookupService.findAssignedEmployeeIds(businessId, serviceId)).thenReturn(List.of());
        when(serviceMapper.toResponse(any(ServiceEntity.class), anyList())).thenReturn(expectedResponse);

        // Act
        ServiceResponse response = useCase.execute(businessId, serviceId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isActive()).isFalse();
        assertThat(service.isActive()).isFalse();

        verify(serviceRepository).findByIdAndBusinessId(serviceId, businessId);
        verify(serviceStatusValidator, never()).validateActivation(any(), any());
        verify(serviceRepository).save(service);
    }

    @Test
    @DisplayName("Deve alternar status do serviço de inativo (false) para ativo (true) com validação de sucesso")
    void shouldToggleFromInactiveToActive() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .name("Corte Masculino")
                .description("Corte com máquina e tesoura")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(800))
                .isActive(false)
                .build();

        ServiceResponse expectedResponse = new ServiceResponse(
                serviceId, businessId, UUID.randomUUID(), "barbershop", "Barbearia",
                "Corte Masculino", "Corte com máquina e tesoura",
                30, BigDecimal.valueOf(800), true, List.of(), Instant.now(), Instant.now()
        );

        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(service));
        doNothing().when(serviceStatusValidator).validateActivation(businessId, service);
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceAssignmentLookupService.findAssignedEmployeeIds(businessId, serviceId)).thenReturn(List.of());
        when(serviceMapper.toResponse(any(ServiceEntity.class), anyList())).thenReturn(expectedResponse);

        // Act
        ServiceResponse response = useCase.execute(businessId, serviceId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isActive()).isTrue();
        assertThat(service.isActive()).isTrue();

        verify(serviceRepository).findByIdAndBusinessId(serviceId, businessId);
        verify(serviceStatusValidator).validateActivation(businessId, service);
        verify(serviceRepository).save(service);
    }

    @Test
    @DisplayName("Deve lançar ConflictException e não alterar status quando houver colisão de nome ativo ao reativar")
    void shouldThrowConflictWhenReactivatingCollidingServiceName() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        ServiceEntity service = ServiceEntity.builder()
                .businessId(businessId)
                .name("Corte Masculino")
                .description("Corte com máquina e tesoura")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(800))
                .isActive(false)
                .build();

        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.of(service));
        doThrow(new ConflictException("Já existe um serviço ativo com o nome 'Corte Masculino' neste salão."))
                .when(serviceStatusValidator).validateActivation(businessId, service);

        // Act & Assert
        assertThrows(ConflictException.class, () -> useCase.execute(businessId, serviceId));

        verify(serviceRepository).findByIdAndBusinessId(serviceId, businessId);
        verify(serviceStatusValidator).validateActivation(businessId, service);
        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o serviço não for encontrado no salão")
    void shouldThrowWhenServiceNotFound() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        when(serviceRepository.findByIdAndBusinessId(serviceId, businessId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(businessId, serviceId));

        verify(serviceRepository).findByIdAndBusinessId(serviceId, businessId);
    }
}


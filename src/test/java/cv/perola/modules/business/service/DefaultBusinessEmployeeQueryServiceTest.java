package cv.perola.modules.business.service;

import cv.perola.modules.business.domain.EmployeeEntity;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBusinessEmployeeQueryServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private DefaultBusinessEmployeeQueryService employeeQueryService;

    @Test
    @DisplayName("Deve retornar EmployeeResponse quando colaborador encontrado por ID")
    void shouldReturnEmployeeResponseWhenEmployeeFoundById() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        EmployeeEntity entity = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Carlos")
                .isActive(true)
                .build();
        EmployeeResponse expectedResponse = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Carlos", true, Instant.now(), Instant.now()
        );
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(entity));
        when(employeeMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        Optional<EmployeeResponse> result = employeeQueryService.findEmployeeById(employeeId);

        // Assert
        assertThat(result).isPresent().contains(expectedResponse);
        verify(employeeRepository).findById(employeeId);
    }

    @Test
    @DisplayName("Deve retornar vazio quando colaborador não encontrado por ID")
    void shouldReturnEmptyWhenEmployeeNotFoundById() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        // Act
        Optional<EmployeeResponse> result = employeeQueryService.findEmployeeById(employeeId);

        // Assert
        assertThat(result).isEmpty();
        verify(employeeRepository).findById(employeeId);
    }

    @Test
    @DisplayName("Deve retornar EmployeeResponse quando colaborador encontrado por ID e businessId")
    void shouldReturnEmployeeResponseWhenFoundByIdAndBusinessId() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        EmployeeEntity entity = EmployeeEntity.builder()
                .businessId(businessId)
                .displayName("Ana")
                .isActive(true)
                .build();
        EmployeeResponse expectedResponse = new EmployeeResponse(
                employeeId, businessId, UUID.randomUUID(), "Ana", true, Instant.now(), Instant.now()
        );
        when(employeeRepository.findByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.of(entity));
        when(employeeMapper.toResponse(entity)).thenReturn(expectedResponse);

        // Act
        Optional<EmployeeResponse> result = employeeQueryService.findEmployeeByIdAndBusinessId(employeeId, businessId);

        // Assert
        assertThat(result).isPresent().contains(expectedResponse);
        verify(employeeRepository).findByIdAndBusinessId(employeeId, businessId);
    }
}

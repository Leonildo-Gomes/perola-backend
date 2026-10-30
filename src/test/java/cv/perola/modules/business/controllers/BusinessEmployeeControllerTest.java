package cv.perola.modules.business.controllers;

import cv.perola.modules.business.dto.AssignEmployeeServicesRequest;
import cv.perola.modules.business.dto.CreateEmployeeRequest;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.dto.EmployeeServicesResponse;
import cv.perola.modules.business.dto.ServiceResponse;
import cv.perola.modules.business.dto.UpdateEmployeeRequest;
import cv.perola.modules.business.usecase.employee.AssignEmployeeServicesUseCase;
import cv.perola.modules.business.usecase.employee.CreateEmployeeUseCase;
import cv.perola.modules.business.usecase.employee.GetEmployeeServicesUseCase;
import cv.perola.modules.business.usecase.employee.ListEmployeesUseCase;
import cv.perola.modules.business.usecase.employee.UpdateEmployeeUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessEmployeeControllerTest {

    @Mock
    private CreateEmployeeUseCase createEmployeeUseCase;

    @Mock
    private ListEmployeesUseCase listEmployeesUseCase;

    @Mock
    private UpdateEmployeeUseCase updateEmployeeUseCase;

    @Mock
    private AssignEmployeeServicesUseCase assignEmployeeServicesUseCase;

    @Mock
    private GetEmployeeServicesUseCase getEmployeeServicesUseCase;

    @InjectMocks
    private BusinessEmployeeController controller;

    @Test
    @DisplayName("Deve chamar CreateEmployeeUseCase e retornar HTTP 201 Created")
    void shouldCreateEmployeeAndReturn201() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(null, "Maria Silva");
        EmployeeResponse expected = new EmployeeResponse(
                UUID.randomUUID(), businessId, null, "Maria Silva", true, Instant.now(), Instant.now()
        );
        when(createEmployeeUseCase.execute(businessId, request)).thenReturn(expected);

        // Act
        ResponseEntity<EmployeeResponse> response = controller.createEmployee(businessId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(createEmployeeUseCase).execute(businessId, request);
    }

    @Test
    @DisplayName("Deve chamar ListEmployeesUseCase e retornar HTTP 200 OK com lista de colaboradores")
    void shouldListEmployeesAndReturn200() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        EmployeeResponse e1 = new EmployeeResponse(
                UUID.randomUUID(), businessId, null, "Maria Silva", true, Instant.now(), Instant.now()
        );
        when(listEmployeesUseCase.execute(businessId)).thenReturn(List.of(e1));

        // Act
        ResponseEntity<List<EmployeeResponse>> response = controller.listEmployees(businessId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(listEmployeesUseCase).execute(businessId);
    }

    @Test
    @DisplayName("Deve chamar UpdateEmployeeUseCase e retornar HTTP 200 OK")
    void shouldUpdateEmployeeAndReturn200() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UpdateEmployeeRequest request = new UpdateEmployeeRequest("Maria Silva - Estilista", true);
        EmployeeResponse expected = new EmployeeResponse(
                employeeId, businessId, null, "Maria Silva - Estilista", true, Instant.now(), Instant.now()
        );
        when(updateEmployeeUseCase.execute(businessId, employeeId, request)).thenReturn(expected);

        // Act
        ResponseEntity<EmployeeResponse> response = controller.updateEmployee(businessId, employeeId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(updateEmployeeUseCase).execute(businessId, employeeId, request);
    }

    @Test
    @DisplayName("Deve chamar GetEmployeeServicesUseCase e retornar HTTP 200 OK")
    void shouldGetEmployeeServicesAndReturn200() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeServicesResponse expected = new EmployeeServicesResponse(employeeId, businessId, List.of());
        when(getEmployeeServicesUseCase.execute(businessId, employeeId)).thenReturn(expected);

        // Act
        ResponseEntity<EmployeeServicesResponse> response = controller.getEmployeeServices(businessId, employeeId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(getEmployeeServicesUseCase).execute(businessId, employeeId);
    }

    @Test
    @DisplayName("Deve chamar AssignEmployeeServicesUseCase e retornar HTTP 200 OK")
    void shouldAssignServicesAndReturn200() {
        // Arrange
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        AssignEmployeeServicesRequest request = new AssignEmployeeServicesRequest(List.of(serviceId));
        ServiceResponse s = new ServiceResponse(
                serviceId, businessId, "Corte", null, 30, new BigDecimal("1000.00"), true, Instant.now(), Instant.now()
        );
        EmployeeServicesResponse expected = new EmployeeServicesResponse(employeeId, businessId, List.of(s));
        when(assignEmployeeServicesUseCase.execute(businessId, employeeId, request)).thenReturn(expected);

        // Act
        ResponseEntity<EmployeeServicesResponse> response = controller.assignServices(businessId, employeeId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(assignEmployeeServicesUseCase).execute(businessId, employeeId, request);
    }
}

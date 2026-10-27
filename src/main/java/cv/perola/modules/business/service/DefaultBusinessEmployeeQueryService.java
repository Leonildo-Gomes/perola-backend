package cv.perola.modules.business.service;

import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultBusinessEmployeeQueryService implements BusinessEmployeeQueryService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    public Optional<EmployeeResponse> findEmployeeById(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employeeMapper::toResponse);
    }

    @Override
    public Optional<EmployeeResponse> findEmployeeByIdAndBusinessId(UUID employeeId, UUID businessId) {
        return employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                .map(employeeMapper::toResponse);
    }
}

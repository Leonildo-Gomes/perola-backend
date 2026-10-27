package cv.perola.modules.business.usecase;

import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.mapper.EmployeeMapper;
import cv.perola.modules.business.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListEmployeesUseCase {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Transactional(readOnly = true)
    public List<EmployeeResponse> execute(UUID businessId) {
        return employeeRepository.findAllByBusinessIdAndIsActiveTrue(businessId)
                .stream()
                .map(employeeMapper::toResponse)
                .toList();
    }
}

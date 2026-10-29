package cv.perola.modules.schedule.usecase;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.core.exception.TenantAccessDeniedException;
import cv.perola.core.exception.ValidationException;
import cv.perola.core.security.TenantContext;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.business.dto.EmployeeResponse;
import cv.perola.modules.business.service.BusinessEmployeeQueryService;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.schedule.domain.EmployeeScheduleBreakEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleDayEntity;
import cv.perola.modules.schedule.domain.EmployeeScheduleEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
import cv.perola.modules.schedule.dto.BreakItemRequest;
import cv.perola.modules.schedule.dto.EmployeeScheduleResponse;
import cv.perola.modules.schedule.dto.SaveEmployeeScheduleRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemRequest;
import cv.perola.modules.schedule.dto.ScheduleDayItemResponse;
import cv.perola.modules.schedule.mapper.ScheduleMapper;
import cv.perola.modules.schedule.repository.EmployeeScheduleBreakRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleDayRepository;
import cv.perola.modules.schedule.repository.EmployeeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateEmployeeScheduleUseCase {

    private final EmployeeScheduleRepository scheduleRepository;
    private final EmployeeScheduleDayRepository scheduleDayRepository;
    private final EmployeeScheduleBreakRepository breakRepository;
    private final BusinessEmployeeQueryService employeeQueryService;
    private final ScheduleMapper scheduleMapper;

    @Transactional
    public EmployeeScheduleResponse execute(UUID employeeId, SaveEmployeeScheduleRequest request,
            UserPrincipal principal) {
        UUID businessId = TenantContext.requireTenantId();

        EmployeeResponse employee = employeeQueryService.findEmployeeById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));

        if (!businessId.equals(employee.businessId())) {
            throw new TenantAccessDeniedException("O colaborador especificado não pertence ao salão ativo.");
        }

        validatePermissions(employee, principal, businessId);
        validateScheduleDaysAndBreaks(request.days());

        Optional<EmployeeScheduleEntity> currentDefault = scheduleRepository
                .findByEmployeeIdAndIsDefaultTrueAndIsActiveTrue(employeeId);

        boolean isActive = request.resolvedIsActive();
        boolean isDefault = isActive
                && (request.resolvedIsDefault() || (request.isDefault() == null && currentDefault.isEmpty()));

        if (isDefault && currentDefault.isPresent()) {
            EmployeeScheduleEntity prevDefault = currentDefault.get();
            prevDefault.setDefault(false);
            scheduleRepository.saveAndFlush(prevDefault);
        }

        EmployeeScheduleEntity scheduleEntity = scheduleMapper.toEntity(employeeId, request, isDefault);
        scheduleEntity = scheduleRepository.save(scheduleEntity);

        List<ScheduleDayItemResponse> dayResponses = new ArrayList<>();

        for (ScheduleDayItemRequest dayReq : request.days()) {
            EmployeeScheduleDayEntity dayEntity = scheduleMapper.toEntity(scheduleEntity.getId(), dayReq);
            dayEntity = scheduleDayRepository.save(dayEntity);

            List<EmployeeScheduleBreakEntity> savedBreaks = new ArrayList<>();
            if (dayEntity.isWorking() && dayReq.breaks() != null && !dayReq.breaks().isEmpty()) {
                for (BreakItemRequest breakReq : dayReq.breaks()) {
                    EmployeeScheduleBreakEntity breakEntity = scheduleMapper.toEntity(dayEntity.getId(), breakReq);
                    savedBreaks.add(breakRepository.save(breakEntity));
                }
            }

            dayResponses.add(scheduleMapper.toResponse(dayEntity, savedBreaks));
        }

        dayResponses.sort(Comparator.comparing(ScheduleDayItemResponse::dayOfWeek));

        return scheduleMapper.toResponse(scheduleEntity, dayResponses);
    }

    private void validatePermissions(EmployeeResponse employee, UserPrincipal principal, UUID businessId) {
        boolean isOwner = principal.getMemberships().stream()
                .anyMatch(m -> businessId.equals(m.businessId()) && UserRole.OWNER.name().equalsIgnoreCase(m.role()));

        if (isOwner) {
            return;
        }

        boolean isOwnEmployee = employee.userId() != null && employee.userId().equals(principal.getId());
        if (!isOwnEmployee) {
            throw new TenantAccessDeniedException("O colaborador só pode gerir o seu próprio expediente.");
        }
    }

    static void validateScheduleDaysAndBreaks(List<ScheduleDayItemRequest> days) {
        if (days == null || days.size() != 7) {
            throw new ValidationException(
                    "O perfil de horário deve conter exatamente os 7 dias da semana (MONDAY a SUNDAY).");
        }

        Set<DayOfWeek> seenDays = EnumSet.noneOf(DayOfWeek.class);

        for (ScheduleDayItemRequest day : days) {
            if (day.dayOfWeek() == null) {
                throw new ValidationException("O dia da semana é obrigatório em todos os itens.");
            }

            if (!seenDays.add(day.dayOfWeek())) {
                throw new ValidationException(
                        "O dia da semana " + day.dayOfWeek() + " foi configurado mais de uma vez.");
            }

            boolean isWorking = day.resolvedIsWorking();

            if (!isWorking) {
                if (day.startTime() != null || day.endTime() != null) {
                    throw new ValidationException("O dia " + day.dayOfWeek()
                            + " está marcado como folga (isWorking=false) e não deve possuir horários de início ou término.");
                }
                if (day.breaks() != null && !day.breaks().isEmpty()) {
                    throw new ValidationException("O dia " + day.dayOfWeek()
                            + " está marcado como folga (isWorking=false) e não pode conter pausas.");
                }
                continue;
            }

            if (day.startTime() == null || day.endTime() == null) {
                throw new ValidationException("Os horários de início e término são obrigatórios para o dia de trabalho "
                        + day.dayOfWeek() + ".");
            }

            if (!day.endTime().isAfter(day.startTime())) {
                throw new ValidationException("O horário de término (" + day.endTime()
                        + ") deve ser posterior ao horário de início (" + day.startTime() + ") para " + day.dayOfWeek()
                        + ".");
            }

            if (day.breaks() != null && !day.breaks().isEmpty()) {
                List<BreakItemRequest> sortedBreaks = new ArrayList<>(day.breaks());
                sortedBreaks.sort(Comparator.comparing(BreakItemRequest::startTime));

                for (BreakItemRequest b : sortedBreaks) {
                    if (!b.endTime().isAfter(b.startTime())) {
                        throw new ValidationException("O horário de término da pausa '" + b.title()
                                + "' deve ser posterior ao horário de início.");
                    }

                    if (b.startTime().isBefore(day.startTime()) || b.endTime().isAfter(day.endTime())) {
                        throw new ValidationException(
                                "A pausa '" + b.title() + "' (" + b.startTime() + " - " + b.endTime()
                                        + ") deve estar totalmente contida no turno (" + day.startTime() + " - "
                                        + day.endTime() + ").");
                    }
                }

                for (int i = 0; i < sortedBreaks.size() - 1; i++) {
                    BreakItemRequest current = sortedBreaks.get(i);
                    BreakItemRequest next = sortedBreaks.get(i + 1);

                    if (current.endTime().isAfter(next.startTime())) {
                        throw new ValidationException("As pausas '" + current.title() + "' e '" + next.title()
                                + "' sobrepõem-se no dia " + day.dayOfWeek() + ".");
                    }
                }
            }
        }
    }
}

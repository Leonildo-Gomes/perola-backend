package cv.perola.modules.identity.service;

import cv.perola.core.exception.ResourceNotFoundException;
import cv.perola.modules.identity.domain.CustomerEntity;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.repository.CustomerRepository;
import cv.perola.modules.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCustomerResolutionService implements CustomerResolutionService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CustomerEntity resolveForBooking(String name, String email, String phone, UUID authenticatedUserId) {
        if ((email == null || email.isBlank()) && authenticatedUserId != null) {
            return resolveForAuthenticatedUser(authenticatedUserId);
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O email do cliente é obrigatório para resolver a identificação.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<CustomerEntity> existingOpt = customerRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingOpt.isPresent()) {
            CustomerEntity existing = existingOpt.get();
            if (existing.getUserId() != null) {
                // Conta registada vinculada
                if (authenticatedUserId != null && authenticatedUserId.equals(existing.getUserId())) {
                    // O utilizador autenticado é o próprio titular da conta
                    if (name != null && !name.isBlank()) {
                        existing.setName(name.trim());
                    }
                    if (phone != null && !phone.isBlank()) {
                        existing.setPhone(phone.trim());
                    }
                    return customerRepository.save(existing);
                }
                // Regra Anti-Spoofing: chamada não autenticada ou por terceiro não sobrescreve nome/telefone da conta registada
                log.info("Cliente registado identificado por email '{}' preservado sem sobrescrita de perfil (Anti-Spoofing)", normalizedEmail);
                return existing;
            } else {
                // Visitante sem conta registada (userId == null)
                if (authenticatedUserId != null) {
                    existing.setUserId(authenticatedUserId);
                }
                if (name != null && !name.isBlank()) {
                    existing.setName(name.trim());
                }
                if (phone != null && !phone.isBlank()) {
                    existing.setPhone(phone.trim());
                }
                return customerRepository.save(existing);
            }
        }

        // Criar novo registo de Customer
        CustomerEntity newCustomer = CustomerEntity.builder()
                .userId(authenticatedUserId)
                .name(name != null ? name.trim() : "")
                .email(normalizedEmail)
                .phone(phone != null ? phone.trim() : "")
                .build();

        return customerRepository.save(newCustomer);
    }

    @Override
    @Transactional
    public CustomerEntity resolveForAuthenticatedUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("O ID do utilizador autenticado é obrigatório.");
        }

        return customerRepository.findByUserId(userId).orElseGet(() -> {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado: " + userId));

            String normalizedEmail = user.getEmail().trim().toLowerCase();
            return customerRepository.findByEmailIgnoreCase(normalizedEmail)
                    .map(existing -> {
                        existing.setUserId(userId);
                        if (existing.getName() == null || existing.getName().isBlank()) {
                            existing.setName(user.getName());
                        }
                        if ((existing.getPhone() == null || existing.getPhone().isBlank()) && user.getPhone() != null) {
                            existing.setPhone(user.getPhone());
                        }
                        return customerRepository.save(existing);
                    })
                    .orElseGet(() -> {
                        CustomerEntity customer = CustomerEntity.builder()
                                .userId(userId)
                                .name(user.getName())
                                .email(normalizedEmail)
                                .phone(user.getPhone() != null ? user.getPhone() : "")
                                .build();
                        customer.setId(userId);
                        return customerRepository.save(customer);
                    });
        });
    }

    @Override
    @Transactional
    public CustomerEntity linkOrRegisterUser(UUID userId, String name, String email, String phone) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email é obrigatório para vincular utilizador a cliente.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<CustomerEntity> existingOpt = customerRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingOpt.isPresent()) {
            CustomerEntity existing = existingOpt.get();
            existing.setUserId(userId);
            if (name != null && !name.isBlank()) {
                existing.setName(name.trim());
            }
            if (phone != null && !phone.isBlank()) {
                existing.setPhone(phone.trim());
            }
            return customerRepository.save(existing);
        }

        CustomerEntity customer = CustomerEntity.builder()
                .userId(userId)
                .name(name != null ? name.trim() : "")
                .email(normalizedEmail)
                .phone(phone != null ? phone.trim() : "")
                .build();
        customer.setId(userId);

        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerEntity> findById(UUID customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        return customerRepository.findById(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerEntity> findByUserId(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return customerRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerEntity> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return customerRepository.findByEmailIgnoreCase(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, CustomerSummary> findCustomerSummariesByIds(Collection<UUID> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(
                        CustomerEntity::getId,
                        c -> new CustomerSummary(
                                c.getId(),
                                c.getUserId(),
                                c.getName(),
                                c.getEmail(),
                                c.getPhone()
                        )
                ));
    }
}

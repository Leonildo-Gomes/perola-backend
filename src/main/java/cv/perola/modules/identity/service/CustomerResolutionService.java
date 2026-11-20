package cv.perola.modules.identity.service;

import cv.perola.modules.identity.domain.CustomerEntity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço público do módulo Identity para resolução e unificação de clientes
 * (visitantes sem conta e utilizadores registados).
 */
public interface CustomerResolutionService {

    /**
     * Resolve ou cria o registo de Customer para um agendamento.
     * Aplica unificação por email lowercase e regra anti-spoofing
     * (não sobrescreve name/phone se o cliente já possuir conta registada).
     */
    CustomerEntity resolveForBooking(String name, String email, String phone, UUID authenticatedUserId);

    /**
     * Resolve o Customer associado ao utilizador autenticado pelo seu ID de utilizador.
     */
    CustomerEntity resolveForAuthenticatedUser(UUID userId);

    /**
     * Vincula um utilizador recém-registado ao seu perfil de Customer (criando ou atualizando userId).
     */
    CustomerEntity linkOrRegisterUser(UUID userId, String name, String email, String phone);

    Optional<CustomerEntity> findById(UUID customerId);

    Optional<CustomerEntity> findByUserId(UUID userId);

    Optional<CustomerEntity> findByEmail(String email);

    Map<UUID, CustomerSummary> findCustomerSummariesByIds(Collection<UUID> customerIds);

    java.util.List<UUID> findCustomerIdsMatching(String search);
}

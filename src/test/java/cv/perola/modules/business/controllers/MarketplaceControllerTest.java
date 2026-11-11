package cv.perola.modules.business.controllers;

import cv.perola.core.shared.PageResponse;
import cv.perola.modules.business.dto.BusinessCategoryResponse;
import cv.perola.modules.business.dto.CategoryResponse;
import cv.perola.modules.business.dto.MarketplaceBusinessDetailResponse;
import cv.perola.modules.business.dto.MarketplaceBusinessSummaryResponse;
import cv.perola.modules.business.dto.MarketplaceServiceResponse;
import cv.perola.modules.business.usecase.business.GetMarketplaceBusinessDetailUseCase;
import cv.perola.modules.business.usecase.business.SearchMarketplaceBusinessesUseCase;
import cv.perola.modules.business.usecase.category.ListMarketplaceCategoriesUseCase;
import cv.perola.modules.business.usecase.service.SearchMarketplaceServicesUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceControllerTest {

        @Mock
        private ListMarketplaceCategoriesUseCase listMarketplaceCategoriesUseCase;

        @Mock
        private SearchMarketplaceBusinessesUseCase searchMarketplaceBusinessesUseCase;

        @Mock
        private GetMarketplaceBusinessDetailUseCase getMarketplaceBusinessDetailUseCase;

        @Mock
        private SearchMarketplaceServicesUseCase searchMarketplaceServicesUseCase;

        @InjectMocks
        private MarketplaceController controller;

        @Test
        @DisplayName("Deve listar categorias ativas e retornar HTTP 200 OK")
        void shouldListCategoriesAndReturn200() {
                // Arrange
                CategoryResponse c1 = new CategoryResponse(UUID.randomUUID(), "barbershop", "Barbearia", "Scissors", 1);
                when(listMarketplaceCategoriesUseCase.execute()).thenReturn(List.of(c1));

                // Act
                ResponseEntity<List<CategoryResponse>> response = controller.listCategories();

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsExactly(c1);
                verify(listMarketplaceCategoriesUseCase).execute();
        }

        @Test
        @DisplayName("Deve pesquisar salões com filtro de categoria e retornar HTTP 200 OK")
        void shouldSearchBusinessesAndReturn200() {
                // Arrange
                BusinessCategoryResponse cat = new BusinessCategoryResponse(
                                UUID.randomUUID(), "barbershop", "Barbearia", "Scissors", true);
                MarketplaceBusinessSummaryResponse summary = new MarketplaceBusinessSummaryResponse(
                                UUID.randomUUID(), "Salão Teste", "Santiago", "Praia", "Centro",
                                "+2382600000", "Atlantic/Cape_Verde", List.of(cat), 4);
                PageResponse<MarketplaceBusinessSummaryResponse> pageResponse = new PageResponse<>(
                                List.of(summary), 0, 20, 1, 1);
                when(searchMarketplaceBusinessesUseCase.execute(eq("Teste"), eq("barbershop"), eq("Santiago"),
                                eq("Praia"), any(Pageable.class)))
                                .thenReturn(pageResponse);

                // Act
                ResponseEntity<PageResponse<MarketplaceBusinessSummaryResponse>> response = controller.searchBusinesses(
                                "Teste", null, "barbershop", "Santiago", "Praia", 0, 20);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(pageResponse);
                verify(searchMarketplaceBusinessesUseCase).execute(eq("Teste"), eq("barbershop"), eq("Santiago"),
                                eq("Praia"), any(Pageable.class));
        }

        @Test
        @DisplayName("Deve buscar detalhes públicos do salão e retornar HTTP 200 OK")
        void shouldGetBusinessDetailAndReturn200() {
                // Arrange
                UUID businessId = UUID.randomUUID();
                MarketplaceBusinessDetailResponse detail = new MarketplaceBusinessDetailResponse(
                                businessId, "Salão Detalhe", "Santiago", "Praia", "Platô",
                                "+2382611111", "Atlantic/Cape_Verde", List.of(), List.of(), List.of());
                when(getMarketplaceBusinessDetailUseCase.execute(businessId)).thenReturn(detail);

                // Act
                ResponseEntity<MarketplaceBusinessDetailResponse> response = controller.getBusinessDetail(businessId);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(detail);
                verify(getMarketplaceBusinessDetailUseCase).execute(businessId);
        }

        @Test
        @DisplayName("Deve pesquisar serviços públicos com filtro de categoria e retornar HTTP 200 OK")
        void shouldSearchServicesAndReturn200() {
                // Arrange
                MarketplaceServiceResponse service = new MarketplaceServiceResponse(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "nails",
                                "Unhas (Manicure & Pedicure)",
                                "Salão Sal", "Sal", "Santa Maria", "Manicure", "Desc", 45, new BigDecimal("1500.00"));
                PageResponse<MarketplaceServiceResponse> pageResponse = new PageResponse<>(
                                List.of(service), 0, 20, 1, 1);
                when(searchMarketplaceServicesUseCase.execute(eq("Manicure"), eq("nails"), eq("Sal"), eq("Santa Maria"),
                                any(Pageable.class)))
                                .thenReturn(pageResponse);

                // Act
                ResponseEntity<PageResponse<MarketplaceServiceResponse>> response = controller.searchServices(
                                "Manicure", null, "nails", "Sal", "Santa Maria", 0, 20);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(pageResponse);
                verify(searchMarketplaceServicesUseCase).execute(eq("Manicure"), eq("nails"), eq("Sal"),
                                eq("Santa Maria"), any(Pageable.class));
        }

}
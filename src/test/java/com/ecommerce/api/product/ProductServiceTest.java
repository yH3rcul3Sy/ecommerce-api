package com.ecommerce.api.product;

import com.ecommerce.api.exception.BusinessException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.product.dto.ProductRequest;
import com.ecommerce.api.product.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Teclado Mecanico")
                .description("RGB")
                .price(new BigDecimal("250.00"))
                .stockQuantity(10)
                .sku("TEC-001")
                .build();
    }

    @Test
    void deveCriarProdutoQuandoSkuNaoExiste() {
        ProductRequest request = new ProductRequest("Teclado Mecanico", "RGB", new BigDecimal("250.00"), 10, "TEC-001");
        when(productRepository.existsBySku("TEC-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("TEC-001");
    }

    @Test
    void naoDeveCriarProdutoComSkuDuplicado() {
        ProductRequest request = new ProductRequest("Teclado Mecanico", "RGB", new BigDecimal("250.00"), 10, "TEC-001");
        when(productRepository.existsBySku("TEC-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU");

        verify(productRepository, never()).save(any());
    }

    @Test
    void devePermitirCriarProdutoSemSku() {
        ProductRequest request = new ProductRequest("Mouse", "Sem fio", new BigDecimal("99.90"), 5, null);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(request);

        assertThat(response.name()).isEqualTo("Mouse");
        verify(productRepository, never()).existsBySku(any());
    }

    @Test
    void deveLancarExcecaoAoBuscarProdutoInexistente() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveListarTodosOsProdutos() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).price()).isEqualByComparingTo("250.00");
    }
}

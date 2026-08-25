package com.ecommerce.api.order;

import com.ecommerce.api.customer.Customer;
import com.ecommerce.api.customer.CustomerRepository;
import com.ecommerce.api.exception.BusinessException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.order.dto.OrderItemRequest;
import com.ecommerce.api.order.dto.OrderRequest;
import com.ecommerce.api.order.dto.OrderResponse;
import com.ecommerce.api.product.Product;
import com.ecommerce.api.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Joao").email("joao@teste.com").build();
        product = Product.builder().id(1L).name("Teclado").price(new BigDecimal("250.00")).stockQuantity(10).build();
    }

    @Test
    void deveCriarPedidoECalcularTotalCorretamente() {
        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(1L, 2)));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.create(request);

        assertThat(response.totalAmount()).isEqualByComparingTo("500.00");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void deveDarBaixaNoEstoqueAoCriarPedido() {
        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(1L, 3)));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.create(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getStockQuantity()).isEqualTo(7); // 10 - 3
    }

    @Test
    void naoDeveCriarPedidoComEstoqueInsuficiente() {
        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(1L, 999)));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Estoque insuficiente");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarPedidoParaClienteInexistente() {
        OrderRequest request = new OrderRequest(99L, List.of(new OrderItemRequest(1L, 1)));
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void naoDeveCriarPedidoParaProdutoInexistente() {
        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(99L, 1)));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void naoDevePermitirAlterarStatusDePedidoJaEntregue() {
        Order order = Order.builder().id(1L).customer(customer).status(OrderStatus.DELIVERED).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.SHIPPED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void devePermitirAvancarStatusDePedidoPendente() {
        Order order = Order.builder().id(1L).customer(customer).status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateStatus(1L, OrderStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    }
}

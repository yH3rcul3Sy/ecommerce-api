package com.ecommerce.api.payment;

import com.ecommerce.api.customer.Customer;
import com.ecommerce.api.exception.BusinessException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.order.Order;
import com.ecommerce.api.order.OrderRepository;
import com.ecommerce.api.order.OrderStatus;
import com.ecommerce.api.payment.dto.PaymentRequest;
import com.ecommerce.api.payment.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order order;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder().id(1L).name("Joao").email("joao@teste.com").build();
        order = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("500.00"))
                .build();
    }

    @Test
    void pagamentoViaPixDeveSerAprovadoImediatamenteEConfirmarPedido() {
        PaymentRequest request = new PaymentRequest(1L, PaymentMethod.PIX);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.create(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.amount()).isEqualByComparingTo("500.00");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void pagamentoViaBoletoDeveFicarPendenteENaoConfirmarPedidoImediatamente() {
        PaymentRequest request = new PaymentRequest(1L, PaymentMethod.BOLETO);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.create(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void naoDevePermitirPagamentoDuplicadoParaOMesmoPedido() {
        PaymentRequest request = new PaymentRequest(1L, PaymentMethod.PIX);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ja possui um pagamento");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void naoDevePermitirPagamentoDePedidoCancelado() {
        order.setStatus(OrderStatus.CANCELED);
        PaymentRequest request = new PaymentRequest(1L, PaymentMethod.PIX);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelado");
    }

    @Test
    void deveLancarExcecaoParaPedidoInexistente() {
        PaymentRequest request = new PaymentRequest(99L, PaymentMethod.PIX);
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

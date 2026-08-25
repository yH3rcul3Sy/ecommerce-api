package com.ecommerce.api.payment;

import com.ecommerce.api.exception.BusinessException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.order.Order;
import com.ecommerce.api.order.OrderRepository;
import com.ecommerce.api.order.OrderStatus;
import com.ecommerce.api.payment.dto.PaymentRequest;
import com.ecommerce.api.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll().stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(Long id) {
        return PaymentResponse.from(getPaymentOrThrow(id));
    }

    /**
     * Processa o pagamento de um pedido.
     * Simulacao de gateway: BOLETO fica PENDING (compensacao leva dias),
     * os demais metodos sao aprovados imediatamente. O valor cobrado vem
     * do proprio pedido (nunca do cliente), evitando manipulacao do valor.
     */
    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido nao encontrado com id: " + request.orderId()));

        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new BusinessException("Este pedido ja possui um pagamento registrado");
        }

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new BusinessException("Nao e possivel pagar um pedido cancelado");
        }

        PaymentStatus status = request.method() == PaymentMethod.BOLETO
                ? PaymentStatus.PENDING
                : PaymentStatus.APPROVED;

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(request.method())
                .status(status)
                .build();

        Payment saved = paymentRepository.save(payment);

        if (status == PaymentStatus.APPROVED) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }

        return PaymentResponse.from(saved);
    }

    Payment getPaymentOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento nao encontrado com id: " + id));
    }
}

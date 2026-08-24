package com.ecommerce.api.payment.dto;

import com.ecommerce.api.payment.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull(message = "Id do pedido e obrigatorio") Long orderId,

        @NotNull(message = "Metodo de pagamento e obrigatorio") PaymentMethod method
) {
}

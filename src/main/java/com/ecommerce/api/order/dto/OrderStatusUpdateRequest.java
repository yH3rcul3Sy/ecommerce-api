package com.ecommerce.api.order.dto;

import com.ecommerce.api.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "Status e obrigatorio") OrderStatus status
) {
}

package com.ecommerce.api.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Nome e obrigatorio") String name,

        String description,

        @NotNull(message = "Preco e obrigatorio")
        @DecimalMin(value = "0.01", message = "Preco deve ser maior que zero") BigDecimal price,

        @NotNull(message = "Quantidade em estoque e obrigatoria")
        @PositiveOrZero(message = "Estoque nao pode ser negativo") Integer stockQuantity,

        String sku
) {
}

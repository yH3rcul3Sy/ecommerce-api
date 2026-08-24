package com.ecommerce.api.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank(message = "Nome e obrigatorio") String name,

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido") String email,

        String phone,

        String address
) {
}

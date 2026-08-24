package com.ecommerce.api.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Formato padrao de resposta de erro devolvido pela API. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
}

package com.ecommerce.api.exception;

/** Lancada quando uma regra de negocio e violada (ex: estoque insuficiente, email duplicado). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

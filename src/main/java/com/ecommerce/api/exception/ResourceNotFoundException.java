package com.ecommerce.api.exception;

/** Lancada quando um recurso (produto, cliente, pedido...) nao e encontrado pelo id informado. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

package com.ecommerce.api.user;

/**
 * Papeis (permissoes) que um usuario autenticado pode ter.
 * ADMIN: pode gerenciar produtos, clientes, pedidos e pagamentos.
 * USER: acesso basico autenticado (pode ser expandido conforme a regra de negocio).
 */
public enum Role {
    ADMIN,
    USER
}

package com.ecommerce.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao do Swagger / OpenAPI.
 * Depois de rodar a aplicacao, a documentacao interativa fica disponivel em:
 * http://localhost:8080/swagger-ui.html
 *
 * O botao "Authorize" na tela do Swagger permite colar o token JWT
 * (sem o prefixo "Bearer ") para testar as rotas protegidas direto pelo navegador.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "E-commerce API",
                version = "1.0.0",
                description = "API REST para gerenciamento de um e-commerce: produtos, clientes, pedidos e pagamentos, com autenticacao JWT.",
                contact = @Contact(name = "Projeto de Portfolio")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}

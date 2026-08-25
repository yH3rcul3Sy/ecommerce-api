package com.ecommerce.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa o fluxo completo da API de ponta a ponta (registro -> login -> cliente
 * -> produto -> pedido -> pagamento), do mesmo jeito que seria testado
 * manualmente com curl/Postman, so que automatizado e rodando contra um
 * banco H2 em memoria (nao usa o MySQL real).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EcommerceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fluxoCompletoDeCompraDeveFuncionarDePontaAPonta() throws Exception {
        // 1. Registrar usuario e obter token JWT
        String registerBody = """
                {"name":"Ana Teste","email":"ana.integration@teste.com","password":"senha123"}
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("token").asText();
        String authHeader = "Bearer " + token;

        // 2. Sem token, rota protegida deve retornar 403
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isForbidden());

        // 3. Cadastrar cliente
        String customerBody = """
                {"name":"Joao Comprador","email":"joao.integration@teste.com","phone":"11999999999","address":"Rua A, 123"}
                """;
        MvcResult customerResult = mockMvc.perform(post("/api/customers")
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content(customerBody))
                .andExpect(status().isCreated())
                .andReturn();
        long customerId = objectMapper.readTree(customerResult.getResponse().getContentAsString()).get("id").asLong();

        // 4. Cadastrar produto com 10 unidades em estoque
        String productBody = """
                {"name":"Teclado Mecanico","description":"RGB","price":250.00,"stockQuantity":10,"sku":"TEC-IT-001"}
                """;
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content(productBody))
                .andExpect(status().isCreated())
                .andReturn();
        long productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asLong();

        // 5. Criar pedido com 2 unidades - total deve ser 500.00
        String orderBody = """
                {"customerId": %d, "items": [{"productId": %d, "quantity": 2}]}
                """.formatted(customerId, productId);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content(orderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount", is(500.00)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn();
        long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        // 6. Estoque do produto deve ter sido reduzido para 8
        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", authHeader))
                .andExpect(jsonPath("$.stockQuantity", is(8)));

        // 7. Pagar o pedido via PIX -> aprovado imediatamente
        String paymentBody = """
                {"orderId": %d, "method": "PIX"}
                """.formatted(orderId);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content(paymentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // 8. Pedido deve estar CONFIRMED apos o pagamento
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", authHeader))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // 9. Nao deve permitir pagar o mesmo pedido duas vezes
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content(paymentBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void naoDevePermitirPedidoComEstoqueInsuficiente() throws Exception {
        String token = registrarERetornarToken("estoque.teste@teste.com");
        String authHeader = "Bearer " + token;

        long customerId = criarCliente(authHeader, "cliente.estoque@teste.com");
        long productId = criarProduto(authHeader, "PROD-ESTOQUE-01", 1); // so 1 em estoque

        String orderBody = """
                {"customerId": %d, "items": [{"productId": %d, "quantity": 5}]}
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content(orderBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Estoque insuficiente")));
    }

    @Test
    void naoDeveAcessarRotaAdminSemPermissaoDeAdmin() throws Exception {
        String token = registrarERetornarToken("usuariocomum@teste.com");
        String authHeader = "Bearer " + token;
        long customerId = criarCliente(authHeader, "para.deletar@teste.com");

        // Usuario comum (role USER) tentando deletar -> rota exige ROLE_ADMIN
        mockMvc.perform(delete("/api/customers/" + customerId)
                        .header("Authorization", authHeader))
                .andExpect(status().isForbidden());
    }

    private String registrarERetornarToken(String email) throws Exception {
        String body = """
                {"name":"Usuario Teste","email":"%s","password":"senha123"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long criarCliente(String authHeader, String email) throws Exception {
        String body = """
                {"name":"Cliente Teste","email":"%s","phone":"11999999999","address":"Rua Teste"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/customers")
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long criarProduto(String authHeader, String sku, int stock) throws Exception {
        String body = """
                {"name":"Produto Teste","description":"desc","price":10.00,"stockQuantity":%d,"sku":"%s"}
                """.formatted(stock, sku);
        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}

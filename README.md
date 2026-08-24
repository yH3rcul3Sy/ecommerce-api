# E-commerce API

API REST para gerenciamento de um e-commerce, construída com **Java + Spring Boot**. Projeto de portfólio focado em demonstrar boas práticas de arquitetura backend: autenticação, persistência, validação, tratamento de erros e documentação.

## Funcionalidades

- **Autenticação JWT** — registro e login de usuários, rotas protegidas por token
- **Clientes** — CRUD completo
- **Produtos** — CRUD completo, com controle de estoque
- **Pedidos** — criação com múltiplos itens, baixa automática de estoque, cálculo de total, atualização de status
- **Pagamentos** — processamento de pagamento vinculado a um pedido (simulação de gateway)
- **Documentação interativa** via Swagger/OpenAPI
- **Tratamento global de erros** com respostas padronizadas

## Stack

| Camada          | Tecnologia                          |
|-----------------|--------------------------------------|
| Linguagem       | Java 21                              |
| Framework       | Spring Boot 3.3.4                    |
| Segurança       | Spring Security + JWT (JJWT)         |
| Persistência    | Spring Data JPA (Hibernate)          |
| Banco de dados  | MySQL 8                              |
| Documentação    | springdoc-openapi (Swagger UI)       |
| Build           | Maven                                |
| Containerização | Docker / Docker Compose              |

## Arquitetura

Organizado por **módulo de domínio** (não por camada técnica), o que facilita navegar o código à medida que o projeto cresce:

```
com.ecommerce.api
├── auth        -> registro/login, geração de JWT
├── security    -> filtro JWT, geração/validação de token
├── user        -> conta de usuário (autenticação)
├── customer    -> cadastro de clientes
├── product     -> cadastro de produtos
├── order       -> pedidos e itens de pedido
├── payment     -> pagamentos
├── config      -> Spring Security, Swagger
└── exception   -> tratamento global de erros
```

Cada módulo segue o padrão `Controller -> Service -> Repository -> Entity`, com DTOs de entrada/saída para nunca expor as entidades JPA diretamente na API.

## Como rodar localmente

### Pré-requisitos
- Java 21 (JDK)
- Maven (ou use o `mvnw` incluso)
- MySQL 8 rodando localmente **ou** Docker

### Opção 1: MySQL local

1. Garanta que o MySQL está rodando e você sabe a senha do usuário usado.
2. Defina as variáveis de ambiente (ajuste conforme seu ambiente):

```bash
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="ecommerce_db"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="sua_senha"
```

3. Rode a aplicação:

```bash
mvn spring-boot:run
```

O banco `ecommerce_db` é criado automaticamente na primeira execução (`createDatabaseIfNotExist=true`), e as tabelas são geradas pelo Hibernate a partir das entidades.

### Opção 2: Docker Compose (recomendado — não depende do MySQL instalado na máquina)

```bash
docker compose up --build
```

Isso sobe um container MySQL isolado + a API, já conectados entre si.

### Acessando a API

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Fluxo de uso (exemplo)

```bash
# 1. Registrar um usuário
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Ana Silva","email":"ana@email.com","password":"senha123"}'

# Resposta contém um "token". Use-o nas próximas chamadas:

# 2. Cadastrar um cliente
curl -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"João Comprador","email":"joao@email.com","phone":"11999999999","address":"Rua A, 123"}'

# 3. Cadastrar um produto
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Teclado Mecanico","description":"RGB","price":250.00,"stockQuantity":10,"sku":"TEC-001"}'

# 4. Criar um pedido
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"items":[{"productId":1,"quantity":2}]}'

# 5. Pagar o pedido
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"method":"PIX"}'
```

Todos os endpoints (exceto `/api/auth/**` e o Swagger) exigem o header `Authorization: Bearer <token>`.

## Deploy na AWS

Veja o guia completo em [`DEPLOY.md`](./DEPLOY.md).

## Próximos passos (ideias de evolução)

- Testes automatizados (unitários com JUnit/Mockito, integração com Testcontainers)
- Paginação e filtros nas listagens
- Migrations com Flyway em vez de `ddl-auto: update`
- Perfis de usuário mais granulares (ex: cliente só vê os próprios pedidos)
- CI/CD com GitHub Actions

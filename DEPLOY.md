# Deploy na AWS

Este guia cobre o caminho mais simples e mais indicado para portfólio: **Elastic Beanstalk** (roda o `.jar`) + **RDS** (MySQL gerenciado). No final há uma seção com a alternativa via **ECS Fargate + Docker**, mais avançada.

> Importante: criar esses recursos na AWS pode gerar custos. Use o **free tier** (RDS `db.t3.micro`/`db.t4g.micro` e EB em instância `t3.micro` costumam entrar no free tier de contas novas), e **destrua os recursos** (`Delete environment` / `Delete DB`) assim que terminar de demonstrar o projeto, para não ser cobrado.

## Pré-requisitos

- Conta AWS (você mesmo cria e configura — isso não é algo que eu faço por você)
- AWS CLI instalado e configurado (`aws configure`) com suas próprias credenciais
- O projeto já compilando localmente (`mvn clean package` gera `target/ecommerce-api-0.0.1-SNAPSHOT.jar`)

## Passo 1 — Criar o banco de dados (RDS MySQL)

1. Console AWS → **RDS** → **Create database**
2. Engine: **MySQL** (versão 8.0)
3. Template: **Free tier**
4. DB instance identifier: `ecommerce-db`
5. Master username: `admin` (ou de sua escolha) + defina uma senha forte
6. Em "Connectivity": **Public access: Yes** (só para o app conseguir conectar; em um cenário mais realista, o app e o banco ficariam na mesma VPC privada)
7. Crie um **Security Group** que libere a porta `3306` apenas para o IP do seu Elastic Beanstalk (ajustamos isso no passo 3)
8. Após criado, anote o **endpoint** (algo como `ecommerce-db.xxxxx.us-east-1.rds.amazonaws.com`)

## Passo 2 — Gerar o pacote da aplicação

```bash
mvn clean package -DskipTests
```

Isso gera `target/ecommerce-api-0.0.1-SNAPSHOT.jar`.

## Passo 3 — Criar o ambiente no Elastic Beanstalk

1. Console AWS → **Elastic Beanstalk** → **Create application**
2. Application name: `ecommerce-api`
3. Platform: **Java** (Corretto 21)
4. Upload do `.jar` gerado no passo 2
5. Em **Configuration → Software → Environment properties**, adicione as mesmas variáveis do `application.yml`:

   | Nome                | Valor                                      |
   |---------------------|---------------------------------------------|
   | `DB_HOST`           | endpoint do RDS (passo 1)                   |
   | `DB_PORT`           | `3306`                                       |
   | `DB_NAME`           | `ecommerce_db`                               |
   | `DB_USERNAME`       | usuário do RDS                              |
   | `DB_PASSWORD`       | senha do RDS                                |
   | `JWT_SECRET`        | uma string base64 aleatória (gere uma nova, não reutilize a de desenvolvimento) |
   | `SERVER_PORT`       | `5000` (Elastic Beanstalk espera essa porta por padrão para o proxy Nginx) |

6. Em **Configuration → Instances → Security groups**, libere no Security Group do RDS a entrada vinda do Security Group do Elastic Beanstalk na porta `3306`
7. Clique em **Create environment** e aguarde o deploy

## Passo 4 — Validar

- Acesse a URL pública gerada pelo Elastic Beanstalk (ex: `ecommerce-api.us-east-1.elasticbeanstalk.com`)
- Teste `GET /swagger-ui.html` e o fluxo de `/api/auth/register`

## Passo 5 — Encerrar os recursos (evitar cobrança)

- Elastic Beanstalk → **Actions → Terminate environment**
- RDS → **Delete** (desmarque snapshot final se não precisar)

---

## Alternativa: ECS Fargate + Docker (mais avançado)

Para quem quiser mostrar conhecimento de containers no currículo:

1. `docker build -t ecommerce-api .`
2. Crie um repositório no **ECR** e faça push da imagem:
   ```bash
   aws ecr create-repository --repository-name ecommerce-api
   aws ecr get-login-password | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com
   docker tag ecommerce-api:latest <account-id>.dkr.ecr.<region>.amazonaws.com/ecommerce-api:latest
   docker push <account-id>.dkr.ecr.<region>.amazonaws.com/ecommerce-api:latest
   ```
3. Crie um cluster **ECS Fargate**, uma **Task Definition** apontando para a imagem no ECR (mesmas variáveis de ambiente da tabela acima)
4. Crie um **Service** no cluster, associado a um **Application Load Balancer**
5. O RDS segue igual ao passo 1 acima

Essa via é mais próxima do que empresas usam em produção, mas tem mais peças móveis — vale fazer depois de já ter o Elastic Beanstalk funcionando.

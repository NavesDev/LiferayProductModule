# Product REST API — E2E Test Suite

Suite de testes end-to-end em Python para validar os endpoints do módulo `product-rest` rodando em um Liferay 7.4.

---

## Pré-requisitos

- Python 3.10+
- Liferay 7.4 rodando em `localhost:8080`
- Módulo `product-rest` deployado (`blade gw :modules:product-rest:product-rest-impl:deploy`)
- Credenciais padrão `test@liferay.com / test` com acesso ao site **Guest**

---

## Instalação

```bash
cd e2e
pip install -r requirements.txt
```

---

## Execução

```bash
# Todos os testes
pytest -v

# Por marcador
pytest -v -m crud
pytest -v -m filters
pytest -v -m associations
pytest -v -m validation

# Com relatório HTML
pytest -v --html=report.html

# Liferay em URL diferente
LIFERAY_URL=http://localhost:9090 pytest -v
```

---

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `LIFERAY_URL` | `http://localhost:8080` | URL base do servidor Liferay |
| `LIFERAY_USER` | `test@liferay.com` | Usuário para autenticação |
| `LIFERAY_PASSWORD` | `test` | Senha do usuário |
| `LIFERAY_SITE_ID` | auto-detectado | ID do site Guest (detectado via API) |

---

## Estrutura

```
e2e/
├── conftest.py                       # Fixtures globais (api, site_id, product_factory)
├── requirements.txt                  # pytest, requests, pytest-html
├── pytest.ini                        # Configuração e marcadores
├── helpers/
│   └── api_client.py                 # Wrapper HTTP com autenticação
└── tests/
    ├── test_products_crud.py         # CRUD básico
    ├── test_products_filters.py      # Filtros, ordenação e paginação
    ├── test_products_associations.py # Endpoints de categories e tags
    └── test_products_validation.py   # Defaults e tratamento de erros
```

---

## Cobertura de Testes

### CRUD (`-m crud`) — 8 testes

| ID | Cenário |
|---|---|
| E2E-C01a | POST retorna id > 0 |
| E2E-C01b | POST retorna campos iguais ao payload |
| E2E-C02 | GET por id retorna produto correto |
| E2E-C03 | GET lista contém produto criado |
| E2E-C04a | PUT atualiza nome |
| E2E-C04b | PUT atualiza preço |
| E2E-C05 | DELETE retorna 204 |
| E2E-C06 | GET após DELETE retorna 4xx |

### Filtros (`-m filters`) — 8 testes

| ID | Cenário |
|---|---|
| E2E-F01 | `?search=` retorna apenas correspondentes |
| E2E-F02 | `?status=published` retorna apenas published |
| E2E-F03 | `?status=draft` retorna apenas draft |
| E2E-F04 | `?inStock=true` retorna apenas com estoque |
| E2E-F05 | `?inStock=false` retorna apenas sem estoque |
| E2E-F06 | `?sort=name` retorna em ordem alfabética |
| E2E-F07 | `?sort=price` retorna em ordem crescente de preço |
| E2E-F08 | `?page=1&pageSize=2` retorna 2 itens |

### Associações (`-m associations`) — 4 testes

| ID | Cenário |
|---|---|
| E2E-A01 | PUT `/categories` com lista vazia zera categoryIds |
| E2E-A02 | PUT `/tags` com lista vazia zera tagIds |
| E2E-A03 | PUT `/categories` com payload nulo retorna 400 |
| E2E-A04 | PUT `/tags` com payload nulo retorna 400 |

### Validação (`-m validation`) — 8 testes

| ID | Cenário |
|---|---|
| E2E-V01a | Produto criado sem `price` usa default 0.0 |
| E2E-V01b | Produto criado sem `stockQuantity` usa default 0 |
| E2E-V01c | Produto criado sem `status` usa default "draft" |
| E2E-V02 | POST com body nulo retorna 400 |
| E2E-V03 | PUT com body nulo retorna 400 |
| E2E-V04 | POST com siteId inválido retorna 4xx |
| E2E-V05 | GET com productId inválido retorna 4xx |
| E2E-V06 | GET de produto com siteId errado retorna 4xx |

---

## Bugs Conhecidos

Os testes abaixo falham devido a bugs identificados na implementação:

| Teste | Falha | Causa |
|---|---|---|
| F02, F03 | `500 Internal Server Error` ao criar produto | `POST` com `status: "published"` causa erro no servidor |
| F04, F05 | 0 resultados para `?inStock=true/false` | Depende do filtro `search` que está com bug |
| F08 | 0 resultados para `?page=1&pageSize=2` | Idem |
| F06, F07 | Falso positivo | Passam mesmo retornando 0 resultados (`[] == sorted([])`) |

**Causa raiz do filtro `search`:** `StringUtil.containsIgnoreCase` em Liferay usa separador vírgula (token matching), não substring. Busca por `"ZTEST"` em `"Produto ZTEST"` retorna falso. Somente busca pelo nome exato funciona.

**Correção pendente em:** `ProductResourceImpl.java:421` — substituir `StringUtil.containsIgnoreCase` por `product.getName().toLowerCase().contains(normalizedSearch)`.

---

## Padrões Utilizados

- **AAA** (Arrange / Act / Assert) em todos os testes
- **Given-When-Then** no nome dos métodos: `dado_X__quando_Y__entao_Z`
- **Fixtures com yield** para garantir cleanup após cada teste, mesmo em caso de falha
- **UUID por teste** nos filtros para isolar dados entre execuções paralelas
- **One assert per test** nos cenários de default e status

---

## API Reference

Base URL: `http://localhost:8080/o/product-rest/v1.0`

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/sites/{siteId}/products` | Listar produtos |
| `POST` | `/sites/{siteId}/products` | Criar produto |
| `GET` | `/sites/{siteId}/products/{productId}` | Buscar produto |
| `PUT` | `/sites/{siteId}/products/{productId}` | Atualizar produto |
| `DELETE` | `/sites/{siteId}/products/{productId}` | Deletar produto |
| `PUT` | `/sites/{siteId}/products/{productId}/categories` | Atualizar categorias |
| `PUT` | `/sites/{siteId}/products/{productId}/tags` | Atualizar tags |

# TDD - Servico de Catalogo de Produtos com REST API

**Status:** Proposto  
**Data:** 2026-05-09  
**Autor:** Codex  
**Modulo base:** `modules/product-service`  
**Contexto tecnico:** Liferay 7.4 / OSGi / Service Builder / REST API

## 1. Contexto

O workspace ja possui um modulo `product-service` criado com Liferay Service Builder e uma entidade inicial `Product` com os campos `name`, `description` e `price`. O modulo ainda nao cobre as capacidades esperadas para um catalogo de produtos utilizavel por canais internos ou externos via REST API.

A necessidade atual e evoluir esse modulo para um servico de catalogo que suporte:

- CRUD de produtos
- Gestao de categorias
- Gestao de tags
- Controle de estoque
- Controle de status do produto (`draft`, `publicado`, `inativo`)
- Busca de produtos com filtros

## 2. Definicao do Problema

Hoje o modelo existente e insuficiente para atender um fluxo de catalogo real. Faltam:

- relacionamento entre produto e classificacoes de negocio
- representacao formal de estoque
- controle de ciclo de vida do produto
- contratos REST para integracao
- mecanismos de busca com filtros e paginacao

Sem isso, o modulo nao atende cenarios basicos como publicar apenas produtos validos, listar produtos disponiveis por categoria, ou filtrar itens com estoque positivo.

## 3. Objetivos

- Disponibilizar uma API REST consistente para gestao e consulta de catalogo
- Manter a regra de negocio centralizada no service layer do Liferay
- Permitir evolucao futura para indexacao e busca mais avancada sem quebrar contrato
- Garantir separacao clara entre modelo transacional, API e estrategia de busca

## 4. Escopo

### Em escopo

- CRUD de produtos
- CRUD de categorias
- CRUD de tags
- Atualizacao e consulta de estoque
- Status de produto com regras de transicao basicas
- Busca paginada de produtos com filtros
- Validacoes funcionais minimas
- API REST versionada

### Fora de escopo

- checkout, carrinho ou pedidos
- precificacao promocional
- multiestoque por centro de distribuicao
- imagens e midias de produto
- workflow editorial complexo
- importacao em massa

## 5. Estado Atual

O modulo atual possui:

- entidade `Product`
- campos: `productId`, `groupId`, auditoria Liferay, `name`, `description`, `price`
- local service e remote service gerados pelo Service Builder

A proposta abaixo expande o dominio sem descartar a base existente.

## 6. Solucao Tecnica

### 6.1 Visao de arquitetura

O servico sera organizado em quatro camadas:

1. **Persistencia e regras transacionais**
   via Liferay Service Builder para entidades e relacionamentos.
2. **Camada de negocio**
   implementando validacoes, regras de status, consistencia de estoque e montagem de filtros.
3. **Camada REST**
   expondo contratos HTTP/JSON para consumo externo.
4. **Busca**
   inicialmente por banco de dados e finders customizados; com possibilidade de evolucao posterior para indexacao.

### 6.2 Componentes propostos

- `product-service-api`
  contratos Java internos do dominio
- `product-service-service`
  entidades, persistence, local services e regras de negocio
- `product-service-rest`
  recursos REST para produtos, categorias, tags e busca

### 6.3 Modelo de dominio proposto

#### Produto

Representa o item principal do catalogo.

Campos recomendados:

- `productId`
- `groupId`, `companyId`, `userId`, `userName`, `createDate`, `modifiedDate`
- `name`
- `description`
- `price`
- `status` (`DRAFT`, `PUBLISHED`, `INACTIVE`)
- `stockQuantity`

Observacao:
O campo de estoque pode nascer no proprio produto para manter a primeira entrega simples. Se houver expectativa de movimentacoes, reserva ou multiplos depositos, o caminho recomendado e separar o estoque em entidade propria.

#### Categoria

Representa uma classificacao navegacional do produto.

Campos recomendados:

- `categoryId`
- `groupId` e auditoria padrao
- `name`
- `description`
- `active`

Relacionamento recomendado:

- `Product` N:N `Category`

#### Tag

Representa uma classificacao livre para busca e agrupamento.

Campos recomendados:

- `tagId`
- `groupId` e auditoria padrao
- `name`

Relacionamento recomendado:

- `Product` N:N `Tag`

### 6.4 Regras de negocio

- Produto em `draft` pode ser criado e alterado livremente.
- Produto em `publicado` deve estar apto para exposicao em busca publica.
- Produto em `inativo` nao deve aparecer em listagens publicas padrao.
- Produto nao deve ser publicado sem `name`, `description`, `price` valido e categorias minimas configuradas.
- Estoque nao pode ser negativo.
- Busca publica deve retornar por padrao apenas produtos `publicado`.
- A API administrativa pode consultar qualquer status.

### 6.5 Maquina de estados

Transicoes permitidas:

- `draft -> publicado`
- `draft -> inativo`
- `publicado -> inativo`
- `inativo -> draft`
- `inativo -> publicado`

Transicoes a validar:

- `draft -> publicado` exige dados obrigatorios e consistencia de estoque conforme regra de negocio definida.

## 7. Contrato REST Proposto

Base path sugerido:

`/o/product-catalog/v1.0`

### 7.1 Produtos

#### Criar produto

`POST /products`

Request:

```json
{
  "name": "Notebook Pro 14",
  "description": "Notebook para uso corporativo",
  "price": 8999.90,
  "status": "draft",
  "stockQuantity": 15,
  "categoryIds": [10, 11],
  "tagIds": [100, 101]
}
```

#### Buscar produto por ID

`GET /products/{productId}`

#### Atualizar produto

`PUT /products/{productId}`

#### Remover produto

`DELETE /products/{productId}`

#### Listar e buscar produtos

`GET /products`

Query params sugeridos:

- `search`: busca textual por nome e descricao
- `status`
- `categoryId`
- `tagId`
- `inStock`: `true` ou `false`
- `page`
- `pageSize`
- `sort`

Exemplo:

`GET /products?search=notebook&status=publicado&categoryId=10&inStock=true&page=1&pageSize=20&sort=name:asc`

### 7.2 Categorias

- `POST /categories`
- `GET /categories/{categoryId}`
- `PUT /categories/{categoryId}`
- `DELETE /categories/{categoryId}`
- `GET /categories`

### 7.3 Tags

- `POST /tags`
- `GET /tags/{tagId}`
- `PUT /tags/{tagId}`
- `DELETE /tags/{tagId}`
- `GET /tags`

### 7.4 Estoque

Para a primeira versao, o estoque pode ser atualizado como parte do produto.

Opcao recomendada para evolucao:

- `PATCH /products/{productId}/inventory`

Request:

```json
{
  "stockQuantity": 20
}
```

Se o time quiser rastreabilidade de movimentacao ja na primeira entrega, o endpoint de estoque deve ser separado desde o inicio.

## 8. Busca de Produtos

### 8.1 Requisitos funcionais

- permitir busca textual por nome e descricao
- filtrar por categoria
- filtrar por tag
- filtrar por status
- filtrar por disponibilidade de estoque
- suportar paginacao e ordenacao

### 8.2 Estrategia inicial

Fase 1:

- consulta relacional com filtros dinamicos
- uso de `DynamicQuery`, `DSLQuery` ou finder customizado
- ordenacao por `name`, `createDate`, `modifiedDate`, `price`

Fase 2:

- indexacao no mecanismo de busca do Liferay para cenarios de maior volume
- suporte a relevancia textual e filtros compostos mais performaticos

## 9. Modelo de Dados

### 9.1 Tabelas esperadas

- `PRODUCT_Product`
- `PRODUCT_Category`
- `PRODUCT_Tag`
- tabela de associacao `PRODUCT_Products_Categories`
- tabela de associacao `PRODUCT_Products_Tags`

### 9.2 Decisoes de modelagem

- `status` deve ser persistido como `int` ou `String` conforme convencao do modulo, mas o contrato REST deve expor valores semanticos estaveis.
- `stockQuantity` deve ser numerico inteiro.
- categorias e tags devem ser entidades explicitas; nao apenas campos texto no produto.
- relacionamentos N:N evitam retrabalho caso um produto precise estar em multiplas categorias.

## 10. Validacoes

- `name` obrigatorio e com tamanho maximo definido
- `description` obrigatoria para publicacao
- `price` obrigatorio e maior ou igual a zero
- `status` deve pertencer ao enum suportado
- `stockQuantity` maior ou igual a zero
- IDs de categorias e tags devem existir antes do vinculo

## 11. Seguranca

- endpoints administrativos exigem autenticacao e permissao apropriada
- leitura publica, se necessaria, deve expor apenas produtos `publicado`
- operacoes de escrita devem registrar auditoria via campos padrao do Liferay
- validacoes de escopo por `groupId` devem impedir acesso cruzado entre sites

## 12. Estrategia de Testes

- testes unitarios para regras de status e validacoes
- testes de integracao para persistence e relacionamentos
- testes de contrato REST para CRUD e busca
- cenarios obrigatorios:
  - publicar produto valido
  - bloquear publicacao de produto invalido
  - buscar apenas publicados em endpoint publico
  - filtrar por categoria, tag e estoque

## 13. Monitoramento e Observabilidade

- logs estruturados para falhas de validacao e erros de integracao
- contadores de operacoes de CRUD e buscas
- medicao de latencia dos endpoints de listagem e busca
- rastreio de erros 4xx e 5xx por recurso REST

## 14. Riscos

- modelar estoque diretamente em `Product` pode limitar evolucao futura
- busca relacional pode degradar com alto volume se nao houver indexacao
- definicao tardia de permissao pode gerar retrabalho na camada REST
- ausencia de regra clara para publicacao pode gerar inconsistencias funcionais

## 15. Plano de Implementacao

### Fase 1 - Dominio

- expandir `service.xml` com campos de status e estoque
- criar entidades `Category` e `Tag`
- criar relacionamentos entre produto, categoria e tag
- gerar codigo via Service Builder

### Fase 2 - Regras de negocio

- implementar validacoes de produto
- implementar transicoes de status
- implementar servicos de associacao de categorias e tags
- implementar regras de filtro e consulta

### Fase 3 - REST API

- criar modulo REST
- expor endpoints de produtos, categorias e tags
- expor endpoint de busca com paginacao e filtros
- mapear erros de negocio para respostas HTTP coerentes

### Fase 4 - Qualidade

- criar testes unitarios e de integracao
- validar performance de busca
- revisar permissoes e observabilidade

## 16. Critérios de Aceite

- e possivel criar, consultar, atualizar e remover produtos
- e possivel criar, consultar, atualizar e remover categorias
- e possivel criar, consultar, atualizar e remover tags
- e possivel alterar e consultar estoque
- e possivel alterar status entre `draft`, `publicado` e `inativo`
- a busca retorna resultados paginados e filtrados
- produtos `inativo` nao aparecem em listagem publica padrao

## 17. Questoes em Aberto

- `Category` deve ser hierarquica ou plana na primeira versao?
- `price` continuara como `double` ou deve migrar para representacao monetaria mais segura?
- a primeira versao precisa de endpoint publico e endpoint administrativo separados?
- o estoque sera apenas saldo atual ou precisaremos de historico de movimentacao?
- produto precisa de `sku` ja na primeira entrega?

## 18. Recomendacoes

- manter a V1 simples com estoque no produto, mas documentar a possibilidade de extracao futura
- tratar `status` como regra de negocio central desde o inicio
- separar claramente API administrativa de busca publica, mesmo que compartilhem o mesmo backend
- considerar `sku` e `BigDecimal` como melhoria recomendada antes de entrar em producao

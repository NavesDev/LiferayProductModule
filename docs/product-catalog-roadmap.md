# Roadmap - Catalogo de Produtos

**Status:** Proposto  
**Data:** 2026-05-09  
**Base documental:** `docs/product-catalog-service-design.md` e estado atual de `modules/product-service`

## 1. Resumo Executivo

O workspace ja possui a fundacao tecnica de um modulo Liferay com Service Builder e a entidade inicial `Product`. Porem, o codigo atual ainda esta em estagio basico: existe apenas o modelo inicial com `name`, `description` e `price`, sem regras de negocio implementadas, sem contratos REST e sem suporte a associacoes com categorias e tags nativas do Liferay, estoque, status ou busca filtrada.

Com base nisso, o roadmap recomendado e evolutivo, com foco em entregar o desafio em camadas:

1. consolidar o dominio transacional
2. expor operacoes administrativas e de consulta via REST
3. habilitar busca e filtros
4. garantir cobertura de testes e validacao final

## 2. Estado Atual

### Ja existe

- workspace Liferay 7.4 configurado
- modulo `product-service` criado com Service Builder
- entidade `Product` com auditoria Liferay
- campos atuais: `name`, `description`, `price`
- servicos local e remoto gerados
- teste de persistencia gerado automaticamente

### Ainda nao existe

- CRUD funcional de produto com regras de negocio
- integracao com categorias nativas do Liferay
- integracao com tags nativas do Liferay
- estoque
- status de produto
- API REST versionada
- busca com filtros e paginacao
- validacoes de publicacao
- testes de negocio e contrato

## 3. Direcionadores do Roadmap

- Priorizar um MVP funcional do desafio antes de refinamentos secundarios.
- Concentrar regras no service layer para evitar logica dispersa.
- Separar claramente o modelo transacional da exposicao REST.
- Manter a implementacao simples o suficiente para caber bem no desafio tecnico.

## 4. Roadmap Proposto

## Fase 0 - Fundacao do Dominio

**Objetivo:** sair do esqueleto gerado para um modulo com modelo preparado para catalogo.

### Entregas

- expandir `service.xml` para incluir:
  - `status`
  - `stockQuantity`
- manter `Product` como unica entidade do modulo
- definir integracao com `AssetCategory` e `AssetTag`
- regenerar Service Builder
- implementar servicos locais com validacoes basicas
- definir enums ou constantes de status
- garantir migracao inicial consistente do banco

### Criterio de saida

- produto pode ser criado e atualizado com modelo novo
- produto consegue referenciar categorias e tags nativas de forma consistente
- estoque nao aceita valor negativo

## Fase 1 - MVP Administrativo de Produtos

**Objetivo:** permitir operacao interna do catalogo com regras minimas.

### Entregas

- CRUD de produtos no service layer
- associacao produto-categoria nativa
- associacao produto-tag nativa
- regras de transicao de status:
  - `draft -> published`
  - `draft -> inactive`
  - `published -> inactive`
  - `inactive -> draft`
  - `inactive -> published`
- validacoes para publicacao:
  - `name` obrigatorio
  - `description` obrigatoria
  - `price` valido
  - categorias minimas configuradas

### Criterio de saida

- equipe interna consegue cadastrar e manter produtos com classificacao e status
- publicacao bloqueia produtos invalidos

## Fase 2 - API REST v1.0

**Objetivo:** expor o catalogo para integracoes internas e externas.

### Entregas

- novo modulo `product-service-rest`
- base path `/o/product-catalog/v1.0`
- endpoints:
  - `POST /products`
  - `GET /products/{productId}`
  - `PUT /products/{productId}`
  - `DELETE /products/{productId}`
  - `GET /products`
  - `PUT /products/{productId}/categories`
  - `PUT /products/{productId}/tags`
- DTOs e mapeamentos desacoplados do modelo Service Builder
- tratamento padrao de erros e validacoes
- paginacao e ordenacao basicas

### Criterio de saida

- consumidores externos conseguem criar, consultar e listar produtos via API
- contrato REST fica estavel para integracao inicial

## Fase 3 - Busca e Consulta de Catalogo

**Objetivo:** tornar o catalogo pesquisavel e utilizavel por canais de consulta.

### Entregas

- filtros em `GET /products`:
  - `search`
  - `status`
  - `categoryId`
  - `tagId`
  - `inStock`
  - `page`
  - `pageSize`
  - `sort`
- listagem publica retornando por padrao apenas produtos `published`
- finders customizados e consultas otimizadas por banco
- definicao clara entre consulta publica e consulta administrativa

### Criterio de saida

- catalogo pode ser consumido por interface administrativa ou canal externo com filtros reais
- consulta publica nao expoe produtos inativos ou rascunhos por padrao

## Fase 4 - Garantia de Testes

**Objetivo:** validar o desafio com cobertura adequada dos fluxos principais.

### Entregas

- testes unitarios para regras de negocio
- testes de integracao para services e persistencia
- testes de contrato da API REST
- cenarios cobrindo:
  - criacao e atualizacao de produto
  - transicoes de status
  - validacoes de publicacao
  - filtros principais de consulta

### Criterio de saida

- fluxo principal coberto por testes
- comportamento esperado validado de ponta a ponta no escopo do desafio

## 5. Priorizacao Recomendada

### Prioridade Alta

- Fase 0
- Fase 1
- Fase 2

### Prioridade Media

- Fase 3
- Fase 4

## 6. Sequencia de Entrega Sugerida

### Onda 1

- remodelagem do dominio
- regeneracao Service Builder
- regras basicas de validacao e status

### Onda 2

- CRUD administrativo completo
- associacoes com categorias e tags nativas
- cobertura minima de testes de negocio

### Onda 3

- API REST v1.0
- DTOs
- erros e paginacao

### Onda 4

- busca filtrada
- consolidacao da suite de testes

## 7. Riscos e Dependencias

### Riscos

- crescimento precoce demais do escopo para alem do MVP
- acoplamento excessivo entre DTO REST e entidades geradas
- ausencia de testes atrasar evolucao segura

### Dependencias

- definicao funcional exata das regras de publicacao
- decisao sobre obrigatoriedade minima de categorias nativas
- decisao sobre quais filtros sao obrigatorios no escopo final do desafio

## 8. Indicadores de Sucesso

- tempo para cadastrar e publicar um produto completo
- percentual de produtos publicados com dados validos
- cobertura de testes das regras de negocio centrais
- sucesso dos fluxos principais nos testes de integracao

## 9. Proximo Passo Imediato

O proximo passo recomendado e iniciar a **Fase 0**, atualizando o `service.xml` para refletir o dominio proposto e, em seguida, regenerar o Service Builder. Sem essa base, as fases seguintes ficam dependentes de retrabalho estrutural.

# Liferay Product Module

Workspace Liferay baseado em `portal-7.4-ga132` com um módulo Service Builder chamado `product-service`.

## Visão geral

Este repositório contém:

- Um workspace Liferay gerenciado com `blade` e plugin `com.liferay.workspace`
- Um módulo `modules/product-service`
- Um serviço gerado com Service Builder para a entidade `Product`

A entidade `Product` possui os seguintes campos:

- `productId`
- `groupId`
- `companyId`
- `userId`
- `userName`
- `createDate`
- `modifiedDate`
- `name`
- `description`
- `price`

## Estrutura principal

- [modules/product-service](modules/product-service)
- [modules/product-service/product-service-service/service.xml](modules/product-service/product-service-service/service.xml)
- [.workspace-rules/liferay-rules.md](.workspace-rules/liferay-rules.md)
- [.workspace-rules/initial-setup-guide.md](.workspace-rules/initial-setup-guide.md)
- [SETUP_GUIDE.md](SETUP_GUIDE.md)

## Pré-requisitos

- Java `17.0.12-oracle`
- Blade CLI instalado e disponível no `PATH`
- Acesso à internet para baixar o bundle do Liferay na inicialização do servidor

## Setup rápido

O passo a passo completo está em [SETUP_GUIDE.md](SETUP_GUIDE.md).

Fluxo resumido:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-oracle
export PATH="$JAVA_HOME/bin:$PATH"

java -version
blade server init
blade server start -t
```

Depois que o portal subir:

- URL: `http://localhost:8080`
- Login padrão: `test@liferay.com`
- Senha padrão: `test`

## Comandos de uso

Gerar código do Service Builder:

```bash
blade gw buildService
```

Formatar o projeto:

```bash
blade gw formatSource
```

Fazer deploy dos módulos:

```bash
blade gw deploy
```

Ver tarefas Gradle disponíveis:

```bash
blade gw tasks
```

## Desenvolvimento

O módulo principal usa Service Builder. Alterações estruturais na entidade devem começar em `service.xml` e depois seguir este fluxo:

```bash
blade gw buildService
blade gw formatSource
blade gw deploy
```

## Referências

- Guia operacional do workspace: [SETUP_GUIDE.md](SETUP_GUIDE.md)
- Guia base para novos usuários: [.workspace-rules/initial-setup-guide.md](.workspace-rules/initial-setup-guide.md)
- Regras gerais de Liferay: [.workspace-rules/liferay-rules.md](.workspace-rules/liferay-rules.md)

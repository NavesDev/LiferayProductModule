# Setup Guide

Este guia prepara o ambiente local para executar este workspace Liferay com Java `17.0.12-oracle`.

Como referência complementar, veja também [.workspace-rules/initial-setup-guide.md](.workspace-rules/initial-setup-guide.md).

## Pré-requisitos

- Java `17.0.12-oracle`
- Blade CLI instalado
- Git instalado

## 1. Configurar Java 17.0.12-oracle

Confirme ou ajuste o caminho do Java Oracle 17 na sua máquina:

Se estiver usando SDKMAN:

```bash
sdk install java 17.0.12-oracle
sdk use java 17.0.12-oracle
```

Se estiver usando uma JVM instalada localmente:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-oracle
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Resultado esperado no `java -version`: Java `17.0.12-oracle`.

## 2. Entrar no projeto

```bash
cd /caminho/do/LiferayProductModule
```

## 3. Validar a estrutura do workspace

Este projeto já contém `gradle.properties` e `settings.gradle`, então não é necessário rodar `blade init -v`.

Se quiser validar:

```bash
ls gradle.properties settings.gradle
```

## 4. Baixar o bundle do Liferay

Se a pasta `bundles/` ainda não existir, inicialize o servidor:

```bash
blade server init
```

Verifique se o bundle foi criado:

```bash
ls bundles
```

## 5. Subir o portal

Opção recomendada, iniciando e acompanhando logs:

```bash
blade server start -t
```

Outras opções úteis:

```bash
blade server start
blade server start -d
blade server run
```

## 6. Validar a subida do ambiente

Acompanhe os logs:

```bash
tail -f bundles/tomcat/logs/catalina.out
```

Siga quando aparecer uma linha parecida com:

```text
Server startup in [X] ms
```

Depois acesse:

- `http://localhost:8080`
- Usuário: `test@liferay.com`
- Senha: `test`

## 7. Comandos de desenvolvimento

Gerar código do Service Builder:

```bash
blade gw buildService
```

Formatar código:

```bash
blade gw formatSource
```

Publicar módulos no servidor:

```bash
blade gw deploy
```

Listar tarefas disponíveis:

```bash
blade gw tasks
```

## 8. Fluxo recomendado para este projeto

Quando alterar [service.xml](modules/product-service/product-service-service/service.xml):

```bash
blade gw buildService
blade gw formatSource
blade gw deploy
```

## Solução de problemas

Se o servidor não iniciar:

```bash
tail -n 200 bundles/tomcat/logs/catalina.out
```

Se quiser reiniciar após parar o processo:

```bash
blade server start -t
```

Se a falha estiver relacionada ao Java, revise:

```bash
java -version
echo $JAVA_HOME
```

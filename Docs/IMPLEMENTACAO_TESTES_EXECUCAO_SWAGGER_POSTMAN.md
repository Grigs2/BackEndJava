# Execução local, Swagger e roteiro de testes no Postman

**Atualizado em:** 03/11/2026

Este documento registra o que foi feito para rodar o backend localmente, documentar as rotas em Swagger/OpenAPI e testar a API de ponta a ponta com uma coleção do Postman. O conteúdo foi levantado a partir do código e validado com uma execução real da coleção.

## Sumário

1. [Resumo do que foi entregue](#1-resumo-do-que-foi-entregue)
2. [Configuração para rodar localmente](#2-configuração-para-rodar-localmente)
3. [Script de variáveis de ambiente](#3-script-de-variáveis-de-ambiente-scriptsdev-envps1)
4. [Documentação Swagger/OpenAPI](#4-documentação-swaggeropenapi)
5. [Coleção do Postman](#5-coleção-do-postman)
6. [Modificações na aplicação](#6-modificações-na-aplicação)
7. [Resultado da validação](#7-resultado-da-validação)
8. [Pontos de atenção e pendências](#8-pontos-de-atenção-e-pendências)

---

## 1. Resumo do que foi entregue

| Item | Local |
|---|---|
| Script de variáveis de ambiente de teste | `scripts/dev-env.ps1` |
| Swagger UI e JSON OpenAPI (springdoc) | `pom.xml`, `config/OpenApiConfig.java`, `config/SecurityConfig.java`, `application.properties` |
| Gerador da coleção e do environment do Postman | `scripts/gerar-colecao-postman.js` |
| Coleção do Postman (165 requisições) | `Docs/postman/TioDaPerua.postman_collection.json` |
| Environment do Postman | `Docs/postman/TioDaPerua.local.postman_environment.json` |
| Correções no backend necessárias para o fluxo completo | `Administrador.java`, `ViaCepValidationService.java` |

---

## 2. Configuração para rodar localmente

### 2.1 Pré-requisitos verificados na máquina

| Item | Situação encontrada | Ação |
|---|---|---|
| Java 21 | Instalado (21.0.3) | Nenhuma |
| PostgreSQL | Não havia instalação no Windows | Uso do container Docker `tiodaperua-db` (PostgreSQL 15, banco `TioDaPerua`, usuário `postgres`, senha `sa`, porta 5432) |
| Docker Desktop | Instalado, mas fechado | Abrir o aplicativo antes de usar o banco |
| Porta 8080 | Ocupada por `AgentService.exe` | O backend local usa a porta **8090** (`SERVER_PORT`) |
| Variáveis de ambiente | Nenhuma definida | Criadas por `scripts/dev-env.ps1` |
| Node.js | Presente em `C:\nvm4w\nodejs` | Necessário só para gerar a coleção e rodar o Newman |

Existem também os containers `tiodaperua-api` e `tiodaperua-front` (parados). Eles usam a 8080 e a 5432, então não devem ser iniciados junto com o backend local.

### 2.2 Passo a passo

1. Abra o Docker Desktop e inicie o banco:

   ```powershell
   docker start tiodaperua-db
   ```

2. Na pasta `BackEndJava`, libere a execução de scripts apenas na sessão atual (a política padrão do Windows bloqueia `.ps1`):

   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
   ```

3. Carregue as variáveis e suba o backend **no mesmo terminal**:

   ```powershell
   . .\scripts\dev-env.ps1
   .\mvnw.cmd spring-boot:run
   ```

4. Considere o backend no ar quando o log não mostrar `Application run failed`. Endereços:

   | Recurso | URL |
   |---|---|
   | API (base) | `http://localhost:8090/TioDaPerua/api` |
   | Swagger UI | `http://localhost:8090/TioDaPerua/api/swagger-ui.html` |
   | JSON OpenAPI | `http://localhost:8090/TioDaPerua/api/v3/api-docs` |

### 2.3 Problemas já encontrados

| Sintoma | Causa | Solução |
|---|---|---|
| `Unable to determine Dialect without JDBC metadata` | O banco está parado | `docker start tiodaperua-db`. O container foi desligado por algo externo ao projeto duas vezes durante os testes. |
| `A variável JWT_SECRET_BASE64 deve ser configurada` | Variáveis carregadas em outro terminal | Rodar `. .\scripts\dev-env.ps1` e o `mvnw` no mesmo terminal |
| Falha de bootstrap do ADMIN (`unsaved transient instance`) | Bug em `Administrador.usuario` | Corrigido, ver [6.1](#61-cascade-no-vínculo-administradorusuario) |
| `Port 8080 was already in use` | Outro processo usa a porta | Manter `SERVER_PORT=8090` |

---

## 3. Script de variáveis de ambiente (`scripts/dev-env.ps1`)

### 3.1 Objetivo

Definir, uma única vez, as variáveis exigidas pelo backend, mantendo a **chave JWT fixa** entre execuções. O backend só precisa que `JWT_SECRET_BASE64` seja estável; ela não precisa ser conhecida por quem escreve o script.

### 3.2 Como funciona

Para cada variável, o script verifica se ela já existe no ambiente do **usuário** do Windows:

- se não existe, cria o valor e grava com `SetEnvironmentVariable(..., 'User')`;
- se existe, reaproveita o valor;
- em ambos os casos, copia o valor para o processo atual (escopo `Process`), para o `mvnw` enxergar.

Por isso o script deve ser executado com o ponto na frente (`. .\scripts\dev-env.ps1`). Nenhum segredo fica no repositório.

### 3.3 Variáveis criadas

| Variável | Valor | Observação |
|---|---|---|
| `JWT_SECRET_BASE64` | 32 bytes aleatórios em Base64 | Gerada uma vez; mantém os tokens válidos entre reinícios |
| `ADMIN_EMAIL` | `admin@tiodaperua.local` | Usada no bootstrap do primeiro ADMIN |
| `ADMIN_PASSWORD` | Senha aleatória (acima de 12 caracteres) | Impressa quando criada. Para ver depois: `$env:ADMIN_PASSWORD` |
| `GPS_CONSENT_VERSION` | `gps-v1` | Sem ela, o consentimento de GPS retorna 503 |
| `TERMS_VERSION` | `termos-v1` | Versão dos termos registrada nos cadastros |
| `JWT_ACCESS_TOKEN_MINUTES` | `120` | Evita que o token expire no meio do roteiro |
| `DOCS_ENABLED` | `true` | Liga o Swagger (padrão do backend: desligado) |
| `SERVER_PORT` | `8090` | Evita o conflito com a porta 8080 |

Para trocar um valor, remova a variável do usuário (`[Environment]::SetEnvironmentVariable('NOME', $null, 'User')`) ou edite pelas configurações do Windows e rode o script de novo.

### 3.4 Variáveis opcionais não definidas

`SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL` e `PASSWORD_RESET_URL` não são definidas. Sem elas, `SolicitarRedefinicaoSenha` responde 202, mas nenhum e-mail é enviado.

---

## 4. Documentação Swagger/OpenAPI

### 4.1 Decisão

Foi usada a opção de gerar a documentação **a partir do código**, com springdoc. Assim ela acompanha os controllers e os DTOs (incluindo `@NotBlank`, `@Size` e `@Pattern`) sem manutenção manual.

### 4.2 O que foi feito

| Arquivo | Alteração |
|---|---|
| `pom.xml` | Dependência `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3` (série 3.x, compatível com Spring Boot 4) |
| `application.properties` | `app.docs.enabled=${DOCS_ENABLED:false}` e propriedades `springdoc.*` ligadas a ela |
| `config/SecurityConfig.java` | Libera `GET /v3/api-docs/**`, `/swagger-ui.html` e `/swagger-ui/**` **somente** quando `app.docs.enabled=true` |
| `config/OpenApiConfig.java` (novo) | Ativo só com `app.docs.enabled=true`. Define título, descrição com a ordem sugerida de teste e o esquema `bearerAuth` (JWT). Remove o cadeado das rotas públicas |
| Controllers (7) | `@Tag(name = "...")` para agrupar as rotas: Admin, Autenticacao, Escola, Motorista, Notificacao, Responsavel, Viagem |

### 4.3 Como usar

1. Abra o Swagger UI.
2. Execute `POST /Autenticacao/Entrar`, copie o `accessToken` e informe em **Authorize**.
3. Para gerar uma coleção diretamente do OpenAPI no Postman: **Import → Link** com `http://localhost:8090/TioDaPerua/api/v3/api-docs`.

### 4.4 Segurança

- Em produção, deixe `DOCS_ENABLED` sem valor (ou `false`). Com isso, as rotas de documentação nem são liberadas.
- A descrição do OpenAPI resume a ordem de teste e avisa que os IDs de perfil são diferentes do ID de usuário.

---

## 5. Coleção do Postman

### 5.1 Arquivos

A coleção e o environment são **gerados** por `scripts/gerar-colecao-postman.js`. Para alterar o roteiro, edite o script e execute:

```powershell
node scripts/gerar-colecao-postman.js
```

O gerador também valida a sintaxe de todos os scripts embutidos antes de gravar os arquivos.

### 5.2 Como usar no Postman

1. Importe a coleção e o environment `Tio da Perua - Local` e selecione o environment.
2. Preencha `adminPassword` (variável do tipo *secret*) com o valor de `$env:ADMIN_PASSWORD`.
3. Com o backend e o banco no ar, rode as pastas **em ordem** pelo Collection Runner (ou rode a coleção inteira).

Requisitos para o fluxo completo: internet (ViaCEP), `GPS_CONSENT_VERSION` definida no backend e PostgreSQL ativo.

### 5.3 Como rodar pela linha de comando (Newman)

```powershell
$env:Path = 'C:\nvm4w\nodejs;' + $env:Path
npx --yes newman run Docs\postman\TioDaPerua.postman_collection.json `
  -e Docs\postman\TioDaPerua.local.postman_environment.json `
  --env-var "adminPassword=$env:ADMIN_PASSWORD"
```

### 5.4 Estrutura (165 requisições)

| Pasta | Conteúdo | Req. |
|---|---|---:|
| 0. Preparação | Health check, geração dos dados únicos e conferência do ViaCEP | 2 |
| 1. Cadastros e login | Cadastro de escola, motorista e dois responsáveis; casos negativos; login dos 5 perfis; `Perfil` e `Autenticar` legado | 20 |
| 2. Perfis (AlterarInfos) | Alteração de dados de motorista, escola e responsável | 3 |
| 3. Motorista: veículo e documentos | Veículo, envio de CNH/vistoria/licenciamento, recusa e aprovação pelo ADMIN | 13 |
| 4. Vínculo motorista x escola | Convite recusado, solicitação aprovada, histórico, conflito | 14 |
| 5. Responsável: dependentes, escola e endereços | Dependentes, vínculo com escola, endereços, `AlterarInfos`, convites entre responsáveis | 23 |
| 6. Viagem e solicitações | Criar viagem, solicitar dependentes, aceite do responsável, prontidão | 15 |
| 7. Execução diária | Consentimento GPS, preparar execução, paradas, iniciar, localização, presenças online e offline, notificações, finalizar | 29 |
| 8. Execução cancelada | Cancelamento com e sem motivo, transição inválida | 4 |
| 9. Administração | Consultas paginadas, responsável criador, suspender e reativar conta | 10 |
| 10. Encerramentos | Suspender/reativar/encerrar vínculos, encerrar vínculo da viagem, desativar viagem, revogar responsável | 15 |
| 11. Autenticação: sessão e senha | Renovar, reuso de refresh, alterar senha, redefinição, sair | 11 |
| 12. Autorização (negativos) | 401 e 403 | 6 |

Cada requisição tem nome no formato `pasta.sequência MÉTODO /caminho`, descrição quando há pré-condição relevante, e testes de status e de conteúdo.

### 5.5 Dados gerados a cada execução

A requisição `0.1` roda um *pre-request script* que grava no environment:

| Dado | Regra |
|---|---|
| `runId`, e-mails | Baseados no horário; um e-mail por perfil |
| CPFs (motorista, responsáveis, dependentes) | Aleatórios com dígitos verificadores válidos |
| CNPJ da escola | Aleatório, válido, final `0001` |
| Placa | Formato `AAA1B23` (aceito pela validação) |
| CNH e RENAVAM | 11 dígitos |
| Datas | `dataHoje`, `dataAmanha`, validade (+365 dias) e realização (−30 dias) |

Por isso a coleção pode ser executada várias vezes sem erro de "CPF já cadastrado" ou "e-mail já cadastrado".

Constantes do environment: `baseUrl`, `adminEmail`, `adminPassword`, `senhaPadrao` (`SenhaForte@2026`) e `senhaNova` (`NovaSenha@2026!`). Os CEPs usados são reais (`01310-100` e `01001-000`), por causa da validação no ViaCEP.

### 5.6 Variáveis capturadas pelos testes

Tokens (`adminToken`, `motoristaToken`, `escolaToken`, `respAToken`, `respBToken` e os `...Refresh`) e IDs de perfil e de usuário (`motoristaId`, `motoristaUsuarioId`, `escolaId`, `respAId`, `respAUsuarioId` e equivalentes), além de `dep1Id`, `dep2Id`, `vinculoId`, `viagemId`, `viagemDiaId`, `solic1Id`, `solic2Id`, `conviteId`, `docCnhId`, `paradasIds` e outros. Cada requisição autentica com o token do perfil correspondente (Bearer); rotas públicas usam `noauth`.

### 5.7 Regras de ordem que o roteiro respeita

- O login por `POST /Autenticacao/Entrar` é a única origem de JWT. Os `Autenticar` por perfil retornam só o perfil.
- O veículo vem antes dos documentos, porque alterar o veículo invalida vistoria e licenciamento.
- A viagem só chega a `EM_ANDAMENTO` com: veículo, endereços validados (motorista, escola e dependente), CNH + vistoria + licenciamento aprovados, vínculo **ATIVO** entre motorista e escola, e ao menos um dependente ativo. A rota `GET /Viagem/Prontidao/{id}` lista os bloqueios.
- `IniciarViagemDia` e `PrepararViagemDia` só criam a execução `PLANEJADA`; o início real é `PUT /Viagem/AlterarStatusViagemDia` com `EM_ANDAMENTO`.
- Reordenar paradas é permitido apenas antes ou durante a execução; a primeira e a última parada (motorista) são fixas.
- Presenças: `ESPERANDO → EMBARCADO → DESEMBARCADO` ou `ESPERANDO → FALTOU`. Finalizar exige todas em `DESEMBARCADO` ou `FALTOU`.
- A localização (GPS) exige execução `EM_ANDAMENTO`, consentimento do motorista e dos responsáveis, e intervalo mínimo de 15 s entre envios.
- Reuso de um refresh token antigo derruba a sessão inteira; por isso o roteiro guarda sempre o refresh novo.
- Suspender o vínculo escola–motorista, desativar a viagem ou encerrar o vínculo faz a prontidão falhar; essas rotas ficam nas pastas finais.

---

## 6. Modificações na aplicação

Além do Swagger (seção 4), duas correções foram necessárias para que o fluxo completo funcionasse.

### 6.1 Cascade no vínculo `Administrador.usuario`

- **Arquivo:** `entities/Administrador.java`
- **Problema:** em um banco novo, o bootstrap do primeiro ADMIN falhava com `TransientPropertyValueException`, porque o `Usuario` não era persistido junto com o `Administrador`. A aplicação não subia.
- **Correção:** `@OneToOne(fetch = LAZY, optional = false, cascade = CascadeType.PERSIST)`.

### 6.2 Validação de CEP pelo ViaCEP

- **Arquivo:** `services/ViaCepValidationService.java`
- **Problema:** o ViaCEP **não envia o campo `erro`** para CEP válido (só envia `"erro": "true"` para inexistente). O código tratava `erro == null` como resposta incompleta e marcava o endereço como `PENDENTE`. Na prática, nenhum endereço ficava `VALIDADO`, e a prontidão da viagem sempre bloqueava.
- **Correção:**
  - `erro == true` continua lançando "CEP não encontrado no ViaCEP";
  - resposta com o campo `cep` preenchido passa a ser `VALIDADO`;
  - resposta nula ou sem `cep`, ou falha de rede, continua `PENDENTE`;
  - o `record` interno ignora campos desconhecidos (`@JsonIgnoreProperties(ignoreUnknown = true)`).
- Os testes existentes usam mock de `CepValidationService`, então não são afetados. Não há teste automatizado para essa classe.

### 6.3 Corpos de requisição com campos primitivos

Não houve mudança no código, mas ficou documentado no roteiro: com a versão atual do Jackson, os DTOs com campos `boolean` primitivos devolvem 400 se o campo não vier no JSON.

| Rota | Campos que precisam ser enviados |
|---|---|
| `POST /Responsavel/ResponderSolicitacao/{id}` | `respondido` e `aceito` |
| `POST /Notificacao/Enviar` | `visto` |

---

## 7. Resultado da validação

A coleção foi executada com o Newman contra o backend local.

- **Primeira rodada:** 165 requisições, 37 asserções com falha. As causas eram os itens 6.2 e 6.3 e consequências em cascata.
- **Última rodada completa:** 165 requisições, 288 asserções, 1 falha (campo errado em um teste de `ConsultarMotorista`). Esse teste foi corrigido e a coleção regerada, **sem nova execução completa depois da correção**.
- Duração aproximada: 35 a 45 segundos.

Os testes automatizados do Maven (`mvnw test`) **não foram executados**.

---

## 8. Pontos de atenção e pendências

### Arquivos que foram sobrescritos
Ao criar os arquivos desta entrega, quatro arquivos que já existiam e não estavam no git foram sobrescritos: `scripts/dev-env.ps1`, `scripts/gerar-colecao-postman.js`, `Docs/postman/TioDaPerua.postman_collection.json` e `Docs/postman/TioDaPerua.local.postman_environment.json`. Também foram apagados resultados antigos do Newman em `target/`. Os originais não foram recuperados. Se houver cópia em outro lugar, vale comparar.

### Outros pontos
- **Credenciais do banco:** `postgres` / `sa` continuam fixas no `application.properties`.
- **Docker:** o container `tiodaperua-db` tem `restart=always`, mas foi parado por algo externo duas vezes. Se o backend não conectar, confira se ele está de pé.
- **Dependência de internet:** sem o ViaCEP, os endereços ficam `PENDENTE` e a viagem não inicia.
- **E-mail de recuperação:** sem SendGrid, só o fluxo negativo de `RedefinirSenha` é testável (o token só chega por e-mail).
- **Documentação antiga:** `API_DOCUMENTACAO_COMPLETA_V1..V3` estão defasadas (por exemplo, `Autenticar` aparece como GET, mas é POST). Considere marcá-las como obsoletas e usar o Swagger como referência.
- **Swagger em produção:** manter `DOCS_ENABLED` desligado.
- **Versão da dependência de validação:** o `pom.xml` fixa `spring-boot-starter-validation` em 3.4.4, diferente do Spring Boot 4.0.3 do projeto. A aplicação sobe, mas vale revisar.

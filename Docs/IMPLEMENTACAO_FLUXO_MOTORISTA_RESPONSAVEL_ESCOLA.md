# IMPLEMENTACAO_FLUXO_MOTORISTA_RESPONSAVEL_ESCOLA.md

## 1. Objetivo

Definir o plano de implementação do fluxo completo entre as entidades **Escola**, **Motorista** e **Responsável** no projeto **Tio da Perua**, cobrindo backend (Java/Spring), frontend (React Native/Expo) e banco de dados.

O fluxo resumido é:

1. Escola se cadastra.
2. Motorista se cadastra.
3. Motorista escolhe escolas cadastradas que pretende atender e solicita atendimento.
4. Escola avalia os documentos do motorista e aprova ou recusa o vínculo.
5. Se aprovado, o motorista passa a ser visível aos responsáveis de alunos daquela escola.
6. Responsável se cadastra.
7. Responsável cadastra dependentes e informa a escola de cada um.
8. Dois fluxos possíveis, em paralelo:
   * **8a/9a/10a (Responsável procura motorista):** o responsável busca motoristas, avalia-os e envia solicitação. Ao aceitar, o motorista recebe os dados do responsável e dos dependentes.
   * **8b/9b/10b (Motorista procura responsável):** o motorista vê dependentes da escola que atende, com dados anonimizados, e envia solicitação ao responsável. Ao aceitar, os dados são compartilhados com o motorista.

A implementação deve seguir `GEMINI.md` e a arquitetura descrita em `IMPLEMENTACAO_ESCOLA_SOLICITACOES.md` (Controller → DTO → Service → Repository).

---

## 2. Estado atual (levantamento no código)

### 2.1 O que já existe

| Etapa | Situação | Evidência |
|---|---|---|
| 1. Cadastro de Escola | Pronto | `EscolaController` `POST /Escola/Cadastrar` |
| 2. Cadastro de Motorista | Pronto | `MotoristaController` `POST /Motorista/Cadastrar` |
| 3. Motorista solicita vínculo com escola | Pronto (backend) | `MotoristaController` `POST /Motorista/Escolas/Vinculos`; `VinculoEscolaService.solicitarVinculo` (valida CNPJ da escola) |
| 3'. Escola convida motorista | Pronto (backend) | `EscolaController` `POST /Escola/Vinculos`; `VinculoEscolaService.convidarMotorista` |
| 4. Escola aprova/recusa vínculo | Parcial | `EscolaController` `POST /Escola/Vinculos/{id}/Aprovar` e `/Recusar`; porém **não há revisão de documentos pela escola** |
| 4. Documentos do motorista | Parcial | `DocumentoMotoristaService`; obrigatórios CNH, VISTORIA, LICENCIAMENTO; status `PENDENTE/APROVADO/RECUSADO`; **revisão só via `/Admin/Documentos/{id}/Revisar`** |
| 5. Motorista visível ao responsável | Não existe | Não há endpoint de busca de motoristas para responsável |
| 6. Cadastro de Responsável | Pronto | `ResponsavelController` `POST /Responsavel/Cadastrar` |
| 7. Cadastro de dependente + escola | Pronto | `POST /Responsavel/CadastrarDependente/{id}`; `POST /Responsavel/VincularEscola/{idR}/{idE}/{idD}` |
| 8a. Responsável busca motorista | Não existe (backend) | Frontend `GuardianSearchDriverScreen.tsx` usa `MOTORISTAS_MOCK` |
| 8b. Motorista busca dependentes | Parcial | `ViagemController` `GET /Viagem/DependentesDisponiveis/{idViagem}` retorna `DependenteDTO` **completo** (sem anonimização) |
| 9b. Motorista solicita dependente | Pronto (parcial) | `ViagemController` `POST /Viagem/SolicitarDependente/{idViagem}/{idDependente}`; `SolicitacaoService.solicitarDependente` |
| 10b. Responsável responde solicitação | Pronto (parcial) | `ResponsavelController` `POST /Responsavel/ResponderSolicitacao/{id}`; `SolicitacaoService.responderSolicitacao` |
| 9a/10a. Responsável solicita motorista | Não existe | Sem entidade/endpoint de solicitação iniciada pelo responsável |
| Confirmação de compartilhamento de dados | Não existe | Nenhum campo de consentimento na solicitação |
| Avaliações entre responsáveis | Não existe | Sem entidade `Avaliacao` |
| Acordo de transporte | Parcial | Hoje é apenas `ViagemDependente` (ativo) criado no aceite; sem registro formal do acordo |

### 2.2 Lacunas identificadas

1. **Escola não avalia documentos** do motorista (passo 4). A análise é exclusiva do administrador.
2. **Não há filtro de visibilidade** de motoristas por escola (passo 5): o responsável não tem como listar apenas motoristas com vínculo `ATIVO` com a escola do dependente.
3. **Solicitação tem apenas um sentido** (motorista → responsável). Falta o sentido responsável → motorista (9a).
4. **Sem anonimização** para o motorista (8b): `DependentesDisponiveis` expõe nome, endereço e demais dados do dependente.
5. **Sem consentimento de compartilhamento** (9a/9b): nenhum registro de que o responsável autorizou a troca de dados.
6. **Sem avaliações** de motoristas por responsáveis (8a).
7. **Sem visão limitada de documentos** do motorista para o responsável (8a/10b).
8. **Frontend com mocks**: `GuardianSearchDriverScreen.tsx` (e possivelmente telas do motorista) não consomem API.
9. **Sem acordo formal**: não há registro de quem combinou o quê, quando, e com qual consentimento.

---

## 3. Regras de negócio

### 3.1 Vínculo Motorista ↔ Escola (passos 3 a 5)

* O motorista só pode ser exibido a responsáveis se houver vínculo com status **`ATIVO`** com a escola (`StatusVinculoEscola.ATIVO`).
* A escola deve aprovar o vínculo somente após **validar documentos obrigatórios** do motorista (CNH, VISTORIA, LICENCIAMENTO) com status `APROVADO` e não vencidos.
* A escola pode **suspender** ou **encerrar** o vínculo. Ao suspender/encerrar, o motorista deixa de aparecer na busca dos responsáveis daquela escola, mas os acordos já firmados devem ser tratados (ver seção 3.5).
* Se a escola recusar, o motorista não aparece para nenhum responsável daquela escola.

### 3.2 Visibilidade do motorista para o responsável (passo 5)

* O responsável só vê motoristas com vínculo `ATIVO` com a escola do dependente que deseja transportar.
* Na busca, são exibidos **somente dados limitados**: nome, foto (se houver), **modelo e cor do veículo**, tempo de atuação, média de avaliações, quantidade de avaliações e **status** (não o número) de cada documento obrigatório (ex.: `CNH: Aprovada / válida até 12/2027`).
* Não são exibidos: CPF, RG, número da CNH, **placa**, endereço e telefone.

### 3.3 Solicitações (passos 9a, 9b, 10a, 10b)

Toda solicitação entre motorista e responsável deve ter:

* **Sentido:** `MOTORISTA_PARA_RESPONSAVEL` ou `RESPONSAVEL_PARA_MOTORISTA`.
* **Itens**: um item por dependente, cada um com sua escola e status próprio (ver seção 3.9).
* **Status da solicitação:** `PENDENTE`, `ACEITA`, `ACEITA_PARCIALMENTE`, `RECUSADA`, `CANCELADA`, `EXPIRADA`.
* **Expiração:** 3 dias após o envio. Ao expirar, todos os itens pendentes passam a `EXPIRADA`.
* **Consentimento de compartilhamento** (`compartilhamentoAutorizado`): obrigatório e explícito no envio pelo responsável (9a). Não é aceito envio sem essa confirmação.
* Apenas o destinatário pode aceitar ou recusar. Não é permitido aceitar duas vezes.

### 3.4 Dados compartilhados após aceite (passos 10a e 10b)

Ao aceite com consentimento:

* **Motorista recebe:** nome, telefone, endereço de embarque/desembarque e dados do(s) dependente(s) incluídos no acordo; nome, telefone e contatos do responsável.
* **Responsável recebe:** nome, foto e dados limitados do motorista (mesmos dados da seção 3.2, acrescidos de telefone após aceite).

Antes do aceite, nenhum dado pessoal identificável é compartilhado.

### 3.5 Acordo de transporte

Ao aceite, é criado um **Acordo de Transporte** que liga: motorista, responsável, dependente(s), escola e vínculo motorista-escola vigente. Ele deve:

* Ter status `ATIVO`, `ENCERRADO` ou `CANCELADO`.
* Registrar `dataInicio`, `dataFim` e `encerradoPor`.
* Ser encerrado automaticamente se o vínculo motorista-escola for encerrado ou suspenso pela escola, ou se o dependente deixar de estar vinculado àquela escola.
* Substituir o uso direto de `ViagemDependente` como fonte de verdade do acordo (o `ViagemDependente` continua existindo para a operação diária da viagem).

### 3.6 Avaliações (passo 8a)

* Somente responsáveis com acordo **ativo ou encerrado** com o motorista podem avaliá-lo.
* Uma avaliação por responsável por acordo (nota 1 a 5 + comentário opcional).
* Motorista pode ser avaliado pelos responsáveis; o motorista **não** avalia responsável neste escopo (decisão a confirmar).
* Comentários passam por moderação simples (administrador pode ocultar).

### 3.7 Anonimização para o motorista (passo 8b)

Na busca de dependentes disponíveis, o motorista vê apenas:

* Identificador interno do dependente (não expor no front como "ID real" de ninguém).
* Iniciais ou apelido, ano de nascimento ou faixa etária, escola (já é a do motorista), período de estudo, e **nome do responsável** (conforme informado na especificação: "apenas a informação do responsável do dependente" é visível).
* Bairro/região aproximada (sem endereço completo) — decisão a confirmar.

Após o aceite do responsável, os dados completos são liberados ao motorista.

---

### 3.8 Proteção do responsável e dos dependentes

Responsáveis e dependentes são a parte mais vulnerável do fluxo. **Escopo do MVP (decidido)**: consentimento revogável, mínimo de dados, auditoria e denúncia. Regras decorrentes:

* **Consentimento revogável**: o responsável pode revogar o compartilhamento a qualquer momento. A revogação encerra o acordo correspondente e notifica o motorista.
* **Mínimo necessário**: dados do dependente só são enviados ao motorista para os itens aceitos. Nenhum contato é trocado antes do aceite.
* **Endereço de embarque**: fornecido somente após aceite e restrito ao necessário para a rota.
* **Auditoria**: todo acesso a dados compartilhados fica registrado (seção 5.4).
* **Denúncia**: responsável e motorista podem denunciar. A denúncia notifica a escola e o administrador. Motorista denunciado pode ter o vínculo suspenso.
* **Avaliações**: não expõem dados do dependente.
* **Verificação reforçada do motorista** (identidade e antecedentes): **decidido: exigida**. Hoje só há documentos de veículo e habilitação, então a política e o fornecedor de verificação precisam ser definidos antes da implementação.

### 3.9 Solicitação com dependentes de escolas diferentes (decidido: Opção A)

**Opção A: solicitação agrupada com itens (recomendada).** Cabeçalho (motorista, responsável, sentido, consentimento, expiração) e um item por dependente, com escola e status próprios.

* Um único envio e um único aceite para o responsável.
* Cada item é validado contra o vínculo `ATIVO` da escola daquele dependente, no envio e no aceite.
* Aceite parcial: o motorista pode aceitar apenas os itens de uma escola. Cada item aceito gera um `AcordoTransporte`.
* Consentimento vale para a solicitação inteira. Os dados compartilhados se limitam aos itens aceitos.
* Se a escola suspender o vínculo, somente os itens daquela escola são cancelados.

**Opção B: uma solicitação por par (motorista, responsável, escola).** Mais simples, sem status por item. Porém o responsável recebe várias solicitações do mesmo motorista e confirma o consentimento várias vezes.

**Opção C: uma solicitação por dependente.** Mais simples de implementar, com agrupamento apenas visual na interface.

**Complexidade da opção A:** moderada. Os pontos mais delicados são o aceite parcial, a expiração e o cancelamento automático de itens quando o vínculo com a escola muda enquanto a solicitação está pendente.

## 4. Backend (Java / Spring)

### 4.1 Novas entidades e enums

| Item | Tipo | Descrição |
|---|---|---|
| `SolicitacaoTransporte` | Entity (nova) | Substitui/evolui `Solicitacao`. Campos: `id`, `motorista`, `responsavel`, `escola`, `sentido` (enum), `status` (enum), `compartilhamentoAutorizado` (boolean), `criadaEm`, `respondidaEm`, `expiraEm`, `mensagem` |
| `SolicitacaoTransporteItem` | Entity (nova) | Um item por dependente: `solicitacao`, `dependente`, `escola`, `status` (`PENDENTE`, `ACEITO`, `RECUSADO`, `CANCELADO`, `EXPIRADO`), `motivoCancelamento` |
| `AcordoTransporte` | Entity (nova) | Acordo formal (seção 3.5). Campos: `id`, `solicitacao`, `motorista`, `responsavel`, `escola`, `status`, `dataInicio`, `dataFim`, `encerradoPor`, `motivoEncerramento` |
| `AcordoTransporteDependente` | Entity (nova, N:N) | Dependentes do acordo |
| `AvaliacaoMotorista` | Entity (nova) | `id`, `acordo`, `responsavel`, `motorista`, `nota` (1–5), `comentario`, `ocultada`, `criadaEm` |
| `StatusSolicitacaoTransporte` | Enum (novo) | `PENDENTE`, `ACEITA`, `RECUSADA`, `CANCELADA`, `EXPIRADA` |
| `SentidoSolicitacaoTransporte` | Enum (novo) | `MOTORISTA_PARA_RESPONSAVEL`, `RESPONSAVEL_PARA_MOTORISTA` |
| `StatusAcordoTransporte` | Enum (novo) | `ATIVO`, `ENCERRADO`, `CANCELADO` |

Observação: `Solicitacao` existente pode ser mantida durante a transição (ver seção 5, migração).

### 4.2 Alterações em entidades existentes

* **`Solicitacao`**: adicionar `sentido`, `compartilhamentoAutorizado`, `status` (ou migrar para `SolicitacaoTransporte`).
* **`Motorista`**: adicionar campos de exibição pública limitada (`fotoUrl`, `anosAtuacao`), se aprovado.
* **`DocumentoMotorista`**: adicionar `validadeAtual` (calculada) e método para exibir apenas status/validade (sem número).
* **`VinculoEscola`** (`StatusVinculoEscola`): nenhuma mudança de enum; reutilizar `ATIVO` como critério de visibilidade.
* **`Dependente`**: garantir que `escola` seja obrigatória para ser solicitado (já validado em `SolicitacaoService`).

### 4.3 Repositórios

| Repositório | Método/Query nova |
|---|---|
| `VinculoEscolaRepository` | `findMotoristasAtivosPorEscola(escolaId)` |
| `DependenteRepository` | `findDependentesDaEscolaSemAcordoComMotorista(escolaId, motoristaId)` |
| `SolicitacaoTransporteRepository` (novo) | `findPendentesPorDestinatario(usuarioId, sentido)`; `existePendente(motoristaId, responsavelId, dependenteIds)` |
| `AcordoTransporteRepository` (novo) | `findAtivosPorMotorista(motoristaId)`; `findAtivosPorResponsavel(responsavelId)`; `existeAcordoAtivo(motoristaId, dependenteId)` |
| `AvaliacaoMotoristaRepository` (novo) | `findByMotoristaIdAndOcultadaFalse(motoristaId)`; `mediaNotaPorMotorista(motoristaId)`; `existeAvaliacao(acordoId, responsavelId)` |

### 4.4 Services

| Service | Alteração |
|---|---|
| `VinculoEscolaService` | Ao aprovar vínculo (`responderComoEscola`), exigir documentos obrigatórios aprovados. Ao suspender/encerrar, encerrar acordos dependentes (chamar `AcordoTransporteService`). |
| `DocumentoMotoristaService` | Novo método `listarResumoParaEscola(escolaId, motoristaId)` retornando apenas status/validade. Escola só acessa motoristas com vínculo `PENDENTE_ESCOLA` ou `ATIVO` com ela. |
| `SolicitacaoService` (evoluir) | Refatorar para suportar os dois sentidos. Criar `enviarPeloResponsavel(...)` (exige `compartilhamentoAutorizado = true`) e `enviarPeloMotorista(...)`. `responderSolicitacao` ao aceitar cria `AcordoTransporte`, libera dados e notifica. |
| `MotoristaBuscaService` (novo) | Busca de motoristas por escola do dependente, retornando `MotoristaVisivelDTO`. |
| `DependenteAnonimizadoService` (novo) | Retorna `DependenteAnonimizadoDTO` para o motorista (seção 3.7). |
| `AcordoTransporteService` (novo) | Criar, encerrar, cancelar acordos; reagir a mudanças de vínculo com escola. |
| `AvaliacaoMotoristaService` (novo) | Criar/listar/ocultar avaliações. Recalcular média. |
| `ResponsavelService` | Método `listarMotoristasDisponiveis(idResponsavel, idDependente)`; ajustar `vincularEscola` para cancelar solicitações pendentes se a escola do dependente mudar. |
| `NotificacaoService` | Novos eventos: solicitação recebida/aceita/recusada, acordo encerrado, vínculo suspenso, avaliação recebida. |
| `ViagemService` / `SolicitacaoService.listarDependentesDisponiveis` | Trocar retorno para DTO anonimizado. |

### 4.5 Controllers e endpoints

#### Escola (`EscolaController`, base `/Escola`)

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/Escola/Motoristas/{idVinculo}/Documentos` | Lista resumo de documentos do motorista do vínculo (somente status/validade) |
| POST | `/Escola/Vinculos/{idVinculo}/Aprovar` | **Alterar**: bloquear se documentos obrigatórios não estiverem aprovados e válidos |

#### Motorista (`MotoristaController`, base `/Motorista`)

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/Motorista/Escolas/Ativas` | Escolas em que o motorista está `ATIVO` |
| POST | `/Motorista/Solicitacoes` | Envia solicitação ao responsável (body: `dependenteIds[]`, `mensagem`) — passo 9b |
| GET | `/Motorista/Dependentes/Disponiveis` | Dependentes das escolas ativas, **anonimizados** — passo 8b |
| GET | `/Motorista/Solicitacoes` | Solicitações enviadas e recebidas |
| POST | `/Motorista/Solicitacoes/{id}/Aceitar` | Aceita solicitação do responsável (10a) — gera acordo e libera dados |
| POST | `/Motorista/Solicitacoes/{id}/Recusar` | Recusa solicitação do responsável (10a) |
| GET | `/Motorista/Acordos` | Acordos ativos e encerrados |
| POST | `/Motorista/Acordos/{id}/Encerrar` | Encerra acordo |
| GET | `/Motorista/Avaliacoes` | Avaliações recebidas |

#### Responsável (`ResponsavelController`, base `/Responsavel`)

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/Responsavel/Dependentes/{idDependente}/Motoristas` | Motoristas com vínculo `ATIVO` com a escola do dependente, dados limitados — passos 5 e 8a |
| GET | `/Responsavel/Motoristas/{idMotorista}` | Perfil limitado do motorista, documentos (status/validade) e avaliações |
| POST | `/Responsavel/Solicitacoes` | Envia solicitação ao motorista (body: `motoristaId`, `dependenteIds[]`, `compartilhamentoAutorizado: true`) — passo 9a |
| GET | `/Responsavel/Solicitacoes` | Solicitações enviadas e recebidas |
| POST | `/Responsavel/Solicitacoes/{id}/Aceitar` | Aceita solicitação do motorista (10b), exige `compartilhamentoAutorizado: true` |
| POST | `/Responsavel/Solicitacoes/{id}/Recusar` | Recusa solicitação do motorista (10b) |
| POST | `/Responsavel/Acordos/{id}/Avaliacao` | Cria avaliação do motorista (8a) |
| GET | `/Responsavel/Acordos` | Acordos ativos e encerrados |
| POST | `/Responsavel/Acordos/{id}/Encerrar` | Encerra acordo |

#### Endpoints legados a manter com compatibilidade

* `/Responsavel/ResponderSolicitacao/{id}` e `/Responsavel/ListarSolicitacoes/{idResponsavel}`: manter enquanto o front migra.
* `/Viagem/DependentesDisponiveis/{idViagem}` e `/Viagem/SolicitarDependente/...`: manter, mas o retorno passa a ser anonimizado. Motorista não deve ver dados completos antes do aceite.

### 4.6 DTOs

Novos DTOs (seguindo o padrão `toDTO()` / `toEntity()`):

* `SolicitacaoTransporteDTO`, `CriarSolicitacaoMotoristaDTO`, `CriarSolicitacaoResponsavelDTO` (com `compartilhamentoAutorizado` obrigatório, validado com `@AssertTrue`).
* `MotoristaVisivelDTO` (dados limitados), `DocumentoResumoDTO` (tipo, status, validade).
* `DependenteAnonimizadoDTO` (sem nome completo nem endereço).
* `AcordoTransporteDTO`, `AvaliacaoMotoristaDTO`, `CriarAvaliacaoDTO` (nota `@Min(1) @Max(5)`).
* `DadosCompartilhadosDTO` (retornado apenas após aceite).

### 4.7 Segurança (`@PreAuthorize` / `ResourceAuthorizationService`)

Adicionar métodos em `AutorizacaoRecursoService`:

* `isDestinatarioDaSolicitacao(authentication, idSolicitacao)`: verifica se o usuário é o motorista ou responsável destinatário, conforme o sentido.
* `isParteDoAcordo(authentication, idAcordo)`.
* `isEscolaDoVinculo(authentication, idVinculo)`.
* `podeVerMotorista(authentication, idMotorista, idDependente)`: verifica vínculo `ATIVO` com a escola do dependente.

### 4.8 Regras de validação e erros

* Solicitação duplicada (mesmo par motorista/responsável com dependente pendente): `409 Conflict`.
* Envio pelo responsável sem `compartilhamentoAutorizado = true`: `400 Bad Request`.
* Motorista sem vínculo `ATIVO` com a escola do dependente: `403 Forbidden`.
* Aceite de solicitação já respondida: `409 Conflict`.
* Avaliação duplicada no mesmo acordo: `409 Conflict`.

Usar exceções existentes (`BusinessException`, `ConflictException`, `ResourceNotFoundException`) e `AccessDeniedException`.

### 4.9 Notificações

| Evento | Destinatário |
|---|---|
| Escola convida ou recebe pedido de motorista | Escola / motorista |
| Escola aprova ou recusa vínculo | Motorista |
| Vínculo suspenso ou encerrado | Motorista e responsáveis afetados |
| Solicitação recebida (9a/9b) | Destinatário |
| Solicitação aceita ou recusada (10a/10b) | Remetente |
| Acordo encerrado | Partes do acordo |
| Avaliação recebida | Motorista |

---

## 5. Banco de dados

### 5.1 Novas tabelas

| Tabela | Colunas principais | Índices/Constraints |
|---|---|---|
| `solicitacao_transporte` | `id`, `motorista_id` (FK), `responsavel_id` (FK), `escola_id` (FK), `sentido`, `status`, `compartilhamento_autorizado`, `mensagem`, `criada_em`, `respondida_em`, `expira_em` | Índice em `(motorista_id, status)` e `(responsavel_id, status)`; CHECK em `sentido` e `status` |
| `solicitacao_transporte_item` | `id`, `solicitacao_id` (FK), `dependente_id` (FK), `escola_id` (FK), `status`, `motivo_cancelamento` | UNIQUE `(solicitacao_id, dependente_id)`; índice `(escola_id, status)` para cancelamento por mudança de vínculo |
| `acordo_transporte` | `id`, `solicitacao_id` (FK, UNIQUE), `motorista_id`, `responsavel_id`, `escola_id`, `status`, `data_inicio`, `data_fim`, `encerrado_por_usuario_id`, `motivo_encerramento` | Índice `(motorista_id, status)`; índice `(responsavel_id, status)` |
| `acordo_transporte_dependente` | `acordo_id` (FK), `dependente_id` (FK) | PK composta |
| `avaliacao_motorista` | `id`, `acordo_id` (FK), `responsavel_id` (FK), `motorista_id` (FK), `nota` (SMALLINT, CHECK 1–5), `comentario` (VARCHAR 500), `ocultada` (BOOLEAN default false), `criada_em` | UNIQUE `(acordo_id, responsavel_id)` |

### 5.2 Alterações em tabelas existentes

* `solicitacao` (atual): adicionar `sentido` (default `MOTORISTA_PARA_RESPONSAVEL`), `status`, `compartilhamento_autorizado` (default false). Ou migrar dados para `solicitacao_transporte` e descontinuar a antiga.
* `motorista`: adicionar `foto_url` (NULL), `data_inicio_atuacao` (DATE, NULL).
* `documento_motorista`: garantir índice em `(motorista_id, tipo, status)` para consultas da escola.
* `vinculo_escola`: índice em `(escola_id, status)` para busca de motoristas visíveis.
* `dependente`: garantir `escola_id` NOT NULL para dependentes com solicitação (ou validar em aplicação, se houver dependentes sem escola).

### 5.3 Migração

* Usar as ferramentas de migração já adotadas no projeto (verificar Flyway/Liquibase; se não houver, usar `ddl-auto` apenas em desenvolvimento e criar script SQL versionado em `BackEndJava/src/main/resources`).
* Migrar `ViagemDependente` ativos hoje para `acordo_transporte` em script único, criando solicitações de origem com `sentido = MOTORISTA_PARA_RESPONSAVEL` e `status = ACEITA`, e `compartilhamento_autorizado = true`.
* Manter `viagem_dependente` como está.

### 5.4 Dados sensíveis (LGPD)

* Criar tabela de **log de acesso** a dados compartilhados (`log_compartilhamento_dados`: `usuario_id`, `tipo_dado`, `dependente_id`, `acessado_em`), para rastreabilidade do compartilhamento.
* Dados de consentimento (`compartilhamento_autorizado`, `criada_em`, `respondida_em`) devem ser preservados como prova do consentimento.

---

## 6. Frontend (React Native / Expo)

### 6.1 Serviços (`frontendreact/src/services`)

| Arquivo | Alteração |
|---|---|
| `api.ts` (`API_ENDPOINTS`) | Adicionar endpoints novos da seção 4.5 |
| `responsavelService.ts` | `listarMotoristasDoDependente`, `buscarMotoristaPerfil`, `enviarSolicitacaoMotorista`, `listarSolicitacoesResponsavel` (nova), `aceitarSolicitacaoMotorista`, `recusarSolicitacaoMotorista`, `avaliarMotorista`, `listarAcordosResponsavel` |
| `motoristaService.ts` | `listarEscolasAtivas`, `listarDependentesDisponiveis` (anonimizado), `enviarSolicitacaoResponsavel`, `aceitarSolicitacaoResponsavel`, `recusarSolicitacaoResponsavel`, `listarAcordosMotorista`, `encerrarAcordo` |
| `escolaService.ts` | `listarDocumentosMotorista(idVinculo)`, `aprovarVinculo` (tratar erro de documentos pendentes) |
| `viagemService.ts` | Ajustar tipos caso `dependentesDisponiveis` passe a retornar DTO anonimizado |
| `notificacaoService.ts` | Sem mudança estrutural; novos tipos de notificação |

### 6.2 Tipos (`frontendreact/src/types.ts`)

Adicionar: `SolicitacaoTransporte`, `SentidoSolicitacao`, `StatusSolicitacao`, `AcordoTransporte`, `MotoristaVisivel`, `DocumentoResumo`, `DependenteAnonimizado`, `AvaliacaoMotorista`, `DadosCompartilhados`.

### 6.3 Telas

#### Escola (`SchoolMainScreen.tsx` e novas telas)

* **Vínculos pendentes** (já existe fluxo no back): exibir resumo de documentos do motorista (CNH, VISTORIA, LICENCIAMENTO) com status e validade antes de aprovar.
* Botões **Aprovar / Recusar** habilitados somente quando documentos obrigatórios estiverem válidos.
* Tratar erro `409` de documentos pendentes com mensagem clara.

#### Motorista

* **`DriverSearchStudentsScreen.tsx`** (busca de alunos): consumir `/Motorista/Dependentes/Disponiveis`. Exibir dependente anonimizado e responsável. Remover dados mock se existirem.
* **`DriverAvailableDependentsScreen.tsx`**: exibir apenas dados anonimizados; botão "Enviar solicitação" ao responsável.
* **`DriverInvitesScreen.tsx`**: receber solicitações do responsável (10a) com botões aceitar/recusar; ao aceitar, exibir dados compartilhados.
* **`DriverStudentsScreen.tsx`**: lista de acordos ativos (com dados completos somente após aceite).
* **Escolas**: tela de escolas ativas e pedidos de vínculo, usando `/Motorista/Escolas` existente.

#### Responsável

* **`GuardianSearchDriverScreen.tsx`**: **remover `MOTORISTAS_MOCK`**; consumir `/Responsavel/Dependentes/{id}/Motoristas`. Mostrar nome, veículo, avaliação média, quantidade de avaliações e status de documentos. Filtro por dependente (escola do dependente).
* **Perfil do motorista** (nova tela): dados limitados, documentos (status/validade) e lista de avaliações. Botão "Solicitar transporte" abre confirmação obrigatória (checkbox): "Ao aceitar, meus dados e os dados do(s) dependente(s) serão compartilhados com o motorista." Sem checkbox marcado, o botão fica desabilitado.
* **`GuardianInvitesScreen.tsx`**: receber solicitações do motorista (10b). Antes de aceitar, mostrar a mesma confirmação de compartilhamento.
* **`GuardianMonitoringScreen.tsx`** / **`GuardianPlansScreen.tsx`**: exibir acordos ativos e botão de encerrar.
* **Avaliação**: após acordo encerrado ou ativo, permitir nota e comentário.

#### Comuns

* **`ProfileScreen.tsx`**: sem mudança funcional.
* **`NoticeBoardScreen.tsx`** / **`NotificationListScreen.tsx`**: exibir novos tipos de notificação e navegar para a solicitação.

### 6.4 Componentes

* `components/ui/`: novo `ConsentimentoCheckbox` (ou reaproveitar `components/forms/Checkbox.tsx`).
* `components/ui/`: novo `DocumentoStatusChip` (usar `Chip`/`StatusBadge` existentes).
* `components/`: novo `AvaliacaoEstrelas` (leitura e entrada).
* `components/forms/`: novo `DependenteAnonimizadoCard`.

### 6.5 Navegação (`navigation/index.ts`)

Novas rotas: `GuardianDriverProfile`, `GuardianDriverRequestConfirm`, `GuardianAgreements`, `DriverAgreements`, `DriverRequestsReceived`, `SchoolDriverDocuments`, `RatingScreen`.

### 6.6 Remoção de mocks

Antes de concluir, validar que nenhuma tela do fluxo usa dados fixos (`*_MOCK`, `setTimeout` simulado).

### 6.7 Busca em cards com detalhe (decidido)

Status: **aprovado pelo usuário** (seção 8, itens 11 a 14). Pendente apenas a implementação.

**Padrão escolhido: lista em cards + tela de detalhe.** Mesmo padrão de apps de delivery: cada card traz só o essencial para decidir se vale abrir, e o toque leva ao detalhe completo. Motivos: funciona bem em telas pequenas, reaproveita `FlatList` existente e mantém a exposição de dados baixa, porque o detalhe só é carregado sob demanda.

Alternativas consideradas:

* **Bottom sheet de prévia** (toque longo ou botão "ver resumo"): bom para comparar rapidamente, mas duplica o conteúdo do detalhe. Pode ser adicionado depois.
* **Mapa com lista**: útil para distância até a escola, porém exige a localização do usuário (já existe `ConsentimentoLocalizacaoDTO`) e aumenta o escopo. Fica fora do MVP.
* **Lista única com filtros e ordenação**: é o núcleo da proposta.

**Card do motorista (visto pelo responsável)**

| Campo | Observação |
|---|---|
| Foto e nome | Nome completo (decidido) |
| Avaliação média | Ex.: ★ 4,8 (23 avaliações) |
| Escola(s) em comum | Escolas atendidas que também são a escola do dependente selecionado |
| Veículo | Modelo e cor (nunca placa) |
| Vagas disponíveis | `veiculo.capacidade` menos dependentes em acordos `ATIVO` |
| Selo de documentos | "Documentos validados" ou alerta de vencimento próximo, sem exibir número ou arquivo |

**Detalhe do motorista (responsável)**

* Dados do motorista: nome completo, foto e tempo na plataforma.
* Avaliações recentes, com comentário, sem dados de outros dependentes.
* Documentos: apenas status e validade de CNH, VISTORIA e LICENCIAMENTO.
* Escolas atendidas e vagas.
* Botão "Solicitar transporte" abre a confirmação de compartilhamento (seção 6.3), que é obrigatória.

**Card do dependente (visto pelo motorista)**

| Campo | Observação |
|---|---|
| Responsável | Nome e avaliação média do responsável |
| Escola | Nome da escola do dependente |
| Turno e região | Região aproximada da escola ou do bairro, sem endereço |
| Dependentes do grupo | Quantidade, para o caso de vários dependentes no mesmo responsável |
| Identificação | Código anonimizado (sem nome, idade exata ou foto do dependente) |
| Série/turma | **Não exibida**, para manter o anonimato |

**Detalhe do dependente (motorista)**

* Responsável: nome, avaliação e quantidade de avaliações.
* Escola, turno e região aproximada.
* Quantidade de dependentes do grupo. Série/turma não é exibida.
* Nenhum contato e nenhum endereço antes do aceite do responsável.

**Busca, filtros e ordenação**

* Responsável: filtro obrigatório por dependente (lista já vem filtrada pela escola do dependente). Ordenação por avaliação, vagas ou nome.
* Motorista: filtros por escola e turno. Ordenação por data de cadastro da solicitação ou por escola.
* Paginação por página ou cursor, com "puxar para atualizar".

**Estados de tela**

* Carregando: skeleton cards.
* Vazio: mensagem explicando o motivo (sem motoristas com vínculo ativo na escola, por exemplo) e orientação para o próximo passo.
* Erro de rede: mensagem com "tentar novamente".
* Dados que mudaram (ex.: motorista perdeu vaga): card removido ao atualizar, com aviso.

**Componentes novos** (complementam 6.4)

* `MotoristaCard` e `DependenteAnonimizadoCard`: componentes de card reutilizáveis entre as listas e o detalhe.
* `FiltroBusca`: barra de filtros com chips e ordenação.
* `ListaVazia` e `SkeletonCard`: estados de carregamento e vazio.

**Endpoints de apoio** (complementam 4.5)

* `GET /Responsavel/Dependentes/{id}/Motoristas?escola=&ordenacao=&pagina=`: retorna o card resumido.
* `GET /Responsavel/Motoristas/{id}`: detalhe.
* `GET /Motorista/Dependentes/Disponiveis?escola=&turno=&pagina=`: retorna o card anonimizado.
* `GET /Motorista/Dependentes/{codigoAnonimo}`: detalhe anonimizado.

---

## 7. Sequência de implementação sugerida

Ordem recomendada, com dependências explícitas:

1. **Banco**: criar enums e tabelas novas (seção 5), script de migração e índices.
2. **Backend base**: entities, enums, repositories (seção 4.1 e 4.3).
3. **Backend escola**: validação de documentos no aprovar vínculo e endpoint de resumo de documentos (depende de 2).
4. **Backend visibilidade**: `MotoristaBuscaService` e endpoints do responsável para listar motoristas (depende de 3).
5. **Backend solicitações**: `SolicitacaoService` nos dois sentidos, consentimento, criação de acordo no aceite (depende de 2 e 4).
6. **Backend anonimização**: `DependenteAnonimizadoService` e troca do retorno de `DependentesDisponiveis` (independente, pode ser paralelo a 5).
7. **Backend avaliações** (depende de 5).
8. **Backend notificações e segurança** (`AutorizacaoRecursoService`) (transversal, fazer junto de 5).
9. **Frontend serviços e tipos** (depende dos endpoints de 3–7).
10. **Frontend telas** da escola, motorista e responsável (depende de 9).
11. **Remoção de mocks** e ajustes de navegação.
12. **Testes**: unitários de service (regras das seções 3 e 4.8), integração de controllers e testes E2E do fluxo completo (escola → motorista → responsável → acordo → avaliação).
13. **Documentação de API**: atualizar `API_DOCUMENTACAO_COMPLETA_V3.md` e a coleção Postman (`Docs/postman/`), conforme `scripts/gerar-colecao-postman.js`.

---

## 8. Ideias complementares e pontos a decidir

1. **Expiração (decidido)**: solicitações expiram em 3 dias, via job agendado, similar a `PasswordResetCleanupService`.
2. **Múltiplos dependentes (decidido: Opção A)**: solicitação agrupada com itens por dependente, conforme seção 3.9.
3. **Veículo (decidido)**: responsável vê apenas modelo e cor. Placa não é exibida.
4. **Localização**: `LocalizacaoViagemService` e o consentimento existente (`ConsentimentoLocalizacaoDTO`) podem ser reaproveitados para exigir consentimento de localização como parte do acordo.
5. **Motorista avaliar responsável**: fora do escopo atual; pode ser adicionado depois para simetria.
6. **Recusa com motivo**: campo opcional de motivo na recusa, visível ao remetente.
7. **Denúncia**: responsável ou motorista pode reportar comportamento inadequado, encaminhando para a escola e para o administrador.
8. **Sugestão de motoristas (aprovado)**: ordenar por média de avaliação, proximidade da escola e vagas disponíveis. Vagas = `veiculo.capacidade` menos dependentes em acordos `ATIVO`.
9. **Escola pode revogar acordo (aprovado)**: se a escola suspender o vínculo, o acordo é encerrado e as partes são notificadas.
10. **Limite de dependentes (corrigido)**: a capacidade é definida pelo motorista em `Veiculo.capacidade`, já validada em `ViagemService`. A escola não define limite. Não há campo `capacidadeMaxima` em `vinculo_escola`.
11. **Card do motorista (decidido)**: nome completo, escolas em comum e vagas disponíveis no card.
12. **Card do dependente (decidido)**: região aproximada e turno no card. Série/turma **não** aparece no detalhe, mantendo o dependente anonimizado.
13. **Padrão de lista (decidido)**: cards com tela de detalhe. Bottom sheet de prévia fica para depois.
14. **Ordenação e filtros (decidido)**: avaliação, vagas e nome entram no MVP.

---

## 9. Critérios de aceite

* [ ] Escola só aprova motorista com documentos obrigatórios aprovados e válidos.
* [ ] Responsável só vê motoristas com vínculo `ATIVO` com a escola do dependente.
* [ ] Responsável não envia solicitação sem confirmar compartilhamento.
* [ ] Motorista vê dependentes apenas anonimizados e com responsável identificado, antes do aceite.
* [ ] Após aceite, dados são compartilhados nos dois sentidos e um acordo é criado.
* [ ] Recusa é comunicada ao remetente.
* [ ] Encerramento de vínculo com a escola encerra os acordos correspondentes.
* [ ] Avaliação só é possível para acordos existentes, uma por responsável por acordo.
* [ ] Nenhuma tela do fluxo usa dados mock.
* [ ] Testes cobrindo regras de negócio e autorização.

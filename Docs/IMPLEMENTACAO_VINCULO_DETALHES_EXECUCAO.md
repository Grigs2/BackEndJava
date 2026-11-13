# Início da viagem sem documentos, vínculo aceito que reaparecia e pop-up da outra parte (API)

**Branch:** `fix/vinculo-detalhes-execucao` (criada a partir de `homologacao`) · **Data:** 13/11/2026
**Situação:** implementado, **156 testes unitários passando**. Sem commit e sem push, aguardando aprovação.
**App correspondente:** frontendreact, mesma branch (`docs/IMPLEMENTACAO_VINCULO_DETALHES_EXECUCAO.md`).

## 1. Problemas relatados

1. Ao iniciar a viagem: *"Execução bloqueada: Documento CNH pendente, recusado ou vencido; … Não há vínculo operacional ativo com a escola 1"*. Ainda não há onde o motorista envie os documentos nem quem os valide.

> **Decisão (13/11/2026):** por enquanto o motorista **precisa** ter vínculo ativo com a escola. Só os documentos deixaram de bloquear.
2. Depois de aceitar o motorista, o aluno continuava aparecendo com "Pedir vaga" na busca e com convite para aceitar.
3. Antes de aceitar um vínculo, quem responde deve ver os dados gerais da outra pessoa (como um contato), sem dados sensíveis: só nome e telefone completos.

## 2. O que mudou

### 2.1 Documentos deixam de bloquear (vínculo com a escola continua obrigatório)

| Arquivo | Mudança |
|---|---|
| `services/ViagemService.java` | `coletarBloqueiosProntidao(viagem, avisos)`: pendências de documentos (`DocumentoMotoristaService.listarPendenciasParaInicio`) vão para `avisos`, não para `bloqueios`. Iniciar a viagem (`EM_ANDAMENTO`) só considera os bloqueios. A falta de vínculo ativo com a escola **continua bloqueando**; o texto agora traz o nome da escola: "Não há vínculo ativo com a escola &lt;nome&gt;". |
| `DTOs/ProntidaoViagemDTO.java` | Novo campo `avisos` (o construtor antigo de 3 argumentos continua existindo). |

Continuam bloqueando: viagem desativada, sem veículo, capacidade zero ou excedida, sem alunos, endereço sem CEP validado (motorista, escola ou aluno), aluno sem escola e **motorista sem vínculo ativo com a escola**. Convite, pedido de vaga e a busca de motoristas também continuam exigindo esse vínculo (sem mudança).

A validação de documentos pelo administrador (`/Admin/Documentos/...`) continua existindo. Só deixou de impedir a viagem.

### 2.2 Vínculo aceito não reaparece

| Arquivo | Mudança |
|---|---|
| `services/SolicitacaoService.java` | Ao aceitar, `encerrarDuplicadas`: outras solicitações em aberto do **mesmo aluno na mesma viagem** (ex.: convite do motorista e pedido do responsável ao mesmo tempo, ou registros antigos) são encerradas (`respondido=true`, `status=RECUSADA`, `dataFim=agora`) e somem das listas. |
| `repositories/SolicitacaoRepository.java` | `findPendentesByDependenteId(dependenteId)`. |
| `DTOs/MotoristaDisponivelDTO.java` | Novos campos `viagensComAluno` e `viagensComSolicitacaoPendente` (ids de viagem). Com eles o app troca "Pedir vaga" por "Já está nesta viagem" ou "Aguardando resposta". |

### 2.3 Pop-up com os dados da outra parte

| Arquivo | Mudança |
|---|---|
| `DTOs/ContraparteDTO.java` (novo) | Nome, telefone, e-mail mascarado, CPF mascarado, bairro/cidade/UF, viagem, escolas; do motorista: veículo (placa mascarada) e avaliação média; do aluno: nome, idade, escola, período, bairro e cidade. |
| `services/ContraparteService.java` (novo) | `motoristaDaSolicitacao` (só responsáveis do aluno) e `responsavelDaSolicitacao` (só o motorista da viagem). Outros usuários recebem 403. |
| `controllers/ResponsavelController.java` | `GET /Responsavel/Solicitacoes/{id}/Contraparte`: o responsável vê o motorista. |
| `controllers/MotoristaController.java` | `GET /Motorista/Solicitacoes/{id}/Contraparte`: o motorista vê o responsável e o aluno. |

**Regras de privacidade do pop-up:**

| Dado | Como aparece | Exemplo |
|---|---|---|
| Nome | completo | Carlos Souza |
| Telefone | completo | (11) 97777-6666 |
| CPF | mascarado | `***.444.777-**` |
| E-mail | mascarado | `ca***@exemplo.dev` |
| Placa | mascarada | `ABC****` |
| Endereço | só bairro e cidade | Ipiranga, São Paulo/SP |
| CPF e endereço completo do aluno | não aparecem | |

## 3. Testes

- `ViagemDocumentGateTest` (+3): a viagem inicia com documento pendente; não inicia sem vínculo com a escola; a prontidão devolve os documentos em `avisos`.
- `SolicitacaoBidirecionalTest` (+1): encerramento de duplicadas ao aceitar. O teste que exige vínculo com a escola continua como estava.
- `ContraparteServiceTest` (novo, 4 casos): máscaras, acesso só para participantes, dados de cada lado.
- Suíte: `./mvnw test -Dtest='!BackEndJavaApplicationTests,!SecurityAuthorizationIntegrationTest'` → **156 testes, 0 falhas**. Os testes de contexto precisam de PostgreSQL e não rodaram.

## 4. Banco

Nenhuma tabela ou coluna nova.

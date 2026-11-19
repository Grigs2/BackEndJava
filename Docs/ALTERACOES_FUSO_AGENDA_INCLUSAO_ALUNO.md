# Alterações: fuso de Brasília, agenda por intervalo e inclusão automática de alunos

Este documento descreve as alterações não commitadas feitas na branch `feat/redesenho-motorista` em três frentes:

1. Datas e horários do sistema passam a seguir o fuso `America/Sao_Paulo`.
2. A agenda do motorista aceita um intervalo opcional (`de` e `ate`).
3. Alunos aceitos depois da preparação de uma execução passam a entrar nas execuções ainda planejadas.

---

## 1. Fuso horário de Brasília

### Motivação

Os métodos `LocalDate.now()` e `LocalDateTime.now()` usam o fuso do servidor. Em um servidor em UTC, entre 21h e 24h de Brasília o "dia de hoje" da agenda já virava para o dia seguinte.

### Componente novo

[DataBrasilia.java](/workspaces/BackEndJava/src/main/java/TioDaPerua/BackEndJava/config/DataBrasilia.java) centraliza o cálculo:

| Método | Retorno |
|---|---|
| `FUSO` | `ZoneId.of("America/Sao_Paulo")` |
| `hoje()` | `LocalDate.now(FUSO)` |
| `agora()` | `LocalDateTime.now(FUSO)` |

### Pontos substituídos

Todas as chamadas `LocalDate.now()` e `LocalDateTime.now()` de regra de negócio foram trocadas por `DataBrasilia.hoje()` ou `DataBrasilia.agora()`, em:

- DTOs: `DependenteAnonimoDTO` (idade), `DocumentoMotoristaDTO` (status vencido).
- Handlers: `GlobalExceptionHandler` e `SecurityJsonExceptionHandler` (campo `timestamp` da resposta de erro).
- Controller: `ViagemController` (padrão do histórico por motorista).
- Services: `AusenciaService`, `AvaliacaoService`, `ContraparteService`, `DependenteService`, `DocumentoMotoristaService`, `HistoricoService`, `MotoristaService`, `NotificacaoService`, `ResponsavelService`, `SolicitacaoService`, `ViagemService`.

### Caso especial em `ViagemService`

`ViagemService` tem o campo `relogio`, um `Clock` de Brasília usado nas operações de agenda e preparação (`LocalDate.now(relogio)`). Ele existe para que os testes possam fixar o instante via `ReflectionTestUtils`. Não é um parâmetro de produção.

---

## 2. Agenda do motorista com intervalo

### Endpoint

`GET /Viagem/Agenda/{idMotorista}` aceita os parâmetros opcionais `de` e `ate` (formato `yyyy-MM-dd`).

| Parâmetros | Intervalo resultante |
|---|---|
| Nenhum | Hoje (Brasília) até hoje + 6 dias (7 dias no total) |
| Só `de` | `de` até `de` + 6 dias |
| `de` e `ate` | `de` até `ate`, com as duas pontas inclusivas |
| `ate` anterior a `de` | Erro de negócio (HTTP 400) |

### Alterações

- [ViagemController.java](/workspaces/BackEndJava/src/main/java/TioDaPerua/BackEndJava/controllers/ViagemController.java): recebe `de` e `ate` como `@RequestParam` e repassa ao service.
- [ViagemService.java](/workspaces/BackEndJava/src/main/java/TioDaPerua/BackEndJava/services/ViagemService.java): `agendaMotorista(idMotorista, de, ate)` calcula o intervalo e valida a ordem das datas.
- [API_DOCUMENTACAO_COMPLETA_V3.md](/workspaces/BackEndJava/Docs/API_DOCUMENTACAO_COMPLETA_V3.md): tabela de endpoints e seção "Agenda virtual e prontidão" atualizadas.

A agenda continua virtual: itens sem execução retornam `materializada: false`, `idViagemDia: null` e `status: null`. Dias já executados mantêm o status real.

---

## 3. Inclusão automática de aluno em execuções planejadas

### Problema

Quando um responsável aceita uma solicitação depois que a execução do dia já foi preparada, o aluno recém-aceito não tinha presença nem parada nessa execução. Ele só aparecia nas execuções seguintes.

### Solução

Ao aceitar a solicitação, `SolicitacaoService` chama o novo método `ViagemService.incluirDependenteEmExecucoesPlanejadas(idViagem, idDependente)`, que:

1. Busca as execuções da viagem com status `PLANEJADA` a partir de hoje (Brasília). Usa a consulta nova `ViagemDiaRepository.findByViagemIdAndStatusAndDataGreaterThanEqual`.
2. Confirma que o dependente tem vínculo ativo com a viagem.
3. Para cada execução, cria `ViagemPresenca` (status `ESPERANDO`) para os alunos vinculados que ainda não têm presença, com snapshots de endereço.
4. Aplica ausências já informadas para os novos alunos.
5. Apaga e reconstrói as paradas da execução, para que a rota inclua o novo aluno.

### Regras

- Execuções `EM_ANDAMENTO`, `FINALIZADA` ou `CANCELADA` não são alteradas. Mantêm o snapshot original.
- Se a execução já foi reordenada (existe `HistoricoOrdemParada`), o método lança `ConflictException`. A inclusão automática não é feita nesse caso.
- Se não há execuções planejadas ou o dependente não está vinculado, o método não faz nada.
- O método é `@Transactional`.

### Testes de unidade

`SolicitacaoBidirecionalTest` e `SolicitacaoNotificationTest` ganharam o mock `ViagemService`, exigido pelo novo `@Autowired` em `SolicitacaoService`.

---

## 4. Testes alterados

[ViagemAgendaTest.java](/workspaces/BackEndJava/src/test/java/TioDaPerua/BackEndJava/services/ViagemAgendaTest.java):

- `showsSevenBrasiliaDatesWithoutMaterializingFutureExecutions`: fixa o relógio em 18/11 23h40 de Brasília (19/11 02h40 UTC) e confirma que o primeiro dia da agenda é 18/11.
- `explicitRangeIsInclusiveAtBothEnds`: confirma que `de` e `ate` entram no intervalo.
- `rejectsRangeEndingBeforeStart`: confirma o erro quando `ate` é anterior a `de`, sem consulta ao banco.

---

## 5. Pontos de atenção

- Os testes acima não foram executados nesta documentação. Antes de homologar, rode `./mvnw test -Dtest=ViagemAgendaTest,SolicitacaoBidirecionalTest,SolicitacaoNotificationTest`.
- A inclusão automática não cobre o caso de execução já reordenada. Nesse caso o motorista precisa preparar a execução novamente ou o aluno fica de fora daquele dia.
- As alterações ainda não estão commitadas. A pasta `scripts/seed-perfis-teste/` não tem modificações nesta leva.

# Redesenho do Responsável — ajustes na API

**Branch:** `feat/redesenho-responsavel` (criada a partir de `homologacao`) · **Data:** 18/11/2026
**Situação:** implementado, **172 testes unitários passando**. Sem commit e sem push.
**App correspondente:** frontendreact, mesma branch (`docs/IMPLEMENTACAO_REDESENHO_RESPONSAVEL.md`).

O redesenho é quase todo no app. Duas regras precisavam do backend e foram autorizadas pelo dono do produto em 18/11/2026.

## 1. CPF completo do motorista com vínculo ativo

O pop-up "Dados do motorista" mostrava o CPF censurado mesmo depois de o responsável já ter contratado o motorista. O telefone já vinha completo.

| Arquivo | Mudança |
|---|---|
| `DTOs/ContraparteDTO.java` | Novos campos `cpf` (completo) e `vinculoAtivo`. `cpfMascarado` continua sendo enviado, para compatibilidade. |
| `services/ContraparteService.java` | `vinculoAtivo(s)`: aceita, sem `dataFim` e com status `ACEITA`. Só em `motoristaDaSolicitacao` (o responsável vendo o motorista) e só com vínculo ativo, `cpf` vem preenchido (`formatarCpf`, `000.000.000-00`). O motorista vendo o responsável nunca recebe o CPF completo. |

E-mail e placa continuam mascarados; não foi pedido mudar.

## 2. Duas viagens no mesmo período

O responsável conseguia aceitar duas viagens do mesmo período (ex.: duas de manhã-ida). Como cada viagem se repete em todas as datas, o aluno ficava em duas execuções na mesma data e período.

| Arquivo | Mudança |
|---|---|
| `repositories/ViagemDependenteRepository.java` | `existeAcordoAtivoNoPeriodo(dependenteId, periodo, viagemId)`: acordo ativo do aluno em outra viagem ativa com o mesmo `periodo`. |
| `services/SolicitacaoService.java` | `registrarResposta` chama `validarPeriodoLivre` ao aceitar, nos dois sentidos (responsável aceita convite; motorista aceita pedido de vaga). Conflito → `ConflictException` (409) com "&lt;nome&gt; já tem uma viagem ativa neste período. Encerre o vínculo atual antes de aceitar outro." Recusar não verifica. Viagens sem período (dados legados) não são bloqueadas. |

Não foi adicionada restrição ao **pedir** vaga ou **convidar**: a checagem fica no aceite, que é quando o acordo nasce. O app já avisa e desabilita o aceite antes (ver o documento do app).

## 3. Testes

- `ContraparteServiceTest`: +3 casos (CPF completo com vínculo ativo; vínculo encerrado volta a mascarar; motorista nunca recebe CPF completo do responsável) e asserções de `cpf`/`vinculoAtivo` no caso de convite pendente.
- `SolicitacaoBidirecionalTest`: +4 casos (responsável não aceita segunda viagem no mesmo período; motorista não aceita pedido com período ocupado; aceita viagem de outro período; recusar não verifica período).
- `./mvnw.cmd test`: 172 testes, 0 falhas.
- A consulta JPQL nova (`existeAcordoAtivoNoPeriodo`) é validada na subida do contexto Spring (`BackEndJavaApplicationTests` e `SecurityAuthorizationIntegrationTest` passaram), mas o resultado só foi exercitado com mocks. Validar com dados reais no Codespace: dois itinerários do mesmo período e o mesmo aluno.

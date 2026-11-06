# Viagem por escola, período MANHA_VOLTA, busca de motoristas e solicitações nos dois sentidos

**Branch:** `feat/busca-solicitacao-viagem-escola` (criada a partir de `origin/homologacao`) · **Data:** 05–06/11/2026
**Situação:** implementado, com testes unitários passando. **Sem commit e sem push**, aguardando aprovação.
**Frontend correspondente:** frontendreact, branch `feat/inicio-busca-cadastros` (`docs/IMPLEMENTACAO_INICIO_BUSCA_CADASTROS.md`).

---

## 1. Motivação

O app passou a ter:
- viagem criada para uma escola ("Viagem para <escola>") e um período igual ao do banco, incluindo "manhã volta";
- busca de motoristas pelo responsável, por proximidade ou por nome;
- pedido de vaga feito pelo responsável, e não só o convite feito pelo motorista;
- busca de alunos por nome pelo motorista.

Nenhum desses recursos existia na API.

## 2. Mudanças no modelo

| Arquivo | Mudança |
|---|---|
| `enums/Periodo.java` | Novo valor `MANHA_VOLTA`. É gravado como texto (`EnumType.STRING`) e tratado como viagem de volta na geração das paradas (só os `*_IDA` são ida) |
| `enums/OrigemSolicitacao.java` (novo) | `MOTORISTA` ou `RESPONSAVEL`: quem pediu. Quem não pediu é quem responde |
| `entities/Viagem.java` | `escola` (`@ManyToOne`, coluna `escola_id`, pode ser nula) e `getNome()` → "Viagem para <escola>" |
| `entities/Solicitacao.java` | `origem` (coluna `origem`, VARCHAR(20), pode ser nula) e `getOrigemEfetiva()`: registro antigo sem origem conta como `MOTORISTA` |

**Banco:** com `spring.jpa.hibernate.ddl-auto=update`, o Hibernate cria `viagem.escola_id` (com FK) e `solicitacao.origem` ao iniciar. Os dados existentes continuam válidos.

**Atenção (MANHA_VOLTA):** em um banco criado antes, a coluna `viagem.periodo` pode ter uma restrição antiga (`viagem_periodo_check`) com só os 5 valores anteriores. O `update` não altera essa restrição, e criar uma viagem `MANHA_VOLTA` falharia. Correção manual:

```sql
ALTER TABLE public.viagem DROP CONSTRAINT IF EXISTS viagem_periodo_check;
```

**Atualização:** isso agora é feito automaticamente na inicialização por `config/CorrecaoEsquemaBanco.java` (ver `Docs/IMPLEMENTACAO_NOTIFICACOES_HISTORICO_TERMOS.md`). Bancos novos não têm o problema.

## 3. DTOs

| DTO | Mudança |
|---|---|
| `ViagemDTO` | + `escolaId` (enviado na criação), `escolaNome`, `nome` |
| `ViagemDetalhadaDTO` | + `escolaId`, `escolaNome`, `nome` e **`ativo`**. Antes faltava `ativo`, e o app mostrava toda viagem como "Inativa" |
| `SolicitacaoDTO` | + `origem`, `dependenteNome`, `viagemNome`, `viagemPeriodo`, `motoristaId`, `motoristaNome`, `responsavelNome` (só exibição; ignorados quando o DTO chega como corpo) |
| `EscolaBuscaDTO` | + `id`, necessário para criar a viagem da escola |
| `MotoristaDisponivelDTO` (novo) | Dados públicos do motorista para o responsável: nome, veículo (modelo, cor, lugares), bairro/cidade/UF, `proximidade` e viagens ativas. Sem CPF, telefone, e-mail ou endereço completo |
| `ResponderSolicitacaoDTO` (novo) | `{ aceito }` |

## 4. Endpoints

| Método e caminho | Autorização | Descrição |
|---|---|---|
| `POST /Viagem/CriarViagem/{idMotorista}` | dono | **Mudou:** aceita `escolaId`; a viagem passa a ser "Viagem para <escola>" |
| `GET /Viagem/DependentesDisponiveis/{idViagem}?nome=` | dono da viagem | **Mudou:** filtro opcional por nome (sem diferença de maiúsculas e acentos) e, se a viagem tem escola, só alunos dessa escola |
| `POST /Viagem/SolicitarDependente/{idViagem}/{idDependente}` | dono da viagem | **Mudou:** registra `origem = MOTORISTA`, valida a escola, recusa aluno que já está na viagem e **notifica o responsável** |
| `GET /Responsavel/{idResponsavel}/Motoristas/Buscar?nome=&idDependente=&somenteProximos=` | o próprio responsável | **Novo:** busca de motoristas com veículo e conta ativa, ordenada por proximidade e nome |
| `POST /Responsavel/SolicitarTransporte/{idViagem}/{idDependente}` | responsável do aluno | **Novo:** pedido de vaga (`origem = RESPONSAVEL`); notifica o motorista |
| `GET /Motorista/{idMotorista}/Solicitacoes` | o próprio motorista | **Novo:** solicitações sem resposta das viagens dele (pedidos recebidos e convites enviados) |
| `POST /Motorista/Solicitacoes/{id}/Responder` `{aceito}` | motorista da viagem (conferido no serviço) | **Novo:** aceita ou recusa o pedido; ao aceitar, inclui o aluno na viagem e notifica o responsável |
| `POST /Responsavel/ResponderSolicitacao/{id}` | responsável da solicitação | **Mudou:** recusa responder um pedido que o próprio responsável fez |

## 5. Regras de negócio

1. **Escola da viagem:** só alunos da mesma escola entram na viagem, nos dois sentidos ("Esta viagem vai para X, e o aluno estuda em outra escola"). Viagens antigas, sem escola, não têm essa restrição.
2. **Sem duplicidade:** não pode haver duas solicitações pendentes para o mesmo aluno na mesma viagem, nem solicitação para aluno que já está nela.
3. **Quem responde:** o pedido do motorista é respondido pelo responsável, e o do responsável pelo motorista da viagem. Ninguém responde o próprio pedido.
4. **Pedido de vaga:** só o responsável do aluno e só em viagem ativa.
5. **Proximidade (`BuscaMotoristaService`):**
   - compara bairro e cidade do **endereço operacional do motorista** com:
     - o endereço estruturado do aluno (`endereco_dependente`), quando existir;
     - ou, se não existir, o endereço em texto do aluno ou do responsável;
   - resultados possíveis: `MESMO_BAIRRO`, `MESMA_CIDADE`, `OUTRA_CIDADE` ou `SEM_REFERENCIA`;
   - não há GPS nem distância em km, porque o sistema não guarda coordenadas de endereço;
   - `somenteProximos=true` mantém só mesmo bairro e mesma cidade.

## 6. Testes

- Novos:
  - `SolicitacaoBidirecionalTest` (10 casos): pedido do responsável, outra escola, aluno de outro responsável, aluno já na viagem, aceite pelo motorista, quem pode responder o quê, convite do motorista com notificação, filtro por escola e nome sem acento;
  - `BuscaMotoristaProximidadeTest` (3 casos).
- `./mvnw test`: **123 testes unitários passaram**, incluindo os que já existiam de solicitação e viagem.
- 9 testes que sobem o contexto completo (`BackEndJavaApplicationTests` e `SecurityAuthorizationIntegrationTest`) falharam por **falta de PostgreSQL** na máquina de teste ("Unable to determine Dialect without JDBC metadata"), não por estas mudanças. Precisam ser rodados com o banco.
- Não foi feito teste de ponta a ponta com o banco real. O app foi testado com uma simulação da API.

## 7. Arquivos

- **Alterados:** `EscolaBuscaDTO`, `SolicitacaoDTO`, `ViagemDTO`, `ViagemDetalhadaDTO`, `MotoristaController`, `ResponsavelController`, `ViagemController`, `Solicitacao`, `Viagem`, `Periodo`, `MotoristaRepository` (`findAtivosComVeiculo`), `SolicitacaoRepository` (`findAbertasByMotoristaId`), `SolicitacaoService`, `ViagemService` (`criarViagem` com escola).
- **Novos:** `MotoristaDisponivelDTO`, `ResponderSolicitacaoDTO`, `OrigemSolicitacao`, `BuscaMotoristaService` e os dois testes.

## 8. Pendências

1. Correção automática da restrição `viagem_periodo_check` (item 2), aguardando aprovação.
2. Rodar a suíte completa com PostgreSQL e atualizar a coleção do Postman com os endpoints novos.
3. Próximos passos de notificações (push, motorista próximo, ausência informada, atraso) estão especificados no frontend: `docs/ESPECIFICACAO_VALIDACAO_NOTIFICACOES_HISTORICO.md`.

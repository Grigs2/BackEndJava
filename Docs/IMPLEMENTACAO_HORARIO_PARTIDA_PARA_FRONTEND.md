# Horário de partida da viagem - guia para o front

Este documento descreve o que o back-end expõe para o horário de partida. Ele substitui a necessidade de inferir regras a partir do código.

## 1. Conceitos

| Conceito | Onde fica | Descrição |
|---|---|---|
| Horário padrão da viagem | `Viagem.horarioPartidaPadrao` | Horário do itinerário. Vem do período na criação, se não for informado. |
| Horário próprio da execução | `ViagemDia.horarioPartida` | Ajuste de uma data específica. Ausente (`null`) quando a execução usa o padrão. |
| Horário efetivo | Campo `horarioPartida` nas respostas | `horarioPartida próprio ?? horarioPartida padrão` |

Formato: sempre `HH:mm` (24 horas, por exemplo `07:00`, `17:30`). Não há fuso horário nem data no campo; o horário é o da viagem em Brasília.

Padrões por período, aplicados quando `horarioPartida` não é enviado na criação:

| Período | Horário padrão |
|---|---|
| `MANHA_IDA`, `MANHA_VOLTA` | `07:00` |
| `TARDE_IDA`, `TARDE_VOLTA` | `12:00` |
| `NOITE_IDA`, `NOITE_VOLTA` | `17:00` |

Viagens criadas antes desta mudança recebem o padrão do período na migração de inicialização. Execuções já existentes não são gravadas; elas resolvem o horário na leitura.

## 2. Regras de negócio

1. **Alterar o padrão** (`AlterarHorarioPartida`) não altera execuções que já começaram, que já passaram ou que têm horário próprio. Execuções futuras e planejadas sem horário próprio passam a usar o novo padrão.
   - Internamente, antes da troca, as execuções planejadas e futuras sem horário próprio recebem o horário antigo gravado. Assim o histórico não muda.
2. **Alterar o horário de uma data** (`AlterarHorarioPartidaDia`):
   - A data deve estar entre hoje e hoje + 6 dias (Brasília).
   - A execução deve estar `PLANEJADA`. Em `EM_ANDAMENTO`, `FINALIZADA`, `CANCELADA` ou `INTERROMPIDA` a alteração é recusada.
   - Se a execução ainda não existe, ela é preparada automaticamente antes de receber o horário.
   - `horarioPartida: null` remove o ajuste e a execução volta a usar o padrão da viagem.
3. O horário de partida **não bloqueia** o início da viagem. Ele é apenas informativo.
4. Viagens desativadas (`ativo=false`) não aceitam alteração de horário.
5. Somente o motorista dono da viagem pode alterar horários (mesma regra dos demais endpoints de `/Viagem`).
6. A notificação de início ("A caminho") informa o horário previsto quando ele existe:
   - Exemplo: `Partida prevista às 07:00.`
   - A mensagem usa o horário efetivo da execução.

## 3. Endpoints

Base: `/TioDaPerua/api`. Todos exigem `Authorization: Bearer <accessToken>`.

### 3.1 Criar viagem (alterado)

`POST /Viagem/CriarViagem/{id_motorista}`

Campo novo opcional no corpo:

```json
{
  "periodo": "MANHA_IDA",
  "nome": "Rota Centro",
  "ativo": true,
  "escolas": [],
  "horarioPartida": "07:15"
}
```

- `horarioPartida` é opcional. Se omitido, usa o padrão do período.
- Se enviado com formato inválido, a resposta é `400`.
- A resposta é `ViagemDTO` com `horarioPartida` preenchido.

### 3.2 Alterar horário padrão da viagem (novo)

`PUT /Viagem/AlterarHorarioPartida/{idViagem}`

Corpo:

```json
{ "horarioPartida": "07:30" }
```

Resposta `200`: `ViagemDTO` atualizado.

### 3.3 Alterar horário de uma data (novo)

`PUT /Viagem/AlterarHorarioPartidaDia/{idViagem}`

Corpo:

```json
{ "data": "2026-11-23", "horarioPartida": "07:45" }
```

Para voltar ao padrão, envie `"horarioPartida": null`.

Resposta `200`: `ViagemDiaDTO`:

```json
{
  "id": 42,
  "data": "2026-11-23",
  "status": "PLANEJADA",
  "dataUltimaAlteracaoStatus": "2026-11-21T07:30:00",
  "viagemId": 8,
  "periodo": "MANHA_IDA",
  "horarioPartida": "07:45",
  "horarioPartidaPersonalizado": true
}
```

- `horarioPartidaPersonalizado = true` quando a execução tem horário próprio.
- Ao remover o ajuste (`null`), `horarioPartida` volta ao padrão e `horarioPartidaPersonalizado` vira `false`.

### 3.4 Agenda do motorista (campo novo)

`GET /Viagem/Agenda/{idMotorista}?de=yyyy-MM-dd&ate=yyyy-MM-dd`

Cada item de `AgendaViagemDTO` ganhou `horarioPartida` (horário efetivo, mesmo quando a execução não está materializada):

```json
{
  "idViagem": 8,
  "periodo": "MANHA_IDA",
  "data": "2026-11-23",
  "idViagemDia": null,
  "status": null,
  "materializada": false,
  "horarioPartida": "07:00"
}
```

### 3.5 Outros endpoints com o campo

| Endpoint | Tipo de resposta | Campo |
|---|---|---|
| `GET /Viagem/ListarViagens/{idMotorista}` | `ViagemDTO` | `horarioPartida` (padrão) |
| `GET /Viagem/Visualizar/{id}` | `ViagemDTO` | `horarioPartida` (padrão) |
| `GET /Viagem/VisualizarViagemDia/{id}` | `ViagemDiaDTO` | `horarioPartida`, `horarioPartidaPersonalizado` |
| `GET /Viagem/Historico/{idMotorista}` | `ViagemDiaResumoDTO` | `horarioPartida` (efetivo) |

## 4. Erros

O corpo de erro segue o formato global da API (`status`, `error`, `message`, `path`).

| Situação | Status | Mensagem (exemplo) |
|---|---|---|
| `horarioPartida` fora de `HH:mm` (por exemplo `7h`, `25:00`) | 400 | `Horário de partida deve estar no formato HH:mm` |
| Data fora da janela de hoje até +6 dias | 400 | `A alteração do horário deve ocorrer entre hoje e os próximos 6 dias` |
| Execução não está `PLANEJADA` | 409 | `O horário de partida só pode ser alterado enquanto a execução estiver planejada` |
| Viagem desativada | 409 | `Não é possível alterar o horário de um itinerário desativado` |
| Viagem não encontrada | 404 | `Viagem não encontrada com id: ...` |
| Usuário não é o motorista dono | 403 | `Acesso não autorizado para este recurso` |

## 5. Sugestão de uso no front

- Exibir `horarioPartida` como "Partida prevista às HH:mm" em telas do motorista e do responsável.
- Na tela de agenda, usar `horarioPartida` de cada item. Quando `materializada=false`, o horário ainda é o padrão.
- Desabilitar a edição da data quando o `status` não for `PLANEJADA`.
- Ao alterar o padrão, avisar o motorista de que execuções já iniciadas, passadas ou com horário próprio não mudam.
- Ao exibir uma execução com `horarioPartidaPersonalizado=true`, destacar que o horário foi ajustado para aquela data.

## 6. Pontos em aberto

- **Nomes dos campos de resumo:** `ViagemDiaResumoDTO` continua com `inicio` e `fim` (datas e horas de início e fim da execução). O contrato do front cita `horarioInicio` e `horarioFim`. Ainda não foi decidido se o back vai renomear ou se o front adapta o mapeamento.
- **Histórico de alteração de horário:** as mudanças de horário não geram registro próprio no histórico de status. Hoje não há auditoria de quem alterou o horário.
- **Notificação de mudança:** alterar o horário não notifica os responsáveis.

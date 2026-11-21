# Contrato backend: linha de estações do responsável

Proposta para validação do front antes da implementação. Contrato de referência no front: `docs/CONTRATO_LINHA_RESPONSAVEL.md` (branch `feat/full-map-guardian` do `frontendreact`).

## Endpoint

```
GET /Viagem/ViagemDia/{viagemDiaId}/Linha?dependenteId={id}
```

- Autorização: mesma regra da rota `GET /Viagem/ViagemDia/{viagemDiaId}/Rota`. Só o responsável do `dependenteId` e só com consentimento de localização desse aluno.
- Erros: mesmo padrão da rota, reaproveitando as validações existentes (`podeAcessarExecucao`, `validarLeituraLocalizacao`).
- `dependenteId` que não pertence ao usuário logado: a definir (proposta: 403).
- Não depende da API do Google. Responde mesmo quando a rota do Google falha.

## Resposta: `LinhaViagemDTO`

```json
{
  "disponivel": true,
  "mensagem": null,
  "indiceAtual": 3,
  "paradas": [
    { "ordem": 1, "tipoParada": "RESPONSAVEL", "acao": "EMBARQUE", "concluida": true, "doFilho": false, "rotulo": null },
    { "ordem": 3, "tipoParada": "RESPONSAVEL", "acao": "EMBARQUE", "concluida": false, "doFilho": true, "rotulo": "Casa do Enzo" },
    { "ordem": 4, "tipoParada": "ESCOLA", "acao": "DESEMBARQUE", "concluida": false, "doFilho": false, "rotulo": "Colégio X" }
  ]
}
```

| Campo | Tipo | Regra |
|---|---|---|
| `disponivel` | boolean | `false` quando a viagem não está em andamento ou não há dados suficientes. |
| `mensagem` | string \| null | Texto para exibir quando `disponivel` é `false`. |
| `indiceAtual` | int \| null | `ordem` da parada atual do motorista. `null` antes de sair ou depois de concluir a viagem. |
| `paradas[]` | array | Todas as paradas da viagem, na ordem da rota, sem filtrar pelo aluno. |
| `paradas[].ordem` | int | Posição na rota, começando em 1. |
| `paradas[].tipoParada` | `MOTORISTA` \| `RESPONSAVEL` \| `ESCOLA` | Tipo da parada. |
| `paradas[].acao` | `EMBARQUE` \| `DESEMBARQUE` | Ida: embarque nas casas e desembarque na escola. Volta: o contrário. |
| `paradas[].concluida` | boolean | Concluída quando ninguém mais aguarda na parada e ninguém a bordo deveria desembarcar nela. Mesma regra de `paradaConcluida` do motorista. |
| `paradas[].doFilho` | boolean | `true` só para a parada do aluno consultado. |
| `paradas[].rotulo` | string \| null | Ver regra de privacidade. |

## Regra de privacidade (obrigatória)

- `rotulo` é preenchido só em dois casos:
  - `tipoParada = ESCOLA`: nome da escola.
  - `doFilho = true`: `Casa do {primeiroNome}` do aluno consultado.
- Nas demais paradas, `rotulo` é `null`. O front mostra "Parada N".
- Endereço nunca entra nesta resposta, nem para a parada do aluno.
- Coordenadas também não entram. A posição da perua continua vindo de `GET /Viagem/Localizacao/Atual/{viagemDiaId}`.

## Regras de negócio

- `paradas` segue a mesma ordem e os mesmos dados da rota do motorista. Reordenação via `PUT /Viagem/ReordenarParadasViagemDia/{idViagemDia}` reflete na resposta.
- Paradas com alunos que faltaram ficam `concluida: true` quando a viagem passa delas.
- `indiceAtual` é a ordem da primeira parada (fora as do motorista) com aluno pendente, igual à parada atual usada no atendimento de chamada.
- Sem rota calculada, `indiceAtual` e `concluida` continuam sendo calculados pelos dados de presença e de ordem.

## Pendências

1. Nome do endpoint `/Linha` e formato de erro: confirmar com o front.
2. Código HTTP para `dependenteId` que não pertence ao usuário: a definir.
3. Paradas do tipo `MOTORISTA` (início e fim da rota): `rotulo` `null`. Confirmar com o front.

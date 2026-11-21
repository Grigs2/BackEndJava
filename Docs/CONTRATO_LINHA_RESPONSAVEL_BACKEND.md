# Contrato backend: linha de estações do responsável

Proposta para validação do front antes da implementação. Contrato de referência no front: `docs/CONTRATO_LINHA_RESPONSAVEL.md` (branch `feat/full-map-guardian` do `frontendreact`).

## Endpoint

```
GET /Viagem/ViagemDia/{viagemDiaId}/Linha?dependenteId={id}
```

- Autorização: mesma regra da rota `GET /Viagem/ViagemDia/{viagemDiaId}/Rota`. Só o responsável do `dependenteId` e só com consentimento de localização desse aluno.
- Erros: mesmo padrão da rota. `dependenteId` que não pertence ao usuário logado: `403`.
- Pré-condições de leitura: acesso à execução, GPS ativo e consentimento do responsável para este aluno específico (`validarLeituraLocalizacaoDoDependente`).
- Não depende da API do Google. Responde mesmo quando a rota do Google falha.

## Resposta: `LinhaViagemDTO`

```json
{
  "disponivel": true,
  "mensagem": null,
  "indiceAtual": 2,
  "paradas": [
    { "ordem": 1, "tipoParada": "RESPONSAVEL", "acao": "EMBARQUE", "concluida": true, "doFilho": false, "rotulo": null },
    { "ordem": 2, "tipoParada": "RESPONSAVEL", "acao": "EMBARQUE", "concluida": false, "doFilho": true, "rotulo": "Casa de Enzo" },
    { "ordem": 3, "tipoParada": "ESCOLA", "acao": "DESEMBARQUE", "concluida": false, "doFilho": false, "rotulo": "Colégio X" }
  ]
}
```

| Campo | Tipo | Regra |
|---|---|---|
| `disponivel` | boolean | `false` quando a viagem não está em andamento ou não há dados suficientes. |
| `mensagem` | string \| null | Texto para exibir quando `disponivel` é `false`. |
| `indiceAtual` | int \| null | `ordem` da primeira parada com aluno pendente. `null` antes de sair ou depois de concluir a viagem. |
| `paradas[]` | array | Paradas da viagem na ordem da rota. Paradas `MOTORISTA` não são devolvidas. |
| `paradas[].ordem` | int | Posição sequencial entre as paradas devolvidas, começando em 1 (igual a "Parada N" no front). |
| `paradas[].tipoParada` | `MOTORISTA` \| `RESPONSAVEL` \| `ESCOLA` | Tipo da parada. |
| `paradas[].acao` | `EMBARQUE` \| `DESEMBARQUE` | Ida: embarque nas casas e desembarque na escola. Volta: o contrário. |
| `paradas[].concluida` | boolean | Calculada no servidor. Embarque: há alunos e todos estão `EMBARCADO` ou `FALTOU`. Desembarque: há alunos e todos estão `DESEMBARCADO` ou `FALTOU`. |
| `paradas[].doFilho` | boolean | `true` só na parada em que o aluno consultado está aguardando (embarque) ou a bordo (desembarque). |
| `paradas[].rotulo` | string \| null | Ver regra de privacidade. |

## Regra de privacidade (obrigatória)

- `rotulo` é preenchido só em dois casos:
  - `tipoParada = ESCOLA`: nome da escola.
  - `doFilho = true`: `Casa de {primeiroNome}` do aluno consultado.
- Nas demais paradas, `rotulo` é `null`. O front mostra "Parada N".
- Endereço nunca entra nesta resposta, nem para a parada do aluno.
- Coordenadas também não entram. A posição da perua continua vindo de `GET /Viagem/Localizacao/Atual/{viagemDiaId}`.

## Regras de negócio

- `paradas` segue a mesma ordem da rota do motorista. Reordenação via `PUT /Viagem/ReordenarParadasViagemDia/{idViagemDia}` reflete na resposta.
- `indiceAtual` é a `ordem` da primeira parada com aluno pendente, o mesmo critério de `proximaParadaPendente`.
- Parada do aluno consultado (`doFilho`): embarque enquanto está `ESPERANDO`, desembarque enquanto está `EMBARCADO`. Se o aluno já desembarcou ou faltou, nenhuma parada fica com `doFilho`.
- Sem rota do Google, a resposta continua igual: não depende da API do Google.

## Pendências

Nenhuma. Endpoint, formato de erro, `MOTORISTA` omitido, `ordem` sequencial e 403 para `dependenteId` não vinculado estão acertados com o front.

# Escolas da viagem - guia para o front

Este documento descreve o que o back-end expõe para o vínculo de escolas com as viagens do motorista, conforme o contrato `docs/CONTRATO_ESCOLAS_VIAGEM_BACK.md` do front. Ele substitui a necessidade de inferir regras a partir do código.

Base da URL: `/TioDaPerua/api`. Todos os endpoints exigem `Authorization: Bearer <accessToken>`.

## 1. Conceitos

| Conceito | Onde fica | Descrição |
|---|---|---|
| Escolas da viagem | `ViagemDTO.escolas` | Lista de `{id, nome}` das escolas que o itinerário atende. |
| Vínculo ativo motorista-escola | Módulo de vínculos escolares | Pré-requisito para a escola entrar no itinerário. |
| Solicitação de aluno | `SolicitacaoDTO` | Só pode ser aceita se a escola do aluno ainda é atendida pela viagem. |
| Parada oculta | Não exposta | Parada de escola removida no dia em que já havia histórico. Não aparece nas respostas. |

Uma viagem só aceita alunos das escolas que ela atende. Lista de escolas vazia não significa "qualquer escola": a viagem é desativada quando perde a última escola.

## 2. Endpoints

### 2.1 Listar escolas disponíveis

`GET /Viagem/EscolasDisponiveis/{idViagem}`

Perfil: dono da viagem (motorista).

Retorna `EscolaResumoDTO[]` (`{id, nome}`) com as escolas que têm vínculo **ATIVO** com o motorista e ainda não são atendidas pela viagem. Lista vazia é resposta válida.

### 2.2 Adicionar escolas à viagem

`POST /Viagem/AdicionarEscolas/{idViagem}`

Perfil: dono da viagem (motorista).

Corpo:

```json
{ "escolaIds": [1, 2] }
```

Regras:
- `escolaIds` é obrigatório e não pode ser vazio (validação `@NotEmpty`).
- Não pode haver id repetido na lista.
- A viagem precisa estar ativa.
- Não pode existir execução de hoje já iniciada ou finalizada: bloqueiam os status `EM_ANDAMENTO`, `FINALIZADA` e `INTERROMPIDA`. Execução de hoje `CANCELADA` não bloqueia.
- Todas as escolas precisam ter vínculo ATIVO com o motorista.

Resposta: `200` com o `ViagemDTO` atualizado.

**Regra a ser seguida pelo front:** ao adicionar escolas, o back **não** insere paradas nem presenças nas execuções. Os alunos dessas escolas entram na execução somente quando o responsável aceita a solicitação. A tela não deve assumir inserção automática.

### 2.3 Criação da viagem

`POST /Viagem/CriarViagem/{id_motorista}`

A viagem precisa atender ao menos uma escola (`400` caso contrário). O nome é opcional. Se vier vazio, o servidor gera:
- ida: `Viagem para escola(s): ...`
- volta: `Viagem de volta das escola(s): ...`

### 2.4 Remover escola da viagem

Não existe endpoint próprio. A remoção acontece quando o vínculo motorista-escola deixa de ser ativo:

- `POST /Motorista/Vinculos/{idVinculo}/Encerrar`
- `POST /Escola/Vinculos/{idVinculo}/Encerrar` (qualquer parte pode encerrar)
- `POST /Escola/Vinculos/{idVinculo}/Suspender` (somente a escola suspende)

Suspender e encerrar exigem motivo. Ao ocorrer, o back-end faz o seguinte para cada viagem ativa do motorista que atendia a escola:

1. Remove a escola de `escolas`.
2. Recusa as solicitações **abertas** dos alunos dessa escola (status `RECUSADA`). O responsável recebe notificação.
3. Remove esses alunos das execuções planejadas a partir de hoje.
4. Se a viagem ficar sem escola, ela é **desativada** (`ativo = false`). Acordos e solicitações abertas da viagem são encerrados.

O front deve atualizar a lista de viagens e as solicitações após essas ações.

### 2.5 Aceitar solicitação de aluno

Endpoints que já existiam, com nova regra:

- Motorista: `POST /Motorista/Solicitacoes/{idSolicitacao}/Responder` (corpo com `aceito`)
- Responsável: `POST /Responsavel/ResponderSolicitacao/{id_solicitacao}`

Se a escola do aluno não é mais atendida pela viagem, a aceitação é recusada com `409`:

```json
{ "status": 409, "error": "Conflict", "message": "A escola do aluno não é mais atendida por esta viagem" }
```

Recusar continua permitido.

### 2.6 Paradas da execução

`GET /Viagem/BuscarParadasViagemDia/{idViagemDia}`

`PUT /Viagem/ReordenarParadasViagemDia/{idViagemDia}`

Regras para o front:
- Paradas de escola removida com histórico **não são retornadas**. O front não precisa tratá-las.
- Na reordenação, envie apenas os ids das paradas que o `GET` retornou. Enviar um id oculto, ou omitir um visível, retorna `400` ("A nova ordem deve conter cada parada existente exatamente uma vez").
- A parada inicial e a final do motorista continuam fixas (`400`).
- A reordenação só é aceita com a execução `PLANEJADA` ou `EM_ANDAMENTO` (`409` nos demais casos).

## 3. Erros

Formato padrão (`ErrorResponseDTO`): `timestamp`, `status`, `error`, `message`, `path`.

| Status | Quando |
|---|---|
| `400` | Lista de escolas vazia ou repetida, escola sem vínculo ativo, reordenação inválida, motivo acima de 500 caracteres. |
| `404` | Viagem ou escola não encontrada. |
| `403` | Usuário que não é dono da viagem tentando adicionar escolas ou alterar o itinerário. |
| `409` | Viagem desativada, execução de hoje já iniciada, aceitação de aluno de escola removida. |

## 4. Fluxo sugerido no front

1. Tela da viagem: chamar `GET /Viagem/EscolasDisponiveis/{idViagem}` para montar a seleção.
2. Confirmar com `POST /Viagem/AdicionarEscolas/{idViagem}`.
3. Na tela do responsável, a solicitação de um aluno só é aceita enquanto a escola for atendida. Trate o `409` com a mensagem recebida.
4. Após suspensão ou encerramento de vínculo escolar, recarregar viagens, solicitações e paradas.
5. Se `ativo` vier `false` em `ViagemDTO`, mostrar a viagem como desativada e não permitir novas escolas.

# Sistema de Notificações

## Objetivo

Este documento descreve o fluxo de notificações no sistema **Tio da Perua**, permitindo a comunicação entre Motoristas, Responsáveis e Escolas. As notificações são persistidas e podem ser marcadas como visualizadas.

----------------------------------------------------------------------
1. LISTAR NOTIFICAÇÕES
----------------------------------------------------------------------

**Endpoint:** `GET /Notificacao/Listar/{id_usuario}`

Retorna todas as notificações **não visualizadas** destinadas ao usuário, ordenadas da mais recente para a mais antiga.

**Estrutura de Resposta (JSON):**

```json
[
  {
    "id": 15,
    "titulo": "Mudança de horário",
    "mensagem": "A viagem foi alterada.",
    "dataCriacao": "2026-06-01T10:30:00",
    "visto": false,
    "remetente": {
      "id": 3,
      "nome": "João",
      "tipoPerfil": "MOTORISTA",
      "telefone": "11999999999"
    }
  }
]
```

----------------------------------------------------------------------
2. VISUALIZAR NOTIFICAÇÃO
----------------------------------------------------------------------

**Endpoint:** `PUT /Notificacao/VisualizarNotificacao/{id_notificacao}`

Marca uma notificação específica como visualizada. Deve ser chamado quando o usuário abre ou lê a notificação no FrontEnd.

**Resposta Esperada:** `204 No Content`.

----------------------------------------------------------------------
3. LISTAR DESTINATÁRIOS
----------------------------------------------------------------------

**Endpoint:** `GET /Notificacao/ListarUsuarios`

Retorna uma lista de todos os usuários do sistema que podem receber notificações. Útil para preencher seletores de destinatários.

**Campos Retornados:** `id`, `nome`, `tipoPerfil`, `telefone`.

**Exemplo de Resposta:**

```json
[
  {
    "id": 7,
    "nome": "Maria",
    "tipoPerfil": "RESPONSAVEL",
    "telefone": "11988887777"
  }
]
```

----------------------------------------------------------------------
4. ENVIAR NOTIFICAÇÃO
----------------------------------------------------------------------

**Endpoint:** `POST /Notificacao/Enviar`

Envia uma nova notificação de um usuário para outro.

**DTO de Entrada (JSON):**

```json
{
   "titulo": "Aviso",
   "mensagem": "Mensagem teste",
   "visto": false,
   "remetenteId": 1,
   "destinatarioId": 2
}
```

**Resposta:** Retorna o objeto da notificação criada com status `201 Created`.

----------------------------------------------------------------------
5. FLUXO COMPLETO
----------------------------------------------------------------------

1. Usuário abre a aba de notificações.
2. FrontEnd chama `GET /Notificacao/Listar/{id_usuario}`.
3. Usuário clica em uma notificação para ler.
4. FrontEnd chama `PUT /Notificacao/VisualizarNotificacao/{id_notificacao}`.
5. A notificação deixa de aparecer na listagem (pois `visto` agora é `true`).
6. Se o usuário deseja enviar um novo aviso:
   a. FrontEnd chama `GET /Notificacao/ListarUsuarios`.
   b. Usuário seleciona o destinatário.
   c. FrontEnd chama `POST /Notificacao/Enviar` com os dados preenchidos.

----------------------------------------------------------------------
6. DTOs UTILIZADOS
----------------------------------------------------------------------

### NotificacaoDTO
- `id`: Long
- `titulo`: String
- `mensagem`: String
- `dataCriacao`: LocalDateTime
- `visto`: boolean
- `remetente`: UsuarioResumoDTO
- `remetenteId`: Long (usado no envio)
- `destinatarioId`: Long (usado no envio)

### UsuarioResumoDTO
- `id`: Long
- `nome`: String
- `tipoPerfil`: String
- `telefone`: String

# Documentação Completa da API REST - Tio da Perua

Esta documentação descreve todos os endpoints da API REST do projeto **Tio da Perua**, organizados por Controller.

**Base URL Local:** `http://localhost:8080/TioDaPerua/api/`

---

## 🚍 Motorista

### Cadastrar Motorista

#### Caso de Uso
Cadastrar Motorista

#### Fluxo de Negócio
Cadastro de Usuários

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Motorista/Cadastrar

#### Request Body (JSON de Exemplo)
```json
{
  "nome": "João do Caminhão",
  "dataNascimento": "1985-05-20",
  "cpf": "123.456.789-00",
  "cnh": "1234567890",
  "usuarioDTO": {
    "email": "joao@motorista.com",
    "senha": "senha123",
    "endereco": "Rua das Flores, 123",
    "telefone": "(11) 99999-9999",
    "tipoPerfil": "MOTORISTA"
  }
}
```

#### Regras de Negócio Aplicadas
- O objeto DTO e o usuário associado não podem ser nulos.
- O campo `tipoPerfil` no `usuarioDTO` deve ser obrigatoriamente `"MOTORISTA"`.

---

### Alterar Informações do Motorista

#### Caso de Uso
Alterar Perfil Motorista

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Motorista/AlterarInfos

#### Request Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João da Silva",
  "dataNascimento": "1985-05-20",
  "cpf": "123.456.789-00",
  "cnh": "1234567890",
  "usuarioDTO": {
    "id": 10,
    "email": "joao.novo@email.com",
    "senha": "senha123",
    "endereco": "Nova Rua, 456",
    "telefone": "(11) 88888-8888",
    "tipoPerfil": "MOTORISTA"
  }
}
```

#### Regras de Negócio Aplicadas
- O campo `senha` no `usuarioDTO` é obrigatório para validação.
- Retorna **401 Unauthorized** se a senha estiver incorreta.
- Atualiza apenas campos editáveis (nome, email, endereco, telefone, etc.).
- Não permite alteração de IDs ou relacionamentos críticos.

---

### Autenticar Motorista

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Motorista/Autenticar

---

## 🏫 Escola

### Cadastrar Escola

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Escola/Cadastrar

---

### Alterar Informações da Escola

#### Caso de Uso
Alterar Perfil Escola

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Escola/AlterarInfos

#### Request Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Novo Nome Escola",
  "admResponsavel": "Diretora Maria",
  "usuarioDTO": {
    "id": 15,
    "email": "escola@novo.com",
    "senha": "senha123",
    "endereco": "Rua Nova, 100",
    "telefone": "(11) 99999-0000",
    "tipoPerfil": "ESCOLA"
  }
}
```

#### Regras de Negócio Aplicadas
- Requer validação de senha (retorna 401 se inválida).

---

### Autenticar Escola

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Escola/Autenticar

---

## 👨‍👩‍👧‍👦 Responsável

### Cadastrar Responsável

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Cadastrar

---

### Alterar Informações do Responsável

#### Caso de Uso
Alterar Perfil Responsável

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/AlterarInfos

#### Request Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria de Souza",
  "cpf": "987.654.321-99",
  "dataNascimento": "1990-10-15",
  "usuario": {
    "id": 11,
    "email": "maria.nova@email.com",
    "senha": "senha123",
    "endereco": "Novo Endereço, 789",
    "telefone": "(11) 77777-7777",
    "tipoPerfil": "RESPONSAVEL"
  }
}
```

#### Regras de Negócio Aplicadas
- Requer validação de senha (retorna 401 se inválida).

---

### Alterar Informações de Dependente

#### Caso de Uso
Alterar Perfil Dependente

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Dependente/AlterarInfos

#### Request Body (JSON de Exemplo)
```json
{
  "id": 50,
  "nome": "Enzo de Souza",
  "cpf": "111.222.333-44",
  "dataNascimento": "2018-01-01",
  "periodo": "TARDE",
  "endereco": "Novo Endereço, 789",
  "senha": "senha123"
}
```

#### Regras de Negócio Aplicadas
- O campo `senha` deve ser a senha do Responsável vinculado.
- Retorna **401 Unauthorized** se a senha estiver incorreta.

---

### Autenticar Responsável

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Autenticar

---

### Listar Solicitações do Responsável

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ListarSolicitacoes/{idResponsavel}

#### Regras de Negócio Aplicadas
- Retorna apenas solicitações onde `dataFim` é nulo (vínculos ativos ou pendentes).

---

### Encerrar Vínculo com Viagem

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/EncerrarVinculoViagem/{idSolicitacao}

#### Path Variables
- `idSolicitacao`: ID da solicitação a ser encerrada.

---

## 🚌 Viagem

### Listar Viagens Ativas

#### Caso de Uso
Listar Viagens do Motorista

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/ListarViagens/{idMotorista}

#### Response Body
`List<ViagemDTO>` onde `ativo = true`.

---

### Desativar Viagem

#### Caso de Uso
Encerrar Rota Permanentemente

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/DesativarViagem/{idViagem}

#### Regras de Negócio Aplicadas
- Define `viagem.ativo = false`.
- Desativa todos os `ViagemDependente` vinculados.
- Encerra todas as solicitações ativas (`dataFim = now()`).
- Retorna **409 Conflict** se a viagem já estiver desativada.

---

### Histórico de Viagens

#### Caso de Uso
Visualizar Rotas Antigas

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/Historico/{idMotorista}

#### Response Body
`List<ViagemDTO>` onde `ativo = false`, ordenado pela mais recente.

---

### Consultar Motorista da Viagem

#### Caso de Uso
Identificar Motorista pelo Dia da Viagem

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/ConsultarMotorista/{idViagemDia}

#### Response Body
`MotoristaViagemDTO` contendo dados do motorista e veículo.

---

## 📦 DTOs Modificados

### ViagemDTO
- Adicionado campo `Boolean ativo`.

### DependenteDTO
- Adicionado campo `EscolaResumoDTO escola`.
- Adicionado campo `String senha` (opcional, usado apenas em `AlterarInfos`).

### MotoristaViagemDTO (Novo)
```json
{
  "motoristaId": 1,
  "nomeMotorista": "João",
  "telefone": "(11) 9...",
  "modeloVeiculo": "Sprinter",
  "placaVeiculo": "ABC-1234",
  "corVeiculo": "Branco"
}
```

---

## Resumo dos Casos de Uso Implementados

| Caso de Uso | Endpoint | Controller |
| ----------- | -------- | ---------- |
| Listar Viagens Ativas | `/Viagem/ListarViagens/{id}` | ViagemController |
| Desativar Viagem | `/Viagem/DesativarViagem/{id}` | ViagemController |
| Histórico de Viagens | `/Viagem/Historico/{id}` | ViagemController |
| Consultar Motorista | `/Viagem/ConsultarMotorista/{id}` | ViagemController |
| Alterar Perfil Motorista | `/Motorista/AlterarInfos` | MotoristaController |
| Alterar Perfil Responsável | `/Responsavel/AlterarInfos` | ResponsavelController |
| Alterar Perfil Dependente | `/Responsavel/Dependente/AlterarInfos` | ResponsavelController |
| Alterar Perfil Escola | `/Escola/AlterarInfos` | EscolaController |
| Encerrar Vínculo | `/Responsavel/EncerrarVinculoViagem/{id}` | ResponsavelController |

---

## Observações Gerais

- **Base URL:** `http://localhost:8080/TioDaPerua/api/`
- **Tratamento de Erros:** Alguns endpoints agora utilizam `ResponseEntity` para retornar status codes como 401 (Não Autorizado), 409 (Conflito) e 404 (Não Encontrado).
- **DTOs:** Incluem mappers estáticos `toDTO` e `toEntity`.

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

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

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

#### Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João do Caminhão",
  "dataNascimento": "1985-05-20",
  "cpf": "123.456.789-00",
  "cnh": "1234567890",
  "usuarioDTO": {
    "id": 10,
    "email": "joao@motorista.com",
    "senha": null,
    "endereco": "Rua das Flores, 123",
    "telefone": "(11) 99999-9999",
    "tipoPerfil": "MOTORISTA"
  },
  "veiculoDTO": null
}
```

#### Regras de Negócio Aplicadas
- O objeto DTO e o usuário associado não podem ser nulos.
- O campo `tipoPerfil` no `usuarioDTO` deve ser obrigatoriamente `"MOTORISTA"`.
- O sistema cria o usuário e vincula ao motorista.

#### Plano de Teste Básico
1. Pré-condições: Nenhuma.
2. Requisição: Enviar POST com os dados acima.
3. Resultado esperado: Motorista retornado com ID preenchido e senha do usuário como `null`.

---

### Cadastrar Veículo

#### Caso de Uso
Cadastrar Veículo

#### Fluxo de Negócio
Configuração de Motorista

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Motorista/CadastrarVeiculo/{id_motorista}

#### Path Variables
- `id_motorista`: ID do motorista que será proprietário do veículo.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "modelo": "Mercedes-Benz Sprinter",
  "placa": "ABC-1234",
  "ano": 2022,
  "capacidade": 20
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João do Caminhão",
  "dataNascimento": "1985-05-20",
  "cpf": "123.456.789-00",
  "cnh": "1234567890",
  "usuarioDTO": { ... },
  "veiculoDTO": {
    "id": 5,
    "modelo": "Mercedes-Benz Sprinter",
    "placa": "ABC-1234",
    "ano": 2022,
    "capacidade": 20
  }
}
```

#### Regras de Negócio Aplicadas
- O ID do motorista e o corpo da requisição não podem ser nulos.
- O veículo é criado e associado ao motorista.

#### Plano de Teste Básico
1. Pré-condições: Motorista cadastrado com ID 1.
2. Requisição: Enviar POST para `/Motorista/CadastrarVeiculo/1` com o payload acima.
3. Resultado esperado: Objeto Motorista retornado com o campo `veiculoDTO` preenchido.

---

### Autenticar Motorista

#### Caso de Uso
Login Motorista

#### Fluxo de Negócio
Autenticação

#### Método HTTP
GET (Implementado com @RequestBody)

#### URL Local
http://localhost:8080/TioDaPerua/api/Motorista/Autenticar

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "email": "joao@motorista.com",
  "senha": "senha123"
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João do Caminhão",
  "dataNascimento": "1985-05-20",
  "cpf": "123.456.789-00",
  "cnh": "1234567890",
  "usuarioDTO": { ... },
  "veiculoDTO": { ... }
}
```

#### Regras de Negócio Aplicadas
- O usuário deve existir e a senha deve ser compatível.
- O perfil do usuário autenticado deve ser `"MOTORISTA"`.

#### Plano de Teste Básico
1. Pré-condições: Motorista cadastrado com e-mail e senha informados.
2. Requisição: Enviar GET com o e-mail e senha no corpo.
3. Resultado esperado: Objeto Motorista completo retornado.

---

## 🏫 Escola

### Cadastrar Escola

#### Caso de Uso
Cadastrar Escola

#### Fluxo de Negócio
Cadastro de Usuários

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Escola/Cadastrar

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "nome": "Escola Municipal Pequeno Príncipe",
  "admResponsavel": "Diretora Cláudia",
  "usuarioDTO": {
    "email": "contato@pequenoprincipe.com",
    "senha": "senha123",
    "endereco": "Rua Escolar, 999",
    "telefone": "(11) 77777-7777",
    "tipoPerfil": "ESCOLA"
  }
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Escola Municipal Pequeno Príncipe",
  "admResponsavel": "Diretora Cláudia",
  "usuarioDTO": {
    "id": 15,
    "email": "contato@pequenoprincipe.com",
    "senha": null,
    "endereco": "Rua Escolar, 999",
    "telefone": "(11) 77777-7777",
    "tipoPerfil": "ESCOLA"
  }
}
```

#### Regras de Negócio Aplicadas
- O objeto DTO e o usuário associado não podem ser nulos.
- O campo `tipoPerfil` no `usuarioDTO` deve ser obrigatoriamente `"ESCOLA"`.
- O sistema cria o usuário e vincula à escola.

#### Plano de Teste Básico
1. Pré-condições: Nenhuma.
2. Requisição: Enviar POST com os dados acima.
3. Resultado esperado: Escola retornada com ID preenchido.

---

### Autenticar Escola

#### Caso de Uso
Login Escola

#### Fluxo de Negócio
Autenticação

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Escola/Autenticar

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "email": "contato@pequenoprincipe.com",
  "senha": "senha123"
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Escola Municipal Pequeno Príncipe",
  "admResponsavel": "Diretora Cláudia",
  "usuarioDTO": { ... }
}
```

#### Regras de Negócio Aplicadas
- O usuário deve existir e a senha deve ser compatível.
- O perfil do usuário autenticado deve ser `"ESCOLA"`.

#### Plano de Teste Básico
1. Pré-condições: Escola cadastrada com e-mail e senha informados.
2. Requisição: Enviar POST com o e-mail e senha no corpo.
3. Resultado esperado: Objeto Escola completo retornado.

---

## 🔔 Notificação

### Enviar Notificação

#### Caso de Uso
Enviar Notificação

#### Fluxo de Negócio
Comunicação

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Notificacao/Enviar

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "titulo": "Atraso na Viagem",
  "mensagem": "A van atrasará 10 minutos devido ao trânsito.",
  "remetenteId": 10,
  "destinatarioId": 20,
  "visto": false
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 100,
  "titulo": "Atraso na Viagem",
  "mensagem": "A van atrasará 10 minutos devido ao trânsito.",
  "data": "2026-05-09T10:00:00",
  "visto": false,
  "remetenteId": 10,
  "destinatarioId": 20
}
```

#### Regras de Negócio Aplicadas
- Valida remetente e destinatário existentes.
- A data é gerada automaticamente no momento do envio.
- O campo `visto` inicia como `false`.

#### Plano de Teste Básico
1. Pré-condições: Usuários com IDs 10 e 20 cadastrados.
2. Requisição: Enviar POST com o payload acima.
3. Resultado esperado: Notificação persistida e retornada com ID e data.

---

### Listar Notificações do Usuário

#### Caso de Uso
Listar Notificações

#### Fluxo de Negócio
Comunicação

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Notificacao/Listar/{id_usuario}

#### Path Variables
- `id_usuario`: ID do usuário cujas notificações serão listadas.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
[
  {
    "id": 100,
    "titulo": "Atraso na Viagem",
    "mensagem": "...",
    "data": "...",
    "visto": false,
    "remetenteId": 10,
    "destinatarioId": 20
  }
]
```

#### Regras de Negócio Aplicadas
- Retorna todas as notificações onde o usuário é o destinatário.

#### Plano de Teste Básico
1. Pré-condições: Notificações enviadas para o usuário ID 20.
2. Requisição: Enviar GET para `/Notificacao/Listar/20`.
3. Resultado esperado: Lista de notificações retornada.

---

## 👨‍👩‍👧‍👦 Responsável

### Cadastrar Responsável

#### Caso de Uso
Cadastrar Responsável

#### Fluxo de Negócio
Cadastro de Usuários

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Cadastrar

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "nome": "Maria Silva",
  "cpf": "987.654.321-99",
  "dataNascimento": "1990-10-15",
  "usuario": {
    "email": "maria@responsavel.com",
    "senha": "senha123",
    "endereco": "Avenida Brasil, 456",
    "telefone": "(11) 88888-8888",
    "tipoPerfil": "RESPONSAVEL"
  }
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria Silva",
  "cpf": "987.654.321-99",
  "dataNascimento": "1990-10-15",
  "usuario": {
    "id": 11,
    "email": "maria@responsavel.com",
    "senha": null,
    "endereco": "Avenida Brasil, 456",
    "telefone": "(11) 88888-8888",
    "tipoPerfil": "RESPONSAVEL"
  },
  "dependentes": []
}
```

#### Regras de Negócio Aplicadas
- O objeto DTO não pode ser nulo.
- O sistema cria o usuário e vincula ao responsável.

#### Plano de Teste Básico
1. Pré-condições: Nenhuma.
2. Requisição: Enviar POST com os dados acima.
3. Resultado esperado: Responsável retornado com ID e lista de dependentes vazia.

---

### Autenticar Responsável

#### Caso de Uso
Login Responsável

#### Fluxo de Negócio
Autenticação

#### Método HTTP
GET (Implementado com @RequestBody)

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Autenticar

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "email": "maria@responsavel.com",
  "senha": "senha123"
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria Silva",
  "cpf": "987.654.321-99",
  "dataNascimento": "1990-10-15",
  "usuario": { ... },
  "dependentes": [ ... ]
}
```

#### Regras de Negócio Aplicadas
- O usuário deve existir e a senha deve ser compatível.
- O perfil do usuário autenticado deve ser `"RESPONSAVEL"`.

#### Plano de Teste Básico
1. Pré-condições: Responsável cadastrado com e-mail e senha informados.
2. Requisição: Enviar GET com o e-mail e senha no corpo.
3. Resultado esperado: Objeto Responsável completo retornado.

---

### Cadastrar Dependente

#### Caso de Uso
Cadastrar Dependente

#### Fluxo de Negócio
Cadastro de Dependentes

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/CadastrarDependente/{id_Responsavel}

#### Path Variables
- `id_Responsavel`: ID do responsável pelo dependente.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "nome": "Enzo Silva",
  "cpf": "111.222.333-44",
  "dataNascimento": "2018-01-01",
  "periodo": "MANHA",
  "endereco": "Avenida Brasil, 456"
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria Silva",
  "cpf": "987.654.321-99",
  "dataNascimento": "1990-10-15",
  "usuario": { ... },
  "dependentes": [
    {
      "id": 50,
      "nome": "Enzo Silva",
      "cpf": "111.222.333-44",
      "dataNascimento": "2018-01-01",
      "periodo": "MANHA",
      "endereco": "Avenida Brasil, 456"
    }
  ]
}
```

#### Regras de Negócio Aplicadas
- O ID do responsável e o DTO do dependente não podem ser nulos.
- O dependente é criado e vinculado à lista de dependentes do responsável.
- Impede o cadastro se o dependente já estiver na lista.

#### Plano de Teste Básico
1. Pré-condições: Responsável cadastrado com ID 2.
2. Requisição: Enviar POST para `/Responsavel/CadastrarDependente/2` com o payload acima.
3. Resultado esperado: Objeto Responsável retornado contendo o novo dependente na lista.

---

### Monitorar Dependente

#### Caso de Uso
Monitorar Dependente

#### Fluxo de Negócio
Monitoramento

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Monitorar/{id_dependente}

#### Path Variables
- `id_dependente`: ID do dependente a ser monitorado.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
{
  "dependenteId": 50,
  "dependenteNome": "Enzo Silva",
  "statusAtual": {
    "id": 200,
    "viagemDiaId": 5,
    "dependenteId": 50,
    "status": "EMBARCADO",
    "horarioEmbarque": "2026-05-09T07:15:00",
    "horarioDesembarque": null
  },
  "historicoRecente": [
    {
      "id": 200,
      "viagemDiaId": 5,
      "dependenteId": 50,
      "status": "EMBARCADO",
      "horarioEmbarque": "2026-05-09T07:15:00",
      "horarioDesembarque": null
    },
    {
      "id": 190,
      "viagemDiaId": 4,
      "dependenteId": 50,
      "status": "DESEMBARCADO",
      "horarioEmbarque": "2026-05-08T07:10:00",
      "horarioDesembarque": "2026-05-08T07:45:00"
    }
  ]
}
```

#### Regras de Negócio Aplicadas
- Busca o dependente e seu histórico de presenças.
- O `statusAtual` é o registro mais recente do histórico.

#### Plano de Teste Básico
1. Pré-condições: Dependente com ID 50 possuindo registros em `ViagemPresenca`.
2. Requisição: Enviar GET para `/Responsavel/Monitorar/50`.
3. Resultado esperado: Objeto de monitoramento com status atual e lista de histórico.

---

### Responder Solicitação de Transporte

#### Caso de Uso
Responder Solicitação

#### Fluxo de Negócio
Solicitação de Transporte

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ResponderSolicitacao/{id_solicitacao}

#### Path Variables
- `id_solicitacao`: ID da solicitação enviada pelo motorista.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "respondido": true,
  "aceito": true
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 300,
  "viagemId": 1,
  "dependenteId": 50,
  "responsavelId": 2,
  "respondido": true,
  "aceito": true,
  "dataInicio": "2026-05-09T08:00:00",
  "dataFim": null
}
```

#### Regras de Negócio Aplicadas
- Atualiza o status da solicitação para `respondido = true`.
- Caso `aceito = true`, o sistema cria automaticamente um registro em `ViagemDependente` (vínculo ativo na rota).

#### Plano de Teste Básico
1. Pré-condições: Solicitação de ID 300 pendente.
2. Requisição: Enviar POST para `/Responsavel/ResponderSolicitacao/300` com `aceito: true`.
3. Resultado esperado: Solicitação retornada como respondida e aceita.

---

### Listar Escolas Cadastradas

#### Caso de Uso
Listar Escolas

#### Fluxo de Negócio
Consulta de Escolas

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ListarEscolas

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
[
  {
    "id": 8,
    "nome": "Escola Municipal Pequeno Príncipe",
    "admResponsavel": "Diretora Cláudia",
    "usuarioDTO": { ... }
  }
]
```

#### Regras de Negócio Aplicadas
- Retorna todas as escolas cadastradas no sistema para que o responsável possa escolher uma para vincular a seus dependentes.

#### Plano de Teste Básico
1. Pré-condições: Escolas cadastradas no sistema.
2. Requisição: Enviar GET para `/Responsavel/ListarEscolas`.
3. Resultado esperado: Lista de DTOs de escolas.

---

### Vincular Escola a Dependente

#### Caso de Uso
Vincular Escola a Dependentes

#### Fluxo de Negócio
Cadastro de Dependentes

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}

#### Path Variables
- `idResponsavel`: ID do responsável.
- `idEscola`: ID da escola a ser vinculada.
- `idDependente`: ID do dependente.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria Silva",
  "usuario": { ... },
  "dependentes": [
    {
      "id": 50,
      "nome": "Enzo Silva",
      "escola": {
        "id": 8,
        "nome": "Escola Municipal Pequeno Príncipe"
      }
    }
  ]
}
```

#### Regras de Negócio Aplicadas
- Valida se o responsável e a escola existem.
- Valida se o dependente pertence ao responsável informado.
- Atualiza o vínculo do dependente com a escola.

#### Plano de Teste Básico
1. Pré-condições: Responsável (2), Escola (8) e Dependente (50) cadastrados; Dependente pertence ao Responsável.
2. Requisição: Enviar POST para `/Responsavel/VincularEscola/2/8/50`.
3. Resultado esperado: Objeto Responsável atualizado com o dependente vinculado à escola.

---

### Listar Solicitações do Responsável

#### Caso de Uso
Visualizar Solicitações

#### Fluxo de Negócio
Solicitação de Transporte

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ListarSolicitacoes/{idResponsavel}

#### Path Variables
- `idResponsavel`: ID do responsável.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
[
  {
    "id": 300,
    "viagemId": 1,
    "dependenteId": 50,
    "responsavelId": 2,
    "respondido": true,
    "aceito": true,
    "dataInicio": "2026-05-09T08:00:00",
    "dataFim": null
  }
]
```

#### Regras de Negócio Aplicadas
- Lista todas as solicitações (respondidas ou não) vinculadas aos dependentes do responsável.

#### Plano de Teste Básico
1. Pré-condições: Solicitações existentes para os dependentes do responsável ID 2.
2. Requisição: Enviar GET para `/Responsavel/ListarSolicitacoes/2`.
3. Resultado esperado: Lista de solicitações.

---

### Encerrar Vínculo com Viagem

#### Caso de Uso
Encerrar Vínculo de Dependente com Viagem

#### Fluxo de Negócio
Gestão de Vínculos

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/EncerrarVinculoViagem

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "idSolicitacao": 300
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 300,
  "viagemId": 1,
  "dependenteId": 50,
  "responsavelId": 2,
  "respondido": true,
  "aceito": true,
  "dataInicio": "2026-05-09T08:00:00",
  "dataFim": "2026-05-12T20:05:00"
}
```

#### Regras de Negócio Aplicadas
- O responsável encerra a participação do dependente na rota informada.
- Define a `dataFim` na solicitação original.
- Marca o registro em `ViagemDependente` como `ativo = false`.
- O dependente deixa de constar em futuras execuções desta viagem, mas mantém seu histórico de presenças anteriores.

#### Plano de Teste Básico
1. Pré-condições: Solicitação ativa (ID 300).
2. Requisição: Enviar PUT para `/Responsavel/EncerrarVinculoViagem` com ID 300.
3. Resultado esperado: Solicitação retornada com `dataFim` preenchida e dependente removido de futuras viagens.

---

## 🚌 Viagem

### Criar Viagem (Rota)

#### Caso de Uso
Criar Viagem

#### Fluxo de Negócio
Configuração de Rota

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/CriarViagem/{id_motorista}

#### Path Variables
- `id_motorista`: ID do motorista que está criando a rota.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "periodo": "MANHA_IDA"
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "motoristaId": 1,
  "periodo": "MANHA_IDA"
}
```

#### Regras de Negócio Aplicadas
- Valida motorista existente e se possui veículo cadastrado.
- Impede a criação de mais de uma viagem para o mesmo motorista no mesmo período.

#### Plano de Teste Básico
1. Pré-condições: Motorista com veículo cadastrado.
2. Requisição: Enviar POST para `/Viagem/CriarViagem/1` com o período desejado.
3. Resultado esperado: Viagem criada e retornada com ID.

---

### Visualizar Detalhes da Viagem

#### Caso de Uso
Visualizar Viagem

#### Fluxo de Negócio
Consulta de Itinerário

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/Visualizar/{id_viagem}

#### Path Variables
- `id_viagem`: ID da viagem a ser visualizada.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "motoristaNome": "João do Caminhão",
  "periodo": "MANHA_IDA",
  "dependentes": [
    {
      "id": 50,
      "nome": "Enzo Silva",
      "endereco": "Avenida Brasil, 456",
      "escola": {
        "id": 8,
        "nome": "Escola Municipal Pequeno Príncipe"
      },
      "responsaveis": [
        {
          "id": 2,
          "nome": "Maria Silva",
          "telefone": "(11) 88888-8888"
        }
      ]
    }
  ]
}
```

#### Regras de Negócio Aplicadas
- Agrega informações de motorista, dependentes vinculados, suas escolas e responsáveis.

#### Plano de Teste Básico
1. Pré-condições: Viagem com ID 1 possuindo dependentes vinculados.
2. Requisição: Enviar GET para `/Viagem/Visualizar/1`.
3. Resultado esperado: DTO detalhado com toda a estrutura da rota.

---

### Listar Dependentes Disponíveis

#### Caso de Uso
Listar Dependentes Disponíveis

#### Fluxo de Negócio
Solicitação de Transporte

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/DependentesDisponiveis/{id_viagem}

#### Path Variables
- `id_viagem`: ID da viagem para a qual se busca novos passageiros.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
[
  {
    "id": 60,
    "nome": "Ana Oliveira",
    "cpf": "222.333.444-55",
    "dataNascimento": "2019-05-10",
    "periodo": "MANHA",
    "endereco": "Rua das Palmeiras, 789"
  }
]
```

#### Regras de Negócio Aplicadas
- Filtra dependentes que não possuem viagem ativa no mesmo período da viagem informada.

#### Plano de Teste Básico
1. Pré-condições: Dependentes cadastrados que ainda não estão vinculados a nenhuma viagem.
2. Requisição: Enviar GET para `/Viagem/DependentesDisponiveis/1`.
3. Resultado esperado: Lista de dependentes elegíveis para convite.

---

### Solicitar Entrada de Dependente

#### Caso de Uso
Solicitar Dependente

#### Fluxo de Negócio
Solicitação de Transporte

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/SolicitarDependente/{id_viagem}/{id_dependente}

#### Path Variables
- `id_viagem`: ID da rota do motorista.
- `id_dependente`: ID do aluno que o motorista deseja convidar.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
{
  "id": 301,
  "viagemId": 1,
  "dependenteId": 60,
  "responsavelId": 3,
  "respondido": false,
  "aceito": false,
  "dataInicio": "2026-05-09T10:30:00",
  "dataFim": null
}
```

#### Regras de Negócio Aplicadas
- Cria uma `Solicitacao` pendente.
- Identifica automaticamente o responsável pelo dependente para envio do convite.

#### Plano de Teste Básico
1. Pré-condições: Dependente disponível para a rota.
2. Requisição: Enviar POST para `/Viagem/SolicitarDependente/1/60`.
3. Resultado esperado: Solicitação criada com `respondido = false`.

---

### Iniciar Viagem Diária

#### Caso de Uso
Iniciar Viagem Diária

#### Fluxo de Negócio
Execução Diária

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/IniciarViagemDia/{id_viagem}

#### Path Variables
- `id_viagem`: ID da rota fixa que será executada hoje.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
{
  "id": 15,
  "data": "2026-05-12",
  "status": "PLANEJADA"
}
```

#### Regras de Negócio Aplicadas
- Cria um registro em `ViagemDia` para a data atual.
- Busca todos os dependentes com vínculo ativo (`ViagemDependente`) para esta rota.
- Cria automaticamente um registro em `ViagemPresenca` para cada dependente com status `ESPERANDO`.
- **Retorno de Instância:** Se a viagem já foi iniciada hoje, o endpoint retorna os dados da execução existente. Caso contrário, retorna a nova execução criada.

#### Plano de Teste Básico
1. Pré-condições: Viagem com dependentes ativos.
2. Requisição: Enviar POST para `/Viagem/IniciarViagemDia/1`.
3. Resultado esperado: Registros criados no banco (ViagemDia e ViagemPresenca).

---

### Alterar Status de ViagemDia

#### Caso de Uso
Alterar Status de ViagemDia

#### Fluxo de Negócio
Execução Diária

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/AlterarStatusViagemDia

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "idViagemDia": 1,
  "novoStatus": "EM_ANDAMENTO"
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "data": "2026-05-12",
  "status": "EM_ANDAMENTO",
  "dataUltimaAlteracaoStatus": "2026-05-12T20:00:00"
}
```

#### Regras de Negócio Aplicadas
- Permite ao motorista gerenciar o progresso da execução diária.
- Atualiza o campo `dataUltimaAlteracaoStatus` com o horário atual do servidor.
- Status válidos: `PLANEJADA`, `EM_ANDAMENTO`, `FINALIZADA`, `CANCELADA`.

#### Plano de Teste Básico
1. Pré-condições: Viagem do dia (ID 1) cadastrada.
2. Requisição: Enviar PUT para `/Viagem/AlterarStatusViagemDia` com `novoStatus: "EM_ANDAMENTO"`.
3. Resultado esperado: Objeto ViagemDia atualizado com novo status e data de alteração preenchida.

---

### Alterar Status de Presença

#### Caso de Uso
Alterar Status de Presença

#### Fluxo de Negócio
Execução Diária

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/AlterarStatusPresenca

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "idViagemDia": 15,
  "idDependente": 50,
  "novoStatus": "EMBARCADO"
}
```

#### Response Body (JSON de Exemplo)
```json
[
  {
    "ordem": 1,
    "tipoParada": "MOTORISTA",
    "nomeLocal": "Saída: Casa do Motorista",
    "endereco": "Rua das Flores, 123",
    "listaDependentes": []
  },
  {
    "ordem": 2,
    "tipoParada": "RESPONSAVEL",
    "nomeLocal": "Casa de Maria Silva",
    "endereco": "Avenida Brasil, 456",
    "listaDependentes": [
      {
        "id": 50,
        "nomeDependente": "Enzo Silva",
        "nomeResponsavel": "Maria Silva",
        "statusEmbarque": "EMBARCADO"
      }
    ]
  },
  ...
]
```

#### Regras de Negócio Aplicadas
- Localiza o registro de presença via `idViagemDia` e `idDependente`.
- Altera o status (`ESPERANDO`, `EMBARCADO`, `DESEMBARCADO`, `FALTOU`).
- Se status for `EMBARCADO`, registra o `horarioEmbarque`.
- Se status for `DESEMBARCADO`, registra o `horarioDesembarque`.
- **Retorno Imediato:** Após a alteração, o sistema recalcula e retorna a lista completa de paradas da viagem, garantindo que o frontend seja atualizado sem chamadas extras.

#### Plano de Teste Básico
1. Pré-condições: Viagem do dia iniciada (ID 15) com dependente (ID 50) com status `ESPERANDO`.
2. Requisição: Enviar PUT para `/Viagem/AlterarStatusPresenca` com o payload acima.
3. Resultado esperado: Lista de paradas retornada com o dependente exibindo `"statusEmbarque": "EMBARCADO"`.

---

### Buscar Paradas da Viagem do Dia

#### Caso de Uso
Gerar Rota Diária

#### Fluxo de Negócio
Execução Diária

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/BuscarParadasViagemDia/{idViagemDia}

#### Path Variables
- `idViagemDia`: ID da execução diária da viagem.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
[
  {
    "ordem": 1,
    "tipoParada": "MOTORISTA",
    "nomeLocal": "Saída: Casa do Motorista",
    "endereco": "Rua das Flores, 123",
    "listaDependentes": []
  },
  {
    "ordem": 2,
    "tipoParada": "RESPONSAVEL",
    "nomeLocal": "Casa de Maria Silva",
    "endereco": "Avenida Brasil, 456",
    "listaDependentes": [
      {
        "id": 50,
        "nomeDependente": "Enzo Silva",
        "nomeResponsavel": "Maria Silva",
        "statusEmbarque": "EMBARCADO"
      }
    ]
  },
  {
    "ordem": 3,
    "tipoParada": "ESCOLA",
    "nomeLocal": "Escola: Pequeno Príncipe",
    "endereco": "Rua Escolar, 999",
    "listaDependentes": [
      {
        "id": 50,
        "nomeDependente": "Enzo Silva",
        "nomeResponsavel": "Maria Silva",
        "statusEmbarque": "ESPERANDO"
      }
    ]
  },
  {
    "ordem": 4,
    "tipoParada": "MOTORISTA",
    "nomeLocal": "Retorno: Casa do Motorista",
    "endereco": "Rua das Flores, 123",
    "listaDependentes": []
  }
]
```

#### Regras de Negócio Aplicadas
- Gera dinamicamente a ordem das paradas baseada no tipo de viagem (Ida ou Volta).
- **Ida:** Motorista -> Responsáveis -> Escolas -> Motorista.
- **Volta:** Motorista -> Escolas -> Responsáveis -> Motorista.
- Agrupa dependentes que moram no mesmo endereço ou estudam na mesma escola.
- **Sincronização em Tempo Real:** Para cada dependente listado em uma parada, o sistema consulta e retorna o `statusEmbarque` atualizado de acordo com o registro diário em `ViagemPresenca`.

#### Plano de Teste Básico
1. Pré-condições: Viagem do dia iniciada.
2. Requisição: Enviar GET para `/Viagem/BuscarParadasViagemDia/5`.
3. Resultado esperado: Lista ordenada de paradas com endereços e dependentes.

---

## Resumo dos Casos de Uso Implementados

| Caso de Uso | Endpoint | Controller |
| ----------- | -------- | ---------- |
| Cadastrar Motorista | `/Motorista/Cadastrar` | MotoristaController |
| Cadastrar Veículo | `/Motorista/CadastrarVeiculo/{id}` | MotoristaController |
| Login Motorista | `/Motorista/Autenticar` | MotoristaController |
| Cadastrar Escola | `/Escola/Cadastrar` | EscolaController |
| Login Escola | `/Escola/Autenticar` | EscolaController |
| Enviar Notificação | `/Notificacao/Enviar` | NotificacaoController |
| Listar Notificações | `/Notificacao/Listar/{id}` | NotificacaoController |
| Cadastrar Responsável | `/Responsavel/Cadastrar` | ResponsavelController |
| Login Responsável | `/Responsavel/Autenticar` | ResponsavelController |
| Cadastrar Dependente | `/Responsavel/CadastrarDependente/{id}` | ResponsavelController |
| Monitorar Dependente | `/Responsavel/Monitorar/{id}` | ResponsavelController |
| Responder Solicitação | `/Responsavel/ResponderSolicitacao/{id}` | ResponsavelController |
| Listar Escolas | `/Responsavel/ListarEscolas` | ResponsavelController |
| Vincular Escola | `/Responsavel/VincularEscola/{r}/{e}/{d}` | ResponsavelController |
| Listar Solicitações (Responsável) | `/Responsavel/ListarSolicitacoes/{id}` | ResponsavelController |
| Encerrar Vínculo com Viagem | `/Responsavel/EncerrarVinculoViagem` | ResponsavelController |
| Criar Viagem | `/Viagem/CriarViagem/{id}` | ViagemController |
| Visualizar Viagem | `/Viagem/Visualizar/{id}` | ViagemController |
| Dependentes Disponíveis | `/Viagem/DependentesDisponiveis/{id}` | ViagemController |
| Solicitar Dependente | `/Viagem/SolicitarDependente/{v}/{d}` | ViagemController |
| Iniciar Viagem Diária | `/Viagem/IniciarViagemDia/{id}` | ViagemController |
| Alterar Status ViagemDia | `/Viagem/AlterarStatusViagemDia` | ViagemController |
| Alterar Status Presença | `/Viagem/AlterarStatusPresenca` | ViagemController |
| Gerar Rota Diária | `/Viagem/BuscarParadasViagemDia/{id}` | ViagemController |

---

## Resumo dos Fluxos de Negócio

| Fluxo | Endpoints Envolvidos |
| ----- | -------------------- |
| Cadastro de Usuários | `/Motorista/Cadastrar`, `/Responsavel/Cadastrar`, `/Escola/Cadastrar` |
| Autenticação | `/Motorista/Autenticar`, `/Responsavel/Autenticar`, `/Escola/Autenticar` |
| Configuração de Rota | `/Viagem/CriarViagem`, `/Motorista/CadastrarVeiculo` |
| Solicitação de Transporte| `/Viagem/DependentesDisponiveis`, `/Viagem/SolicitarDependente`, `/Responsavel/ResponderSolicitacao`, `/Responsavel/ListarSolicitacoes` |
| Execução Diária | `/Viagem/IniciarViagemDia`, `/Viagem/AlterarStatusViagemDia`, `/Viagem/AlterarStatusPresenca`, `/Viagem/BuscarParadasViagemDia` |
| Monitoramento | `/Responsavel/Monitorar` |
| Consulta de Escolas | `/Responsavel/ListarEscolas`, `/Responsavel/VincularEscola` |
| Gestão de Vínculos | `/Responsavel/EncerrarVinculoViagem` |
| Comunicação | `/Notificacao/Enviar`, `/Notificacao/Listar` |

---

## Observações Gerais

- **Base URL:** `http://localhost:8080/TioDaPerua/api/`
- **Tratamento de Erros:** Atualmente, a API retorna `null` para casos de erro ou objeto não encontrado. Em futuras versões, recomenda-se o uso de `ResponseEntity` com status codes adequados (404, 400, etc.).
- **Segurança:** A autenticação é simplificada (apenas verificação de e-mail e senha no banco) e não utiliza JWT ou Sessão no momento.
- **DTOs:** As conversões entre Entidade e DTO são realizadas via métodos estáticos `toDTO` e `toEntity` dentro das próprias classes DTO/Record.

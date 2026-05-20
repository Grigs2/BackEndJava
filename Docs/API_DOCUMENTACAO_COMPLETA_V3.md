# Documentação Completa da API REST - Tio da Perua (V3)

## Introdução
Esta documentação consolida e padroniza todos os endpoints da API REST do projeto **Tio da Perua**. O sistema é responsável pela gestão de transporte escolar, integrando Motoristas, Responsáveis, Escolas e Dependentes.

Esta versão (V3) substitui integralmente as documentações anteriores e reflete o estado atual do código-fonte, incluindo correções de compatibilidade para o FrontEnd Web e novas funcionalidades de gestão de perfil e viagens.

---

## Informações Gerais

- **Base URL Local:** `http://localhost:8080/TioDaPerua/api/`
- **Formato de Dados:** JSON (UTF-8)
- **Convenções REST:** 
  - `GET`: Consulta de dados.
  - `POST`: Criação de recursos ou operações complexas (ex: autenticação).
  - `PUT`: Atualização de recursos ou alteração de status.
  - `DELETE`: Remoção de recursos (não utilizado nesta versão, priorizando Soft Delete).
- **Tratamento de Erros:** A API utiliza códigos de status HTTP e, em casos específicos, retorna corpos de resposta com mensagens de erro ou `null`.
- **Autenticação:** Realizada via e-mail e senha. Os endpoints de autenticação foram migrados para `POST` para suporte completo a navegadores.

---

## Resumo dos Casos de Uso Implementados

| Caso de Uso | Endpoint | Controller |
| :--- | :--- | :--- |
| Cadastrar Motorista | `POST /Motorista/Cadastrar` | MotoristaController |
| Cadastrar Veículo | `POST /Motorista/CadastrarVeiculo/{id}` | MotoristaController |
| Autenticar Motorista | `POST /Motorista/Autenticar` | MotoristaController |
| Alterar Perfil Motorista | `PUT /Motorista/AlterarInfos` | MotoristaController |
| Cadastrar Escola | `POST /Escola/Cadastrar` | EscolaController |
| Autenticar Escola | `POST /Escola/Autenticar` | EscolaController |
| Alterar Perfil Escola | `PUT /Escola/AlterarInfos` | EscolaController |
| Cadastrar Responsável | `POST /Responsavel/Cadastrar` | ResponsavelController |
| Autenticar Responsável | `POST /Responsavel/Autenticar` | ResponsavelController |
| Alterar Perfil Responsável | `PUT /Responsavel/AlterarInfos` | ResponsavelController |
| Cadastrar Dependente | `POST /Responsavel/CadastrarDependente/{id}` | ResponsavelController |
| Alterar Perfil Dependente | `PUT /Responsavel/Dependente/AlterarInfos` | ResponsavelController |
| Monitorar Dependente | `GET /Responsavel/Monitorar/{id}` | ResponsavelController |
| Responder Solicitação | `POST /Responsavel/ResponderSolicitacao/{id}` | ResponsavelController |
| Listar Escolas | `GET /Responsavel/ListarEscolas` | ResponsavelController |
| Vincular Escola | `POST /Responsavel/VincularEscola/{r}/{e}/{d}` | ResponsavelController |
| Listar Solicitações (Responsável) | `GET /Responsavel/ListarSolicitacoes/{id}` | ResponsavelController |
| Encerrar Vínculo com Viagem | `PUT /Responsavel/EncerrarVinculoViagem/{id}` | ResponsavelController |
| Criar Viagem (Rota) | `POST /Viagem/CriarViagem/{id}` | ViagemController |
| Visualizar Detalhes da Viagem | `GET /Viagem/Visualizar/{id}` | ViagemController |
| Listar Viagens Ativas | `GET /Viagem/ListarViagens/{id}` | ViagemController |
| Desativar Viagem | `PUT /Viagem/DesativarViagem/{id}` | ViagemController |
| Histórico de Viagens | `GET /Viagem/Historico/{id}` | ViagemController |
| Consultar Motorista da Viagem | `GET /Viagem/ConsultarMotorista/{id}` | ViagemController |
| Visualizar Viagem Dia | `GET /Viagem/VisualizarViagemDia/{id}` | ViagemController |
| Dependentes Disponíveis | `GET /Viagem/DependentesDisponiveis/{id}` | ViagemController |
| Solicitar Dependente | `POST /Viagem/SolicitarDependente/{v}/{d}` | ViagemController |
| Iniciar Viagem Diária | `POST /Viagem/IniciarViagemDia/{id}` | ViagemController |
| Alterar Status ViagemDia | `PUT /Viagem/AlterarStatusViagemDia` | ViagemController |
| Alterar Status Presença | `PUT /Viagem/AlterarStatusPresenca` | ViagemController |
| Buscar Paradas da Viagem do Dia | `GET /Viagem/BuscarParadasViagemDia/{id}` | ViagemController |
| Enviar Notificação | `POST /Notificacao/Enviar` | NotificacaoController |
| Listar Notificações do Usuário | `GET /Notificacao/Listar/{id}` | NotificacaoController |

---

## 🚍 Motorista

### Cadastrar Motorista

Caso de Uso
Cadastrar Motorista

Fluxo de Negócio
Cadastro de Usuários

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Motorista/Cadastrar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
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

Response Body (JSON de Exemplo)
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

Regras de Negócio Aplicadas
* O objeto DTO e o usuário associado não podem ser nulos.
* O campo `tipoPerfil` no `usuarioDTO` deve ser obrigatoriamente `"MOTORISTA"`.
* O sistema cria o usuário e vincula ao motorista.

Códigos de Resposta Esperados
* 200 OK
* 400 Bad Request

Plano de Teste Básico
Pré-condições:
* Nenhuma.
Requisição:
* Enviar POST com os dados acima.
Resultado esperado:
* Motorista retornado com ID preenchido e senha do usuário como `null`.

---

### Cadastrar Veículo

Caso de Uso
Cadastrar Veículo

Fluxo de Negócio
Configuração de Motorista

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Motorista/CadastrarVeiculo/{id_motorista}

Path Variables
* `id_motorista`: ID do motorista que será proprietário do veículo.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "modelo": "Mercedes-Benz Sprinter",
  "placa": "ABC-1234",
  "ano": 2022,
  "capacidade": 20,
  "cor": "Branco"
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João do Caminhão",
  "veiculoDTO": {
    "id": 5,
    "modelo": "Mercedes-Benz Sprinter",
    "placa": "ABC-1234",
    "ano": 2022,
    "capacidade": 20,
    "cor": "Branco"
  }
}
```

Regras de Negócio Aplicadas
* O ID do motorista e o corpo da requisição não podem ser nulos.
* O veículo é criado e associado ao motorista.

Códigos de Resposta Esperados
* 200 OK
* 400 Bad Request

Plano de Teste Básico
Pré-condições:
* Motorista cadastrado com ID 1.
Requisição:
* Enviar POST para `/Motorista/CadastrarVeiculo/1` com o payload acima.
Resultado esperado:
* Objeto Motorista retornado com o campo `veiculoDTO` preenchido.

---

### Autenticar Motorista

Caso de Uso
Login Motorista

Fluxo de Negócio
Autenticação

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Motorista/Autenticar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "email": "joao@motorista.com",
  "senha": "senha123"
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João do Caminhão",
  "usuarioDTO": {
    "id": 10,
    "email": "joao@motorista.com",
    "senha": null,
    "tipoPerfil": "MOTORISTA"
  }
}
```

Regras de Negócio Aplicadas
* O usuário deve existir e a senha deve ser compatível.
* O perfil do usuário autenticado deve ser `"MOTORISTA"`.

Códigos de Resposta Esperados
* 200 OK
* 401 Unauthorized (se falhar na autenticação interna)

Plano de Teste Básico
Pré-condições:
* Motorista cadastrado com e-mail e senha informados.
Requisição:
* Enviar POST com o e-mail e senha no corpo.
Resultado esperado:
* Objeto Motorista completo retornado.

---

### Alterar Informações do Motorista

Caso de Uso
Alterar Perfil Motorista

Fluxo de Negócio
Gestão de Perfil

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Motorista/AlterarInfos

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
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

Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João da Silva",
  "usuarioDTO": {
    "id": 10,
    "email": "joao.novo@email.com",
    "senha": null,
    "endereco": "Nova Rua, 456"
  }
}
```

Regras de Negócio Aplicadas
* O campo `senha` no `usuarioDTO` é obrigatório para validação.
* O sistema valida se a senha informada corresponde à senha atual no banco.
* Atualiza apenas campos editáveis. Não permite alteração de IDs ou tipo de perfil.

Códigos de Resposta Esperados
* 200 OK
* 400 Bad Request
* 401 Unauthorized (senha inválida)
* 404 Not Found

Plano de Teste Básico
Pré-condições:
* Motorista autenticado com ID 1 e senha "senha123".
Requisição:
* Enviar PUT com dados novos e senha correta.
Resultado esperado:
* Retorno 200 com DTO atualizado.

---

## 🏫 Escola

### Cadastrar Escola

Caso de Uso
Cadastrar Escola

Fluxo de Negócio
Cadastro de Usuários

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Escola/Cadastrar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
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

Response Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Escola Municipal Pequeno Príncipe",
  "admResponsavel": "Diretora Cláudia",
  "usuarioDTO": {
    "id": 15,
    "email": "contato@pequenoprincipe.com",
    "senha": null,
    "tipoPerfil": "ESCOLA"
  }
}
```

Regras de Negócio Aplicadas
* O objeto DTO e o usuário associado não podem ser nulos.
* O campo `tipoPerfil` no `usuarioDTO` deve ser obrigatoriamente `"ESCOLA"`.

Códigos de Resposta Esperados
* 200 OK
* 400 Bad Request

Plano de Teste Básico
Pré-condições:
* Nenhuma.
Requisição:
* Enviar POST com os dados acima.
Resultado esperado:
* Escola retornada com ID preenchido.

---

### Autenticar Escola

Caso de Uso
Login Escola

Fluxo de Negócio
Autenticação

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Escola/Autenticar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "email": "contato@pequenoprincipe.com",
  "senha": "senha123"
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Escola Municipal Pequeno Príncipe",
  "usuarioDTO": { ... }
}
```

Regras de Negócio Aplicadas
* O usuário deve existir e a senha deve ser compatível.
* O perfil do usuário autenticado deve ser `"ESCOLA"`.

Códigos de Resposta Esperados
* 200 OK
* 401 Unauthorized

---

### Alterar Informações da Escola

Caso de Uso
Alterar Perfil Escola

Fluxo de Negócio
Gestão de Perfil

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Escola/AlterarInfos

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
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

Response Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Novo Nome Escola",
  "usuarioDTO": { ... }
}
```

Regras de Negócio Aplicadas
* Requer validação de senha atual (campo `senha` no `usuarioDTO`).
* Atualiza nome e dados de contato.

Códigos de Resposta Esperados
* 200 OK
* 401 Unauthorized
* 404 Not Found

---

## 👨‍👩‍👧‍👦 Responsável

### Cadastrar Responsável

Caso de Uso
Cadastrar Responsável

Fluxo de Negócio
Cadastro de Usuários

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Cadastrar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
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

Response Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria Silva",
  "usuario": {
    "id": 11,
    "email": "maria@responsavel.com",
    "senha": null,
    "tipoPerfil": "RESPONSAVEL"
  },
  "dependentes": []
}
```

Regras de Negócio Aplicadas
* O objeto DTO não pode ser nulo.
* O sistema cria o usuário e vincula ao responsável.

---

### Autenticar Responsável

Caso de Uso
Login Responsável

Fluxo de Negócio
Autenticação

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Autenticar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "email": "maria@responsavel.com",
  "senha": "senha123"
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria Silva",
  "usuario": { ... },
  "dependentes": [ ... ]
}
```

Regras de Negócio Aplicadas
* O perfil do usuário autenticado deve ser `"RESPONSAVEL"`.

---

### Alterar Informações do Responsável

Caso de Uso
Alterar Perfil Responsável

Fluxo de Negócio
Gestão de Perfil

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/AlterarInfos

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
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

Regras de Negócio Aplicadas
* Requer validação de senha atual (campo `senha` dentro do objeto `usuario`).
* Retorna **401 Unauthorized** se a senha estiver incorreta.

---

### Responder Solicitação de Transporte

Caso de Uso
Responder Solicitação

Fluxo de Negócio
Solicitação de Transporte

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ResponderSolicitacao/{id_solicitacao}

Path Variables
* `id_solicitacao`: ID da solicitação enviada pelo motorista.

---

### Listar Escolas Cadastradas

Caso de Uso
Listar Escolas

Fluxo de Negócio
Consulta de Escolas

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ListarEscolas

---

### Vincular Escola a Dependente

Caso de Uso
Vincular Escola a Dependentes

Fluxo de Negócio
Cadastro de Dependentes

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}

---

### Listar Solicitações do Responsável

Caso de Uso
Visualizar Solicitações

Fluxo de Negócio
Solicitação de Transporte

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ListarSolicitacoes/{idResponsavel}

Path Variables
* `idResponsavel`: ID do responsável.

Regras de Negócio Aplicadas
* Retorna apenas solicitações onde `dataFim` é nulo (solicitações ativas ou em aberto).

---

### Encerrar Vínculo com Viagem

Caso de Uso
Encerrar Vínculo de Dependente com Viagem

Fluxo de Negócio
Gestão de Vínculos

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/EncerrarVinculoViagem/{idSolicitacao}

Path Variables
* `idSolicitacao`: ID da solicitação que será encerrada.

Regras de Negócio Aplicadas
* Define a `dataFim` na solicitação original como o horário atual.
* Marca o registro em `ViagemDependente` como `ativo = false`.
* O dependente deixa de constar em futuras execuções desta viagem.

---

## 🧒 Dependente

### Cadastrar Dependente

Caso de Uso
Cadastrar Dependente

Fluxo de Negócio
Cadastro de Dependentes

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/CadastrarDependente/{id_Responsavel}

Path Variables
* `id_Responsavel`: ID do responsável pelo dependente.

Request Body (JSON de Exemplo)
```json
{
  "nome": "Enzo Silva",
  "cpf": "111.222.333-44",
  "dataNascimento": "2018-01-01",
  "periodo": "MANHA",
  "endereco": "Avenida Brasil, 456"
}
```

---

### Alterar Informações de Dependente

Caso de Uso
Alterar Perfil Dependente

Fluxo de Negócio
Gestão de Perfil

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Dependente/AlterarInfos

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
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

Response Body (JSON de Exemplo)
```json
{
  "id": 50,
  "nome": "Enzo de Souza",
  "cpf": "111.222.333-44",
  "dataNascimento": "2018-01-01",
  "periodo": "TARDE",
  "endereco": "Novo Endereço, 789",
  "escola": {
    "id": 8,
    "nome": "Escola Municipal Pequeno Príncipe"
  }
}
```

Regras de Negócio Aplicadas
* O campo `senha` deve ser preenchido com a senha do **Responsável** vinculado.
* O sistema valida a senha contra o primeiro responsável associado ao dependente.
* Retorna **401 Unauthorized** se a senha estiver incorreta.

---

### Monitorar Dependente

Caso de Uso
Monitorar Dependente

Fluxo de Negócio
Monitoramento

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Monitorar/{id_dependente}

Path Variables
* `id_dependente`: ID do dependente a ser monitorado.

---

## 🛣️ Viagem

### Criar Viagem (Rota)

Caso de Uso
Criar Viagem

Fluxo de Negócio
Configuração de Rota

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/CriarViagem/{id_motorista}

Regras de Negócio Aplicadas
* A nova viagem é criada com o campo `ativo` definido como `true` por padrão.

---

### Visualizar Detalhes da Viagem

Caso de Uso
Visualizar Viagem

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/Visualizar/{id_viagem}

Regras de Negócio Aplicadas
* Agrega informações de motorista, dependentes vinculados, suas escolas e responsáveis.
* O `DependenteDTO` retornado inclui o campo `escola` (`EscolaResumoDTO`).

---

### Listar Viagens Ativas

Caso de Uso
Listar Viagens do Motorista

Fluxo de Negócio
Gestão de Viagens

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/ListarViagens/{idMotorista}

Path Variables
* `idMotorista`: ID do motorista proprietário das rotas.

Response Body (JSON de Exemplo)
```json
[
  {
    "id": 1,
    "motoristaId": 1,
    "periodo": "MANHA_IDA",
    "ativo": true
  }
]
```

Regras de Negócio Aplicadas
* Retorna apenas viagens vinculadas ao motorista informado onde `ativo = true`.

---

### Desativar Viagem

Caso de Uso
Encerrar Rota Permanentemente

Fluxo de Negócio
Gestão de Viagens

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/DesativarViagem/{idViagem}

Path Variables
* `idViagem`: ID da viagem a ser desativada.

Regras de Negócio Aplicadas
* Operação transacional em cascata.
* Define `viagem.ativo = false`.
* Desativa todos os registros de `ViagemDependente` relacionados (`ativo = false`).
* Encerra todas as solicitações ativas associadas à viagem (`dataFim = now()`).
* Se a viagem já estiver desativada, retorna **409 Conflict**.

Códigos de Resposta Esperados
* 200 OK
* 404 Not Found
* 409 Conflict

---

### Histórico de Viagens

Caso de Uso
Visualizar Execuções de Viagens Antigas

Fluxo de Negócio
Gestão de Viagens

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/Historico/{idMotorista}

Path Variables
* `idMotorista`: ID do motorista.

Response Body (JSON de Exemplo)
```json
[
  {
    "id": 15,
    "data": "2026-05-12",
    "status": "FINALIZADA",
    "dataUltimaAlteracaoStatus": "2026-05-12T10:00:00"
  },
  {
    "id": 14,
    "data": "2026-05-11",
    "status": "FINALIZADA",
    "dataUltimaAlteracaoStatus": "2026-05-11T10:05:00"
  }
]
```

Regras de Negócio Aplicadas
* Retorna a lista de viagens diárias (`ViagemDia`) executadas por qualquer rota do motorista.
* Ordenado do registro mais recente para o mais antigo.

---

### Consultar Motorista da Viagem

Caso de Uso
Identificar Motorista pelo Dia da Viagem

Fluxo de Negócio
Monitoramento / Segurança

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/ConsultarMotorista/{idViagemDia}

Path Variables
* `idViagemDia`: ID da execução diária da viagem.

Response Body (JSON de Exemplo)
```json
{
  "motoristaId": 1,
  "nomeMotorista": "João do Caminhão",
  "telefone": "(11) 99999-9999",
  "modeloVeiculo": "Mercedes Sprinter",
  "placaVeiculo": "ABC-1234",
  "corVeiculo": "Branco"
}
```

Regras de Negócio Aplicadas
* Permite ao passageiro/responsável obter dados de contato e do veículo a partir de uma viagem em andamento ou planejada.

---

### Visualizar Viagem Dia

#### Caso de Uso
Visualizar Detalhes da Execução Diária

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/VisualizarViagemDia/{id_viagemDia}

#### Path Variables
- `id_viagemDia`: ID da execução diária da viagem.

#### Response Body (JSON de Exemplo)
```json
{
  "id": 15,
  "data": "2026-05-12",
  "status": "EM_ANDAMENTO",
  "dataUltimaAlteracaoStatus": "2026-05-12T10:00:00"
}
```

#### Regras de Negócio Aplicadas
- Retorna os detalhes de uma execução diária específica (`ViagemDia`) a partir de seu ID.
- Se o ID não existir, retorna **404 Not Found**.

#### Plano de Teste Básico
1. Pré-condições: Viagem do dia (ID 15) cadastrada.
2. Requisição: Enviar GET para `/Viagem/VisualizarViagemDia/15`.
3. Resultado esperado: Objeto ViagemDia correspondente retornado.

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

## Observações Gerais

- **Convenções REST:** A API segue padrões RESTful, utilizando os métodos HTTP adequados para cada operação.
- **Soft Delete:** A remoção de rotas (Viagens) não exclui registros do banco; utiliza-se o campo `ativo` para controle de visibilidade (Soft Delete), preservando o histórico de presenças.
- **Validação de Senha:** Todos os endpoints de `AlterarInfos` exigem a senha atual do usuário no payload. Caso a senha esteja incorreta, a API retorna `401 Unauthorized`.
- **Integridade de Vínculos:** Ao desativar uma viagem, o sistema automaticamente encerra os vínculos ativos dos dependentes e as solicitações pendentes/aceitas relacionadas, garantindo que nenhum aluno fique "preso" a uma rota inexistente.
- **Compatibilidade FrontEnd:** Todos os endpoints de autenticação utilizam `POST` para permitir o envio do `RequestBody` em conformidade com os padrões de navegadores modernos.

---

## Histórico de Versões

| Versão | Descrição |
| :--- | :--- |
| V1 | Documentação inicial (Estrutura detalhada) |
| V2 | Inclusão de novas funcionalidades e correções de FrontEnd |
| V3 | Consolidação completa e definitiva (Padronização final) |

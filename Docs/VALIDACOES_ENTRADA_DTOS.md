# Especificação e Guia de Testes: Validações de Entrada via DTOs

**Branch de Referência:** `validacao-infos-entrada`  
**Context-Path da API:** `/TioDaPerua/api/`  
**Padrão de Resposta de Erro:** HTTP 400 (*Bad Request*) com corpo no formato `Map<String, String>` (`{ "nomeDoCampo": "mensagemDeErro" }`).

---

## 1. Visão Geral e Arquitetura de Validação

Esta documentação descreve as regras de integridade e validação de entrada de dados da API do **Tio da Perua**, utilizando as anotações do **Jakarta Bean Validation** (`jakarta.validation.constraints.*`), do **Hibernate Validator** (`org.hibernate.validator.constraints.br.*`) e validações customizadas (`@CNH`).

O objetivo desta especificação é servir de referência para desenvolvedores e equipe de testes (QA), documentando:
1. Todos os campos validados em cada DTO de entrada.
2. As regras de negócio e restrições de formato aplicadas.
3. As mensagens de erro retornadas quando uma restrição for violada.
4. Exemplos claros de payloads válidos e cenários de teste com dados inválidos.

---

## 2. Tabela Detalhada de Validações por DTO

### 2.1. UsuarioDTO
Utilizado nos fluxos de autenticação e como sub-objeto nos cadastros de Motorista, Responsável e Escola.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `email` | `String` | `@NotBlank`<br>`@Email` | Obrigatório. Formato de e-mail padrão RFC 5322. | `"Email está nullo"`<br>`"E-mail inválido."` | `"motorista@tiodaperua.com.br"` | `""`, `"   "`, `null`, `"usuarioSemArroba"`, `"usuario@.com"` |
| `senha` | `String` | `@NotBlank` | Obrigatório. Não pode ser vazio ou conter apenas espaços. | `"Senha está nullo"` | `"SenhaForte@123"` | `""`, `"   "`, `null` |
| `endereco`| `String` | `@NotBlank` | Obrigatório. Logradouro completo com número e bairro. | `"Endereço está nullo"` | `"Av. Paulista, 1000, Bela Vista, São Paulo - SP"` | `""`, `"   "`, `null` |
| `telefone`| `String` | `@NotBlank`<br>`@Pattern(regexp = "^\\+\\d{2}\\d{2}\\d{8,9}$")` | Obrigatório. Padrão internacional E.164: Inicia com `+`, seguido de 2 dígitos de DDI, 2 de DDD e 8 ou 9 dígitos numéricos (total 12 a 13 dígitos após o `+`). | `"Telefone está nullo"`<br>`"O telefone deve estar no padrão E.164 (+5511999999999)."` | `"+5511987654321"` (celular)<br>`"+551134567890"` (fixo) | `"(11) 98765-4321"` (com máscara), `"11987654321"` (sem `+` e DDI), `"+55119876543210"` (dígitos a mais), `""`, `null` |
| `tipoPerfil`| `String`| `@NotBlank` | Obrigatório. Define o tipo de usuário no sistema (`MOTORISTA`, `RESPONSAVEL`, `ESCOLA`). | `"Tipo de perfil está nullo"` | `"MOTORISTA"`, `"RESPONSAVEL"`, `"ESCOLA"` | `""`, `"   "`, `null` |

---

### 2.2. VeiculoDTO
Utilizado no cadastro de veículos dos motoristas escolares.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `modelo` | `String` | `@NotBlank` | Obrigatório. Modelo/marca do veículo. | `"Modelo está nullo"` | `"Mercedes-Benz Sprinter 415 CDI"` | `""`, `"   "`, `null` |
| `placa` | `String` | `@NotBlank`<br>`@Pattern(regexp = "^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$")` | Obrigatório. Padrão Mercosul (`ABC1D23`) ou tradicional cinza (`ABC1234` ou `ABC-1234`). Letras sempre maiúsculas. | `"Placa está nullo"`<br>`"A placa deve estar no formato (ABC1D23)."` | `"ABC1D23"` (Mercosul)<br>`"ABC-1234"` (Antigo c/ hífen)<br>`"ABC1234"` (Antigo s/ hífen) | `"abc1d23"` (minúsculas), `"123ABCD"` (invertido), `"AB1234"` (faltando letra), `"ABC12345"` (8 caracteres), `""`, `null` |
| `ano` | `int` | `@NotNull`<br>`@Min(1900)`<br>`@Max(2027)` | Obrigatório. Ano de fabricação do veículo entre 1900 e 2027. | `"Ano está nullo"`<br>`"O ano do veículo deve ser maior que 1900."`<br>`"O ano do veículo não pode ser superior ao ano modelo atual."` | `2022`, `2024` | `1899` (inferior a 1900), `2028` (superior a 2027) |
| `capacidade`| `int` | `@NotNull` | Obrigatório. Lotação de passageiros sentados. | `"Capacidade está nullo"` | `15`, `20` | Envio de valor não numérico no JSON |
| `cor` | `String` | `@NotBlank` | Obrigatório. Cor predominante do veículo escolar. | `"Cor está nullo"` | `"Amarelo"`, `"Branco"` | `""`, `"   "`, `null` |

---

### 2.3. MotoristaDTO
Utilizado no cadastro e atualização do perfil do motorista.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `nome` | `String` | `@NotBlank` | Obrigatório. Nome completo do motorista. | `"Nome está nullo"` | `"Carlos Alberto da Silva"` | `""`, `"   "`, `null` |
| `dataNascimento` | `LocalDate` | `@NotNull`<br>`@Past`<br>`@JsonFormat(pattern = "yyyy-MM-dd")` | Obrigatório. Data no passado no padrão `AAAA-MM-DD`. | `"dataNascimento está nullo"`<br>`"A data deve ser no passado."` | `"1985-06-20"` | `"2099-01-01"` (futuro), `"15/06/1985"` (padrão errado), `null` |
| `cpf` | `String` | `@NotBlank`<br>`@Pattern(regexp = "^\\d{11}$")`<br>`@CPF` | Obrigatório. Exatamente 11 dígitos numéricos com dígitos verificadores matematicamente válidos. | `"CPF está nullo"`<br>`"O CPF deve conter exatamente 11 dígitos numéricos."`<br>`"CPF inválido."` | `"85367683050"` (ou outro CPF válido sem pontuação) | `"123.456.789-00"` (com máscara), `"1234567890"` (10 dígitos), `"11111111111"` (repetido), `"00000000001"` (DV inválido), `""`, `null` |
| `cnh` | `String` | `@NotBlank`<br>`@Pattern(regexp = "^\\d{11}$")`<br>`@CNH` | Obrigatório. 11 dígitos numéricos, sem sequências de 11 repetidos, com validação de dois DVs segundo algoritmo SENATRAN (`CNHValidator`). | `"CNH está nullo"`<br>`"A CNH deve conter exatamente 11 dígitos numéricos."`<br>`"CNH inválida."` | `"03932840500"` (número válido pelo algoritmo SENATRAN) | `"11111111111"` (repetido), `"123456789"` (tamanho inválido), `"00000000001"` (DV incorreto), `""`, `null` |
| `usuarioDTO` | `UsuarioDTO` | `@NotNull`<br>`@Valid` | Obrigatório. Objeto `UsuarioDTO` validado em cascata. | `"Usuário está nullo"` | Objeto `UsuarioDTO` preenchido | `null`, ou objeto `UsuarioDTO` com e-mail inválido |
| `veiculoDTO` | `VeiculoDTO` | `@NotNull`<br>`@Valid` | Obrigatório. Objeto `VeiculoDTO` validado em cascata. | `"Veículo está nullo"` | Objeto `VeiculoDTO` preenchido | `null`, ou objeto `VeiculoDTO` com ano fora do intervalo |

---

### 2.4. ResponsavelDTO
Utilizado no cadastro e atualização do perfil do responsável familiar.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `nome` | `String` | `@NotBlank` | Obrigatório. Nome completo do responsável. | `"Nome do responsável está nullo"` | `"Mariana Antunes de Souza"` | `""`, `"   "`, `null` |
| `cpf` | `String` | `@NotBlank`<br>`@Pattern(regexp = "^\\d{11}$")`<br>`@CPF` | Obrigatório. Exatamente 11 dígitos numéricos com dígitos verificadores de CPF válidos. | `"CPF do responsável está nullo"`<br>`"O CPF deve conter exatamente 11 dígitos numéricos."`<br>`"CPF inválido."` | `"85367683050"` | `"123.456.789-00"` (com máscara), `"11111111111"` (repetido), `""`, `null` |
| `dataNascimento` | `LocalDate` | `@NotNull`<br>`@Past`<br>`@JsonFormat(pattern = "yyyy-MM-dd")` | Obrigatório. Data no passado no formato `AAAA-MM-DD`. | `"Data de nascimento do responsável está nullo"`<br>`"A data deve ser no passado."` | `"1982-11-03"` | `"2035-12-01"` (futuro), `null` |
| `usuario` | `UsuarioDTO` | `@NotNull`<br>`@Valid` | Obrigatório. Dados de login/usuário validados em cascata. | `"Usuário do responsável está nullo"` | Objeto `UsuarioDTO` válido | `null`, ou dados de usuário inválidos |
| `dependentes` | `List<DependenteDTO>` | `@NotEmpty`<br>`@Valid` | Obrigatório. Lista contendo dependentes a serem cadastrados, validados em cascata. | `"Dependentes do responsável está nullo"` | `[ { ...DependenteDTO... } ]` | `null`, `[]` (lista vazia), ou dependente com dados inválidos |

---

### 2.5. DependenteDTO
Utilizado no cadastro e atualização dos dados dos alunos dependentes.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `nome` | `String` | `@NotBlank` | Obrigatório. Nome completo do aluno. | `"Nome do dependente está nullo"` | `"Lucas Antunes de Souza"` | `""`, `"   "`, `null` |
| `cpf` | `String` | `@NotBlank`<br>`@Pattern(regexp = "^\\d{11}$")`<br>`@CPF` | Obrigatório. 11 dígitos numéricos com dígitos verificadores oficiais de CPF válidos. | `"CPF do dependente está nullo"`<br>`"O CPF deve conter exatamente 11 dígitos numéricos."`<br>`"CPF inválido."` | `"85367683050"` | `"123.456.789-00"` (com máscara), `"12345678"` (8 dígitos), `"11111111111"`, `""`, `null` |
| `dataNascimento` | `LocalDate` | `@NotNull`<br>`@Past`<br>`@JsonFormat(pattern = "yyyy-MM-dd")` | Obrigatório. Data de nascimento no passado no padrão `AAAA-MM-DD`. | `"Data de nascimento do dependente está nullo"`<br>`"A data deve ser no passado."` | `"2015-08-22"` | `"2030-01-01"` (data futura), `null` |
| `periodo` | `String` | `@NotBlank` | Obrigatório. Período de aula do aluno. | `"Período do dependente está nullo"` | `"MANHA"`, `"TARDE"`, `"INTEGRAL"` | `""`, `"   "`, `null` |
| `endereco` | `String` | `@NotBlank` | Obrigatório. Endereço de embarque/desembarque do aluno. | `"Endereço do dependente está nullo"` | `"Rua das Gaivotas, 45, Casa 2"` | `""`, `"   "`, `null` |
| `escola` | `EscolaResumoDTO` | `@NotNull`<br>`@Valid` | Obrigatório. Objeto resumo da escola vinculada ao aluno. | `"Escola do dependente está nullo"` | `{"id": 1, "nome": "Colégio Integral"}` | `null`, ou objeto com `nome` vazio |
| `senha` | `String` | `@NotBlank` | Obrigatório. Senha do dependente utilizada na alteração cadastral. | `"Senha do dependente está nullo"` | `"SenhaAluno123"` | `""`, `"   "`, `null` |

---

### 2.6. EscolaDTO e EscolaResumoDTO
Utilizados no cadastro da instituição escolar e na seleção simplificada da escola.

#### EscolaDTO
| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `nome` | `String` | `@NotBlank` | Obrigatório. Razão social / nome da escola. | `"Nome está nullo"` | `"Colégio Dom Pedro II"` | `""`, `"   "`, `null` |
| `admResponsavel` | `String` | `@NotBlank` | Obrigatório. Nome do diretor ou administrador responsável. | `"Administrador responsável está nullo"` | `"Prof. Marcos Aurélio"` | `""`, `"   "`, `null` |
| `usuarioDTO` | `UsuarioDTO` | `@NotNull`<br>`@Valid` | Obrigatório. Usuário institucional com perfil obrigatório `"ESCOLA"`. | `"Usuário está nullo"` | Objeto `UsuarioDTO` com `tipoPerfil = "ESCOLA"` | `null`, ou objeto com `tipoPerfil != "ESCOLA"` |

#### EscolaResumoDTO
| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `Long` | *(Nenhuma)* | Identificador numérico da escola. | — | `1` | — |
| `nome` | `String` | `@NotBlank` | Obrigatório. Nome da escola. | `"Nome da escola está nullo"` | `"Colégio Santa Cruz"` | `""`, `"   "`, `null` |

---

### 2.7. ViagemDTO
Utilizado na criação e gerenciamento de itinerários de transporte escolar.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `motoristaId` | `Long` | `@NotNull` | Obrigatório. Identificador numérico do motorista responsável pelo itinerário. | `"ID do motorista está nullo"` | `1` | `null` |
| `periodo` | `String` | `@NotBlank` | Obrigatório. Deve corresponder a um dos valores do Enum `Periodo`: `MANHA_IDA`, `TARDE_IDA`, `TARDE_VOLTA`, `NOITE_IDA`, `NOITE_VOLTA`. | `"Período está nullo"` | `"MANHA_IDA"`, `"TARDE_VOLTA"` | `""`, `"MADRUGADA"`, `null` |
| `ativo` | `Boolean` | *(Nenhuma)* | Opcional. Situação do itinerário (default `false`). | — | `true`, `false`, `null` | — |

---

### 2.8. SolicitacaoDTO
Utilizado para gerenciar solicitações de transporte entre responsáveis e motoristas.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `viagemId` | `Long` | `@NotNull` | Obrigatório. ID da viagem/itinerário solicitado. | `"ID da viagem está nullo"` | `10` | `null` |
| `dependenteId` | `Long` | `@NotNull` | Obrigatório. ID do aluno a ser transportado. | `"ID do dependente está nullo"` | `5` | `null` |
| `responsavelId` | `Long` | `@NotNull` | Obrigatório. ID do responsável solicitante. | `"ID do responsável está nullo"` | `2` | `null` |
| `respondido` | `boolean` | *(Nenhuma)* | Indicador de resposta da solicitação. | — | `true`, `false` | — |
| `aceito` | `boolean` | *(Nenhuma)* | Indicador de aceite da solicitação. | — | `true`, `false` | — |
| `dataInicio` | `LocalDateTime` | `@JsonFormat(pattern = "yyyy-MM-dd")` | Data de início da vigência do transporte no padrão `AAAA-MM-DD`. | — | `"2026-02-01"` | Formato inválido `"01/02/2026"` |
| `dataFim` | `LocalDateTime` | `@JsonFormat(pattern = "yyyy-MM-dd")` | Data de término da vigência no padrão `AAAA-MM-DD`. | — | `"2026-12-15"` | Formato inválido `"15/12/2026"` |

---

### 2.9. NotificacaoDTO
Utilizado para o envio e leitura de mensagens no sistema.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `titulo` | `String` | `@NotBlank` | Obrigatório. Título da notificação. | `"Titulo está nullo"` | `"Atraso no Embarque"` | `""`, `"   "`, `null` |
| `mensagem` | `String` | `@NotBlank` | Obrigatório. Conteúdo explicativo do aviso. | `"Mensagem está nullo"` | `"O transporte chegará 10 minutos mais tarde."` | `""`, `"   "`, `null` |
| `dataCriacao` | `LocalDateTime` | `@NotNull`<br>`@Past`<br>`@JsonFormat(pattern = "yyyy-MM-dd")` | Obrigatório. Data de geração no padrão `AAAA-MM-DD`, no passado. | `"Data de criação está nullo"`<br>`"A data deve ser no passado."` | `"2026-09-01"` | `"2099-01-01"` (futuro), `null` |
| `visto` | `boolean` | *(Nenhuma)* | Indicador de visualização da notificação. | — | `true`, `false` | — |
| `remetente` | `UsuarioResumoDTO`| `@NotNull` | Obrigatório. Objeto de resumo do usuário emissor. | `"Remetente está nullo"` | `{"id": 1, "nome": "Carlos"}` | `null` |
| `remetenteId` | `Long` | `@NotNull` | Obrigatório. ID do remetente. | `"ID do remetente está nullo"` | `1` | `null` |
| `destinatarioId`| `Long` | `@NotNull` | Obrigatório. ID do destinatário. | `"ID do destinatário está nullo"` | `2` | `null` |

---

### 2.10. AlterarStatusPresencaDTO
Utilizado pelo motorista para registrar a presença de um dependente em uma execução diária.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `idViagemDia` | `Long` | `@NotNull` | Obrigatório. ID da execução diária da viagem (`ViagemDia`). | `"ID da viagem está nullo"` | `15` | `null` |
| `idDependente` | `Long` | `@NotNull` | Obrigatório. ID do aluno. | `"ID do dependente está nullo"` | `3` | `null` |
| `novoStatus` | `String` | `@NotBlank` | Obrigatório. Deve ser um dos valores do Enum `StatusViagemPresenca`: `ESPERANDO`, `EMBARCADO`, `DESEMBARCADO`, `FALTOU`. | `"Novo status está nullo"` | `"EMBARCADO"`, `"DESEMBARCADO"`, `"FALTOU"` | `""`, `"STATUS_INEXISTENTE"`, `null` |

---

### 2.11. AlterarStatusViagemDiaDTO
Utilizado para mudar o estado operacional da viagem diária.

| Campo | Tipo | Validação / Anotação | Regra de Negócio / Formato | Mensagem de Erro | Exemplo Válido | Exemplos Inválidos (Casos de Teste) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `idViagemDia` | `Long` | `@NotNull` | Obrigatório. ID da execução diária da viagem (`ViagemDia`). | `"ID da viagem está nullo"` | `15` | `null` |
| `novoStatus` | `String` | `@NotBlank` | Obrigatório. Deve ser um dos valores do Enum `StatusViagemDia`: `PLANEJADA`, `EM_ANDAMENTO`, `FINALIZADA`, `CANCELADA`. | `"Novo status está nullo"` | `"EM_ANDAMENTO"`, `"FINALIZADA"` | `""`, `"PAUSADA"`, `null` |

---

## 3. Especificações Especiais de Formato

### 3.1. Validador Customizado de CNH (`@CNH` e `CNHValidator`)
- **Campo:** `MotoristaDTO.cnh`
- **Regras:**
  1. Deve conter **exatamente 11 dígitos numéricos** (`^\d{11}$`).
  2. Não permite números com os **11 dígitos repetidos** (ex.: `00000000000`, `11111111111`, etc.).
  3. **1º Dígito Verificador (SENATRAN):** Multiplicação dos 9 primeiros dígitos pelos pesos decrescentes de 9 a 1. O resto da divisão por 11 define o dígito (se resto $\ge 10$, assume `0`).
  4. **2º Dígito Verificador (SENATRAN):** Multiplicação dos 9 primeiros dígitos pelos pesos crescentes de 1 a 9. O resto da divisão por 11 define o dígito (se resto $\ge 10$, assume `0`).
  5. Ambos os dígitos calculados devem ser exatamente iguais ao 10º e 11º dígitos informados.

### 3.2. Telefone no Padrão E.164
- **Expressão Regular:** `^\+\d{2}\d{2}\d{8,9}$`
- **Estrutura Obrigatória:**
  - Prefixo `+`
  - 2 dígitos do país (ex.: `55` para Brasil)
  - 2 dígitos do DDD (ex.: `11` para São Paulo)
  - 8 dígitos (telefone fixo) ou 9 dígitos (celular)
- **Exemplo:** `+5511988887777`

### 3.3. Placa Veicular (Mercosul e Tradicional)
- **Expressão Regular:** `^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$`
- **Formatos Aceitos:**
  - Mercosul: 3 letras, 1 número, 1 letra, 2 números (ex.: `ABC1D23`)
  - Tradicional: 3 letras, hífen opcional, 4 números (ex.: `ABC-1234` ou `ABC1234`)

---

## 4. Matriz de Endpoints e DTOs de Entrada

| Endpoint | Método HTTP | DTO / Parâmetros | Validações Ativas |
| :--- | :--- | :--- | :--- |
| `/Motorista/Cadastrar` | `POST` | `MotoristaDTO` | `@Valid @RequestBody` |
| `/Motorista/AlterarInfos` | `PUT` | `MotoristaDTO` | `@Valid @RequestBody` |
| `/Motorista/CadastrarVeiculo/{id_motorista}` | `POST` | `VeiculoDTO` | `@Valid @RequestBody` |
| `/Motorista/Autenticar` | `POST` | `UsuarioDTO` | `@Valid @RequestBody` |
| `/Responsavel/Cadastrar` | `POST` | `ResponsavelDTO` | `@Valid @RequestBody` |
| `/Responsavel/AlterarInfos` | `PUT` | `ResponsavelDTO` | `@Valid @RequestBody` |
| `/Responsavel/Autenticar` | `POST` | `UsuarioDTO` | `@Valid @RequestBody` |
| `/Responsavel/CadastrarDependente/{id_Responsavel}` | `POST` | `Long id_Responsavel`<br>`DependenteDTO dto` | `@NotNull @PathVariable`<br>`@Valid @RequestBody` |
| `/Responsavel/Dependente/AlterarInfos` | `PUT` | `DependenteDTO` | `@Valid @RequestBody` |
| `/Responsavel/ResponderSolicitacao/{id_solicitacao}` | `POST` | `Long id_solicitacao`<br>`SolicitacaoDTO dto` | `@NotNull @PathVariable`<br>`@Valid @RequestBody` |
| `/Responsavel/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}` | `POST` | `Long idResponsavel`<br>`Long idEscola`<br>`Long idDependente` | `@NotNull @PathVariable` em todos |
| `/Responsavel/ListarSolicitacoes/{idResponsavel}` | `GET` | `Long idResponsavel` | `@NotNull @PathVariable` |
| `/Responsavel/EncerrarVinculoViagem/{idSolicitacao}` | `PUT` | `Long idSolicitacao` | `@NotNull @PathVariable` |
| `/Escola/Cadastrar` | `POST` | `EscolaDTO` | `@Valid @RequestBody` |
| `/Escola/Autenticar` | `POST` | `UsuarioDTO` | `@Valid @RequestBody` |
| `/Viagem/CriarViagem/{id_motorista}` | `POST` | `Long id_motorista`<br>`ViagemDTO dto` | `@NotNull @PathVariable`<br>`@Valid @RequestBody` |
| `/Viagem/ListarViagens/{idMotorista}` | `GET` | `Long idMotorista` | `@NotNull @PathVariable` |
| `/Viagem/Visualizar/{id_viagem}` | `GET` | `Long id_viagem` | `@NotNull @PathVariable` |
| `/Viagem/VisualizarViagemDia/{id_viagemDia}` | `GET` | `Long id_viagemDia` | `@NotNull @PathVariable` |
| `/Viagem/DependentesDisponiveis/{id_viagem}` | `GET` | `Long id_viagem` | `@NotNull @PathVariable` |
| `/Viagem/SolicitarDependente/{id_viagem}/{id_dependente}` | `POST` | `Long id_viagem`<br>`Long id_dependente` | `@NotNull @PathVariable` em ambos |
| `/Viagem/IniciarViagemDia/{id_viagem}` | `POST` | `Long id_viagem` | `@NotNull @PathVariable` |
| `/Viagem/AlterarStatusViagemDia` | `PUT` | `AlterarStatusViagemDiaDTO` | `@NotNull @Valid @RequestBody` |
| `/Viagem/AlterarStatusPresenca` | `PUT` | `AlterarStatusPresencaDTO` | `@NotNull @Valid @RequestBody` |
| `/Viagem/BuscarParadasViagemDia/{idViagemDia}` | `GET` | `Long idViagemDia` | `@NotNull @PathVariable` |
| `/Notificacao/Enviar` | `POST` | `NotificacaoDTO` | `@NotNull @Valid @RequestBody` |

---

## 5. Checklist de Testes para o QA / Testador Humano

- [ ] **CT01 - Cadastro de Usuário com E-mail inválido:** Enviar `"email": "invalido"` $\rightarrow$ Deve retornar 400 com `"E-mail inválido."`.
- [ ] **CT02 - Cadastro de Usuário com Telefone fora do padrão E.164:** Enviar `"telefone": "(11) 98888-7777"` $\rightarrow$ Deve retornar 400 com `"O telefone deve estar no padrão E.164 (+5511999999999)."`.
- [ ] **CT03 - Cadastro de Motorista com CNH repetida:** Enviar `"cnh": "11111111111"` $\rightarrow$ Deve retornar 400 com `"CNH inválida."`.
- [ ] **CT04 - Cadastro de Motorista com CPF inválido:** Enviar `"cpf": "12345678901"` $\rightarrow$ Deve retornar 400 com `"CPF inválido."`.
- [ ] **CT05 - Cadastro de Veículo com placa em letras minúsculas:** Enviar `"placa": "abc1d23"` $\rightarrow$ Deve retornar 400 com `"A placa deve estar no formato (ABC1D23)."`.
- [ ] **CT06 - Cadastro de Veículo com ano fora dos limites:** Enviar `"ano": 1890` ou `2030` $\rightarrow$ Deve retornar 400 indicando limite de ano.
- [ ] **CT07 - Alteração de Status de Viagem com valor inválido:** Enviar `"novoStatus": "PAUSADA"` $\rightarrow$ Deve retornar 400.
- [ ] **CT08 - Alteração de Status de Presença com status fora do enum:** Enviar `"novoStatus": "SAIU"` $\rightarrow$ Deve retornar 400.
- [ ] **CT09 - Cadastro de Escola com perfil diferente:** Enviar `"tipoPerfil": "MOTORISTA"` na rota `/Escola/Cadastrar` $\rightarrow$ Deve ser bloqueado com 400.
- [ ] **CT10 - Cadastro de Dependente com CPF válido de 11 dígitos:** Enviar CPF válido sem máscara (11 dígitos) $\rightarrow$ Deve aceitar a validação e cadastrar o aluno normalmente.

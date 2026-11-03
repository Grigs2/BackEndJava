# GEMINI.md - Tio da Perua (BackEndJava)

Este documento fornece contexto e diretrizes para o desenvolvimento no projeto **Tio da Perua**, um sistema de back-end desenvolvido em Java para o TCC de ADS que controla:

* Usuários (Motoristas, Responsáveis, Escolas e Administradores)
* Dependentes (Alunos)
* Veículos
* Viagens (itinerários)
* Execução diária das viagens
* Presença dos alunos
* Solicitações de transporte
* Notificações.

---

## 🛠 Estrutura do Projeto

A aplicação segue uma arquitetura em camadas padrão:

- `controllers`: Endpoints REST que recebem requisições e retornam respostas DTO.
- `services`: Camada de lógica de negócio.
- `repositories`: Interfaces JpaRepository para acesso ao banco de dados.
- `entities`: Mapeamento das tabelas do banco de dados usando JPA.
- `DTOs`: Objetos de transferência de dados, frequentemente implementados como Java Records ou classes com métodos estáticos de conversão (`toDTO`, `toEntity`).

---

### Tecnologias Principais
- **Linguagem:** Java 21
- **Framework:** Spring Boot (v3.4+)
- **Persistência:** Spring Data JPA / Hibernate
- **Banco de Dados:** PostgreSQL
- **Ferramentas:** Maven, Docker, Lombok

---


## 💻 Comandos Úteis

### Build e Execução (Local)
- **Compilar e rodar testes:** `./mvnw clean install`
- **Executar a aplicação:** `./mvnw spring-boot:run`

### Docker
- **Build da imagem:** `docker build -t tiodaperua-backend .`
- **Executar via Docker:** `docker run -p 8080:8080 tiodaperua-backend`

### Configuração obrigatória de autenticação

O backend requer `JWT_SECRET_BASE64` no ambiente. Gere no mínimo 32 bytes aleatórios e informe-os em Base64; não use segredo padrão nem o versione. Exemplo PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

`JWT_ACCESS_TOKEN_MINUTES` e `JWT_REFRESH_TOKEN_DAYS` são opcionais (padrões: 15 minutos e 30 dias). Senhas novas usam BCrypt, com mínimo de 12 caracteres e máximo de 72 bytes UTF-8; senhas legadas em texto simples são migradas para hash no primeiro login válido sem reset geral. Alteração de senha exige a senha atual e revoga todos os refresh tokens do usuário; access tokens emitidos expiram normalmente. Os refresh tokens são rotativos e sua versão persistida é armazenada como hash. Payloads de entrada devem usar Jakarta Validation (`@Valid`) e manter o formato global de erro.

Configure `JWT_SECRET_BASE64` antes de iniciar localmente (`$env:JWT_SECRET_BASE64='<valor-gerado>'` no PowerShell) ou injete-o como variável de ambiente no container. Sem a chave a aplicação falha ao iniciar; isso é intencional e evita segredo padrão.

Cadastro, login e renovação são públicos. Todos os endpoints de negócio exigem `Authorization: Bearer <accessToken>` e aplicam autorização por perfil e propriedade/vínculo. O endpoint de logout revoga o refresh token; access tokens já emitidos permanecem válidos até a expiração curta.

---

O primeiro administrador é provisionado por bootstrap, sem endpoint público: configure `ADMIN_EMAIL` e `ADMIN_PASSWORD` no primeiro início; a senha deve seguir a mesma política de 12 caracteres/72 bytes. Se já existir um admin válido, essas variáveis não são necessárias. O bootstrap falha explicitamente se encontrar usuário/perfil inconsistentes; não corrige dados automaticamente. `ADMIN_BOOTSTRAP_ENABLED=false` desativa o bootstrap (por exemplo, em testes). O ADMIN não é uma Escola e autentica por `/Autenticacao/Entrar` com `tipoPerfil=ADMIN`.

Recuperação de senha usa a API SendGrid e exige `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL` (remetente verificado) e `PASSWORD_RESET_URL` (URL do frontend que recebe `token`). Os segredos devem ser injetados pelo ambiente/gerenciador de segredos, nunca versionados. O endpoint público sempre retorna mensagem genérica; tokens de uso único são armazenados apenas como hash, expiram em 15 minutos, e os pedidos têm limite de 5 por e-mail/IP por hora e cooldown de 60 segundos. A fila assíncrona de envio é limitada; falhas são registradas sem incluir e-mail ou token. A confirmação de contato permanece fora de escopo.

Configure opcionalmente `TERMS_VERSION` (versão inicial registrada: `31-10-2026`). Novos cadastros precisam enviar `aceitouTermos=true`; sem a variável, o cadastro continua permitido e o instante é guardado com versão nula. O perfil ADMIN tem endereço e telefone vazios por serem colunas legadas obrigatórias, sem uso como contato.

Endereços de dependentes são registros estruturados, editáveis e desativados logicamente; o campo textual legado permanece compatível e não deve ser interpretado automaticamente. ViaCEP é chamado com timeout curto apenas para verificar existência do CEP: indisponibilidade deixa o endereço com validação `PENDENTE`, sem bloquear o cadastro. Não persistir nem usar para sobrescrever os campos digitados os demais dados retornados pelo provedor. Cada execução diária copia os endereços de embarque/desembarque para snapshots imutáveis da presença; alterações cadastrais não podem reescrever histórico.

A rede de responsáveis mantém a tabela de associação existente para vínculos ativos e registra convites/decisões em tabela aditiva. Toda autorização deve consultar os vínculos ativos, nunca selecionar o primeiro responsável como regra universal. Só o responsável criador convida contas existentes; o destinatário precisa autenticar para aceitar/recusar. Convites expiram em 7 dias, têm categoria controlada e histórico; todos os responsáveis ativos podem responder solicitações, mas apenas o convidador ou ADMIN revoga. Ambiguidades de autoria em dados legados devem ser corrigidas explicitamente pelo ADMIN, sem inferência arbitrária.

O vínculo operacional motorista-escola é distinto do vínculo escola-dependente. `ViagemEscola` armazena cada ciclo; `HistoricoVinculoEscola` registra cada transição sem substituir decisões anteriores. Solicitações do motorista ficam `PENDENTE_ESCOLA`; convites da escola ficam `PENDENTE_MOTORISTA`, e somente a parte destinatária decide. Busca escolar retorna somente nome/CNPJ; a escola localiza motoristas por e-mail ou CPF exatos em corpo POST, e os resultados expõem somente ID/nome. Não registrar nem retornar os identificadores informados. Motivo é obrigatório na recusa, suspensão e encerramento; aprovação e reativação aceitam motivo opcional. A escola suspende/reativa; qualquer parte encerra. Recusa e encerramento permitem novo ciclo com histórico preservado. Para impedir duplicidade pendente sob concorrência, toda criação de ciclo bloqueia transacionalmente a linha da escola antes da consulta/criação. Novas colunas e a trilha são aditivas; preservar linhas legadas de `viagem_escola` sem relações/status e não interpretá-las como vínculos aprovados.

## 📝 Convenções de Desenvolvimento

- **Configuração:** As configurações principais estão em `src/main/resources/application.properties`. O context-path da API é `/TioDaPerua/api/`.
- **Injeção de Dependência:** O projeto utiliza preferencialmente `@Autowired` em campos.
- **DTOs:** Utilize DTOs para comunicação entre o front-end e o back-end. As conversões devem ser mantidas dentro das classes DTO.
- **Lombok:** Utilize anotações do Lombok (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`) para reduzir o código boilerplate em entidades.
- **Tratamento de Erros:** Atualmente, os controllers realizam verificações básicas de nulo e retornam `null` em caso de erro. Futuras melhorias devem incluir `ResponseEntity` e `ControllerAdvice`.
- **Nomenclatura:**
    - Pacotes seguem o padrão `TioDaPerua.BackEndJava.[camada]`.
    - Alguns métodos de controller utilizam PascalCase (ex: `Cadastrar`), mantenha a consistência com o código existente ou sugira refatoração.
- **Padronização de Desenvolvimento:** Os arquivos já presentes no código devem ser usado como modelo para a criação de qualquer implementação no sistema
---

## 🗄 Banco de Dados

- O banco de dados configurado é o PostgreSQL.
- O Hibernate está configurado com `ddl-auto=update`, o que significa que ele criará/atualizará as tabelas automaticamente com base nas entidades.
- As sequências de ID seguem o padrão `seq_[nome_da_tabela]`.

---

## 🧠 Domain Model (Diagrama de Classe Semântico)

---

### Classe: Usuario

* Atributos:

  * id: long
  * email: String
  * senha: String
  * endereco: String
  * telefone: String
  * tipoPerfil: String
* Responsabilidades:

  * Autenticação e identificação do sistema
* Métodos:

  * cadastrar(email, senha)
  * login(email, senha)
* Regras:

  * Um Usuario DEVE representar exatamente um perfil (Motorista, Responsavel ou Escola)

---

### Classe: Motorista

* Atributos:

  * id, nome, dataNascimento, cpf, cnh
  * usuario: Usuario
  * veiculo: Veiculo
  * viagens: List<Viagem>
* Responsabilidades:

  * Gerenciar itinerários (Viagens)
* Relações:

  * 1:1 Usuario
  * 1:1 Veiculo
  * 1:N Viagem

---

### Classe: Responsavel

* Atributos:

  * id, nome, cpf, dataNascimento
  * usuario: Usuario
  * dependentes: List<Dependente>
  * solicitacoes: List<Solicitacao>
* Responsabilidades:

  * Gerenciar dependentes e solicitações

---

### Classe: Escola

* Atributos:

  * id, nome
  * usuario: Usuario
  * alunos: List<Dependente>
* Responsabilidades:

  * Gerenciar matrícula de alunos

---

### Classe: Dependente

* Atributos:

  * id, nome, cpf, dataNascimento
  * periodo: String
  * endereco: String
  * escola: Escola
  * responsaveis: List<Responsavel>
* Responsabilidades:

  * Representar aluno transportado
* Regras:

  * DEVE ter pelo menos um Responsavel
  * DEVE estar vinculado a uma Escola para participar de viagens

---

### Classe: Veiculo

* Atributos:

  * id, modelo, placa, ano, capacidade
* Responsabilidades:

  * Representar veículo do motorista

---

## 🚍 Núcleo de Transporte (IMPORTANTE)

### Classe: Viagem (Itinerário)

* Atributos:

  * id
  * motorista: Motorista
  * ENUM: Periodo
    * MANHA_IDA
    * TARDE_IDA
    * TARDE_VOLTA
    * NOITE_IDA
    * NOITE_VOLTA
* Responsabilidades:

  * Representar um itinerário fixo (rota)
* Regras:

  * NÃO representa execução diária
  * NÃO armazena presença
  * Uma Viagem DEVE possuir um Periodo
  * NÃO pode existir mais de uma Viagem com:
    (motorista + periodo + mesma rota)

---

### Classe: ViagemDependente (Vínculo Estrutural)

* Atributos:

  * id
  * viagem: Viagem
  * dependente: Dependente
  * ativo: boolean
* Responsabilidades:

  * Definir quais alunos fazem parte da rota
* Regras:

  * NÃO deve conter data de presença
  * Vigência controlada via Solicitação

---

### Classe: ViagemDia (Execução Diária)

* Atributos:

  * id
  * viagem: Viagem
  * data: LocalDate
  * status: ENUM (PLANEJADA, EM_ANDAMENTO, FINALIZADA, CANCELADA)
* Responsabilidades:

  * Representar uma execução da viagem em um dia específico
* Regras:

  * Deve existir no máximo UMA por (viagem + data)
  Regras:
  - UNIQUE (viagem, data)
  - Representa execução da viagem
---

### Classe: ViagemPresenca (Controle de Presença) 

* Atributos:

  * id
  * viagemDia: ViagemDia
  * dependente: Dependente
  * status: ENUM (
    ESPERANDO, EMBARCADO, DESEMBARCADO, FALTOU
    )
  * horarioEmbarque: LocalDateTime
  * horarioDesembarque: LocalDateTime
* Responsabilidades:

  * Registrar presença real do aluno na viagem
* Regras:

  * Representa EVENTO (histórico)
  - Um registro por (dependente + viagemDia)

---

## 🔁 Relacionamentos Importantes

* Viagem 1:N ViagemDia
* ViagemDia 1:N ViagemPresenca
* Dependente 1:N ViagemPresenca
* Viagem 1:N ViagemDependente

---

## 📄 Classe: Solicitacao

* Atributos:

  * id
  * viagem: Viagem
  * passageiro: Dependente
  * responsavel: Responsavel
  * dataInicio: Date
  * dataFim: Date
  * aceito: boolean
  * respondido: boolean
* Responsabilidades:

  * Controlar vigência do vínculo com a viagem
* Regras:

  * Define quando o dependente participa da rota
  * Deve ser aceita para gerar vínculo ativo

---

## 🔔 Classe: Notificacao

* Atributos:

  * id
  * titulo
  * mensagem
  * data
  * visto: boolean
  * remetente: Usuario
  * destinatario: Usuario
* Responsabilidades:

  * Comunicação entre usuários

---

## 📏 Regras de Negócio

* Um Usuario DEVE ter apenas um perfil
* Uma Viagem DEVE ter um Motorista
* Uma Solicitacao aceita ATIVA define participação na rota
* ViagemDependente NÃO controla presença
* ViagemPresenca É a fonte oficial de histórico
* Um Dependente pode:
  * Estar na rota (estrutura)
  * Não comparecer (evento → Faltou)
* Toda mudança de status de Dependente deve ser feita por um motorista

---

## 🔄 Fluxos de Uso

### Fluxo: Configuração de Rota

1. Motorista cria Viagem
2. Responsavel solicita entrada do Dependente
3. Solicitação é aceita
4. Dependente passa a integrar ViagemDependente

---

### Fluxo: Execução Diária

1. Sistema cria ViagemDia automaticamente
2. Para cada dependente ativo:

  * cria ViagemPresenca (EsperandoEmbarque)

---

### Fluxo: Embarque

1. Motorista registra embarque
2. Status → Embarcado
3. Horário é salvo

---

### Fluxo: Desembarque

1. Motorista registra saída
2. Status → Desembarcado

---

### Fluxo: Falta

1. Se não embarcou:

  * status → Faltou

---

## 🏗 Regras Arquiteturais

- NÃO devem ser usados emojis no projeto
- Entities NÃO devem conter regras de negócio
- Services DEVEM conter toda lógica

- TODAS relações JPA DEVEM usar FetchType.LAZY por padrão
- Evitar FetchType.EAGER para prevenir:
  - loops infinitos
  - consumo excessivo de memória
  - problemas de serialização

- Controllers NÃO acessam repositories diretamente
- Repositories são a única camada de persistência
* DTOs DEVEM ser usados na API para entrada e saída de dados.

### Padronização de Fluxo de DTOs (OBRIGATÓRIO)
Para garantir a separação de responsabilidades e facilitar a manutenção, o projeto segue rigorosamente o seguinte fluxo de dados:

1. **Controllers** são responsáveis por receber DTOs nas requisições.
2. **Controllers** DEVEM converter o DTO para Entity (ou extrair IDs/primitivos) usando métodos `toEntity()` antes de chamar a camada de Service.
3. **Services** NÃO recebem DTOs como parâmetro. Eles trabalham exclusivamente com Entities, IDs, Enums e tipos primitivos.
4. **Services** DEVEM retornar Entities (ou tipos básicos) para o Controller.
5. **Controllers** são responsáveis por converter a Entity retornada em um DTO de resposta usando métodos `toDTO()`.

**Exemplo de Fluxo Correto:**
`Request JSON → DTO → Controller (toEntity) → Service (trabalha com Entity) → Controller (toDTO) → Response JSON`

* Toda leitura de dados DEVE passar por:
  * Service
  * Repository
  * Query explícita
    * Exemplos:
      * buscarDependentesAtivosPorData(viagemId, data)
      * listarPresencasPorViagemDia(viagemDiaId)
      * obterHistoricoDependente(dependenteId)
---

## ⚠️ Restrições Importantes

* NÃO usar Viagem para controle diário
* NÃO usar ViagemDependente para presença
* NÃO sobrescrever histórico
* SEMPRE usar ViagemPresenca para auditoria

- NÃO duplicar relações (ex: lista + tabela associativa)
- NÃO colocar lógica em Entities

- SEMPRE garantir:
  - UNIQUE (viagem_id, data) em ViagemDia
  - UNIQUE (dependente_id, viagem_dia_id) em ViagemPresenca

### Execução diária, endereços e auditoria

- `Viagem` representa o itinerário recorrente; `ViagemDia` representa uma execução datada. Datas virtuais da agenda não devem gerar linhas de execução.
- Endereços operacionais de motorista/escola usam entidade própria ligada 1:1 a `Usuario`; preservar `Usuario.endereco` legado, mas não usá-lo para liberar uma viagem.
- Uma execução materializada mantém snapshots de presenças e paradas. Reordenação só altera a ordem, nunca participantes/endereço; paradas extremas do motorista são fixas.
- Transições de estado e mudanças de ordem devem registrar ator e instante em histórico persistente; operações terminais não reabrem a execução.
- O início deve consultar prontidão, incluindo documentos vigentes, capacidade, vínculos escolares ativos e CEP validado dos endereços estruturados relevantes ao período.
---


## 🎯 Próximos Passos Sugeridos
1. Implementar validações robustas usando `jakarta.validation`.
2. Substituir retornos `null` por `ResponseEntity` com status codes adequados.
3. Adicionar documentação Swagger/OpenAPI.
4. Implementar segurança com Spring Security / JWT.
5. Respeitar e implementar a estrutura do diagrama de classe descrito em "Domain Model (Diagrama de Classe Semântico)"
6. Implementar todos os casos de Uso restante listados em "Fluxos de Uso"

## 📚 Documentação Complementar

A IA DEVE utilizar obrigatoriamente os documentos complementares presentes na pasta `/docs`.

Documentos auxiliares:

* `/Docs/`

A IA DEVE considerar TODOS os documentos como parte do mesmo contexto arquitetural do projeto.

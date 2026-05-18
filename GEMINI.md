# GEMINI.md - Tio da Perua (BackEndJava)

Este documento fornece contexto e diretrizes para o desenvolvimento no projeto **Tio da Perua**, um sistema de back-end desenvolvido em Java para o TCC de ADS que controla:

* Usuários (Motoristas, Responsáveis e Escolas)
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

---

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

* `/Docs/IMPLEMENTACAO_GUIADA.md`

  * Contém:

    * plano de implementação
    * ordem correta dos casos de uso
    * regras arquiteturais adicionais
    * responsabilidades por camada
    * regras de repositories
    * padrões obrigatórios de implementação

A IA DEVE considerar TODOS os documentos como parte do mesmo contexto arquitetural do projeto.

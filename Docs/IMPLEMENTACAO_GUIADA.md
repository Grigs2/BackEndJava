# Plano de Implementação Guiado para IA

Este plano define a estratégia obrigatória de implementação dos casos de uso restantes do sistema.

A IA DEVE seguir rigorosamente esta ordem de implementação para evitar:

* inconsistências arquiteturais
* duplicidade de lógica
* quebra de relacionamentos
* uso incorreto das entidades
* violações das regras de negócio

As implementações DEVEM reutilizar como padrão estrutural os arquivos já existentes no projeto, principalmente:

* Controllers existentes
* DTOs existentes
* Services existentes
* Conversões DTO ↔ Entity já utilizadas

A IA NÃO deve criar novos padrões arquiteturais diferentes dos já utilizados no sistema.

---

# Estratégia Geral de Desenvolvimento

A implementação DEVE seguir esta ordem:

1. Estruturas base
2. Fluxos estruturais
3. Fluxos operacionais
4. Fluxos de monitoramento
5. Fluxos auxiliares

A IA NÃO deve implementar funcionalidades isoladamente sem validar dependências anteriores.

---

# Regras Gerais de Implementação

## Controllers

Controllers DEVEM:

* Receber DTOs
* Validar nulos básicos
* Chamar Services
* Retornar DTOs

Controllers NÃO DEVEM:

* Conter regras de negócio
* Acessar repositories diretamente
* Manipular entidades diretamente

A estrutura dos endpoints DEVE seguir o padrão já existente:

```java id="f0c2n2"
@PostMapping("/Cadastrar")
public MotoristaDTO cadastrar(@RequestBody MotoristaDTO dto)
```

---

## Services

Toda regra de negócio DEVE existir em Services.

Services DEVEM:

* Validar regras
* Buscar entidades
* Controlar fluxo
* Atualizar estados
* Persistir alterações

---

## Repositories

Repositories DEVEM:

* Apenas acessar banco
* Conter queries explícitas
* NÃO conter lógica

---

## DTOs

Toda comunicação REST DEVE usar DTOs.

DTOs DEVEM:

* possuir métodos:

```java id="ckn7xj"
toDTO()
toEntity()
```

* evitar retorno direto de Entities
* evitar loops de serialização

---

# Regras Obrigatórias para JPA

TODAS relações JPA DEVEM usar:

```java id="0g6k8u"
fetch = FetchType.LAZY
```

É PROIBIDO usar:

```java id="p5k3zx"
FetchType.EAGER
```

Motivos:

* evitar recursão infinita
* evitar consumo excessivo de memória
* evitar problemas JSON

---

# Regras Obrigatórias de Arquitetura

A IA DEVE seguir obrigatoriamente:

* Entities NÃO possuem lógica
* Services concentram toda regra
* Controllers NÃO acessam repositories
* Queries DEVEM ser explícitas
* NÃO navegar entidades diretamente

É PROIBIDO:

```java id="m85t8s"
viagem.getDependentes()
viagem.getEscolas()
```

Toda leitura complexa DEVE passar por:

* Service
* Repository
* Query específica

---

# ORDEM OBRIGATÓRIA DE IMPLEMENTAÇÃO

---

# ETAPA 1 — Estrutura Base de Viagem

## Objetivo

Criar a estrutura mínima para gerenciamento de viagens.

## Implementações obrigatórias

### 1. Criar ENUMs

A IA DEVE criar:

* Periodo
* StatusViagemDia
* StatusViagemPresenca

---

### 2. Criar Entities

A IA DEVE criar:

* Viagem
* ViagemDependente
* ViagemDia
* ViagemPresenca

As entities DEVEM:

* usar Lombok
* usar JPA
* usar FetchType.LAZY
* usar relacionamentos normalizados
* NÃO conter regras de negócio

---

### 3. Criar Repositories

Criar repositories para:

* Viagem
* ViagemDependente
* ViagemDia
* ViagemPresenca

Repositories DEVEM conter queries explícitas.

---

# ETAPA 2 — Fluxo de Criação de Viagem

## Caso de Uso

"Um motorista poderá criar sua viagem"

---

## Regras

Uma viagem:

* pertence a um motorista
* possui um Periodo
* representa apenas uma rota fixa
* NÃO representa execução diária

---

## Implementações obrigatórias

### Criar:

* ViagemDTO
* ViagemController
* ViagemService

---

## Endpoint esperado

```java id="e0o5vq"
@PostMapping("/CriarViagem/{id_motorista}")
```

---

## Service DEVE validar

* motorista existente
* motorista possui veículo
* período válido
* não permitir duplicidade:

    * motorista + periodo + mesma rota

---

# ETAPA 3 — Visualização de Viagem

## Caso de Uso

"Visualizar viagem"

---

## Objetivo

Permitir visualizar:

* motorista
* endereços
* escolas
* dependentes
* responsáveis

---

## Regra crítica

A IA NÃO deve navegar entidades diretamente.

A IA DEVE criar query explícita.

---

## Implementações obrigatórias

### Criar DTOs específicos

* ViagemDetalhadaDTO
* DependenteResumoDTO
* ResponsavelResumoDTO
* EscolaResumoDTO

---

## Criar método no Service

```java id="s5m5i7"
visualizarDetalhesViagem(idViagem)
```

---

## Repository DEVE possuir query explícita

Utilizar JOIN FETCH apenas quando necessário.

---

# ETAPA 4 — Solicitação de Entrada na Viagem

## Casos de Uso

* Motorista visualiza dependentes disponíveis
* Motorista solicita entrada do dependente
* Sistema cria solicitação
* Responsável aceita ou recusa

---

# Fluxo obrigatório

1. Motorista consulta dependentes elegíveis
2. Motorista solicita vínculo
3. Sistema cria Solicitacao
4. Responsável responde
5. Sistema cria ViagemDependente quando aceito

---

## Regras obrigatórias

A IA DEVE validar:

* dependente possui escola
* dependente pertence ao responsável
* solicitação ainda não existe
* período compatível
* viagem compatível

---

## Implementações obrigatórias

### Endpoints

```java id="l3u4o7"
@GetMapping("/DependentesDisponiveis/{id_viagem}")

@PostMapping("/SolicitarDependente/{id_viagem}/{id_dependente}")

@PostMapping("/ResponderSolicitacao/{id_solicitacao}")
```

---

# ETAPA 5 — Inicialização da Viagem Diária

## Caso de Uso

"Motorista inicia ViagemDia"

---

## Objetivo

Criar execução diária da viagem.

---

## Fluxo obrigatório

1. Motorista inicia viagem
2. Sistema cria ViagemDia
3. Sistema busca dependentes ativos
4. Sistema cria ViagemPresenca automaticamente

---

## Implementações obrigatórias

### Criar método

```java id="n0h0s1"
iniciarViagemDia(idViagem)
```

---

## Service DEVE

* validar existência da viagem
* validar viagem ainda não iniciada no dia
* criar ViagemDia
* criar presenças automaticamente

---

# ETAPA 6 — Controle de Presença

## Caso de Uso

"Motorista altera status do dependente"

---

## Status válidos

* ESPERANDO
* EMBARCADO
* DESEMBARCADO
* FALTOU

---

## Regras obrigatórias

A IA DEVE:

* registrar horário de embarque
* registrar horário de desembarque
* impedir alteração inválida
* manter histórico

---

## Endpoint esperado

```java id="h9g3v2"
@PostMapping("/AlterarStatusPresenca/{id_presenca}")
```

---

# ETAPA 7 — Monitoramento do Dependente

## Caso de Uso

"Responsável monitora dependente"

---

## Objetivo

Responsável visualiza:

* viagem atual
* status atual
* histórico recente

---

## Regra crítica

O sistema DEVE utilizar:

* ViagemDia
* ViagemPresenca

NUNCA:

* ViagemDependente

---

## Implementações obrigatórias

### Criar método

```java id="x7f4k1"
monitorarDependente(idDependente)
```

---

# ETAPA 8 — Notificações

## Caso de Uso

"Enviar notificação"

---

## Objetivo

Permitir comunicação entre usuários.

---

## Implementações obrigatórias

### Criar:

* NotificacaoDTO
* NotificacaoController
* NotificacaoService

---

## Endpoint esperado

```java id="y1q6w5"
@PostMapping("/Enviar")
```

---

## Regras obrigatórias

A IA DEVE:

* validar remetente
* validar destinatário
* registrar data
* iniciar como:

```java id="qqz4of"
visto = false
```

---

# Regras Finais para IA

A IA DEVE:

* reutilizar padrões já existentes
* manter consistência de nomenclatura
* seguir arquitetura atual
* evitar abstrações desnecessárias

A IA NÃO DEVE:

* criar lógica em Entities
* usar EAGER
* duplicar relacionamentos
* acessar entidades via navegação profunda
* criar endpoints fora do padrão atual

---

# Resultado Esperado

Ao final da implementação, o sistema DEVE possuir:

* controle estrutural de rotas
* execução diária de viagens
* controle histórico de presença
* monitoramento em tempo real
* fluxo de solicitações
* comunicação entre usuários

Tudo respeitando:

* arquitetura em camadas
* normalização de dados
* separação de responsabilidades
* compatibilidade com geração automática por IA

---
# Regras Obrigatórias para Repositories

Repositories representam a camada oficial de acesso ao banco de dados.

Toda operação de persistência DEVE passar por repositories.

---

## Estrutura Obrigatória

Todos repositories DEVEM:

* extender:

```java
JpaRepository<Entity, Long>
```

* permanecer dentro do pacote:

```text
repositories
```

* utilizar:

```java
@Repository
```

quando necessário

---

## Queries

Repositories DEVEM utilizar:

* métodos derivados do Spring Data JPA
  OU
* `@Query`

A IA DEVE priorizar `@Query` quando:

* houver JOIN
* houver filtros complexos
* houver múltiplas entidades
* houver necessidade de controle explícito

---

## Exemplo de padrão obrigatório

```java
public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    @Query("""
        SELECT mot
        FROM Motorista mot
        WHERE mot.usuario.email = :email
    """)
    Optional<Motorista> findByEmail(String email);

}
```

---

## Regras Obrigatórias

Repositories NÃO DEVEM:

* conter regra de negócio
* alterar fluxo do sistema
* validar regras
* conter lógica operacional

Repositories DEVEM:

* apenas consultar
* apenas persistir
* retornar entidades corretamente

---

## Uso de Optional

Toda busca unitária DEVE retornar:

```java
Optional<Entity>
```

A validação de existência DEVE ocorrer nos Services.

Exemplo correto:

```java
Motorista motorista = motoristaRepository
    .findById(id)
    .orElseThrow(...);
```

---

## Queries Obrigatórias para o Projeto

A IA DEVE priorizar criação de queries explícitas para:

* dependentes ativos por viagem
* viagem diária por data
* histórico de presença
* solicitações pendentes
* monitoramento do dependente
* detalhes completos da viagem

---

## JOIN FETCH

A IA DEVE usar `JOIN FETCH` apenas quando:

* necessário para retorno DTO específico
* evitar LazyInitializationException

A IA NÃO DEVE usar JOIN FETCH indiscriminadamente.

---

## Regra Crítica

Controllers NÃO acessam repositories diretamente.

Fluxo obrigatório:

```text
Controller → Service → Repository
```

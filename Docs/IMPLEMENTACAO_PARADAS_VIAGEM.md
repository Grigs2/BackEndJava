# IMPLEMENTACAO_PARADAS_VIAGEM.md

# Objetivo

Implementar o sistema de geração e visualização de paradas de uma ViagemDia.

O objetivo é permitir que o motorista visualize a sequência de locais que devem ser percorridos durante a execução diária da rota, incluindo:

- Casa do motorista
- Casas dos responsáveis
- Escolas
- Dependentes relacionados a cada parada

A implementação DEVE respeitar toda a arquitetura existente do projeto descrita em:

- GEMINI.md
- IMPLEMENTACAO_GUIADA.md

---

# Conceito Arquitetural

A lista de paradas NÃO representa estrutura permanente da rota.

Ela representa:

- uma projeção operacional
- um snapshot diário
- uma visão calculada da execução da viagem naquele dia

Portanto:

- NÃO deve ser persistida em banco
- NÃO deve existir entidade Parada
- NÃO deve existir tabela parada_viagem
- NÃO deve existir relacionamento JPA adicional

A lista de paradas deve ser gerada dinamicamente através da camada Service.

---

# Motivação Técnica

Persistir as paradas geraria problemas:

- duplicação de dados
- inconsistência entre endereço atual e endereço salvo
- necessidade de sincronização
- aumento desnecessário da complexidade

Como os dados já existem em:

- Usuario.endereco
- Escola
- Dependente
- Responsavel
- ViagemDependente
- ViagemPresenca

A lista pode ser reconstruída dinamicamente sempre que necessário.

---

# Regras de Negócio

## Ordem da Viagem

A ordem das paradas depende do tipo/período da viagem.

---

# Regras de Ida

Para viagens:

- MANHA_IDA
- TARDE_IDA
- NOITE_IDA

A ordem DEVE ser:

1. Casa do motorista
2. Casas dos responsáveis
3. Escolas
4. Casa do motorista

---

# Regras de Volta

Para viagens:

- TARDE_VOLTA
- NOITE_VOLTA

A ordem DEVE ser:

1. Casa do motorista
2. Escolas
3. Casas dos responsáveis
4. Casa do motorista

---

# Agrupamento Obrigatório

Paradas DEVEM ser agrupadas por endereço.

Exemplo:

Escola A:
- Dependente 1
- Dependente 2

Casa Responsável X:
- Dependente 3
- Dependente 4

NÃO pode existir repetição de parada para o mesmo endereço.

---

# Origem do Endereço

## Casa do Motorista

Origem:
- motorista.usuario.endereco

---

## Casa do Dependente

Origem:
- primeiro responsável do dependente

Regra:
- utilizar:
  dependente.getResponsaveis().get(0)

Origem do endereço:
- responsavel.usuario.endereco

---

## Escola

Origem:
- dependente.getEscola()

---

# Regras Importantes

## Dependentes ativos

A lista de paradas DEVE considerar apenas:

- dependentes ativos na viagem

Fonte oficial:
- ViagemDependente

---

## ViagemDia

A lista DEVE ser vinculada à execução diária.

A geração das paradas acontece:

- quando a ViagemDia é iniciada
  OU
- quando o aplicativo solicitar os detalhes da ViagemDia

---

# Decisão Arquitetural Obrigatória

A implementação DEVE utilizar:

- DTOs de visualização
- objetos calculados em memória
- services especializados

NÃO deve criar:
- entities
- repositories
- migrations
- tabelas

para representar paradas.

---

# Estrutura Recomendada

Criar DTOs específicos para visualização.

---

# DTO: ParadaDTO

Representa uma parada da viagem.

Campos sugeridos:

- ordem
- tipoParada
- nomeLocal
- endereco
- listaDependentes

---

# DTO: DependenteParadaDTO

Campos sugeridos:

- id
- nomeDependente
- nomeResponsavel

---

# Enum recomendado

Criar enum:

TipoParada:

- MOTORISTA
- RESPONSAVEL
- ESCOLA

---

# Fluxo de Implementação

## Etapa 1

Criar DTOs:

- ParadaDTO
- DependenteParadaDTO

---

## Etapa 2

Criar método no ViagemService:

```java
public List<ParadaDTO> gerarParadasViagemDia(Long idViagemDia)
```
---
## Etapa 3

Buscar:

* ViagemDia
* Viagem
* Dependentes ativos

Utilizar repositories existentes.

---

## Etapa 4

Separar lógica por tipo da viagem.

Sugestão:
```java
private boolean isViagemIda(Periodo periodo)
```
---
## Etapa 5

Criar agrupamentos em memória.

Sugestão:
```java
Map<String, ParadaDTO>
```

Chave:

* endereço

---

## Etapa 6

Montar lista ordenada de paradas.

IMPORTANTE:

* a ordem final deve ser estável
* evitar duplicações

---

## Estrutura Recomendada de Service
### Método principal

Responsável apenas por:

* validar
* buscar dados
* coordenar montagem 

---

### Métodos privados recomendados

#### adicionarParadasResponsaveis()

Responsável por:

* agrupar casas
* relacionar dependentes 

---

#### adicionarParadasEscolas()

Responsável por:

* agrupar escolas
* relacionar dependentes

---

#### criarParadaMotorista()

Responsável por:

* montar parada inicial/final


---
## Responsabilidades por Camada
### Controller

Responsável por:

* receber request
* converter DTOs
* retornar DTOs

NÃO deve:

* montar lógica de paradas

---

### Service

Responsável por:

* TODA regra de negócio
* agrupamento
* ordenação
* geração das paradas

---

### Repository

Responsável apenas por:

* queries
* leitura de dados

NÃO deve:

* montar DTOs complexos
* aplicar regra de negócio

---

### Repositories Recomendados

Caso necessário criar queries novas.

---

### ViagemDiaRepository

Sugestão:
```java
Optional<ViagemDia> findById(Long id);
```

---

### ViagemDependenteRepository

Sugestão:
```java
@Query("""
SELECT vd
FROM ViagemDependente vd
JOIN FETCH vd.dependente d
JOIN FETCH d.escola
JOIN FETCH d.responsaveis r
JOIN FETCH r.usuario
WHERE vd.viagem.id = :idViagem
AND vd.ativo = true
""")
List<ViagemDependente> buscarDependentesCompletos(Long idViagem);
```


Objetivo:

* evitar LazyInitializationException
* evitar N+1

---

### Endpoint Recomendado

Controller:
```java
@GetMapping("/BuscarParadasViagemDia/{idViagemDia}")
```

Retorno:
```java
List<ParadaDTO>
```

---

### Exemplo Esperado de Retorno
```json
[
  {
    "ordem": 1,
    "tipoParada": "MOTORISTA",
    "nomeLocal": "Casa Motorista",
    "endereco": "Rua X",
    "listaDependentes": []
  },
  {
    "ordem": 2,
    "tipoParada": "RESPONSAVEL",
    "nomeLocal": "Casa João",
    "endereco": "Rua Y",
    "listaDependentes": [
      {
        "id": 1,
        "nomeDependente": "Pedro",
        "nomeResponsavel": "João"
      }
    ]
  }
]
 ```

---

## Regras Obrigatórias
* NÃO adicionar lógica em Entities
* NÃO criar persistência desnecessária
* NÃO acessar repository diretamente no Controller
* NÃO retornar Entities na API
* SEMPRE utilizar DTOs
* SEMPRE manter separação de responsabilidades

---

## Objetivo Final

O motorista deve conseguir:

* visualizar sequência operacional da rota
* saber quem embarca/desembarca em cada parada
* visualizar escolas agrupadas
* visualizar responsáveis agrupados
* executar a rota de forma clara e organizada

sem duplicação estrutural no banco de dados.
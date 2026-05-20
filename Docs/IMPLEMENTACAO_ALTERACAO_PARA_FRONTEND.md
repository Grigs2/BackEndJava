# Guia de Implementação — BackEnd

## Projeto TioDaPerua

### Documento direcionado para implementação via IA Gemini CLI

Este documento define as alterações necessárias no BackEnd do sistema, incluindo:

* alterações estruturais no banco;
* novos endpoints;
* mudanças de comportamento;
* atualização de DTOs;
* regras de negócio;
* impactos em queries existentes;
* atualização da documentação da API.

O objetivo é garantir compatibilidade futura com o FrontEnd e resolver problemas atuais de usabilidade e fluxo operacional.

---

# 1. ALTERAÇÃO DE ENTIDADE — VIAGEM

## Objetivo

Permitir distinguir viagens ativas de viagens encerradas/finalizadas.

---

## Alteração necessária

Adicionar novo campo boolean na entidade `Viagem`.

### Campo novo

```java
private Boolean ativo;
```

---

## Regras

### Valor padrão

Ao criar uma nova viagem:

```java
ativo = true
```

---

## Banco de dados

### Migration SQL

Adicionar coluna:

```sql
ALTER TABLE viagem
ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
```

---

## DTOs afetados

Todos os DTOs que representam Viagem devem conter:

```java
private Boolean ativo;
```

Incluindo:

* ViagemDTO
* ViagemResumoDTO
* ViagemDiaDTO (se aplicável)

---

# 2. NOVO ENDPOINT — LISTAR VIAGENS ATIVAS POR MOTORISTA

## Endpoint

```http
GET /Viagem/ListarViagens/{id_motorista}
```

---

## Objetivo

Retornar apenas viagens ativas vinculadas ao motorista informado.

---

## Regra obrigatória

Retornar apenas:

```sql
ativo = true
```

---

## Query esperada

Pseudo-query:

```sql
SELECT *
FROM viagem
WHERE motorista_id = :idMotorista
AND ativo = true
```

---

## Resposta

Lista de:

```java
List<ViagemDTO>
```

---

## Casos de erro

### Motorista inexistente

```http
404 NOT FOUND
```

### Motorista sem viagens

Retornar:

```http
200 OK
[]
```

---

# 3. NOVO ENDPOINT — DESATIVAR VIAGEM

## Endpoint

```http
PUT /Viagem/DesativarViagem/{id_viagem}
```

---

# Objetivo

Finalizar completamente uma viagem ativa.

---

# Regras de negócio

Ao desativar uma viagem:

---

## 1. Alterar Viagem

```java
viagem.ativo = false;
```

---

## 2. Alterar todos vínculos Viagem_dependente

Todos os registros:

```sql
viagem_dependente
```

que possuem:

```sql
viagem_id = id_viagem
```

devem ter:

```java
ativo = false
```

---

## 3. Finalizar solicitações relacionadas

Todas as solicitações que possuem:

```sql
viagem_id = id_viagem
```

e que:

```sql
data_fim IS NULL
```

devem receber:

```java
data_fim = now()
```

---

# Fluxo transacional obrigatório

Essa operação deve ser feita em transação única.

Exemplo:

```java
@Transactional
```

---

# Ordem recomendada

1. Buscar viagem
2. Validar existência
3. Alterar viagem.ativo
4. Atualizar viagem_dependente
5. Atualizar solicitações
6. Commit

---

# Casos de erro

## Viagem inexistente

```http
404 NOT FOUND
```

---

## Viagem já desativada

Sugestão:

```http
409 CONFLICT
```

Mensagem:

```json
{
  "erro": "Viagem já finalizada"
}
```

---

# 4. NOVO ENDPOINT — HISTÓRICO DE VIAGENS FINALIZADAS

## Endpoint

```http
GET /Viagem/Historico/{id_motorista}
```

---

## Objetivo

Retornar viagens finalizadas do motorista.

---

## Regra obrigatória

Retornar:

```sql
ativo = false
```

---

## Query esperada

```sql
SELECT *
FROM viagem
WHERE motorista_id = :idMotorista
AND ativo = false
ORDER BY data DESC
```

---

## Resposta

```java
List<ViagemDTO>
```

---

# 5. ALTERAÇÃO — DEPENDENTEDTO

## Objetivo

Permitir que responsáveis visualizem corretamente a escola vinculada aos dependentes.

---

# Alteração obrigatória

Adicionar:

```java
private EscolaResumoDTO escola;
```

em:

```java
DependenteDTO
```

---

# Regras

A escola deve ser carregada:

* no listar dependentes;
* autenticação;
* endpoints de detalhes.

---

# Atenção

Evitar:

```java
LazyInitializationException
```

Utilizar:

* JOIN FETCH;
* projection;
* mapper adequado.

---

# 6. NOVOS ENDPOINTS — ALTERAR INFORMAÇÕES DE PERFIL

---

# Endpoints

```http
PUT /Responsavel/AlterarInfos
PUT /Dependente/AlterarInfos
PUT /Motorista/AlterarInfos
PUT /Escola/AlterarInfos
```

---

# Objetivo

Permitir atualização completa de cadastro.

---

# Regras obrigatórias

O endpoint deve receber:

* DTO completo;
* incluindo senha preenchida.

---

# Validação obrigatória

Antes de atualizar:

```java
BCrypt.checkpw(...)
```

ou equivalente.

---

# Fluxo esperado

1. Buscar usuário atual
2. Validar senha enviada
3. Se inválida:

   ```http
   401 UNAUTHORIZED
   ```
4. Atualizar campos permitidos
5. Salvar
6. Retornar DTO atualizado

---

# IMPORTANTE

Não permitir alteração:

* ID
* permissões
* relacionamentos críticos

---

# Recomendação

Criar DTO específico:

```java
AlterarInfosDTO
```

ao invés de reutilizar DTO de autenticação.

---

# Campos recomendados

## Responsável

* nome
* telefone
* email
* senha

## Motorista

* nome
* telefone
* email
* senha
* veículo

## Escola

* nome
* endereço
* telefone
* senha

## Dependente

* nome
* escola
* senha

---

# 7. NOVO ENDPOINT — CONSULTAR MOTORISTA DA VIAGEM DIA

## Endpoint

```http
GET /Viagem/ConsultarMotorista/{id_viagemDia}
```

---

# Objetivo

Permitir que responsáveis visualizem:

* motorista;
* veículo;
* informações resumidas.

---

# Novo DTO obrigatório

```java
MotoristaViagemDTO
```

---

# Estrutura sugerida

```java
public class MotoristaViagemDTO {

    private Long motoristaId;

    private String nomeMotorista;

    private String telefone;

    private String modeloVeiculo;

    private String placaVeiculo;

    private String corVeiculo;
}
```

---

# Query

Relacionar:

* viagem_dia
* viagem
* motorista

---

# Casos de erro

## ViagemDia inexistente

```http
404 NOT FOUND
```

---

# 8. ALTERAÇÃO — ENCERRAR VÍNCULO VIAGEM

## Endpoint atual

```http
/Responsavel/EncerrarVinculoViagem
```

---

# Novo comportamento

Passar ID pela URL:

```http
PUT /Responsavel/EncerrarVinculoViagem/{idSolicitacao}
```

---

# Remover

Uso de DTO contendo:

```java
idSolicitacao
```

---

# Controller esperado

Exemplo:

```java
@PutMapping("/EncerrarVinculoViagem/{idSolicitacao}")
```

---

# Benefícios

* endpoint REST mais correto;
* simplifica FrontEnd;
* reduz payload desnecessário.

---

# 9. ALTERAÇÃO — LISTARSOLICITACOES

## Objetivo

Ocultar solicitações já encerradas.

---

# Regra obrigatória

Retornar apenas:

```sql
data_fim IS NULL
```

---

# Query esperada

```sql
SELECT *
FROM solicitacao
WHERE data_fim IS NULL
```

---

# Impacto esperado

A funcionalidade:

* deixa de mostrar histórico;
* passa a mostrar apenas vínculos ativos.

---

# 10. DOCUMENTAÇÃO DA API

## Arquivo obrigatório

Atualizar:

```text
API_DOCUMENTACAO_COMPLETA.md
```

---

# Deve incluir

## Novos endpoints

* exemplos request;
* exemplos response;
* erros possíveis.

---

## Novos DTOs

* MotoristaViagemDTO
* EscolaResumoDTO dentro de DependenteDTO
* AlterarInfosDTOs

---

## Regras de negócio

* viagens ativas;
* finalização;
* encerramento automático de solicitações.

---

## Atualizações de endpoints antigos

* EncerrarVinculoViagem
* listarSolicitacoes

---

# 11. RECOMENDAÇÕES TÉCNICAS IMPORTANTES

---

# Transactions

Usar:

```java
@Transactional
```

em:

* DesativarViagem;
* AlterarInfos.

---

# Soft delete lógico

A implementação de:

```java
ativo = false
```

deve ser tratada como soft delete funcional.

Nunca deletar viagens fisicamente.

---

# Segurança

Validar:

* ownership;
* permissões;
* autenticação.

Principalmente em:

* AlterarInfos;
* DesativarViagem.

---

# Padronização de resposta

Sugestão:

## Sucesso

```json
{
  "mensagem": "Operação realizada com sucesso"
}
```

## Erro

```json
{
  "erro": "Descrição"
}
```

---

# 12. PRIORIDADE DE IMPLEMENTAÇÃO

## Alta prioridade

1. Campo ativo
2. DesativarViagem
3. listarSolicitacoes
4. ListarViagens

---

## Média prioridade

5. Histórico
6. ConsultarMotorista
7. DependenteDTO escola

---

## Baixa prioridade

8. AlterarInfos
9. Atualização documentação

---

# 13. RESULTADO ESPERADO

Após implementação:

* o sistema terá separação entre viagens ativas e históricas;
* vínculos serão encerrados corretamente;
* responsáveis terão melhor visibilidade;
* o FrontEnd conseguirá trabalhar com menos inconsistências;
* o fluxo operacional ficará mais previsível;
* testes de integração terão ambiente mais estável;
* UX geral do sistema será significativamente melhorada.

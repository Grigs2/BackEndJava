# IMPLEMENTACAO_STATUS_VIAGEM_E_ENCERRAMENTO_VINCULO.md

## Objetivo

Este documento descreve o plano de implementação de duas novas funcionalidades no sistema Tio da Perua:

1. Permitir que o motorista altere o status de uma `ViagemDia`.
   2. Um novo campo que marque o último horário (Data e Hora) da alteração de status de uma viagem deve ser criado 
2. Permitir que o responsável encerre o vínculo de um dependente com uma viagem.

Este documento complementa o `GEMINI.md` e deve ser seguido integralmente pela IA Gemini CLI.

---

# 1. Alterar Status de ViagemDia

## Objetivo da Regra de Negócio

O motorista deve poder alterar manualmente o status de uma `ViagemDia`.

### Status disponíveis (`StatusViagemDia`)

* `PLANEJADA`
* `EM_ANDAMENTO`
* `FINALIZADA`
* `CANCELADA`

---

## Regras de Negócio

### PLANEJADA → EM_ANDAMENTO

* Indica o início efetivo da execução da viagem.

### EM_ANDAMENTO → FINALIZADA

* Indica que a rota foi concluída.

### PLANEJADA → CANCELADA

* Viagem cancelada antes do início.

### Restrições

* Não permitir alteração se a `ViagemDia` não existir.
* O status informado deve existir no enum.
* O sistema deve persistir a alteração.

---

## Endpoint

### Tipo

`PUT`

### URL

```text
/Viagem/AlterarStatusViagemDia
```

### Request Body

```json
{
  "idViagemDia": 1,
  "novoStatus": "EM_ANDAMENTO"
}
```

### Response

```json
{
  "id": 1,
  "data": "2025-11-24",
  "dataUltimaAlteracaoStatus": "2025-11-24T07:10:00",
  "status": "EM_ANDAMENTO"
}
```

---

## DTO

### AlterarStatusViagemDiaDTO.java

```java
import java.time.LocalDateTime;

public record AlterarStatusViagemDiaDTO(
        Long idViagemDia,
        LocalDateTime dataUltimaAlteracaoStatus,
        String novoStatus
) {
}
```

---

## Service

### Assinatura

```java
public ViagemDia alterarStatusViagemDia(
    Long idViagemDia,
    String novoStatus
)
```

### Fluxo

1. Validar parâmetros.
2. Buscar `ViagemDia`.
3. Validar existência.
4. Converter `novoStatus` para `StatusViagemDia`.
5. Salvar horário da conversão de status no novo campo `dataUltimaAlteracaoStatus`.
6. Atualizar status.
7. Salvar.
8. Retornar entidade.

---
### Entidade

Qualquer tipo de alteração nas entidades do banco que precisem serem feitas para que a última data de alteração
do status de uma viagem seja persistida e manipulada corretamente pelo sistema deve ser feita respeitando
a arquitetura do projeto.

---

## Controller

```java
@PutMapping("/AlterarStatusViagemDia")
public ViagemDiaDTO alterarStatusViagemDia(
        @RequestBody AlterarStatusViagemDiaDTO dto) {

    if (dto == null) return null;

    ViagemDia viagemDia = viagemService.alterarStatusViagemDia(
            dto.idViagemDia(),
            dto.novoStatus()
    );

    if (viagemDia == null) return null;

    return ViagemDiaDTO.toDTO(viagemDia);
}
```

---

# 2. Encerrar Vínculo de Dependente com Viagem

## Objetivo da Regra de Negócio

O responsável deve poder encerrar a participação de um dependente em uma viagem.

---

## Regras de Negócio

Ao encerrar o vínculo, o sistema deve:

1. Localizar a solicitação aceita e ativa.
2. Definir:

    * `dataFim = LocalDate.now()`
3. Garantir:

    * `respondido = true`
    * `aceito = true`
4. Localizar o `ViagemDependente` correspondente.
5. Definir:

    * `ativo = false`
6. Persistir ambas as entidades.

---

## Impacto Funcional

Após o encerramento:

* O dependente deixa de aparecer em:

    * Buscar dependentes ativos.
    * Iniciar `ViagemDia`.
    * Buscar paradas da viagem.
    * Monitoramento do responsável.

* O histórico permanece preservado.

---

## Endpoint

### Tipo

`PUT`

### URL

```text
/Responsavel/EncerrarVinculoViagem
```

### Request Body

```json
{
  "idSolicitacao": 10
}
```

---

## DTO

### EncerrarVinculoViagemDTO.java

```java
public record EncerrarVinculoViagemDTO(
    Long idSolicitacao
) {}
```

# Padronização de ResponseEntity

## Objetivo

Esta documentação descreve a estratégia de padronização das respostas HTTP adotada no projeto **Tio da Perua (BackEndJava)**. O objetivo é garantir consistência, facilitar a manutenção e fornecer feedback adequado aos consumidores da API (Front-end).

A estratégia baseia-se no uso sistemático de `ResponseEntity<T>` em todos os Controllers e no lançamento de exceções específicas na camada de Service, tratadas globalmente por um `@RestControllerAdvice`.

## Status HTTP Utilizados

| Status | Uso |
|----------|----------|
| 200 OK | Consultas realizadas com sucesso e atualizações bem-sucedidas. |
| 201 CREATED | Cadastro de novos registros realizado com sucesso. |
| 204 NO CONTENT | Operação válida processada com sucesso que não requer corpo de resposta. |
| 400 BAD REQUEST | Dados de entrada inválidos, DTOs malformados ou violação de regras de negócio simples. |
| 401 UNAUTHORIZED | Falha de autenticação (email/senha inválidos) ou falta de credenciais. |
| 403 FORBIDDEN | Operação não permitida para o perfil do usuário atual. |
| 404 NOT FOUND | Registro solicitado não encontrado no banco de dados. |
| 409 CONFLICT | Conflitos de estado, como tentativa de cadastrar registro duplicado ou operação em recurso já desativado. |
| 500 INTERNAL SERVER ERROR | Erros inesperados no servidor. |

## Endpoints Alterados

Todos os endpoints dos seguintes controllers foram padronizados:

- `EscolaController`
- `MotoristaController`
- `ResponsavelController`
- `ViagemController`
- `NotificacaoController`

### Antes vs. Depois (Exemplos)

| Controller | Endpoint | Antes | Depois |
|------------|-----------|--------|---------|
| EscolaController | `/Cadastrar` | `EscolaDTO` (null se erro) | `ResponseEntity<EscolaDTO>` (201 Created ou 400) |
| MotoristaController | `/Autenticar` | `MotoristaDTO` (null se erro) | `ResponseEntity<MotoristaDTO>` (200 OK ou 401) |
| ViagemController | `/DesativarViagem` | `ResponseEntity<?>` (manual status) | `ResponseEntity<?>` (Exceção lançada pelo Service) |
| ResponsavelController | `/ListarEscolas` | `List<EscolaDTO>` | `ResponseEntity<List<EscolaDTO>>` (200 OK) |

## Exceções Tratadas

As seguintes exceções customizadas foram implementadas e são mapeadas para status HTTP específicos:

1.  **ResourceNotFoundException**: Mapeia para **404 Not Found**.
2.  **BusinessException**: Mapeia para **400 Bad Request**.
3.  **UnauthorizedException**: Mapeia para **401 Unauthorized**.
4.  **ConflictException**: Mapeia para **409 Conflict**.
5.  **IllegalArgumentException**: Mapeia para **400 Bad Request** (geralmente para validações simples).

Além disso, um tratador genérico para `Exception.class` garante que qualquer erro não previsto retorne **500 Internal Server Error** em um formato JSON padronizado.

## Formato de Erro Padronizado

Em caso de erro, a API retorna o seguinte objeto JSON:

```json
{
  "timestamp": "2026-05-26T02:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Escola não encontrada com id: 1",
  "path": "/TioDaPerua/api/Escola/1"
}
```

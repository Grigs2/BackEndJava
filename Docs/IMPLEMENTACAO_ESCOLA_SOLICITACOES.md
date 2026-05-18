# IMPLEMENTACAO_ESCOLA_SOLICITACOES.md

## Objetivo

Este documento define o plano de implementação das seguintes funcionalidades no projeto **Tio da Perua**:

1. Responsável deve poder visualizar solicitações de transporte dos seus dependentes.
2. Cadastro de Escola.
3. Autenticação (login) de Escola.
4. Responsável deve poder visualizar a lista de escolas cadastradas.
5. Responsável deve poder vincular uma escola a um ou mais dependentes sob sua responsabilidade.

A implementação deve seguir integralmente as regras definidas em `GEMINI.md` e demais documentos da pasta `/Docs`.

---

# 1. Regras Arquiteturais Obrigatórias

## 1.1 Fluxo Controller → DTO → Service → Repository

Fluxo obrigatório:

```text
Request JSON
→ DTO
→ Controller (toEntity / extração de IDs)
→ Service
→ Repository
→ Service
→ Controller (toDTO)
→ Response JSON
```

## 1.2 Regras por camada

### Controllers

* Recebem DTOs.
* Validam parâmetros nulos.
* Convertem DTOs para Entity.
* Nunca acessam repositories.
* Convertem retorno do Service para DTO.

### Services

* Contêm toda a lógica de negócio.
* Recebem Entities, IDs, enums e tipos primitivos.
* Utilizam repositories.
* Retornam Entities.

### Repositories

* São a única camada de acesso ao banco.
* Devem conter queries explícitas quando necessário.

### Entities

* Não devem conter lógica de negócio.

### DTOs

* Responsáveis por `toDTO()` e `toEntity()`.

---

# 2. Funcionalidades a Implementar

---

# 2.1 Cadastro de Escola

## Objetivo

Permitir que um usuário com perfil `ESCOLA` seja cadastrado no sistema.

## Fluxo de negócio

1. Cliente envia `EscolaDTO` contendo:

    * nome
    * usuarioDTO
2. `usuarioDTO.tipoPerfil` deve ser `ESCOLA`.
3. Sistema cria `Usuario`.
4. Sistema cria `Escola` vinculada ao usuário.
5. Retorna `EscolaDTO` completo.

## Endpoint

```http
POST /Escola/Cadastrar
```

## Controller

Criar `EscolaController`.

## Service

Criar método:

```java
Escola cadastrarEscola(Escola escola)
```

## Repository

Criar:

```java
public interface EscolaRepository extends JpaRepository<Escola, Long>
```

---

# 2.2 Login de Escola

## Objetivo

Permitir autenticação de usuários do tipo `ESCOLA`.

## Fluxo

1. Recebe `UsuarioDTO` com email e senha.
2. Usa `UsuarioService.autenticar()`.
3. Valida se `tipoPerfil == ESCOLA`.
4. Busca entidade `Escola` vinculada.
5. Retorna `EscolaDTO`.

## Endpoint

```http
GET /Escola/Autenticar
```

## Repository

Adicionar em `EscolaRepository`:

```java
@Query("SELECT e FROM Escola e WHERE e.usuario.id = :id")
Optional<Escola> findByUsuarioId(Long id);
```

## UsuarioService

Criar método:

```java
Escola logarEscola(Usuario usuario)
```

---

# 2.3 Listar Escolas

## Objetivo

Permitir que o responsável visualize todas as escolas cadastradas.

## Fluxo

1. Responsável chama endpoint.
2. Service consulta `escolaRepository.findAll()`.
3. Retorna lista de `EscolaDTO`.

## Endpoint

```http
GET /Responsavel/ListarEscolas
```

## Service

Adicionar em `ResponsavelService` ou `EscolaService`:

```java
List<Escola> listarEscolas();
```

---

# 2.4 Vincular Escola a Dependentes

## Objetivo

Permitir que um responsável associe uma escola já cadastrada a um ou mais dependentes sob sua responsabilidade.

## Regras de negócio

* A escola deve existir.
* O responsável deve existir.
* Todos os dependentes informados devem pertencer ao responsável.
* Cada dependente selecionado passa a referenciar a escola escolhida.
* O retorno deve conter o `ResponsavelDTO` atualizado com os dependentes já vinculados à escola.

## Fluxo

1. Recebe:

    * `idResponsavel`
    * `idEscola`
    * `idDependente`
2. Validar existência do responsável.
3. Validar existência da escola.
4. Para o ID do Dependente:

    * buscar o dependente vinculado ao responsável.
    * validar que o dependente existe.
    * definir `dependente.setEscola(escola)`.
    * persistir a alteração.
5. Retornar o responsável atualizado.

## Endpoint

```http
POST /Responsavel/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}
```


## Service

```java
Responsavel vincularEscola(Long idResponsavel,
                           Long idEscola,
                           Long idDependente)
```

## Repository necessário

Crie e maneje repositórios necessários para implementação dessa funcionalidade.

## Objetivo

Permitir que um responsável associe uma escola já cadastrada aos dependentes sob sua responsabilidade.

## Regras de negócio

* O dependente deve pertencer ao responsável informado.
* A escola deve existir.
* Um dependente passa a referenciar a escola escolhida.

## Fluxo

1. Recebe:

    * `idEscola`
    * lista de `idsDependentes`
2. Validar existência do responsável.
3. Validar existência da escola.
4. Para cada dependente:

    * validar que pertence ao responsável.
    * definir `dependente.setEscola(escola)`.
    * salvar.
5. Retornar `ResponsavelDTO` atualizado.

## Endpoint

```http
POST /Responsavel/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}
```


## Service

```java
Responsavel vincularEscola(Long idResponsavel,
                           Long idEscola,
                           Long idDependente)
```

## Repository necessário

### DependenteRepository

```java
@Query("""
SELECT d
FROM Dependente d
JOIN d.responsaveis r
WHERE d.id = :dependenteId
AND r.id = :responsavelId
""")
Optional<Dependente> findByIdAndResponsavelId(Long dependenteId,
                                             Long responsavelId);
```

---

# 2.5 Responsável Visualizar Solicitações

## Objetivo

Permitir que o responsável visualize todas as solicitações relacionadas aos seus dependentes.

## Regras de negócio

Uma solicitação deve aparecer quando:

* `solicitacao.responsavel.id == idResponsavel`

## Endpoint

```http
GET /Responsavel/ListarSolicitacoes/{idResponsavel}
```

## Service

```java
List<Solicitacao> listarSolicitacoes(Long idResponsavel)
```

## Repository

```java
@Query("""
SELECT s
FROM Solicitacao s
WHERE s.responsavel.id = :idResponsavel
ORDER BY s.id DESC
""")
List<Solicitacao> findByResponsavelId(Long idResponsavel);
```

## Retorno

Lista de `SolicitacaoDTO`.

---

# 3. DTOs Necessários

## EscolaDTO

Campos mínimos:

* id
* nome
* usuarioDTO

Métodos:

```java
static EscolaDTO toDTO(Escola entity)
static Escola toEntity(EscolaDTO dto)
```

---

## SolicitaçãoDTO

Deve já existir. Caso necessário, garantir campos:

* id
* viagemDTO
* passageiroDTO
* responsavelDTO
* dataInicio
* dataFim
* aceito
* respondido

---

# 4. Controllers

---

## EscolaController

Criar endpoints:

```java
@PostMapping("/Cadastrar")
public EscolaDTO cadastrar(@RequestBody EscolaDTO dto)

@GetMapping("/Autenticar")
public EscolaDTO autenticar(@RequestBody UsuarioDTO dto)
```

---

## ResponsavelController

Adicionar endpoints:

```java
@GetMapping("/ListarEscolas")
public List<EscolaDTO> listarEscolas()

@PostMapping("/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}")
public ResponsavelDTO vincularEscola(
        @PathVariable Long idResponsavel,
        @PathVariable Long idEscola,
        @PathVariable Long idDependente)

@GetMapping("/ListarSolicitacoes/{idResponsavel}")
public List<SolicitacaoDTO> listarSolicitacoes(@PathVariable Long idResponsavel)
```

---

# 5. Services

## EscolaService

Métodos:

```java
Escola cadastrarEscola(Escola escola)
List<Escola> listarEscolas()
```

---

## UsuarioService

Adicionar:

```java
Escola logarEscola(Usuario usuario)
```

---

## ResponsavelService

Adicionar:

```java
Responsavel vincularEscola(Long idResponsavel,
                           Long idEscola,
                           List<Long> idsDependentes)

List<Solicitacao> listarSolicitacoes(Long idResponsavel)
```

---

# 6. Repositories

## EscolaRepository

```java
public interface EscolaRepository extends JpaRepository<Escola, Long> {

    @Query("SELECT e FROM Escola e WHERE e.usuario.id = :id")
    Optional<Escola> findByUsuarioId(Long id);
}
```

---

## SolicitacaoRepository

Adicionar:

```java
@Query("""
SELECT s
FROM Solicitacao s
WHERE s.responsavel.id = :idResponsavel
ORDER BY s.id DESC
""")
List<Solicitacao> findByResponsavelId(Long idResponsavel);
```

---

## DependenteRepository

Adicionar:

```java
@Query("""
SELECT d
FROM Dependente d
JOIN d.responsaveis r
WHERE d.id = :dependenteId
AND r.id = :responsavelId
""")
Optional<Dependente> findByIdAndResponsavelId(Long dependenteId,
                                             Long responsavelId);
```

---

# 7. Ordem Recomendada de Implementação

1. Criar `EscolaRepository`.
2. Criar `EscolaDTO`.
3. Criar `EscolaService`.
4. Adicionar `UsuarioService.logarEscola()`.
5. Criar `EscolaController`.
6. Implementar `ListarEscolas`.
7. Implementar `VincularEscola`.
8. Implementar `ListarSolicitacoes`.
9. Executar `./mvnw clean install`.
10. Validar endpoints manualmente.

---

# 8. Casos de Uso Atendidos

## UC-01 — Cadastrar Escola

* Ator: Escola
* Endpoint: `POST /Escola/Cadastrar`

## UC-02 — Autenticar Escola

* Ator: Escola
* Endpoint: `GET /Escola/Autenticar`

## UC-03 — Listar Escolas

* Ator: Responsável
* Endpoint: `GET /Responsavel/ListarEscolas`

## UC-04 — Vincular Escola a Dependentes

* Ator: Responsável
* Endpoint: `POST /Responsavel/VincularEscola/{idResponsavel}/{idEscola}`

## UC-05 — Visualizar Solicitações

* Ator: Responsável
* Endpoint: `GET /Responsavel/ListarSolicitacoes/{idResponsavel}`

---

# 9. Validações Obrigatórias

## Cadastro Escola

* DTO não pode ser nulo.
* UsuarioDTO não pode ser nulo.
* tipoPerfil deve ser `ESCOLA`.

## Login Escola

* Email e senha obrigatórios.
* Perfil deve ser `ESCOLA`.

## Vincular Escola

* Responsável deve existir.
* Escola deve existir.
* Dependentes devem pertencer ao responsável.

## Listar Solicitações

* Responsável deve existir.

---

# 10. Critérios de Aceitação

A implementação será considerada correta se:

* Escola puder ser cadastrada.
* Escola puder autenticar.
* Responsável puder listar escolas.
* Responsável puder vincular escola a dependentes.
* Dependentes passarem a retornar a escola vinculada.
* Responsável puder visualizar solicitações.
* Código compilar sem erros.
* Implementação seguir integralmente `GEMINI.md`.



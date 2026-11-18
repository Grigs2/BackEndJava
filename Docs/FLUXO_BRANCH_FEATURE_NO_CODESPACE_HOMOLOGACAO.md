# Fluxo de branch de feature no codespace da homologação

Este tutorial mostra como desenvolver uma nova funcionalidade no backend usando o **mesmo codespace** que está na homologação, sem criar outro codespace. A regra deste fluxo é: **a feature e a homologação não rodam ao mesmo tempo**. Enquanto você desenvolve, o backend roda o código da feature. Depois do merge, você volta para a `homologacao` e reinicia o backend.

Nada é enviado direto para a `homologacao`. A entrada de código acontece apenas por Pull Request.

---

## Visão geral

```
homologacao (remota) ◄── Pull Request ── feat/minha-feature (remota)
        │                                      ▲
        └── git switch -c feat/minha-feature ──┘  (desenvolvimento no codespace)
```

1. Atualizar a `homologacao` local.
2. Criar a branch de feature a partir dela.
3. Desenvolver, testar e commitar na feature.
4. Subir a feature e abrir um PR para `homologacao`.
5. Fazer o merge no GitHub.
6. Voltar para a `homologacao`, atualizar e reiniciar o backend.

---

## Pré-requisitos

- Estar no codespace do backend (`Grigs2/BackEndJava`).
- O secret `JWT_SECRET_BASE64` já configurado (ver [AMBIENTE_CODESPACE.md](/workspaces/BackEndJava/Docs/AMBIENTE_CODESPACE.md), seção 3).

---

## Passo 1: verificar o estado atual

Antes de trocar de branch, veja o que está pendente:

```bash
cd /workspaces/BackEndJava
git status
```

Se houver alterações não commitadas, escolha uma das opções abaixo.

- **Elas pertencem à feature:** você vai levá-las junto ao criar a branch (o Git mantém as alterações ao trocar, desde que não conflitem). Use essa opção se for trabalho seu da feature.
- **Elas não pertencem à feature** (por exemplo, `.gitignore`, `mvnw` ou arquivos de apoio): guarde-as antes de trocar:

```bash
git stash push -u -m "pendencias antes da feature"
```

Para recuperá-las depois: `git stash pop`.

Não faça commit de arquivos que não sejam da feature na branch de feature.

---

## Passo 2: atualizar a homologação e criar a feature

```bash
git switch homologacao
git pull origin homologacao
git switch -c feat/nome-da-funcionalidade
```

Padrão de nome: `feat/...` para funcionalidades, `fix/...` para correções.

Neste ponto o backend em execução (se ainda estiver ativo) continua com o código antigo na memória. Para usar o código da feature, reinicie-o (passo 3).

---

## Passo 3: rodar o backend com a feature

Pare o backend atual (`Ctrl+C`) e rode de novo:

```bash
./mvnw spring-boot:run
```

Se a feature alterou `pom.xml`, baixe as dependências antes:

```bash
./mvnw -q dependency:go-offline
```

A porta 8080 continua sendo a mesma. Como só há um backend rodando, não há conflito de porta.

---

## Passo 4: desenvolver e commitar

Trabalhe normalmente na branch de feature. Antes de commitar, confirme a branch atual:

```bash
git branch --show-current
git status
git add <arquivos-da-feature>
git commit -m "feat: descrição curta da mudança"
```

Prefira adicionar arquivos específicos com `git add <arquivos>` em vez de `git add .`, para não incluir pendências que não são da feature.

Para publicar a branch pela primeira vez:

```bash
git push -u origin feat/nome-da-funcionalidade
```

Nas próximas vezes, basta `git push`.

---

## Passo 5: abrir o Pull Request

1. No GitHub, acesse o repositório `Grigs2/BackEndJava`.
2. Clique em **Compare & pull request** (ou em **Pull requests → New pull request**).
3. Confira os campos:
   - **base:** `homologacao`
   - **compare:** `feat/nome-da-funcionalidade`
4. Descreva a mudança e crie o PR.
5. Após a revisão, faça o **merge**.

Não faça merge local com `git merge` seguido de `git push` na `homologacao`. Isso ignora a revisão do PR.

---

## Passo 6: voltar para a homologação

Depois do merge no GitHub:

```bash
git switch homologacao
git pull origin homologacao
```

Reinicie o backend para rodar o código da homologação:

```bash
# Ctrl+C no terminal do backend
./mvnw spring-boot:run
```

Se houver alterações de dependência no `pom.xml` trazidas pelo merge, rode `./mvnw -q dependency:go-offline` antes.

Se você guardou pendências no passo 1, recupere-as com `git stash pop` apenas se fizer sentido para o seu trabalho.

Opcionalmente, apague a branch local já mergeada:

```bash
git branch -d feat/nome-da-funcionalidade
```

---

## Resumo rápido

| Etapa | Comando principal |
|---|---|
| Atualizar homologação | `git switch homologacao && git pull origin homologacao` |
| Criar feature | `git switch -c feat/nome` |
| Rodar backend | `./mvnw spring-boot:run` |
| Commitar e publicar | `git add <arquivos> && git commit -m "..." && git push -u origin feat/nome` |
| Entrar no código | Pull Request com base `homologacao` |
| Voltar para homologação | `git switch homologacao && git pull origin homologacao` + reiniciar backend |

---

## Cuidados

- **Banco de dados:** o banco (volume `pgdata`) é o mesmo para as duas branches. Uma feature com entidades novas altera o schema via `ddl-auto=update`, e essa mudança continua no banco ao voltar para a homologação.
- **Processos:** trocar de branch não reinicia o backend. Sempre reinicie após `git switch`.
- **Frontend:** se o frontend também estiver no codespace, reinicie o `expo start` após trocar de branch do backend, caso dependa de alterações de API.

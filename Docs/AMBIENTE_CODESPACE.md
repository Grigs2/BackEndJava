# Ambiente GitHub Codespaces — TioDaPerua

Este guia explica como subir, no GitHub Codespaces, o ambiente de **backend + PostgreSQL** e o ambiente de **frontend**, e como fazê-los conversarem entre si, do mesmo jeito que você testa localmente. Não é preciso instalar Java, Node, Postgres ou Docker na sua máquina.

Este arquivo existe em duas pastas (o mesmo conteúdo):
- `BackEndJava/Docs/AMBIENTE_CODESPACE.md`
- `frontendreact/docs/AMBIENTE_CODESPACE.md`

---

## 1. Visão geral

São **dois codespaces separados**, um por repositório:

| Codespace | Repositório | O que roda | Porta |
|---|---|---|---|
| Backend | `Grigs2/BackEndJava` | Spring Boot (Java 21) + PostgreSQL 16 (via Docker Compose) | `8080` |
| Frontend | `Grigs2/frontendreact` | Expo Web (Node 22) | `8081` |

Como o navegador roda na sua máquina e os serviços rodam em máquinas remotas, a comunicação acontece pelas URLs públicas que o Codespaces gera, no formato:

```
https://<nome-do-codespace>-<porta>.app.github.dev
```

Fluxo:
1. Você cria o codespace do **backend** e pega a URL dele na porta 8080.
2. Você cria o codespace do **frontend** e configura essa URL do backend nele (`EXPO_PUBLIC_API_URL`).
3. Você abre a URL do frontend (porta 8081) no navegador do seu PC.

Cada repositório pode estar em uma branch diferente (`main`, `feat/...`, `dev`, etc.). Você escolhe a branch na hora de criar cada codespace.

---

## 2. Pré-requisitos (uma vez só)

1. Ter acesso aos repositórios `Grigs2/BackEndJava` e `Grigs2/frontendreact` no GitHub.
2. Ter o plano do Codespaces disponível na sua conta (o plano gratuito tem limite mensal de horas; ver em https://github.com/settings/billing/summary).
3. Abrir https://github.com/codespaces para ver e gerenciar seus ambientes.

---

## 3. Configurar os secrets (uma vez só)

Os valores sensíveis **não ficam no repositório**. Eles ficam como *secrets* do Codespaces, vinculados à sua conta.

1. Acesse https://github.com/settings/codespaces.
2. Em **Codespaces secrets**, clique em **New secret** e crie:

| Nome do secret | Obrigatório | Para que serve |
|---|---|---|
| `JWT_SECRET_BASE64` | Sim | Chave de assinatura dos tokens JWT (mesmo valor que você usa localmente) |
| `ADMIN_EMAIL` | Sim | E-mail do administrador criado no bootstrap |
| `ADMIN_PASSWORD` | Sim | Senha do administrador criado no bootstrap |
| `POSTGRES_PASSWORD` | Não | Senha do Postgres do codespace. Se não existir, usa `postgres` (é um banco só de teste, dentro do codespace) |
| `SENDGRID_API_KEY` | Não | Só se for testar envio de e-mail |
| `SENDGRID_FROM_EMAIL` | Não | Só se for testar envio de e-mail |

3. Em **Repository access**, selecione **Select repositories** e marque **`BackEndJava`**. Sem isso o secret não aparece no codespace do backend.

> Se você criar um secret depois que o codespace já existe, reinicie o codespace (ver seção 9).

---

## 4. Criar o codespace do backend

1. Acesse o repositório `Grigs2/BackEndJava` no GitHub.
2. Clique em **Code → Codespaces → ⋯ → New with options...** (ou **Create codespace on <branch>**).
3. Em **Branch**, escolha a branch que quer testar (ex.: `main`, `feat/...`).
4. Em **Machine type**, deixe `2-core` (ou maior, se o Maven ficar lento).
5. Clique em **Create codespace**.

Na primeira vez, o Codespaces:
- constrói o container com Java 21 (`.devcontainer/devcontainer.json`);
- sobe o PostgreSQL 16 (`.devcontainer/docker-compose.yml`), com o banco `TioDaPerua` e um volume persistente;
- baixa as dependências do Maven (`postCreateCommand`).

Aguarde a mensagem de que o ambiente está pronto. Isso pode levar alguns minutos.

### 4.1 Subir a API

No terminal do codespace (`Terminal → New Terminal`), na pasta `/workspaces/BackEndJava`:

```bash
./mvnw spring-boot:run
```

Espere aparecer `Started BackEndJavaApplication` (ou similar) nos logs. O Hibernate cria/atualiza as tabelas sozinho (`ddl-auto=update`).

Deixe esse terminal aberto. Para parar, use `Ctrl+C`.

### 4.2 Deixar a porta 8080 pública e copiar a URL

1. Abra a aba **PORTS** (no painel inferior).
2. Localize a linha **8080 — Backend API**.
3. Clique com o botão direito em **Port Visibility → Public**.
   - Isso é importante: o navegador do frontend faz uma requisição de pré-verificação (CORS `OPTIONS`) que não envia cookies de login do GitHub. Com a porta em **Private**, essa requisição pode ser bloqueada.
4. Copie o endereço da coluna **Forwarded Address**. Ele tem este formato:

```
https://<nome-do-codespace>-8080.app.github.dev
```

5. A base da API é esse endereço com o caminho do contexto da aplicação:

```
https://<nome-do-codespace>-8080.app.github.dev/TioDaPerua/api
```

Guarde essa URL: ela será usada no codespace do frontend.

### 4.3 Testar o backend

No navegador, abra:

```
https://<nome-do-codespace>-8080.app.github.dev/TioDaPerua/api/Autenticacao/Entrar
```

Uma resposta de método não permitido (GET) ou de erro de login (sem corpo) já mostra que a API está no ar. Para testar de verdade, use o login pelo frontend.

> A documentação Swagger só aparece se `DOCS_ENABLED` estiver `true` (no `docker-compose.yml` ela já vem `true` no codespace).

---

## 5. Criar o codespace do frontend

1. Acesse o repositório `Grigs2/frontendreact` no GitHub.
2. Clique em **Code → Codespaces → ⋯ → New with options...**.
3. Escolha a **branch** (ex.: `feat/menu_inferior` ou a que você quiser testar).
4. Clique em **Create codespace**.

O `postCreateCommand` roda `npm install` automaticamente.

### 5.1 Apontar o frontend para o backend do codespace

O app lê a URL da API da variável `EXPO_PUBLIC_API_URL`. No codespace do frontend, crie o arquivo **`.env.local`** na raiz do projeto (ele já está no `.gitignore`, então não vai para o repositório):

No terminal:

```bash
echo "EXPO_PUBLIC_API_URL=https://<nome-do-codespace-do-backend>-8080.app.github.dev/TioDaPerua/api" > .env.local
```

Substitua `<nome-do-codespace-do-backend>` pelo nome do codespace do backend (o trecho que aparece antes de `-8080` na URL da seção 4.2).

Confirme o conteúdo:

```bash
cat .env.local
```

### 5.2 Subir o frontend (Expo Web)

```bash
npx expo start --web --port 8081
```

Quando o terminal mostrar o servidor pronto, vá na aba **PORTS**:
1. Localize **8081 — Frontend (Expo Web)**.
2. Clique com o botão direito em **Port Visibility → Public**.
3. Abra o **Forwarded Address** no navegador do seu PC.

> A URL do frontend fica como `https://<nome-do-codespace-do-frontend>-8081.app.github.dev`. Ela precisa estar na lista de origens do CORS do backend. O backend já aceita `https://*.app.github.dev`, então funciona para qualquer codespace.

> Esse fluxo testa no navegador. Abrir pelo app **Expo Go** no celular não é o foco aqui, porque o celular não consegue acessar a rede do codespace sem o modo tunnel do Expo.

---

## 6. Trocar de branch

- **Testar outra branch do mesmo repositório:** o jeito mais simples é criar um codespace novo com a branch desejada (seções 4 e 5). Você pode manter vários codespaces ao mesmo tempo.
- **Trocar a branch dentro do mesmo codespace:**

```bash
git fetch origin
git checkout <nome-da-branch>
git pull
```

Depois, reinicie o serviço (backend: `Ctrl+C` e rode `./mvnw spring-boot:run` de novo; frontend: `Ctrl+C` e rode `npx expo start --web --port 8081` de novo). Se mudou dependência (`pom.xml` ou `package.json`), rode `./mvnw -q dependency:go-offline` ou `npm install` antes.

- **Backend e frontend em branches diferentes:** tudo bem. Cada codespace tem sua própria branch. Só lembre que a URL da API no frontend aponta para o codespace do backend, não para uma branch.

---

## 7. Parar e retomar

- Codespaces para sozinho após um tempo sem uso (padrão de 30 minutos). Você não perde o código.
- Para retomar: acesse https://github.com/codespaces, clique no codespace e aguarde ligar.
- Ao retomar, os processos (`spring-boot:run` e `expo start`) **não sobem sozinhos**. Rode-os de novo.
- O banco Postgres fica no volume `pgdata`, então os dados sobrevivem a paradas. Para zerar o banco, remova o volume (seção 9).
- **A URL do backend pode mudar** se você recriar o codespace do backend. Se mudar, atualize `EXPO_PUBLIC_API_URL` no `.env.local` do frontend e reinicie o `expo start`.

---

## 8. Dia a dia (resumo)

1. Abra https://github.com/codespaces e ligue o codespace do **backend**.
2. Terminal: `./mvnw spring-boot:run`.
3. PORTS: porta 8080 **Public**. Copie o endereço.
4. Ligue o codespace do **frontend**. Confira/atualize `.env.local`.
5. Terminal: `npx expo start --web --port 8081`.
6. PORTS: porta 8081 **Public**. Abra o endereço no navegador do PC.

---

## 9. Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| Erro `defina o secret JWT_SECRET_BASE64 no Codespaces` ao criar/subir o backend | Secret não criado ou sem acesso ao repositório `BackEndJava` | Confira a seção 3 e reinicie o codespace (`Ctrl+Shift+P` → **Codespaces: Rebuild Container**) |
| Frontend abre, mas login/chamadas falham no navegador com erro de CORS | Porta 8080 ou 8081 está **Private**, ou a URL em `.env.local` está errada | Deixe as duas portas **Public**. Confira o `.env.local` e reinicie o `expo start` |
| Frontend não usa a URL nova | O Expo lê as variáveis só na inicialização | Pare o `expo start` (`Ctrl+C`) e rode de novo |
| `Port 8080 already in use` no backend | O `spring-boot:run` anterior ainda está rodando | Pare com `Ctrl+C` no terminal antigo, ou use `pkill -f spring-boot` |
| Backend não conecta no banco | Container `db` parado | Veja `docker compose -f .devcontainer/docker-compose.yml ps` e reinicie com **Codespaces: Rebuild Container** |
| Quer zerar o banco | Dados antigos no volume | No terminal: `docker compose -f .devcontainer/docker-compose.yml down -v`, depois **Rebuild Container** |
| Mudou o `devcontainer.json` ou o `docker-compose.yml` e nada mudou | Configuração do container não foi reaplicada | `Ctrl+Shift+P` → **Codespaces: Rebuild Container** |
| `npm install` falha no frontend | Versão do Node | O devcontainer já usa Node 22 (exigido pelo React Native 0.88). Confira com `node -v` |
| Não sei o nome do codespace | — | Ele aparece na URL do codespace em https://github.com/codespaces, ou na coluna **Forwarded Address** da aba PORTS |

Comandos úteis no terminal do codespace:

```bash
# Status dos containers (backend)
docker compose -f .devcontainer/docker-compose.yml ps

# Logs do Postgres
docker compose -f .devcontainer/docker-compose.yml logs db

# Versão do Node e do Java
node -v
java -version
```

---

## 10. Segurança

- Não commite secrets, senhas ou o `.env.local` no repositório.
- Os valores sensíveis entram no codespace só pelos *Codespaces secrets* (seção 3).
- A porta **Public** expõe a API na internet pública, mas o acesso continua exigindo o login/JWT da aplicação. Use só para testes e desligue o codespace quando não estiver usando.

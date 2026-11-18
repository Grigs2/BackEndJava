# Conectar na IDE ao PostgreSQL do Tio da Perua

Este tutorial mostra como consultar o banco usado pelo backend pela extensão **PostgreSQL** da Microsoft no VS Code (Codespace).

---

## 1. Entender o endereço do banco

O backend não usa `localhost`. A URL vem da variável `SPRING_DATASOURCE_URL`:

```
jdbc:postgresql://db:5432/TioDaPerua
```

- `db` é o serviço do PostgreSQL na rede do Docker.
- A porta é `5432`.
- O banco é `TioDaPerua`.

Confira no seu Codespace:

```bash
echo $SPRING_DATASOURCE_URL
echo $SPRING_DATASOURCE_USERNAME
```

Não use `localhost` na conexão: ele não aponta para o banco.

---

## 2. Descobrir a senha

A senha que o backend usa vem da variável `SPRING_DATASOURCE_PASSWORD`. Ela tem prioridade sobre o `spring.datasource.password` do `application.properties`, por isso a senha `sa` do arquivo pode não funcionar.

```bash
echo $SPRING_DATASOURCE_PASSWORD
```

Não grave essa senha em arquivos versionados.

---

## 3. Instalar a extensão

1. Abra a aba de extensões (`Ctrl+Shift+X`).
2. Procure **PostgreSQL** do publisher **Microsoft** (`ms-ossdata.vscode-pgsql`).
3. Clique em **Install**.

---

## 4. Criar a conexão

1. Abra o painel **PostgreSQL** na barra lateral.
2. Clique em **Add Connection** (ou no ícone de adicionar conexão).
3. Preencha:

| Campo | Valor |
|---|---|
| Server name | `db` |
| Authentication type | `Password` |
| User name | `postgres` |
| Password | valor de `echo $SPRING_DATASOURCE_PASSWORD` |
| Database name | `TioDaPerua` |
| Connection name | `banco_tio_perua` (ou outro nome) |
| Server group | `Servers` |

4. Se houver a seção **Advanced** (ou "Opções avançadas"), abra-a e defina **SSL Mode** como **Disable** (ou **prefer**).
5. Marque **Save Password** se quiser que a senha fique salva no VS Code.
6. Clique em **Connect**.

---

## 5. Executar consultas

1. Clique com o botão direito na conexão e escolha **New Query** (ou abra um arquivo `.sql` e conecte).
2. Escreva e execute, por exemplo:

```sql
SELECT id, email, "tipoPerfil"
FROM usuario
ORDER BY id;
```

A coluna `tipoPerfil` tem letra maiúscula, então precisa de aspas duplas. Confirme os demais nomes no explorador de objetos da própria extensão, porque o Hibernate cria as tabelas com base nas entidades.

Para ver as contas de teste criadas pelo seed, use `scripts/seed-perfis-teste/credenciais.txt`. Esse arquivo não é versionado.

---

## Problemas comuns

| Mensagem | Causa | Solução |
|---|---|---|
| `connection to server at "172.18.0.2" ... failed` ou timeout | Host errado (`localhost`) | Use `db` como Server name |
| `server does not support SSL, but SSL was required` | Cliente exige SSL e o servidor do Docker não tem | Defina **SSL Mode** como **Disable** |
| `password authentication failed for user "postgres"` | Senha diferente da usada pelo backend | Use o valor de `echo $SPRING_DATASOURCE_PASSWORD`, não o `sa` do `application.properties` |
| Conexão não aparece no painel | Perfil não foi salvo | Conclua o formulário e clique em **Connect** |

Se o backend não estiver rodando, a conexão com o banco ainda funciona, porque ela depende só do container `db`. Mas as tabelas só existem depois que a aplicação subir pela primeira vez com `ddl-auto=update`.

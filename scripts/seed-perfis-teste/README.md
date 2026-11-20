# Perfis de teste - Tio da Perua

## Atalho: tudo de uma vez no Codespace

Na raiz do repositorio do backend, com os secrets `JWT_SECRET_BASE64`, `ADMIN_EMAIL` e `ADMIN_PASSWORD` configurados no Codespaces:

```bash
bash scripts/preparar-ambiente-teste.sh
```

O script sobe a API se ela ainda nao estiver no ar (log em `/tmp/tiodaperua-backend.log`), instala o Python 3 se faltar, cria os perfis (`seed_perfis.py`) e monta o cenario de uso (`seed_cenario.py`, secao "Cenario de uso" abaixo). Pode rodar de novo quando quiser: nada e duplicado.

| Opcao | Efeito |
|---|---|
| `--simular-gps` | No fim, fica enviando a posicao da perua de SP a cada 15 s, movendo-a no mapa (Ctrl+C para parar). Sem isso, a posicao fica "desatualizada" no app depois de 45 s. |
| `--so-perfis` | So cria as contas, sem viagens nem cenario. |

Se a API ja estiver rodando em outro terminal, o script so roda as seeds. `GPS_CONSENT_VERSION` e `TERMS_VERSION` recebem `teste-codespace` quando nao estao definidas; se a API ja estava no ar sem `GPS_CONSENT_VERSION`, o cenario avisa e segue sem GPS.

## Cenario de uso (`seed_cenario.py`)

Roda sobre as contas abaixo e deixa o app pronto para testar:

| Conta | O que encontrar |
|---|---|
| `motorista.sp@...` | Documentos aprovados, viagens Manha ida/volta. **Viagem de hoje (manha-ida) em andamento**, com posicao de GPS. |
| `responsavel.sp1@...` (Lucas e Sofia) | Card destacado: a casa dos dois e a proxima parada depois da Ana. Mapa ao vivo no "Acompanhar". Parada "Embarque de Lucas, Sofia" no app do motorista. |
| `responsavel.sp2@...` (Ana Clara) | Ana e a proxima parada da viagem: "PROXIMO EVENTO DA VIAGEM" e horario estimado. |
| `motorista.rj@...` | Viagens Manha e Tarde (ida/volta). **Manha-ida em andamento com os 3 alunos embarcados e sem GPS**; tarde-ida so preparada. |
| `responsavel.rj1@...` (Gabriel, Laura, Mateus) | Luz "No veiculo", proxima interacao "chegada na escola" e **mapa alternativo em linha de metro** (sem localizacao). |
| `responsavel.rj2@...` (Sofia e Davi) | Viagem da tarde "Aguardando inicio". |
| `motorista.bh@...` / `responsavel.bh1@...` (Miguel) | Convite de transporte **pendente** para aceitar ou recusar. |
| Todos os responsaveis | Notificacoes nao lidas (viagem iniciada, embarques, convites) para o popout do sino. |
| ADMIN (`ADMIN_EMAIL`) | Documentos ja revisados; auditoria com as aprovacoes. |

As viagens de hoje usam a data do servidor. Em outro dia, rode o script de novo: ele prepara e inicia a viagem da nova data. Se a viagem de hoje ja tiver sido finalizada, ela e mantida como esta.

Este diretorio recria, pela API, um conjunto de perfis de teste coerentes: cada escola, motorista, responsavel e dependente fica na mesma cidade.

## O que e criado

| Cidade | Escola | Motorista (veiculo) | Responsaveis (dependentes) |
|---|---|---|---|
| Sao Paulo/SP | Escola Municipal Monteiro Lobato (Pinheiros) | Carlos Eduardo Lima (Fiat Ducato, RTA2B34) | Fernanda Souza Ribeiro (2), Rodrigo Almeida Nunes (1) |
| Rio de Janeiro/RJ | Escola Municipal Cecilia Meireles (Copacabana) | Paulo Roberto Gomes (Mercedes-Benz Sprinter, BRA2E19) | Juliana Martins Pereira (3), Marcelo Teixeira Batista (2) |
| Belo Horizonte/MG | Escola Municipal Professora Helena Antipoff (Funcionarios) | Anderson Rocha Mendes (Renault Master, FGH3I45) | Patricia Lopes Vieira (1) |

Total: 3 escolas, 3 motoristas, 5 responsaveis e 9 dependentes. Cada motorista ja fica com o vinculo escola-motorista aceito.

Credenciais (senha igual para todas, definida em `dados.json`). O seed grava a lista completa em `scripts/seed-perfis-teste/credenciais.txt`, que esta no `.gitignore`:

- Senha: `Teste@TioDaPerua2026`
- Escolas: `escola-sp@teste.tiodaperua.com.br`, `escola-rj@...`, `escola-bh@...`
- Motoristas: `motorista.sp@teste.tiodaperua.com.br`, `motorista.rj@...`, `motorista.bh@...`
- Responsaveis: `responsavel.sp1@...`, `responsavel.sp2@...`, `responsavel.rj1@...`, `responsavel.rj2@...`, `responsavel.bh1@...` (dominio `teste.tiodaperua.com.br`)

## Arquivos

- `dados.json`: fonte de verdade de todos os perfis (nomes, CPF, CNPJ, CNH, placas, enderecos, periodos). Edite aqui para mudar os dados.
- `seed_perfis.py`: cria os perfis pela API usando apenas a biblioteca padrao do Python 3, e grava `credenciais.txt`.
- `credenciais.txt`: gerado pelo seed (nao versionado).
- `README.md`: este tutorial.

## Passo a passo em um Codespace novo

1. Permissao do Maven wrapper (se aparecer `Permission denied`):

   ```bash
   chmod +x mvnw
   git update-index --chmod=+x mvnw   # para o git guardar a permissao
   ```

2. Defina as variaveis obrigatorias antes de subir o backend. Gere a chave JWT com:

   ```bash
   export JWT_SECRET_BASE64=$(openssl rand -base64 32)
   export ADMIN_EMAIL=admin@teste.tiodaperua.com.br
   export ADMIN_PASSWORD='Admin@TioDaPerua2026'
   ```

   Sem `JWT_SECRET_BASE64` a aplicacao nao inicia (comportamento intencional). A senha do admin precisa ter 12 caracteres ou mais.

3. Garanta que o PostgreSQL esta rodando em `localhost:5432`, banco `TioDaPerua`, usuario `postgres`, conforme `src/main/resources/application.properties`.

4. Suba a aplicacao (em um terminal):

   ```bash
   ./mvnw spring-boot:run -Dmaven.test.skip=true
   ```

   Ela responde em `http://localhost:8080/TioDaPerua/api`.

5. Em outro terminal, rode o seed:

   ```bash
   python3 scripts/seed-perfis-teste/seed_perfis.py
   ```

   Para outra URL: `python3 scripts/seed-perfis-teste/seed_perfis.py --url http://host:porta/TioDaPerua/api` (ou variavel `TIO_API_URL`).

O script e reexecutavel: contas que ja existem (login valido) sao reaproveitadas e nao sao recriadas. Se ele falhar no meio, corrija a causa e rode de novo.

## Regras que o seed respeita

- CPF e CNPJ tem digito verificador valido e sao unicos.
- Placas no formato `ABC1D23` ou `ABC1234`.
- Os CEPs dos enderecos operacionais (escolas e motoristas) precisam existir no ViaCEP. Se o CEP nao existir, a escola ou o motorista nao e cadastrado. Os enderecos de dependentes aceitam indisponibilidade do ViaCEP (ficam `PENDENTE`).
- Motorista tem idade minima de 18 anos.
- Dependentes nao tem CPF (sao menores de idade).

Os CEPs usados foram conferidos no ViaCEP. Se um CEP deixar de existir no futuro, troque em `dados.json` por outro da mesma rua e bairro.

## Problemas comuns

- `API indisponivel`: o backend nao esta rodando na porta 8080 ou o contexto nao e `/TioDaPerua/api`.
- `CEP nao encontrado no ViaCEP`: troque o CEP em `dados.json` (veja a regra acima).
- `Login ... retornou 500`: geralmente o PostgreSQL nao esta acessivel. Confira os logs do `spring-boot:run`.
- `ModuleNotFoundError` ao rodar o script: o Python do sistema esta incompleto. Use outro Python 3 ou reinstale o pacote da biblioteca padrao do Python.

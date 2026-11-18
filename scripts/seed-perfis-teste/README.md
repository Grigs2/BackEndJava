# Perfis de teste - Tio da Perua

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
   ./mvnw spring-boot:run
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

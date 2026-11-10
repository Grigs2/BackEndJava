# Implementação do Fluxo de Acordo Motorista–Responsável–Escola (Back-end)

Branch: `feat/fluxo_acordo_motorista_responsavel`
Documento de referência do plano: [IMPLEMENTACAO_FLUXO_MOTORISTA_RESPONSAVEL_ESCOLA.md](./IMPLEMENTACAO_FLUXO_MOTORISTA_RESPONSAVEL_ESCOLA.md)

## 1. Resumo

Este documento descreve o que foi implementado no back-end para o fluxo entre Motorista, Responsável e Escola, incluindo as correções da última rodada de homologação.

## 2. Funcionalidades implementadas

### 2.1 Solicitações
- Motorista solicita atendimento a escolas cadastradas.
- Escola aprova ou recusa a solicitação do motorista, com base nos documentos.
- Motorista envia solicitação a um responsável (busca de dependentes anonimizada).
- Responsável envia solicitação a um motorista (busca com informações limitadas do motorista).
- Solicitações expiram após **3 dias**.
- Antes de enviar, o responsável confirma que as informações dos dependentes serão compartilhadas se o motorista aceitar.
- Ao aceitar, é fechado o acordo de transporte para os dependentes envolvidos.

### 2.2 Viagens com várias escolas
- Uma viagem (`Viagem`) pode atender **mais de uma escola**.
- Relação: `Viagem` ↔ `Escola` (ManyToMany), tabela `viagem_escola_destino`.
- `ViagemService.criarViagem(...)` recebe `List<Long> escolaIds`.
- Filtros de busca e notificações consideram a lista de escolas da viagem.
- Migração aplicada: os registros existentes de `viagem.escola_id` foram copiados para `viagem_escola_destino` (4 registros). A coluna `viagem.escola_id` permanece, nula.

### 2.3 Nome editável da viagem
- Campo `nomeCustomizado` em `Viagem`. Quando nulo, o nome padrão é calculado por `Viagem.nomePadrao()`:
  - Ida: `Viagem para escola(s): e1, e2, ...`
  - Volta: `Viagem de volta das escola(s): e1, e2, ...`
- A direção (ida/volta) vem do sufixo do período (`MANHA_IDA`, `MANHA_VOLTA`, etc.).
- Endpoint: `PUT /Viagem/AtualizarNome/{id}` com body `{ "nome": "..." }`. Enviar `nome: null` restaura o padrão.
- DTOs: `ViagemDTO`, `ViagemDetalhadaDTO`, `NomeViagemDTO`.

### 2.4 Aba "Próximos" do responsável
- Correção em `BuscaMotoristaService`: motoristas que atendem a escola do dependente passam a aparecer na busca com `somenteProximos=true`.
- Premissas adotadas:
  - Quando o endereço não tem cidade, usa-se a cidade da escola do dependente.
  - "Jardim Vila Mariana" e "Vila Mariana" são tratados como a mesma cidade.

### 2.5 Notificações
- Texto de notificação de viagem passou a usar `Viagem para escola(s): ...`.
- Mensagens de desembarque em `MensagensNotificacao`.

### 2.6 Documentos
- A validação de documentos do motorista é feita **somente pelo admin**, via API (`GET /Admin/Documentos/Pendentes`, `PUT /Admin/Documentos/{id}/Revisar`).

## 3. Testes

- Novos testes: `ViagemNomeTest` (4 casos).
- Ajustados: `MensagensNotificacaoTest`, `SolicitacaoBidirecionalTest`, `BuscaMotoristaProximidadeTest`.
- Suíte completa executada com sucesso: `.\mvnw.cmd -q test`.
- Validação manual via API:
  - Viagens 20 (MANHA_IDA) e 21 (MANHA_VOLTA) do motorista m1 com nome padrão.
  - Renomear para "Teste renomeada" e resetar para o padrão.
  - "Próximos" do responsável r1 retorna o motorista para os dependentes Lucas Lima e Sofia Lima.

## 4. Cenário de teste

- Dados de acesso em [dados_acesso_teste.txt](./dados_acesso_teste.txt).
- 4 escolas (e1 a e4), 3 motoristas (m1 a m3) e 4 responsáveis (r1 a r4), com 1 a 3 dependentes cada, todos na mesma região.

## 5. Pendências

- Validação de documentos pela **escola** (não implementada).
- Envio de documentos pelo **motorista** para a escola (não implementado).
- Atualizar a seção 2.1 do plano de implementação, que ainda descreve o estado anterior.

## 6. Configuração de execução

- Subir o back: `.\mvnw.cmd spring-boot:run` (porta 8090, contexto `/TioDaPerua/api`).
- Observação: o `AdministradorBootstrap` falha na inicialização se houver usuários com perfil ADMIN sem registro na tabela `administrador`. Usuários `security-test-*` criados pelos testes podem causar isso. Solução: alterar o `tipo_perfil` desses usuários para `TESTE_SEGURANCA`.
- Para acesso pela rede local (celular), o front deve apontar para o IP da máquina, e a porta 8090 precisa estar liberada no firewall (perfil de rede Privado ou regra de entrada).

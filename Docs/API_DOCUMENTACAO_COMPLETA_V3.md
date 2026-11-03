# Documentação Completa da API REST - Tio da Perua (V3)

## Introdução
Esta documentação consolida e padroniza todos os endpoints da API REST do projeto **Tio da Perua**. O sistema é responsável pela gestão de transporte escolar, integrando Motoristas, Responsáveis, Escolas e Dependentes.

Esta versão (V3) substitui integralmente as documentações anteriores e reflete o estado atual do código-fonte, incluindo correções de compatibilidade para o FrontEnd Web e novas funcionalidades de gestão de perfil e viagens.

---

## Informações Gerais

- **Base URL Local:** `http://localhost:8080/TioDaPerua/api/`
- **Formato de Dados:** JSON (UTF-8)
- **Convenções REST:** 
  - `GET`: Consulta de dados.
  - `POST`: Criação de recursos ou operações complexas (ex: autenticação).
  - `PUT`: Atualização de recursos ou alteração de status.
  - `DELETE`: Remoção de recursos (não utilizado nesta versão, priorizando Soft Delete).
- **Tratamento de Erros:** A API utiliza códigos HTTP e o formato JSON definido em `RESPONSE_ENTITY.md`.
- **Autenticação:** A API usa Bearer access token JWT. Cadastro, entrada e renovação são públicos; endpoints de negócio exigem token válido e autorização por perfil/propriedade do recurso.

## Autenticação e sessão

### Entrar

`POST /Autenticacao/Entrar`

```json
{
  "email": "motorista@example.com",
  "senha": "senha-informada"
}
```

Resposta de sucesso (`200 OK`):

```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt>",
  "accessTokenExpiresIn": 900,
  "refreshToken": "<token-opaco>",
  "refreshTokenExpiresIn": 2592000,
  "perfil": {
    "id": 8,
    "usuarioId": 51,
    "nome": "Motorista",
    "tipoPerfil": "MOTORISTA"
  }
}
```

O access token expira em 15 minutos e o refresh token em 30 dias por padrão. Os prazos podem ser configurados por `JWT_ACCESS_TOKEN_MINUTES` e `JWT_REFRESH_TOKEN_DAYS`. O segredo HMAC deve vir de `JWT_SECRET_BASE64`, em Base64 e com no mínimo 32 bytes aleatórios decodificados. Gere um segredo diferente por ambiente e não o armazene no repositório.

PowerShell para gerar um segredo:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

### Renovar

`POST /Autenticacao/Renovar`

```json
{ "refreshToken": "<refresh-token-atual>" }
```

Retorna um novo access token e um novo refresh token. O refresh token usado é invalidado a cada renovação; o cliente deve substituir o token antigo pelo novo e descartar o anterior.
Se um refresh token já invalidado for reapresentado, a sessão correspondente é revogada e será necessário autenticar novamente.

### Encerrar sessão

`POST /Autenticacao/Sair` requer `Authorization: Bearer <accessToken>` e recebe:

```json
{ "refreshToken": "<refresh-token-da-sessao>" }
```

Retorna `204 No Content` e revoga os refresh tokens da sessão. Como o access token é JWT sem consulta de estado por requisição, ele ainda pode ser usado até expirar (no máximo 15 minutos por padrão); o cliente deve descartá-lo imediatamente ao sair.

### Alterar senha

`POST /Autenticacao/AlterarSenha` requer `Authorization: Bearer <access-token>` e recebe:

```json
{
  "senhaAtual": "<senha-atual>",
  "novaSenha": "<nova-senha>"
}
```

O usuário é identificado exclusivamente pelo JWT; não envie um ID no corpo. A senha atual precisa ser confirmada. A nova senha deve ter ao menos 12 caracteres e no máximo 72 bytes UTF-8, sem exigência de composição específica. A regra também se aplica a novas contas e ao bootstrap ADMIN; senhas existentes não são invalidadas por comprimento. A troca responde `204 No Content` e revoga todos os refresh tokens do usuário. Access tokens já emitidos continuam válidos até a expiração (15 minutos por padrão); o cliente deve descartá-los e autenticar novamente.

### Recuperar senha

`POST /Autenticacao/SolicitarRedefinicaoSenha` é público e recebe:

```json
{ "email": "usuario@example.com" }
```

Responde `202 Accepted` com a mesma mensagem genérica para e-mail inexistente, pedido limitado ou pedido aceito:

```json
{
  "mensagem": "Se houver uma conta e o pedido puder ser processado, enviaremos instruções para redefinir a senha."
}
```

Para uma conta encontrada, o backend envia um link pelo SendGrid. O token aleatório só é persistido como hash, expira em 15 minutos e é de uso único. Há limite de 5 pedidos por e-mail e por IP em janela móvel de 1 hora, com intervalo de 60 segundos entre pedidos; os identificadores de rate limit são HMAC e as tentativas são removidas após 24 horas.

`POST /Autenticacao/RedefinirSenha` é público e recebe:

```json
{
  "token": "<token recebido por e-mail>",
  "novaSenha": "<nova senha conforme a política>"
}
```

Responde `204 No Content` ao concluir a redefinição. Token ausente, inválido, expirado ou já usado é rejeitado. A nova senha segue a política geral e todos os refresh tokens do usuário são revogados. Contas sem confirmação prévia do e-mail podem usar o endereço cadastrado; a redefinição concluída comprova posse apenas para esse fluxo. Confirmação de contato ainda não é oferecida.

Para habilitar o envio, configure `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL` (remetente verificado no SendGrid) e `PASSWORD_RESET_URL` (URL do frontend que recebe o parâmetro `token`). Não versione credenciais. O envio é assíncrono; falhas do provedor e saturação da fila são registradas pelo backend, mantendo a resposta pública genérica para impedir enumeração de contas.

### Uso do access token e autorização

Enviar `Authorization: Bearer <accessToken>` em todas as chamadas de negócio. Sem token válido, a API responde `401`; usuário autenticado sem perfil/vínculo permitido recebe `403`. Motoristas só operam suas viagens/execuções, responsáveis acessam dependentes e solicitações vinculados, e as notificações são limitadas ao remetente autenticado ou ao destinatário autenticado. A associação de um perfil não concede acesso a recursos de outro perfil.

Os endpoints `POST /Motorista/Autenticar`, `POST /Responsavel/Autenticar` e `POST /Escola/Autenticar` continuam disponíveis temporariamente para compatibilidade e migração de senha, mas não emitem tokens e não autorizam chamadas de negócio. Novos clientes devem usar `/Autenticacao/Entrar`.

Senhas novas são armazenadas com BCrypt. Para contas existentes em texto simples, o primeiro login correto migra a senha para hash; a resposta de login nunca inclui senha. Erros de validação de payload retornam `400 Bad Request` no formato padrão da API, sem ecoar valores de campos. Em produção, servir a API exclusivamente por HTTPS e manter o segredo JWT fora do código e dos logs.

---

Administradores usam o perfil `ADMIN` no mesmo endpoint de entrada. Não existe cadastro público de administrador: o primeiro perfil é criado no bootstrap descrito em `GEMINI.md`.

## Cadastro, documentos e perfis

Novos cadastros de motorista, responsável e escola devem enviar `aceitouTermos: true` dentro de `usuarioDTO` (ou `usuario`, no cadastro de responsável). O backend persiste o instante do aceite e `TERMS_VERSION`. O valor inicial configurado é `31-10-2026`; a versão é definida pela variável de ambiente `TERMS_VERSION`. Se ela estiver ausente, o cadastro continua permitido, o aceite permanece obrigatório e a versão persistida fica nula. Contas já existentes não são bloqueadas nesta fase.

O cadastro de motorista exige idade mínima de 18 anos completos, CPF válido e dados de contato consistentes. Dependentes não têm idade mínima; data de nascimento futura é rejeitada. O cadastro de escola exige CNPJ válido e único. CNPJ/CPF são validados por formato e dígitos, sem consulta de autenticidade a serviço oficial.

### Documentos do motorista

Os documentos são metadados, sem upload de arquivos. Cada envio cria uma nova submissão e preserva o histórico anterior:

`POST /Motorista/{id_motorista}/Documentos` — somente o próprio motorista.

Exemplo de envio de CNH:

```json
{
  "tipo": "CNH",
  "numero": "12345678901",
  "categoria": "D",
  "dataValidade": "2027-10-31"
}
```

Para vistoria, envie `tipo: "VISTORIA"`, `dataRealizacao` e `dataValidade`; para licenciamento, envie `tipo: "LICENCIAMENTO"`, o RENAVAM completo com 11 dígitos no campo `numero` e `dataValidade`. O número da CNH também deve ter 11 dígitos. Vistoria e licenciamento são vinculados ao veículo atual do motorista. A resposta informa `PENDENTE`, `APROVADO`, `RECUSADO` ou `VENCIDO` (derivado da validade de documento aprovado).

`GET /Motorista/{id_motorista}/Documentos` retorna o histórico do próprio motorista. O ADMIN consulta a fila pendente em `GET /Admin/Documentos/Pendentes` e decide em `PUT /Admin/Documentos/{idDocumento}/Revisar`, enviando `{"status":"APROVADO"}` ou `{"status":"RECUSADO","motivoRecusa":"..."}`. O motivo de recusa é opcional. Apenas a submissão mais recente daquele tipo pode ser revisada; registros anteriores permanecem no histórico. O campo legado `Motorista.cnh` é atualizado somente depois da aprovação da nova CNH.

O planejamento continua permitido sem a documentação aprovada. A mudança da execução para `EM_ANDAMENTO` só ocorre se a CNH, a vistoria e o licenciamento mais recentes estiverem aprovados, dentro da validade e associados ao veículo atual.

### Endereços e responsáveis associados

Os endpoints abaixo exigem autenticação como `RESPONSAVEL` e verificam o vínculo ativo do usuário com o dependente.

`GET /Responsavel/Dependente/{idDependente}/Enderecos` lista endereços ativos. `POST` no mesmo caminho adiciona um endereço; `PUT /Responsavel/Dependente/{idDependente}/Enderecos/{idEndereco}` altera; `DELETE` nesse caminho desativa sem apagar o registro.

Exemplo de criação:

```json
{
  "cep": "01001-000",
  "logradouro": "Praça da Sé",
  "numero": "10",
  "complemento": null,
  "bairro": "Sé",
  "cidade": "São Paulo",
  "uf": "SP",
  "finalidade": "AMBOS"
}
```

O CEP é normalizado para oito dígitos e consultado no ViaCEP somente para confirmar sua existência; os campos digitados não são preenchidos nem sobrescritos. CEP inexistente ou com formato inválido é rejeitado. Se o ViaCEP estiver temporariamente indisponível, o endereço é salvo com `statusValidacao: "PENDENTE"`; a validação é repetida quando o endereço for reenviado ou editado. A resposta pode informar `VALIDADO` ou `PENDENTE`. Há no máximo um endereço ativo por finalidade; `AMBOS` conflita com `EMBARQUE` e `DESEMBARQUE`.

O campo textual legado `Dependente.endereco` e os DTOs antigos permanecem compatíveis; seu conteúdo não é convertido automaticamente, pois não há como decompor endereços livres com segurança. Ao criar a execução diária, os endereços estruturados ativos são copiados para `enderecoEmbarqueSnapshot` e `enderecoDesembarqueSnapshot` em cada presença, campos também disponibilizados em `ViagemPresencaDTO`. Quando falta endereço estruturado para uma finalidade, o snapshot legado preserva o endereço do responsável criador (ou o texto legado do dependente, se a autoria não puder ser inferida). Alterar ou desativar um endereço não modifica execuções passadas. Execuções antigas, criadas antes da inclusão dos snapshots, não podem ser reconstruídas retroativamente.

`POST /Responsavel/Dependente/{idDependente}/Convites` cria um convite para uma conta de responsável já cadastrada, enviando `{"email":"responsavel@example.com","categoria":"PAI"}`. Somente o responsável criador do dependente pode convidar. As categorias aceitas são `PAI`, `MAE`, `RESPONSAVEL_LEGAL` e `OUTRO`; a categoria proposta é confirmada pelo convidado ao aceitar. O convite expira em 7 dias, não pode haver dois convites pendentes para a mesma conta/dependente, e convites recusados ou expirados podem ser reenviados. A API não envia e-mail/push de convite; o destinatário consulta `GET /Responsavel/Convites/Pendentes` e aceita ou recusa autenticado como a conta convidada em `PUT /Responsavel/Convites/{idConvite}/Aceitar` ou `/Recusar`.

`GET /Responsavel/Dependente/{idDependente}/Responsaveis` lista os vínculos ativos; para vínculos legados sem categoria, `categoria` será nula. `GET /Responsavel/Dependente/{idDependente}/Convites` consulta o histórico. `DELETE /Responsavel/Dependente/{idDependente}/Responsaveis/{idResponsavel}` revoga o vínculo; somente o convidador ou ADMIN pode fazê-lo. Para revogar o responsável criador, ADMIN deve primeiro atribuir outro criador ativo.

Vínculos legados são preservados. Se o criador não está registrado e há apenas um responsável ativo, o backend infere esse responsável. Casos legados ambíguos aparecem em `GET /Admin/Dependentes/ResponsavelCriadorPendentes`; ADMIN atribui um responsável já vinculado por `PUT /Admin/Dependentes/{idDependente}/ResponsavelCriador`, com corpo `{"responsavelId":123}`. Todos os responsáveis ativos podem consultar/alterar endereços e responder solicitações de transporte do dependente.

### Atualização do veículo

`PUT /Motorista/{id_motorista}/Veiculo` atualiza os dados do veículo existente do motorista autenticado. Não permite informar o ID de veículo de outro usuário. A placa é normalizada e validada e deve ser única. Uma alteração de dados invalida as revisões anteriores de vistoria/licenciamento desse veículo; envie os documentos novamente para poder iniciar uma execução.

### Endereço operacional estruturado de motorista e escola

Os cadastros de motorista e escola exigem `enderecoOperacionalDTO` com CEP, logradouro, número, complemento, bairro, cidade e UF. O objeto correspondente é armazenado em tabela própria, ligado 1:1 à conta `Usuario`. O campo textual legado `usuarioDTO.endereco` continua no contrato para compatibilidade, mas não libera o início de viagens.

O CEP é normalizado e consultado no ViaCEP. CEP inexistente ou malformado é rejeitado; se o provedor estiver indisponível, o endereço é salvo com `statusValidacao: "PENDENTE"`. Para iniciar uma execução, todos os endereços operacionais necessários devem estar com status `VALIDADO`. O cliente não pode definir o status: o servidor ignora esse valor na entrada.

Em `PUT /Motorista/AlterarInfos` e `PUT /Escola/AlterarInfos`, o objeto estruturado é opcional para permitir que contas antigas o incluam. Omiti-lo preserva o endereço estruturado existente; enviá-lo atualiza o cadastro e solicita nova validação.

### Campos adicionais nos cadastros

O cadastro de escola recebe o campo `cnpj` na raiz de `EscolaDTO`; `admResponsavel` continua representando o responsável pela escola. O campo textual `endereco` do usuário é mantido por compatibilidade, enquanto o endereço operacional estruturado é enviado no campo `enderecoOperacionalDTO`. O campo `aceitouTermos` deve ser incluído no `usuarioDTO`/`usuario` do cadastro. Nunca envie senha em respostas; o DTO de usuário omite esse valor.

### Gestão administrativa e auditoria

Os endpoints abaixo exigem perfil `ADMIN`. As pesquisas são paginadas com `pagina` (padrão `0`) e `tamanho` (padrão `50`, máximo `100`) e retornam a estrutura `Page` do Spring, com os dados em `content`.

| Operação | Endpoint e filtros |
| --- | --- |
| Pesquisar usuários | `GET /Admin/Usuarios?id=&tipoPerfil=&email=&suspensa=&pagina=&tamanho=` |
| Pesquisar escolas | `GET /Admin/Escolas?nome=&cnpj=&suspensa=&pagina=&tamanho=` |
| Pesquisar vínculos motorista-escola | `GET /Admin/Vinculos/MotoristaEscola?escolaId=&motoristaId=&status=&pagina=&tamanho=` |
| Pesquisar vínculos responsável-dependente | `GET /Admin/Vinculos/ResponsavelDependente?dependenteId=&responsavelId=&pagina=&tamanho=` |
| Pesquisar auditoria | `GET /Admin/Auditoria?atorId=&acao=&tipoAlvo=&alvoId=&inicio=&fim=&pagina=&tamanho=` |

A pesquisa de usuário usa e-mail exato, sem diferenciar maiúsculas/minúsculas; o valor retornado é mascarado (`m***@e***.com`) e não inclui telefone, endereço ou senha. A pesquisa de escola aceita nome parcial e CNPJ exato válido. Vínculos motorista-escola aceitam os status `PENDENTE_ESCOLA`, `PENDENTE_MOTORISTA`, `ATIVO`, `RECUSADO`, `SUSPENSO` e `ENCERRADO`. Vínculos responsável-dependente retornam somente associações ativas. Auditoria aceita `inicio` e `fim` em ISO-8601; o início não pode ser posterior ao fim.

Para suspender uma conta, envie `PUT /Admin/Usuarios/{idUsuario}/Suspender` com motivo obrigatório:

```json
{"motivo":"Justificativa administrativa"}
```

Para reativar, use `PUT /Admin/Usuarios/{idUsuario}/Reativar`; o corpo pode ser omitido ou conter `{"motivo":"Revisão concluída"}`. Ambos retornam `204 No Content`. Não é permitido suspender ou reativar a própria conta, nem suspender o último ADMIN ativo. A suspensão bloqueia novos logins e renovações de refresh token, revoga os refresh tokens existentes e não altera viagens. Access tokens já emitidos continuam válidos até expirar (no máximo 15 minutos).

Consultas e alterações administrativas são auditadas com ator, ação, alvo, resultado e instante. Motivos são registrados quando fornecidos, sem gravar credenciais ou tokens. Os eventos permanecem por 365 dias e são então expurgados automaticamente. A consulta de auditoria também é registrada.

## Resumo dos Casos de Uso Implementados

| Caso de Uso | Endpoint | Controller |
| :--- | :--- | :--- |
| Cadastrar Motorista | `POST /Motorista/Cadastrar` | MotoristaController |
| Cadastrar Veículo | `POST /Motorista/CadastrarVeiculo/{id}` | MotoristaController |
| Consultar próprio perfil (motorista) | `GET /Motorista/Perfil` | MotoristaController |
| Atualizar veículo | `PUT /Motorista/{id}/Veiculo` | MotoristaController |
| Listar documentos do motorista | `GET /Motorista/{id}/Documentos` | MotoristaController |
| Enviar documento do motorista | `POST /Motorista/{id}/Documentos` | MotoristaController |
| Fila de revisão documental | `GET /Admin/Documentos/Pendentes` | AdministradorController |
| Revisar documento | `PUT /Admin/Documentos/{idDocumento}/Revisar` | AdministradorController |
| Pesquisar usuários/escolas/vínculos | `GET /Admin/Usuarios`, `/Escolas`, `/Vinculos/MotoristaEscola` ou `/Vinculos/ResponsavelDependente` | AdministradorController |
| Consultar auditoria administrativa | `GET /Admin/Auditoria` | AdministradorController |
| Suspender/reativar conta | `PUT /Admin/Usuarios/{id}/Suspender` ou `/Reativar` | AdministradorController |
| Buscar escolas para vínculo | `GET /Motorista/Escolas?termo={texto}` | MotoristaController |
| Solicitar vínculo com escola | `POST /Motorista/Escolas/Vinculos` | MotoristaController |
| Listar vínculos e histórico do motorista | `GET /Motorista/Vinculos` e `GET /Motorista/Vinculos/{id}/Historico` | MotoristaController |
| Consultar convites pendentes do motorista | `GET /Motorista/Vinculos/Pendentes` | MotoristaController |
| Aceitar/recusar convite da escola | `POST /Motorista/Vinculos/{id}/Aceitar` ou `/Recusar` | MotoristaController |
| Encerrar vínculo pelo motorista | `POST /Motorista/Vinculos/{id}/Encerrar` | MotoristaController |
| Autenticar Motorista | `POST /Motorista/Autenticar` | MotoristaController |
| Alterar perfil/endereço operacional do motorista | `PUT /Motorista/AlterarInfos` | MotoristaController |
| Cadastrar Escola | `POST /Escola/Cadastrar` | EscolaController |
| Consultar próprio perfil (escola) | `GET /Escola/Perfil` | EscolaController |
| Localizar motorista por e-mail/CPF exato | `POST /Escola/Vinculos/Motoristas/Buscar` | EscolaController |
| Convidar motorista para vínculo | `POST /Escola/Vinculos` | EscolaController |
| Listar vínculos e pendências da escola | `GET /Escola/Vinculos` e `GET /Escola/Vinculos/Pendentes` | EscolaController |
| Listar histórico de vínculo escolar | `GET /Escola/Vinculos/{id}/Historico` | EscolaController |
| Aprovar/recusar solicitação do motorista | `POST /Escola/Vinculos/{id}/Aprovar` ou `/Recusar` | EscolaController |
| Suspender/reativar/encerrar vínculo | `POST /Escola/Vinculos/{id}/Suspender`, `/Reativar` ou `/Encerrar` | EscolaController |
| Autenticar Escola | `POST /Escola/Autenticar` | EscolaController |
| Alterar perfil/endereço operacional da escola | `PUT /Escola/AlterarInfos` | EscolaController |
| Cadastrar Responsável | `POST /Responsavel/Cadastrar` | ResponsavelController |
| Consultar próprio perfil (responsável) | `GET /Responsavel/Perfil` | ResponsavelController |
| Autenticar Responsável | `POST /Responsavel/Autenticar` | ResponsavelController |
| Entrar (token) | `POST /Autenticacao/Entrar` | AutenticacaoController |
| Renovar sessão | `POST /Autenticacao/Renovar` | AutenticacaoController |
| Encerrar sessão | `POST /Autenticacao/Sair` | AutenticacaoController |
| Alterar senha | `POST /Autenticacao/AlterarSenha` | AutenticacaoController |
| Solicitar redefinição | `POST /Autenticacao/SolicitarRedefinicaoSenha` | AutenticacaoController |
| Redefinir senha | `POST /Autenticacao/RedefinirSenha` | AutenticacaoController |
| Alterar Perfil Responsável | `PUT /Responsavel/AlterarInfos` | ResponsavelController |
| Cadastrar Dependente | `POST /Responsavel/CadastrarDependente/{id}` | ResponsavelController |
| Alterar Perfil Dependente | `PUT /Responsavel/Dependente/AlterarInfos` | ResponsavelController |
| Listar endereços do dependente | `GET /Responsavel/Dependente/{id}/Enderecos` | ResponsavelController |
| Adicionar endereço do dependente | `POST /Responsavel/Dependente/{id}/Enderecos` | ResponsavelController |
| Alterar endereço do dependente | `PUT /Responsavel/Dependente/{id}/Enderecos/{idEndereco}` | ResponsavelController |
| Desativar endereço do dependente | `DELETE /Responsavel/Dependente/{id}/Enderecos/{idEndereco}` | ResponsavelController |
| Convidar responsável | `POST /Responsavel/Dependente/{id}/Convites` | ResponsavelController |
| Listar convites pendentes | `GET /Responsavel/Convites/Pendentes` | ResponsavelController |
| Listar responsáveis ativos | `GET /Responsavel/Dependente/{id}/Responsaveis` | ResponsavelController |
| Listar histórico de convites | `GET /Responsavel/Dependente/{id}/Convites` | ResponsavelController |
| Aceitar/recusar convite | `PUT /Responsavel/Convites/{id}/Aceitar` ou `/Recusar` | ResponsavelController |
| Revogar responsável associado | `DELETE /Responsavel/Dependente/{id}/Responsaveis/{idResponsavel}` | ResponsavelController |
| Listar criadores legados pendentes | `GET /Admin/Dependentes/ResponsavelCriadorPendentes` | AdministradorController |
| Atribuir responsável criador | `PUT /Admin/Dependentes/{id}/ResponsavelCriador` | AdministradorController |
| Monitorar Dependente | `GET /Responsavel/Monitorar/{id}` | ResponsavelController |
| Responder Solicitação | `POST /Responsavel/ResponderSolicitacao/{id}` | ResponsavelController |
| Listar Escolas | `GET /Responsavel/ListarEscolas` | ResponsavelController |
| Vincular Escola | `POST /Responsavel/VincularEscola/{r}/{e}/{d}` | ResponsavelController |
| Listar Solicitações (Responsável) | `GET /Responsavel/ListarSolicitacoes/{id}` | ResponsavelController |
| Encerrar Vínculo com Viagem | `PUT /Responsavel/EncerrarVinculoViagem/{id}` | ResponsavelController |
| Criar Viagem (Rota) | `POST /Viagem/CriarViagem/{id}` | ViagemController |
| Visualizar Detalhes da Viagem | `GET /Viagem/Visualizar/{id}` | ViagemController |
| Listar Viagens Ativas | `GET /Viagem/ListarViagens/{id}` | ViagemController |
| Consultar agenda virtual de 7 dias | `GET /Viagem/Agenda/{idMotorista}` | ViagemController |
| Consultar prontidão da viagem | `GET /Viagem/Prontidao/{idViagem}` | ViagemController |
| Desativar Viagem | `PUT /Viagem/DesativarViagem/{id}` | ViagemController |
| Histórico de Viagens | `GET /Viagem/Historico/{id}` | ViagemController |
| Consultar Motorista da Viagem | `GET /Viagem/ConsultarMotorista/{id}` | ViagemController |
| Visualizar Viagem Dia | `GET /Viagem/VisualizarViagemDia/{id}` | ViagemController |
| Dependentes Disponíveis | `GET /Viagem/DependentesDisponiveis/{id}` | ViagemController |
| Solicitar Dependente | `POST /Viagem/SolicitarDependente/{v}/{d}` | ViagemController |
| Iniciar Viagem Diária | `POST /Viagem/IniciarViagemDia/{id}` | ViagemController |
| Preparar execução em data escolhida | `POST /Viagem/PrepararViagemDia/{id}` | ViagemController |
| Alterar Status ViagemDia | `PUT /Viagem/AlterarStatusViagemDia` | ViagemController |
| Alterar Status Presença | `PUT /Viagem/AlterarStatusPresenca` | ViagemController |
| Histórico de Presença da ViagemDia | `GET /Viagem/HistoricoPresenca/{idViagemDia}` | ViagemController |
| Consentimento GPS do motorista | `PUT /Viagem/Localizacao/Consentimento/Motorista` | ViagemController |
| Consentimento GPS do responsável por dependente | `PUT /Viagem/Localizacao/Consentimento/Dependente/{idDependente}` | ViagemController |
| Estado de consentimento GPS da execução | `GET /Viagem/Localizacao/Consentimento/Execucao/{idViagemDia}` | ViagemController |
| Atualizar posição GPS | `PUT /Viagem/Localizacao` | ViagemController |
| Consultar posição atual | `GET /Viagem/Localizacao/Atual/{idViagemDia}` | ViagemController |
| Consultar histórico GPS paginado | `GET /Viagem/Localizacao/Historico/{idViagemDia}` | ViagemController |
| Sincronizar presença offline | `POST /Viagem/SincronizarPresencasOffline` | ViagemController |
| Buscar Paradas da Viagem do Dia | `GET /Viagem/BuscarParadasViagemDia/{id}` | ViagemController |
| Reordenar paradas restantes | `PUT /Viagem/ReordenarParadasViagemDia/{id}` | ViagemController |
| Consultar histórico de status | `GET /Viagem/HistoricoStatusViagemDia/{id}` | ViagemController |
| Consultar histórico de ordem das paradas | `GET /Viagem/HistoricoOrdemParadas/{id}` | ViagemController |
| Enviar Notificação | `POST /Notificacao/Enviar` | NotificacaoController |
| Listar Notificações do Usuário | `GET /Notificacao/Listar/{id}` | NotificacaoController |

---

## 🚍 Motorista

### Cadastrar Motorista

Caso de Uso
Cadastrar Motorista

Fluxo de Negócio
Cadastro de Usuários

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Motorista/Cadastrar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "nome": "João do Caminhão",
  "dataNascimento": "1985-05-20",
  "cpf": "529.982.247-25",
  "cnh": "1234567890",
  "usuarioDTO": {
    "email": "joao@motorista.com",
    "senha": "Senha segura 123!",
    "endereco": "Rua das Flores, 123",
    "telefone": "(11) 99999-9999",
    "tipoPerfil": "MOTORISTA",
    "aceitouTermos": true
  },
  "enderecoOperacionalDTO": {
    "cep": "01001-000",
    "logradouro": "Praça da Sé",
    "numero": "100",
    "complemento": "",
    "bairro": "Sé",
    "cidade": "São Paulo",
    "uf": "SP"
  }
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João do Caminhão",
  "dataNascimento": "1985-05-20",
  "cpf": "529.982.247-25",
  "cnh": "1234567890",
  "usuarioDTO": {
    "id": 10,
    "email": "joao@motorista.com",
    "senha": null,
    "endereco": "Rua das Flores, 123",
    "telefone": "(11) 99999-9999",
    "tipoPerfil": "MOTORISTA",
    "aceitouTermos": true
  },
  "veiculoDTO": null,
  "enderecoOperacionalDTO": {
    "id": 31,
    "cep": "01001000",
    "logradouro": "Praça da Sé",
    "numero": "100",
    "complemento": "",
    "bairro": "Sé",
    "cidade": "São Paulo",
    "uf": "SP",
    "statusValidacao": "VALIDADO"
  }
}
```

Regras de Negócio Aplicadas
* O objeto DTO e o usuário associado não podem ser nulos.
* O campo `tipoPerfil` no `usuarioDTO` deve ser obrigatoriamente `"MOTORISTA"`.
* `enderecoOperacionalDTO` é obrigatório em novos cadastros; o status de validação é definido pelo servidor.
* O sistema cria o usuário e vincula ao motorista.

Códigos de Resposta Esperados
* 200 OK
* 400 Bad Request

Plano de Teste Básico
Pré-condições:
* Nenhuma.
Requisição:
* Enviar POST com os dados acima.
Resultado esperado:
* Motorista retornado com ID preenchido e senha do usuário como `null`.

---

### Cadastrar Veículo

Caso de Uso
Cadastrar Veículo

Fluxo de Negócio
Configuração de Motorista

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Motorista/CadastrarVeiculo/{id_motorista}

Path Variables
* `id_motorista`: ID do motorista que será proprietário do veículo.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "modelo": "Mercedes-Benz Sprinter",
  "placa": "ABC-1234",
  "ano": 2022,
  "capacidade": 20,
  "cor": "Branco"
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João do Caminhão",
  "veiculoDTO": {
    "id": 5,
    "modelo": "Mercedes-Benz Sprinter",
    "placa": "ABC-1234",
    "ano": 2022,
    "capacidade": 20,
    "cor": "Branco"
  }
}
```

Regras de Negócio Aplicadas
* O ID do motorista e o corpo da requisição não podem ser nulos.
* O veículo é criado e associado ao motorista.

Códigos de Resposta Esperados
* 200 OK
* 400 Bad Request

Plano de Teste Básico
Pré-condições:
* Motorista cadastrado com ID 1.
Requisição:
* Enviar POST para `/Motorista/CadastrarVeiculo/1` com o payload acima.
Resultado esperado:
* Objeto Motorista retornado com o campo `veiculoDTO` preenchido.

---

### Autenticar Motorista

Caso de Uso
Login Motorista

Fluxo de Negócio
Autenticação

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Motorista/Autenticar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "email": "joao@motorista.com",
  "senha": "Senha segura 123!"
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João do Caminhão",
  "usuarioDTO": {
    "id": 10,
    "email": "joao@motorista.com",
    "senha": null,
    "tipoPerfil": "MOTORISTA"
  }
}
```

Regras de Negócio Aplicadas
* O usuário deve existir e a senha deve ser compatível.
* O perfil do usuário autenticado deve ser `"MOTORISTA"`.

Códigos de Resposta Esperados
* 200 OK
* 401 Unauthorized (se falhar na autenticação interna)

Plano de Teste Básico
Pré-condições:
* Motorista cadastrado com e-mail e senha informados.
Requisição:
* Enviar POST com o e-mail e senha no corpo.
Resultado esperado:
* Objeto Motorista completo retornado.

---

### Alterar Informações do Motorista

Caso de Uso
Alterar Perfil Motorista

Fluxo de Negócio
Gestão de Perfil

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Motorista/AlterarInfos

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João da Silva",
  "dataNascimento": "1985-05-20",
  "cpf": "123.456.789-00",
  "cnh": "1234567890",
  "usuarioDTO": {
    "id": 10,
    "email": "joao.novo@email.com",
    "senha": "Senha segura 123!",
    "endereco": "Nova Rua, 456",
    "telefone": "(11) 88888-8888",
    "tipoPerfil": "MOTORISTA"
  }
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "nome": "João da Silva",
  "usuarioDTO": {
    "id": 10,
    "email": "joao.novo@email.com",
    "senha": null,
    "endereco": "Nova Rua, 456"
  }
}
```

Regras de Negócio Aplicadas
* O campo `senha` no `usuarioDTO` é obrigatório para validação.
* O sistema valida se a senha informada corresponde à senha atual no banco.
* Atualiza apenas campos editáveis. Não permite alteração de IDs ou tipo de perfil.

Códigos de Resposta Esperados
* 200 OK
* 400 Bad Request
* 401 Unauthorized (senha inválida)
* 404 Not Found

Plano de Teste Básico
Pré-condições:
* Motorista autenticado com ID 1 e senha "Senha segura 123!".
Requisição:
* Enviar PUT com dados novos e senha correta.
Resultado esperado:
* Retorno 200 com DTO atualizado.

---

## 🏫 Escola

### Cadastrar Escola

Caso de Uso
Cadastrar Escola

Fluxo de Negócio
Cadastro de Usuários

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Escola/Cadastrar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "nome": "Escola Municipal Pequeno Príncipe",
  "admResponsavel": "Diretora Cláudia",
  "cnpj": "11.222.333/0001-81",
  "usuarioDTO": {
    "email": "contato@pequenoprincipe.com",
    "senha": "Senha segura 123!",
    "endereco": "Rua Escolar, 999",
    "telefone": "(11) 77777-7777",
    "tipoPerfil": "ESCOLA",
    "aceitouTermos": true
  },
  "enderecoOperacionalDTO": {
    "cep": "01001-000",
    "logradouro": "Praça da Sé",
    "numero": "100",
    "complemento": "",
    "bairro": "Sé",
    "cidade": "São Paulo",
    "uf": "SP"
  }
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Escola Municipal Pequeno Príncipe",
  "cnpj": "11222333000181",
  "admResponsavel": "Diretora Cláudia",
  "usuarioDTO": {
    "id": 15,
    "email": "contato@pequenoprincipe.com",
    "senha": null,
    "aceitouTermos": true,
    "tipoPerfil": "ESCOLA"
  },
  "enderecoOperacionalDTO": {
    "id": 32,
    "cep": "01001000",
    "logradouro": "Praça da Sé",
    "numero": "100",
    "complemento": "",
    "bairro": "Sé",
    "cidade": "São Paulo",
    "uf": "SP",
    "statusValidacao": "VALIDADO"
  }
}
```

Regras de Negócio Aplicadas
* O objeto DTO e o usuário associado não podem ser nulos.
* O campo `tipoPerfil` no `usuarioDTO` deve ser obrigatoriamente `"ESCOLA"`.
* `enderecoOperacionalDTO` é obrigatório em novos cadastros; o status de validação é definido pelo servidor.

Códigos de Resposta Esperados
* 200 OK
* 400 Bad Request

Plano de Teste Básico
Pré-condições:
* Nenhuma.
Requisição:
* Enviar POST com os dados acima.
Resultado esperado:
* Escola retornada com ID preenchido.

---

### Autenticar Escola

Caso de Uso
Login Escola

Fluxo de Negócio
Autenticação

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Escola/Autenticar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "email": "contato@pequenoprincipe.com",
  "senha": "Senha segura 123!"
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Escola Municipal Pequeno Príncipe",
  "usuarioDTO": { ... }
}
```

Regras de Negócio Aplicadas
* O usuário deve existir e a senha deve ser compatível.
* O perfil do usuário autenticado deve ser `"ESCOLA"`.

Códigos de Resposta Esperados
* 200 OK
* 401 Unauthorized

---

### Alterar Informações da Escola

Caso de Uso
Alterar Perfil Escola

Fluxo de Negócio
Gestão de Perfil

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Escola/AlterarInfos

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Novo Nome Escola",
  "admResponsavel": "Diretora Maria",
  "usuarioDTO": {
    "id": 15,
    "email": "escola@novo.com",
    "senha": "Senha segura 123!",
    "endereco": "Rua Nova, 100",
    "telefone": "(11) 99999-0000",
    "tipoPerfil": "ESCOLA"
  }
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 8,
  "nome": "Novo Nome Escola",
  "usuarioDTO": { ... }
}
```

Regras de Negócio Aplicadas
* Requer validação de senha atual (campo `senha` no `usuarioDTO`).
* Atualiza nome e dados de contato.

Códigos de Resposta Esperados
* 200 OK
* 401 Unauthorized
* 404 Not Found

---

### Vínculo operacional entre motorista e escola

Esse fluxo representa a parceria operacional entre uma escola e um motorista. Ele não altera o vínculo escola-dependente, que continua sendo cadastrado pelo responsável.

Todos os endpoints exigem access token e o perfil indicado. O motorista pode pesquisar escolas por nome (mínimo 2 e máximo 100 caracteres; até 20 resultados) ou por CNPJ exato. A resposta da busca contém somente nome e CNPJ; a solicitação usa o CNPJ selecionado:

```http
GET /TioDaPerua/api/Motorista/Escolas?termo=Pequeno
```

```json
[
  {
    "nome": "Escola Municipal Pequeno Príncipe",
    "cnpj": "11222333000181"
  }
]
```

```http
POST /TioDaPerua/api/Motorista/Escolas/Vinculos
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "cnpj": "11222333000181"
}
```

A escola pode localizar um motorista por e-mail ou CPF exatos, usando o corpo da requisição para evitar colocar identificadores pessoais na URL. O resultado contém apenas o ID e o nome; informe exatamente um campo:

```http
POST /TioDaPerua/api/Escola/Vinculos/Motoristas/Buscar
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "email": "motorista@exemplo.com"
}
```

```json
[
  {
    "id": 20,
    "nome": "Nome do Motorista"
  }
]
```

Para criar o convite, a escola envia o ID obtido na busca:

```http
POST /TioDaPerua/api/Escola/Vinculos
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "motoristaId": 20
}
```

Solicitações e convites retornam o vínculo criado, por exemplo:

```json
{
  "id": 30,
  "escolaId": 8,
  "escolaNome": "Escola Municipal Pequeno Príncipe",
  "escolaCnpj": "11222333000181",
  "motoristaId": 20,
  "motoristaNome": "Nome do Motorista",
  "status": "PENDENTE_MOTORISTA",
  "criadoEm": "2026-10-31T21:00:00Z",
  "atualizadoEm": "2026-10-31T21:00:00Z"
}
```

Motorista e escola podem consultar seus próprios vínculos em `/Motorista/Vinculos` e `/Escola/Vinculos`. As listas `/Motorista/Vinculos/Pendentes` e `/Escola/Vinculos/Pendentes` mostram somente convites que exigem decisão do respectivo perfil. Convites não enviam e-mail ou push; o destinatário os consulta pela API.

Estados:

* `PENDENTE_ESCOLA`: motorista solicitou; somente a escola pode aprovar ou recusar.
* `PENDENTE_MOTORISTA`: escola convidou; somente o motorista pode aceitar ou recusar.
* `ATIVO`: vínculo aprovado/aceito.
* `RECUSADO`: ciclo encerrado por recusa.
* `SUSPENSO`: escola suspendeu um vínculo ativo; somente a escola pode reativar.
* `ENCERRADO`: qualquer uma das partes encerrou um vínculo ativo ou suspenso.

Recusa, suspensão e encerramento exigem `{"motivo":"..."}` (máximo de 500 caracteres). Aprovação/aceite e reativação aceitam motivo opcional. Cada criação/transição grava ator, data/hora, estados anterior/novo e motivo na trilha histórica, consultável em `/Motorista/Vinculos/{id}/Historico` ou `/Escola/Vinculos/{id}/Historico`. Recusas e encerramentos preservam o ciclo anterior; um novo pedido cria outro ciclo. Apenas um ciclo pendente pode existir por par motorista-escola; tentativas concorrentes são serializadas pelo bloqueio transacional da escola. Vínculos ativos ou suspensos impedem novo ciclo.

Respostas comuns: `201 Created` na solicitação/convite, `200 OK` em consultas e transições, `400 Bad Request` para identificador/motivo inválido, `403 Forbidden` para usuário fora do vínculo, `404 Not Found` para perfil/escola/motorista inexistente e `409 Conflict` para duplicidade ou transição inválida.

---

## 👨‍👩‍👧‍👦 Responsável

### Cadastrar Responsável

Caso de Uso
Cadastrar Responsável

Fluxo de Negócio
Cadastro de Usuários

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Cadastrar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "nome": "Maria Silva",
  "cpf": "529.982.247-25",
  "dataNascimento": "1990-10-15",
  "usuario": {
    "email": "maria@responsavel.com",
    "senha": "Senha segura 123!",
    "endereco": "Avenida Brasil, 456",
    "telefone": "(11) 88888-8888",
    "tipoPerfil": "RESPONSAVEL",
    "aceitouTermos": true
  }
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria Silva",
  "usuario": {
    "id": 11,
    "email": "maria@responsavel.com",
    "senha": null,
    "aceitouTermos": true,
    "tipoPerfil": "RESPONSAVEL"
  },
  "dependentes": []
}
```

Regras de Negócio Aplicadas
* O objeto DTO não pode ser nulo.
* O sistema cria o usuário e vincula ao responsável.

---

### Autenticar Responsável

Caso de Uso
Login Responsável

Fluxo de Negócio
Autenticação

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Autenticar

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "email": "maria@responsavel.com",
  "senha": "Senha segura 123!"
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria Silva",
  "usuario": { ... },
  "dependentes": [ ... ]
}
```

Regras de Negócio Aplicadas
* O perfil do usuário autenticado deve ser `"RESPONSAVEL"`.

---

### Alterar Informações do Responsável

Caso de Uso
Alterar Perfil Responsável

Fluxo de Negócio
Gestão de Perfil

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/AlterarInfos

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "id": 2,
  "nome": "Maria de Souza",
  "cpf": "987.654.321-99",
  "dataNascimento": "1990-10-15",
  "usuario": {
    "id": 11,
    "email": "maria.nova@email.com",
    "senha": "Senha segura 123!",
    "endereco": "Novo Endereço, 789",
    "telefone": "(11) 77777-7777",
    "tipoPerfil": "RESPONSAVEL"
  }
}
```

Regras de Negócio Aplicadas
* Requer validação de senha atual (campo `senha` dentro do objeto `usuario`).
* Retorna **401 Unauthorized** se a senha estiver incorreta.

---

### Responder Solicitação de Transporte

Caso de Uso
Responder Solicitação

Fluxo de Negócio
Solicitação de Transporte

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ResponderSolicitacao/{id_solicitacao}

Path Variables
* `id_solicitacao`: ID da solicitação enviada pelo motorista.

---

### Listar Escolas Cadastradas

Caso de Uso
Listar Escolas

Fluxo de Negócio
Consulta de Escolas

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ListarEscolas

---

### Vincular Escola a Dependente

Caso de Uso
Vincular Escola a Dependentes

Fluxo de Negócio
Cadastro de Dependentes

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}

---

### Listar Solicitações do Responsável

Caso de Uso
Visualizar Solicitações

Fluxo de Negócio
Solicitação de Transporte

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/ListarSolicitacoes/{idResponsavel}

Path Variables
* `idResponsavel`: ID do responsável.

Regras de Negócio Aplicadas
* Retorna apenas solicitações onde `dataFim` é nulo (solicitações ativas ou em aberto).

---

### Encerrar Vínculo com Viagem

Caso de Uso
Encerrar Vínculo de Dependente com Viagem

Fluxo de Negócio
Gestão de Vínculos

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/EncerrarVinculoViagem/{idSolicitacao}

Path Variables
* `idSolicitacao`: ID da solicitação que será encerrada.

Regras de Negócio Aplicadas
* Define a `dataFim` na solicitação original como o horário atual.
* Marca o registro em `ViagemDependente` como `ativo = false`.
* O dependente deixa de constar em futuras execuções desta viagem.

---

## 🧒 Dependente

### Cadastrar Dependente

Caso de Uso
Cadastrar Dependente

Fluxo de Negócio
Cadastro de Dependentes

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/CadastrarDependente/{id_Responsavel}

Path Variables
* `id_Responsavel`: ID do responsável pelo dependente.

Request Body (JSON de Exemplo)
```json
{
  "nome": "Enzo Silva",
  "cpf": "111.222.333-44",
  "dataNascimento": "2018-01-01",
  "periodo": "MANHA",
  "endereco": "Avenida Brasil, 456"
}
```

---

### Alterar Informações de Dependente

Caso de Uso
Alterar Perfil Dependente

Fluxo de Negócio
Gestão de Perfil

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Dependente/AlterarInfos

Path Variables
Nenhuma.

Query Parameters
Nenhum.

Request Headers
Nenhum.

Request Body (JSON de Exemplo)
```json
{
  "id": 50,
  "nome": "Enzo de Souza",
  "cpf": "111.222.333-44",
  "dataNascimento": "2018-01-01",
  "periodo": "TARDE",
  "endereco": "Novo Endereço, 789",
  "senha": "Senha segura 123!"
}
```

Response Body (JSON de Exemplo)
```json
{
  "id": 50,
  "nome": "Enzo de Souza",
  "cpf": "111.222.333-44",
  "dataNascimento": "2018-01-01",
  "periodo": "TARDE",
  "endereco": "Novo Endereço, 789",
  "escola": {
    "id": 8,
    "nome": "Escola Municipal Pequeno Príncipe"
  }
}
```

Regras de Negócio Aplicadas
* O campo `senha` deve ser preenchido com a senha do **Responsável** vinculado.
* O sistema valida a senha contra o primeiro responsável associado ao dependente.
* Retorna **401 Unauthorized** se a senha estiver incorreta.

---

### Monitorar Dependente

Caso de Uso
Monitorar Dependente

Fluxo de Negócio
Monitoramento

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Responsavel/Monitorar/{id_dependente}

Path Variables
* `id_dependente`: ID do dependente a ser monitorado.

---

## 🛣️ Viagem

### Criar Viagem (Rota)

Caso de Uso
Criar Viagem

Fluxo de Negócio
Configuração de Rota

Método HTTP
POST

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/CriarViagem/{id_motorista}

Regras de Negócio Aplicadas
* A nova viagem é criada com o campo `ativo` definido como `true` por padrão.

---

### Visualizar Detalhes da Viagem

Caso de Uso
Visualizar Viagem

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/Visualizar/{id_viagem}

Regras de Negócio Aplicadas
* Agrega informações de motorista, dependentes vinculados, suas escolas e responsáveis.
* O `DependenteDTO` retornado inclui o campo `escola` (`EscolaResumoDTO`).

---

### Listar Viagens Ativas

Caso de Uso
Listar Viagens do Motorista

Fluxo de Negócio
Gestão de Viagens

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/ListarViagens/{idMotorista}

Path Variables
* `idMotorista`: ID do motorista proprietário das rotas.

Response Body (JSON de Exemplo)
```json
[
  {
    "id": 1,
    "motoristaId": 1,
    "periodo": "MANHA_IDA",
    "ativo": true
  }
]
```

Regras de Negócio Aplicadas
* Retorna apenas viagens vinculadas ao motorista informado onde `ativo = true`.

---

### Desativar Viagem

Caso de Uso
Encerrar Rota Permanentemente

Fluxo de Negócio
Gestão de Viagens

Método HTTP
PUT

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/DesativarViagem/{idViagem}

Path Variables
* `idViagem`: ID da viagem a ser desativada.

Regras de Negócio Aplicadas
* Operação transacional em cascata.
* Define `viagem.ativo = false`.
* Desativa todos os registros de `ViagemDependente` relacionados (`ativo = false`).
* Encerra todas as solicitações ativas associadas à viagem (`dataFim = now()`).
* Se a viagem já estiver desativada, retorna **409 Conflict**.

Códigos de Resposta Esperados
* 200 OK
* 404 Not Found
* 409 Conflict

---

### Histórico de Viagens

Caso de Uso
Visualizar Execuções de Viagens Antigas

Fluxo de Negócio
Gestão de Viagens

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/Historico/{idMotorista}

Path Variables
* `idMotorista`: ID do motorista.

Response Body (JSON de Exemplo)
```json
[
  {
    "id": 15,
    "data": "2026-05-12",
    "status": "FINALIZADA",
    "dataUltimaAlteracaoStatus": "2026-05-12T10:00:00"
  },
  {
    "id": 14,
    "data": "2026-05-11",
    "status": "FINALIZADA",
    "dataUltimaAlteracaoStatus": "2026-05-11T10:05:00"
  }
]
```

Regras de Negócio Aplicadas
* Retorna a lista de viagens diárias (`ViagemDia`) executadas por qualquer rota do motorista.
* Ordenado do registro mais recente para o mais antigo.

---

### Consultar Motorista da Viagem

Caso de Uso
Identificar Motorista pelo Dia da Viagem

Fluxo de Negócio
Monitoramento / Segurança

Método HTTP
GET

URL Local
http://localhost:8080/TioDaPerua/api/Viagem/ConsultarMotorista/{idViagemDia}

Path Variables
* `idViagemDia`: ID da execução diária da viagem.

Response Body (JSON de Exemplo)
```json
{
  "motoristaId": 1,
  "nomeMotorista": "João do Caminhão",
  "telefone": "(11) 99999-9999",
  "modeloVeiculo": "Mercedes Sprinter",
  "placaVeiculo": "ABC-1234",
  "corVeiculo": "Branco"
}
```

Regras de Negócio Aplicadas
* Permite ao passageiro/responsável obter dados de contato e do veículo a partir de uma viagem em andamento ou planejada.

---

### Visualizar Viagem Dia

#### Caso de Uso
Visualizar Detalhes da Execução Diária

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/VisualizarViagemDia/{id_viagemDia}

#### Path Variables
- `id_viagemDia`: ID da execução diária da viagem.

#### Response Body (JSON de Exemplo)
```json
{
  "id": 15,
  "data": "2026-05-12",
  "status": "EM_ANDAMENTO",
  "dataUltimaAlteracaoStatus": "2026-05-12T10:00:00"
}
```

#### Regras de Negócio Aplicadas
- Retorna os detalhes de uma execução diária específica (`ViagemDia`) a partir de seu ID.
- Se o ID não existir, retorna **404 Not Found**.

#### Plano de Teste Básico
1. Pré-condições: Viagem do dia (ID 15) cadastrada.
2. Requisição: Enviar GET para `/Viagem/VisualizarViagemDia/15`.
3. Resultado esperado: Objeto ViagemDia correspondente retornado.

---

### Iniciar Viagem Diária

#### Caso de Uso
Iniciar Viagem Diária

#### Fluxo de Negócio
Execução Diária

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/IniciarViagemDia/{id_viagem}

#### Path Variables
- `id_viagem`: ID da rota fixa que será executada hoje.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
{
  "id": 15,
  "data": "2026-05-12",
  "status": "PLANEJADA"
}
```

#### Regras de Negócio Aplicadas
- Cria um registro em `ViagemDia` para a data atual.
- Busca todos os dependentes com vínculo ativo (`ViagemDependente`) para esta rota.
- Cria automaticamente um registro em `ViagemPresenca` para cada dependente com status `ESPERANDO`.
- Gera e persiste os snapshots de presença/endereço e a ordem inicial das paradas.
- **Retorno de Instância:** Se a viagem já foi iniciada hoje, o endpoint retorna os dados da execução existente. Caso contrário, retorna a nova execução criada.

#### Plano de Teste Básico
1. Pré-condições: Viagem com dependentes ativos.
2. Requisição: Enviar POST para `/Viagem/IniciarViagemDia/1`.
3. Resultado esperado: Registros criados no banco (ViagemDia e ViagemPresenca).

---

### Alterar Status de ViagemDia

#### Caso de Uso
Alterar Status de ViagemDia

#### Fluxo de Negócio
Execução Diária

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/AlterarStatusViagemDia

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "idViagemDia": 1,
  "novoStatus": "EM_ANDAMENTO",
  "motivo": null
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 1,
  "data": "2026-05-12",
  "status": "EM_ANDAMENTO",
  "dataUltimaAlteracaoStatus": "2026-05-12T20:00:00"
}
```

#### Regras de Negócio Aplicadas
- Transições permitidas: `PLANEJADA → EM_ANDAMENTO` ou `CANCELADA`; `EM_ANDAMENTO → FINALIZADA` ou `INTERROMPIDA`. Estados terminais não reabrem.
- O início só é permitido quando a prontidão não contém bloqueios: documentos do motorista válidos, capacidade suficiente, vínculo ativo com as escolas e endereços operacionais/dependentes estruturados com CEP validado.
- A finalização exige que todas as presenças estejam em `DESEMBARCADO` ou `FALTOU`.
- `motivo` é obrigatório para `CANCELADA` ou `INTERROMPIDA`; nos demais status é opcional.
- Cada transição registra status anterior/novo, usuário ator, instante do servidor e motivo. `dataUltimaAlteracaoStatus` também é atualizado.

#### Plano de Teste Básico
1. Pré-condições: Viagem do dia (ID 1) cadastrada.
2. Requisição: Enviar PUT para `/Viagem/AlterarStatusViagemDia` com `novoStatus: "EM_ANDAMENTO"`.
3. Resultado esperado: Objeto ViagemDia atualizado com novo status e data de alteração preenchida.

---

### Alterar Status de Presença

#### Caso de Uso
Alterar Status de Presença

#### Fluxo de Negócio
Execução Diária

#### Método HTTP
PUT

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/AlterarStatusPresenca

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "idViagemDia": 15,
  "idDependente": 50,
  "novoStatus": "EMBARCADO"
}
```

#### Response Body (JSON de Exemplo)
```json
[
  {
    "id": 101,
    "ordem": 1,
    "tipoParada": "MOTORISTA",
    "nomeLocal": "Saída: Casa do Motorista",
    "endereco": "Rua das Flores, 123",
    "listaDependentes": []
  },
  {
    "id": 102,
    "ordem": 2,
    "tipoParada": "RESPONSAVEL",
    "nomeLocal": "Casa de Maria Silva",
    "endereco": "Avenida Brasil, 456",
    "listaDependentes": [
      {
        "id": 50,
        "nomeDependente": "Enzo Silva",
        "nomeResponsavel": "Maria Silva",
        "statusEmbarque": "EMBARCADO"
      }
    ]
  },
  ...
]
```

#### Regras de Negócio Aplicadas
- Localiza o registro de presença via `idViagemDia` e `idDependente`.
- Somente o motorista autenticado dono da execução pode alterar o status.
- A alteração é permitida enquanto a execução estiver `PLANEJADA` ou `EM_ANDAMENTO`; execuções encerradas não aceitam alterações.
- Transições permitidas: `ESPERANDO` para `EMBARCADO` ou `FALTOU`, e `EMBARCADO` para `DESEMBARCADO`. Retrocessos não são permitidos.
- Repetir uma atualização para o status atual é idempotente e não cria outro evento nem altera os horários.
- Se status for `EMBARCADO`, registra o `horarioEmbarque`.
- Se status for `DESEMBARCADO`, registra o `horarioDesembarque`.
- Cada transição efetiva registra status anterior/novo, ator e instante em histórico append-only. Não há expurgo automático até definição de política formal de retenção.
- **Retorno Imediato:** Após a alteração, o sistema recalcula e retorna a lista completa de paradas da viagem, garantindo que o frontend seja atualizado sem chamadas extras.

#### Plano de Teste Básico
1. Pré-condições: Viagem do dia iniciada (ID 15) com dependente (ID 50) com status `ESPERANDO`.
2. Requisição: Enviar PUT para `/Viagem/AlterarStatusPresenca` com o payload acima.
3. Resultado esperado: Lista de paradas retornada com o dependente exibindo `"statusEmbarque": "EMBARCADO"`.

---

### Histórico de Presença da ViagemDia

#### Caso de Uso
Consultar auditoria de embarques, desembarques e faltas de uma execução.

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/HistoricoPresenca/{idViagemDia}

#### Path Variables
- `idViagemDia`: ID da execução diária da viagem.

#### Autorização
- Somente o motorista autenticado dono da execução pode consultar o histórico.

#### Response Body (JSON de Exemplo)
```json
[
  {
    "id": 31,
    "idPresenca": 110,
    "idDependente": 50,
    "statusAnterior": "ESPERANDO",
    "statusNovo": "EMBARCADO",
    "atorUsuarioId": 15,
    "ocorridoEm": "2026-11-01T08:15:00"
  }
]
```

O histórico registra somente transições efetivas. Repetições idempotentes do status atual não criam linhas adicionais. Os eventos não são removidos automaticamente enquanto não houver uma política formal de retenção.

---

### Consentimento, envio e consulta de localização GPS

Os endpoints de localização não encaminham coordenadas a provedores externos. A roteirização continua manual. O sistema exige que o motorista e pelo menos um responsável ativo por cada dependente da execução tenham aceitado a versão vigente da política GPS. O consentimento permanece válido até ser revogado ou a versão da política mudar. Configure `GPS_CONSENT_VERSION`; sem essa configuração, o registro de consentimento e o GPS são recusados com HTTP 503.

`PUT /Viagem/Localizacao/Consentimento/Motorista` e `PUT /Viagem/Localizacao/Consentimento/Dependente/{idDependente}` recebem:

```json
{
  "aceito": true
}
```

O segundo endpoint exige vínculo ativo do responsável com o dependente. Respostas registram a versão da política e o instante; eventos de aceite/revogação são mantidos por 1 ano. Após esse prazo, eventos antigos são expurgados, preservando o evento mais recente por usuário, escopo, dependente e versão de política para manter o estado vigente. Essa retenção é distinta dos 7 dias de retenção dos pontos GPS. Revogar um consentimento durante uma execução `EM_ANDAMENTO` suspende definitivamente o GPS dessa execução para todos; a viagem e a presença manual continuam funcionando. Essa suspensão é específica da execução; uma execução posterior poderá usar GPS se os consentimentos vigentes continuarem aceitos.

`GET /Viagem/Localizacao/Consentimento/Execucao/{idViagemDia}` é restrito ao motorista dono e informa o consentimento do motorista, os IDs dos dependentes ainda sem um responsável consentindo, se a execução foi suspensa e se pode receber posição.

`PUT /Viagem/Localizacao` é restrito ao motorista dono e aceita somente execução `EM_ANDAMENTO`, com todos os consentimentos vigentes e sem suspensão:

```json
{
  "idViagemDia": 15,
  "latitude": -23.55052,
  "longitude": -46.63331,
  "instanteDispositivo": "2026-11-01T11:15:00Z"
}
```

O backend valida os limites geográficos, rejeita timestamp futuro acima de 30 segundos, timestamp com mais de 5 minutos de atraso e posição enviada antes do intervalo mínimo de 15 segundos. Coordenadas são arredondadas a 5 casas decimais. Uma posição posterior no dispositivo não pode ser substituída por outra mais antiga. Cada atualização grava histórico e atualiza separadamente a posição corrente.

`GET /Viagem/Localizacao/Atual/{idViagemDia}` permite consulta pelo motorista dono ou responsável ativo ligado a um dependente daquela execução. A resposta contém `instanteDispositivo`, `recebidoEm` e `desatualizada`; a posição é considerada desatualizada após 45 segundos sem atualização. Se ainda não houver posição, a API responde como recurso indisponível.

`GET /Viagem/Localizacao/Historico/{idViagemDia}` usa `inicio` e `fim` em ISO-8601 UTC, além de `pagina` (padrão 0) e `tamanho` (padrão 100, máximo 500). O intervalo deve estar dentro dos últimos 7 dias. A resposta é paginada e só é acessível a perfis autorizados com consentimentos vigentes. O histórico é expurgado de hora em hora quando ultrapassa 7 dias; a posição corrente é apagada ao terminar, cancelar ou interromper a execução. Dados de localização não são incluídos em logs nem em notificações.

---

### Sincronização offline de presença

`POST /Viagem/SincronizarPresencasOffline` exige autenticação de motorista e autorização do motorista dono de cada execução. O endpoint recebe de 1 a 100 eventos, processados na ordem enviada, cada um em sua própria transação. Um erro em um item não desfaz os demais.

```json
{
  "eventos": [
    {
      "idEvento": "9f1c9947-a151-4b13-9e31-4cfd079e28e2",
      "idViagemDia": 15,
      "idDependente": 7,
      "statusEsperado": "ESPERANDO",
      "statusNovo": "EMBARCADO",
      "ocorridoEm": "2026-11-01T11:15:00Z"
    }
  ]
}
```

O cliente gera um UUID v4 por evento e o mantém em todas as tentativas de reenvio. O instante `ocorridoEm` deve ser UTC. Apenas `ESPERANDO → EMBARCADO`, `ESPERANDO → FALTOU` e `EMBARCADO → DESEMBARCADO` são aceitos, e somente enquanto a execução estiver `EM_ANDAMENTO`. O endpoint não sincroniza GPS, reordenação de paradas ou estado do ciclo da viagem.

A resposta contém um resultado para cada item, preservando a ordem:

```json
{
  "resultados": [
    {
      "idEvento": "9f1c9947-a151-4b13-9e31-4cfd079e28e2",
      "idViagemDia": 15,
      "idDependente": 7,
      "resultado": "APLICADO",
      "statusAtual": "EMBARCADO",
      "mensagem": null
    }
  ]
}
```

`APLICADO` indica transição persistida; `DUPLICADO` indica reenvio do mesmo UUID e conteúdo; `CONFLITO` indica divergência de estado, execução terminal ou UUID reutilizado com outro conteúdo; `REJEITADO` indica evento inválido ou ator sem autorização. Em conflito, `statusAtual` informa o estado observado quando disponível. Cada registro de histórico guarda UUID, instante do dispositivo e instante de recebimento do servidor. Erros de estrutura do envelope (lista ausente/vazia ou acima de 100 itens) são recusados pela validação da requisição; eventos inválidos individualmente retornam `REJEITADO` sem interromper os outros itens.

---

### Buscar Paradas da Viagem do Dia

#### Caso de Uso
Gerar Rota Diária

#### Fluxo de Negócio
Execução Diária

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Viagem/BuscarParadasViagemDia/{idViagemDia}

#### Path Variables
- `idViagemDia`: ID da execução diária da viagem.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
[
  {
    "id": 101,
    "ordem": 1,
    "tipoParada": "MOTORISTA",
    "nomeLocal": "Saída: Casa do Motorista",
    "endereco": "Rua das Flores, 123",
    "listaDependentes": []
  },
  {
    "id": 102,
    "ordem": 2,
    "tipoParada": "RESPONSAVEL",
    "nomeLocal": "Casa de Maria Silva",
    "endereco": "Avenida Brasil, 456",
    "listaDependentes": [
      {
        "id": 50,
        "nomeDependente": "Enzo Silva",
        "nomeResponsavel": "Maria Silva",
        "statusEmbarque": "EMBARCADO"
      }
    ]
  },
  {
    "id": 103,
    "ordem": 3,
    "tipoParada": "ESCOLA",
    "nomeLocal": "Escola: Pequeno Príncipe",
    "endereco": "Rua Escolar, 999",
    "listaDependentes": [
      {
        "id": 50,
        "nomeDependente": "Enzo Silva",
        "nomeResponsavel": "Maria Silva",
        "statusEmbarque": "ESPERANDO"
      }
    ]
  },
  {
    "id": 104,
    "ordem": 4,
    "tipoParada": "MOTORISTA",
    "nomeLocal": "Retorno: Casa do Motorista",
    "endereco": "Rua das Flores, 123",
    "listaDependentes": []
  }
]
```

#### Regras de Negócio Aplicadas
- Retorna as paradas persistidas como snapshot da execução, sem recalcular endereço ou participantes depois da preparação.
- **Ida:** Motorista -> Responsáveis -> Escolas -> Motorista.
- **Volta:** Motorista -> Escolas -> Responsáveis -> Motorista.
- Agrupa dependentes que moram no mesmo endereço ou estudam na mesma escola.
- **Sincronização em Tempo Real:** Para cada dependente listado em uma parada, o sistema consulta e retorna o `statusEmbarque` atualizado de acordo com o registro diário em `ViagemPresenca`.

#### Plano de Teste Básico
1. Pré-condições: Viagem do dia iniciada.
2. Requisição: Enviar GET para `/Viagem/BuscarParadasViagemDia/5`.
3. Resultado esperado: Lista ordenada de paradas com endereços e dependentes.

---

### Agenda virtual e prontidão

`GET /Viagem/Agenda/{idMotorista}` retorna cada itinerário ativo para hoje e os seis dias seguintes. A agenda não cria execuções futuras: itens sem execução retornam `materializada: false`, `idViagemDia: null` e `status: null`.

`GET /Viagem/Prontidao/{idViagem}` retorna os bloqueios antes do início:

```json
{
  "idViagem": 7,
  "pronta": false,
  "bloqueios": [
    "Endereço estruturado do motorista ausente ou sem CEP validado",
    "Não há vínculo operacional ativo com a escola 12"
  ]
}
```

### Preparar execução em data futura

`POST /Viagem/PrepararViagemDia/{id_viagem}` recebe uma data entre hoje e os seis dias seguintes:

```json
{ "data": "2026-10-31" }
```

A operação materializa `ViagemDia`, presenças e snapshots uma única vez para a combinação itinerário/data. O endpoint legado `POST /Viagem/IniciarViagemDia/{id_viagem}` continua preparando a data de hoje. Preparar não inicia a execução; o motorista ainda precisa solicitar a transição para `EM_ANDAMENTO`, sujeita à prontidão.

### Reordenar paradas e consultar auditoria

`PUT /Viagem/ReordenarParadasViagemDia/{idViagemDia}` recebe a ordem completa das paradas já persistidas:

```json
{ "idsParadas": [101, 103, 102, 104] }
```

Cada parada da resposta inclui o `id` persistido, que deve ser usado na reordenação. O pedido deve conter todos os IDs exatamente uma vez; as paradas inicial/final do motorista são fixas. Durante a execução, paradas concluídas não podem mudar de posição. Participantes e endereços do snapshot não são alterados por este endpoint.

Os endpoints `GET /Viagem/HistoricoStatusViagemDia/{idViagemDia}` e `GET /Viagem/HistoricoOrdemParadas/{idViagemDia}` retornam respectivamente as transições de estado e as mudanças de ordem, com ator e instante registrados.

---

## 🔔 Notificação

### Enviar Notificação

#### Caso de Uso
Enviar Notificação

#### Fluxo de Negócio
Comunicação

#### Método HTTP
POST

#### URL Local
http://localhost:8080/TioDaPerua/api/Notificacao/Enviar

#### Path Variables
Nenhuma.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
```json
{
  "titulo": "Atraso na Viagem",
  "mensagem": "A van atrasará 10 minutos devido ao trânsito.",
  "remetenteId": 10,
  "destinatarioId": 20,
  "visto": false
}
```

#### Response Body (JSON de Exemplo)
```json
{
  "id": 100,
  "titulo": "Atraso na Viagem",
  "mensagem": "A van atrasará 10 minutos devido ao trânsito.",
  "data": "2026-05-09T10:00:00",
  "visto": false,
  "remetenteId": 10,
  "destinatarioId": 20
}
```

#### Regras de Negócio Aplicadas
- Valida remetente e destinatário existentes.
- A data é gerada automaticamente no momento do envio.
- O campo `visto` inicia como `false`.

#### Plano de Teste Básico
1. Pré-condições: Usuários com IDs 10 e 20 cadastrados.
2. Requisição: Enviar POST com o payload acima.
3. Resultado esperado: Notificação persistida e retornada com ID e data.

---

### Listar Notificações do Usuário

#### Caso de Uso
Listar Notificações

#### Fluxo de Negócio
Comunicação

#### Método HTTP
GET

#### URL Local
http://localhost:8080/TioDaPerua/api/Notificacao/Listar/{id_usuario}

#### Path Variables
- `id_usuario`: ID do usuário cujas notificações serão listadas.

#### Query Parameters
Nenhum.

#### Request Body (JSON de Exemplo)
Não possui request body.

#### Response Body (JSON de Exemplo)
```json
[
  {
    "id": 100,
    "titulo": "Atraso na Viagem",
    "mensagem": "...",
    "data": "...",
    "visto": false,
    "remetenteId": 10,
    "destinatarioId": 20
  }
]
```

#### Regras de Negócio Aplicadas
- Retorna todas as notificações onde o usuário é o destinatário.

#### Plano de Teste Básico
1. Pré-condições: Notificações enviadas para o usuário ID 20.
2. Requisição: Enviar GET para `/Notificacao/Listar/20`.
3. Resultado esperado: Lista de notificações retornada.

---

### Notificações automáticas e retenção

Além do envio manual existente, o backend cria notificações in-app na mesma transação das seguintes operações:

- resposta aceita ou recusada a uma solicitação de transporte: notifica o motorista que solicitou;
- início, finalização, cancelamento ou interrupção de execução: notifica os responsáveis ativos dos dependentes da execução;
- embarque, desembarque ou falta: notifica os responsáveis ativos do dependente envolvido.

A criação de solicitação não gera notificação. Repetir uma transição já concluída não gera outro evento: respostas de solicitação repetidas são conflitantes, transições de presença repetidas não alteram o histórico e eventos offline repetidos retornam `DUPLICADO`. As notificações são persistidas no banco; falha na gravação interrompe e reverte a operação de negócio, sem falha silenciosa. Não há envio push/e-mail nem preferências de categoria/canal nesta etapa. Registros lidos e não lidos são expurgados quando ultrapassam 90 dias, em job diário.

---

## Observações Gerais

- **Convenções REST:** A API segue padrões RESTful, utilizando os métodos HTTP adequados para cada operação.
- **Soft Delete:** A remoção de rotas (Viagens) não exclui registros do banco; utiliza-se o campo `ativo` para controle de visibilidade (Soft Delete), preservando o histórico de presenças.
- **Validação de Senha:** Todos os endpoints de `AlterarInfos` exigem a senha atual do usuário no payload. Caso a senha esteja incorreta, a API retorna `401 Unauthorized`.
- **Integridade de Vínculos:** Ao desativar uma viagem, o sistema automaticamente encerra os vínculos ativos dos dependentes e as solicitações pendentes/aceitas relacionadas, garantindo que nenhum aluno fique "preso" a uma rota inexistente.
- **Compatibilidade FrontEnd:** Todos os endpoints de autenticação utilizam `POST` para permitir o envio do `RequestBody` em conformidade com os padrões de navegadores modernos.

---

## Histórico de Versões

| Versão | Descrição |
| :--- | :--- |
| V1 | Documentação inicial (Estrutura detalhada) |
| V2 | Inclusão de novas funcionalidades e correções de FrontEnd |
| V3 | Consolidação completa e definitiva (Padronização final) |

# Notificações com tipo, central, histórico, ausência, atraso, troca de veículo, termos e push

**Branch:** `feat/busca-solicitacao-viagem-escola` · **Data:** 06/11/2026
**Situação:** implementado, com **137 testes unitários passando**. **Sem commit e sem push**, aguardando aprovação.
**Especificação:** frontendreact `docs/ESPECIFICACAO_VALIDACAO_NOTIFICACOES_HISTORICO.md`.
**App correspondente:** frontendreact `docs/IMPLEMENTACAO_VALIDACAO_NOTIFICACOES_HISTORICO.md`.

**Fora desta entrega:** "motorista próximo" (GPS), NFC, preferências de notificação por tipo, troca de motorista.

---

## 1. Banco de dados

Tudo é criado pelo `spring.jpa.hibernate.ddl-auto=update` ao iniciar. Nenhum script manual é necessário.

| Tabela / coluna | Origem | Observação |
|---|---|---|
| `notificacao.tipo` (40), `prioridade` (10), `viagem_dia_id`, `dependente_id`, `lida_em`, `chave_dedup` (160, única) | `entities/Notificacao.java` | Colunas novas, todas opcionais. Registros antigos aparecem como `GERAL`/`NORMAL`. `tipo` e `prioridade` são texto, sem CHECK, para tipos novos não exigirem migração |
| `dispositivo_push` | `entities/DispositivoPush.java` (novo) | usuario, `expo_token` (único), plataforma, ativo, `criado_em`, `ultimo_uso_em` |
| `ausencia_informada` | `entities/AusenciaInformada.java` (novo) | dependente, informada_por, data, trecho (`TODOS`/`IDA`/`VOLTA`), motivo (200), `criada_em`. Única por dependente + data + trecho |
| Restrição `viagem_periodo_check` | `config/CorrecaoEsquemaBanco.java` (novo) | Ao iniciar, executa `ALTER TABLE IF EXISTS public.viagem DROP CONSTRAINT IF EXISTS viagem_periodo_check`, para bancos antigos aceitarem `MANHA_VOLTA`. Se falhar, só registra um aviso no log |

## 2. Tipos de notificação

`enums/TipoNotificacao.java` (novo). Cada tipo tem grupo (para os filtros do app) e prioridade:

| Grupo | Tipos | Prioridade |
|---|---|---|
| VIAGEM | `MOTORISTA_A_CAMINHO`, `EMBARQUE`, `DESEMBARQUE`, `VIAGEM_FINALIZADA` | NORMAL |
| ALERTA | `AUSENCIA`, `AUSENCIA_INFORMADA`, `ATRASO`, `VIAGEM_CANCELADA`, `VIAGEM_INTERROMPIDA`, `TROCA_VEICULO` | ALTA |
| SOLICITACAO | `SOLICITACAO` | NORMAL |
| AVISO | `MANUAL`, `GERAL` | NORMAL |

## 3. Textos (`services/MensagensNotificacao.java`, novo)

Só o primeiro nome do aluno e do motorista e nunca o endereço, porque o texto pode aparecer na tela bloqueada. Exemplos:

| Evento | Texto |
|---|---|
| Embarque | "Enzo embarcou às 06:42 com Carlos (ABC1D23)." |
| Desembarque na ida / volta | "Enzo foi entregue na EMEF Pedro II às 07:15." / "Enzo foi entregue em casa às 12:40." |
| Falta | "Enzo não embarcou na Viagem para EMEF Pedro II (Manhã · ida) de hoje. Se for engano, fale com o motorista." |
| Início | "Carlos saiu para a Viagem para … ." |
| Cancelamento / interrupção | "… foi cancelada: Pneu furado. Providencie outro transporte." |
| Atraso | "… está cerca de 15 min atrasada: trânsito." |
| Troca de veículo | "Hoje a viagem será feita no Renault Master prata (XYZ5678)." |
| Ausência avisada | "Enzo não vai na ida de amanhã: consulta médica." |

## 4. Serviço de notificações

`services/NotificacaoService.java`:
- `record Contexto(tipo, viagemDiaId, dependenteId, chaveEvento)` acompanha cada aviso automático. O método antigo de 4 argumentos continua existindo e grava `GERAL`.
- **Deduplicação:** quando há `chaveEvento`, a chave `tipo:chaveEvento:destinatario` (até 160 caracteres) é gravada em `chave_dedup`, e quem já recebeu aquele evento não recebe de novo (ex.: sincronização offline repetida).
- Avisos manuais (`/Notificacao/Enviar`) são gravados como `MANUAL`.
- `listar(usuario, somenteNaoLidas, grupo, pagina, tamanho)`: página padrão 30, máximo 100, mais recentes primeiro.
- `contarNaoLidas`, `marcarTodasLidas` (grava `lida_em`); `visualizarNotificacao` também grava `lida_em`.
- Depois de gravar, publica `NotificacoesCriadasEvento` (usado pelo push).
- **Retenção mantida em 90 dias** (o teste da equipe exige isso). A especificação pedia 1 ano; mudar é decisão do grupo.

**Quem chama:**
- `ViagemService`: presença (`EMBARQUE`, `DESEMBARQUE`, `AUSENCIA`, chave `presenca:{viagemDia}:{aluno}:{status}`), execução (`MOTORISTA_A_CAMINHO`, `VIAGEM_FINALIZADA`, `VIAGEM_CANCELADA`, `VIAGEM_INTERROMPIDA`, chave `execucao:{viagemDia}:{status}`), atraso e troca de veículo.
- `SolicitacaoService`: os 4 avisos de solicitação viraram `SOLICITACAO` com o aluno; o texto agora diz "Responda em Vínculos." (nome da aba no app).
- `AusenciaService`: `AUSENCIA_INFORMADA`.

## 5. Endpoints novos ou alterados

| Método e caminho | Acesso | O que faz |
|---|---|---|
| `GET /Notificacao/Listar/{idUsuario}?status=TODAS\|NAO_LIDAS&grupo=VIAGEM\|ALERTA\|SOLICITACAO\|AVISO&pagina=&tamanho=` | o próprio usuário | Central paginada. **Sem parâmetros, igual a antes** (só as não lidas) |
| `GET /Notificacao/NaoLidas/Contagem` | autenticado | `{ "naoLidas": n }` |
| `PUT /Notificacao/MarcarTodasLidas` | autenticado | `{ "marcadas": n }` |
| `POST /Notificacao/Dispositivos` `{expoToken, plataforma}` | autenticado | Registra o aparelho (ou transfere o token para este usuário) |
| `POST /Notificacao/Dispositivos/Remover` `{expoToken}` | autenticado | Para de enviar push para o aparelho |
| `GET /Viagem/Historico/{idMotorista}?de=&ate=` | dono | **Mudou a resposta:** `List<ViagemDiaResumoDTO>` (mantém `id`, `data`, `status`, `dataUltimaAlteracaoStatus` e acrescenta viagem, período, contagens, início, fim e motivo). Padrão: último ano |
| `POST /Viagem/ViagemDia/{id}/Atraso` `{minutos, motivo}` | dono da execução | Avisa os responsáveis dos alunos que estão aguardando ou no veículo. 5 a 180 min; motivo até 200; só PLANEJADA/EM_ANDAMENTO; no máximo um aviso a cada 10 min (409) |
| `GET /Responsavel/Dependente/{id}/Historico?de=&ate=` | responsável do aluno | `List<HistoricoAlunoDTO>`. Padrão: 30 dias; máximo 366 |
| `GET /Responsavel/Dependente/{id}/Ausencias` | responsável do aluno | Ausências de hoje em diante |
| `POST /Responsavel/Dependente/{id}/Ausencias` `{data, trecho, motivo}` | responsável do aluno | Avisa a ausência (ver regras abaixo). 201 |
| `DELETE /Responsavel/Dependente/{id}/Ausencias/{idAusencia}` | responsável do aluno | Cancela; recusado (409) se a viagem daquele dia já foi preparada |
| `POST /Autenticacao/AceitarTermos` | autenticado | Novo aceite da versão vigente |
| `GET /Termos/vigente` | **público** | `{versao, caminhoPdf}` |
| `GET /termos/termos-de-uso-e-privacidade.pdf` | **público** | PDF dos termos (arquivo estático) |

`AutenticacaoResponseDTO` ganhou `termosPendentes` (login e renovação): `true` quando `TERMS_VERSION` está configurada e é diferente da versão que o usuário aceitou.

## 6. Regras novas

**Ausência avisada** (`services/AusenciaService.java`, novo)
- Data de hoje até 60 dias; trecho `TODOS` (padrão), `IDA` ou `VOLTA`.
- Não aceita dois avisos que se sobrepõem no mesmo dia: "ida e volta" conflita com qualquer aviso do dia; "só ida"/"só volta" conflita com o mesmo trecho ou com "ida e volta" (409).
- Viagem do dia ainda não preparada: o aluno já entra como `FALTOU` quando ela for preparada (`ViagemService.prepararViagemDia` → `aplicarAusenciasInformadas`).
- Viagem do dia já preparada e não encerrada: o aluno que está `ESPERANDO` passa a `FALTOU` na hora.
- O motorista das viagens afetadas e os outros responsáveis do aluno recebem o aviso.

**Troca de veículo:** `MotoristaService.alterarVeiculo` chama `ViagemService.notificarTrocaDeVeiculo` quando muda modelo, placa ou cor. Avisa os responsáveis das viagens de hoje planejadas ou em andamento (uma vez por placa por dia). `ViagemService` é injetado com `@Lazy` para evitar ciclo.

**Histórico** (`services/HistoricoService.java`, novo; DTOs `ViagemDiaResumoDTO` e `HistoricoAlunoDTO`): início, fim e motivo vêm do histórico de status da viagem do dia.

## 7. Push (`services/PushService.java`, novo)

- Escuta `NotificacoesCriadasEvento` **depois do commit** (`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`) em uma thread separada (`@Async("pushExecutor")`, bean novo em `config/AsyncConfig.java`: 2 a 4 threads, fila de 500). Falha no push nunca desfaz o aviso in-app.
- Envia para a Expo Push API em lotes de 100: título, texto, som, `priority` (`high` para alertas), `channelId` (`alertas` ou `viagem`) e `data` com `notificacaoId`, `tipo`, `viagemDiaId`, `dependenteId`.
- Token devolvido como `DeviceNotRegistered` é desativado.
- Propriedades: `app.push.enabled` (padrão `true`) e `app.push.expo-url`.
- Tokens aceitos: `ExponentPushToken[...]` ou `ExpoPushToken[...]`.

## 8. Termos

- `services/TermosService.java`: `CAMINHO_PDF`, `getVersaoVigente()`, `termosPendentes(usuario)`; `registrarAceite` também grava `aceitouTermos = true`.
- `controllers/TermosController.java` (novo) e `config/SecurityConfig.java` (`permitAll` em `GET /Termos/vigente` e `/termos/**`).
- PDF em `src/main/resources/static/termos/termos-de-uso-e-privacidade.pdf`, gerado a partir do mesmo texto do app (`frontendreact/scripts/gerar_pdf_termos.py`). Ao mudar o texto: gerar o PDF de novo e atualizar `TERMS_VERSION`; quem aceitou a versão anterior verá o pedido de novo aceite.

## 9. Testes

Ajustados (as chamadas passaram a levar o `Contexto` com o tipo):
`ViagemPresencaStatusTest`, `ViagemStatusMachineTest`, `SolicitacaoNotificationTest`, `SolicitacaoBidirecionalTest` (texto "Responda em Vínculos.").

Novos:

| Classe | O que verifica |
|---|---|
| `MensagensNotificacaoTest` | Textos de embarque, desembarque (ida/volta), cancelamento, atraso, ausência e dados ausentes |
| `AusenciaServiceTest` | Data fora do intervalo, trecho inválido, sobreposição no mesmo dia, aviso aos outros responsáveis e descrição do dia |
| `PushServiceTest` | Mensagem de alerta (prioridade, canal, dados) e desativação de `DeviceNotRegistered` |
| `NotificacaoServiceTest` (+1) | Deduplicação por evento e gravação de tipo, prioridade e referências |
| `TermosServiceTest` (+1) | Novo aceite quando a versão muda |

Comando: `./mvnw test -Dtest='!BackEndJavaApplicationTests,!SecurityAuthorizationIntegrationTest'` → **137 testes, 0 falhas**.
Os testes de contexto (`BackEndJavaApplicationTests`, `SecurityAuthorizationIntegrationTest`) precisam de PostgreSQL e não rodaram neste ambiente.

## 10. Arquivos

**Novos:** `config/CorrecaoEsquemaBanco.java`, `controllers/TermosController.java`, `DTOs/AusenciaDTO.java`, `DTOs/AvisoAtrasoDTO.java`, `DTOs/DispositivoPushDTO.java`, `DTOs/HistoricoAlunoDTO.java`, `DTOs/ViagemDiaResumoDTO.java`, `entities/AusenciaInformada.java`, `entities/DispositivoPush.java`, `enums/TipoNotificacao.java`, `repositories/AusenciaInformadaRepository.java`, `repositories/DispositivoPushRepository.java`, `services/AusenciaService.java`, `services/HistoricoService.java`, `services/MensagensNotificacao.java`, `services/PushService.java`, `resources/static/termos/termos-de-uso-e-privacidade.pdf` e os testes da seção 9.

**Alterados nesta fase:** `config/AsyncConfig.java`, `config/SecurityConfig.java`, `controllers/AutenticacaoController.java`, `controllers/NotificacaoController.java`, `controllers/ResponsavelController.java`, `controllers/ViagemController.java`, `DTOs/AutenticacaoResponseDTO.java`, `DTOs/NotificacaoDTO.java`, `entities/Notificacao.java`, `repositories/NotificacaoRepository.java`, `services/AutenticacaoService.java`, `services/MotoristaService.java`, `services/NotificacaoService.java`, `services/SolicitacaoService.java`, `services/TermosService.java`, `services/UsuarioService.java`, `services/ViagemService.java`.

## 11. Pendências

- Rodar a API com PostgreSQL e conferir a criação das tabelas e colunas novas.
- Decidir a retenção das notificações (90 dias hoje × 1 ano na especificação).
- Push em aparelho real precisa de *development build* do app e do ID do projeto no EAS.
- Configurar `TERMS_VERSION` no ambiente para o novo aceite funcionar.

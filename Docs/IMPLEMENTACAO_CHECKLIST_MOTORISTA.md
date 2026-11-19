# Checklist de ajustes do motorista: backend (19/11/2026)

Branch `feat/ajustes-checklist-motorista`, criada a partir de `homologacao`. A lista completa de itens e o que mudou no app estão em `docs/IMPLEMENTACAO_CHECKLIST_MOTORISTA.md` no repositório do frontend.

## 1. Vínculo escola + motorista passa a ser opcional

Isto reverte a decisão de 13/11. O vínculo continua existindo, mas deixa de ser exigido.

| Onde | Antes | Agora |
|------|-------|-------|
| `SolicitacaoService.validarAlunoParaViagem` | Recusava o pedido de vaga ou o convite com "O motorista não tem vínculo ativo com a escola X" | Não verifica mais o vínculo |
| `ViagemService.coletarBloqueiosProntidao` | A falta de vínculo bloqueava o início da viagem | Vira **aviso** (`avisos`). A viagem inicia normalmente e o app mostra o aviso em "Para regularizar". |
| `BuscaMotoristaService.buscar` | O responsável só achava motoristas vinculados à escola do aluno | Acha os vinculados **e** os que já têm viagem ativa para a escola. Para isso foi criada a consulta `ViagemRepository.listarIdsMotoristasComViagemParaEscolas`. |

## 2. "Você é o próximo" (notificação `PROXIMO_ALUNO`)

Alternativa à previsão de chegada enquanto o Google Maps não está ligado.

- **Quando dispara:**
  - quando a viagem inicia (`EM_ANDAMENTO`);
  - a cada presença registrada, online (`alterarStatusPresenca`) ou pela fila offline (`sincronizarPresencaOffline`).
- **Regra:** pega a primeira parada da rota atual (já com a ordem que o motorista ajustou) que ainda tem aluno pendente. Ver `ViagemService.proximaParadaPendente`.
  - Na ida, a parada de casa é a busca (aluno `ESPERANDO`).
  - Na volta, a parada de casa é a entrega (aluno `EMBARCADO`).
- **Quem recebe:** se essa parada for a casa de um aluno, os responsáveis de cada aluno pendente ali.
  - Ida: "Carlos está indo buscar Ana agora. Fique atento."
  - Volta: "… está indo deixar Ana agora."
- **Sem repetição:** a chave `proximo:{viagemDiaId}:{dependenteId}` garante que cada aluno recebe o aviso uma vez por viagem.
- **Tipo:** `PROXIMO_ALUNO`, grupo `VIAGEM`, prioridade `ALTA`. É gravado como texto, então não precisa de ajuste no banco.
- **Falhas:** uma falha aqui é ignorada e não desfaz a presença nem o início da viagem.

## 3. Interromper viagem em bancos antigos

O valor `INTERROMPIDA` entrou no enum `StatusViagemDia` em 03/11. As tabelas criadas antes disso têm uma restrição CHECK com os valores antigos, e o `ddl-auto=update` não a recria, então o banco recusava a interrupção.

A classe `CorrecaoEsquemaBanco` agora também remove, na inicialização:
- `viagem_dia_status_check`;
- `historico_viagem_dia_status_anterior_check`;
- `historico_viagem_dia_status_novo_check`.

O comando é idempotente: não faz nada se a restrição não existir.

## Testes

- `ProximaParadaTest` (novo): próxima parada na ida, na volta, antes de sair da escola e sem pendentes, e o texto da notificação.
- `SolicitacaoBidirecionalTest.pedidoEnviadoMesmoSemVinculoDoMotoristaComAEscola`: invertido, agora o pedido é aceito.
- `ViagemDocumentGateTest.iniciaSemVinculoComAEscolaEMostraAviso`: invertido, agora a viagem inicia e a falta de vínculo aparece em `avisos`.
- Suíte: `./mvnw -o test -Dtest='!BackEndJavaApplicationTests,!SecurityAuthorizationIntegrationTest'` com **164 testes passando**.
- Não testado contra um PostgreSQL real.

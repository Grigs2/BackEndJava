# Mensagens rápidas, ordem das paradas e revisão de documentos: backend (22/11/2026)

Branch `feat/mensagens-paradas-documentos`. O que mudou no app está em `docs/IMPLEMENTACAO_MENSAGENS_PARADAS_DOCUMENTOS.md`, no repositório do frontend.

## 1. Ordem das paradas: cada aluno embarca antes de desembarcar

Em `ViagemService.reordenarParadas`, depois das validações que já existiam, a nova sequência passa por `separarDesembarquesAntecipados(sequencia)`:

- **Desembarque na escola antes do embarque de parte dos alunos dela.** Os alunos que ainda vão embarcar saem dessa parada e vão para outro desembarque na mesma escola (mesmo endereço), depois do último embarque deles. Se já existe um desembarque assim, ele é reaproveitado. Se não existe, é criada uma `ViagemParada` nova (mesmo tipo, operação, nome, endereço e validação), **antes da parada final do motorista**. Os participantes são copiados para a parada nova e removidos da antiga (`orphanRemoval`).
- **Desembarque na escola antes de todos os alunos dela:** `BusinessException`, com a mensagem "precisa vir depois do embarque de pelo menos um aluno dessa escola".
- **Na volta, desembarque em casa antes do embarque na escola:** `BusinessException`, com o nome do aluno.
- Alunos com presença `FALTOU` ou `DESEMBARCADO` não contam.

**Gravação:**
- continua em duas etapas, por causa da restrição única (`viagem_dia_id`, `ordem`);
- a parada criada entra no `HistoricoOrdemParada` com `ordemAnterior = 0` (o app mostra "novo desembarque criado na posição N").

**Testes:** `OrdemParadasEscolaTest` (novo) cobre:
- ordem válida;
- desembarque extra criado no fim;
- reaproveitamento de um desembarque que já existe;
- escola antes de todos;
- casa antes da escola na volta;
- aluno que faltou.

## 2. Revisão de documentos com conferências automáticas

`GET /Admin/Documentos/Revisao` (apenas ADMIN) devolve, para cada documento pendente, um `DocumentoRevisaoDTO` com:
- o documento;
- o motorista (id, nome e idade);
- o veículo (placa e modelo);
- as conferências (`ConferenciaDocumentoDTO`: item, situação `OK`/`ATENCAO`/`ERRO` e detalhe);
- `podeAprovar`.

`ConferenciaDocumentoService` (estático, sem banco) confere:

| Documento | Conferência | Situação quando falha |
|---|---|---|
| CNH | Dígitos verificadores do número de registro | ATENCAO |
| CNH | Número diferente da CNH do cadastro | ATENCAO |
| CNH | Categoria D ou E (CTB, art. 138) | ATENCAO |
| CNH | Idade de 21 anos ou mais (CTB, art. 138, I) | ATENCAO |
| Vistoria | Realizada há no máximo 6 meses (CTB, art. 136, II) | ATENCAO |
| Licenciamento | Dígito verificador do RENAVAM | ATENCAO |
| Vistoria e licenciamento | É do veículo atual do motorista | ATENCAO |
| Todos | Validade: vencido ou sem data | **ERRO** (impede aprovar, como antes) |
| Todos | Vence em 30 dias ou menos | ATENCAO |

- Os dígitos verificadores só **avisam**: os números da seed não passam no cálculo, e dígito certo não comprova que o documento existe. A regra de aprovação no `revisar` continua igual.
- `GET /Admin/Documentos/Pendentes` continua igual, porque a seed do Lucas e o painel do administrador usam esse endpoint.

**Testes:** `ConferenciaDocumentoServiceTest` (novo), com números calculados à mão no próprio teste, cobre:
- os dígitos da CNH, inclusive o caso com desconto;
- o dígito do RENAVAM;
- a CNH sem nenhum aviso;
- a CNH com todos os avisos;
- documento vencido;
- vistoria antiga;
- o licenciamento.

## 3. Mensagens rápidas

O envio usa a mensagem manual que já existia. A única mudança é que `MotoristaViagemDTO` (`GET` do motorista da viagem do dia) passou a devolver `motoristaUsuarioId`, para o responsável conseguir mandar mensagem ao motorista.

## Testes

`./mvnw -o test -Dtest='!BackEndJavaApplicationTests,!SecurityAuthorizationIntegrationTest'` com **202 testes passando** (antes eram 189).

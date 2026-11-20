# Localização do veículo e rota com previsão de chegada (19/11/2026)

Junto com o app (veja `docs/COMO_GERAR_APK_E_MAPAS.md` no repositório do frontend), o backend passou a:

1. aceitar a localização do motorista sem depender de todos os responsáveis;
2. calcular a rota e a previsão de chegada com o Google (Geocoding e Routes).

## 1. Localização: nova regra de consentimento

| Antes | Agora |
|---|---|
| Sem `GPS_CONSENT_VERSION`, todo envio de GPS recebia 503 | `application.properties` usa o valor padrão `2026-11`. Trocar o valor pede um novo consentimento de todos. |
| O motorista só enviava a posição se **todos** os alunos da viagem tivessem um responsável que autorizou | O motorista envia com o **próprio** consentimento, desde que o GPS não esteja suspenso |
| Um responsável revogar suspendia o GPS da viagem para todos | Revogar só tira o acesso **daquele** responsável ao mapa. A suspensão continua quando o **motorista** revoga. |
| Não havia como o app saber se o responsável já tinha autorizado | `GET /Viagem/Localizacao/Consentimento/Dependente/{idDependente}` devolve `{ aceito, versaoPolitica, ocorridoEm }` |
| — | Ler a posição (atual ou histórico) exige que o responsável tenha autorizado para ao menos um filho na viagem. Sem isso, a resposta é **409** com a mensagem iniciada por "Autorize…", e o app mostra o botão "Permitir". |

Código:
- `LocalizacaoViagemService`:
  - `exigirGpsAtivo`, `exigirConsentimentoDoLeitor` (novo), `consultarConsentimentoResponsavel` (novo) e `validarLeituraLocalizacao` (novo, usado pela rota);
  - `registrarConsentimentoResponsavel` deixou de suspender a viagem.
- `ViagemController`: novo `GET .../Consentimento/Dependente/{idDependente}`.

## 2. Rota e previsão de chegada

`GET /Viagem/ViagemDia/{idViagemDia}/Rota?latitude=&longitude=`
- A latitude e a longitude são opcionais e vêm do app do motorista.
- Autorização: `podeAcessarExecucao`. O responsável também precisa ter autorizado a localização.

**Como calcula (`RotaViagemService`):**

1. **Paradas pendentes**, na ordem atual da viagem (`gerarParadasViagemDia`). A regra é a mesma da tela de atendimento:
   - parada de embarque: pendente enquanto há aluno `ESPERANDO`;
   - parada de desembarque: pendente enquanto há aluno `ESPERANDO` ou `EMBARCADO`.
2. **Origem da rota**, nesta ordem de preferência:
   1. a posição enviada pelo app do motorista;
   2. a última posição salva (`LocalizacaoViagemAtual`);
   3. para o motorista sem GPS, a casa dele.
3. **Coordenadas das paradas:** o endereço é convertido pelo Google Geocoding uma única vez e guardado na tabela nova `geocodificacao_endereco` (entidade `GeocodificacaoEndereco`). Endereços não encontrados também ficam guardados, para não repetir a consulta.
4. **Rota:** o Google Routes calcula a rota de carro, com trânsito (`TRAFFIC_AWARE`), passando pelas paradas. O resultado fica em memória por **60 segundos** por viagem; isso controla o custo.
5. **Resposta (`RotaViagemDTO`):**
   - **motorista:**
     - todas as paradas pendentes (`ParadaRotaDTO`), cada uma com o tempo acumulado em segundos, os metros e o horário previsto de chegada;
     - o desenho de cada trecho (`polylines`);
     - a próxima manobra (`proximaInstrucao`, `metrosAteInstrucao`);
   - **responsável:** só as paradas dos próprios filhos, cada uma com `dependenteId`, `acao` e um rótulo ("Busca de Lucas em casa", "Ana chega à escola"), e só os trechos até a última delas. **Nunca vão coordenadas de paradas de outras famílias.**
     - Aluno `ESPERANDO`: a parada de embarque (casa na ida, escola na volta).
     - Aluno `EMBARCADO`: a parada de desembarque (escola na ida, casa na volta).
     - Desembarcado ou com falta: não aparece.
6. **Sem a chave ou com o Google fora do ar:** a resposta tem `disponivel: false` e uma `mensagem`, nunca um erro. O app continua mostrando o mapa com o veículo.

**Configuração:**
- `application.properties`: `app.google.maps.api-key=${GOOGLE_MAPS_API_KEY:}`.
- No Codespace: o secret `GOOGLE_MAPS_API_KEY` com a chave **"Tio da Perua - Backend"**, que tem só Geocoding e Routes. A chave nunca é enviada ao app.

**Arquivos novos:**
- `GoogleMapsClient`
- `RotaViagemService`
- `RotaViagemDTO`
- `ParadaRotaDTO`
- `GeocodificacaoEndereco`
- `GeocodificacaoEnderecoRepository`

## Testes

- `RotaViagemServiceTest` (novo), que cobre:
  - o tempo acumulado das paradas para o motorista;
  - o responsável vendo só o filho, sem outras paradas;
  - a previsão até a escola depois do embarque;
  - a resposta sem chave do Google;
  - o responsável sem posição do veículo;
  - a regra de parada pendente.
- `LocalizacaoViagemServiceTest`, com os casos novos:
  - o motorista envia mesmo sem o consentimento dos responsáveis;
  - sem o consentimento do motorista, a posição é recusada;
  - o responsável que autorizou vê a posição;
  - o responsável sem autorização recebe o pedido "Autorize".
- Suíte: `./mvnw -o test -Dtest='!BackEndJavaApplicationTests,!SecurityAuthorizationIntegrationTest'` com **173 testes passando**.
- A chamada à Routes API foi conferida contra o Google de verdade: ela devolve a duração por trecho, a distância, o desenho e as instruções em português.
- O fluxo completo ainda não foi testado com o banco PostgreSQL do Codespace.

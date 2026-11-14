# Resumo do redesenho das telas do motorista

Status: trabalho em andamento, **não commitado**. Branch `homologacao` (back `e1c14c2`, front `0353c04` no momento da puxada).

## 1. Ambiente de teste

- Backend: Spring Boot na porta **8090** (8080 estava ocupada), contexto `/TioDaPerua/api`. Comando: `.\mvnw.cmd spring-boot:run` dentro de `BackEndJava`. Banco PostgreSQL local em `localhost:5432/TioDaPerua`.
- Frontend: Expo na porta **8081**. Comando: `npx.cmd expo start --port 8081` dentro de `frontendreact`. Node 26.13.1 instalado em `C:\Program Files\nodejs`; no PowerShell use `npx.cmd`, pois `npm.ps1` é bloqueado pela política de execução.
- `frontendreact/.env.local` aponta para `EXPO_PUBLIC_API_URL=http://localhost:8090/TioDaPerua/api`. Antes apontava para `192.168.15.9:5000`, por isso os logins de teste falhavam.
- Contas de cenário: senha `TioPerua#Teste2026` (e1 a e4, m1 a m3, r1 a r4). Admin usa a variável `ADMIN_PASSWORD`.

## 2. Escopo removido pelo usuário

- Google Directions (rotas reais), clima, card 3D do veículo.
- Responsável (R1/R2) como tela própria.
- Flyway.

## 3. Backend

- `NotificacaoDTO.toEntity` passou a mapear `dependenteId` e `viagemDiaId`.
- `DependenteParadaDTO` passou a trazer o id do usuário responsável.
- `ViagemDiaDTO` passou a enviar `viagemId` e `periodo` (valores: `MANHA_IDA`, `MANHA_VOLTA`, `TARDE_IDA`, `TARDE_VOLTA`, `NOITE_IDA`, `NOITE_VOLTA`). Sem `periodo` a home mostrava o período errado.
- Testes do backend passaram após as mudanças de notificação.
- Conhecido, não corrigido: `NotificacaoDTO.visto` é `boolean` primitivo, então um campo ausente no JSON gera um 400 enganoso.
- Não verificado: se o "Aviso geral" chega a todos os responsáveis. Também não verificado: `ViagemDia.viagem` é LAZY e `toDTO` depende de open-in-view.

## 4. Frontend: o que foi feito

### Home do motorista (`DriverMainScreen.tsx`)
- Botão "Iniciar rota" removido.
- Card "Em rota": tag "Viagem atual - em rota", título "Rota da manhã (ida)", `x/y embarcados`, "Indo para <rua e número | nome da escola>".
- Botão "Próxima viagem" único, com o texto "Hoje - manhã - volta" abaixo. Prioridade: hoje antes de amanhã; depois MANHA_IDA, TARDE_IDA, MANHA_VOLTA, TARDE_VOLTA. Usa `labelLines={2}`.
- Estilo preto e branco, como os outros botões.
- Os helpers de período foram movidos para `utils/periodo.ts` (`TURNO_LABEL`, `SENTIDO_LABEL`, `rotuloRota`).

### Header e navegação (`AppLayout.tsx`)
- Sino flutuante, sem faixa branca, só quando `showBell`.
- A tela de atendimento não mostra menu inferior nem sino (`TELAS_SEM_NAVEGACAO`).

### Tela de atendimento (`DriverAttendanceScreen.tsx`)
Layout pelo spec: sem scroll, sem menu inferior, sem sino.

- **Mapa pequeno (40%):** chips "Ocultar mapa" e "Tela cheia", pequenos, sem ícone, centralizados na parte inferior do mapa. Botão voltar no canto superior esquerdo, que vai para a home.
- **Região inferior (60%):**
  - Card do aluno com o nome e "Mensagem" pequeno no canto superior direito.
  - Subtítulo "Embarque/Desembarque: rua + número" ou nome da escola.
  - Card de direções: mock estático, fundo escuro, ícone amarelo, "Em 120 m, vire à direita", "Trânsito moderado · 29 °C · chega em 2 min". Sem integração real.
  - Barra de progresso (`x/y embarcados` ou `desembarcados`).
  - Navegação entre alunos com setas, no fim da região, com espaço depois da barra.
  - Botões principais, em uma linha abaixo da navegação: Embarcou/Desembarcou (verde, com ícone à esquerda e borda preta) e Faltou (vermelho, com ícone).
  - Quando todos foram marcados, o botão "Finalizar viagem" aparece no lugar dos botões principais.
  - Legenda "Faltam N alunos para finalizar", compactada para não ser cortada.
- **Mapa oculto:** "Ver mapa" pequeno no canto superior direito, sem ícone. Nome da viagem à esquerda, com a fonte reduzida se for longo. "Aviso geral" e "Mensagem" aparecem só nessa visão e na visão inicial, com "Mensagem" ao lado de "Aviso geral".
- **Tela cheia:** overlay sobre a tela, com o mapa ocupando tudo. Barra superior com seta de voltar (para a home) à esquerda e chip "Reduzir mapa" à direita. Card flutuante branco embaixo com barra de progresso, nome do aluno, endereço e os botões Embarcou/Mensagem/Faltou no estilo do mapa tela cheia, sem ícones. Arrastar o card troca de aluno.
- Navegação por swipe entre alunos funciona na região que não é o mapa, nas visões de mapa oculto e de mapa pequeno.

### Mapa (`TripMap.tsx` e `TripMap.web.tsx`)
- Prop `preencher` adicionada. No `TripMap.web.tsx`, que é o que o celular e o navegador usam, o mapa é um iframe do OpenStreetMap com marcador na posição do veículo. A aparência não é a do mapa nativo.

### Outros
- `ui/Button.tsx` ganhou `labelLines`.
- `utils/paradas.ts` ganhou `enderecoResumido` (texto antes do primeiro " - ").
- `AvisoAtrasoModal.tsx` e `NotificationCreateScreen.tsx` foram ajustados para os novos dados de notificação.

### Validação
- `npx.cmd tsc --noEmit` passou (exit 0) após a última rodada de alterações no frontend.
- Layout real só foi confirmado pelo usuário no celular e pela prévia web. O mapa nativo (`react-native-maps`) não roda no navegador.

## 5. Telas e funcionalidades inacessíveis ou sem destino

- `DriverMapScreen.tsx` (rota "mapa" do M6) era um placeholder. Não é mais acessado pela tela de atendimento, porque "Tela cheia" agora abre o overlay. Continua no código, sem link.
- `DriverFocusScreen.tsx` (M5 antigo) não está linkado em nenhum lugar. Não foi removido.
- "Interromper viagem": não se sabe mais onde fica. Não há botão para ele na tela de atendimento.
- Histórico de eventos: não foi reintegrado às telas novas.
- "Posição do veículo" e "Abrir no mapa": não se decidiu se permanecem.
- Rótulos contextuais "Embarcou"/"Desembarcou" foram implementados, mas não foram confirmados pelo usuário.
- Botão "Mensagem" do card (vai para o responsável do aluno) e "Mensagem" do topo (aviso geral da viagem) são duas ações. O usuário disse que são a mesma coisa; não foram unificadas.
- Sentido de volta: assumiu-se que embarque e desembarque são invertidos em relação à IDA (IDA embarca nos endereços dos alunos e desembarca na escola). Não confirmado com o usuário.

## 6. Observação importante: telas de rotas e viagem

As telas de **rotas, viagens, agenda e itinerários** (`DriverTripsScreen.tsx`) e a **configuração de uma rota/viagem** (`DriverTripDetailsScreen.tsx`) **foram alteradas** neste trabalho, por decisão do usuário, e não foram revisadas uma a uma depois disso:

- `DriverTripsScreen`: botão "Nova viagem" no topo da agenda (no lugar do botão "Nova" do cabeçalho); subtítulo do filtro passou de "Período" para "Sentido da viagem"; cabeçalho sem botão à direita.
- `DriverTripDetailsScreen`: nova seção "Roteiro de hoje" (lista das paradas, com acesso à ordem das paradas); "Preparar viagem de hoje" virou "Iniciar viagem"; novo botão "Enviar aviso" (abre `AvisoAtrasoModal`); "Desativar Viagem" virou "Cancelar viagem", com o diálogo de confirmação ajustado.

## 7. Pendências

- Confirmar no celular: chips "Ocultar mapa" e "Tela cheia" sobre o mapa nativo; legenda "Faltam N alunos" visível; ícones e borda dos botões.
- Decidir os itens da seção 5.
- Sincronizar `Checklist_redesenho_motorista.md` com as rodadas 2 a 11 (o checklist ainda não reflete essas mudanças).
- Commit: aguardando autorização do usuário.

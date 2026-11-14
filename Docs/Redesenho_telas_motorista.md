# Redesenho das telas do Motorista: análise atual x proposta

Referência visual: `figma_proposta_ajuste_telas.jpeg` (seção "06 · Fluxos de Telas").

Legenda das camadas: **Front** (frontendreact), **Back** (BackEndJava), **Banco** (PostgreSQL, schema via `spring.jpa.hibernate.ddl-auto=update`).

---

## 1. Telas do Motorista (M1 a M6)

| Tela | Hoje | Proposto | Front | Back | Banco |
|---|---|---|---|---|---|
| **M1 Início** | Card "Em rota" ou "viagens ativas", lista "Próximas viagens", documentos, atalhos | Card da viagem atual ("Próxima viagem"), card 3D do veículo, atalhos Perfil/Histórico/Alunos/Documentos | `DriverMainScreen.tsx`: novo card de viagem e card do veículo | `GET /Motorista/Veiculo` já existe. Falta dado de modelo/foto para o 3D | Tabela `veiculo`: confirmar campos de modelo/cor. Asset 3D fica fora do banco |
| **M2 Lista de viagens** | Lista + agenda de 7 dias, filtro Ida/Volta/Todos, modal de criar viagem | Chips de data, filtro Ida/Volta, botão "Nova viagem" | `DriverTripsScreen.tsx`: ajustes visuais | `GET /Viagem/ListarViagens` e `/Viagem/Agenda` já atendem | Nenhuma |
| **M3 Detalhe da viagem** | Nome, dependentes, adicionar dependente, desativar. Sem roteiro de paradas | Roteiro de paradas em ordem, "Enviar aviso", "Cancelar viagem", "Iniciar viagem" | `DriverTripDetailsScreen.tsx`: lista de paradas, botões de aviso e cancelamento | Paradas via `BuscarParadasViagemDia`. Aviso por viagem via `Notificacao` (ver decisão de "Mensagem") | `viagem_parada` tem `ordem`, `nome_local`, `endereco`, **sem latitude/longitude** |
| **M4 Em rota (mapa compacto)** | Mapa de 180px no topo, status, progresso, contagem, "Finalizar", "Avisar atraso", "Alterar ordem" | Mapa compacto, card de próxima instrução, contador 0/15 embarcados, botões Embarcou/Faltou, "Alterar viagem" | `DriverAttendanceScreen.tsx`: layout novo com card de instrução e botões grandes | Presença já existe (`AlterarStatusPresenca`). "Alterar viagem" reaproveita `AtualizarNome` e reordenação | Nenhuma para os botões |
| **M5 Modo foco** | Não existe | Sem mapa, instrução grande, "Aviso geral", "Mensagem", "Ver mapa" | Novo modo em `DriverAttendanceScreen.tsx` | "Aviso geral" e "Mensagem" via `Notificacao/Enviar` com `viagemDiaId` (e `dependenteId` para mensagem direcionada) | `notificacao` já tem `tipo`, `prioridade`, `viagem_dia_id`, `dependente_id` |
| **M6 Mapa em tela cheia** | Não existe | Mapa tela cheia com painel flutuante | `TripMap.tsx` e `TripMap.web.tsx`: modo tela cheia e painel | Nenhuma | Depende de coordenadas em `viagem_parada` (ver seção 3) |

### Pontos transversais do Motorista

- **Instrução de navegação com distância** (M4/M5): provedor de rota decidido como **Google Directions**. Exige chave de API, que deve ficar em variável de ambiente no back ou em `.env.local` no front. Nunca no código.
- **Temperatura** (M5): exige API de clima. Não foi decidido o provedor.
- **Mapa e ETA**: sem coordenadas nas paradas não há rota real. Ver seção 3.

---

## 2. Telas do Responsável (R1 e R2)

| Tela | Hoje | Proposto | Front | Back | Banco |
|---|---|---|---|---|---|
| **R1 Motoristas** | Não auditado em detalhe | Lista de motoristas com ocupação `X/Y` | Tela de motoristas do responsável | Endpoint com contagem de ocupação por motorista. Hoje `ViagemController` não expõe isso ao responsável | Consulta agregada sobre `viagem_dependente` e capacidade do veículo. Sem tabela nova |
| **R2 Rastreamento** | `ResponsavelController /Monitorar/{id_dependente}` existe | Chips de dependentes, "Avisar ausência" | Chips novos na tela de monitoramento | `/Responsavel/Ausencias` já cobre "Avisar ausência" | `ausencia_informada` já tem `data`, `trecho`, `motivo`. Sem mudança |

---

## 3. Banco de dados

| Mudança | Motivo | Observação |
|---|---|---|
| Adicionar `latitude` e `longitude` em `viagem_parada` | Mapa, rota e ETA (M3, M4, M6) | Decisão tomada: **sim**. Entidade `ViagemParada.java` |
| Campos de modelo/cor/foto em `veiculo` (se não existirem) | Card 3D do veículo (M1) | Confirmar campos atuais em `Veiculo.java` |
| Nenhuma tabela de mensagem/chat | "Mensagem" (M5) | Decisão tomada: usar notificação direcionada (`Notificacao`) |

Como o schema é gerenciado por `ddl-auto=update`, as colunas novas são criadas no startup. **Não há migrations**, o que é um risco para homologação. Vale avaliar Flyway antes de mudar o banco de forma mais pesada.

---

## 4. Resumo por camada

- **Front**: a maior parte do trabalho. Layout de M1, M3, M4, M5, M6 e chips de R2. Modos de mapa não têm reaproveitamento de backend.
- **Back**: endpoint de ocupação para R1 (`X/Y`). Aviso por viagem via `Notificacao`. Serviço de rota (Google Directions) e, se confirmado, de clima.
- **Banco**: `latitude`/`longitude` em `viagem_parada`. Possivelmente campos de veículo.

---

## 5. Decisões tomadas

| Item | Decisão |
|---|---|
| "Mensagem" (M5) | Notificação direcionada, reaproveitando `Notificacao`. Sem chat |
| Coordenadas em `viagem_parada` | Adicionar `latitude` e `longitude` |
| Provedor de rota | Google Directions |

## 6. Fora de escopo por decisão

- Google Directions e instruções de navegação por enquanto.
- Temperatura (clima).
- Responsável (R1 e R2).
- Card 3D do veículo.
- Flyway: schema segue com `ddl-auto=update`.

## 7. Próximos passos

- Fluxo de telas hoje x proposto: [Fluxo_telas_motorista.md](./Fluxo_telas_motorista.md).
- Perguntas de validação de fluxo pendentes (atalhos da Home, etapa "Preparar o dia", ordem das paradas).

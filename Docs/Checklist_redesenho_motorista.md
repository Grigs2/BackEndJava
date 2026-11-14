# Checklist de implementação: Redesenho das telas do Motorista

Base: [Redesenho_telas_motorista.md](./Redesenho_telas_motorista.md) e [Fluxo_telas_motorista.md](./Fluxo_telas_motorista.md).

Legenda: ⏸️ stand by (fora desta entrega) · ❓ pendente de decisão · 📝 relatado, sem ação agora

---

## Fora de escopo
- [ ] Responsável (R1 e R2)
- [ ] Card 3D do veículo
- [ ] Clima (temperatura)
- [ ] Google Directions e distância nas instruções
- [ ] Flyway (schema segue com `ddl-auto=update`)
- [ ] Telas fora do mockup sem mudança: Detalhe do aluno, Histórico de execução, Ordem das paradas (só muda o ponto de entrada)

## Mapa (básico, para validação visual)
- [x] Mapa ocupando 40% no topo da viagem atual (M4), usando `TripMap` existente
- [x] Botão "Tela cheia" na viagem atual, abrindo a tela de mapa
- [ ] Tela de mapa em tela cheia (M6) com o mapa básico
- [ ] Mapa sem detalhes nesta entrega: sem rota, sem instruções por distância e sem marcadores de parada enquanto a fonte de coordenadas não for definida
- [ ] Detalhes do mapa (rota, marcadores, painel flutuante) ficam em stand by para próxima entrega

## Banco de dados
- [ ] Adicionar `latitude` e `longitude` (nullable, `BigDecimal`) em `ViagemParada.java`
- [ ] Incluir os campos no DTO de paradas
- [ ] ❓ Definir a fonte das coordenadas das paradas (geocodificar endereço ou marcar ponto manualmente). Adiado com os detalhes do mapa, então entra em stand by nesta entrega

## Back-end
- [ ] Preencher latitude e longitude ao criar ou alterar paradas (depende da fonte acima)
- [ ] Confirmar que "Enviar aviso" (M3) e "Aviso geral" (M5) usam `ViagemDia/{id}/Atraso`, sem endpoint novo
- [ ] Confirmar que "Mensagem" (M5) usa `Notificacao/Enviar` com `dependenteId` e `viagemDiaId`, indo só para o responsável do aluno da tela
- [ ] Embarcou e Faltou seguem em `AlterarStatusPresenca`, sem mudança
- [ ] "Alterar viagem" segue em `AtualizarNome` e reordenação, sem mudança
- [ ] Testes de backend para as mudanças de parada

## Front-end

**Início (M1)**
- [x] Reduzir os atalhos de 9 para 4: Perfil, Histórico, Alunos, Documentos
- [x] Manter o sino de notificações no cabeçalho
- [x] Card "Próxima viagem" com a viagem atual

**Lista de viagens (M2)**
- [x] Chips de data
- [x] Filtro Ida/Volta
- [x] Botão "Nova viagem" (modal já existe, ajustar visual e posição)
- [x] Menu inferior continua apontando para Rotas

**Detalhe da viagem (M3)**
- [x] Seção de roteiro com paradas em ordem
- [x] Tocar no card do roteiro abre a reordenação (antes de iniciar)
- [x] Botão "Enviar aviso" (chama o atraso)
- [x] Botão "Cancelar viagem"
- [x] Botão "Iniciar viagem" (abre "Preparar o dia")
- [x] Manter "Adicionar dependente"

**Preparar o dia (DriverTripDay)**
- [x] Manter a tela como está (configuração antes da viagem)
- [x] Ajustar só a entrada, que vem do "Iniciar viagem" do M3

**Viagem atual (M4, tela DriverAttendance)**
- [x] Sem rolagem, sem menu inferior e sem sino
- [x] Mapa ocupa 40% da altura, com "Ocultar mapa", "Tela cheia" e voltar para o início
- [x] 60%: botões "Aviso geral" e "Mensagem" no topo
- [x] Card do aluno com "Mensagem ao responsável", linha "Embarque/Desembarque: <endereço resumido ou nome da escola>"
- [x] Barra de progresso "x/y embarcados" (ida no embarque, volta no desembarque) ou "x/y desembarcados"
- [x] "Alterar viagem" e setas para navegar entre destinos (abre no primeiro destino pendente; também por deslize)
- [x] Botões "Embarcou/Desembarcou" e "Faltou"; "Finalizar viagem" substitui os dois quando todos os alunos foram marcados
- [x] Regra de progresso assumida: ida embarca na casa do aluno e desembarca na escola; volta é o contrário. Confirmar com o usuário
- [ ] Interromper viagem sem lugar visível (decisão pendente)
- [ ] Histórico de eventos e detalhe do aluno removidos desta tela. Decidir se voltam

**Modo foco (M5) e Mapa (M6)**
- [ ] Modo foco não é mais tela separada: o conteúdo da M5 foi absorvido pela tela de viagem atual. Rotas `DriverFocus` e `DriverMap` seguem no código, para revisar
- [x] "Tela cheia" abre o mapa básico (M6)

**Navegação**
- [x] Verificado no código: nenhuma tela ficou inalcançável. Vínculos segue na aba inferior; Veículo e Escolas seguem em Perfil ("Meu veículo", "Minhas escolas"). Só saíram dos atalhos da Início.

## Validação
- [ ] Type-check e build do front sem erros
- [ ] Testes de backend passando
- [ ] Teste manual do fluxo completo com o login m1@cenario.local
- [ ] Atualizar os documentos em `BackEndJava/Docs/`

## 📝 Relatado, sem ação agora
- Seção "Cadastros" em Perfil para concentrar Veículo, Escolas e Vínculos. Fica registrado como próximo passo, sem implementação nesta entrega.

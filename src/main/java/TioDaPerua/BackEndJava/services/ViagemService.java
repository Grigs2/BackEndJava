package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.*;
import TioDaPerua.BackEndJava.entities.*;
import TioDaPerua.BackEndJava.enums.*;
import TioDaPerua.BackEndJava.repositories.*;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.HistoricoViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;
import java.util.stream.Collectors;

import TioDaPerua.BackEndJava.config.DataBrasilia;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViagemService {

    private static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private ViagemRepository viagemRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private ViagemDependenteRepository viagemDependenteRepository;

    @Autowired
    private ViagemDiaRepository viagemDiaRepository;

    @Autowired
    private ViagemPresencaRepository viagemPresencaRepository;

    @Autowired
    private EnderecoDependenteRepository enderecoDependenteRepository;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private DocumentoMotoristaService documentoMotoristaService;

    @Autowired
    private ViagemEscolaRepository viagemEscolaRepository;

    @Autowired
    private HistoricoViagemDiaRepository historicoViagemDiaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ViagemParadaRepository viagemParadaRepository;

    @Autowired
    private HistoricoOrdemParadaRepository historicoOrdemParadaRepository;

    @Autowired
    private HistoricoViagemPresencaRepository historicoViagemPresencaRepository;

    @Autowired
    private LocalizacaoViagemAtualRepository localizacaoViagemAtualRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private EscolaRepository escolaRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private AusenciaInformadaRepository ausenciaInformadaRepository;

    /** Dias da agenda são calendário de Brasília, independentemente do fuso do servidor. */
    private Clock relogio = Clock.system(FUSO_BRASILIA);

    @Transactional(readOnly = true)
    public List<AgendaViagemDTO> agendaMotorista(Long idMotorista, LocalDate de, LocalDate ate) {
        LocalDate inicio = de == null ? LocalDate.now(relogio) : de;
        LocalDate fim = ate == null ? inicio.plusDays(6) : ate;
        if (fim.isBefore(inicio)) {
            throw new BusinessException("Intervalo inválido: 'ate' não pode ser anterior a 'de'");
        }
        List<Viagem> viagens = viagemRepository.findAtivasByMotoristaId(idMotorista);
        Map<String, ViagemDia> execucoes = viagemDiaRepository
                .findByMotoristaIdAndDataBetween(idMotorista, inicio, fim).stream()
                .collect(Collectors.toMap(
                        viagemDia -> chaveAgenda(viagemDia.getViagem().getId(), viagemDia.getData()),
                        viagemDia -> viagemDia));

        List<AgendaViagemDTO> agenda = new ArrayList<>();
        for (Viagem viagem : viagens) {
            for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
                ViagemDia execucao = execucoes.get(chaveAgenda(viagem.getId(), data));
                agenda.add(new AgendaViagemDTO(
                        viagem.getId(),
                        viagem.getPeriodo(),
                        data,
                        execucao == null ? null : execucao.getId(),
                        execucao == null ? null : execucao.getStatus(),
                        execucao != null,
                        HorarioPartida.formatar(HorarioPartida.resolver(viagem, execucao))));
            }
        }
        return agenda;
    }

    private String chaveAgenda(Long viagemId, LocalDate data) {
        return viagemId + ":" + data;
    }

    @Transactional(readOnly = true)
    public ProntidaoViagemDTO consultarProntidao(Long idViagem) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        List<String> avisos = new ArrayList<>();
        List<String> bloqueios = coletarBloqueiosProntidao(viagem, avisos);
        return new ProntidaoViagemDTO(idViagem, bloqueios.isEmpty(), bloqueios, avisos.stream().distinct().toList());
    }

    /**
     * Pendências que impedem iniciar a viagem. Documentos do motorista não bloqueiam (ainda não há quem os valide)
     * e entram em {@code avisos}. O vínculo ativo do motorista com a escola continua obrigatório.
     */
    private List<String> coletarBloqueiosProntidao(Viagem viagem, List<String> avisos) {
        List<String> bloqueios = new ArrayList<>();
        if (!Boolean.TRUE.equals(viagem.getAtivo())) {
            bloqueios.add("O itinerário está desativado");
        }

        Motorista motorista = viagem.getMotorista();
        if (motorista == null) {
            bloqueios.add("O itinerário não possui motorista");
            return bloqueios;
        }
        if (motorista.getVeiculo() == null) {
            bloqueios.add("Motorista deve possuir um veículo cadastrado");
        } else if (motorista.getVeiculo().getCapacidade() <= 0) {
            bloqueios.add("A capacidade do veículo deve ser maior que zero");
        }
        if (motorista.getUsuario() == null
                || motorista.getUsuario().getEnderecoOperacional() == null
                || motorista.getUsuario().getEnderecoOperacional().getStatusValidacao()
                != StatusValidacaoEndereco.VALIDADO) {
            bloqueios.add("Endereço estruturado do motorista ausente ou sem CEP validado");
        }
        avisos.addAll(documentoMotoristaService.listarPendenciasParaInicio(motorista.getId()));

        List<ViagemDependente> dependentes = viagemDependenteRepository.buscarDependentesCompletos(viagem.getId());
        if (dependentes.isEmpty()) {
            bloqueios.add("O itinerário não possui dependentes ativos");
        }
        if (motorista.getVeiculo() != null
                && dependentes.size() > motorista.getVeiculo().getCapacidade()) {
            bloqueios.add("A quantidade de dependentes excede a capacidade do veículo");
        }

        List<Long> dependenteIds = dependentes.stream()
                .map(vinculo -> vinculo.getDependente().getId())
                .toList();
        Map<Long, List<EnderecoDependente>> enderecosPorDependente = dependenteIds.isEmpty()
                ? Map.of()
                : enderecoDependenteRepository.findByDependenteIdInAndAtivoTrue(dependenteIds).stream()
                        .collect(Collectors.groupingBy(endereco -> endereco.getDependente().getId()));
        boolean ida = isViagemIda(viagem.getPeriodo());
        Set<Long> escolaIds = dependentes.stream()
                .map(vinculo -> vinculo.getDependente().getEscola())
                .filter(Objects::nonNull)
                .map(Escola::getId)
                .collect(Collectors.toSet());
        Set<Long> escolasComVinculoAtivo = escolaIds.isEmpty()
                ? Set.of()
                : new HashSet<>(viagemEscolaRepository.listarIdsEscolasAtivasDoMotorista(
                        motorista.getId(), escolaIds, StatusVinculoEscola.ATIVO));
        Set<Long> escolasVerificadas = new HashSet<>();

        for (ViagemDependente vinculo : dependentes) {
            Dependente dependente = vinculo.getDependente();
            Escola escola = dependente.getEscola();
            if (escola == null) {
                bloqueios.add("Dependente " + dependente.getId() + " não possui escola vinculada");
            } else {
                if (escolasVerificadas.add(escola.getId())) {
                    if (escola.getUsuario() == null
                            || escola.getUsuario().getEnderecoOperacional() == null
                            || escola.getUsuario().getEnderecoOperacional().getStatusValidacao()
                            != StatusValidacaoEndereco.VALIDADO) {
                        bloqueios.add("Endereço estruturado da escola " + escola.getId()
                                + " ausente ou sem CEP validado");
                    }
                    if (!escolasComVinculoAtivo.contains(escola.getId())) {
                        // Vínculo com a escola é opcional: só avisa, não impede iniciar.
                        avisos.add("Não há vínculo ativo com a escola " + (escola.getNome() != null ? escola.getNome() : escola.getId()));
                    }
                }

                FinalidadeEndereco finalidade = ida
                        ? FinalidadeEndereco.EMBARQUE : FinalidadeEndereco.DESEMBARQUE;
                EnderecoDependente endereco = selecionarEndereco(
                        enderecosPorDependente.getOrDefault(dependente.getId(), List.of()), finalidade);
                if (endereco == null) {
                    bloqueios.add("Dependente " + dependente.getId()
                            + " não possui endereço estruturado para " + finalidade.name().toLowerCase());
                } else if (endereco.getStatusValidacao() != StatusValidacaoEndereco.VALIDADO) {
                    bloqueios.add("CEP do endereço do dependente " + dependente.getId() + " não está validado");
                }
            }
        }
        return bloqueios.stream().distinct().toList();
    }

    private EnderecoDependente selecionarEndereco(List<EnderecoDependente> enderecos,
                                                 FinalidadeEndereco finalidade) {
        return enderecos.stream()
                .filter(endereco -> endereco.getFinalidade() == finalidade
                        || endereco.getFinalidade() == FinalidadeEndereco.AMBOS)
                .findFirst()
                .orElse(null);
    }

    public List<Viagem> listarViagensAtivas(Long idMotorista) {
        return viagemRepository.findAtivasByMotoristaId(idMotorista);
    }

    public List<ViagemDiaDTO> historicoViagens(Long idMotorista) {
        return viagemDiaRepository.findByMotoristaId(idMotorista).stream()
                .map(ViagemDiaDTO::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void desativarViagem(Long idViagem) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));

        if (!viagem.getAtivo()) {
            throw new ConflictException("Viagem já está desativada");
        }

        viagem.setAtivo(false);
        viagemRepository.save(viagem);

        viagemDependenteRepository.desativarPorViagemId(idViagem);
        solicitacaoRepository.encerrarPorViagemId(idViagem, DataBrasilia.agora());
    }

    public MotoristaViagemDTO consultarMotorista(Long idViagemDia) {
        ViagemDia vd = viagemDiaRepository.findById(idViagemDia)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia));

        Viagem viagem = vd.getViagem();
        if (viagem == null) throw new BusinessException("Execução de viagem sem vínculo com itinerário");

        Motorista motorista = viagem.getMotorista();
        if (motorista == null) throw new BusinessException("Viagem sem motorista vinculado");

        Veiculo veiculo = motorista.getVeiculo();

        return new MotoristaViagemDTO(
                motorista.getId(),
                motorista.getNome(),
                motorista.getUsuario() != null ? motorista.getUsuario().getTelefone() : null,
                veiculo != null ? veiculo.getModelo() : null,
                veiculo != null ? veiculo.getPlaca() : null,
                veiculo != null ? veiculo.getCor() : "N/A",
                motorista.getUsuario() != null ? motorista.getUsuario().getId() : null
        );
    }

    @Transactional(readOnly = true)
    public List<ParadaDTO> gerarParadasViagemDia(Long idViagemDia) {
        ViagemDia viagemDia = viagemDiaRepository.findById(idViagemDia)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia));

        List<ViagemParada> paradasPersistidas = viagemParadaRepository.listarPorViagemDia(idViagemDia);
        if (!paradasPersistidas.isEmpty()) {
            return mapearParadas(paradasPersistidas);
        }
        return gerarParadasProjetadas(viagemDia);
    }

    private List<ParadaDTO> gerarParadasProjetadas(ViagemDia viagemDia) {
        Viagem viagem = viagemDia.getViagem();
        if (viagem == null) return new ArrayList<>();

        List<ViagemDependente> vinculados = viagemDependenteRepository.buscarDependentesCompletos(viagem.getId());

        boolean ida = isViagemIda(viagem.getPeriodo());
        Map<Long, ViagemPresenca> presencasPorDependente = viagemPresencaRepository
                .findByViagemDiaId(viagemDia.getId()).stream()
                .collect(Collectors.toMap(presenca -> presenca.getDependente().getId(), presenca -> presenca));
        Map<Long, Dependente> dependentesPorId = vinculados.stream()
                .map(ViagemDependente::getDependente)
                .collect(Collectors.toMap(Dependente::getId, dependente -> dependente));

        List<ParadaDTO> paradas = new ArrayList<>();
        int ordem = 1;

        paradas.add(criarParadaMotorista(ordem++, viagem.getMotorista(), "Saída: Casa do Motorista"));

        if (ida) {
            List<ParadaDTO> paradasResp = adicionarParadasResponsaveis(
                    ordem, vinculados, viagemDia.getId(), ida, presencasPorDependente);
            paradas.addAll(paradasResp);
            ordem = paradas.size() + 1;
            List<ParadaDTO> paradasEscola = adicionarParadasEscolas(
                    ordem, vinculados, viagemDia.getId(), presencasPorDependente);
            paradas.addAll(paradasEscola);
        } else {
            List<ParadaDTO> paradasEscola = adicionarParadasEscolas(
                    ordem, vinculados, viagemDia.getId(), presencasPorDependente);
            paradas.addAll(paradasEscola);
            ordem = paradas.size() + 1;
            List<ParadaDTO> paradasResp = adicionarParadasResponsaveis(
                    ordem, vinculados, viagemDia.getId(), ida, presencasPorDependente);
            paradas.addAll(paradasResp);
        }

        ordem = paradas.size() + 1;
        paradas.add(criarParadaMotorista(ordem, viagem.getMotorista(), "Retorno: Casa do Motorista"));

        return paradas;
    }

    private boolean isViagemIda(Periodo periodo) {
        return periodo == Periodo.MANHA_IDA || periodo == Periodo.TARDE_IDA || periodo == Periodo.NOITE_IDA;
    }

    private ParadaDTO criarParadaMotorista(int ordem, Motorista motorista, String label) {
        String endereco = motorista != null && motorista.getUsuario() != null
                && motorista.getUsuario().getEnderecoOperacional() != null
                ? motorista.getUsuario().getEnderecoOperacional().formatar() : null;
        return new ParadaDTO(ordem, TipoParada.MOTORISTA, label, endereco, new ArrayList<>());
    }

    private List<ParadaDTO> adicionarParadasResponsaveis(
            int ordemInicial,
            List<ViagemDependente> vinculados,
            Long idViagemDia,
            boolean ida,
            Map<Long, ViagemPresenca> presencasPorDependente) {
        Map<String, List<DependenteParadaDTO>> agrupamento = new LinkedHashMap<>();
        Map<String, String> nomesLocais = new HashMap<>();

        for (ViagemDependente vd : vinculados) {
            var dep = vd.getDependente();
            ViagemPresenca presenca = presencasPorDependente.get(dep.getId());
            Responsavel resp = responsavelPrincipal(dep);
            if (possuiSnapshotDeEndereco(presenca, ida)) {
                String endereco = enderecoDaParadaResponsavel(presenca, ida);
                String statusEmbarque = presenca != null && presenca.getStatus() != null
                        ? presenca.getStatus().name() : "N/A";

                agrupamento.computeIfAbsent(endereco, k -> new ArrayList<>())
                        .add(new DependenteParadaDTO(
                                dep.getId(), dep.getNome(), resp == null ? "N/A" : resp.getNome(), statusEmbarque,
                                usuarioIdDo(resp)));
                nomesLocais.putIfAbsent(endereco, enderecoNome(dep, presenca, ida, resp));
            }
        }

        List<ParadaDTO> paradas = new ArrayList<>();
        int ordem = ordemInicial;
        for (Map.Entry<String, List<DependenteParadaDTO>> entry : agrupamento.entrySet()) {
            paradas.add(new ParadaDTO(ordem++, TipoParada.RESPONSAVEL, nomesLocais.get(entry.getKey()), entry.getKey(), entry.getValue()));
        }
        return paradas;
    }

    private List<ParadaDTO> adicionarParadasEscolas(
            int ordemInicial,
            List<ViagemDependente> vinculados,
            Long idViagemDia,
            Map<Long, ViagemPresenca> presencasPorDependente) {
        Map<String, List<DependenteParadaDTO>> agrupamento = new LinkedHashMap<>();
        Map<String, String> nomesLocais = new HashMap<>();

        for (ViagemDependente vd : vinculados) {
            var dep = vd.getDependente();
            var escola = dep.getEscola();
            if (escola != null && escola.getUsuario() != null
                    && escola.getUsuario().getEnderecoOperacional() != null) {
                String endereco = escola.getUsuario().getEnderecoOperacional().formatar();
                String nomeResp = dep.getResponsaveis() != null && !dep.getResponsaveis().isEmpty()
                        ? responsavelPrincipal(dep).getNome() : "N/A";
                ViagemPresenca presenca = presencasPorDependente.get(dep.getId());
                String statusEmbarque = presenca != null && presenca.getStatus() != null
                        ? presenca.getStatus().name() : "N/A";

                agrupamento.computeIfAbsent(endereco, k -> new ArrayList<>())
                        .add(new DependenteParadaDTO(dep.getId(), dep.getNome(), nomeResp, statusEmbarque,
                                usuarioIdDo(responsavelPrincipal(dep))));
                nomesLocais.putIfAbsent(endereco, "Escola: " + escola.getNome());
            }
        }

        List<ParadaDTO> paradas = new ArrayList<>();
        int ordem = ordemInicial;
        for (Map.Entry<String, List<DependenteParadaDTO>> entry : agrupamento.entrySet()) {
            paradas.add(new ParadaDTO(ordem++, TipoParada.ESCOLA, nomesLocais.get(entry.getKey()), entry.getKey(), entry.getValue()));
        }
        return paradas;
    }

    @Transactional
    public ViagemDia iniciarViagemDia(Long idViagem, Long atorUsuarioId) {
        return prepararViagemDia(idViagem, LocalDate.now(relogio), atorUsuarioId);
    }

    @Transactional
    public ViagemDia prepararViagemDia(Long idViagem, LocalDate data, Long atorUsuarioId) {
        if (idViagem == null || data == null || atorUsuarioId == null) {
            throw new IllegalArgumentException("Viagem, data e usuário responsável pela preparação são obrigatórios");
        }
        LocalDate hoje = LocalDate.now(relogio);
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(6))) {
            throw new BusinessException("A preparação deve ocorrer entre hoje e os próximos 6 dias");
        }

        Viagem viagem = viagemRepository.findByIdForUpdate(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        if (!Boolean.TRUE.equals(viagem.getAtivo())) {
            throw new ConflictException("Não é possível preparar uma execução de itinerário desativado");
        }
        validarAtorMotorista(viagem, atorUsuarioId);

        Optional<ViagemDia> existente = viagemDiaRepository.findByViagemIdAndData(idViagem, data);
        if (existente.isPresent()) return existente.get();

        ViagemDia viagemDia = new ViagemDia();
        viagemDia.setViagem(viagem);
        viagemDia.setData(data);
        viagemDia.setStatus(StatusViagemDia.PLANEJADA);
        viagemDia.setDataUltimaAlteracaoStatus(DataBrasilia.agora());
        viagemDia = viagemDiaRepository.save(viagemDia);
        registrarHistoricoViagemDia(viagemDia, null, StatusViagemDia.PLANEJADA, atorUsuarioId, null);

        List<ViagemDependente> ativos = viagemDependenteRepository.buscarDependentesCompletos(idViagem);
        List<Long> dependenteIds = ativos.stream()
                .map(vinculo -> vinculo.getDependente().getId())
                .toList();
        Map<Long, List<EnderecoDependente>> enderecosPorDependente = dependenteIds.isEmpty()
                ? Map.of()
                : enderecoDependenteRepository.findByDependenteIdInAndAtivoTrue(dependenteIds).stream()
                        .collect(Collectors.groupingBy(endereco -> endereco.getDependente().getId()));

        for (ViagemDependente vd : ativos) {
            ViagemPresenca presenca = new ViagemPresenca();
            presenca.setViagemDia(viagemDia);
            presenca.setDependente(vd.getDependente());
            presenca.setStatus(StatusViagemPresenca.ESPERANDO);
            preencherSnapshots(presenca, vd.getDependente(),
                    enderecosPorDependente.getOrDefault(vd.getDependente().getId(), List.of()));
            viagemPresencaRepository.save(presenca);
        }

        viagemPresencaRepository.flush();
        aplicarAusenciasInformadas(viagemDia, dependenteIds);
        materializarParadas(viagemDia, gerarParadasProjetadas(viagemDia), ativos, enderecosPorDependente);
        return viagemDia;
    }

    /**
     * Sincroniza as execuções ainda PLANEJADAS (de hoje em diante, em Brasília) com os vínculos ativos da viagem:
     * alunos aceitos depois da preparação ganham presença e as paradas são reconstruídas. Execuções já iniciadas
     * ou encerradas mantêm o snapshot original.
     */
    @Transactional
    public void incluirDependenteEmExecucoesPlanejadas(Long idViagem, Long idDependente) {
        List<ViagemDia> execucoes = viagemDiaRepository.findByViagemIdAndStatusAndDataGreaterThanEqual(
                idViagem, StatusViagemDia.PLANEJADA, LocalDate.now(relogio));
        if (execucoes.isEmpty()) return;

        List<ViagemDependente> vinculados = viagemDependenteRepository.buscarDependentesCompletos(idViagem);
        if (vinculados.stream().noneMatch(v -> v.getDependente().getId().equals(idDependente))) return;
        Map<Long, List<EnderecoDependente>> enderecosPorDependente = enderecoDependenteRepository
                .findByDependenteIdInAndAtivoTrue(vinculados.stream().map(v -> v.getDependente().getId()).toList())
                .stream()
                .collect(Collectors.groupingBy(endereco -> endereco.getDependente().getId()));

        for (ViagemDia execucao : execucoes) {
            Set<Long> comPresenca = viagemPresencaRepository.findByViagemDiaId(execucao.getId()).stream()
                    .map(presenca -> presenca.getDependente().getId())
                    .collect(Collectors.toSet());
            List<ViagemDependente> faltantes = vinculados.stream()
                    .filter(v -> !comPresenca.contains(v.getDependente().getId()))
                    .toList();
            if (faltantes.isEmpty()) continue;

            if (!historicoOrdemParadaRepository.findByViagemDia_IdOrderByOcorridoEmAscIdAsc(execucao.getId()).isEmpty()) {
                throw new ConflictException("A rota de " + execucao.getData()
                        + " já foi reordenada; não é possível incluir o aluno automaticamente nesta execução");
            }

            List<Long> novosIds = new ArrayList<>();
            for (ViagemDependente vd : faltantes) {
                Dependente dependente = vd.getDependente();
                ViagemPresenca presenca = new ViagemPresenca();
                presenca.setViagemDia(execucao);
                presenca.setDependente(dependente);
                presenca.setStatus(StatusViagemPresenca.ESPERANDO);
                preencherSnapshots(presenca, dependente,
                        enderecosPorDependente.getOrDefault(dependente.getId(), List.of()));
                viagemPresencaRepository.save(presenca);
                novosIds.add(dependente.getId());
            }
            viagemPresencaRepository.flush();
            aplicarAusenciasInformadas(execucao, novosIds);

            viagemParadaRepository.deleteAll(viagemParadaRepository.listarPorViagemDia(execucao.getId()));
            viagemParadaRepository.flush();
            materializarParadas(execucao, gerarParadasProjetadas(execucao), vinculados, enderecosPorDependente);
        }
    }

    /**
     * Alunos com ausência avisada pelo responsável para a data já entram como FALTOU (o histórico registra o
     * responsável como autor). O motorista já foi avisado quando a ausência foi registrada.
     */
    private void aplicarAusenciasInformadas(ViagemDia viagemDia, List<Long> dependenteIds) {
        if (dependenteIds.isEmpty() || ausenciaInformadaRepository == null) return;
        Periodo periodo = viagemDia.getViagem() == null ? null : viagemDia.getViagem().getPeriodo();
        Map<Long, AusenciaInformada> ausencias = new HashMap<>();
        for (AusenciaInformada a : ausenciaInformadaRepository.findByDependenteIdInAndData(dependenteIds, viagemDia.getData())) {
            if (a.valePara(periodo)) ausencias.putIfAbsent(a.getDependente().getId(), a);
        }
        if (ausencias.isEmpty()) return;
        for (ViagemPresenca presenca : viagemPresencaRepository.findByViagemDiaId(viagemDia.getId())) {
            AusenciaInformada a = ausencias.get(presenca.getDependente().getId());
            if (a == null || presenca.getStatus() != StatusViagemPresenca.ESPERANDO) continue;
            marcarFaltaPorAusencia(presenca, a.getInformadaPor().getId());
        }
    }

    /** Marca FALTOU por ausência avisada (sem notificar de novo os responsáveis, que foram os autores). */
    void marcarFaltaPorAusencia(ViagemPresenca presenca, Long responsavelUsuarioId) {
        presenca.setStatus(StatusViagemPresenca.FALTOU);
        viagemPresencaRepository.save(presenca);
        registrarHistoricoViagemPresenca(presenca, StatusViagemPresenca.ESPERANDO, StatusViagemPresenca.FALTOU,
                responsavelUsuarioId, null, null, Instant.now());
    }

    private void materializarParadas(ViagemDia viagemDia,
                                     List<ParadaDTO> paradas,
                                     List<ViagemDependente> dependentes,
                                     Map<Long, List<EnderecoDependente>> enderecosPorDependente) {
        Map<Long, ViagemPresenca> presencasPorDependente = viagemPresencaRepository
                .findByViagemDiaId(viagemDia.getId()).stream()
                .collect(Collectors.toMap(presenca -> presenca.getDependente().getId(), presenca -> presenca));
        Map<Long, Dependente> dependentesPorId = dependentes.stream()
                .map(ViagemDependente::getDependente)
                .collect(Collectors.toMap(Dependente::getId, dependente -> dependente));
        boolean ida = isViagemIda(viagemDia.getViagem().getPeriodo());
        List<ViagemParada> entidades = new ArrayList<>();

        for (ParadaDTO dto : paradas) {
            ViagemParada parada = new ViagemParada();
            parada.setViagemDia(viagemDia);
            parada.setOrdem(dto.ordem());
            parada.setTipoParada(dto.tipoParada());
            TipoOperacaoParada operacao = dto.tipoParada() == TipoParada.MOTORISTA
                    ? TipoOperacaoParada.EXTREMO
                    : dto.tipoParada() == TipoParada.RESPONSAVEL
                    ? (ida ? TipoOperacaoParada.EMBARQUE : TipoOperacaoParada.DESEMBARQUE)
                    : (ida ? TipoOperacaoParada.DESEMBARQUE : TipoOperacaoParada.EMBARQUE);
            parada.setOperacao(operacao);
            parada.setNomeLocal(dto.nomeLocal());
            parada.setEndereco(dto.endereco());

            if (dto.listaDependentes() != null) {
                for (DependenteParadaDTO participanteDTO : dto.listaDependentes()) {
                    ViagemPresenca presenca = presencasPorDependente.get(participanteDTO.id());
                    if (presenca == null) {
                        throw new BusinessException("Participante da parada não possui presença na execução");
                    }
                    ViagemParadaParticipante participante = new ViagemParadaParticipante();
                    participante.setParada(parada);
                    participante.setPresenca(presenca);
                    participante.setDependenteIdSnapshot(participanteDTO.id());
                    participante.setNomeDependenteSnapshot(participanteDTO.nomeDependente());
                    participante.setNomeResponsavelSnapshot(participanteDTO.nomeResponsavel());
                    parada.getParticipantes().add(participante);
                }
            }
            parada.setStatusValidacaoEndereco(statusEnderecoParada(
                    parada, dependentesPorId, enderecosPorDependente, ida));
            entidades.add(parada);
        }
        viagemParadaRepository.saveAll(entidades);
        viagemParadaRepository.flush();
    }

    @Transactional
    public List<ParadaDTO> reordenarParadas(Long idViagemDia, List<Long> idsParadas, Long atorUsuarioId) {
        if (idViagemDia == null || idsParadas == null || idsParadas.isEmpty() || atorUsuarioId == null) {
            throw new IllegalArgumentException("Execução, ordem das paradas e usuário responsável são obrigatórios");
        }
        ViagemDia viagemDia = viagemDiaRepository.findByIdForUpdate(idViagemDia)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Execução de viagem não encontrada com id: " + idViagemDia));
        validarAtorMotorista(viagemDia.getViagem(), atorUsuarioId);
        if (viagemDia.getStatus() != StatusViagemDia.PLANEJADA
                && viagemDia.getStatus() != StatusViagemDia.EM_ANDAMENTO) {
            throw new ConflictException("A rota só pode ser reordenada antes ou durante a execução");
        }

        List<ViagemParada> paradasAtuais = viagemParadaRepository.listarPorViagemDia(idViagemDia);
        if (paradasAtuais.isEmpty()) {
            throw new ConflictException("A execução ainda não possui uma rota materializada");
        }
        Set<Long> idsAtuais = paradasAtuais.stream().map(ViagemParada::getId).collect(Collectors.toSet());
        if (idsParadas.size() != idsAtuais.size()
                || new HashSet<>(idsParadas).size() != idsParadas.size()
                || !idsAtuais.equals(new HashSet<>(idsParadas))) {
            throw new BusinessException("A nova ordem deve conter cada parada existente exatamente uma vez");
        }
        if (paradasAtuais.size() < 2
                || paradasAtuais.get(0).getTipoParada() != TipoParada.MOTORISTA
                || paradasAtuais.get(paradasAtuais.size() - 1).getTipoParada() != TipoParada.MOTORISTA
                || !idsParadas.get(0).equals(paradasAtuais.get(0).getId())
                || !idsParadas.get(idsParadas.size() - 1).equals(paradasAtuais.get(paradasAtuais.size() - 1).getId())) {
            throw new BusinessException("As paradas inicial e final do motorista não podem ser reordenadas");
        }

        Map<Long, ViagemParada> paradasPorId = paradasAtuais.stream()
                .collect(Collectors.toMap(ViagemParada::getId, parada -> parada));
        if (viagemDia.getStatus() == StatusViagemDia.EM_ANDAMENTO) {
            for (int indice = 0; indice < paradasAtuais.size(); indice++) {
                ViagemParada parada = paradasAtuais.get(indice);
                if (paradaConcluida(parada)
                        && !parada.getId().equals(idsParadas.get(indice))) {
                    throw new BusinessException("Paradas já concluídas não podem ser reordenadas");
                }
            }
        }

        // Cada aluno precisa embarcar antes de desembarcar. Desembarque na escola adiantado para antes de alguns
        // embarques vira um desembarque extra nessa escola, no fim da rota (antes da casa do motorista).
        List<ViagemParada> sequencia = new ArrayList<>(idsParadas.stream().map(paradasPorId::get).toList());
        List<ViagemParada> criadas = separarDesembarquesAntecipados(sequencia);

        List<HistoricoOrdemParada> historicos = new ArrayList<>();
        Usuario ator = usuarioRepository.findById(atorUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
        LocalDateTime agora = DataBrasilia.agora();
        Map<Long, Integer> ordemAnterior = paradasAtuais.stream()
                .collect(Collectors.toMap(ViagemParada::getId, ViagemParada::getOrdem));
        // Duas etapas por causa da restrição única (viagem_dia_id, ordem): primeiro todas para ordens livres.
        int deslocamento = sequencia.size() + paradasAtuais.size();
        for (int indice = 0; indice < sequencia.size(); indice++) {
            sequencia.get(indice).setOrdem(indice + 1 + deslocamento);
        }
        viagemParadaRepository.saveAll(sequencia);
        viagemParadaRepository.flush();

        for (int indice = 0; indice < sequencia.size(); indice++) {
            ViagemParada parada = sequencia.get(indice);
            int novaOrdem = indice + 1;
            parada.setOrdem(novaOrdem);
            Integer anterior = parada.getId() == null ? null : ordemAnterior.get(parada.getId());
            boolean nova = criadas.contains(parada);
            if (nova || (anterior != null && anterior != novaOrdem)) {
                HistoricoOrdemParada historico = new HistoricoOrdemParada();
                historico.setViagemDia(viagemDia);
                historico.setParada(parada);
                // 0 = parada criada nesta alteração (desembarque extra na escola).
                historico.setOrdemAnterior(nova ? 0 : anterior);
                historico.setOrdemNova(novaOrdem);
                historico.setAtor(ator);
                historico.setOcorridoEm(agora);
                historicos.add(historico);
            }
        }
        viagemParadaRepository.saveAll(sequencia);
        viagemParadaRepository.flush();
        historicoOrdemParadaRepository.saveAll(historicos);
        return mapearParadas(viagemParadaRepository.listarPorViagemDia(idViagemDia));
    }

    /**
     * Garante que cada aluno embarque antes de desembarcar, na sequência pedida pelo motorista.
     *
     * - Desembarque na ESCOLA colocado antes do embarque de parte dos alunos dela: esses alunos passam para um
     *   desembarque extra na mesma escola, depois do último embarque deles (reaproveita um desembarque dessa escola
     *   que já esteja depois; senão cria um novo antes da casa do motorista).
     * - Desembarque na escola antes do embarque de TODOS os alunos dela, ou desembarque em casa antes do embarque
     *   na escola (volta): recusado.
     *
     * Alunos que faltaram ou já desembarcaram não contam. Altera a lista e devolve as paradas criadas.
     */
    static List<ViagemParada> separarDesembarquesAntecipados(List<ViagemParada> sequencia) {
        Map<Long, Integer> posicaoEmbarque = new HashMap<>();
        for (int i = 0; i < sequencia.size(); i++) {
            ViagemParada parada = sequencia.get(i);
            if (parada.getOperacao() != TipoOperacaoParada.EMBARQUE) continue;
            for (ViagemParadaParticipante participante : parada.getParticipantes()) {
                posicaoEmbarque.put(chaveAluno(participante), i);
            }
        }

        List<ViagemParada> criadas = new ArrayList<>();
        for (int i = 0; i < sequencia.size(); i++) {
            ViagemParada parada = sequencia.get(i);
            if (parada.getOperacao() != TipoOperacaoParada.DESEMBARQUE) continue;
            final int posicao = i;
            List<ViagemParadaParticipante> antecipados = parada.getParticipantes().stream()
                    .filter(ViagemService::aindaVaiDesembarcar)
                    .filter(p -> {
                        Integer embarque = posicaoEmbarque.get(chaveAluno(p));
                        return embarque != null && embarque > posicao;
                    })
                    .toList();
            if (antecipados.isEmpty()) continue;

            String nomes = antecipados.stream()
                    .map(p -> primeiroNome(p.getNomeDependenteSnapshot()))
                    .collect(Collectors.joining(", "));
            if (parada.getTipoParada() != TipoParada.ESCOLA) {
                throw new BusinessException("O desembarque de " + nomes
                        + " não pode vir antes do embarque na escola");
            }
            long restantes = parada.getParticipantes().stream().filter(ViagemService::aindaVaiDesembarcar).count()
                    - antecipados.size();
            if (restantes <= 0) {
                throw new BusinessException("O desembarque em " + nomeLocalSemPrefixo(parada)
                        + " precisa vir depois do embarque de pelo menos um aluno dessa escola");
            }

            int ultimoEmbarque = antecipados.stream()
                    .mapToInt(p -> posicaoEmbarque.get(chaveAluno(p)))
                    .max()
                    .orElse(posicao);
            ViagemParada destino = null;
            for (int k = ultimoEmbarque + 1; k < sequencia.size(); k++) {
                ViagemParada candidata = sequencia.get(k);
                if (candidata != parada && candidata.getOperacao() == TipoOperacaoParada.DESEMBARQUE
                        && candidata.getTipoParada() == TipoParada.ESCOLA
                        && Objects.equals(candidata.getEndereco(), parada.getEndereco())) {
                    destino = candidata;
                    break;
                }
            }
            if (destino == null) {
                destino = new ViagemParada();
                destino.setViagemDia(parada.getViagemDia());
                destino.setTipoParada(parada.getTipoParada());
                destino.setOperacao(parada.getOperacao());
                destino.setNomeLocal(parada.getNomeLocal());
                destino.setEndereco(parada.getEndereco());
                destino.setStatusValidacaoEndereco(parada.getStatusValidacaoEndereco());
                int fim = sequencia.isEmpty() || sequencia.get(sequencia.size() - 1).getTipoParada() != TipoParada.MOTORISTA
                        ? sequencia.size() : sequencia.size() - 1;
                sequencia.add(fim, destino);
                criadas.add(destino);
            }
            for (ViagemParadaParticipante antecipado : antecipados) {
                ViagemParadaParticipante copia = new ViagemParadaParticipante();
                copia.setParada(destino);
                copia.setPresenca(antecipado.getPresenca());
                copia.setDependenteIdSnapshot(antecipado.getDependenteIdSnapshot());
                copia.setNomeDependenteSnapshot(antecipado.getNomeDependenteSnapshot());
                copia.setNomeResponsavelSnapshot(antecipado.getNomeResponsavelSnapshot());
                destino.getParticipantes().add(copia);
                parada.getParticipantes().remove(antecipado);
            }
        }
        return criadas;
    }

    private static Long chaveAluno(ViagemParadaParticipante participante) {
        if (participante.getDependenteIdSnapshot() != null) return participante.getDependenteIdSnapshot();
        return participante.getPresenca() == null ? null : participante.getPresenca().getId();
    }

    private static boolean aindaVaiDesembarcar(ViagemParadaParticipante participante) {
        StatusViagemPresenca status = participante.getPresenca() == null ? null : participante.getPresenca().getStatus();
        return status != StatusViagemPresenca.FALTOU && status != StatusViagemPresenca.DESEMBARCADO;
    }

    private static String primeiroNome(String nome) {
        return nome == null || nome.isBlank() ? "aluno" : nome.trim().split("\\s+")[0];
    }

    private static String nomeLocalSemPrefixo(ViagemParada parada) {
        String nome = parada.getNomeLocal() == null ? "escola" : parada.getNomeLocal();
        return nome.replaceFirst("^(Escola|Desembarque|Embarque)[^:]*:\\s*", "");
    }

    private boolean paradaConcluida(ViagemParada parada) {
        if (parada.getOperacao() == TipoOperacaoParada.EMBARQUE) {
            return !parada.getParticipantes().isEmpty() && parada.getParticipantes().stream()
                    .allMatch(participante -> participante.getPresenca().getStatus()
                            == StatusViagemPresenca.EMBARCADO
                            || participante.getPresenca().getStatus() == StatusViagemPresenca.FALTOU);
        }
        if (parada.getOperacao() == TipoOperacaoParada.DESEMBARQUE) {
            return !parada.getParticipantes().isEmpty() && parada.getParticipantes().stream()
                    .allMatch(participante -> participante.getPresenca().getStatus()
                            == StatusViagemPresenca.DESEMBARCADO
                            || participante.getPresenca().getStatus() == StatusViagemPresenca.FALTOU);
        }
        return parada.getTipoParada() == TipoParada.MOTORISTA;
    }

    private StatusValidacaoEndereco statusEnderecoParada(
            ViagemParada parada,
            Map<Long, Dependente> dependentesPorId,
            Map<Long, List<EnderecoDependente>> enderecosPorDependente,
            boolean ida) {
        if (parada.getTipoParada() == TipoParada.MOTORISTA) {
            Motorista motorista = parada.getViagemDia().getViagem().getMotorista();
            EnderecoOperacional endereco = motorista == null || motorista.getUsuario() == null
                    ? null : motorista.getUsuario().getEnderecoOperacional();
            return endereco == null ? null : endereco.getStatusValidacao();
        }
        if (parada.getParticipantes().isEmpty()) {
            return null;
        }
        Long dependenteId = parada.getParticipantes().get(0).getDependenteIdSnapshot();
        Dependente dependente = dependentesPorId.get(dependenteId);
        if (dependente == null) {
            return null;
        }
        if (parada.getTipoParada() == TipoParada.ESCOLA) {
            EnderecoOperacional endereco = dependente.getEscola() == null || dependente.getEscola().getUsuario() == null
                    ? null : dependente.getEscola().getUsuario().getEnderecoOperacional();
            return endereco == null ? null : endereco.getStatusValidacao();
        }
        FinalidadeEndereco finalidade = ida
                ? FinalidadeEndereco.EMBARQUE : FinalidadeEndereco.DESEMBARQUE;
        EnderecoDependente endereco = selecionarEndereco(
                enderecosPorDependente.getOrDefault(dependenteId, List.of()), finalidade);
        return endereco == null ? null : endereco.getStatusValidacao();
    }

    private List<ParadaDTO> mapearParadas(List<ViagemParada> paradas) {
        List<Long> dependenteIds = paradas.stream()
                .flatMap(parada -> parada.getParticipantes().stream())
                .map(participante -> participante.getDependenteIdSnapshot())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Long> usuarioResponsavelPorDependente = dependenteIds.isEmpty()
                ? Map.of()
                : responsavelRepository.findUsuariosAtivosPorDependentes(dependenteIds).stream()
                        .collect(Collectors.toMap(
                                DependenteResponsavelUsuarioProjection::getDependenteId,
                                DependenteResponsavelUsuarioProjection::getUsuarioId,
                                (primeiro, segundo) -> primeiro));

        return paradas.stream()
                .map(parada -> new ParadaDTO(
                        parada.getId(),
                        parada.getOrdem(),
                        parada.getTipoParada(),
                        parada.getNomeLocal(),
                        parada.getEndereco(),
                        parada.getParticipantes().stream()
                                .map(participante -> new DependenteParadaDTO(
                                        participante.getDependenteIdSnapshot(),
                                        participante.getNomeDependenteSnapshot(),
                                        participante.getNomeResponsavelSnapshot(),
                                        participante.getPresenca().getStatus() == null
                                                ? "N/A" : participante.getPresenca().getStatus().name(),
                                        usuarioResponsavelPorDependente.get(participante.getDependenteIdSnapshot())))
                                .toList()))
                .toList();
    }

    /**
     * Cria a viagem do motorista para um período. Com escolaIds, a viagem atende essas escolas,
     * tem nome padrão de ida/volta e só aceita alunos de uma delas.
     */
    public Viagem criarViagem(Long idMotorista, Viagem viagem) {
        return criarViagem(idMotorista, viagem, null);
    }
    public Viagem criarViagem(Long idMotorista, Viagem viagem, List<Long> escolaIds) {
        if (viagem == null || idMotorista == null) throw new IllegalArgumentException("Dados da viagem e motorista são obrigatórios");
        if (escolaIds != null && !escolaIds.isEmpty()) {
            List<Long> idsUnicos = escolaIds.stream().distinct().toList();
            List<Escola> escolas = escolaRepository.findAllById(idsUnicos);
            if (escolas.size() != idsUnicos.size()) {
                throw new ResourceNotFoundException("Escola não encontrada entre os ids: " + idsUnicos);
            }
            viagem.setEscolas(new ArrayList<>(escolas));
        }

        Motorista motorista = motoristaRepository.findById(idMotorista)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + idMotorista));

        if (motorista.getVeiculo() == null) {
            throw new BusinessException("Motorista deve possuir um veículo cadastrado para criar viagens");
        }

        Optional<Viagem> viagemExistente = viagemRepository.findByMotoristaIdAndPeriodo(idMotorista, viagem.getPeriodo());
        if (viagemExistente.isPresent()) {
            throw new ConflictException("Motorista já possui uma viagem ativa para este período: " + viagem.getPeriodo());
        }

        if (viagem.getHorarioPartidaPadrao() == null && viagem.getPeriodo() != null) {
            viagem.setHorarioPartidaPadrao(viagem.getPeriodo().horarioPartidaPadrao());
        }
        viagem.setMotorista(motorista);
        return viagemRepository.save(viagem);
    }

    /** Edita o nome da viagem; vazio volta ao nome padrão de ida/volta. */
    @Transactional
    public Viagem atualizarNome(Long idViagem, String nome) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        viagem.setNome(nome);
        return viagemRepository.save(viagem);
    }

    /**
     * Altera o horário de partida padrão. Execuções que ainda não têm horário próprio e já não estão planejadas
     * (ou são de datas passadas) recebem o horário anterior antes da troca, para que o histórico não mude.
     */
    @Transactional
    public Viagem alterarHorarioPartidaPadrao(Long idViagem, LocalTime horario) {
        if (idViagem == null || horario == null) {
            throw new IllegalArgumentException("Viagem e horário de partida são obrigatórios");
        }
        Viagem viagem = viagemRepository.findByIdForUpdate(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        if (!Boolean.TRUE.equals(viagem.getAtivo())) {
            throw new ConflictException("Não é possível alterar o horário de um itinerário desativado");
        }

        LocalTime anterior = HorarioPartida.resolver(viagem, null);
        if (anterior != null && !anterior.equals(horario)) {
            LocalDate hoje = LocalDate.now(relogio);
            List<ViagemDia> congeladas = viagemDiaRepository
                    .findSemHorarioProprioParaCongelar(idViagem, StatusViagemDia.PLANEJADA, hoje);
            congeladas.forEach(dia -> dia.setHorarioPartida(anterior));
            viagemDiaRepository.saveAll(congeladas);
        }

        viagem.setHorarioPartidaPadrao(horario);
        return viagemRepository.save(viagem);
    }

    /**
     * Altera o horário de partida de uma data dentro da janela de preparação. Horário nulo volta ao padrão.
     * Se a execução ainda não existe, ela é preparada antes. Só é permitido enquanto estiver PLANEJADA.
     */
    @Transactional
    public ViagemDia alterarHorarioPartidaDia(Long idViagem, LocalDate data, LocalTime horario, Long atorUsuarioId) {
        if (idViagem == null || data == null || atorUsuarioId == null) {
            throw new IllegalArgumentException("Viagem, data e usuário responsável pela alteração são obrigatórios");
        }
        LocalDate hoje = LocalDate.now(relogio);
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(6))) {
            throw new BusinessException("A alteração do horário deve ocorrer entre hoje e os próximos 6 dias");
        }

        ViagemDia viagemDia = prepararViagemDia(idViagem, data, atorUsuarioId);
        if (viagemDia.getStatus() != StatusViagemDia.PLANEJADA) {
            throw new ConflictException("O horário de partida só pode ser alterado enquanto a execução estiver planejada");
        }
        viagemDia.setHorarioPartida(horario);
        return viagemDiaRepository.save(viagemDia);
    }

    @Transactional
    public List<ParadaDTO> alterarStatusPresenca(
            Long idViagemDia, Long idDependente, String novoStatus, Long atorUsuarioId) {
        if (idViagemDia == null || idDependente == null || novoStatus == null || atorUsuarioId == null) {
            throw new IllegalArgumentException(
                    "IDs de execução, dependente, usuário responsável e novo status são obrigatórios");
        }

        ViagemDia viagemDia = viagemDiaRepository.findByIdForUpdate(idViagemDia)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Execução de viagem não encontrada com id: " + idViagemDia));
        validarAtorMotorista(viagemDia.getViagem(), atorUsuarioId);
        StatusViagemDia statusExecucao = viagemDia.getStatus();
        if (statusExecucao == StatusViagemDia.FINALIZADA
                || statusExecucao == StatusViagemDia.CANCELADA
                || statusExecucao == StatusViagemDia.INTERROMPIDA) {
            throw new ConflictException("Presenças de uma execução encerrada não podem ser alteradas");
        }
        ViagemPresenca presenca = viagemPresencaRepository
                .findByViagemDiaIdAndDependenteId(idViagemDia, idDependente)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de presença não encontrado"));

        StatusViagemPresenca status;
        try {
            status = StatusViagemPresenca.valueOf(novoStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status de presença inválido: " + novoStatus);
        }

        StatusViagemPresenca anterior = presenca.getStatus();
        if (anterior == status) {
            return gerarParadasViagemDia(idViagemDia);
        }
        boolean transicaoPermitida = (anterior == StatusViagemPresenca.ESPERANDO
                && (status == StatusViagemPresenca.EMBARCADO || status == StatusViagemPresenca.FALTOU))
                || (anterior == StatusViagemPresenca.EMBARCADO
                && status == StatusViagemPresenca.DESEMBARCADO);
        if (!transicaoPermitida) {
            throw new ConflictException("Transição de presença não permitida: " + anterior + " para " + status);
        }

        if (status == StatusViagemPresenca.EMBARCADO) {
            presenca.setHorarioEmbarque(DataBrasilia.agora());
        } else if (status == StatusViagemPresenca.DESEMBARCADO) {
            presenca.setHorarioDesembarque(DataBrasilia.agora());
        }

        presenca.setStatus(status);
        viagemPresencaRepository.save(presenca);
        registrarHistoricoViagemPresenca(presenca, anterior, status, atorUsuarioId, null, null, Instant.now());
        notificarResponsaveisDaPresenca(atorUsuarioId, presenca, viagemDia, status);
        notificarProximaParada(atorUsuarioId, viagemDia);

        return gerarParadasViagemDia(idViagemDia);
    }

    @Transactional
    public ResultadoEventoPresencaDTO sincronizarPresencaOffline(
            EventoPresencaOfflineDTO evento, Long atorUsuarioId) {
        if (evento == null || evento.idEvento() == null || evento.idViagemDia() == null
                || evento.idDependente() == null || evento.statusEsperado() == null
                || evento.statusNovo() == null || evento.ocorridoEm() == null || atorUsuarioId == null) {
            throw new BusinessException("Evento offline e usuário autenticado são obrigatórios");
        }
        if (evento.idViagemDia() <= 0 || evento.idDependente() <= 0) {
            throw new BusinessException("Os identificadores da execução e do dependente devem ser positivos");
        }
        if (evento.idEvento().version() != 4) {
            throw new BusinessException("O identificador do evento offline deve ser um UUID v4");
        }

        ViagemDia viagemDia = viagemDiaRepository.findByIdForUpdate(evento.idViagemDia())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Execução de viagem não encontrada com id: " + evento.idViagemDia()));
        validarAtorMotorista(viagemDia.getViagem(), atorUsuarioId);

        Optional<HistoricoViagemPresenca> eventoAnterior =
                historicoViagemPresencaRepository.findByAtor_IdAndEventoClienteId(
                        atorUsuarioId, evento.idEvento());
        if (eventoAnterior.isPresent()) {
            HistoricoViagemPresenca historico = eventoAnterior.get();
            boolean mesmoAlvo = evento.idViagemDia().equals(historico.getPresenca().getViagemDia().getId())
                    && evento.idDependente().equals(historico.getPresenca().getDependente().getId());
            boolean mesmoEvento = mesmoAlvo
                    && evento.statusEsperado() == historico.getStatusAnterior()
                    && evento.statusNovo() == historico.getStatusNovo()
                    && evento.ocorridoEm().equals(historico.getInstanteDispositivo());
            StatusViagemPresenca statusAtual = mesmoAlvo
                    ? buscarStatusPresencaAtual(evento.idViagemDia(), evento.idDependente())
                    : null;
            if (mesmoEvento) {
                return new ResultadoEventoPresencaDTO(
                        evento.idEvento(), evento.idViagemDia(), evento.idDependente(),
                        StatusEventoSincronizacao.DUPLICADO, statusAtual,
                        "Evento já sincronizado");
            }
            return new ResultadoEventoPresencaDTO(
                    evento.idEvento(), evento.idViagemDia(), evento.idDependente(),
                    StatusEventoSincronizacao.CONFLITO, statusAtual,
                    "UUID já utilizada por outro conteúdo");
        }

        ViagemPresenca presenca = viagemPresencaRepository
                .findByViagemDiaIdAndDependenteId(evento.idViagemDia(), evento.idDependente())
                .orElseThrow(() -> new ResourceNotFoundException("Registro de presença não encontrado"));
        StatusViagemPresenca atual = presenca.getStatus();
        if (viagemDia.getStatus() != StatusViagemDia.EM_ANDAMENTO) {
            return resultadoOffline(evento, StatusEventoSincronizacao.CONFLITO, atual,
                    "Sincronização permitida somente durante execução EM_ANDAMENTO");
        }
        if (evento.ocorridoEm().isAfter(Instant.now().plusSeconds(30))) {
            return resultadoOffline(evento, StatusEventoSincronizacao.REJEITADO, atual,
                    "Instante do dispositivo não pode estar no futuro");
        }
        if (atual != evento.statusEsperado()) {
            return resultadoOffline(evento, StatusEventoSincronizacao.CONFLITO, atual,
                    "Estado atual diverge do estado esperado");
        }
        boolean transicaoPermitida = (atual == StatusViagemPresenca.ESPERANDO
                && (evento.statusNovo() == StatusViagemPresenca.EMBARCADO
                || evento.statusNovo() == StatusViagemPresenca.FALTOU))
                || (atual == StatusViagemPresenca.EMBARCADO
                && evento.statusNovo() == StatusViagemPresenca.DESEMBARCADO);
        if (!transicaoPermitida) {
            return resultadoOffline(evento, StatusEventoSincronizacao.REJEITADO, atual,
                    "Transição de presença não permitida");
        }

        LocalDateTime horarioOcorrido = LocalDateTime.ofInstant(evento.ocorridoEm(), ZoneId.systemDefault());
        if (evento.statusNovo() == StatusViagemPresenca.EMBARCADO) {
            presenca.setHorarioEmbarque(horarioOcorrido);
        } else if (evento.statusNovo() == StatusViagemPresenca.DESEMBARCADO) {
            presenca.setHorarioDesembarque(horarioOcorrido);
        }
        presenca.setStatus(evento.statusNovo());
        viagemPresencaRepository.save(presenca);
        registrarHistoricoViagemPresenca(
                presenca, atual, evento.statusNovo(), atorUsuarioId, evento.idEvento(),
                evento.ocorridoEm(), Instant.now());
        notificarResponsaveisDaPresenca(atorUsuarioId, presenca, viagemDia, evento.statusNovo());
        notificarProximaParada(atorUsuarioId, viagemDia);

        return resultadoOffline(evento, StatusEventoSincronizacao.APLICADO, evento.statusNovo(), null);
    }

    private StatusViagemPresenca buscarStatusPresencaAtual(Long idViagemDia, Long idDependente) {
        return viagemPresencaRepository.findByViagemDiaIdAndDependenteId(idViagemDia, idDependente)
                .map(ViagemPresenca::getStatus)
                .orElse(null);
    }

    private ResultadoEventoPresencaDTO resultadoOffline(
            EventoPresencaOfflineDTO evento,
            StatusEventoSincronizacao resultado,
            StatusViagemPresenca statusAtual,
            String mensagem) {
        return new ResultadoEventoPresencaDTO(
                evento.idEvento(), evento.idViagemDia(), evento.idDependente(),
                resultado, statusAtual, mensagem);
    }

    private void notificarResponsaveisDaPresenca(
            Long atorUsuarioId, ViagemPresenca presenca, ViagemDia viagemDia, StatusViagemPresenca status) {
        Dependente dependente = presenca.getDependente();
        Viagem viagem = viagemDia == null ? null : viagemDia.getViagem();
        MensagensNotificacao.Texto texto;
        TipoNotificacao tipo;
        switch (status) {
            case EMBARCADO -> {
                texto = MensagensNotificacao.embarque(dependente, viagem, presenca.getHorarioEmbarque());
                tipo = TipoNotificacao.EMBARQUE;
            }
            case DESEMBARCADO -> {
                texto = MensagensNotificacao.desembarque(dependente, viagem, presenca.getHorarioDesembarque());
                tipo = TipoNotificacao.DESEMBARQUE;
            }
            case FALTOU -> {
                texto = MensagensNotificacao.falta(dependente, viagem);
                tipo = TipoNotificacao.AUSENCIA;
            }
            default -> {
                return;
            }
        }
        Long dependenteId = dependente == null ? null : dependente.getId();
        Long viagemDiaId = viagemDia == null ? null : viagemDia.getId();
        List<DependenteResponsavelUsuarioProjection> vinculos =
                responsavelRepository.findUsuariosAtivosPorDependentes(List.of(dependenteId));
        Set<Long> destinatarioIds = vinculos.stream()
                .map(DependenteResponsavelUsuarioProjection::getUsuarioId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        notificacaoService.criarNotificacoesAutomaticas(
                atorUsuarioId, destinatarioIds, texto.titulo(), texto.mensagem(),
                new NotificacaoService.Contexto(tipo, viagemDiaId, dependenteId,
                        "presenca:" + viagemDiaId + ":" + dependenteId + ":" + status.name()));
    }

    private void notificarResponsaveisDaExecucao(
            Long atorUsuarioId, ViagemDia viagemDia, StatusViagemDia status, String motivo) {
        Viagem viagem = viagemDia.getViagem();
        MensagensNotificacao.Texto texto;
        TipoNotificacao tipo;
        switch (status) {
            case EM_ANDAMENTO -> {
                texto = MensagensNotificacao.aCaminho(viagem, HorarioPartida.resolver(viagem, viagemDia));
                tipo = TipoNotificacao.MOTORISTA_A_CAMINHO;
            }
            case FINALIZADA -> {
                texto = MensagensNotificacao.finalizada(viagem);
                tipo = TipoNotificacao.VIAGEM_FINALIZADA;
            }
            case CANCELADA -> {
                texto = MensagensNotificacao.cancelada(viagem, motivo);
                tipo = TipoNotificacao.VIAGEM_CANCELADA;
            }
            case INTERROMPIDA -> {
                texto = MensagensNotificacao.interrompida(viagem, motivo);
                tipo = TipoNotificacao.VIAGEM_INTERROMPIDA;
            }
            default -> {
                return;
            }
        }
        Long idViagemDia = viagemDia.getId();
        Set<Long> destinatarioIds = responsaveisDaExecucao(idViagemDia, null);
        if (destinatarioIds.isEmpty()) {
            return;
        }
        notificacaoService.criarNotificacoesAutomaticas(
                atorUsuarioId, destinatarioIds, texto.titulo(), texto.mensagem(),
                new NotificacaoService.Contexto(tipo, idViagemDia, null, "execucao:" + idViagemDia + ":" + status.name()));
    }

    /**
     * Usuários responsáveis pelos alunos da viagem do dia. Com {@code somenteStatus}, só dos alunos nesses status
     * (ex.: atraso avisa só quem ainda não foi entregue).
     */
    private Set<Long> responsaveisDaExecucao(Long idViagemDia, Set<StatusViagemPresenca> somenteStatus) {
        List<Long> dependenteIds = viagemPresencaRepository.findByViagemDiaId(idViagemDia).stream()
                .filter(p -> somenteStatus == null || somenteStatus.contains(p.getStatus()))
                .map(presenca -> presenca.getDependente().getId())
                .distinct()
                .toList();
        if (dependenteIds.isEmpty()) {
            return Set.of();
        }
        return responsavelRepository.findUsuariosAtivosPorDependentes(dependenteIds).stream()
                .map(DependenteResponsavelUsuarioProjection::getUsuarioId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * O motorista avisa que a viagem do dia está atrasada. Avisa os responsáveis dos alunos ainda não entregues;
     * no máximo um aviso a cada 10 minutos por viagem.
     */
    @Transactional
    public void avisarAtraso(Long idViagemDia, int minutos, String motivo, Long atorUsuarioId) {
        if (idViagemDia == null || atorUsuarioId == null) {
            throw new IllegalArgumentException("Execução e usuário responsável são obrigatórios");
        }
        if (minutos < 5 || minutos > 180) {
            throw new BusinessException("Informe um atraso entre 5 e 180 minutos");
        }
        String motivoNormalizado = motivo == null || motivo.isBlank() ? null : motivo.trim();
        if (motivoNormalizado != null && motivoNormalizado.length() > 200) {
            throw new BusinessException("O motivo deve ter no máximo 200 caracteres");
        }
        ViagemDia viagemDia = viagemDiaRepository.findById(idViagemDia)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia));
        validarAtorMotorista(viagemDia.getViagem(), atorUsuarioId);
        if (viagemDia.getStatus() != StatusViagemDia.PLANEJADA && viagemDia.getStatus() != StatusViagemDia.EM_ANDAMENTO) {
            throw new ConflictException("Só é possível avisar atraso em viagem planejada ou em andamento");
        }
        if (notificacaoRepository.existsByTipoAndViagemDiaIdAndDataAfter(
                TipoNotificacao.ATRASO.name(), idViagemDia, DataBrasilia.agora().minusMinutes(10))) {
            throw new ConflictException("Um aviso de atraso desta viagem foi enviado há menos de 10 minutos");
        }
        Set<Long> destinatarioIds = responsaveisDaExecucao(idViagemDia,
                Set.of(StatusViagemPresenca.ESPERANDO, StatusViagemPresenca.EMBARCADO));
        if (destinatarioIds.isEmpty()) {
            throw new BusinessException("Não há alunos aguardando ou a bordo para avisar");
        }
        MensagensNotificacao.Texto texto = MensagensNotificacao.atraso(viagemDia.getViagem(), minutos, motivoNormalizado);
        notificacaoService.criarNotificacoesAutomaticas(atorUsuarioId, destinatarioIds, texto.titulo(), texto.mensagem(),
                new NotificacaoService.Contexto(TipoNotificacao.ATRASO, idViagemDia, null, null));
    }

    /**
     * Avisa os responsáveis das viagens de hoje (planejadas ou em andamento) de que o veículo do motorista mudou.
     * Chamado pelo MotoristaService quando os dados do veículo são alterados.
     */
    @Transactional
    public void notificarTrocaDeVeiculo(Motorista motorista) {
        if (motorista == null || motorista.getVeiculo() == null || motorista.getUsuario() == null) {
            return;
        }
        Set<Long> destinatarioIds = new LinkedHashSet<>();
        for (Viagem viagem : viagemRepository.findAtivasByMotoristaId(motorista.getId())) {
            viagemDiaRepository.findByViagemIdAndData(viagem.getId(), LocalDate.now(relogio))
                    .filter(vd -> vd.getStatus() == StatusViagemDia.PLANEJADA || vd.getStatus() == StatusViagemDia.EM_ANDAMENTO)
                    .ifPresent(vd -> destinatarioIds.addAll(responsaveisDaExecucao(vd.getId(), null)));
        }
        if (destinatarioIds.isEmpty()) {
            return;
        }
        var v = motorista.getVeiculo();
        MensagensNotificacao.Texto texto = MensagensNotificacao.trocaVeiculo(v.getModelo(), v.getCor(), v.getPlaca());
        notificacaoService.criarNotificacoesAutomaticas(motorista.getUsuario().getId(), destinatarioIds,
                texto.titulo(), texto.mensagem(),
                new NotificacaoService.Contexto(TipoNotificacao.TROCA_VEICULO, null, null,
                        "veiculo:" + v.getId() + ":" + LocalDate.now(relogio) + ":" + v.getPlaca()));
    }

    private void registrarHistoricoViagemPresenca(
            ViagemPresenca presenca,
            StatusViagemPresenca anterior,
            StatusViagemPresenca novo,
            Long atorUsuarioId,
            UUID eventoClienteId,
            Instant instanteDispositivo,
            Instant recebidoEm) {
        Usuario ator = usuarioRepository.findById(atorUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
        HistoricoViagemPresenca historico = new HistoricoViagemPresenca();
        historico.setPresenca(presenca);
        historico.setStatusAnterior(anterior);
        historico.setStatusNovo(novo);
        historico.setAtor(ator);
        historico.setOcorridoEm(DataBrasilia.agora());
        historico.setEventoClienteId(eventoClienteId);
        historico.setInstanteDispositivo(instanteDispositivo);
        historico.setRecebidoEm(recebidoEm);
        historicoViagemPresencaRepository.save(historico);
    }

    @Transactional
    public ViagemDia alterarStatusViagemDia(Long idViagemDia,
                                           String novoStatus,
                                           String motivo,
                                           Long atorUsuarioId) {
        if (idViagemDia == null || novoStatus == null || atorUsuarioId == null) {
            throw new IllegalArgumentException("Execução, novo status e usuário responsável são obrigatórios");
        }

        ViagemDia viagemDia = viagemDiaRepository.findByIdForUpdate(idViagemDia)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia));

        StatusViagemDia status;
        try {
            status = StatusViagemDia.valueOf(novoStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status de execução de viagem inválido: " + novoStatus);
        }

        validarAtorMotorista(viagemDia.getViagem(), atorUsuarioId);
        StatusViagemDia anterior = viagemDia.getStatus();
        boolean transicaoPermitida = (anterior == StatusViagemDia.PLANEJADA
                && (status == StatusViagemDia.EM_ANDAMENTO || status == StatusViagemDia.CANCELADA))
                || (anterior == StatusViagemDia.EM_ANDAMENTO
                && (status == StatusViagemDia.FINALIZADA || status == StatusViagemDia.INTERROMPIDA));
        if (!transicaoPermitida) {
            throw new ConflictException("Transição de status não permitida: "
                    + anterior + " para " + status);
        }

        String motivoNormalizado = motivo == null || motivo.isBlank() ? null : motivo.trim();
        if ((status == StatusViagemDia.CANCELADA || status == StatusViagemDia.INTERROMPIDA)
                && motivoNormalizado == null) {
            throw new BusinessException("Informe o motivo para cancelar ou interromper a execução");
        }
        if (motivoNormalizado != null && motivoNormalizado.length() > 1000) {
            throw new BusinessException("O motivo deve ter no máximo 1000 caracteres");
        }

        if (status == StatusViagemDia.EM_ANDAMENTO) {
            List<String> bloqueios = coletarBloqueiosProntidao(viagemDia.getViagem(), new ArrayList<>());
            if (!bloqueios.isEmpty()) {
                throw new BusinessException("Execução bloqueada: " + String.join("; ", bloqueios));
            }
        } else if (status == StatusViagemDia.FINALIZADA) {
            List<ViagemPresenca> presencas = viagemPresencaRepository.findByViagemDiaId(idViagemDia);
            if (presencas.isEmpty() || presencas.stream().anyMatch(presenca ->
                    presenca.getStatus() != StatusViagemPresenca.DESEMBARCADO
                            && presenca.getStatus() != StatusViagemPresenca.FALTOU)) {
                throw new ConflictException("A execução só pode ser finalizada quando todas as presenças estiverem "
                        + "DESEMBARCADO ou FALTOU");
            }
        }

        viagemDia.setStatus(status);
        LocalDateTime agora = DataBrasilia.agora();
        viagemDia.setDataUltimaAlteracaoStatus(agora);
        viagemDiaRepository.save(viagemDia);
        registrarHistoricoViagemDia(viagemDia, anterior, status, atorUsuarioId, motivoNormalizado);
        if (status == StatusViagemDia.FINALIZADA || status == StatusViagemDia.CANCELADA
                || status == StatusViagemDia.INTERROMPIDA) {
            localizacaoViagemAtualRepository.deleteByViagemDiaId(idViagemDia);
        }
        notificarResponsaveisDaExecucao(atorUsuarioId, viagemDia, status, motivoNormalizado);
        if (status == StatusViagemDia.EM_ANDAMENTO) {
            notificarProximaParada(atorUsuarioId, viagemDia);
        }

        return viagemDia;
    }

    /**
     * "Você é o próximo": avisa os responsáveis dos alunos da próxima parada de casa ainda pendente.
     * Na ida a parada de casa é a busca (aluno ESPERANDO); na volta é a entrega (aluno EMBARCADO).
     * Cada aluno recebe o aviso uma vez por execução (chave de deduplicação). Falhas aqui não desfazem a presença.
     */
    private void notificarProximaParada(Long atorUsuarioId, ViagemDia viagemDia) {
        if (viagemDia == null || viagemDia.getId() == null || viagemDia.getStatus() != StatusViagemDia.EM_ANDAMENTO) {
            return;
        }
        try {
            Viagem viagem = viagemDia.getViagem();
            boolean ida = viagem != null && isViagemIda(viagem.getPeriodo());
            ParadaDTO proxima = proximaParadaPendente(gerarParadasViagemDia(viagemDia.getId()), ida);
            if (proxima == null || proxima.tipoParada() != TipoParada.RESPONSAVEL) {
                return;
            }
            String pendente = ida ? StatusViagemPresenca.ESPERANDO.name() : StatusViagemPresenca.EMBARCADO.name();
            for (DependenteParadaDTO aluno : proxima.listaDependentes()) {
                if (aluno.id() == null || !pendente.equals(aluno.statusEmbarque())) continue;
                Set<Long> destinatarioIds = responsavelRepository.findUsuariosAtivosPorDependentes(List.of(aluno.id()))
                        .stream()
                        .map(DependenteResponsavelUsuarioProjection::getUsuarioId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (destinatarioIds.isEmpty()) continue;
                MensagensNotificacao.Texto texto = MensagensNotificacao.proximo(aluno.nomeDependente(), viagem, ida);
                notificacaoService.criarNotificacoesAutomaticas(
                        atorUsuarioId, destinatarioIds, texto.titulo(), texto.mensagem(),
                        new NotificacaoService.Contexto(TipoNotificacao.PROXIMO_ALUNO, viagemDia.getId(), aluno.id(),
                                "proximo:" + viagemDia.getId() + ":" + aluno.id()));
            }
        } catch (RuntimeException e) {
            // O aviso é complementar: não pode impedir o registro de presença nem o início da viagem.
        }
    }

    /** Primeira parada (fora as do motorista) com aluno ainda pendente nela, na ordem atual da rota. */
    static ParadaDTO proximaParadaPendente(List<ParadaDTO> paradas, boolean ida) {
        if (paradas == null) return null;
        for (ParadaDTO parada : paradas) {
            if (parada.tipoParada() == TipoParada.MOTORISTA || parada.listaDependentes() == null) continue;
            boolean embarqueAqui = (parada.tipoParada() == TipoParada.RESPONSAVEL) == ida;
            String pendente = embarqueAqui ? StatusViagemPresenca.ESPERANDO.name() : StatusViagemPresenca.EMBARCADO.name();
            if (parada.listaDependentes().stream().anyMatch(d -> pendente.equals(d.statusEmbarque()))) {
                return parada;
            }
        }
        return null;
    }

    private void validarAtorMotorista(Viagem viagem, Long atorUsuarioId) {
        if (viagem == null || viagem.getMotorista() == null
                || viagem.getMotorista().getUsuario() == null
                || !atorUsuarioId.equals(viagem.getMotorista().getUsuario().getId())) {
            throw new AccessDeniedException("Somente o motorista da viagem pode preparar ou alterar sua execução");
        }
    }

    private void registrarHistoricoViagemDia(ViagemDia viagemDia,
                                             StatusViagemDia anterior,
                                             StatusViagemDia novo,
                                             Long atorUsuarioId,
                                             String motivo) {
        Usuario ator = usuarioRepository.findById(atorUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
        HistoricoViagemDia historico = new HistoricoViagemDia();
        historico.setViagemDia(viagemDia);
        historico.setStatusAnterior(anterior);
        historico.setStatusNovo(novo);
        historico.setAtor(ator);
        historico.setOcorridoEm(DataBrasilia.agora());
        historico.setMotivo(motivo);
        historicoViagemDiaRepository.save(historico);
    }

    public Viagem buscarViagem(Long idViagem) {
        return viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
    }

    public ViagemDiaDTO visualizarViagemDia(Long idViagemDia) {
        return viagemDiaRepository.findById(idViagemDia)
                .map(ViagemDiaDTO::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia));
    }

    @Transactional(readOnly = true)
    public List<HistoricoViagemDiaDTO> historicoStatusViagemDia(Long idViagemDia) {
        if (!viagemDiaRepository.existsById(idViagemDia)) {
            throw new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia);
        }
        return historicoViagemDiaRepository.listarPorViagemDia(idViagemDia).stream()
                .map(HistoricoViagemDiaDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoViagemPresencaDTO> historicoPresencasViagemDia(Long idViagemDia) {
        if (!viagemDiaRepository.existsById(idViagemDia)) {
            throw new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia);
        }
        return historicoViagemPresencaRepository
                .findByPresenca_ViagemDia_IdOrderByOcorridoEmAscIdAsc(idViagemDia).stream()
                .map(HistoricoViagemPresencaDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoOrdemParadaDTO> historicoOrdemParadas(Long idViagemDia) {
        if (!viagemDiaRepository.existsById(idViagemDia)) {
            throw new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia);
        }
        return historicoOrdemParadaRepository.findByViagemDia_IdOrderByOcorridoEmAscIdAsc(idViagemDia).stream()
                .map(HistoricoOrdemParadaDTO::toDTO)
                .toList();
    }

    public List<ViagemDependente> buscarVinculados(Long idViagem) {
        return viagemDependenteRepository.findDetalhesByViagemId(idViagem);
    }

    private void preencherSnapshots(ViagemPresenca presenca,
                                    Dependente dependente,
                                    List<EnderecoDependente> enderecos) {
        EnderecoDependente embarque = enderecos.stream()
                .filter(endereco -> endereco.getFinalidade() == FinalidadeEndereco.EMBARQUE
                        || endereco.getFinalidade() == FinalidadeEndereco.AMBOS)
                .findFirst()
                .orElse(null);
        EnderecoDependente desembarque = enderecos.stream()
                .filter(endereco -> endereco.getFinalidade() == FinalidadeEndereco.DESEMBARQUE
                        || endereco.getFinalidade() == FinalidadeEndereco.AMBOS)
                .findFirst()
                .orElse(null);
        if (embarque != null) {
            presenca.setEnderecoEmbarqueSnapshot(EnderecoSnapshot.de(embarque));
        }
        if (desembarque != null) {
            presenca.setEnderecoDesembarqueSnapshot(EnderecoSnapshot.de(desembarque));
        }
        if (embarque == null || desembarque == null) {
            Responsavel responsavel = responsavelPrincipal(dependente);
            String enderecoLegado = responsavel != null && responsavel.getUsuario() != null
                    ? responsavel.getUsuario().getEndereco() : dependente.getEndereco();
            if (embarque == null) {
                presenca.setEnderecoEmbarqueLegadoSnapshot(enderecoLegado);
            }
            if (desembarque == null) {
                presenca.setEnderecoDesembarqueLegadoSnapshot(enderecoLegado);
            }
        }
    }

    private String enderecoDaParadaResponsavel(ViagemPresenca presenca, boolean ida) {
        if (presenca != null) {
            EnderecoSnapshot snapshot = ida
                    ? presenca.getEnderecoEmbarqueSnapshot()
                    : presenca.getEnderecoDesembarqueSnapshot();
            if (snapshot != null) {
                return snapshot.formatar();
            }
        }
        return null;
    }

    private String enderecoNome(Dependente dependente,
                                ViagemPresenca presenca,
                                boolean ida,
                                Responsavel responsavel) {
        if (presenca != null && (ida
                ? presenca.getEnderecoEmbarqueSnapshot() != null
                : presenca.getEnderecoDesembarqueSnapshot() != null)) {
            return (ida ? "Embarque de " : "Desembarque de ") + dependente.getNome();
        }
        return responsavel == null ? "Endereço de " + dependente.getNome() : "Casa de " + responsavel.getNome();
    }

    private boolean possuiSnapshotDeEndereco(ViagemPresenca presenca, boolean ida) {
        return presenca != null && (ida
                ? presenca.getEnderecoEmbarqueSnapshot() != null
                : presenca.getEnderecoDesembarqueSnapshot() != null);
    }

    private Long usuarioIdDo(Responsavel responsavel) {
        return responsavel == null || responsavel.getUsuario() == null ? null : responsavel.getUsuario().getId();
    }

    private Responsavel responsavelPrincipal(Dependente dependente) {
        if (dependente.getResponsavelCriador() != null) {
            return dependente.getResponsavelCriador();
        }
        if (dependente.getResponsaveis() == null || dependente.getResponsaveis().size() != 1) {
            return null;
        }
        return dependente.getResponsaveis().get(0);
    }
}

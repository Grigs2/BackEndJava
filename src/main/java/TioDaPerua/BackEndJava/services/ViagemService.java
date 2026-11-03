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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViagemService {

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

    @Transactional(readOnly = true)
    public List<AgendaViagemDTO> agendaMotorista(Long idMotorista) {
        LocalDate inicio = LocalDate.now();
        LocalDate fim = inicio.plusDays(6);
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
                        execucao != null));
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
        List<String> bloqueios = coletarBloqueiosProntidao(viagem);
        return new ProntidaoViagemDTO(idViagem, bloqueios.isEmpty(), bloqueios);
    }

    private List<String> coletarBloqueiosProntidao(Viagem viagem) {
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
        bloqueios.addAll(documentoMotoristaService.listarPendenciasParaInicio(motorista.getId()));

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
                        bloqueios.add("Não há vínculo operacional ativo com a escola " + escola.getId());
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
        solicitacaoRepository.encerrarPorViagemId(idViagem, LocalDateTime.now());
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
                veiculo != null ? veiculo.getCor() : "N/A"
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
                                dep.getId(), dep.getNome(), resp == null ? "N/A" : resp.getNome(), statusEmbarque));
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
                        .add(new DependenteParadaDTO(dep.getId(), dep.getNome(), nomeResp, statusEmbarque));
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
        return prepararViagemDia(idViagem, LocalDate.now(), atorUsuarioId);
    }

    @Transactional
    public ViagemDia prepararViagemDia(Long idViagem, LocalDate data, Long atorUsuarioId) {
        if (idViagem == null || data == null || atorUsuarioId == null) {
            throw new IllegalArgumentException("Viagem, data e usuário responsável pela preparação são obrigatórios");
        }
        LocalDate hoje = LocalDate.now();
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
        viagemDia.setDataUltimaAlteracaoStatus(LocalDateTime.now());
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
        materializarParadas(viagemDia, gerarParadasProjetadas(viagemDia), ativos, enderecosPorDependente);
        return viagemDia;
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

        List<HistoricoOrdemParada> historicos = new ArrayList<>();
        Usuario ator = usuarioRepository.findById(atorUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
        LocalDateTime agora = LocalDateTime.now();
        int deslocamento = paradasAtuais.size();
        for (int indice = 0; indice < idsParadas.size(); indice++) {
            ViagemParada parada = paradasPorId.get(idsParadas.get(indice));
            int novaOrdem = indice + 1;
            if (parada.getOrdem() != novaOrdem) {
                HistoricoOrdemParada historico = new HistoricoOrdemParada();
                historico.setViagemDia(viagemDia);
                historico.setParada(parada);
                historico.setOrdemAnterior(parada.getOrdem());
                historico.setOrdemNova(novaOrdem);
                historico.setAtor(ator);
                historico.setOcorridoEm(agora);
                historicos.add(historico);
            }
            parada.setOrdem(parada.getOrdem() + deslocamento);
        }
        viagemParadaRepository.saveAll(paradasAtuais);
        viagemParadaRepository.flush();

        for (int indice = 0; indice < idsParadas.size(); indice++) {
            paradasPorId.get(idsParadas.get(indice)).setOrdem(indice + 1);
        }
        viagemParadaRepository.saveAll(paradasAtuais);
        viagemParadaRepository.flush();
        historicoOrdemParadaRepository.saveAll(historicos);
        return mapearParadas(viagemParadaRepository.listarPorViagemDia(idViagemDia));
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
                                                ? "N/A" : participante.getPresenca().getStatus().name()))
                                .toList()))
                .toList();
    }

    public Viagem criarViagem(Long idMotorista, Viagem viagem) {
        if (viagem == null || idMotorista == null) throw new IllegalArgumentException("Dados da viagem e motorista são obrigatórios");

        Motorista motorista = motoristaRepository.findById(idMotorista)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + idMotorista));

        if (motorista.getVeiculo() == null) {
            throw new BusinessException("Motorista deve possuir um veículo cadastrado para criar viagens");
        }

        Optional<Viagem> viagemExistente = viagemRepository.findByMotoristaIdAndPeriodo(idMotorista, viagem.getPeriodo());
        if (viagemExistente.isPresent()) {
            throw new ConflictException("Motorista já possui uma viagem ativa para este período: " + viagem.getPeriodo());
        }

        viagem.setMotorista(motorista);
        return viagemRepository.save(viagem);
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
            presenca.setHorarioEmbarque(LocalDateTime.now());
        } else if (status == StatusViagemPresenca.DESEMBARCADO) {
            presenca.setHorarioDesembarque(LocalDateTime.now());
        }

        presenca.setStatus(status);
        viagemPresencaRepository.save(presenca);
        registrarHistoricoViagemPresenca(presenca, anterior, status, atorUsuarioId, null, null, Instant.now());
        notificarResponsaveisDaPresenca(atorUsuarioId, idDependente, status);

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
        notificarResponsaveisDaPresenca(atorUsuarioId, evento.idDependente(), evento.statusNovo());

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
            Long atorUsuarioId, Long dependenteId, StatusViagemPresenca status) {
        String mensagem = switch (status) {
            case EMBARCADO -> "O embarque do dependente foi registrado.";
            case DESEMBARCADO -> "O desembarque do dependente foi registrado.";
            case FALTOU -> "A ausência do dependente foi registrada.";
            case ESPERANDO -> null;
        };
        if (mensagem == null) {
            return;
        }
        List<DependenteResponsavelUsuarioProjection> vinculos =
                responsavelRepository.findUsuariosAtivosPorDependentes(List.of(dependenteId));
        Set<Long> destinatarioIds = vinculos.stream()
                .map(DependenteResponsavelUsuarioProjection::getUsuarioId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        notificacaoService.criarNotificacoesAutomaticas(
                atorUsuarioId, destinatarioIds, "Atualização de presença", mensagem);
    }

    private void notificarResponsaveisDaExecucao(
            Long atorUsuarioId, Long idViagemDia, StatusViagemDia status) {
        String mensagem = switch (status) {
            case EM_ANDAMENTO -> "A viagem foi iniciada.";
            case FINALIZADA -> "A viagem foi finalizada.";
            case CANCELADA -> "A viagem foi cancelada.";
            case INTERROMPIDA -> "A viagem foi interrompida.";
            case PLANEJADA -> null;
        };
        if (mensagem == null) {
            return;
        }
        List<Long> dependenteIds = viagemPresencaRepository.findByViagemDiaId(idViagemDia).stream()
                .map(presenca -> presenca.getDependente().getId())
                .distinct()
                .toList();
        if (dependenteIds.isEmpty()) {
            return;
        }
        Set<Long> destinatarioIds = responsavelRepository.findUsuariosAtivosPorDependentes(dependenteIds).stream()
                .map(DependenteResponsavelUsuarioProjection::getUsuarioId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        notificacaoService.criarNotificacoesAutomaticas(
                atorUsuarioId, destinatarioIds, "Atualização da viagem", mensagem);
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
        historico.setOcorridoEm(LocalDateTime.now());
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
            List<String> bloqueios = coletarBloqueiosProntidao(viagemDia.getViagem());
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
        LocalDateTime agora = LocalDateTime.now();
        viagemDia.setDataUltimaAlteracaoStatus(agora);
        viagemDiaRepository.save(viagemDia);
        registrarHistoricoViagemDia(viagemDia, anterior, status, atorUsuarioId, motivoNormalizado);
        if (status == StatusViagemDia.FINALIZADA || status == StatusViagemDia.CANCELADA
                || status == StatusViagemDia.INTERROMPIDA) {
            localizacaoViagemAtualRepository.deleteByViagemDiaId(idViagemDia);
        }
        notificarResponsaveisDaExecucao(atorUsuarioId, idViagemDia, status);

        return viagemDia;
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
        historico.setOcorridoEm(LocalDateTime.now());
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

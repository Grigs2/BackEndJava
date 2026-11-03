package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.AtualizarLocalizacaoViagemDTO;
import TioDaPerua.BackEndJava.DTOs.ConsentimentoLocalizacaoDTO;
import TioDaPerua.BackEndJava.DTOs.EstadoConsentimentoLocalizacaoDTO;
import TioDaPerua.BackEndJava.DTOs.LocalizacaoViagemDTO;
import TioDaPerua.BackEndJava.entities.ConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.HistoricoLocalizacaoViagem;
import TioDaPerua.BackEndJava.entities.LocalizacaoViagemAtual;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.SuspensaoLocalizacaoViagem;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.enums.AcaoConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.TipoEscopoConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.ServiceUnavailableException;
import TioDaPerua.BackEndJava.repositories.ConsentimentoLocalizacaoRepository;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoLocalizacaoViagemRepository;
import TioDaPerua.BackEndJava.repositories.LocalizacaoViagemAtualRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.SuspensaoLocalizacaoViagemRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import TioDaPerua.BackEndJava.repositories.DependenteResponsavelUsuarioProjection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class LocalizacaoViagemService {

    private static final Duration INTERVALO_MINIMO_GPS = Duration.ofSeconds(15);
    private static final Duration RETENCAO_HISTORICO_GPS = Duration.ofDays(7);
    private static final Duration RETENCAO_AUDITORIA_CONSENTIMENTO = Duration.ofDays(365);
    private static final int TAMANHO_MAXIMO_PAGINA = 500;

    @Value("${app.gps.consent.version:}")
    private String versaoPolitica;

    private final ViagemDiaRepository viagemDiaRepository;
    private final ViagemPresencaRepository viagemPresencaRepository;
    private final LocalizacaoViagemAtualRepository localizacaoAtualRepository;
    private final HistoricoLocalizacaoViagemRepository historicoLocalizacaoRepository;
    private final ConsentimentoLocalizacaoRepository consentimentoRepository;
    private final SuspensaoLocalizacaoViagemRepository suspensaoRepository;
    private final MotoristaRepository motoristaRepository;
    private final ResponsavelRepository responsavelRepository;
    private final DependenteRepository dependenteRepository;
    private final UsuarioRepository usuarioRepository;

    public LocalizacaoViagemService(
            ViagemDiaRepository viagemDiaRepository,
            ViagemPresencaRepository viagemPresencaRepository,
            LocalizacaoViagemAtualRepository localizacaoAtualRepository,
            HistoricoLocalizacaoViagemRepository historicoLocalizacaoRepository,
            ConsentimentoLocalizacaoRepository consentimentoRepository,
            SuspensaoLocalizacaoViagemRepository suspensaoRepository,
            MotoristaRepository motoristaRepository,
            ResponsavelRepository responsavelRepository,
            DependenteRepository dependenteRepository,
            UsuarioRepository usuarioRepository) {
        this.viagemDiaRepository = viagemDiaRepository;
        this.viagemPresencaRepository = viagemPresencaRepository;
        this.localizacaoAtualRepository = localizacaoAtualRepository;
        this.historicoLocalizacaoRepository = historicoLocalizacaoRepository;
        this.consentimentoRepository = consentimentoRepository;
        this.suspensaoRepository = suspensaoRepository;
        this.motoristaRepository = motoristaRepository;
        this.responsavelRepository = responsavelRepository;
        this.dependenteRepository = dependenteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ConsentimentoLocalizacaoDTO registrarConsentimentoMotorista(Long usuarioId, boolean aceito) {
        exigirIdentidade(usuarioId);
        Motorista motorista = motoristaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new AccessDeniedException("Somente motorista pode registrar este consentimento"));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        String versao = versaoPoliticaAtual();

        Optional<ConsentimentoLocalizacao> atual = consentimentoRepository
                .findTopByUsuario_IdAndEscopoAndDependenteIsNullAndVersaoPoliticaOrderByIdDesc(
                        usuarioId, TipoEscopoConsentimentoLocalizacao.MOTORISTA, versao);
        ConsentimentoLocalizacao consentimento = registrarTransicaoConsentimento(
                usuario, TipoEscopoConsentimentoLocalizacao.MOTORISTA, null, aceito, versao, atual);

        if (!aceito && atual.map(this::estaAceito).orElse(false)) {
            suspenderExecucoesAtivas(
                    viagemDiaRepository.findIdsByMotoristaUsuarioIdAndStatus(
                            motorista.getUsuario().getId(), StatusViagemDia.EM_ANDAMENTO),
                    usuario);
        }
        return ConsentimentoLocalizacaoDTO.from(consentimento);
    }

    @Transactional
    public ConsentimentoLocalizacaoDTO registrarConsentimentoResponsavel(
            Long dependenteId, Long usuarioId, boolean aceito) {
        exigirIdentidade(usuarioId);
        Responsavel responsavel = responsavelRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new AccessDeniedException("Somente responsável pode registrar este consentimento"));
        Dependente dependente = dependenteRepository.findByIdAndResponsavelId(dependenteId, responsavel.getId())
                .orElseThrow(() -> new AccessDeniedException("Responsável não possui vínculo ativo com o dependente"));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        String versao = versaoPoliticaAtual();

        Optional<ConsentimentoLocalizacao> atual = consentimentoRepository
                .findTopByUsuario_IdAndEscopoAndDependente_IdAndVersaoPoliticaOrderByIdDesc(
                        usuarioId, TipoEscopoConsentimentoLocalizacao.DEPENDENTE, dependenteId, versao);
        ConsentimentoLocalizacao consentimento = registrarTransicaoConsentimento(
                usuario, TipoEscopoConsentimentoLocalizacao.DEPENDENTE, dependente, aceito, versao, atual);

        if (!aceito && atual.map(this::estaAceito).orElse(false)) {
            suspenderExecucoesAtivas(
                    viagemDiaRepository.findIdsByDependenteIdAndStatus(
                            dependenteId, StatusViagemDia.EM_ANDAMENTO),
                    usuario);
        }
        return ConsentimentoLocalizacaoDTO.from(consentimento);
    }

    @Transactional(readOnly = true)
    public EstadoConsentimentoLocalizacaoDTO consultarEstadoConsentimento(Long viagemDiaId, Long usuarioId) {
        ViagemDia execucao = buscarExecucao(viagemDiaId);
        validarDono(execucao, usuarioId);
        String versao = versaoPoliticaAtual();
        boolean motoristaConsentiu = consentimentoMotoristaAtivo(usuarioId, versao);
        List<Long> pendentes = dependentesSemConsentimento(viagemDiaId, versao);
        boolean suspenso = suspensaoRepository.existsByViagemDia_Id(viagemDiaId);
        boolean podeAtualizar = execucao.getStatus() == StatusViagemDia.EM_ANDAMENTO
                && motoristaConsentiu && pendentes.isEmpty() && !suspenso;
        return new EstadoConsentimentoLocalizacaoDTO(
                viagemDiaId, versao, motoristaConsentiu, pendentes, suspenso, podeAtualizar);
    }

    @Transactional
    public LocalizacaoViagemDTO registrarPosicao(AtualizarLocalizacaoViagemDTO dto, Long usuarioId) {
        if (dto == null || dto.idViagemDia() == null || dto.latitude() == null
                || dto.longitude() == null || dto.instanteDispositivo() == null) {
            throw new BusinessException("Execução, coordenadas e instante do dispositivo são obrigatórios");
        }
        validarCoordenadas(dto.latitude(), dto.longitude());

        ViagemDia execucao = viagemDiaRepository.findByIdForUpdate(dto.idViagemDia())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Execução de viagem não encontrada com id: " + dto.idViagemDia()));
        validarDono(execucao, usuarioId);
        if (execucao.getStatus() != StatusViagemDia.EM_ANDAMENTO) {
            throw new ConflictException("GPS só pode ser registrado durante execução EM_ANDAMENTO");
        }
        exigirGpsAtivo(dto.idViagemDia(), usuarioId);

        Instant agora = Instant.now();
        if (dto.instanteDispositivo().isAfter(agora.plusSeconds(30))) {
            throw new BusinessException("Instante do dispositivo não pode estar no futuro");
        }
        if (dto.instanteDispositivo().isBefore(agora.minus(Duration.ofMinutes(5)))) {
            throw new BusinessException("Posição do dispositivo está desatualizada");
        }
        Optional<LocalizacaoViagemAtual> atual = localizacaoAtualRepository.findByViagemDia_Id(dto.idViagemDia());
        if (atual.isPresent()) {
            if (Duration.between(atual.get().getRecebidoEm(), agora).compareTo(INTERVALO_MINIMO_GPS) < 0) {
                throw new ConflictException("Aguarde 15 segundos antes de enviar outra posição");
            }
            if (dto.instanteDispositivo().isBefore(atual.get().getInstanteDispositivo())) {
                throw new ConflictException("Posição atrasada não pode substituir uma posição mais recente");
            }
        }

        BigDecimal latitude = dto.latitude().setScale(5, RoundingMode.HALF_UP);
        BigDecimal longitude = dto.longitude().setScale(5, RoundingMode.HALF_UP);
        HistoricoLocalizacaoViagem historico = new HistoricoLocalizacaoViagem();
        historico.setViagemDia(execucao);
        historico.setLatitude(latitude);
        historico.setLongitude(longitude);
        historico.setInstanteDispositivo(dto.instanteDispositivo());
        historico.setRecebidoEm(agora);
        historicoLocalizacaoRepository.save(historico);

        LocalizacaoViagemAtual localizacao = atual.orElseGet(LocalizacaoViagemAtual::new);
        localizacao.setViagemDia(execucao);
        localizacao.setLatitude(latitude);
        localizacao.setLongitude(longitude);
        localizacao.setInstanteDispositivo(dto.instanteDispositivo());
        localizacao.setRecebidoEm(agora);
        LocalizacaoViagemAtual salva = localizacaoAtualRepository.save(localizacao);
        return LocalizacaoViagemDTO.fromCurrent(salva, agora);
    }

    @Transactional(readOnly = true)
    public LocalizacaoViagemDTO consultarPosicaoAtual(Long viagemDiaId, Long usuarioId) {
        ViagemDia execucao = validarAcessoLeitura(viagemDiaId, usuarioId);
        exigirGpsAtivo(viagemDiaId, usuarioDono(execucao));
        LocalizacaoViagemAtual localizacao = localizacaoAtualRepository.findByViagemDia_Id(viagemDiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Posição atual indisponível"));
        return LocalizacaoViagemDTO.fromCurrent(localizacao, Instant.now());
    }

    @Transactional(readOnly = true)
    public Page<LocalizacaoViagemDTO> consultarHistorico(
            Long viagemDiaId, Long usuarioId, Instant inicio, Instant fim, int pagina, int tamanho) {
        ViagemDia execucao = validarAcessoLeitura(viagemDiaId, usuarioId);
        exigirGpsAtivo(viagemDiaId, usuarioDono(execucao));
        Instant agora = Instant.now();
        if (inicio == null || fim == null || inicio.isAfter(fim) || fim.isAfter(agora)
                || inicio.isBefore(agora.minus(RETENCAO_HISTORICO_GPS))) {
            throw new BusinessException("Informe um intervalo válido dentro dos últimos 7 dias");
        }
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new BusinessException("Página deve ser não negativa e tamanho deve estar entre 1 e 500");
        }

        Pageable pageable = PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Order.asc("recebidoEm"), Sort.Order.asc("id")));
        return historicoLocalizacaoRepository
                .findByViagemDia_IdAndRecebidoEmBetweenOrderByRecebidoEmAscIdAsc(
                        viagemDiaId, inicio, fim, pageable)
                .map(localizacao -> LocalizacaoViagemDTO.fromHistory(localizacao, agora));
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    @Transactional
    public void expurgarHistoricoExpirado() {
        historicoLocalizacaoRepository.deleteExpirados(Instant.now().minus(RETENCAO_HISTORICO_GPS));
        consentimentoRepository.deleteAntigosPreservandoUltimo(
                Instant.now().minus(RETENCAO_AUDITORIA_CONSENTIMENTO));
    }

    private ConsentimentoLocalizacao registrarTransicaoConsentimento(
            Usuario usuario,
            TipoEscopoConsentimentoLocalizacao escopo,
            Dependente dependente,
            boolean aceito,
            String versao,
            Optional<ConsentimentoLocalizacao> atual) {
        boolean jaAceito = atual.map(this::estaAceito).orElse(false);
        if (jaAceito == aceito && atual.isPresent()) {
            return atual.get();
        }
        if (!aceito && !jaAceito) {
            return atual.orElse(null);
        }

        ConsentimentoLocalizacao evento = new ConsentimentoLocalizacao();
        evento.setUsuario(usuario);
        evento.setEscopo(escopo);
        evento.setDependente(dependente);
        evento.setAcao(aceito
                ? AcaoConsentimentoLocalizacao.ACEITO
                : AcaoConsentimentoLocalizacao.REVOGADO);
        evento.setVersaoPolitica(versao);
        evento.setOcorridoEm(Instant.now());
        return consentimentoRepository.save(evento);
    }

    private void exigirGpsAtivo(Long viagemDiaId, Long motoristaUsuarioId) {
        String versao = versaoPoliticaAtual();
        EstadoConsentimentoLocalizacaoDTO estado =
                calcularEstadoConsentimento(viagemDiaId, motoristaUsuarioId, versao);
        if (estado.gpsSuspenso()) {
            throw new ConflictException("GPS suspenso para esta execução após revogação de consentimento");
        }
        if (!estado.motoristaConsentiu() || !estado.dependentesPendentes().isEmpty()) {
            throw new ConflictException("GPS indisponível: consentimento do motorista e de um responsável ativo "
                    + "por dependente participante são necessários");
        }
    }

    private EstadoConsentimentoLocalizacaoDTO calcularEstadoConsentimento(
            Long viagemDiaId, Long motoristaUsuarioId, String versao) {
        ViagemDia execucao = buscarExecucao(viagemDiaId);
        boolean motoristaConsentiu = consentimentoMotoristaAtivo(motoristaUsuarioId, versao);
        List<Long> pendentes = dependentesSemConsentimento(viagemDiaId, versao);
        boolean suspenso = suspensaoRepository.existsByViagemDia_Id(viagemDiaId);
        return new EstadoConsentimentoLocalizacaoDTO(
                viagemDiaId,
                versao,
                motoristaConsentiu,
                pendentes,
                suspenso,
                execucao.getStatus() == StatusViagemDia.EM_ANDAMENTO
                        && motoristaConsentiu && pendentes.isEmpty() && !suspenso);
    }

    private boolean consentimentoMotoristaAtivo(Long usuarioId, String versao) {
        return consentimentoRepository
                .findTopByUsuario_IdAndEscopoAndDependenteIsNullAndVersaoPoliticaOrderByIdDesc(
                        usuarioId, TipoEscopoConsentimentoLocalizacao.MOTORISTA, versao)
                .map(this::estaAceito)
                .orElse(false);
    }

    private List<Long> dependentesSemConsentimento(Long viagemDiaId, String versao) {
        List<Long> dependenteIds = viagemPresencaRepository.findByViagemDiaId(viagemDiaId).stream()
                .map(presenca -> presenca.getDependente().getId())
                .distinct()
                .toList();
        if (dependenteIds.isEmpty()) {
            return List.of();
        }

        List<DependenteResponsavelUsuarioProjection> vinculos =
                responsavelRepository.findUsuariosAtivosPorDependentes(dependenteIds);
        Map<Long, Set<Long>> usuariosPorDependente = new HashMap<>();
        Set<Long> usuarioIds = new HashSet<>();
        for (DependenteResponsavelUsuarioProjection vinculo : vinculos) {
            usuariosPorDependente.computeIfAbsent(vinculo.getDependenteId(), ignored -> new HashSet<>())
                    .add(vinculo.getUsuarioId());
            usuarioIds.add(vinculo.getUsuarioId());
        }
        if (usuarioIds.isEmpty()) {
            return new ArrayList<>(dependenteIds);
        }

        List<ConsentimentoLocalizacao> ultimosConsentimentos =
                consentimentoRepository.findLatestByDependentesAndUsuarios(
                        TipoEscopoConsentimentoLocalizacao.DEPENDENTE, dependenteIds, usuarioIds, versao);
        Set<String> consentimentosAtivos = new HashSet<>();
        for (ConsentimentoLocalizacao consentimento : ultimosConsentimentos) {
            if (estaAceito(consentimento)) {
                consentimentosAtivos.add(chaveConsentimento(
                        consentimento.getDependente().getId(), consentimento.getUsuario().getId()));
            }
        }

        return dependenteIds.stream()
                .filter(dependenteId -> usuariosPorDependente.getOrDefault(dependenteId, Set.of()).stream()
                        .noneMatch(usuarioId -> consentimentosAtivos.contains(
                                chaveConsentimento(dependenteId, usuarioId))))
                .toList();
    }

    private String chaveConsentimento(Long dependenteId, Long usuarioId) {
        return dependenteId + ":" + usuarioId;
    }

    private void suspenderExecucoesAtivas(Collection<Long> execucaoIds, Usuario ator) {
        for (Long execucaoId : execucaoIds) {
            ViagemDia execucao = viagemDiaRepository.findByIdForUpdate(execucaoId).orElse(null);
            if (execucao == null || execucao.getStatus() != StatusViagemDia.EM_ANDAMENTO
                    || suspensaoRepository.existsByViagemDia_Id(execucaoId)) {
                continue;
            }
            SuspensaoLocalizacaoViagem suspensao = new SuspensaoLocalizacaoViagem();
            suspensao.setViagemDia(execucao);
            suspensao.setAtor(ator);
            suspensao.setSuspensoEm(Instant.now());
            suspensaoRepository.save(suspensao);
        }
    }

    private ViagemDia validarAcessoLeitura(Long viagemDiaId, Long usuarioId) {
        ViagemDia execucao = buscarExecucao(viagemDiaId);
        boolean motoristaDono = viagemDiaRepository
                .existsByIdAndViagem_Motorista_Usuario_Id(viagemDiaId, usuarioId);
        boolean responsavelAtivo = viagemDiaRepository.existeVinculoResponsavel(viagemDiaId, usuarioId);
        if (!motoristaDono && !responsavelAtivo) {
            throw new AccessDeniedException("Usuário não autorizado a consultar a localização desta execução");
        }
        return execucao;
    }

    private void validarDono(ViagemDia execucao, Long usuarioId) {
        if (usuarioId == null || execucao.getViagem() == null
                || execucao.getViagem().getMotorista() == null
                || execucao.getViagem().getMotorista().getUsuario() == null
                || !usuarioId.equals(execucao.getViagem().getMotorista().getUsuario().getId())) {
            throw new AccessDeniedException("Somente o motorista dono pode registrar ou consultar o estado do GPS");
        }
    }

    private Long usuarioDono(ViagemDia execucao) {
        return execucao.getViagem().getMotorista().getUsuario().getId();
    }

    private ViagemDia buscarExecucao(Long viagemDiaId) {
        if (viagemDiaId == null) {
            throw new BusinessException("ID da execução é obrigatório");
        }
        return viagemDiaRepository.findById(viagemDiaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Execução de viagem não encontrada com id: " + viagemDiaId));
    }

    private void validarCoordenadas(BigDecimal latitude, BigDecimal longitude) {
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BusinessException("Latitude ou longitude fora dos limites permitidos");
        }
    }

    private boolean estaAceito(ConsentimentoLocalizacao consentimento) {
        return consentimento.getAcao() == AcaoConsentimentoLocalizacao.ACEITO;
    }

    private String versaoPoliticaAtual() {
        if (versaoPolitica == null || versaoPolitica.isBlank()) {
            throw new ServiceUnavailableException("GPS indisponível: versão da política não configurada");
        }
        return versaoPolitica.trim();
    }

    private void exigirIdentidade(Long usuarioId) {
        if (usuarioId == null) {
            throw new BusinessException("Usuário autenticado é obrigatório");
        }
    }
}

package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.ConviteResponsavelDTO;
import TioDaPerua.BackEndJava.DTOs.CriarConviteResponsavelDTO;
import TioDaPerua.BackEndJava.DTOs.DependenteResponsavelCriadorPendenteDTO;
import TioDaPerua.BackEndJava.DTOs.VinculoResponsavelDTO;
import TioDaPerua.BackEndJava.entities.ConviteResponsavel;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.StatusConviteResponsavel;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.repositories.ConviteResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class RedeResponsaveisService {
    private static final Duration VALIDADE_CONVITE = Duration.ofDays(7);

    private final DependenteRepository dependenteRepository;
    private final ResponsavelRepository responsavelRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConviteResponsavelRepository conviteRepository;
    private final AuditoriaAdministrativaService auditoriaService;

    public RedeResponsaveisService(DependenteRepository dependenteRepository,
                                   ResponsavelRepository responsavelRepository,
                                   UsuarioRepository usuarioRepository,
                                   ConviteResponsavelRepository conviteRepository,
                                   AuditoriaAdministrativaService auditoriaService) {
        this.dependenteRepository = dependenteRepository;
        this.responsavelRepository = responsavelRepository;
        this.usuarioRepository = usuarioRepository;
        this.conviteRepository = conviteRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public ConviteResponsavelDTO convidar(Long dependenteId,
                                          Long usuarioConvidadorId,
                                          CriarConviteResponsavelDTO request) {
        Dependente dependente = dependenteRepository.findByIdForUpdate(dependenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado"));
        Responsavel convidador = responsavelRepository.findByUsuarioId(usuarioConvidadorId)
                .orElseThrow(() -> new UnauthorizedException("Responsável autenticado não encontrado"));
        validarResponsavelCriador(dependente, convidador);

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Usuario usuarioConvidado = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Conta de responsável não encontrada"));
        if (!"RESPONSAVEL".equals(usuarioConvidado.getTipoPerfil())) {
            throw new BusinessException("O e-mail informado não pertence a uma conta de responsável");
        }
        Responsavel convidado = responsavelRepository.findByUsuarioId(usuarioConvidado.getId())
                .orElseThrow(() -> new BusinessException("A conta informada não possui perfil de responsável"));
        if (convidado.getId().equals(convidador.getId())) {
            throw new ConflictException("O responsável não pode convidar a própria conta");
        }
        if (dependente.getResponsaveis().stream().anyMatch(responsavel ->
                responsavel.getId().equals(convidado.getId()))) {
            throw new ConflictException("O responsável já possui vínculo ativo com este dependente");
        }

        Instant agora = Instant.now();
        conviteRepository.expirarConvitesVencidos(
                dependenteId,
                convidado.getId(),
                StatusConviteResponsavel.PENDENTE,
                StatusConviteResponsavel.EXPIRADO,
                agora);
        boolean convitePendente = conviteRepository
                .buscarPorDependenteEConvidadoEStatus(
                        dependenteId, convidado.getId(), StatusConviteResponsavel.PENDENTE)
                .stream()
                .anyMatch(convite -> convite.getExpiraEm().isAfter(agora));
        if (convitePendente) {
            throw new ConflictException("Já existe convite pendente para esta conta");
        }

        ConviteResponsavel convite = new ConviteResponsavel();
        convite.setDependente(dependente);
        convite.setConvidador(convidador);
        convite.setConvidado(convidado);
        convite.setCategoria(request.categoria());
        convite.setStatus(StatusConviteResponsavel.PENDENTE);
        convite.setCriadoEm(agora);
        convite.setExpiraEm(agora.plus(VALIDADE_CONVITE));
        return ConviteResponsavelDTO.toDTO(conviteRepository.save(convite));
    }

    @Transactional
    public List<ConviteResponsavelDTO> listarPendentes(Long usuarioId) {
        Instant agora = Instant.now();
        List<ConviteResponsavel> pendentes = conviteRepository.findByConvidadoIdAndStatusOrderByCriadoEmDesc(
                responsavelRepository.findByUsuarioId(usuarioId)
                        .orElseThrow(() -> new UnauthorizedException("Responsável autenticado não encontrado"))
                        .getId(),
                StatusConviteResponsavel.PENDENTE);
        List<ConviteResponsavel> expirados = pendentes.stream()
                .filter(convite -> !convite.getExpiraEm().isAfter(agora))
                .toList();
        expirados.forEach(convite -> convite.setStatus(StatusConviteResponsavel.EXPIRADO));
        if (!expirados.isEmpty()) {
            conviteRepository.saveAll(expirados);
        }
        return conviteRepository.listarPendentesDoUsuario(
                        usuarioId, StatusConviteResponsavel.PENDENTE, agora)
                .stream()
                .map(ConviteResponsavelDTO::toDTO)
                .toList();
    }

    @Transactional
    public List<ConviteResponsavelDTO> listarHistorico(Long dependenteId, Long usuarioId) {
        dependenteRepository.findByIdAndResponsavelUsuarioId(dependenteId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dependente não encontrado para o responsável autenticado"));
        List<ConviteResponsavel> convites = conviteRepository.findByDependenteIdOrderByCriadoEmDesc(dependenteId);
        Instant agora = Instant.now();
        List<ConviteResponsavel> expirados = convites.stream()
                .filter(convite -> convite.getStatus() == StatusConviteResponsavel.PENDENTE
                        && !convite.getExpiraEm().isAfter(agora))
                .toList();
        expirados.forEach(convite -> convite.setStatus(StatusConviteResponsavel.EXPIRADO));
        if (!expirados.isEmpty()) {
            conviteRepository.saveAll(expirados);
        }
        return convites.stream()
                .map(ConviteResponsavelDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VinculoResponsavelDTO> listarResponsaveisAtivos(Long dependenteId, Long usuarioId) {
        Dependente dependente = dependenteRepository.findByIdAndResponsavelUsuarioId(dependenteId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dependente não encontrado para o responsável autenticado"));
        java.util.Map<Long, ConviteResponsavel> convitesAceitos = new java.util.HashMap<>();
        conviteRepository.findByDependenteIdOrderByCriadoEmDesc(dependenteId).stream()
                .filter(convite -> convite.getStatus() == StatusConviteResponsavel.ACEITO)
                .forEach(convite -> convitesAceitos.putIfAbsent(convite.getConvidado().getId(), convite));
        return dependente.getResponsaveis().stream()
                .map(responsavel -> VinculoResponsavelDTO.toDTO(
                        dependente, responsavel, convitesAceitos.get(responsavel.getId())))
                .toList();
    }

    @Transactional
    public ConviteResponsavelDTO responder(Long conviteId, Long usuarioId, boolean aceitar) {
        ConviteResponsavel referencia = conviteRepository.findById(conviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));
        dependenteRepository.findByIdForUpdate(referencia.getDependente().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dependente do convite não encontrado"));
        ConviteResponsavel convite = conviteRepository.buscarParaResposta(conviteId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));
        if (convite.getStatus() != StatusConviteResponsavel.PENDENTE) {
            throw new ConflictException("Convite não está pendente");
        }

        Instant agora = Instant.now();
        if (!convite.getExpiraEm().isAfter(agora)) {
            convite.setStatus(StatusConviteResponsavel.EXPIRADO);
            return ConviteResponsavelDTO.toDTO(conviteRepository.save(convite));
        }

        convite.setRespondidoEm(agora);
        if (!aceitar) {
            convite.setStatus(StatusConviteResponsavel.RECUSADO);
            return ConviteResponsavelDTO.toDTO(conviteRepository.save(convite));
        }

        Dependente dependente = convite.getDependente();
        Responsavel responsavel = convite.getConvidado();
        if (dependente.getResponsaveis().stream().anyMatch(atual ->
                atual.getId().equals(responsavel.getId()))) {
            throw new ConflictException("O responsável já possui vínculo ativo com este dependente");
        }
        responsavel.getDependentes().add(dependente);
        dependente.getResponsaveis().add(responsavel);
        responsavelRepository.save(responsavel);
        convite.setStatus(StatusConviteResponsavel.ACEITO);
        return ConviteResponsavelDTO.toDTO(conviteRepository.save(convite));
    }

    @Transactional
    public void revogar(Long dependenteId, Long responsavelId, Long usuarioId, boolean administrador) {
        Dependente dependente = dependenteRepository.findByIdForUpdate(dependenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado"));
        Responsavel alvo = dependente.getResponsaveis().stream()
                .filter(responsavel -> responsavel.getId().equals(responsavelId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo ativo não encontrado"));

        if (!administrador) {
            validarResponsavelCriador(dependente,
                    responsavelRepository.findByUsuarioId(usuarioId)
                            .orElseThrow(() -> new UnauthorizedException("Responsável autenticado não encontrado")));
        }
        if (dependente.getResponsavelCriador() != null
                && dependente.getResponsavelCriador().getId().equals(responsavelId)) {
            throw new ConflictException("Atribua outro responsável criador antes de revogar este vínculo");
        }

        Instant agora = Instant.now();
        List<ConviteResponsavel> aceitos = conviteRepository.buscarAceitosDoVinculo(
                dependenteId, responsavelId, StatusConviteResponsavel.ACEITO);
        if (!aceitos.isEmpty()) {
            ConviteResponsavel convite = aceitos.get(0);
            convite.setStatus(StatusConviteResponsavel.REVOGADO);
            convite.setRevogadoEm(agora);
            convite.setRevogadoPorUsuarioId(usuarioId);
            conviteRepository.save(convite);
        } else {
            ConviteResponsavel registroLegado = new ConviteResponsavel();
            registroLegado.setDependente(dependente);
            registroLegado.setConvidado(alvo);
            registroLegado.setConvidador(dependente.getResponsavelCriador());
            registroLegado.setStatus(StatusConviteResponsavel.REVOGADO);
            registroLegado.setCriadoEm(agora);
            registroLegado.setExpiraEm(agora);
            registroLegado.setRevogadoEm(agora);
            registroLegado.setRevogadoPorUsuarioId(usuarioId);
            conviteRepository.save(registroLegado);
        }

        alvo.getDependentes().removeIf(atual -> atual.getId().equals(dependenteId));
        dependente.getResponsaveis().removeIf(atual -> atual.getId().equals(responsavelId));
        responsavelRepository.save(alvo);
    }

    @Transactional(readOnly = true)
    public List<DependenteResponsavelCriadorPendenteDTO> listarDependentesComResponsavelCriadorIndefinido() {
        return dependenteRepository.findByResponsavelCriadorIsNullOrderById().stream()
                .filter(dependente -> dependente.getResponsaveis().size() > 1)
                .map(DependenteResponsavelCriadorPendenteDTO::toDTO)
                .toList();
    }

    @Transactional
    public void atribuirResponsavelCriador(Long dependenteId, Long responsavelId, Long administradorUsuarioId) {
        Dependente dependente = dependenteRepository.findByIdForUpdate(dependenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado"));
        Responsavel responsavel = dependente.getResponsaveis().stream()
                .filter(atual -> atual.getId().equals(responsavelId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "O responsável selecionado não possui vínculo ativo com o dependente"));
        if (dependente.getResponsavelCriador() != null
                && dependente.getResponsavelCriador().getId().equals(responsavelId)) {
            throw new ConflictException("O responsável selecionado já é o responsável criador");
        }
        dependente.setResponsavelCriador(responsavel);
        dependente.setResponsavelCriadorDefinidoEm(Instant.now());
        dependente.setResponsavelCriadorDefinidoPorUsuarioId(administradorUsuarioId);
        dependenteRepository.save(dependente);
        auditoriaService.registrarAlteracao(
                administradorUsuarioId, "RESPONSAVEL_CRIADOR_ATRIBUIDO",
                "DEPENDENTE", dependenteId, "SUCESSO", null);
    }

    private void validarResponsavelCriador(Dependente dependente, Responsavel responsavel) {
        if (dependente.getResponsavelCriador() == null) {
            List<Responsavel> responsaveis = dependente.getResponsaveis();
            if (responsaveis.size() == 1) {
                dependente.setResponsavelCriador(responsaveis.get(0));
                dependenteRepository.save(dependente);
            } else {
                throw new BusinessException(
                        "O responsável criador deste dependente precisa ser definido pelo ADMIN");
            }
        }
        if (!dependente.getResponsavelCriador().getId().equals(responsavel.getId())) {
            throw new AccessDeniedException(
                    "Somente o responsável que cadastrou o dependente pode gerenciar convites");
        }
        if (dependente.getResponsaveis().stream().noneMatch(atual ->
                atual.getId().equals(responsavel.getId()))) {
            throw new AccessDeniedException(
                    "O responsável criador não possui vínculo ativo com o dependente");
        }
    }
}

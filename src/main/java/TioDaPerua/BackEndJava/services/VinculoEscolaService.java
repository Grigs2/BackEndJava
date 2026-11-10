package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.EscolaBuscaDTO;
import TioDaPerua.BackEndJava.DTOs.HistoricoVinculoEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.MotoristaBuscaDTO;
import TioDaPerua.BackEndJava.DTOs.VinculoEscolaDTO;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.HistoricoVinculoEscola;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.ViagemEscola;
import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoVinculoEscolaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemEscolaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class VinculoEscolaService {
    private static final List<StatusVinculoEscola> STATUS_PENDENTES = List.of(
            StatusVinculoEscola.PENDENTE_ESCOLA,
            StatusVinculoEscola.PENDENTE_MOTORISTA);
    private static final List<StatusVinculoEscola> STATUS_ATIVOS = List.of(
            StatusVinculoEscola.ATIVO,
            StatusVinculoEscola.SUSPENSO);

    private final EscolaRepository escolaRepository;
    private final MotoristaRepository motoristaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ViagemEscolaRepository vinculoRepository;
    private final HistoricoVinculoEscolaRepository historicoRepository;
    private final ViagemDependenteRepository viagemDependenteRepository;
    private final DocumentoMotoristaService documentoMotoristaService;

    public VinculoEscolaService(EscolaRepository escolaRepository,
                                MotoristaRepository motoristaRepository,
                                UsuarioRepository usuarioRepository,
                                ViagemEscolaRepository vinculoRepository,
                                HistoricoVinculoEscolaRepository historicoRepository,
                                ViagemDependenteRepository viagemDependenteRepository,
                                DocumentoMotoristaService documentoMotoristaService) {
        this.escolaRepository = escolaRepository;
        this.motoristaRepository = motoristaRepository;
        this.usuarioRepository = usuarioRepository;
        this.vinculoRepository = vinculoRepository;
        this.historicoRepository = historicoRepository;
        this.viagemDependenteRepository = viagemDependenteRepository;
        this.documentoMotoristaService = documentoMotoristaService;
    }

    @Transactional(readOnly = true)
    public List<EscolaBuscaDTO> buscarEscolas(String termo) {
        String filtro = termo == null ? "" : termo.trim();
        if (filtro.length() < 2 || filtro.length() > 100) {
            throw new BusinessException("Informe ao menos 2 caracteres e no máximo 100 para buscar escolas");
        }

        String cnpj = DocumentoFiscalValidator.normalizarCnpj(filtro);
        if (cnpj.length() == 14) {
            return escolaRepository.findByCnpj(cnpj)
                    .map(EscolaBuscaDTO::toDTO)
                    .map(List::of)
                    .orElseGet(List::of);
        }
        return escolaRepository.findTop20ByNomeContainingIgnoreCaseOrderByNomeAsc(filtro).stream()
                .map(EscolaBuscaDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MotoristaBuscaDTO> buscarMotoristas(Long usuarioEscolaId, String email, String cpf) {
        buscarEscolaDoUsuario(usuarioEscolaId);
        boolean informouEmail = email != null && !email.isBlank();
        boolean informouCpf = cpf != null && !cpf.isBlank();
        if (informouEmail == informouCpf) {
            throw new BusinessException("Informe exatamente um e-mail ou CPF para localizar o motorista");
        }

        Optional<Motorista> motorista;
        if (informouEmail) {
            String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
            if (!DocumentoFiscalValidator.emailValido(emailNormalizado)) {
                throw new BusinessException("E-mail inválido");
            }
            motorista = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                    .filter(usuario -> "MOTORISTA".equals(usuario.getTipoPerfil()))
                    .flatMap(usuario -> motoristaRepository.findByUsuarioId(usuario.getId()));
        } else {
            String cpfNormalizado = DocumentoFiscalValidator.normalizarCpf(cpf);
            if (!DocumentoFiscalValidator.cpfValido(cpfNormalizado)) {
                throw new BusinessException("CPF inválido");
            }
            motorista = motoristaRepository.findByCpf(cpfNormalizado);
        }

        return motorista.map(MotoristaBuscaDTO::toDTO)
                .map(List::of)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado"));
    }

    @Transactional
    public VinculoEscolaDTO solicitarVinculo(Long usuarioMotoristaId, String cnpjInformado) {
        Motorista motorista = buscarMotoristaDoUsuario(usuarioMotoristaId);
        String cnpj = DocumentoFiscalValidator.normalizarCnpj(cnpjInformado);
        if (!DocumentoFiscalValidator.cnpjValido(cnpj)) {
            throw new BusinessException("CNPJ inválido");
        }
        Escola escolaEncontrada = escolaRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada"));
        Long escolaId = escolaEncontrada.getId();
        Escola escola = escolaRepository.findByIdForUpdate(escolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada"));
        return iniciarCiclo(escola, motorista, StatusVinculoEscola.PENDENTE_ESCOLA,
                exigirUsuario(motorista.getUsuario()));
    }

    @Transactional
    public VinculoEscolaDTO convidarMotorista(Long usuarioEscolaId, Long motoristaId) {
        Escola escolaAtual = buscarEscolaDoUsuario(usuarioEscolaId);
        Escola escola = escolaRepository.findByIdForUpdate(escolaAtual.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada"));
        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado"));
        return iniciarCiclo(escola, motorista, StatusVinculoEscola.PENDENTE_MOTORISTA,
                exigirUsuario(escola.getUsuario()));
    }

    @Transactional
    public VinculoEscolaDTO responderComoEscola(Long usuarioEscolaId, Long vinculoId,
                                                 boolean aprovado, String motivo) {
        ViagemEscola vinculo = buscarVinculoParaAtualizacao(vinculoId);
        Usuario ator = validarEscolaDoVinculo(vinculo, usuarioEscolaId);
        validarEstado(vinculo, StatusVinculoEscola.PENDENTE_ESCOLA);
        if (aprovado) {
            validarDocumentosDoMotorista(vinculo.getMotorista());
        }
        alterarEstado(vinculo, aprovado ? StatusVinculoEscola.ATIVO : StatusVinculoEscola.RECUSADO,
                ator, motivoObrigatorioSeAplicavel(motivo, !aprovado, "recusar"));
        return VinculoEscolaDTO.toDTO(vinculo);
    }

    @Transactional
    public VinculoEscolaDTO responderComoMotorista(Long usuarioMotoristaId, Long vinculoId,
                                                    boolean aceito, String motivo) {
        ViagemEscola vinculo = buscarVinculoParaAtualizacao(vinculoId);
        Usuario ator = validarMotoristaDoVinculo(vinculo, usuarioMotoristaId);
        validarEstado(vinculo, StatusVinculoEscola.PENDENTE_MOTORISTA);
        alterarEstado(vinculo, aceito ? StatusVinculoEscola.ATIVO : StatusVinculoEscola.RECUSADO,
                ator, motivoObrigatorioSeAplicavel(motivo, !aceito, "recusar"));
        return VinculoEscolaDTO.toDTO(vinculo);
    }

    @Transactional
    public VinculoEscolaDTO suspender(Long usuarioEscolaId, Long vinculoId, String motivo) {
        ViagemEscola vinculo = buscarVinculoParaAtualizacao(vinculoId);
        Usuario ator = validarEscolaDoVinculo(vinculo, usuarioEscolaId);
        validarEstado(vinculo, StatusVinculoEscola.ATIVO);
        alterarEstado(vinculo, StatusVinculoEscola.SUSPENSO,
                ator, motivoObrigatorioSeAplicavel(motivo, true, "suspender"));
        encerrarAcordosDoVinculo(vinculo);
        return VinculoEscolaDTO.toDTO(vinculo);
    }

    @Transactional
    public VinculoEscolaDTO reativar(Long usuarioEscolaId, Long vinculoId, String motivo) {
        ViagemEscola vinculo = buscarVinculoParaAtualizacao(vinculoId);
        Usuario ator = validarEscolaDoVinculo(vinculo, usuarioEscolaId);
        validarEstado(vinculo, StatusVinculoEscola.SUSPENSO);
        alterarEstado(vinculo, StatusVinculoEscola.ATIVO,
                ator, motivoObrigatorioSeAplicavel(motivo, false, "reativar"));
        return VinculoEscolaDTO.toDTO(vinculo);
    }

    @Transactional
    public VinculoEscolaDTO encerrar(Long usuarioId, Long vinculoId, String motivo) {
        ViagemEscola vinculo = buscarVinculoParaAtualizacao(vinculoId);
        Usuario ator = validarParteDoVinculo(vinculo, usuarioId);
        if (vinculo.getStatus() != StatusVinculoEscola.ATIVO
                && vinculo.getStatus() != StatusVinculoEscola.SUSPENSO) {
            throw new ConflictException("Somente um vínculo ativo ou suspenso pode ser encerrado");
        }
        alterarEstado(vinculo, StatusVinculoEscola.ENCERRADO,
                ator, motivoObrigatorioSeAplicavel(motivo, true, "encerrar"));
        encerrarAcordosDoVinculo(vinculo);
        return VinculoEscolaDTO.toDTO(vinculo);
    }

    /** Sem vínculo ativo com a escola, os acordos do motorista com os alunos dela deixam de valer. */
    private void encerrarAcordosDoVinculo(ViagemEscola vinculo) {
        viagemDependenteRepository.desativarAcordosDoMotoristaNaEscola(
                vinculo.getMotorista().getId(), vinculo.getEscola().getId());
    }

    private void validarDocumentosDoMotorista(Motorista motorista) {
        if (motorista.getVeiculo() == null) {
            throw new BusinessException("O motorista precisa cadastrar um veículo antes de ser aprovado pela escola");
        }
        List<String> pendencias = documentoMotoristaService.listarPendenciasParaInicio(motorista.getId());
        if (!pendencias.isEmpty()) {
            throw new BusinessException("Não é possível aprovar o motorista. " + String.join("; ", pendencias));
        }
    }

    @Transactional(readOnly = true)
    public List<VinculoEscolaDTO> listarDoMotorista(Long usuarioMotoristaId) {
        Motorista motorista = buscarMotoristaDoUsuario(usuarioMotoristaId);
        return vinculoRepository.listarDoMotorista(motorista.getId()).stream()
                .map(VinculoEscolaDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VinculoEscolaDTO> listarPendentesDaEscola(Long usuarioEscolaId) {
        Escola escola = buscarEscolaDoUsuario(usuarioEscolaId);
        return vinculoRepository.listarDaEscolaPorStatus(
                        escola.getId(), StatusVinculoEscola.PENDENTE_ESCOLA).stream()
                .map(VinculoEscolaDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VinculoEscolaDTO> listarPendentesDoMotorista(Long usuarioMotoristaId) {
        Motorista motorista = buscarMotoristaDoUsuario(usuarioMotoristaId);
        return vinculoRepository.listarDoMotoristaPorStatus(
                        motorista.getId(), StatusVinculoEscola.PENDENTE_MOTORISTA).stream()
                .map(VinculoEscolaDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VinculoEscolaDTO> listarDaEscola(Long usuarioEscolaId) {
        Escola escola = buscarEscolaDoUsuario(usuarioEscolaId);
        return vinculoRepository.listarDaEscola(escola.getId()).stream()
                .map(VinculoEscolaDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoVinculoEscolaDTO> listarHistorico(Long usuarioId, Long vinculoId) {
        ViagemEscola vinculo = vinculoRepository.findById(vinculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo não encontrado"));
        validarParteDoVinculo(vinculo, usuarioId);
        return historicoRepository.findByVinculo_IdOrderByOcorridoEmAscIdAsc(vinculoId).stream()
                .map(HistoricoVinculoEscolaDTO::toDTO)
                .toList();
    }

    private VinculoEscolaDTO iniciarCiclo(Escola escola, Motorista motorista,
                                           StatusVinculoEscola estadoInicial, Usuario ator) {
        if (motorista.getId() == null || escola.getId() == null) {
            throw new BusinessException("Motorista e escola devem estar cadastrados");
        }
        boolean existePendente = vinculoRepository.existsByEscola_IdAndMotorista_IdAndStatusIn(
                escola.getId(), motorista.getId(), STATUS_PENDENTES);
        if (existePendente) {
            throw new ConflictException("Já existe uma solicitação pendente para este par");
        }
        boolean existeAtivo = vinculoRepository.existsByEscola_IdAndMotorista_IdAndStatusIn(
                escola.getId(), motorista.getId(), STATUS_ATIVOS);
        if (existeAtivo) {
            throw new ConflictException("Já existe um vínculo ativo ou suspenso para este par");
        }

        Instant agora = Instant.now();
        ViagemEscola vinculo = new ViagemEscola();
        vinculo.setEscola(escola);
        vinculo.setMotorista(motorista);
        vinculo.setStatus(estadoInicial);
        vinculo.setCriadoPor(ator);
        vinculo.setCriadoEm(agora);
        vinculo.setAtualizadoEm(agora);
        vinculo = vinculoRepository.save(vinculo);
        registrarHistorico(vinculo, null, estadoInicial, ator, null, agora);
        return VinculoEscolaDTO.toDTO(vinculo);
    }

    private ViagemEscola buscarVinculoParaAtualizacao(Long vinculoId) {
        return vinculoRepository.findByIdForUpdate(vinculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo não encontrado"));
    }

    private void alterarEstado(ViagemEscola vinculo, StatusVinculoEscola novoStatus,
                               Usuario ator, String motivo) {
        StatusVinculoEscola anterior = vinculo.getStatus();
        Instant agora = Instant.now();
        vinculo.setStatus(novoStatus);
        vinculo.setAtualizadoEm(agora);
        vinculoRepository.save(vinculo);
        registrarHistorico(vinculo, anterior, novoStatus, ator, motivo, agora);
    }

    private void registrarHistorico(ViagemEscola vinculo, StatusVinculoEscola anterior,
                                    StatusVinculoEscola novo, Usuario ator, String motivo, Instant instante) {
        HistoricoVinculoEscola historico = new HistoricoVinculoEscola();
        historico.setVinculo(vinculo);
        historico.setStatusAnterior(anterior);
        historico.setStatusNovo(novo);
        historico.setAtor(ator);
        historico.setMotivo(motivo);
        historico.setOcorridoEm(instante);
        historicoRepository.save(historico);
    }

    private Usuario validarEscolaDoVinculo(ViagemEscola vinculo, Long usuarioId) {
        Escola escola = buscarEscolaDoUsuario(usuarioId);
        if (vinculo.getEscola() == null || !escola.getId().equals(vinculo.getEscola().getId())) {
            throw new AccessDeniedException("A escola não pertence a este vínculo");
        }
        return exigirUsuario(escola.getUsuario());
    }

    private Usuario validarMotoristaDoVinculo(ViagemEscola vinculo, Long usuarioId) {
        Motorista motorista = buscarMotoristaDoUsuario(usuarioId);
        if (vinculo.getMotorista() == null || !motorista.getId().equals(vinculo.getMotorista().getId())) {
            throw new AccessDeniedException("O motorista não pertence a este vínculo");
        }
        return exigirUsuario(motorista.getUsuario());
    }

    private Usuario validarParteDoVinculo(ViagemEscola vinculo, Long usuarioId) {
        Optional<Escola> escola = escolaRepository.findByUsuarioId(usuarioId);
        if (escola.isPresent() && vinculo.getEscola() != null
                && escola.get().getId().equals(vinculo.getEscola().getId())) {
            return exigirUsuario(escola.get().getUsuario());
        }
        Optional<Motorista> motorista = motoristaRepository.findByUsuarioId(usuarioId);
        if (motorista.isPresent() && vinculo.getMotorista() != null
                && motorista.get().getId().equals(vinculo.getMotorista().getId())) {
            return exigirUsuario(motorista.get().getUsuario());
        }
        throw new AccessDeniedException("O usuário autenticado não participa deste vínculo");
    }

    private void validarEstado(ViagemEscola vinculo, StatusVinculoEscola esperado) {
        if (vinculo.getStatus() != esperado) {
            throw new ConflictException("A ação não é permitida no estado atual do vínculo");
        }
    }

    private String motivoObrigatorioSeAplicavel(String motivo, boolean obrigatorio, String acao) {
        String normalizado = motivo == null ? null : motivo.trim();
        if (normalizado != null && normalizado.isEmpty()) {
            normalizado = null;
        }
        if (normalizado != null && normalizado.length() > 500) {
            throw new BusinessException("O motivo deve ter no máximo 500 caracteres");
        }
        if (obrigatorio && normalizado == null) {
            throw new BusinessException("Informe o motivo para " + acao + " o vínculo");
        }
        return normalizado;
    }

    private Motorista buscarMotoristaDoUsuario(Long usuarioId) {
        return motoristaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Motorista autenticado não encontrado"));
    }

    private Escola buscarEscolaDoUsuario(Long usuarioId) {
        return escolaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Escola autenticada não encontrada"));
    }

    private Usuario exigirUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new UnauthorizedException("O perfil autenticado não possui usuário válido");
        }
        return usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));
    }
}

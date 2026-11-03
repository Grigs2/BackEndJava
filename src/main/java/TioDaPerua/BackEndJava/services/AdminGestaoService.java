package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.AdminAuditoriaDTO;
import TioDaPerua.BackEndJava.DTOs.AdminEscolaResumoDTO;
import TioDaPerua.BackEndJava.DTOs.AdminUsuarioResumoDTO;
import TioDaPerua.BackEndJava.DTOs.AdminVinculoMotoristaEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.AdminVinculoResponsavelDTO;
import TioDaPerua.BackEndJava.entities.AuditoriaAdministrativa;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.StatusConviteResponsavel;
import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.AdminVinculoResponsavelProjection;
import TioDaPerua.BackEndJava.repositories.AuditoriaAdministrativaRepository;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.RefreshTokenRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemEscolaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;

@Service
public class AdminGestaoService {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final UsuarioRepository usuarioRepository;
    private final EscolaRepository escolaRepository;
    private final ViagemEscolaRepository viagemEscolaRepository;
    private final ResponsavelRepository responsavelRepository;
    private final AuditoriaAdministrativaRepository auditoriaRepository;
    private final AuditoriaAdministrativaService auditoriaService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;

    public AdminGestaoService(
            UsuarioRepository usuarioRepository,
            EscolaRepository escolaRepository,
            ViagemEscolaRepository viagemEscolaRepository,
            ResponsavelRepository responsavelRepository,
            AuditoriaAdministrativaRepository auditoriaRepository,
            AuditoriaAdministrativaService auditoriaService,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenService jwtTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.escolaRepository = escolaRepository;
        this.viagemEscolaRepository = viagemEscolaRepository;
        this.responsavelRepository = responsavelRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.auditoriaService = auditoriaService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
    }

    public Page<AdminUsuarioResumoDTO> pesquisarUsuarios(
            Long atorId, Long id, String tipoPerfil, String email, Boolean suspensa, int pagina, int tamanho) {
        Page<Usuario> resultado = usuarioRepository.pesquisarAdministrativamente(
                id, normalizar(tipoPerfil), normalizarEmail(email), suspensa, pageable(pagina, tamanho));
        auditoriaService.registrarConsulta(atorId, "USUARIOS_PESQUISADOS", "USUARIO");
        return resultado.map(usuario -> new AdminUsuarioResumoDTO(
                usuario.getId(), usuario.getTipoPerfil(),
                Boolean.TRUE.equals(usuario.getContaSuspensa()), mascararEmail(usuario.getEmail())));
    }

    public Page<AdminEscolaResumoDTO> pesquisarEscolas(
            Long atorId, String nome, String cnpj, Boolean suspensa, int pagina, int tamanho) {
        String nomeNormalizado = normalizar(nome);
        if (nomeNormalizado != null && nomeNormalizado.length() > 100) {
            throw new BusinessException("O nome de pesquisa deve ter até 100 caracteres");
        }
        String cnpjNormalizado = null;
        if (cnpj != null && !cnpj.isBlank()) {
            cnpjNormalizado = DocumentoFiscalValidator.normalizarCnpj(cnpj);
            if (!DocumentoFiscalValidator.cnpjValido(cnpjNormalizado)) {
                throw new BusinessException("CNPJ inválido para pesquisa");
            }
        }
        Page<Escola> resultado = escolaRepository.pesquisarAdministrativamente(
                nomeNormalizado, cnpjNormalizado, suspensa, pageable(pagina, tamanho));
        auditoriaService.registrarConsulta(atorId, "ESCOLAS_PESQUISADAS", "ESCOLA");
        return resultado.map(escola -> new AdminEscolaResumoDTO(
                escola.getId(), escola.getNome(), escola.getCnpj(),
                escola.getUsuario() == null ? null : escola.getUsuario().getId(),
                escola.getUsuario() != null && Boolean.TRUE.equals(escola.getUsuario().getContaSuspensa())));
    }

    public Page<AdminVinculoMotoristaEscolaDTO> pesquisarVinculosMotoristaEscola(
            Long atorId, Long escolaId, Long motoristaId, StatusVinculoEscola status,
            int pagina, int tamanho) {
        var resultado = viagemEscolaRepository.pesquisarAdministrativamente(
                escolaId, motoristaId, status, pageable(pagina, tamanho));
        auditoriaService.registrarConsulta(atorId, "VINCULOS_MOTORISTA_ESCOLA_PESQUISADOS", "VINCULO_ESCOLA");
        return resultado.map(vinculo -> new AdminVinculoMotoristaEscolaDTO(
                vinculo.getId(),
                vinculo.getEscola() == null ? null : vinculo.getEscola().getId(),
                vinculo.getMotorista() == null ? null : vinculo.getMotorista().getId(),
                vinculo.getStatus(), vinculo.getCriadoEm(), vinculo.getAtualizadoEm()));
    }

    public Page<AdminVinculoResponsavelDTO> pesquisarVinculosResponsavelDependente(
            Long atorId, Long dependenteId, Long responsavelId, int pagina, int tamanho) {
        Page<AdminVinculoResponsavelProjection> resultado =
                responsavelRepository.pesquisarVinculosAdministrativamente(
                        dependenteId, responsavelId, StatusConviteResponsavel.ACEITO,
                        pageable(pagina, tamanho));
        auditoriaService.registrarConsulta(
                atorId, "VINCULOS_RESPONSAVEL_DEPENDENTE_PESQUISADOS", "VINCULO_RESPONSAVEL");
        return resultado.map(vinculo -> new AdminVinculoResponsavelDTO(
                vinculo.getDependenteId(), vinculo.getResponsavelId(), "ATIVO",
                vinculo.getCategoria() == null ? null : vinculo.getCategoria().name(),
                Boolean.TRUE.equals(vinculo.getResponsavelCriador()), vinculo.getVinculadoEm()));
    }

    public Page<AdminAuditoriaDTO> pesquisarAuditoria(
            Long atorId,
            Long filtroAtorId,
            String acao,
            String tipoAlvo,
            Long alvoId,
            Instant inicio,
            Instant fim,
            int pagina,
            int tamanho) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new BusinessException("O início do intervalo não pode ser posterior ao fim");
        }
        String acaoNormalizada = normalizar(acao);
        String tipoAlvoNormalizado = normalizar(tipoAlvo);
        Page<AuditoriaAdministrativa> resultado = auditoriaRepository.pesquisar(
                filtroAtorId, acaoNormalizada, tipoAlvoNormalizado, alvoId, inicio, fim,
                pageable(pagina, tamanho));
        auditoriaService.registrarConsulta(atorId, "AUDITORIA_PESQUISADA", "AUDITORIA_ADMINISTRATIVA");
        return resultado.map(evento -> new AdminAuditoriaDTO(
                evento.getId(), evento.getAtor().getId(), evento.getAcao(),
                evento.getTipoAlvo(), evento.getAlvoId(), evento.getResultado(),
                evento.getMotivo(), evento.getOcorridoEm()));
    }

    @Transactional
    public void suspenderUsuario(Long atorId, Long usuarioId, String motivo) {
        if (atorId == null || usuarioId == null || motivo == null || motivo.isBlank()
                || motivo.trim().length() > 1000) {
            throw new BusinessException("Usuário e motivo de suspensão são obrigatórios");
        }
        if (atorId.equals(usuarioId)) {
            throw new AccessDeniedException("O administrador não pode suspender a própria conta");
        }

        List<Usuario> administradores = usuarioRepository.bloquearAdministradores();
        Usuario usuario = usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (Boolean.TRUE.equals(usuario.getContaSuspensa())) {
            throw new ConflictException("A conta já está suspensa");
        }
        if ("ADMIN".equals(usuario.getTipoPerfil())
                && administradores.stream().filter(admin -> !Boolean.TRUE.equals(admin.getContaSuspensa())).count() <= 1) {
            throw new ConflictException("Não é permitido suspender o último administrador ativo");
        }

        usuario.setContaSuspensa(true);
        usuarioRepository.save(usuario);
        refreshTokenRepository.revogarSessoesDoUsuario(usuarioId, jwtTokenService.now());
        auditoriaService.registrarAlteracao(
                atorId, "USUARIO_SUSPENSO", "USUARIO", usuarioId, "SUCESSO", motivo.trim());
    }

    @Transactional
    public void reativarUsuario(Long atorId, Long usuarioId, String motivo) {
        if (atorId == null || usuarioId == null || (motivo != null && motivo.trim().length() > 1000)) {
            throw new BusinessException("Usuário e motivo válido são obrigatórios");
        }
        if (atorId.equals(usuarioId)) {
            throw new AccessDeniedException("O administrador não pode reativar a própria conta");
        }
        Usuario usuario = usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (!Boolean.TRUE.equals(usuario.getContaSuspensa())) {
            throw new ConflictException("A conta não está suspensa");
        }
        usuario.setContaSuspensa(false);
        usuarioRepository.save(usuario);
        auditoriaService.registrarAlteracao(
                atorId, "USUARIO_REATIVADO", "USUARIO", usuarioId, "SUCESSO",
                normalizar(motivo));
    }

    private Pageable pageable(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new BusinessException("A paginação deve usar página não negativa e tamanho entre 1 e 100");
        }
        return PageRequest.of(pagina, tamanho);
    }

    private String normalizar(String filtro) {
        return filtro == null || filtro.isBlank() ? null : filtro.trim();
    }

    private String normalizarEmail(String email) {
        String normalizado = normalizar(email);
        if (normalizado != null && normalizado.length() > 254) {
            throw new BusinessException("E-mail de pesquisa inválido");
        }
        return normalizado;
    }

    private String mascararEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int separador = email.indexOf('@');
        if (separador <= 0 || separador == email.length() - 1) {
            return "***";
        }
        String dominio = email.substring(separador + 1);
        int ponto = dominio.lastIndexOf('.');
        String dominioMascarado = dominio.charAt(0) + "***"
                + (ponto >= 0 ? dominio.substring(ponto) : "");
        return email.charAt(0) + "***@" + dominioMascarado;
    }
}

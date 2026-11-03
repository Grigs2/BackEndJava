package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.AuditoriaAdministrativa;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.AuditoriaAdministrativaRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuditoriaAdministrativaService {

    private final AuditoriaAdministrativaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaAdministrativaService(
            AuditoriaAdministrativaRepository auditoriaRepository,
            UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrarAlteracao(
            Long atorUsuarioId,
            String acao,
            String tipoAlvo,
            Long alvoId,
            String resultado,
            String motivo) {
        salvar(atorUsuarioId, acao, tipoAlvo, alvoId, resultado, motivo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarConsulta(Long atorUsuarioId, String acao, String tipoAlvo) {
        salvar(atorUsuarioId, acao, tipoAlvo, null, "SUCESSO", null);
    }

    private void salvar(
            Long atorUsuarioId,
            String acao,
            String tipoAlvo,
            Long alvoId,
            String resultado,
            String motivo) {
        if (atorUsuarioId == null || acao == null || acao.isBlank()
                || tipoAlvo == null || tipoAlvo.isBlank()
                || resultado == null || resultado.isBlank()
                || acao.length() > 60 || tipoAlvo.length() > 60 || resultado.length() > 30
                || (motivo != null && motivo.length() > 1000)) {
            throw new BusinessException("Dados inválidos para registro da auditoria administrativa");
        }

        Usuario ator = usuarioRepository.findById(atorUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado"));
        if (!"ADMIN".equals(ator.getTipoPerfil())) {
            throw new AccessDeniedException("Somente ADMIN pode registrar operações administrativas");
        }
        AuditoriaAdministrativa evento = new AuditoriaAdministrativa();
        evento.setAtor(ator);
        evento.setAcao(acao);
        evento.setTipoAlvo(tipoAlvo);
        evento.setAlvoId(alvoId);
        evento.setResultado(resultado);
        evento.setMotivo(motivo);
        evento.setOcorridoEm(Instant.now());
        auditoriaRepository.save(evento);
    }

    @Scheduled(cron = "0 45 3 * * *", zone = "UTC")
    @Transactional
    public void expurgarAuditoriasAntigas() {
        auditoriaRepository.deleteByOcorridoEmBefore(Instant.now().minus(365, ChronoUnit.DAYS));
    }
}

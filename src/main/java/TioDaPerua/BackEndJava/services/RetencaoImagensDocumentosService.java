package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RetencaoImagensDocumentosService {

    static final Duration PRAZO_RETENCAO = Duration.ofDays(30);

    private static final Logger log = LoggerFactory.getLogger(RetencaoImagensDocumentosService.class);

    private final UsuarioRepository usuarioRepository;
    private final ImagemDocumentoMotoristaService imagemService;

    public RetencaoImagensDocumentosService(UsuarioRepository usuarioRepository,
                                            ImagemDocumentoMotoristaService imagemService) {
        this.usuarioRepository = usuarioRepository;
        this.imagemService = imagemService;
    }

    @Scheduled(cron = "0 40 3 * * *", zone = "UTC")
    public void purgarImagensDeContasSuspensas() {
        Instant agora = Instant.now();
        int legadas = usuarioRepository.iniciarContagemDeSuspensoesAnteriores(agora);
        if (legadas > 0) {
            log.info("Contagem de retenção iniciada para {} conta(s) suspensa(s) antes do deploy", legadas);
        }

        Instant limite = agora.minus(PRAZO_RETENCAO);
        for (Long usuarioId : usuarioRepository.findIdsSuspensosDesde(limite)) {
            try {
                int removidas = imagemService.purgarImagensDeContaSuspensaDesde(usuarioId, limite);
                if (removidas > 0) {
                    log.info("Retenção de 30 dias aplicada: {} imagem(ns) de documento removida(s)", removidas);
                }
            } catch (RuntimeException e) {
                log.error("Falha ao purgar imagens de documentos de uma conta suspensa", e);
            }
        }
    }
}

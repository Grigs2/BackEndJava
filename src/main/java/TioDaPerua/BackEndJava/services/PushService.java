package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.DispositivoPush;
import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.DispositivoPushRepository;
import TioDaPerua.BackEndJava.repositories.NotificacaoRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Push pelo Expo Push Service (https://exp.host/--/api/v2/push/send).
 *
 * O aviso in-app já está gravado quando o push é tentado: o envio acontece depois do commit, em outra thread, e
 * uma falha aqui nunca desfaz nem bloqueia o evento de negócio. Tokens que o Expo informa como inválidos
 * (DeviceNotRegistered) são desativados.
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    private static final Pattern TOKEN = Pattern.compile("^Expo(nent)?PushToken\\[[A-Za-z0-9_\\-]+\\]$");
    private static final int LOTE = 100;

    private final DispositivoPushRepository dispositivoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final boolean habilitado;
    private final RestClient restClient;

    public PushService(DispositivoPushRepository dispositivoRepository,
                       NotificacaoRepository notificacaoRepository,
                       UsuarioRepository usuarioRepository,
                       @Value("${app.push.enabled:true}") boolean habilitado,
                       @Value("${app.push.expo-url:https://exp.host/--/api/v2/push/send}") String expoUrl) {
        this.dispositivoRepository = dispositivoRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.habilitado = habilitado;
        this.restClient = RestClient.builder().baseUrl(expoUrl).build();
    }

    /** Registra (ou transfere para este usuário) o token do aparelho. */
    @Transactional
    public void registrar(Long usuarioId, String expoToken, String plataforma) {
        if (expoToken == null || !TOKEN.matcher(expoToken.trim()).matches()) {
            throw new BusinessException("Token de notificação inválido");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        DispositivoPush d = dispositivoRepository.findByExpoToken(expoToken.trim()).orElseGet(() -> {
            DispositivoPush novo = new DispositivoPush();
            novo.setExpoToken(expoToken.trim());
            novo.setCriadoEm(Instant.now());
            return novo;
        });
        d.setUsuario(usuario);
        d.setPlataforma(plataforma == null ? null : plataforma.trim().toLowerCase());
        d.setAtivo(true);
        d.setUltimoUsoEm(Instant.now());
        dispositivoRepository.save(d);
    }

    /** Desativa o token (ao sair da conta). Só o dono do token pode desativá-lo. */
    @Transactional
    public void remover(Long usuarioId, String expoToken) {
        dispositivoRepository.findByExpoToken(expoToken == null ? "" : expoToken.trim())
                .filter(d -> d.getUsuario() != null && usuarioId.equals(d.getUsuario().getId()))
                .ifPresent(d -> {
                    d.setAtivo(false);
                    dispositivoRepository.save(d);
                });
    }

    @Async("pushExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoCriarNotificacoes(NotificacaoService.NotificacoesCriadasEvento evento) {
        if (!habilitado || evento == null || evento.notificacaoIds().isEmpty()) return;
        try {
            enviar(notificacaoRepository.findAllById(evento.notificacaoIds()));
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar push: {}", e.getMessage());
        }
    }

    /** Monta as mensagens (uma por aparelho ativo do destinatário) e envia em lotes de até 100. */
    void enviar(List<Notificacao> notificacoes) {
        Map<Long, List<Notificacao>> porUsuario = notificacoes.stream()
                .filter(n -> n.getDestinatario() != null)
                .collect(Collectors.groupingBy(n -> n.getDestinatario().getId()));
        if (porUsuario.isEmpty()) return;
        List<DispositivoPush> dispositivos = dispositivoRepository.findAtivosDosUsuarios(porUsuario.keySet());

        List<Map<String, Object>> mensagens = new ArrayList<>();
        List<DispositivoPush> alvo = new ArrayList<>();
        for (DispositivoPush d : dispositivos) {
            for (Notificacao n : porUsuario.getOrDefault(d.getUsuario().getId(), List.of())) {
                mensagens.add(mensagem(d.getExpoToken(), n));
                alvo.add(d);
            }
        }
        for (int i = 0; i < mensagens.size(); i += LOTE) {
            int fim = Math.min(i + LOTE, mensagens.size());
            processarResposta(post(mensagens.subList(i, fim)), alvo.subList(i, fim));
        }
    }

    static Map<String, Object> mensagem(String token, Notificacao n) {
        Map<String, Object> data = new HashMap<>();
        data.put("notificacaoId", n.getId());
        data.put("tipo", n.getTipo() == null ? "GERAL" : n.getTipo());
        if (n.getViagemDiaId() != null) data.put("viagemDiaId", n.getViagemDiaId());
        if (n.getDependenteId() != null) data.put("dependenteId", n.getDependenteId());
        Map<String, Object> m = new HashMap<>();
        m.put("to", token);
        m.put("title", n.getTitulo());
        m.put("body", n.getMensagem());
        m.put("sound", "default");
        m.put("priority", "ALTA".equals(n.getPrioridade()) ? "high" : "default");
        m.put("channelId", "ALTA".equals(n.getPrioridade()) ? "alertas" : "viagem");
        m.put("data", data);
        return m;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(List<Map<String, Object>> lote) {
        return restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(lote)
                .retrieve()
                .body(Map.class);
    }

    /** Desativa tokens que o Expo devolveu como "DeviceNotRegistered". */
    @SuppressWarnings("unchecked")
    void processarResposta(Map<String, Object> resposta, List<DispositivoPush> alvo) {
        Object data = resposta == null ? null : resposta.get("data");
        if (!(data instanceof List<?> tickets)) return;
        for (int i = 0; i < tickets.size() && i < alvo.size(); i++) {
            if (!(tickets.get(i) instanceof Map<?, ?> t) || !"error".equals(t.get("status"))) continue;
            Object details = t.get("details");
            String erro = details instanceof Map<?, ?> det ? String.valueOf(det.get("error")) : null;
            if ("DeviceNotRegistered".equals(erro)) {
                DispositivoPush d = alvo.get(i);
                d.setAtivo(false);
                dispositivoRepository.save(d);
            } else {
                log.warn("Expo recusou o push: {}", t.get("message"));
            }
        }
    }
}

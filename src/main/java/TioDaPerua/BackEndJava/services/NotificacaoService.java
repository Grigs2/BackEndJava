package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.TipoNotificacao;
import TioDaPerua.BackEndJava.repositories.NotificacaoRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificacaoService {

    private static final long RETENCAO_DIAS = 90;
    public static final int TAMANHO_PAGINA_PADRAO = 30;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Dados de um aviso automático: tipo (define prioridade e grupo), viagem do dia e aluno relacionados e uma chave
     * de evento. Com chave, cada destinatário recebe no máximo um aviso daquele evento (ex.: embarque repetido por
     * sincronização offline não gera dois avisos).
     */
    public record Contexto(TipoNotificacao tipo, Long viagemDiaId, Long dependenteId, String chaveEvento) {
        public static Contexto de(TipoNotificacao tipo) {
            return new Contexto(tipo, null, null, null);
        }
    }

    /** Evento publicado após gravar avisos; o envio de push o consome depois do commit. */
    public record NotificacoesCriadasEvento(List<Long> notificacaoIds) {
    }

    public Notificacao enviarNotificacao(Notificacao notificacao, Long remetenteId, Long destinatarioId) {
        if (notificacao == null) throw new IllegalArgumentException("Notificação não pode ser nula");
        if (notificacao.getTitulo() == null || notificacao.getTitulo().isEmpty()) throw new BusinessException("Título é obrigatório");
        if (notificacao.getMensagem() == null || notificacao.getMensagem().isEmpty()) throw new BusinessException("Mensagem é obrigatória");
        if (remetenteId == null) throw new BusinessException("Remetente é obrigatório");
        if (destinatarioId == null) throw new BusinessException("Destinatário é obrigatório");

        Usuario remetente = usuarioRepository.findById(remetenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Remetente não encontrado com id: " + remetenteId));
        Usuario destinatario = usuarioRepository.findById(destinatarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Destinatário não encontrado com id: " + destinatarioId));

        notificacao.setData(LocalDateTime.now());
        notificacao.setVisto(false);
        notificacao.setRemetente(remetente);
        notificacao.setDestinatario(destinatario);
        notificacao.setTipo(TipoNotificacao.MANUAL.name());
        notificacao.setPrioridade(TipoNotificacao.MANUAL.getPrioridade().name());

        Notificacao salva = notificacaoRepository.save(notificacao);
        publicar(List.of(salva));
        return salva;
    }

    public List<Notificacao> listarNaoVistasPorUsuario(Long usuarioId) {
        if (usuarioId == null) throw new IllegalArgumentException("ID do usuário é obrigatório");
        return notificacaoRepository.findNaoVistasByDestinatarioId(usuarioId);
    }

    /**
     * Central de notificações.
     *
     * @param somenteNaoLidas true para a aba "Não lidas"
     * @param grupo           VIAGEM, ALERTA, SOLICITACAO ou AVISO; nulo para todos
     * @param pagina          começa em 0
     */
    @Transactional(readOnly = true)
    public List<Notificacao> listar(Long usuarioId, boolean somenteNaoLidas, String grupo, int pagina, int tamanho) {
        if (usuarioId == null) throw new IllegalArgumentException("ID do usuário é obrigatório");
        int tamanhoValido = tamanho <= 0 || tamanho > 100 ? TAMANHO_PAGINA_PADRAO : tamanho;
        PageRequest page = PageRequest.of(Math.max(pagina, 0), tamanhoValido);
        if (grupo == null || grupo.isBlank()) {
            return notificacaoRepository.buscarDoUsuario(usuarioId, somenteNaoLidas, page);
        }
        TipoNotificacao.Grupo g;
        try {
            g = TipoNotificacao.Grupo.valueOf(grupo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Grupo de notificação inválido: " + grupo);
        }
        return notificacaoRepository.buscarDoUsuarioPorTipos(usuarioId, somenteNaoLidas, TipoNotificacao.nomesDoGrupo(g), page);
    }

    public long contarNaoLidas(Long usuarioId) {
        return notificacaoRepository.contarNaoLidas(usuarioId);
    }

    @Transactional
    public int marcarTodasLidas(Long usuarioId) {
        return notificacaoRepository.marcarTodasLidas(usuarioId, LocalDateTime.now());
    }

    public void visualizarNotificacao(Long id) {
        if (id == null) throw new IllegalArgumentException("ID da notificação é obrigatório");
        Notificacao n = notificacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada com id: " + id));
        if (!Boolean.TRUE.equals(n.getVisto())) {
            n.setVisto(true);
            n.setLidaEm(LocalDateTime.now());
            notificacaoRepository.save(n);
        }
    }

    /** Aviso automático sem tipo específico (mantido por compatibilidade). */
    @Transactional
    public void criarNotificacoesAutomaticas(
            Long remetenteId, Collection<Long> destinatariosIds, String titulo, String mensagem) {
        criarNotificacoesAutomaticas(remetenteId, destinatariosIds, titulo, mensagem, Contexto.de(TipoNotificacao.GERAL));
    }

    @Transactional
    public void criarNotificacoesAutomaticas(
            Long remetenteId, Collection<Long> destinatariosIds, String titulo, String mensagem, Contexto contexto) {
        if (remetenteId == null || destinatariosIds == null || titulo == null || titulo.isBlank()
                || mensagem == null || mensagem.isBlank()) {
            throw new BusinessException("Remetente, destinatários, título e mensagem são obrigatórios");
        }
        if (destinatariosIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException("Os destinatários das notificações não podem ser nulos");
        }
        Contexto ctx = contexto != null ? contexto : Contexto.de(TipoNotificacao.GERAL);

        Set<Long> destinatarioIds = new LinkedHashSet<>(destinatariosIds);
        destinatarioIds.remove(remetenteId);
        if (ctx.chaveEvento() != null) {
            destinatarioIds.removeIf(id -> notificacaoRepository.existsByChaveDedup(chave(ctx, id)));
        }
        if (destinatarioIds.isEmpty()) {
            return;
        }

        Usuario remetente = usuarioRepository.findById(remetenteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Remetente não encontrado com id: " + remetenteId));
        List<Usuario> destinatarios = usuarioRepository.findAllById(destinatarioIds);
        Map<Long, Usuario> destinatariosPorId = destinatarios.stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        if (!destinatariosPorId.keySet().containsAll(destinatarioIds)) {
            throw new ResourceNotFoundException("Um ou mais destinatários de notificação não foram encontrados");
        }

        LocalDateTime agora = LocalDateTime.now();
        List<Notificacao> notificacoes = destinatarioIds.stream()
                .map(destinatarioId -> {
                    Notificacao notificacao = new Notificacao();
                    notificacao.setTitulo(titulo);
                    notificacao.setMensagem(mensagem);
                    notificacao.setData(agora);
                    notificacao.setVisto(false);
                    notificacao.setRemetente(remetente);
                    notificacao.setDestinatario(destinatariosPorId.get(destinatarioId));
                    notificacao.setTipo(ctx.tipo().name());
                    notificacao.setPrioridade(ctx.tipo().getPrioridade().name());
                    notificacao.setViagemDiaId(ctx.viagemDiaId());
                    notificacao.setDependenteId(ctx.dependenteId());
                    notificacao.setChaveDedup(ctx.chaveEvento() == null ? null : chave(ctx, destinatarioId));
                    return notificacao;
                })
                .toList();
        List<Notificacao> salvas = notificacaoRepository.saveAll(notificacoes);
        publicar(salvas);
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    public void expurgarNotificacoesAntigas() {
        notificacaoRepository.deleteByDataBefore(LocalDateTime.now().minusDays(RETENCAO_DIAS));
    }

    private static String chave(Contexto ctx, Long destinatarioId) {
        String base = ctx.tipo().name() + ":" + ctx.chaveEvento() + ":" + destinatarioId;
        return base.length() > 160 ? base.substring(0, 160) : base;
    }

    private void publicar(Collection<Notificacao> salvas) {
        if (salvas == null || eventPublisher == null) return;
        List<Long> ids = salvas.stream().map(Notificacao::getId).filter(Objects::nonNull).toList();
        if (!ids.isEmpty()) {
            eventPublisher.publishEvent(new NotificacoesCriadasEvento(ids));
        }
    }
}

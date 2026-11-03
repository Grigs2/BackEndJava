package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.NotificacaoDTO;
import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.NotificacaoRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificacaoService {

    private static final long RETENCAO_DIAS = 90;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

        return notificacaoRepository.save(notificacao);
    }

    public List<Notificacao> listarNaoVistasPorUsuario(Long usuarioId) {
        if (usuarioId == null) throw new IllegalArgumentException("ID do usuário é obrigatório");
        return notificacaoRepository.findNaoVistasByDestinatarioId(usuarioId);
    }

    public void visualizarNotificacao(Long id) {
        if (id == null) throw new IllegalArgumentException("ID da notificação é obrigatório");
        Notificacao n = notificacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada com id: " + id));
        n.setVisto(true);
        notificacaoRepository.save(n);
    }

    @Transactional
    public void criarNotificacoesAutomaticas(
            Long remetenteId, Collection<Long> destinatariosIds, String titulo, String mensagem) {
        if (remetenteId == null || destinatariosIds == null || titulo == null || titulo.isBlank()
                || mensagem == null || mensagem.isBlank()) {
            throw new BusinessException("Remetente, destinatários, título e mensagem são obrigatórios");
        }
        if (destinatariosIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException("Os destinatários das notificações não podem ser nulos");
        }

        Set<Long> destinatarioIds = new LinkedHashSet<>(destinatariosIds);
        destinatarioIds.remove(remetenteId);
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
                    return notificacao;
                })
                .toList();
        notificacaoRepository.saveAll(notificacoes);
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    public void expurgarNotificacoesAntigas() {
        notificacaoRepository.deleteByDataBefore(LocalDateTime.now().minusDays(RETENCAO_DIAS));
    }
}

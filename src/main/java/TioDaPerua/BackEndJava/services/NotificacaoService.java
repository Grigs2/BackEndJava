package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.NotificacaoDTO;
import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.NotificacaoRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificacaoService {

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
}

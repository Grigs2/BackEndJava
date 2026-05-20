package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.NotificacaoDTO;
import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.NotificacaoRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificacaoService {

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Notificacao enviarNotificacao(Notificacao notificacao, Long remetenteId, Long destinatarioId) {
        if (notificacao == null) return null;

        Optional<Usuario> remetenteOpt = usuarioRepository.findById(remetenteId);
        Optional<Usuario> destinatarioOpt = usuarioRepository.findById(destinatarioId);

        if (remetenteOpt.isEmpty() || destinatarioOpt.isEmpty()) return null;

        notificacao.setData(LocalDateTime.now());
        notificacao.setVisto(false);
        notificacao.setRemetente(remetenteOpt.get());
        notificacao.setDestinatario(destinatarioOpt.get());

        return notificacaoRepository.save(notificacao);
    }

    public List<Notificacao> listarPorUsuario(Long usuarioId) {
        return notificacaoRepository.findByDestinatarioId(usuarioId);
    }
}

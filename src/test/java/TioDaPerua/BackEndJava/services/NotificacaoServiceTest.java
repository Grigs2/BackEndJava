package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.NotificacaoRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private NotificacaoService service;

    @Test
    void naoRepeteOMesmoEventoParaQuemJaFoiAvisadoEGravaTipo() {
        Usuario remetente = usuario(10L);
        Usuario destinatario = usuario(30L);
        var ctx = new NotificacaoService.Contexto(
                TioDaPerua.BackEndJava.enums.TipoNotificacao.EMBARQUE, 4L, 7L, "presenca:4:7:EMBARCADO");
        when(notificacaoRepository.existsByChaveDedup("EMBARQUE:presenca:4:7:EMBARCADO:20")).thenReturn(true);
        when(notificacaoRepository.existsByChaveDedup("EMBARQUE:presenca:4:7:EMBARCADO:30")).thenReturn(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(remetente));
        when(usuarioRepository.findAllById(Set.of(30L))).thenReturn(List.of(destinatario));
        when(notificacaoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.criarNotificacoesAutomaticas(10L, List.of(20L, 30L), "Embarque confirmado", "Enzo embarcou.", ctx);

        verify(notificacaoRepository).saveAll(argThat(notificacoes -> {
            List<Notificacao> salvas = new java.util.ArrayList<>();
            notificacoes.forEach(salvas::add);
            assertEquals(1, salvas.size());
            Notificacao n = salvas.get(0);
            assertEquals(30L, n.getDestinatario().getId());
            assertEquals("EMBARQUE", n.getTipo());
            assertEquals("NORMAL", n.getPrioridade());
            assertEquals(4L, n.getViagemDiaId());
            assertEquals(7L, n.getDependenteId());
            assertEquals("EMBARQUE:presenca:4:7:EMBARCADO:30", n.getChaveDedup());
            return true;
        }));
    }

    @Test
    void createsInAppNotificationsForDistinctRecipientsAndExcludesSender() {
        Usuario remetente = usuario(10L);
        Usuario primeiroDestinatario = usuario(20L);
        Usuario segundoDestinatario = usuario(30L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(remetente));
        when(usuarioRepository.findAllById(Set.of(20L, 30L)))
                .thenReturn(List.of(primeiroDestinatario, segundoDestinatario));
        when(notificacaoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.criarNotificacoesAutomaticas(
                10L, List.of(10L, 20L, 20L, 30L), "Atualização", "Uma atualização ocorreu.");

        verify(notificacaoRepository).saveAll(argThat(notificacoes -> {
            List<Notificacao> salvas = new java.util.ArrayList<>();
            notificacoes.forEach(salvas::add);
            assertEquals(2, salvas.size());
            assertEquals(Set.of(20L, 30L), salvas.stream()
                    .map(notificacao -> notificacao.getDestinatario().getId()).collect(java.util.stream.Collectors.toSet()));
            assertEquals("Atualização", salvas.get(0).getTitulo());
            assertEquals("Uma atualização ocorreu.", salvas.get(0).getMensagem());
            assertEquals(remetente, salvas.get(0).getRemetente());
            assertFalse(salvas.get(0).getVisto());
            assertNotNull(salvas.get(0).getData());
            return true;
        }));
    }

    @Test
    void expungesNotificationsOlderThanNinetyDays() {
        when(notificacaoRepository.deleteByDataBefore(any(LocalDateTime.class))).thenReturn(4);
        LocalDateTime antes = LocalDateTime.now();

        service.expurgarNotificacoesAntigas();

        verify(notificacaoRepository).deleteByDataBefore(argThat(limite ->
                !limite.isAfter(antes.minusDays(89))
                        && !limite.isBefore(antes.minusDays(91))));
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }
}

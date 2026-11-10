package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.DispositivoPush;
import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.repositories.DispositivoPushRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class PushServiceTest {

    @Test
    void mensagemDeAlertaUsaPrioridadeAltaECanalDeAlertas() {
        Notificacao n = new Notificacao();
        n.setId(9L);
        n.setTitulo("Viagem cancelada");
        n.setMensagem("A viagem de hoje foi cancelada.");
        n.setTipo("VIAGEM_CANCELADA");
        n.setPrioridade("ALTA");
        n.setViagemDiaId(4L);

        Map<String, Object> m = PushService.mensagem("ExponentPushToken[abc]", n);

        assertEquals("ExponentPushToken[abc]", m.get("to"));
        assertEquals("high", m.get("priority"));
        assertEquals("alertas", m.get("channelId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) m.get("data");
        assertEquals(9L, data.get("notificacaoId"));
        assertEquals("VIAGEM_CANCELADA", data.get("tipo"));
        assertEquals(4L, data.get("viagemDiaId"));
        assertFalse(data.containsKey("dependenteId"));
    }

    @Test
    void desativaAparelhoQueOExpoNaoReconheceMais() {
        DispositivoPushRepository repo = mock(DispositivoPushRepository.class);
        PushService service = new PushService(repo, null, null, true, "http://localhost:9/push");
        DispositivoPush ok = new DispositivoPush();
        DispositivoPush antigo = new DispositivoPush();

        service.processarResposta(Map.of("data", List.of(
                Map.of("status", "ok"),
                Map.of("status", "error", "details", Map.of("error", "DeviceNotRegistered")))), List.of(ok, antigo));

        assertTrue(ok.isAtivo());
        assertFalse(antigo.isAtivo());
        verify(repo).save(antigo);
        verifyNoMoreInteractions(repo);
    }
}

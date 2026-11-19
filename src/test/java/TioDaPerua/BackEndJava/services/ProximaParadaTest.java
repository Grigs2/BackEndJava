package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.DependenteParadaDTO;
import TioDaPerua.BackEndJava.DTOs.ParadaDTO;
import TioDaPerua.BackEndJava.enums.TipoParada;
import TioDaPerua.BackEndJava.enums.TipoNotificacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Regra do aviso "Você é o próximo": qual parada é a próxima da rota. */
class ProximaParadaTest {

    private static DependenteParadaDTO aluno(long id, String status) {
        return new DependenteParadaDTO(id, "Aluno " + id, "Resp", status, 100L + id);
    }

    private static ParadaDTO parada(int ordem, TipoParada tipo, DependenteParadaDTO... alunos) {
        return new ParadaDTO((long) ordem, ordem, tipo, "Parada " + ordem, "Rua " + ordem, List.of(alunos));
    }

    private static final ParadaDTO SAIDA = parada(0, TipoParada.MOTORISTA);

    @Test
    void naIdaAProximaEAPrimeiraCasaComAlunoEsperando() {
        List<ParadaDTO> rota = List.of(SAIDA,
                parada(1, TipoParada.RESPONSAVEL, aluno(1, "EMBARCADO")),
                parada(2, TipoParada.RESPONSAVEL, aluno(2, "ESPERANDO"), aluno(3, "ESPERANDO")),
                parada(3, TipoParada.ESCOLA, aluno(1, "EMBARCADO"), aluno(2, "ESPERANDO"), aluno(3, "ESPERANDO")));

        assertEquals(2, ViagemService.proximaParadaPendente(rota, true).ordem());
    }

    @Test
    void naIdaDepoisDeTodosEmbarcadosAProximaEAEscola() {
        List<ParadaDTO> rota = List.of(SAIDA,
                parada(1, TipoParada.RESPONSAVEL, aluno(1, "EMBARCADO")),
                parada(2, TipoParada.ESCOLA, aluno(1, "EMBARCADO")));

        assertEquals(TipoParada.ESCOLA, ViagemService.proximaParadaPendente(rota, true).tipoParada());
    }

    @Test
    void naVoltaAProximaCasaEAEntregaDeQuemEstaEmbarcado() {
        List<ParadaDTO> rota = List.of(SAIDA,
                parada(1, TipoParada.ESCOLA, aluno(1, "EMBARCADO"), aluno(2, "EMBARCADO")),
                parada(2, TipoParada.RESPONSAVEL, aluno(1, "DESEMBARCADO")),
                parada(3, TipoParada.RESPONSAVEL, aluno(2, "EMBARCADO")));

        assertEquals(3, ViagemService.proximaParadaPendente(rota, false).ordem());
    }

    @Test
    void naVoltaAntesDeSairDaEscolaNinguemEOProximoEmCasa() {
        List<ParadaDTO> rota = List.of(SAIDA,
                parada(1, TipoParada.ESCOLA, aluno(1, "ESPERANDO")),
                parada(2, TipoParada.RESPONSAVEL, aluno(1, "ESPERANDO")));

        assertEquals(TipoParada.ESCOLA, ViagemService.proximaParadaPendente(rota, false).tipoParada());
    }

    @Test
    void semPendentesNaoHaProxima() {
        List<ParadaDTO> rota = List.of(SAIDA,
                parada(1, TipoParada.RESPONSAVEL, aluno(1, "FALTOU")),
                parada(2, TipoParada.ESCOLA, aluno(1, "FALTOU")));

        assertNull(ViagemService.proximaParadaPendente(rota, true));
    }

    @Test
    void textoDoAvisoFalaEmBuscarNaIdaEDeixarNaVolta() {
        assertTrue(MensagensNotificacao.proximo("Ana Souza", null, true).mensagem().contains("buscar Ana"));
        assertTrue(MensagensNotificacao.proximo("Ana Souza", null, false).mensagem().contains("deixar Ana"));
        assertEquals(TipoNotificacao.Grupo.VIAGEM, TipoNotificacao.PROXIMO_ALUNO.getGrupo());
    }
}

package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.ViagemParada;
import TioDaPerua.BackEndJava.entities.ViagemParadaParticipante;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import TioDaPerua.BackEndJava.enums.TipoOperacaoParada;
import TioDaPerua.BackEndJava.enums.TipoParada;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Regra da ordem das paradas: cada aluno embarca antes de desembarcar; escola adiantada gera desembarque extra. */
class OrdemParadasEscolaTest {

    private static long seq = 1;

    @Test
    void ordemValidaNaoMudaNada() {
        ViagemParada saida = motorista();
        ViagemParada casaAna = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(1, "Ana Clara"));
        ViagemParada casaEnzo = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(2, "Enzo"));
        ViagemParada escola = parada(TipoParada.ESCOLA, TipoOperacaoParada.DESEMBARQUE, aluno(1, "Ana Clara"), aluno(2, "Enzo"));
        ViagemParada chegada = motorista();
        List<ViagemParada> rota = new ArrayList<>(List.of(saida, casaAna, casaEnzo, escola, chegada));

        assertTrue(ViagemService.separarDesembarquesAntecipados(rota).isEmpty());
        assertEquals(5, rota.size());
    }

    @Test
    void escolaAdiantadaCriaDesembarqueExtraNoFimAntesDaCasaDoMotorista() {
        ViagemParada saida = motorista();
        ViagemParada casaAna = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(1, "Ana Clara"));
        ViagemParada casaEnzo = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(2, "Enzo"));
        ViagemParada escola = parada(TipoParada.ESCOLA, TipoOperacaoParada.DESEMBARQUE, aluno(1, "Ana Clara"), aluno(2, "Enzo"));
        ViagemParada chegada = motorista();
        // Motorista leva a Ana à escola antes de buscar o Enzo.
        List<ViagemParada> rota = new ArrayList<>(List.of(saida, casaAna, escola, casaEnzo, chegada));

        List<ViagemParada> criadas = ViagemService.separarDesembarquesAntecipados(rota);

        assertEquals(1, criadas.size());
        ViagemParada extra = criadas.get(0);
        assertEquals(List.of(saida, casaAna, escola, casaEnzo, extra, chegada), rota);
        assertEquals(TipoParada.ESCOLA, extra.getTipoParada());
        assertEquals(TipoOperacaoParada.DESEMBARQUE, extra.getOperacao());
        assertEquals(escola.getEndereco(), extra.getEndereco());
        assertEquals(List.of(1L), ids(escola));
        assertEquals(List.of(2L), ids(extra));
    }

    @Test
    void reaproveitaDesembarqueDaMesmaEscolaQueJaEstaDepois() {
        ViagemParada saida = motorista();
        ViagemParada casaAna = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(1, "Ana"));
        ViagemParada casaEnzo = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(2, "Enzo"));
        ViagemParada casaJulia = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(3, "Julia"));
        ViagemParada escola = parada(TipoParada.ESCOLA, TipoOperacaoParada.DESEMBARQUE, aluno(1, "Ana"), aluno(3, "Julia"));
        ViagemParada escolaExtra = parada(TipoParada.ESCOLA, TipoOperacaoParada.DESEMBARQUE, aluno(2, "Enzo"));
        ViagemParada chegada = motorista();
        List<ViagemParada> rota = new ArrayList<>(List.of(saida, casaAna, escola, casaEnzo, casaJulia, escolaExtra, chegada));

        assertTrue(ViagemService.separarDesembarquesAntecipados(rota).isEmpty());
        assertEquals(List.of(1L), ids(escola));
        assertEquals(List.of(2L, 3L), ids(escolaExtra));
    }

    @Test
    void escolaAntesDeTodosOsEmbarquesERecusada() {
        ViagemParada escola = parada(TipoParada.ESCOLA, TipoOperacaoParada.DESEMBARQUE, aluno(1, "Ana"), aluno(2, "Enzo"));
        List<ViagemParada> rota = new ArrayList<>(List.of(motorista(), escola,
                parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(1, "Ana")),
                parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(2, "Enzo")),
                motorista()));

        BusinessException e = assertThrows(BusinessException.class,
                () -> ViagemService.separarDesembarquesAntecipados(rota));
        assertTrue(e.getMessage().contains("pelo menos um aluno"));
    }

    @Test
    void naVoltaCasaAntesDoEmbarqueNaEscolaERecusada() {
        ViagemParada escola = parada(TipoParada.ESCOLA, TipoOperacaoParada.EMBARQUE, aluno(1, "Ana"));
        ViagemParada casa = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.DESEMBARQUE, aluno(1, "Ana"));
        List<ViagemParada> rota = new ArrayList<>(List.of(motorista(), casa, escola, motorista()));

        BusinessException e = assertThrows(BusinessException.class,
                () -> ViagemService.separarDesembarquesAntecipados(rota));
        assertTrue(e.getMessage().contains("Ana"));
    }

    @Test
    void alunoQueFaltouNaoObrigaDesembarqueExtra() {
        ViagemParada casaAna = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, aluno(1, "Ana"));
        ViagemParadaParticipante enzoFaltou = aluno(2, "Enzo");
        enzoFaltou.getPresenca().setStatus(StatusViagemPresenca.FALTOU);
        ViagemParada casaEnzo = parada(TipoParada.RESPONSAVEL, TipoOperacaoParada.EMBARQUE, enzoFaltou);
        // Na escola, o Enzo aponta para a mesma presença (como no banco).
        ViagemParadaParticipante enzoNaEscola = aluno(2, "Enzo");
        enzoNaEscola.setPresenca(enzoFaltou.getPresenca());
        ViagemParada escola = parada(TipoParada.ESCOLA, TipoOperacaoParada.DESEMBARQUE, aluno(1, "Ana"), enzoNaEscola);
        List<ViagemParada> rota = new ArrayList<>(List.of(motorista(), casaAna, escola, casaEnzo, motorista()));

        assertTrue(ViagemService.separarDesembarquesAntecipados(rota).isEmpty());
    }

    // -------------------------------------------------------------- montagem

    private static ViagemParada motorista() {
        ViagemParada p = new ViagemParada();
        p.setId(seq++);
        p.setTipoParada(TipoParada.MOTORISTA);
        p.setOperacao(TipoOperacaoParada.EXTREMO);
        return p;
    }

    private static ViagemParada parada(TipoParada tipo, TipoOperacaoParada operacao, ViagemParadaParticipante... alunos) {
        ViagemParada p = new ViagemParada();
        p.setId(seq++);
        p.setTipoParada(tipo);
        p.setOperacao(operacao);
        p.setNomeLocal(tipo == TipoParada.ESCOLA ? "Escola: EMEF Pedro II" : "Casa");
        p.setEndereco(tipo == TipoParada.ESCOLA ? "Rua Bom Pastor, 500" : "Rua " + p.getId());
        for (ViagemParadaParticipante a : alunos) {
            a.setParada(p);
            p.getParticipantes().add(a);
        }
        return p;
    }

    private static ViagemParadaParticipante aluno(long id, String nome) {
        ViagemPresenca presenca = new ViagemPresenca();
        presenca.setId(100 + id);
        presenca.setStatus(StatusViagemPresenca.ESPERANDO);
        ViagemParadaParticipante p = new ViagemParadaParticipante();
        p.setPresenca(presenca);
        p.setDependenteIdSnapshot(id);
        p.setNomeDependenteSnapshot(nome);
        return p;
    }

    private static List<Long> ids(ViagemParada parada) {
        return parada.getParticipantes().stream().map(ViagemParadaParticipante::getDependenteIdSnapshot).sorted().toList();
    }
}

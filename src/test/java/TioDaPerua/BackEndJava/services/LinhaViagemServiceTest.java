package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.DependenteParadaDTO;
import TioDaPerua.BackEndJava.DTOs.LinhaViagemDTO;
import TioDaPerua.BackEndJava.DTOs.ParadaDTO;
import TioDaPerua.BackEndJava.enums.TipoParada;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinhaViagemServiceTest {

    private static final long ANA = 31L;
    private static final long ENZO = 32L;

    // Ida: motorista, casa da Ana (esperando), casa do Enzo (esperando), escola com os dois.
    private static List<ParadaDTO> paradasIda(String statusAna, String statusEnzo) {
        return List.of(
                parada(1, TipoParada.MOTORISTA),
                parada(2, TipoParada.RESPONSAVEL, aluno(ANA, "Ana Clara", statusAna)),
                parada(3, TipoParada.RESPONSAVEL, aluno(ENZO, "Enzo", statusEnzo)),
                parada(4, TipoParada.ESCOLA, "Colégio X",
                        aluno(ANA, "Ana Clara", statusAna), aluno(ENZO, "Enzo", statusEnzo)));
    }

    @Test
    void omiteMotoristaNumeraSequencialmenteEMarcaAlunoEAtual() {
        LinhaViagemDTO linha = LinhaViagemService.montar(paradasIda("ESPERANDO", "ESPERANDO"), true, ENZO);

        assertTrue(linha.disponivel());
        assertEquals(1, linha.indiceAtual());
        assertEquals(3, linha.paradas().size());
        assertEquals(1, linha.paradas().get(0).ordem());
        assertEquals("RESPONSAVEL", linha.paradas().get(0).tipoParada());
        assertEquals("EMBARQUE", linha.paradas().get(0).acao());
        assertNull(linha.paradas().get(0).rotulo());
        assertTrue(linha.paradas().get(1).doFilho());
        assertEquals("Casa do Enzo", linha.paradas().get(1).rotulo());
        assertEquals("Colégio X", linha.paradas().get(2).rotulo());
        assertEquals("DESEMBARQUE", linha.paradas().get(2).acao());
        assertFalse(linha.paradas().get(2).doFilho());
    }

    @Test
    void indiceAtualEhNuloAposAlunoEmbarcadoETodasAsParadasConcluidasDoEmbarque() {
        LinhaViagemDTO linha = LinhaViagemService.montar(paradasIda("EMBARCADO", "EMBARCADO"), true, ENZO);

        assertEquals(1, linha.paradas().get(0).ordem());
        assertTrue(linha.paradas().get(0).concluida());
        assertTrue(linha.paradas().get(1).concluida());
        assertFalse(linha.paradas().get(2).concluida());
        assertEquals(3, linha.indiceAtual());
        assertEquals("Colégio X", linha.paradas().get(2).rotulo());
        assertFalse(linha.paradas().get(1).doFilho());
        assertTrue(linha.paradas().get(2).doFilho());
    }

    @Test
    void alunoFaltosoNaoEhMarcadoComoAtualNemRecebeRotuloDeCasa() {
        LinhaViagemDTO linha = LinhaViagemService.montar(paradasIda("FALTOU", "ESPERANDO"), true, ANA);

        assertEquals(2, linha.indiceAtual());
        assertTrue(linha.paradas().get(0).concluida());
        assertFalse(linha.paradas().get(0).doFilho());
        assertNull(linha.paradas().get(0).rotulo());
    }

    @Test
    void alunoSemParadaNaViagemFicaIndisponivel() {
        LinhaViagemDTO linha = LinhaViagemService.montar(paradasIda("ESPERANDO", "ESPERANDO"), true, 999L);

        assertFalse(linha.disponivel());
        assertNull(linha.indiceAtual());
        assertTrue(linha.paradas().isEmpty());
    }

    private static ParadaDTO parada(int ordem, TipoParada tipo, DependenteParadaDTO... alunos) {
        return parada(ordem, tipo, null, alunos);
    }

    private static ParadaDTO parada(int ordem, TipoParada tipo, String nomeLocal, DependenteParadaDTO... alunos) {
        return new ParadaDTO(ordem, tipo, nomeLocal, null, List.of(alunos));
    }

    private static DependenteParadaDTO aluno(long id, String nome, String status) {
        return new DependenteParadaDTO(id, nome, "Responsável", status, 200L);
    }
}

package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.Periodo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ViagemNomeTest {

    private static Escola escola(String nome) {
        Escola e = new Escola();
        e.setNome(nome);
        return e;
    }

    @Test
    void nomePadraoDeIdaListaTodasAsEscolas() {
        Viagem v = new Viagem();
        v.setPeriodo(Periodo.MANHA_IDA);
        v.setEscolas(List.of(escola("EMEF A"), escola("EMEI B")));

        assertEquals("Viagem para escola(s): EMEF A, EMEI B", v.getNome());
    }

    @Test
    void nomePadraoDeVoltaUsaTextoDeVolta() {
        Viagem v = new Viagem();
        v.setPeriodo(Periodo.TARDE_VOLTA);
        v.setEscolas(List.of(escola("EMEF A")));

        assertEquals("Viagem de volta das escola(s): EMEF A", v.getNome());
    }

    @Test
    void nomeEditadoPrevaleceESemTextoVoltaAoPadrao() {
        Viagem v = new Viagem();
        v.setPeriodo(Periodo.MANHA_IDA);
        v.setEscolas(List.of(escola("EMEF A")));

        v.setNome("  Rota da Vila  ");
        assertEquals("Rota da Vila", v.getNome());

        v.setNome("   ");
        assertEquals("Viagem para escola(s): EMEF A", v.getNome());
    }

    @Test
    void semEscolasNaoHaNomePadrao() {
        Viagem v = new Viagem();
        v.setPeriodo(Periodo.MANHA_IDA);

        assertNull(v.getNome());
    }
}

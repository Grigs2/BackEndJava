package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Veiculo;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.enums.Periodo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Textos das notificações: primeiro nome, horário e destino, sem endereço. */
class MensagensNotificacaoTest {

    private static Viagem viagem(Periodo periodo) {
        Escola escola = new Escola();
        escola.setNome("EMEF Pedro II");
        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca("ABC1D23");
        Motorista motorista = new Motorista();
        motorista.setNome("Carlos Alberto Lima");
        motorista.setVeiculo(veiculo);
        Viagem v = new Viagem();
        v.setEscola(escola);
        v.setMotorista(motorista);
        v.setPeriodo(periodo);
        return v;
    }

    private static Dependente aluno() {
        Dependente d = new Dependente();
        d.setNome("Enzo Gabriel Souza");
        return d;
    }

    @Test
    void embarqueTrazPrimeiroNomeHorarioMotoristaEPlaca() {
        var t = MensagensNotificacao.embarque(aluno(), viagem(Periodo.MANHA_IDA), LocalDateTime.of(2026, 11, 6, 6, 42));
        assertEquals("Embarque confirmado", t.titulo());
        assertEquals("Enzo embarcou às 06:42 com Carlos (ABC1D23).", t.mensagem());
    }

    @Test
    void desembarqueNaIdaFalaDaEscolaENaVoltaDeCasa() {
        LocalDateTime hora = LocalDateTime.of(2026, 11, 6, 7, 15);
        assertEquals("Enzo foi entregue na EMEF Pedro II às 07:15.",
                MensagensNotificacao.desembarque(aluno(), viagem(Periodo.MANHA_IDA), hora).mensagem());
        assertEquals("Enzo foi entregue em casa às 07:15.",
                MensagensNotificacao.desembarque(aluno(), viagem(Periodo.TARDE_VOLTA), hora).mensagem());
    }

    @Test
    void canceladaIncluiMotivoENomeDaViagem() {
        var t = MensagensNotificacao.cancelada(viagem(Periodo.MANHA_IDA), "Pneu furado");
        assertEquals("A Viagem para EMEF Pedro II (Manhã · ida) de hoje foi cancelada: Pneu furado. Providencie outro transporte.",
                t.mensagem());
    }

    @Test
    void atrasoEAusenciaInformada() {
        assertEquals("A Viagem para EMEF Pedro II (Tarde · ida) está cerca de 15 min atrasada.",
                MensagensNotificacao.atraso(viagem(Periodo.TARDE_IDA), 15, " ").mensagem());
        LocalDate hoje = LocalDate.of(2026, 11, 6);
        String quando = AusenciaService.descricaoQuando("IDA", hoje.plusDays(1), hoje);
        assertEquals("Enzo não vai na ida de amanhã: Consulta médica.",
                MensagensNotificacao.ausenciaInformada(aluno(), quando, "Consulta médica").mensagem());
    }

    @Test
    void semDadosUsaTextoGenericoSemQuebrar() {
        var t = MensagensNotificacao.embarque(null, null, null);
        assertEquals("O aluno embarcou.", t.mensagem());
        assertFalse(MensagensNotificacao.falta(null, null).mensagem().isBlank());
    }
}

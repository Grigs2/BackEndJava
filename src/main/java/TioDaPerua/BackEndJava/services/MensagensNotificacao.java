package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.enums.Periodo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Textos das notificações automáticas. Regras de privacidade: só o primeiro nome do aluno e do motorista
 * (o texto pode aparecer na tela bloqueada) e nunca o endereço.
 */
public final class MensagensNotificacao {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private MensagensNotificacao() {
    }

    public record Texto(String titulo, String mensagem) {
    }

    public static String primeiroNome(String nome, String padrao) {
        if (nome == null || nome.isBlank()) return padrao;
        return nome.trim().split("\\s+")[0];
    }

    public static String periodo(Periodo periodo) {
        if (periodo == null) return "";
        return switch (periodo) {
            case MANHA_IDA -> "Manhã · ida";
            case MANHA_VOLTA -> "Manhã · volta";
            case TARDE_IDA -> "Tarde · ida";
            case TARDE_VOLTA -> "Tarde · volta";
            case NOITE_IDA -> "Noite · ida";
            case NOITE_VOLTA -> "Noite · volta";
        };
    }

    /** "Viagem para EMEF Pedro II (Manhã · ida)" ou "viagem Manhã · ida" quando a viagem não tem escola. */
    public static String viagem(Viagem v) {
        if (v == null) return "viagem";
        String p = periodo(v.getPeriodo());
        return v.getNome() != null ? v.getNome() + (p.isEmpty() ? "" : " (" + p + ")") : ("viagem " + p).trim();
    }

    private static String hora(LocalDateTime t) {
        return t == null ? "" : " às " + HORA.format(t);
    }

    private static boolean ida(Viagem v) {
        Periodo p = v == null ? null : v.getPeriodo();
        return p == Periodo.MANHA_IDA || p == Periodo.TARDE_IDA || p == Periodo.NOITE_IDA;
    }

    public static Texto embarque(Dependente d, Viagem v, LocalDateTime quando) {
        Motorista m = v == null ? null : v.getMotorista();
        String placa = m != null && m.getVeiculo() != null && m.getVeiculo().getPlaca() != null
                ? " (" + m.getVeiculo().getPlaca() + ")" : "";
        String motorista = m != null ? " com " + primeiroNome(m.getNome(), "o motorista") + placa : "";
        return new Texto("Embarque confirmado",
                primeiroNome(d == null ? null : d.getNome(), "O aluno") + " embarcou" + hora(quando) + motorista + ".");
    }

    public static Texto desembarque(Dependente d, Viagem v, LocalDateTime quando) {
        String aluno = primeiroNome(d == null ? null : d.getNome(), "O aluno");
        String escola = v != null && v.getEscola() != null ? v.getEscola().getNome()
                : d != null && d.getEscola() != null ? d.getEscola().getNome() : null;
        String local = ida(v) ? (escola != null ? " na " + escola : " na escola") : " em casa";
        return new Texto("Desembarque confirmado", aluno + " foi entregue" + local + hora(quando) + ".");
    }

    public static Texto falta(Dependente d, Viagem v) {
        return new Texto("Ausência registrada",
                primeiroNome(d == null ? null : d.getNome(), "O aluno") + " não embarcou na " + viagem(v)
                        + " de hoje. Se for engano, fale com o motorista.");
    }

    public static Texto aCaminho(Viagem v) {
        Motorista m = v == null ? null : v.getMotorista();
        return new Texto("A caminho",
                primeiroNome(m == null ? null : m.getNome(), "O motorista") + " saiu para a " + viagem(v) + ".");
    }

    public static Texto finalizada(Viagem v) {
        return new Texto("Viagem concluída", "A " + viagem(v) + " de hoje foi concluída.");
    }

    public static Texto cancelada(Viagem v, String motivo) {
        return new Texto("Viagem cancelada", "A " + viagem(v) + " de hoje foi cancelada" + motivo(motivo)
                + ". Providencie outro transporte.");
    }

    public static Texto interrompida(Viagem v, String motivo) {
        return new Texto("Viagem interrompida", "A " + viagem(v) + " foi interrompida" + motivo(motivo)
                + ". Aguarde o contato do motorista.");
    }

    public static Texto atraso(Viagem v, int minutos, String motivo) {
        return new Texto("Viagem atrasada", "A " + viagem(v) + " está cerca de " + minutos + " min atrasada"
                + motivo(motivo) + ".");
    }

    public static Texto trocaVeiculo(String modelo, String cor, String placa) {
        String desc = String.join(" ", modelo == null ? "" : modelo, cor == null ? "" : cor.toLowerCase()).trim();
        return new Texto("Veículo diferente hoje", "Hoje a viagem será feita no " + desc
                + (placa == null ? "" : " (" + placa + ")") + ".");
    }

    public static Texto ausenciaInformada(Dependente d, String quando, String motivo) {
        return new Texto("Ausência avisada",
                primeiroNome(d == null ? null : d.getNome(), "O aluno") + " não vai " + quando + motivo(motivo) + ".");
    }

    private static String motivo(String motivo) {
        return motivo == null || motivo.isBlank() ? "" : ": " + motivo.trim();
    }
}

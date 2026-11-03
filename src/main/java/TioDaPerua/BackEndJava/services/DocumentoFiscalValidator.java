package TioDaPerua.BackEndJava.services;

import java.util.regex.Pattern;

public final class DocumentoFiscalValidator {

    private static final Pattern CNPJ_FORMAT = Pattern.compile("[A-Z0-9]{12}[0-9]{2}");
    private static final Pattern EMAIL_FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PLACA_FORMAT = Pattern.compile("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}");

    private DocumentoFiscalValidator() {
    }

    public static String normalizarCnpj(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    public static boolean cnpjValido(String cnpj) {
        String normalizado = normalizarCnpj(cnpj);
        if (normalizado == null || !CNPJ_FORMAT.matcher(normalizado).matches()
                || normalizado.chars().distinct().count() == 1) {
            return false;
        }
        int primeiro = calcularDigito(normalizado.substring(0, 12), new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int segundo = calcularDigito(normalizado.substring(0, 12) + primeiro,
                new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return normalizado.charAt(12) == (char) ('0' + primeiro)
                && normalizado.charAt(13) == (char) ('0' + segundo);
    }

    public static String normalizarCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    public static boolean cpfValido(String cpf) {
        String normalizado = normalizarCpf(cpf);
        if (normalizado == null || !normalizado.matches("\\d{11}")
                || normalizado.chars().distinct().count() == 1) {
            return false;
        }
        int primeiro = calcularDigitoCpf(normalizado.substring(0, 9), 10);
        int segundo = calcularDigitoCpf(normalizado.substring(0, 9) + primeiro, 11);
        return normalizado.charAt(9) == (char) ('0' + primeiro)
                && normalizado.charAt(10) == (char) ('0' + segundo);
    }

    public static String normalizarTelefone(String telefone) {
        return telefone == null ? null : telefone.replaceAll("\\D", "");
    }

    public static boolean telefoneValido(String telefone) {
        String normalizado = normalizarTelefone(telefone);
        return normalizado != null && normalizado.matches("(\\d{10,11}|55\\d{10,11})");
    }

    public static boolean emailValido(String email) {
        return email != null && EMAIL_FORMAT.matcher(email.trim()).matches();
    }

    public static String normalizarPlaca(String placa) {
        return placa == null ? null : placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    public static boolean placaValida(String placa) {
        String normalizada = normalizarPlaca(placa);
        return normalizada != null && PLACA_FORMAT.matcher(normalizada).matches();
    }

    private static int calcularDigitoCpf(String base, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int calcularDigito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (base.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}

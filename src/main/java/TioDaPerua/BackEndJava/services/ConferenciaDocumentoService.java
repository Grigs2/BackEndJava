package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.ConferenciaDocumentoDTO;
import TioDaPerua.BackEndJava.DTOs.DocumentoMotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.DocumentoRevisaoDTO;
import TioDaPerua.BackEndJava.DTOs.ImagemDocumentoDTO;
import TioDaPerua.BackEndJava.config.DataBrasilia;
import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Veiculo;
import TioDaPerua.BackEndJava.enums.TipoDocumento;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Conferências automáticas dos documentos do motorista, para o administrador revisar mais rápido.
 *
 * Não substituem olhar o documento: dígito verificador certo não prova que a CNH existe.
 * Só documento vencido ou sem validade (ERRO) impede a aprovação; o resto é ATENCAO, para o administrador conferir.
 */
public final class ConferenciaDocumentoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Prazo para avisar que o documento está perto de vencer. */
    static final int DIAS_AVISO_VENCIMENTO = 30;
    /** CTB, art. 136, II: inspeção semestral do veículo de transporte escolar. */
    static final int MESES_VISTORIA = 6;
    /** CTB, art. 138, I: condutor de escolar com mais de 21 anos. */
    static final int IDADE_MINIMA = 21;

    private ConferenciaDocumentoService() {
    }

    public static DocumentoRevisaoDTO paraRevisao(DocumentoMotorista documento, List<ImagemDocumentoDTO> imagens) {
        LocalDate hoje = DataBrasilia.hoje();
        Motorista motorista = documento.getMotorista();
        Veiculo veiculo = documento.getVeiculo() != null ? documento.getVeiculo()
                : motorista == null ? null : motorista.getVeiculo();
        Integer idade = motorista == null || motorista.getDataNascimento() == null ? null
                : Period.between(motorista.getDataNascimento(), hoje).getYears();
        List<ConferenciaDocumentoDTO> conferencias = conferir(documento, motorista, idade, hoje);
        // A aprovação também exige as imagens obrigatórias (mesma regra de ImagemDocumentoMotoristaService).
        boolean imagensCompletas = imagensObrigatoriasEnviadas(documento.getTipo(), imagens);
        boolean podeAprovar = imagensCompletas && conferencias.stream().noneMatch(c -> "ERRO".equals(c.situacao()));
        return new DocumentoRevisaoDTO(
                DocumentoMotoristaDTO.toDTO(documento),
                motorista == null ? null : motorista.getId(),
                motorista == null ? null : motorista.getNome(),
                idade,
                veiculo == null ? null : veiculo.getPlaca(),
                veiculo == null ? null : veiculo.getModelo(),
                conferencias,
                podeAprovar,
                imagens);
    }

    /** CNH e licenciamento exigem frente e verso; vistoria exige uma foto. */
    static boolean imagensObrigatoriasEnviadas(TipoDocumento tipo, List<ImagemDocumentoDTO> imagens) {
        if (tipo == null) return false;
        List<TioDaPerua.BackEndJava.enums.LadoDocumento> enviados = imagens == null ? List.of()
                : imagens.stream().map(ImagemDocumentoDTO::lado).toList();
        return enviados.containsAll(TioDaPerua.BackEndJava.enums.LadoDocumento.aceitosPara(tipo));
    }

    static List<ConferenciaDocumentoDTO> conferir(DocumentoMotorista documento, Motorista motorista,
                                                  Integer idade, LocalDate hoje) {
        List<ConferenciaDocumentoDTO> lista = new ArrayList<>();
        TipoDocumento tipo = documento.getTipo();
        if (tipo == TipoDocumento.CNH) {
            lista.add(cnhValida(documento.getNumero())
                    ? ConferenciaDocumentoDTO.ok("Número", "Dígitos verificadores da CNH conferem")
                    : ConferenciaDocumentoDTO.atencao("Número", "Os dígitos verificadores não conferem: confira o número no documento"));
            String cadastrada = motorista == null ? null : motorista.getCnh();
            if (cadastrada != null && !cadastrada.isBlank() && !digitos(cadastrada).equals(digitos(documento.getNumero()))) {
                lista.add(ConferenciaDocumentoDTO.atencao("Cadastro", "Número diferente da CNH informada no cadastro do motorista"));
            }
            String categoria = documento.getCategoria() == null ? "" : documento.getCategoria().toUpperCase();
            lista.add(categoria.contains("D") || categoria.contains("E")
                    ? ConferenciaDocumentoDTO.ok("Categoria", "Categoria " + categoria + " permite transporte escolar")
                    : ConferenciaDocumentoDTO.atencao("Categoria", "Transporte escolar exige categoria D ou E (CTB, art. 138)"));
            if (idade != null) {
                lista.add(idade >= IDADE_MINIMA
                        ? ConferenciaDocumentoDTO.ok("Idade", idade + " anos")
                        : ConferenciaDocumentoDTO.atencao("Idade", idade + " anos: o CTB exige mais de 21 anos para transporte escolar"));
            }
        } else if (tipo == TipoDocumento.VISTORIA) {
            LocalDate realizada = documento.getDataRealizacao();
            if (realizada == null) {
                lista.add(ConferenciaDocumentoDTO.atencao("Realização", "Data da vistoria não informada"));
            } else {
                lista.add(!realizada.isBefore(hoje.minusMonths(MESES_VISTORIA))
                        ? ConferenciaDocumentoDTO.ok("Realização", "Feita em " + DATA.format(realizada))
                        : ConferenciaDocumentoDTO.atencao("Realização", "Feita em " + DATA.format(realizada)
                                + ": a vistoria do escolar é semestral (CTB, art. 136)"));
            }
        } else if (tipo == TipoDocumento.LICENCIAMENTO) {
            lista.add(renavamValido(documento.getNumero())
                    ? ConferenciaDocumentoDTO.ok("RENAVAM", "Dígito verificador confere")
                    : ConferenciaDocumentoDTO.atencao("RENAVAM", "O dígito verificador não confere: confira o número no documento"));
        }
        if (tipo != TipoDocumento.CNH && motorista != null && motorista.getVeiculo() != null && documento.getVeiculo() != null
                && !motorista.getVeiculo().getId().equals(documento.getVeiculo().getId())) {
            lista.add(ConferenciaDocumentoDTO.atencao("Veículo", "Documento de outro veículo: o motorista trocou de veículo depois"));
        }
        lista.add(validade(documento.getDataValidade(), hoje));
        return lista;
    }

    static ConferenciaDocumentoDTO validade(LocalDate validade, LocalDate hoje) {
        if (validade == null) {
            return ConferenciaDocumentoDTO.erro("Validade", "Sem data de validade");
        }
        if (validade.isBefore(hoje)) {
            return ConferenciaDocumentoDTO.erro("Validade", "Vencido em " + DATA.format(validade));
        }
        long dias = ChronoUnit.DAYS.between(hoje, validade);
        if (dias <= DIAS_AVISO_VENCIMENTO) {
            return ConferenciaDocumentoDTO.atencao("Validade", "Vence em " + dias + (dias == 1 ? " dia" : " dias")
                    + " (" + DATA.format(validade) + ")");
        }
        return ConferenciaDocumentoDTO.ok("Validade", "Válido até " + DATA.format(validade));
    }

    /** Dígitos verificadores do número de registro da CNH (11 dígitos). */
    public static boolean cnhValida(String numero) {
        String n = digitos(numero);
        if (!n.matches("\\d{11}") || n.chars().distinct().count() == 1) return false;
        int soma = 0;
        for (int i = 0, peso = 9; i < 9; i++, peso--) soma += (n.charAt(i) - '0') * peso;
        int dv1 = soma % 11;
        int desconto = 0;
        if (dv1 >= 10) {
            dv1 = 0;
            desconto = 2;
        }
        soma = 0;
        for (int i = 0, peso = 1; i < 9; i++, peso++) soma += (n.charAt(i) - '0') * peso;
        int resto = soma % 11;
        int dv2 = resto >= 10 ? 0 : resto - desconto;
        return dv1 == n.charAt(9) - '0' && dv2 == n.charAt(10) - '0';
    }

    /** Dígito verificador do RENAVAM (9 ou 11 dígitos; os de 9 recebem zeros à esquerda). */
    public static boolean renavamValido(String numero) {
        String n = digitos(numero);
        if (n.length() == 9) n = "00" + n;
        if (!n.matches("\\d{11}") || n.chars().distinct().count() == 1) return false;
        int[] pesos = {2, 3, 4, 5, 6, 7, 8, 9, 2, 3};
        int soma = 0;
        for (int i = 0; i < 10; i++) soma += (n.charAt(9 - i) - '0') * pesos[i];
        int dv = (soma * 10) % 11;
        if (dv == 10) dv = 0;
        return dv == n.charAt(10) - '0';
    }

    private static String digitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}

package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.ConferenciaDocumentoDTO;
import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.enums.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConferenciaDocumentoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 22);

    @Test
    void digitosDaCnh() {
        // 1·9+2·8+…+9·1 = 165 → resto 0 → dv1 = 0; 1·1+2·2+…+9·9 = 285 → resto 10 → dv2 = 0.
        assertTrue(ConferenciaDocumentoService.cnhValida("12345678900"));
        // 2·9+3·1 = 21 → resto 10 → dv1 = 0 e desconto 2; 2·1+3·9 = 29 → resto 7 → dv2 = 7 - 2 = 5.
        assertTrue(ConferenciaDocumentoService.cnhValida("200.000.003-05"));
        assertFalse(ConferenciaDocumentoService.cnhValida("12345678901"));
        assertFalse(ConferenciaDocumentoService.cnhValida("11111111111"));
        assertFalse(ConferenciaDocumentoService.cnhValida("1234"));
    }

    @Test
    void aprovacaoExigeAsImagensObrigatorias() {
        var frente = new TioDaPerua.BackEndJava.DTOs.ImagemDocumentoDTO(1L, TioDaPerua.BackEndJava.enums.LadoDocumento.FRENTE, "image/jpeg", 10L, null);
        var verso = new TioDaPerua.BackEndJava.DTOs.ImagemDocumentoDTO(1L, TioDaPerua.BackEndJava.enums.LadoDocumento.VERSO, "image/jpeg", 10L, null);
        var unica = new TioDaPerua.BackEndJava.DTOs.ImagemDocumentoDTO(2L, TioDaPerua.BackEndJava.enums.LadoDocumento.UNICA, "image/jpeg", 10L, null);

        assertTrue(ConferenciaDocumentoService.imagensObrigatoriasEnviadas(TipoDocumento.CNH, List.of(frente, verso)));
        assertFalse(ConferenciaDocumentoService.imagensObrigatoriasEnviadas(TipoDocumento.CNH, List.of(frente)));
        assertFalse(ConferenciaDocumentoService.imagensObrigatoriasEnviadas(TipoDocumento.VISTORIA, List.of()));
        assertTrue(ConferenciaDocumentoService.imagensObrigatoriasEnviadas(TipoDocumento.VISTORIA, List.of(unica)));

        DocumentoMotorista semFotos = documento(TipoDocumento.CNH, "12345678900", "D", HOJE.plusYears(2));
        assertFalse(ConferenciaDocumentoService.paraRevisao(semFotos, List.of(frente)).podeAprovar());
        assertTrue(ConferenciaDocumentoService.paraRevisao(semFotos, List.of(frente, verso)).podeAprovar());
    }

    @Test
    void digitoDoRenavam() {
        // Base 0123456789 invertida × pesos 2,3,4,5,6,7,8,9,2,3 = 202; 2020 mod 11 = 7.
        assertTrue(ConferenciaDocumentoService.renavamValido("01234567897"));
        assertFalse(ConferenciaDocumentoService.renavamValido("01234567890"));
        assertFalse(ConferenciaDocumentoService.renavamValido("00000000000"));
    }

    @Test
    void cnhCompletaETudoOk() {
        DocumentoMotorista doc = documento(TipoDocumento.CNH, "12345678900", "D", HOJE.plusYears(2));

        List<ConferenciaDocumentoDTO> lista = ConferenciaDocumentoService.conferir(doc, motorista("12345678900"), 35, HOJE);

        assertTrue(lista.stream().allMatch(c -> "OK".equals(c.situacao())), lista.toString());
    }

    @Test
    void cnhComAvisosParaOAdministrador() {
        DocumentoMotorista doc = documento(TipoDocumento.CNH, "12345678901", "B", HOJE.plusDays(10));

        List<ConferenciaDocumentoDTO> lista = ConferenciaDocumentoService.conferir(doc, motorista("99999999999"), 20, HOJE);

        assertEquals("ATENCAO", situacao(lista, "Número"));
        assertEquals("ATENCAO", situacao(lista, "Cadastro"));
        assertEquals("ATENCAO", situacao(lista, "Categoria"));
        assertEquals("ATENCAO", situacao(lista, "Idade"));
        assertEquals("ATENCAO", situacao(lista, "Validade"));
        assertTrue(lista.stream().noneMatch(c -> "ERRO".equals(c.situacao())));
    }

    @Test
    void documentoVencidoEErroEImpedeAprovar() {
        assertEquals("ERRO", ConferenciaDocumentoService.validade(HOJE.minusDays(1), HOJE).situacao());
        assertEquals("ERRO", ConferenciaDocumentoService.validade(null, HOJE).situacao());
        assertEquals("OK", ConferenciaDocumentoService.validade(HOJE.plusDays(31), HOJE).situacao());
    }

    @Test
    void vistoriaComMaisDeSeisMesesPedeAtencao() {
        DocumentoMotorista doc = documento(TipoDocumento.VISTORIA, "VIST-1", null, HOJE.plusMonths(3));
        doc.setDataRealizacao(HOJE.minusMonths(7));

        List<ConferenciaDocumentoDTO> lista = ConferenciaDocumentoService.conferir(doc, motorista(null), 40, HOJE);

        assertEquals("ATENCAO", situacao(lista, "Realização"));
        assertEquals("OK", situacao(lista, "Validade"));
    }

    @Test
    void licenciamentoConfereRenavam() {
        DocumentoMotorista doc = documento(TipoDocumento.LICENCIAMENTO, "01234567897", null, HOJE.plusYears(1));

        assertEquals("OK", situacao(ConferenciaDocumentoService.conferir(doc, motorista(null), 40, HOJE), "RENAVAM"));
    }

    private static DocumentoMotorista documento(TipoDocumento tipo, String numero, String categoria, LocalDate validade) {
        DocumentoMotorista d = new DocumentoMotorista();
        d.setTipo(tipo);
        d.setNumero(numero);
        d.setCategoria(categoria);
        d.setDataValidade(validade);
        return d;
    }

    private static Motorista motorista(String cnh) {
        Motorista m = new Motorista();
        m.setCnh(cnh);
        return m;
    }

    private static String situacao(List<ConferenciaDocumentoDTO> lista, String item) {
        return lista.stream().filter(c -> c.item().equals(item)).map(ConferenciaDocumentoDTO::situacao).findFirst().orElse("-");
    }
}

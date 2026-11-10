package TioDaPerua.BackEndJava.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuscaMotoristaProximidadeTest {

    private static BuscaMotoristaService.Referencia texto(String endereco) {
        return BuscaMotoristaService.Referencia.texto(SolicitacaoService.normalizar(endereco), null);
    }

    @Test
    void comparaComEnderecoEmTextoIgnorandoAcentos() {
        var ref = texto("Rua das Flores, 120 - Centro, São Paulo/SP - CEP 01001-000");
        assertEquals(BuscaMotoristaService.MESMO_BAIRRO, BuscaMotoristaService.proximidade("Centro", "Sao Paulo", ref));
        assertEquals(BuscaMotoristaService.MESMA_CIDADE, BuscaMotoristaService.proximidade("Ipiranga", "São Paulo", ref));
        assertEquals(BuscaMotoristaService.OUTRA_CIDADE, BuscaMotoristaService.proximidade("Centro", "Santo André", ref));
    }

    @Test
    void enderecoTextoSemCidadeUsaCidadeDaEscolaDoDependente() {
        var ref = BuscaMotoristaService.Referencia.texto(
                SolicitacaoService.normalizar("Rua Vergueiro, 1000 - Vila Mariana"),
                SolicitacaoService.normalizar("São Paulo"));
        assertEquals(BuscaMotoristaService.MESMA_CIDADE,
                BuscaMotoristaService.proximidade("Jardim Vila Mariana", "São Paulo", ref));
        assertEquals(BuscaMotoristaService.OUTRA_CIDADE,
                BuscaMotoristaService.proximidade("Centro", "Santo André", ref));
    }

    @Test
    void comparaComEnderecoEstruturadoDoAluno() {
        var ref = BuscaMotoristaService.Referencia.estruturada("ipiranga", "sao paulo");
        assertEquals(BuscaMotoristaService.MESMO_BAIRRO, BuscaMotoristaService.proximidade("Ipiranga", "São Paulo", ref));
        assertEquals(BuscaMotoristaService.MESMA_CIDADE, BuscaMotoristaService.proximidade("Moema", "São Paulo", ref));
    }

    @Test
    void semReferenciaOuSemCidadeNaoClassifica() {
        assertEquals(BuscaMotoristaService.SEM_REFERENCIA, BuscaMotoristaService.proximidade("Centro", "São Paulo", null));
        assertEquals(BuscaMotoristaService.SEM_REFERENCIA,
                BuscaMotoristaService.proximidade("Centro", null, texto("Rua A, São Paulo")));
    }
}

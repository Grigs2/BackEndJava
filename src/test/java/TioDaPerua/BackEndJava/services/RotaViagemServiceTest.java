package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.DependenteParadaDTO;
import TioDaPerua.BackEndJava.DTOs.ParadaDTO;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.enums.Periodo;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.TipoParada;
import TioDaPerua.BackEndJava.repositories.DependenteResponsavelUsuarioProjection;
import TioDaPerua.BackEndJava.repositories.GeocodificacaoEnderecoRepository;
import TioDaPerua.BackEndJava.repositories.LocalizacaoViagemAtualRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RotaViagemServiceTest {

    @Mock private GoogleMapsClient google;
    @Mock private ViagemService viagemService;
    @Mock private LocalizacaoViagemService localizacaoViagemService;
    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private LocalizacaoViagemAtualRepository localizacaoAtualRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private GeocodificacaoEnderecoRepository geocodificacaoRepository;
    @InjectMocks private RotaViagemService service;

    private static final long MOTORISTA = 100L;
    private static final long RESPONSAVEL = 200L;

    @BeforeEach
    void setUp() {
        ViagemDia execucao = new ViagemDia();
        execucao.setId(9L);
        execucao.setStatus(StatusViagemDia.EM_ANDAMENTO);
        Viagem viagem = new Viagem();
        viagem.setPeriodo(Periodo.MANHA_IDA);
        execucao.setViagem(viagem);
        lenient().when(viagemDiaRepository.findById(9L)).thenReturn(Optional.of(execucao));
        lenient().when(viagemDiaRepository.existsByIdAndViagem_Motorista_Usuario_Id(9L, MOTORISTA)).thenReturn(true);
        lenient().when(google.configurado()).thenReturn(true);
        lenient().when(geocodificacaoRepository.findByChave(anyString())).thenReturn(Optional.empty());
        lenient().when(google.geocodificar(anyString())).thenReturn(Optional.of(new GoogleMapsClient.Coordenada(-23.5, -46.6)));
        // Ida: casa da Ana (esperando), casa do Enzo (esperando), escola.
        lenient().when(viagemService.gerarParadasViagemDia(9L)).thenReturn(List.of(
                parada(1, TipoParada.MOTORISTA),
                parada(2, TipoParada.RESPONSAVEL, aluno(31, "Ana Clara", "ESPERANDO")),
                parada(3, TipoParada.RESPONSAVEL, aluno(32, "Enzo", "ESPERANDO")),
                parada(4, TipoParada.ESCOLA, aluno(31, "Ana Clara", "ESPERANDO"), aluno(32, "Enzo", "ESPERANDO"))));
        lenient().when(google.calcularRota(any(), anyList())).thenReturn(List.of(
                new GoogleMapsClient.Trecho(300, 1000, "poly1", "Vire à direita na Rua A", 200L),
                new GoogleMapsClient.Trecho(240, 900, "poly2", null, null),
                new GoogleMapsClient.Trecho(600, 3000, "poly3", null, null)));
    }

    @Test
    void motoristaRecebeTodasAsParadasComTempoAcumulado() {
        var rota = service.calcular(9L, MOTORISTA, -23.6, -46.7);

        assertTrue(rota.disponivel());
        assertEquals(3, rota.paradas().size());
        assertEquals(300, rota.paradas().get(0).segundos());
        assertEquals(540, rota.paradas().get(1).segundos());
        assertEquals(1140, rota.paradas().get(2).segundos());
        assertEquals("Vire à direita na Rua A", rota.proximaInstrucao());
        assertEquals(List.of("poly1", "poly2", "poly3"), rota.polylines());
        verify(localizacaoViagemService, never()).validarLeituraLocalizacao(any(), any());
    }

    @Test
    void responsavelVeSoATempoAteACasaDoFilhoSemOutrasParadas() {
        when(localizacaoAtualRepository.findByViagemDia_Id(9L)).thenReturn(Optional.of(posicao()));
        when(responsavelRepository.findUsuariosAtivosPorDependentes(anyList()))
                .thenReturn(List.of(vinculo(32L, RESPONSAVEL), vinculo(31L, 999L)));

        var rota = service.calcular(9L, RESPONSAVEL, null, null);

        assertTrue(rota.disponivel());
        assertEquals(1, rota.paradas().size());
        var alvo = rota.paradas().get(0);
        assertEquals(32L, alvo.dependenteId());
        assertEquals("EMBARQUE", alvo.acao());
        assertEquals(540, alvo.segundos());
        assertEquals("Busca de Enzo em casa", alvo.rotulo());
        assertEquals(List.of("poly1", "poly2"), rota.polylines());
        assertNull(rota.proximaInstrucao());
        verify(localizacaoViagemService).validarLeituraLocalizacao(9L, RESPONSAVEL);
    }

    @Test
    void depoisDeEmbarcadoAPrevisaoEAteAEscola() {
        when(viagemService.gerarParadasViagemDia(9L)).thenReturn(List.of(
                parada(2, TipoParada.RESPONSAVEL, aluno(31, "Ana Clara", "EMBARCADO")),
                parada(3, TipoParada.RESPONSAVEL, aluno(32, "Enzo", "ESPERANDO")),
                parada(4, TipoParada.ESCOLA, aluno(31, "Ana Clara", "EMBARCADO"), aluno(32, "Enzo", "ESPERANDO"))));
        when(google.calcularRota(any(), anyList())).thenReturn(List.of(
                new GoogleMapsClient.Trecho(240, 900, "poly2", null, null),
                new GoogleMapsClient.Trecho(600, 3000, "poly3", null, null)));
        when(localizacaoAtualRepository.findByViagemDia_Id(9L)).thenReturn(Optional.of(posicao()));
        when(responsavelRepository.findUsuariosAtivosPorDependentes(anyList())).thenReturn(List.of(vinculo(31L, RESPONSAVEL)));

        var alvo = service.calcular(9L, RESPONSAVEL, null, null).paradas().get(0);

        assertEquals("DESEMBARQUE", alvo.acao());
        assertEquals("Ana chega à escola", alvo.rotulo());
        assertEquals(840, alvo.segundos());
    }

    @Test
    void semChaveDoGoogleARotaFicaIndisponivelSemErro() {
        when(google.configurado()).thenReturn(false);

        var rota = service.calcular(9L, MOTORISTA, -23.6, -46.7);

        assertFalse(rota.disponivel());
        assertTrue(rota.mensagem().contains("chave"));
        verify(google, never()).calcularRota(any(), anyList());
    }

    @Test
    void responsavelSemPosicaoDoVeiculoAguarda() {
        when(localizacaoAtualRepository.findByViagemDia_Id(9L)).thenReturn(Optional.empty());

        var rota = service.calcular(9L, RESPONSAVEL, -1.0, -1.0);

        assertFalse(rota.disponivel());
        assertTrue(rota.mensagem().contains("primeira posição"));
    }

    @Test
    void paradaPendenteSegueAMesmaRegraDoAtendimento() {
        assertTrue(RotaViagemService.pendente(parada(4, TipoParada.ESCOLA, aluno(1, "A", "EMBARCADO")), true));
        assertFalse(RotaViagemService.pendente(parada(2, TipoParada.RESPONSAVEL, aluno(1, "A", "EMBARCADO")), true));
        assertFalse(RotaViagemService.pendente(parada(4, TipoParada.ESCOLA, aluno(1, "A", "DESEMBARCADO")), true));
        assertTrue(RotaViagemService.pendente(parada(4, TipoParada.ESCOLA, aluno(1, "A", "ESPERANDO")), false));
    }

    private static ParadaDTO parada(int ordem, TipoParada tipo, DependenteParadaDTO... alunos) {
        return new ParadaDTO((long) ordem, ordem, tipo, "Parada " + ordem, "Rua " + ordem + ", São Paulo", List.of(alunos));
    }

    private static DependenteParadaDTO aluno(long id, String nome, String status) {
        return new DependenteParadaDTO(id, nome, "Resp", status, null);
    }

    private static TioDaPerua.BackEndJava.entities.LocalizacaoViagemAtual posicao() {
        var p = new TioDaPerua.BackEndJava.entities.LocalizacaoViagemAtual();
        p.setLatitude(new java.math.BigDecimal("-23.55"));
        p.setLongitude(new java.math.BigDecimal("-46.63"));
        return p;
    }

    private static DependenteResponsavelUsuarioProjection vinculo(Long dependenteId, Long usuarioId) {
        return new DependenteResponsavelUsuarioProjection() {
            @Override public Long getDependenteId() { return dependenteId; }
            @Override public Long getUsuarioId() { return usuarioId; }
        };
    }
}

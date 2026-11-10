package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.AusenciaInformada;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.TipoNotificacao;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.AusenciaInformadaRepository;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.DependenteResponsavelUsuarioProjection;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AusenciaServiceTest {

    @Mock private AusenciaInformadaRepository ausenciaRepository;
    @Mock private DependenteRepository dependenteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ViagemDependenteRepository viagemDependenteRepository;
    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ViagemPresencaRepository viagemPresencaRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private ViagemService viagemService;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private AusenciaService service;

    @Test
    void recusaDataForaDoIntervaloOuTrechoInvalido() {
        LocalDate hoje = LocalDate.now();
        assertThrows(BusinessException.class, () -> service.registrar(7L, 1L, hoje.minusDays(1), "TODOS", null));
        assertThrows(BusinessException.class, () -> service.registrar(7L, 1L, hoje.plusDays(61), "TODOS", null));
        assertThrows(BusinessException.class, () -> service.registrar(7L, 1L, hoje, "MEIO", null));
        verifyNoInteractions(notificacaoService);
    }

    @Test
    void recusaAusenciaRepetidaParaOMesmoDia() {
        LocalDate amanha = LocalDate.now().plusDays(1);
        when(ausenciaRepository.existsByDependenteIdAndDataAndTrecho(7L, amanha, "IDA")).thenReturn(false);
        when(ausenciaRepository.existsByDependenteIdAndDataAndTrecho(7L, amanha, "TODOS")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.registrar(7L, 1L, amanha, "ida", null));
    }

    @Test
    void idaEVoltaConflitaComQualquerAvisoDoDia() {
        LocalDate amanha = LocalDate.now().plusDays(1);
        when(ausenciaRepository.existsByDependenteIdAndData(7L, amanha)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.registrar(7L, 1L, amanha, "TODOS", null));
    }

    @Test
    void registraEAvisaOsOutrosResponsaveis() {
        LocalDate amanha = LocalDate.now().plusDays(1);
        Dependente enzo = new Dependente();
        enzo.setId(7L);
        enzo.setNome("Enzo Souza");
        Usuario ator = new Usuario();
        ator.setId(1L);
        when(dependenteRepository.findById(7L)).thenReturn(Optional.of(enzo));
        when(ausenciaRepository.existsByDependenteIdAndData(7L, amanha)).thenReturn(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ator));
        when(ausenciaRepository.save(any())).thenAnswer(inv -> {
            AusenciaInformada a = inv.getArgument(0);
            a.setId(50L);
            return a;
        });
        when(viagemDependenteRepository.findAtivosByDependenteId(7L)).thenReturn(List.of());
        DependenteResponsavelUsuarioProjection outro = mock(DependenteResponsavelUsuarioProjection.class);
        when(outro.getUsuarioId()).thenReturn(2L);
        when(responsavelRepository.findUsuariosAtivosPorDependentes(List.of(7L))).thenReturn(List.of(outro));

        var dto = service.registrar(7L, 1L, amanha, null, "  Consulta  ");

        assertEquals("TODOS", dto.trecho());
        assertEquals("Consulta", dto.motivo());
        verify(notificacaoService).criarNotificacoesAutomaticas(eq(1L), eq(Set.of(2L)),
                eq("Ausência avisada"), eq("Enzo não vai no transporte de amanhã: Consulta."),
                argThat((NotificacaoService.Contexto c) -> c.tipo() == TipoNotificacao.AUSENCIA_INFORMADA
                        && "ausencia:50".equals(c.chaveEvento())));
    }

    @Test
    void descreveOQuandoDoAviso() {
        LocalDate hoje = LocalDate.of(2026, 11, 6);
        assertEquals("no transporte de hoje", AusenciaService.descricaoQuando("TODOS", hoje, hoje));
        assertEquals("na volta de 12/11", AusenciaService.descricaoQuando("VOLTA", LocalDate.of(2026, 11, 12), hoje));
    }
}

package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.AtualizarLocalizacaoViagemDTO;
import TioDaPerua.BackEndJava.entities.ConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.HistoricoLocalizacaoViagem;
import TioDaPerua.BackEndJava.entities.LocalizacaoViagemAtual;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.enums.AcaoConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.TipoEscopoConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.ConsentimentoLocalizacaoRepository;
import TioDaPerua.BackEndJava.repositories.DependenteResponsavelUsuarioProjection;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoLocalizacaoViagemRepository;
import TioDaPerua.BackEndJava.repositories.LocalizacaoViagemAtualRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.SuspensaoLocalizacaoViagemRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalizacaoViagemServiceTest {

    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ViagemPresencaRepository viagemPresencaRepository;
    @Mock private LocalizacaoViagemAtualRepository localizacaoAtualRepository;
    @Mock private HistoricoLocalizacaoViagemRepository historicoLocalizacaoRepository;
    @Mock private ConsentimentoLocalizacaoRepository consentimentoRepository;
    @Mock private SuspensaoLocalizacaoViagemRepository suspensaoRepository;
    @Mock private MotoristaRepository motoristaRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private DependenteRepository dependenteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private LocalizacaoViagemService service;

    private ViagemDia execucao;
    private Usuario motoristaUsuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "versaoPolitica", "gps-v1");
        motoristaUsuario = new Usuario();
        motoristaUsuario.setId(100L);
        Motorista motorista = new Motorista();
        motorista.setId(10L);
        motorista.setUsuario(motoristaUsuario);
        Viagem viagem = new Viagem();
        viagem.setMotorista(motorista);
        execucao = new ViagemDia();
        execucao.setId(90L);
        execucao.setViagem(viagem);
        execucao.setStatus(StatusViagemDia.EM_ANDAMENTO);

        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
        when(viagemDiaRepository.findById(90L)).thenReturn(Optional.of(execucao));
        when(viagemPresencaRepository.findByViagemDiaId(90L)).thenReturn(List.of(presenca(5L)));
        when(responsavelRepository.findUsuariosAtivosPorDependentes(List.of(5L)))
                .thenReturn(List.of(vinculo(5L, 200L)));
        when(consentimentoRepository.findLatestByDependentesAndUsuarios(
                TipoEscopoConsentimentoLocalizacao.DEPENDENTE, List.of(5L), java.util.Set.of(200L), "gps-v1"))
                .thenReturn(List.of(consentimento(200L, 5L, AcaoConsentimentoLocalizacao.ACEITO)));
        when(consentimentoRepository
                .findTopByUsuario_IdAndEscopoAndDependenteIsNullAndVersaoPoliticaOrderByIdDesc(
                        100L, TipoEscopoConsentimentoLocalizacao.MOTORISTA, "gps-v1"))
                .thenReturn(Optional.of(consentimento(100L, null, AcaoConsentimentoLocalizacao.ACEITO)));
        when(suspensaoRepository.existsByViagemDia_Id(90L)).thenReturn(false);
    }

    @Test
    void storesRoundedPositionInCurrentAndAppendOnlyHistory() {
        when(localizacaoAtualRepository.findByViagemDia_Id(90L)).thenReturn(Optional.empty());
        when(historicoLocalizacaoRepository.save(any(HistoricoLocalizacaoViagem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(localizacaoAtualRepository.save(any(LocalizacaoViagemAtual.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AtualizarLocalizacaoViagemDTO dto = new AtualizarLocalizacaoViagemDTO(
                90L, new BigDecimal("-23.550520"), new BigDecimal("-46.633308"), Instant.now());

        var result = service.registrarPosicao(dto, 100L);

        assertEquals(new BigDecimal("-23.55052"), result.latitude());
        assertEquals(new BigDecimal("-46.63331"), result.longitude());
        assertEquals(90L, result.idViagemDia());
        verify(historicoLocalizacaoRepository).save(any(HistoricoLocalizacaoViagem.class));
        verify(localizacaoAtualRepository).save(any(LocalizacaoViagemAtual.class));
    }

    @Test
    void rejectsUpdatesSentBeforeMinimumInterval() {
        LocalizacaoViagemAtual atual = new LocalizacaoViagemAtual();
        atual.setRecebidoEm(Instant.now().minusSeconds(5));
        atual.setInstanteDispositivo(Instant.now().minusSeconds(5));
        when(localizacaoAtualRepository.findByViagemDia_Id(90L)).thenReturn(Optional.of(atual));
        AtualizarLocalizacaoViagemDTO dto = new AtualizarLocalizacaoViagemDTO(
                90L, new BigDecimal("-23.55"), new BigDecimal("-46.63"), Instant.now());

        assertThrows(ConflictException.class, () -> service.registrarPosicao(dto, 100L));

        verify(historicoLocalizacaoRepository, never()).save(any(HistoricoLocalizacaoViagem.class));
    }

    @Test
    void blocksGpsWhenNoActiveResponsibleHasConsentedForEveryParticipant() {
        when(consentimentoRepository.findLatestByDependentesAndUsuarios(
                TipoEscopoConsentimentoLocalizacao.DEPENDENTE, List.of(5L), java.util.Set.of(200L), "gps-v1"))
                .thenReturn(List.of());
        AtualizarLocalizacaoViagemDTO dto = new AtualizarLocalizacaoViagemDTO(
                90L, new BigDecimal("-23.55"), new BigDecimal("-46.63"), Instant.now());

        assertThrows(ConflictException.class, () -> service.registrarPosicao(dto, 100L));

        verify(localizacaoAtualRepository, never()).save(any(LocalizacaoViagemAtual.class));
    }

    private ViagemPresenca presenca(Long dependenteId) {
        ViagemPresenca presenca = new ViagemPresenca();
        presenca.setDependente(new Dependente(
                dependenteId, "Dependente", null, null, null, null));
        return presenca;
    }

    private DependenteResponsavelUsuarioProjection vinculo(Long dependenteId, Long usuarioId) {
        return new DependenteResponsavelUsuarioProjection() {
            @Override
            public Long getDependenteId() {
                return dependenteId;
            }

            @Override
            public Long getUsuarioId() {
                return usuarioId;
            }
        };
    }

    private ConsentimentoLocalizacao consentimento(
            Long usuarioId, Long dependenteId, AcaoConsentimentoLocalizacao acao) {
        ConsentimentoLocalizacao consentimento = new ConsentimentoLocalizacao();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        consentimento.setUsuario(usuario);
        consentimento.setEscopo(dependenteId == null
                ? TipoEscopoConsentimentoLocalizacao.MOTORISTA
                : TipoEscopoConsentimentoLocalizacao.DEPENDENTE);
        if (dependenteId != null) {
            consentimento.setDependente(new Dependente(dependenteId, null, null, null, null, null));
        }
        consentimento.setAcao(acao);
        consentimento.setVersaoPolitica("gps-v1");
        return consentimento;
    }
}

package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.ConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.enums.AcaoConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.TipoEscopoConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.exceptions.ServiceUnavailableException;
import TioDaPerua.BackEndJava.repositories.ConsentimentoLocalizacaoRepository;
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

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalizacaoConsentimentoRevogacaoTest {

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

    private Usuario motoristaUsuario;
    private ViagemDia execucao;

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
    }

    @Test
    void revocationImmediatelySuspendsGpsForActiveExecution() {
        when(motoristaRepository.findByUsuarioId(100L)).thenReturn(Optional.of(execucao.getViagem().getMotorista()));
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(motoristaUsuario));
        when(consentimentoRepository
                .findTopByUsuario_IdAndEscopoAndDependenteIsNullAndVersaoPoliticaOrderByIdDesc(
                        100L, TipoEscopoConsentimentoLocalizacao.MOTORISTA, "gps-v1"))
                .thenReturn(Optional.of(consentimento(AcaoConsentimentoLocalizacao.ACEITO)));
        when(consentimentoRepository.save(any(ConsentimentoLocalizacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(viagemDiaRepository.findIdsByMotoristaUsuarioIdAndStatus(100L, StatusViagemDia.EM_ANDAMENTO))
                .thenReturn(List.of(90L));
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
        when(suspensaoRepository.existsByViagemDia_Id(90L)).thenReturn(false);
        when(suspensaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.registrarConsentimentoMotorista(100L, false);

        assertFalse(result.aceito());
        verify(suspensaoRepository).save(any());
    }

    @Test
    void scheduledCleanupAppliesGpsAndConsentRetentionPolicies() {
        service.expurgarHistoricoExpirado();

        verify(historicoLocalizacaoRepository).deleteExpirados(any(Instant.class));
        verify(consentimentoRepository).deleteAntigosPreservandoUltimo(any(Instant.class));
    }

    @Test
    void refusesConsentWhenGpsPolicyVersionIsNotConfigured() {
        ReflectionTestUtils.setField(service, "versaoPolitica", "");
        when(motoristaRepository.findByUsuarioId(100L)).thenReturn(Optional.of(execucao.getViagem().getMotorista()));
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(motoristaUsuario));

        assertThrows(ServiceUnavailableException.class,
                () -> service.registrarConsentimentoMotorista(100L, true));
    }

    private ConsentimentoLocalizacao consentimento(AcaoConsentimentoLocalizacao acao) {
        ConsentimentoLocalizacao consentimento = new ConsentimentoLocalizacao();
        consentimento.setUsuario(motoristaUsuario);
        consentimento.setEscopo(TipoEscopoConsentimentoLocalizacao.MOTORISTA);
        consentimento.setAcao(acao);
        consentimento.setVersaoPolitica("gps-v1");
        return consentimento;
    }
}

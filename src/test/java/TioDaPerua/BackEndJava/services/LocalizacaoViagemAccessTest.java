package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalizacaoViagemAccessTest {

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

    @Test
    void deniesLocationReadToUserWithoutActiveExecutionRelationship() {
        Usuario dono = new Usuario();
        dono.setId(100L);
        Motorista motorista = new Motorista();
        motorista.setUsuario(dono);
        Viagem viagem = new Viagem();
        viagem.setMotorista(motorista);
        ViagemDia execucao = new ViagemDia();
        execucao.setId(90L);
        execucao.setViagem(viagem);

        when(viagemDiaRepository.findById(90L)).thenReturn(Optional.of(execucao));
        when(viagemDiaRepository.existsByIdAndViagem_Motorista_Usuario_Id(90L, 999L)).thenReturn(false);
        when(viagemDiaRepository.existeVinculoResponsavel(90L, 999L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.consultarPosicaoAtual(90L, 999L));
    }
}

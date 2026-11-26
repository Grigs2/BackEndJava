package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.SubmeterDocumentoMotoristaDTO;
import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Veiculo;
import TioDaPerua.BackEndJava.enums.StatusDocumento;
import TioDaPerua.BackEndJava.enums.TipoDocumento;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.repositories.AdministradorRepository;
import TioDaPerua.BackEndJava.repositories.DocumentoMotoristaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoMotoristaServiceTest {

    @Mock private DocumentoMotoristaRepository documentoRepository;
    @Mock private MotoristaRepository motoristaRepository;
    @Mock private AdministradorRepository administradorRepository;
    @Mock private AuditoriaAdministrativaService auditoriaService;
    @Mock private ImagemDocumentoMotoristaService imagemService;
    @InjectMocks private DocumentoMotoristaService service;

    @Test
    void blocksStartWhenAnyRequiredDocumentIsMissing() {
        when(motoristaRepository.findById(20L)).thenReturn(Optional.of(motoristaComVeiculo()));
        when(documentoRepository.findFirstByMotoristaIdAndTipoOrderByIdDesc(20L, TipoDocumento.CNH))
                .thenReturn(Optional.empty());
        when(documentoRepository.findFirstByMotoristaIdAndTipoOrderByIdDesc(20L, TipoDocumento.VISTORIA))
                .thenReturn(Optional.empty());
        when(documentoRepository.findFirstByMotoristaIdAndTipoOrderByIdDesc(20L, TipoDocumento.LICENCIAMENTO))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.validarDocumentosParaInicio(20L));
    }

    @Test
    void allowsStartOnlyWhenAllRequiredDocumentsAreApprovedAndUnexpired() {
        when(motoristaRepository.findById(20L)).thenReturn(Optional.of(motoristaComVeiculo()));
        for (TipoDocumento tipo : TipoDocumento.values()) {
            DocumentoMotorista documento = new DocumentoMotorista();
            documento.setTipo(tipo);
            documento.setStatus(StatusDocumento.APROVADO);
            documento.setDataValidade(LocalDate.now().plusDays(1));
            if (tipo != TipoDocumento.CNH) {
                documento.setVeiculo(motoristaComVeiculo().getVeiculo());
            }
            when(documentoRepository.findFirstByMotoristaIdAndTipoOrderByIdDesc(20L, tipo))
                    .thenReturn(Optional.of(documento));
        }

        assertDoesNotThrow(() -> service.validarDocumentosParaInicio(20L));
    }

    @Test
    void blocksStartWhenVehicleDocumentsWereInvalidatedByAnEdit() {
        when(motoristaRepository.findById(20L)).thenReturn(Optional.of(motoristaComVeiculo()));
        for (TipoDocumento tipo : TipoDocumento.values()) {
            DocumentoMotorista documento = new DocumentoMotorista();
            documento.setTipo(tipo);
            documento.setStatus(StatusDocumento.APROVADO);
            documento.setDataValidade(LocalDate.now().plusDays(1));
            if (tipo != TipoDocumento.CNH) {
                documento.setVeiculo(motoristaComVeiculo().getVeiculo());
            }
            if (tipo == TipoDocumento.VISTORIA) {
                documento.setSubstituidoEm(Instant.now());
            }
            when(documentoRepository.findFirstByMotoristaIdAndTipoOrderByIdDesc(20L, tipo))
                    .thenReturn(Optional.of(documento));
        }

        assertThrows(BusinessException.class, () -> service.validarDocumentosParaInicio(20L));
    }

    @Test
    void requiresFullDocumentNumbersForCnhAndLicensing() {
        assertThrows(BusinessException.class, () -> service.submeter(20L,
                new SubmeterDocumentoMotoristaDTO(TipoDocumento.CNH, "1234", "D", null,
                        LocalDate.now().plusDays(100))));
    }

    @Test
    void rejectsCnhWhoseCheckDigitsDoNotMatch() {
        BusinessException erro = assertThrows(BusinessException.class, () -> service.submeter(20L,
                new SubmeterDocumentoMotoristaDTO(TipoDocumento.CNH, "04512387612", "D", null,
                        LocalDate.now().plusDays(100))));
        assertTrue(erro.getMessage().contains("dígitos verificadores"));
    }

    @Test
    void rejectsRenavamWhoseCheckDigitDoesNotMatch() {
        BusinessException erro = assertThrows(BusinessException.class, () -> service.submeter(20L,
                new SubmeterDocumentoMotoristaDTO(TipoDocumento.LICENCIAMENTO, "01234567890", null, null,
                        LocalDate.now().plusDays(100))));
        assertTrue(erro.getMessage().contains("dígito verificador"));
    }

    @Test
    void acceptsCnhWithValidCheckDigits() {
        when(motoristaRepository.findById(20L)).thenReturn(Optional.of(motoristaComVeiculo()));
        when(documentoRepository.findFirstByMotoristaIdAndTipoOrderByIdDesc(20L, TipoDocumento.CNH))
                .thenReturn(Optional.empty());
        when(documentoRepository.save(org.mockito.ArgumentMatchers.any(DocumentoMotorista.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        assertDoesNotThrow(() -> service.submeter(20L,
                new SubmeterDocumentoMotoristaDTO(TipoDocumento.CNH, "12345678900", "D", null,
                        LocalDate.now().plusDays(100))));
    }

    private Motorista motoristaComVeiculo() {
        Motorista motorista = new Motorista();
        motorista.setId(20L);
        Veiculo veiculo = new Veiculo();
        veiculo.setId(30L);
        motorista.setVeiculo(veiculo);
        return motorista;
    }
}

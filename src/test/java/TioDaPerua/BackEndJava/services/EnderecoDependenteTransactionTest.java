package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.EnderecoDependenteDTO;
import TioDaPerua.BackEndJava.DTOs.EnderecoDependenteRequestDTO;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.EnderecoDependente;
import TioDaPerua.BackEndJava.enums.FinalidadeEndereco;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.EnderecoDependenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnderecoDependenteTransactionTest {

    @Mock private DependenteRepository dependenteRepository;
    @Mock private EnderecoDependenteRepository enderecoRepository;

    private EnderecoDependenteTransaction transaction;

    @BeforeEach
    void setUp() {
        transaction = new EnderecoDependenteTransaction(dependenteRepository, enderecoRepository);
    }

    @Test
    void storesNormalizedAddressAndPendingCepValidation() {
        Dependente dependente = new Dependente();
        dependente.setId(5L);
        when(dependenteRepository.findByIdAndResponsavelUsuarioIdForUpdate(5L, 10L))
                .thenReturn(Optional.of(dependente));
        when(enderecoRepository.existeConflitoDeFinalidade(
                5L, -1L, FinalidadeEndereco.EMBARQUE, FinalidadeEndereco.AMBOS)).thenReturn(false);
        when(enderecoRepository.save(any(EnderecoDependente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnderecoDependenteDTO result = transaction.adicionar(
                5L, 10L, request(), StatusValidacaoEndereco.PENDENTE);

        ArgumentCaptor<EnderecoDependente> captor = ArgumentCaptor.forClass(EnderecoDependente.class);
        verify(enderecoRepository).save(captor.capture());
        assertEquals("12345678", captor.getValue().getCep());
        assertEquals("SP", captor.getValue().getUf());
        assertEquals(StatusValidacaoEndereco.PENDENTE, captor.getValue().getStatusValidacao());
        assertEquals("PENDENTE", result.statusValidacao());
    }

    @Test
    void rejectsAnotherActiveAddressForAnOverlappingPurpose() {
        Dependente dependente = new Dependente();
        dependente.setId(5L);
        when(dependenteRepository.findByIdAndResponsavelUsuarioIdForUpdate(5L, 10L))
                .thenReturn(Optional.of(dependente));
        when(enderecoRepository.existeConflitoDeFinalidade(
                5L, -1L, FinalidadeEndereco.EMBARQUE, FinalidadeEndereco.AMBOS)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> transaction.adicionar(5L, 10L, request(), StatusValidacaoEndereco.VALIDADO));
    }

    private EnderecoDependenteRequestDTO request() {
        return new EnderecoDependenteRequestDTO(
                "12345-678", "Rua das Flores", "10", null,
                "Centro", "São Paulo", "sp", FinalidadeEndereco.EMBARQUE);
    }
}

package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.enums.StatusDocumento;
import TioDaPerua.BackEndJava.enums.TipoDocumento;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.FormatoArquivoNaoPermitidoException;
import TioDaPerua.BackEndJava.repositories.DocumentoMotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ImagemDocumentoMotoristaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagemDocumentoMotoristaServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 0x01, 0x00};

    @Mock private DocumentoMotoristaRepository documentoRepository;
    @Mock private ImagemDocumentoMotoristaRepository imagemRepository;

    @TempDir Path diretorio;

    private ImagemDocumentoMotoristaService criarService() {
        return new ImagemDocumentoMotoristaService(documentoRepository, imagemRepository, diretorio.toString());
    }

    private DocumentoMotorista documento(TipoDocumento tipo, StatusDocumento status) {
        Motorista motorista = new Motorista();
        motorista.setId(20L);
        DocumentoMotorista documento = new DocumentoMotorista();
        documento.setId(7L);
        documento.setMotorista(motorista);
        documento.setTipo(tipo);
        documento.setStatus(status);
        return documento;
    }

    private void documentoExiste(DocumentoMotorista documento) {
        lenient().when(documentoRepository.findById(7L)).thenReturn(Optional.of(documento));
    }

    @Test
    void rejeitaFormatoQueNaoEhJpegNemPng() {
        documentoExiste(documento(TipoDocumento.CNH, StatusDocumento.PENDENTE));
        var arquivo = new MockMultipartFile("arquivo", "cnh.gif", "image/gif", GIF);

        assertThrows(FormatoArquivoNaoPermitidoException.class,
                () -> criarService().enviar(20L, 7L, "FRENTE", arquivo));
        verify(imagemRepository, never()).save(any());
    }

    @Test
    void rejeitaLadoQueNaoSeAplicaAoTipoDeDocumento() {
        documentoExiste(documento(TipoDocumento.VISTORIA, StatusDocumento.PENDENTE));
        var arquivo = new MockMultipartFile("arquivo", "v.jpg", "image/jpeg", JPEG);

        assertThrows(BusinessException.class, () -> criarService().enviar(20L, 7L, "FRENTE", arquivo));
    }

    @Test
    void bloqueiaEnvioQuandoDocumentoJaFoiAprovado() {
        documentoExiste(documento(TipoDocumento.CNH, StatusDocumento.APROVADO));
        var arquivo = new MockMultipartFile("arquivo", "cnh.jpg", "image/jpeg", JPEG);

        assertThrows(ConflictException.class, () -> criarService().enviar(20L, 7L, "FRENTE", arquivo));
    }

    @Test
    void gravaJpegValidoComHashEDevolveMesmoConteudo() throws Exception {
        documentoExiste(documento(TipoDocumento.CNH, StatusDocumento.PENDENTE));
        when(imagemRepository.findByDocumentoIdAndLado(any(), any())).thenReturn(Optional.empty());
        when(imagemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var arquivo = new MockMultipartFile("arquivo", "cnh.jpg", "image/jpeg", JPEG);

        var dto = criarService().enviar(20L, 7L, "frente", arquivo);

        assertEquals(TioDaPerua.BackEndJava.enums.LadoDocumento.FRENTE, dto.lado());
        assertEquals("image/jpeg", dto.mimeType());
        assertEquals((long) JPEG.length, dto.tamanhoBytes());
        Path gravado = diretorio.resolve("motoristas/20/documentos/7/FRENTE");
        assertArrayEquals(JPEG, Files.readAllBytes(gravado));
    }

    @Test
    void leituraFalhaQuandoArquivoFoiAlteradoNoArmazenamento() throws Exception {
        documentoExiste(documento(TipoDocumento.CNH, StatusDocumento.PENDENTE));
        Path arquivo = diretorio.resolve("motoristas/20/documentos/7/FRENTE");
        Files.createDirectories(arquivo.getParent());
        Files.write(arquivo, new byte[]{1, 2, 3});
        var imagem = new TioDaPerua.BackEndJava.entities.ImagemDocumentoMotorista();
        imagem.setCaminho("motoristas/20/documentos/7/FRENTE");
        imagem.setMimeType("image/jpeg");
        imagem.setSha256("hash-diferente");
        when(imagemRepository.findByDocumentoIdAndLado(7L, TioDaPerua.BackEndJava.enums.LadoDocumento.FRENTE))
                .thenReturn(Optional.of(imagem));

        assertThrows(IllegalStateException.class, () -> criarService().ler(20L, 7L, "FRENTE"));
    }

    @Test
    void aprovacaoExigeFrenteEVersoParaCnh() {
        when(imagemRepository.findByDocumentoIdAndLado(any(), any())).thenReturn(Optional.empty());

        assertThrows(ConflictException.class,
                () -> criarService().exigirImagensObrigatorias(7L, TipoDocumento.CNH));
    }
}

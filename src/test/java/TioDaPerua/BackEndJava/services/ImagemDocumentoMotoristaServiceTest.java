package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.StatusDocumento;
import TioDaPerua.BackEndJava.enums.TipoDocumento;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.FormatoArquivoNaoPermitidoException;
import TioDaPerua.BackEndJava.repositories.DocumentoMotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ImagemDocumentoMotoristaRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static TioDaPerua.BackEndJava.services.ImagemDocumentoMotoristaService.MOTIVO_RETENCAO_IMAGENS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static final String CHAVE = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private static final String OUTRA_CHAVE = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=";

    @Mock private DocumentoMotoristaRepository documentoRepository;
    @Mock private ImagemDocumentoMotoristaRepository imagemRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @TempDir Path diretorio;

    private ImagemDocumentoMotoristaService criarService() {
        return criarService(CHAVE);
    }

    private ImagemDocumentoMotoristaService criarService(String chave) {
        return new ImagemDocumentoMotoristaService(
                documentoRepository, imagemRepository, usuarioRepository, diretorio.toString(), chave);
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
        byte[] armazenado = Files.readAllBytes(gravado);
        assertEquals(12 + JPEG.length + 16, armazenado.length);
        assertFalse(Arrays.equals(Arrays.copyOfRange(armazenado, 0, JPEG.length), JPEG));
        assertFalse(armazenado[0] == (byte) 0xFF && armazenado[1] == (byte) 0xD8);
    }

    @Test
    void imagemNaoPodeSerLidaComOutraChave() throws Exception {
        documentoExiste(documento(TipoDocumento.CNH, StatusDocumento.PENDENTE));
        when(imagemRepository.findByDocumentoIdAndLado(any(), any())).thenReturn(Optional.empty());
        when(imagemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        criarService().enviar(20L, 7L, "frente", new MockMultipartFile("arquivo", "cnh.jpg", "image/jpeg", JPEG));

        var imagem = new TioDaPerua.BackEndJava.entities.ImagemDocumentoMotorista();
        imagem.setCaminho("motoristas/20/documentos/7/FRENTE");
        imagem.setMimeType("image/jpeg");
        imagem.setSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JPEG)));
        when(imagemRepository.findByDocumentoIdAndLado(7L, TioDaPerua.BackEndJava.enums.LadoDocumento.FRENTE))
                .thenReturn(Optional.of(imagem));

        assertThrows(IllegalStateException.class, () -> criarService(OUTRA_CHAVE).ler(20L, 7L, "FRENTE"));
    }

    @Test
    void exigeChaveDeTamanhoValido() {
        assertThrows(IllegalStateException.class, () -> criarService(""));
        assertThrows(IllegalStateException.class, () -> criarService("AAAA"));
    }

    @Test
    void purgaImagensDeContaSuspensaAlemDoPrazoEExigeReenvio() throws Exception {
        var documento = documento(TipoDocumento.CNH, StatusDocumento.APROVADO);
        var usuario = new Usuario();
        usuario.setId(99L);
        usuario.setContaSuspensa(true);
        usuario.setContaSuspensaEm(Instant.now().minus(Duration.ofDays(31)));
        var imagem = new TioDaPerua.BackEndJava.entities.ImagemDocumentoMotorista();
        imagem.setDocumento(documento);
        imagem.setCaminho("motoristas/20/documentos/7/FRENTE");
        Path arquivo = diretorio.resolve(imagem.getCaminho());
        Files.createDirectories(arquivo.getParent());
        Files.write(arquivo, new byte[]{1});
        when(usuarioRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(usuario));
        when(imagemRepository.findByDocumentoMotoristaUsuarioId(99L)).thenReturn(List.of(imagem));

        int removidas = criarService().purgarImagensDeContaSuspensaDesde(99L, Instant.now().minus(Duration.ofDays(30)));

        assertEquals(1, removidas);
        assertEquals(StatusDocumento.RECUSADO, documento.getStatus());
        assertEquals(MOTIVO_RETENCAO_IMAGENS, documento.getMotivoRecusa());
        verify(imagemRepository).deleteAll(List.of(imagem));
        assertFalse(Files.exists(arquivo));
    }

    @Test
    void naoPurgaContaSuspensaHaMenosDeTrintaDias() {
        var usuario = new Usuario();
        usuario.setContaSuspensa(true);
        usuario.setContaSuspensaEm(Instant.now().minus(Duration.ofDays(5)));
        when(usuarioRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(usuario));

        assertEquals(0, criarService().purgarImagensDeContaSuspensaDesde(99L, Instant.now().minus(Duration.ofDays(30))));
        verify(imagemRepository, never()).deleteAll(any());
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

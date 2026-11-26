package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.ConteudoImagemDocumentoDTO;
import TioDaPerua.BackEndJava.DTOs.ImagemDocumentoDTO;
import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.entities.ImagemDocumentoMotorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.LadoDocumento;
import TioDaPerua.BackEndJava.enums.StatusDocumento;
import TioDaPerua.BackEndJava.enums.TipoDocumento;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.FormatoArquivoNaoPermitidoException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.DocumentoMotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ImagemDocumentoMotoristaRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImagemDocumentoMotoristaService {

    private static final Logger log = LoggerFactory.getLogger(ImagemDocumentoMotoristaService.class);

    static final long TAMANHO_MAXIMO_BYTES = 1024L * 1024L;
    static final String MOTIVO_RETENCAO_IMAGENS =
            "Imagens removidas após 30 dias de suspensão da conta. Reenvie o documento.";
    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";
    private static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final String ALGORITMO_CIFRA = "AES/GCM/NoPadding";
    private static final int TAMANHO_CHAVE_BYTES = 32;
    private static final int TAMANHO_IV_BYTES = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final DocumentoMotoristaRepository documentoRepository;
    private final ImagemDocumentoMotoristaRepository imagemRepository;
    private final UsuarioRepository usuarioRepository;
    private final Path diretorioBase;
    private final SecretKeySpec chave;
    private final SecureRandom aleatorio = new SecureRandom();

    public ImagemDocumentoMotoristaService(DocumentoMotoristaRepository documentoRepository,
                                           ImagemDocumentoMotoristaRepository imagemRepository,
                                           UsuarioRepository usuarioRepository,
                                           @Value("${app.storage.documentos.dir}") String diretorioBase,
                                           @Value("${app.storage.documentos.chave-base64}") String chaveBase64) {
        this.documentoRepository = documentoRepository;
        this.imagemRepository = imagemRepository;
        this.usuarioRepository = usuarioRepository;
        this.diretorioBase = Paths.get(diretorioBase).toAbsolutePath().normalize();
        this.chave = criarChave(chaveBase64);
    }

    @Transactional
    public ImagemDocumentoDTO enviar(Long motoristaId, Long documentoId, String ladoTexto, MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Envie o arquivo da imagem no campo 'arquivo'");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new MaxUploadSizeExceededException(TAMANHO_MAXIMO_BYTES);
        }
        LadoDocumento lado = LadoDocumento.de(ladoTexto);
        DocumentoMotorista documento = localizarDoMotorista(motoristaId, documentoId);

        if (documento.getSubstituidoEm() != null) {
            throw new ConflictException("Uma submissão substituída não aceita novas imagens");
        }
        if (documento.getStatus() != StatusDocumento.PENDENTE && documento.getStatus() != StatusDocumento.RECUSADO) {
            throw new ConflictException("Imagens só podem ser alteradas enquanto o documento estiver pendente ou recusado");
        }
        if (!LadoDocumento.aceitosPara(documento.getTipo()).contains(lado)) {
            throw new BusinessException("O lado " + lado + " não se aplica ao documento " + documento.getTipo());
        }

        byte[] conteudo = lerBytes(arquivo);
        String mimeType = detectarMimeType(conteudo);
        String sha256 = sha256(conteudo);

        String caminhoRelativo = caminhoRelativo(motoristaId, documentoId, lado);
        gravarArquivo(caminhoRelativo, conteudo);

        ImagemDocumentoMotorista imagem = imagemRepository.findByDocumentoIdAndLado(documentoId, lado)
                .orElseGet(ImagemDocumentoMotorista::new);
        imagem.setDocumento(documento);
        imagem.setLado(lado);
        imagem.setCaminho(caminhoRelativo);
        imagem.setMimeType(mimeType);
        imagem.setTamanhoBytes((long) conteudo.length);
        imagem.setSha256(sha256);
        imagem.setEnviadoEm(Instant.now());
        return ImagemDocumentoDTO.toDTO(imagemRepository.save(imagem));
    }

    @Transactional(readOnly = true)
    public List<ImagemDocumentoDTO> listar(Long motoristaId, Long documentoId) {
        localizarDoMotorista(motoristaId, documentoId);
        return imagemRepository.findByDocumentoIdOrderByLadoAsc(documentoId).stream()
                .map(ImagemDocumentoDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConteudoImagemDocumentoDTO ler(Long motoristaId, Long documentoId, String ladoTexto) {
        LadoDocumento lado = LadoDocumento.de(ladoTexto);
        if (motoristaId != null) {
            localizarDoMotorista(motoristaId, documentoId);
        } else {
            localizar(documentoId);
        }
        ImagemDocumentoMotorista imagem = imagemRepository.findByDocumentoIdAndLado(documentoId, lado)
                .orElseThrow(() -> new ResourceNotFoundException("Imagem " + lado + " não encontrada para o documento"));
        byte[] conteudo = lerArquivo(imagem.getCaminho());
        if (!sha256(conteudo).equals(imagem.getSha256())) {
            throw new IllegalStateException("Integridade da imagem do documento " + documentoId + " comprometida");
        }
        return new ConteudoImagemDocumentoDTO(imagem.getMimeType(), conteudo);
    }

    @Transactional
    public void excluirDoDocumento(Long documentoId) {
        List<ImagemDocumentoMotorista> imagens = imagemRepository.findByDocumentoIdOrderByLadoAsc(documentoId);
        imagens.forEach(imagem -> apagarArquivo(imagem.getCaminho()));
        imagemRepository.deleteAll(imagens);
    }

    @Transactional
    public int purgarImagensDeContaSuspensaDesde(Long usuarioId, Instant limite) {
        Usuario usuario = usuarioRepository.findByIdForUpdate(usuarioId).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getContaSuspensa())
                || usuario.getContaSuspensaEm() == null || usuario.getContaSuspensaEm().isAfter(limite)) {
            return 0;
        }
        List<ImagemDocumentoMotorista> imagens = imagemRepository.findByDocumentoMotoristaUsuarioId(usuarioId);
        if (imagens.isEmpty()) {
            return 0;
        }
        List<String> caminhos = imagens.stream().map(ImagemDocumentoMotorista::getCaminho).toList();
        imagens.forEach(imagem -> exigirReenvioSeAindaValido(imagem.getDocumento()));
        imagemRepository.deleteAll(imagens);
        apagarArquivosAposCommit(caminhos);
        return imagens.size();
    }

    private void exigirReenvioSeAindaValido(DocumentoMotorista documento) {
        if (documento.getSubstituidoEm() == null
                && (documento.getStatus() == StatusDocumento.APROVADO || documento.getStatus() == StatusDocumento.PENDENTE)) {
            documento.setStatus(StatusDocumento.RECUSADO);
            documento.setMotivoRecusa(MOTIVO_RETENCAO_IMAGENS);
        }
    }

    private void apagarArquivosAposCommit(List<String> caminhos) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            caminhos.forEach(this::apagarArquivoSemFalhar);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                caminhos.forEach(ImagemDocumentoMotoristaService.this::apagarArquivoSemFalhar);
            }
        });
    }

    private void apagarArquivoSemFalhar(String caminhoRelativo) {
        try {
            apagarArquivo(caminhoRelativo);
        } catch (UncheckedIOException e) {
            log.error("Imagem removida do banco não pôde ser apagada do armazenamento", e);
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ImagemDocumentoDTO>> listarPorDocumentos(List<Long> documentoIds) {
        if (documentoIds.isEmpty()) {
            return Map.of();
        }
        return imagemRepository.findByDocumentoIdIn(documentoIds).stream()
                .collect(Collectors.groupingBy(
                        imagem -> imagem.getDocumento().getId(),
                        Collectors.mapping(ImagemDocumentoDTO::toDTO, Collectors.toList())));
    }

    @Transactional(readOnly = true)
    public void exigirImagensObrigatorias(Long documentoId, TipoDocumento tipo) {
        List<LadoDocumento> faltantes = LadoDocumento.aceitosPara(tipo).stream()
                .filter(lado -> imagemRepository.findByDocumentoIdAndLado(documentoId, lado).isEmpty())
                .toList();
        if (!faltantes.isEmpty()) {
            throw new ConflictException("Imagens obrigatórias ausentes para aprovação: " + faltantes);
        }
    }

    private DocumentoMotorista localizar(Long documentoId) {
        return documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado com id: " + documentoId));
    }

    private DocumentoMotorista localizarDoMotorista(Long motoristaId, Long documentoId) {
        DocumentoMotorista documento = localizar(documentoId);
        if (!documento.getMotorista().getId().equals(motoristaId)) {
            throw new AccessDeniedException("Documento não pertence ao motorista informado");
        }
        return documento;
    }

    private byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
        }
    }

    static String detectarMimeType(byte[] conteudo) {
        if (comecaCom(conteudo, ASSINATURA_JPEG)) {
            return MIME_JPEG;
        }
        if (comecaCom(conteudo, ASSINATURA_PNG)) {
            return MIME_PNG;
        }
        throw new FormatoArquivoNaoPermitidoException("Formato não permitido. Envie uma imagem JPEG ou PNG");
    }

    private static boolean comecaCom(byte[] conteudo, byte[] assinatura) {
        return conteudo.length >= assinatura.length
                && Arrays.equals(conteudo, 0, assinatura.length, assinatura, 0, assinatura.length);
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }

    private static String caminhoRelativo(Long motoristaId, Long documentoId, LadoDocumento lado) {
        return "motoristas/" + motoristaId + "/documentos/" + documentoId + "/" + lado.name();
    }

    private void gravarArquivo(String caminhoRelativo, byte[] conteudo) {
        Path destino = resolver(caminhoRelativo);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, cifrar(conteudo, caminhoRelativo));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar a imagem do documento", e);
        }
    }

    private byte[] lerArquivo(String caminhoRelativo) {
        try {
            return decifrar(Files.readAllBytes(resolver(caminhoRelativo)), caminhoRelativo);
        } catch (NoSuchFileException e) {
            throw new ResourceNotFoundException("Arquivo da imagem não encontrado no armazenamento");
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a imagem do documento", e);
        }
    }

    private static SecretKeySpec criarChave(String chaveBase64) {
        if (chaveBase64 == null || chaveBase64.isBlank()) {
            throw new IllegalStateException("A variável DOCUMENTOS_CHAVE_BASE64 deve ser configurada");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(chaveBase64.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("DOCUMENTOS_CHAVE_BASE64 deve conter uma chave Base64 válida", exception);
        }
        if (bytes.length != TAMANHO_CHAVE_BYTES) {
            throw new IllegalStateException("DOCUMENTOS_CHAVE_BASE64 deve decodificar para exatamente 32 bytes");
        }
        return new SecretKeySpec(bytes, "AES");
    }

    private byte[] cifrar(byte[] conteudo, String caminhoRelativo) {
        byte[] iv = new byte[TAMANHO_IV_BYTES];
        aleatorio.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO_CIFRA);
            cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            cipher.updateAAD(caminhoRelativo.getBytes(StandardCharsets.UTF_8));
            byte[] cifrado = cipher.doFinal(conteudo);
            return ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao criptografar a imagem do documento", e);
        }
    }

    private byte[] decifrar(byte[] armazenado, String caminhoRelativo) {
        if (armazenado.length <= TAMANHO_IV_BYTES + TAMANHO_TAG_BITS / 8) {
            throw new IllegalStateException("Imagem do documento armazenada em formato inválido");
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO_CIFRA);
            cipher.init(Cipher.DECRYPT_MODE, chave,
                    new GCMParameterSpec(TAMANHO_TAG_BITS, armazenado, 0, TAMANHO_IV_BYTES));
            cipher.updateAAD(caminhoRelativo.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(armazenado, TAMANHO_IV_BYTES, armazenado.length - TAMANHO_IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Imagem do documento não pôde ser decifrada", e);
        }
    }

    private void apagarArquivo(String caminhoRelativo) {
        try {
            Files.deleteIfExists(resolver(caminhoRelativo));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao remover a imagem do documento", e);
        }
    }

    private Path resolver(String caminhoRelativo) {
        Path destino = diretorioBase.resolve(caminhoRelativo).normalize();
        if (!destino.startsWith(diretorioBase)) {
            throw new IllegalStateException("Caminho de armazenamento inválido");
        }
        return destino;
    }
}

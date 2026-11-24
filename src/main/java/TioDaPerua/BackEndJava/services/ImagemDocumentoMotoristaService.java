package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.ConteudoImagemDocumentoDTO;
import TioDaPerua.BackEndJava.DTOs.ImagemDocumentoDTO;
import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.entities.ImagemDocumentoMotorista;
import TioDaPerua.BackEndJava.enums.LadoDocumento;
import TioDaPerua.BackEndJava.enums.StatusDocumento;
import TioDaPerua.BackEndJava.enums.TipoDocumento;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.FormatoArquivoNaoPermitidoException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.DocumentoMotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ImagemDocumentoMotoristaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImagemDocumentoMotoristaService {

    static final long TAMANHO_MAXIMO_BYTES = 1024L * 1024L;
    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";
    private static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final DocumentoMotoristaRepository documentoRepository;
    private final ImagemDocumentoMotoristaRepository imagemRepository;
    private final Path diretorioBase;

    public ImagemDocumentoMotoristaService(DocumentoMotoristaRepository documentoRepository,
                                           ImagemDocumentoMotoristaRepository imagemRepository,
                                           @Value("${app.storage.documentos.dir}") String diretorioBase) {
        this.documentoRepository = documentoRepository;
        this.imagemRepository = imagemRepository;
        this.diretorioBase = Paths.get(diretorioBase).toAbsolutePath().normalize();
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
            Files.write(destino, conteudo);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar a imagem do documento", e);
        }
    }

    private byte[] lerArquivo(String caminhoRelativo) {
        try {
            return Files.readAllBytes(resolver(caminhoRelativo));
        } catch (NoSuchFileException e) {
            throw new ResourceNotFoundException("Arquivo da imagem não encontrado no armazenamento");
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a imagem do documento", e);
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

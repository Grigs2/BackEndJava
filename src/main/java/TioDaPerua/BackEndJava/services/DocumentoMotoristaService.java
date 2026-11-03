package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.DocumentoMotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.RevisarDocumentoDTO;
import TioDaPerua.BackEndJava.DTOs.SubmeterDocumentoMotoristaDTO;
import TioDaPerua.BackEndJava.entities.Administrador;
import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.enums.StatusDocumento;
import TioDaPerua.BackEndJava.enums.TipoDocumento;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.AdministradorRepository;
import TioDaPerua.BackEndJava.repositories.DocumentoMotoristaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class DocumentoMotoristaService {

    private static final List<TipoDocumento> DOCUMENTOS_OBRIGATORIOS =
            Arrays.asList(TipoDocumento.CNH, TipoDocumento.VISTORIA, TipoDocumento.LICENCIAMENTO);

    private final DocumentoMotoristaRepository documentoRepository;
    private final MotoristaRepository motoristaRepository;
    private final AdministradorRepository administradorRepository;

    @Autowired
    private AuditoriaAdministrativaService auditoriaService;

    public DocumentoMotoristaService(DocumentoMotoristaRepository documentoRepository,
                                     MotoristaRepository motoristaRepository,
                                     AdministradorRepository administradorRepository) {
        this.documentoRepository = documentoRepository;
        this.motoristaRepository = motoristaRepository;
        this.administradorRepository = administradorRepository;
    }

    @Transactional
    public DocumentoMotoristaDTO submeter(Long motoristaId, SubmeterDocumentoMotoristaDTO dto) {
        if (motoristaId == null || dto == null || dto.tipo() == null) {
            throw new IllegalArgumentException("Motorista e tipo de documento são obrigatórios");
        }

        validarMetadados(dto);
        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + motoristaId));

        DocumentoMotorista documento = new DocumentoMotorista();
        documentoRepository.findFirstByMotoristaIdAndTipoOrderByIdDesc(motoristaId, dto.tipo())
                .ifPresent(anterior -> {
                    anterior.setSubstituidoEm(Instant.now());
                    documentoRepository.save(anterior);
                });
        documento.setMotorista(motorista);
        documento.setTipo(dto.tipo());
        documento.setNumero(normalizar(dto.numero()));
        documento.setCategoria(normalizar(dto.categoria()));
        documento.setDataRealizacao(dto.dataRealizacao());
        documento.setDataValidade(dto.dataValidade());
        documento.setStatus(StatusDocumento.PENDENTE);
        documento.setSubmetidoEm(Instant.now());

        if (dto.tipo() != TipoDocumento.CNH) {
            if (motorista.getVeiculo() == null) {
                throw new BusinessException("Cadastre um veículo antes de enviar vistoria ou licenciamento");
            }
            documento.setVeiculo(motorista.getVeiculo());
        }

        return DocumentoMotoristaDTO.toDTO(documentoRepository.save(documento));
    }

    @Transactional(readOnly = true)
    public List<DocumentoMotoristaDTO> listarDoMotorista(Long motoristaId) {
        if (!motoristaRepository.existsById(motoristaId)) {
            throw new ResourceNotFoundException("Motorista não encontrado com id: " + motoristaId);
        }
        return documentoRepository.findByMotoristaIdOrderByIdDesc(motoristaId).stream()
                .map(DocumentoMotoristaDTO::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentoMotoristaDTO> listarPendentes() {
        return documentoRepository.findByStatusAndSubstituidoEmIsNullOrderBySubmetidoEmAsc(StatusDocumento.PENDENTE).stream()
                .map(DocumentoMotoristaDTO::toDTO)
                .toList();
    }

    @Transactional
    public DocumentoMotoristaDTO revisar(Long administradorUsuarioId, Long documentoId, RevisarDocumentoDTO dto) {
        if (administradorUsuarioId == null || documentoId == null || dto == null || dto.status() == null) {
            throw new IllegalArgumentException("Administrador, documento e decisão são obrigatórios");
        }
        if (dto.status() != StatusDocumento.APROVADO && dto.status() != StatusDocumento.RECUSADO) {
            throw new BusinessException("A revisão deve aprovar ou recusar o documento");
        }
        String motivo = normalizar(dto.motivoRecusa());
        if (dto.status() == StatusDocumento.RECUSADO && motivo != null && motivo.length() > 1000) {
            throw new BusinessException("O motivo da recusa deve ter no máximo 1000 caracteres");
        }

        Administrador administrador = administradorRepository.findByUsuarioId(administradorUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado"));
        DocumentoMotorista documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado com id: " + documentoId));
        DocumentoMotorista atual = documentoRepository.findFirstByMotoristaIdAndTipoOrderByIdDesc(
                        documento.getMotorista().getId(), documento.getTipo())
                .orElseThrow(() -> new ResourceNotFoundException("Documento atual não encontrado"));
        if (!atual.getId().equals(documentoId)) {
            throw new ConflictException("Uma submissão mais recente já substituiu este documento");
        }
        if (documento.getStatus() != StatusDocumento.PENDENTE) {
            throw new ConflictException("Somente documentos pendentes podem ser revisados");
        }
        if (documento.getSubstituidoEm() != null) {
            throw new ConflictException("Uma submissão substituída não pode ser revisada");
        }
        if (dto.status() == StatusDocumento.APROVADO
                && (documento.getDataValidade() == null
                || documento.getDataValidade().isBefore(LocalDate.now()))) {
            throw new BusinessException("Não é possível aprovar documento vencido ou sem validade");
        }

        documento.setStatus(dto.status());
        documento.setMotivoRecusa(dto.status() == StatusDocumento.RECUSADO ? motivo : null);
        documento.setRevisadoEm(Instant.now());
        documento.setRevisadoPor(administrador);
        if (dto.status() == StatusDocumento.APROVADO && documento.getTipo() == TipoDocumento.CNH) {
            Motorista motorista = documento.getMotorista();
            motorista.setCnh(documento.getNumero());
            motoristaRepository.save(motorista);
        }
        DocumentoMotorista salvo = documentoRepository.save(documento);
        auditoriaService.registrarAlteracao(
                administradorUsuarioId,
                dto.status() == StatusDocumento.APROVADO ? "DOCUMENTO_APROVADO" : "DOCUMENTO_RECUSADO",
                "DOCUMENTO_MOTORISTA",
                documentoId,
                "SUCESSO",
                motivo);
        return DocumentoMotoristaDTO.toDTO(salvo);
    }

    @Transactional
    public void invalidarDocumentosDoVeiculo(Long veiculoId) {
        if (veiculoId == null) {
            throw new IllegalArgumentException("ID do veículo é obrigatório");
        }
        Instant agora = Instant.now();
        documentoRepository.findByVeiculoIdAndSubstituidoEmIsNull(veiculoId).forEach(documento -> {
            documento.setSubstituidoEm(agora);
            documentoRepository.save(documento);
        });
    }

    @Transactional(readOnly = true)
    public void validarDocumentosParaInicio(Long motoristaId) {
        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + motoristaId));
        if (motorista.getVeiculo() == null) {
            throw new BusinessException("Motorista deve possuir um veículo cadastrado para iniciar a viagem");
        }
        List<String> pendencias = listarPendenciasParaInicio(motoristaId);
        if (!pendencias.isEmpty()) {
            throw new BusinessException("Não é possível iniciar a viagem. " + String.join("; ", pendencias));
        }
    }

    @Transactional(readOnly = true)
    public List<String> listarPendenciasParaInicio(Long motoristaId) {
        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + motoristaId));
        if (motorista.getVeiculo() == null) {
            return List.of();
        }
        return DOCUMENTOS_OBRIGATORIOS.stream()
                .filter(tipo -> documentoRepository.findFirstByMotoristaIdAndTipoOrderByIdDesc(motoristaId, tipo)
                        .filter(documento -> documento.getStatus() == StatusDocumento.APROVADO)
                        .filter(documento -> documento.getSubstituidoEm() == null)
                        .filter(documento -> documento.getDataValidade() != null
                                && !documento.getDataValidade().isBefore(LocalDate.now()))
                        .filter(documento -> tipo == TipoDocumento.CNH
                                || (documento.getVeiculo() != null
                                && documento.getVeiculo().getId().equals(motorista.getVeiculo().getId())))
                        .isEmpty())
                .map(tipo -> "Documento " + tipo.name() + " pendente, recusado ou vencido")
                .toList();
    }

    private void validarMetadados(SubmeterDocumentoMotoristaDTO dto) {
        if (dto.dataValidade() == null) {
            throw new BusinessException("A data de validade é obrigatória");
        }
        switch (dto.tipo()) {
            case CNH -> {
                exigirNumeroOnzeDigitos(dto.numero(), "O número da CNH deve conter 11 dígitos");
                exigirTexto(dto.categoria(), "A categoria da CNH é obrigatória");
            }
            case VISTORIA -> {
                if (dto.dataRealizacao() == null) {
                    throw new BusinessException("A data da vistoria é obrigatória");
                }
                if (dto.dataRealizacao().isAfter(LocalDate.now())) {
                    throw new BusinessException("A data da vistoria não pode estar no futuro");
                }
                if (dto.dataValidade().isBefore(dto.dataRealizacao())) {
                    throw new BusinessException("A validade da vistoria não pode anteceder sua realização");
                }
            }
            case LICENCIAMENTO -> exigirNumeroOnzeDigitos(dto.numero(), "O RENAVAM deve conter 11 dígitos");
        }
    }

    private void exigirTexto(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
    }

    private void exigirNumeroOnzeDigitos(String value, String message) {
        if (value == null || !value.replaceAll("\\D", "").matches("\\d{11}")) {
            throw new BusinessException(message);
        }
    }

    private String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

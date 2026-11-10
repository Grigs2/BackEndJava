package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.AvaliacaoDTO;
import TioDaPerua.BackEndJava.entities.Avaliacao;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.AvaliacaoRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Avaliações de motoristas pelos responsáveis. Só quem tem acordo ativo com o motorista pode avaliá-lo,
 * e cada responsável tem uma única avaliação por motorista (nova nota substitui a anterior).
 */
@Service
public class AvaliacaoService {

    static final int MAX_AVALIACOES_RETORNADAS = 20;

    @Autowired private AvaliacaoRepository avaliacaoRepository;
    @Autowired private ResponsavelRepository responsavelRepository;
    @Autowired private MotoristaRepository motoristaRepository;
    @Autowired private ViagemDependenteRepository viagemDependenteRepository;

    @Transactional
    public AvaliacaoDTO avaliar(Long usuarioResponsavelId, Long motoristaId, Integer nota, String comentario) {
        Responsavel responsavel = responsavelRepository.findByUsuarioId(usuarioResponsavelId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado para o usuário: " + usuarioResponsavelId));
        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + motoristaId));

        if (!viagemDependenteRepository.existeAcordoAtivo(responsavel.getId(), motoristaId)) {
            throw new AccessDeniedException("Só é possível avaliar um motorista com acordo de transporte ativo");
        }
        if (nota == null || nota < 1 || nota > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 1 e 5");
        }

        Avaliacao avaliacao = avaliacaoRepository.findByResponsavelIdAndMotoristaId(responsavel.getId(), motoristaId)
                .orElseGet(Avaliacao::new);
        LocalDateTime agora = LocalDateTime.now();
        if (avaliacao.getId() == null) {
            avaliacao.setResponsavel(responsavel);
            avaliacao.setMotorista(motorista);
            avaliacao.setCriadoEm(agora);
        }
        avaliacao.setNota(nota);
        avaliacao.setComentario(comentario != null && !comentario.isBlank() ? comentario.trim() : null);
        avaliacao.setAtualizadoEm(agora);
        return AvaliacaoDTO.toDTO(avaliacaoRepository.save(avaliacao));
    }

    @Transactional(readOnly = true)
    public AvaliacaoDTO.Resumo resumo(Long motoristaId) {
        if (!motoristaRepository.existsById(motoristaId)) {
            throw new ResourceNotFoundException("Motorista não encontrado com id: " + motoristaId);
        }
        Double media = avaliacaoRepository.mediaDoMotorista(motoristaId);
        List<Avaliacao> todas = avaliacaoRepository.listarDoMotorista(motoristaId);
        List<AvaliacaoDTO> recentes = todas.stream()
                .limit(MAX_AVALIACOES_RETORNADAS)
                .map(AvaliacaoDTO::toDTO)
                .toList();
        return new AvaliacaoDTO.Resumo(arredondar(media), todas.size(), recentes);
    }

    static Double arredondar(Double media) {
        if (media == null) return null;
        return BigDecimal.valueOf(media).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}

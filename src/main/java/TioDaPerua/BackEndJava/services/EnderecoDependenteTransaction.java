package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.EnderecoDependenteDTO;
import TioDaPerua.BackEndJava.DTOs.EnderecoDependenteRequestDTO;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.EnderecoDependente;
import TioDaPerua.BackEndJava.enums.FinalidadeEndereco;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.EnderecoDependenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class EnderecoDependenteTransaction {
    private final DependenteRepository dependenteRepository;
    private final EnderecoDependenteRepository enderecoRepository;

    public EnderecoDependenteTransaction(DependenteRepository dependenteRepository,
                                         EnderecoDependenteRepository enderecoRepository) {
        this.dependenteRepository = dependenteRepository;
        this.enderecoRepository = enderecoRepository;
    }

    @Transactional(readOnly = true)
    public List<EnderecoDependenteDTO> listar(Long dependenteId, Long usuarioId) {
        validarAcesso(dependenteId, usuarioId);
        return enderecoRepository.findByDependenteIdAndAtivoTrueOrderById(dependenteId).stream()
                .map(EnderecoDependenteDTO::toDTO)
                .toList();
    }

    @Transactional
    public EnderecoDependenteDTO adicionar(Long dependenteId,
                                           Long usuarioId,
                                           EnderecoDependenteRequestDTO request,
                                           StatusValidacaoEndereco statusValidacao) {
        Dependente dependente = buscarDependenteParaEdicao(dependenteId, usuarioId);
        validarFinalidadeDisponivel(dependenteId, null, request.finalidade());
        Instant agora = Instant.now();
        EnderecoDependente endereco = new EnderecoDependente();
        endereco.setDependente(dependente);
        endereco.setCriadoEm(agora);
        preencherEndereco(endereco, request, statusValidacao, agora);
        return EnderecoDependenteDTO.toDTO(enderecoRepository.save(endereco));
    }

    @Transactional
    public EnderecoDependenteDTO alterar(Long dependenteId,
                                         Long enderecoId,
                                         Long usuarioId,
                                         EnderecoDependenteRequestDTO request,
                                         StatusValidacaoEndereco statusValidacao) {
        buscarDependenteParaEdicao(dependenteId, usuarioId);
        EnderecoDependente endereco = enderecoRepository.findByIdAndDependenteId(enderecoId, dependenteId)
                .filter(EnderecoDependente::isAtivo)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço ativo não encontrado para o dependente"));
        validarFinalidadeDisponivel(dependenteId, enderecoId, request.finalidade());
        preencherEndereco(endereco, request, statusValidacao, Instant.now());
        return EnderecoDependenteDTO.toDTO(enderecoRepository.save(endereco));
    }

    @Transactional
    public void desativar(Long dependenteId, Long enderecoId, Long usuarioId) {
        buscarDependenteParaEdicao(dependenteId, usuarioId);
        EnderecoDependente endereco = enderecoRepository.findByIdAndDependenteId(enderecoId, dependenteId)
                .filter(EnderecoDependente::isAtivo)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço ativo não encontrado para o dependente"));
        endereco.setAtivo(false);
        endereco.setAtualizadoEm(Instant.now());
        enderecoRepository.save(endereco);
    }

    private void preencherEndereco(EnderecoDependente endereco,
                                   EnderecoDependenteRequestDTO request,
                                   StatusValidacaoEndereco statusValidacao,
                                   Instant agora) {
        endereco.setCep(request.cep().replace("-", ""));
        endereco.setLogradouro(request.logradouro().trim());
        endereco.setNumero(request.numero().trim());
        endereco.setComplemento(request.complemento() == null || request.complemento().isBlank()
                ? null : request.complemento().trim());
        endereco.setBairro(request.bairro().trim());
        endereco.setCidade(request.cidade().trim());
        endereco.setUf(request.uf().trim().toUpperCase(java.util.Locale.ROOT));
        endereco.setFinalidade(request.finalidade());
        endereco.setStatusValidacao(statusValidacao);
        endereco.setAtivo(true);
        endereco.setAtualizadoEm(agora);
    }

    private void validarFinalidadeDisponivel(Long dependenteId, Long enderecoId, FinalidadeEndereco finalidade) {
        long idExcluido = enderecoId == null ? -1L : enderecoId;
        if (enderecoRepository.existeConflitoDeFinalidade(
                dependenteId, idExcluido, finalidade, FinalidadeEndereco.AMBOS)) {
            throw new ConflictException("Já existe endereço ativo para esta finalidade");
        }
    }

    private Dependente buscarDependenteParaEdicao(Long dependenteId, Long usuarioId) {
        return dependenteRepository.findByIdAndResponsavelUsuarioIdForUpdate(dependenteId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dependente não encontrado para o responsável autenticado"));
    }

    private void validarAcesso(Long dependenteId, Long usuarioId) {
        if (dependenteRepository.findByIdAndResponsavelUsuarioId(dependenteId, usuarioId).isEmpty()) {
            throw new ResourceNotFoundException("Dependente não encontrado para o responsável autenticado");
        }
    }
}

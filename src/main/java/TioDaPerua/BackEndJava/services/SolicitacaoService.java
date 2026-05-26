package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.repositories.*;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SolicitacaoService {

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private DependenteRepository dependenteRepository;

    @Autowired
    private ViagemRepository viagemRepository;

    @Autowired
    private ViagemDependenteRepository viagemDependenteRepository;

    public List<Dependente> listarDependentesDisponiveis(Long idViagem) {
        return dependenteRepository.findDisponiveisParaViagem(idViagem);
    }

    public Solicitacao solicitarDependente(Long idViagem, Long idDependente) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        Dependente dependente = dependenteRepository.findById(idDependente)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado com id: " + idDependente));

        // Validar dependente possui escola
        if (dependente.getEscola() == null) {
            throw new BusinessException("Dependente deve estar vinculado a uma escola para solicitar transporte");
        }

        // Validar solicitação ainda não existe
        Optional<Solicitacao> existente = solicitacaoRepository.findPendenteByDependenteAndViagem(idDependente, idViagem);
        if (existente.isPresent()) {
            throw new ConflictException("Já existe uma solicitação pendente para este dependente nesta viagem");
        }

        // Criar solicitação
        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setViagem(viagem);
        solicitacao.setDependente(dependente);
        
        // Pega o primeiro responsável (regra: deve ter pelo menos um)
        if (dependente.getResponsaveis() != null && !dependente.getResponsaveis().isEmpty()) {
            solicitacao.setResponsavel(dependente.getResponsaveis().get(0));
        } else {
            throw new BusinessException("Dependente não possui responsáveis vinculados");
        }

        solicitacao.setRespondido(false);
        solicitacao.setAceito(false);
        solicitacao.setDataInicio(LocalDateTime.now());

        return solicitacaoRepository.save(solicitacao);
    }

    public Solicitacao responderSolicitacao(Long idSolicitacao, boolean aceito) {
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com id: " + idSolicitacao));

        if (solicitacao.isRespondido()) {
            throw new ConflictException("Solicitação já foi respondida");
        }

        solicitacao.setRespondido(true);
        solicitacao.setAceito(aceito);

        if (aceito) {
            // Criar vínculo ativo em ViagemDependente
            ViagemDependente vinculo = new ViagemDependente();
            vinculo.setViagem(solicitacao.getViagem());
            vinculo.setDependente(solicitacao.getDependente());
            vinculo.setAtivo(true);
            viagemDependenteRepository.save(vinculo);
        }

        return solicitacaoRepository.save(solicitacao);
    }
}

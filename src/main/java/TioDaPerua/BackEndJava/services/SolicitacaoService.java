package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.repositories.*;
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
        Optional<Viagem> viagemOpt = viagemRepository.findById(idViagem);
        Optional<Dependente> dependenteOpt = dependenteRepository.findById(idDependente);

        if (viagemOpt.isEmpty() || dependenteOpt.isEmpty()) return null;

        Viagem viagem = viagemOpt.get();
        Dependente dependente = dependenteOpt.get();

        // Validar dependente possui escola
        if (dependente.getEscola() == null) return null;

        // Validar solicitação ainda não existe
        Optional<Solicitacao> existente = solicitacaoRepository.findPendenteByDependenteAndViagem(idDependente, idViagem);
        if (existente.isPresent()) return null;

        // Criar solicitação
        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setViagem(viagem);
        solicitacao.setDependente(dependente);
        
        // Pega o primeiro responsável (regra: deve ter pelo menos um)
        if (dependente.getResponsaveis() != null && !dependente.getResponsaveis().isEmpty()) {
            solicitacao.setResponsavel(dependente.getResponsaveis().get(0));
        } else {
            return null; // Não pode solicitar sem responsável
        }

        solicitacao.setRespondido(false);
        solicitacao.setAceito(false);
        solicitacao.setDataInicio(LocalDateTime.now());

        return solicitacaoRepository.save(solicitacao);
    }

    public Solicitacao responderSolicitacao(Long idSolicitacao, boolean aceito) {
        Optional<Solicitacao> solicitacaoOpt = solicitacaoRepository.findById(idSolicitacao);
        if (solicitacaoOpt.isEmpty()) return null;

        Solicitacao solicitacao = solicitacaoOpt.get();
        if (solicitacao.isRespondido()) return null;

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

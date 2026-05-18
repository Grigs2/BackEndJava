package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ResponsavelService {

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private DependenteRepository dependenteRepository;

    @Autowired
    private ViagemPresencaRepository viagemPresencaRepository;

    @Autowired
    private EscolaRepository escolaRepository;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private ViagemDependenteRepository viagemDependenteRepository;

    public Dependente buscarDependente(Long idDependente) {
        return dependenteRepository.findById(idDependente).orElse(null);
    }

    public List<ViagemPresenca> buscarHistoricoDependente(Long idDependente) {
        return viagemPresencaRepository.findHistoricoByDependenteId(idDependente);
    }

    public Responsavel cadastrarResponsavel(Responsavel responsavel) {
        if (responsavel == null) return null;

        if (responsavel.getId() != null) {
            Optional<Responsavel> responsavelOpt = responsavelRepository.findById(responsavel.getId());
            if (responsavelOpt.isPresent()) {
                Responsavel responsavelExistente = responsavelOpt.get();
                if (responsavel.getDependentes() != null) {
                    for (Dependente d : responsavel.getDependentes()) {
                        if (!responsavelExistente.getDependentes().contains(d)) {
                            responsavelExistente.getDependentes().add(d);
                        }
                    }
                }
                return responsavelRepository.save(responsavelExistente);
            }
        }
        return responsavelRepository.save(responsavel);
    }

    public Responsavel cadastrarDependente(Long idResponsavel, Dependente dependente) {
        if (dependente == null || idResponsavel == null) return null;
        
        Optional<Responsavel> responsavelOpt = responsavelRepository.findById(idResponsavel);
        if (responsavelOpt.isPresent()) {
            Responsavel responsavelExistente = responsavelOpt.get();
            if (responsavelExistente.getDependentes() != null) {
                if (!responsavelExistente.getDependentes().contains(dependente)) {
                    responsavelExistente.getDependentes().add(dependente);
                } else {
                    return null; // Dependente já cadastrado
                }
            } else {
                responsavelExistente.setDependentes(new java.util.ArrayList<>());
                responsavelExistente.getDependentes().add(dependente);
            }
            return responsavelRepository.save(responsavelExistente);
        }
        return null;
    }

    public Responsavel vincularEscola(Long idResponsavel, Long idEscola, Long idDependente) {
        if (idResponsavel == null || idEscola == null || idDependente == null) return null;

        Optional<Responsavel> responsavelOpt = responsavelRepository.findById(idResponsavel);
        Optional<Escola> escolaOpt = escolaRepository.findById(idEscola);

        if (responsavelOpt.isPresent() && escolaOpt.isPresent()) {
            Optional<Dependente> dependenteOpt = dependenteRepository.findByIdAndResponsavelId(idDependente, idResponsavel);
            if (dependenteOpt.isPresent()) {
                Dependente dependente = dependenteOpt.get();
                dependente.setEscola(escolaOpt.get());
                dependenteRepository.save(dependente);
                return responsavelRepository.findById(idResponsavel).orElse(null);
            }
        }
        return null;
    }

    public List<Solicitacao> listarSolicitacoes(Long idResponsavel) {
        if (idResponsavel == null) return null;
        return solicitacaoRepository.findByResponsavelId(idResponsavel);
    }

    public Solicitacao encerrarVinculoViagem(Long idSolicitacao) {
        if (idSolicitacao == null) return null;

        Optional<Solicitacao> solicitacaoOpt = solicitacaoRepository.findById(idSolicitacao);
        if (solicitacaoOpt.isEmpty()) return null;

        Solicitacao solicitacao = solicitacaoOpt.get();

        // Validar que a solicitação está aceita e respondida (vínculo ativo)
        if (!solicitacao.isAceito() || !solicitacao.isRespondido()) return null;
        // Validar que já não foi encerrada
        if (solicitacao.getDataFim() != null) return null;

        // Atualizar Solicitação
        solicitacao.setDataFim(LocalDateTime.now());
        solicitacaoRepository.save(solicitacao);

        // Localizar ViagemDependente correspondente
        if (solicitacao.getViagem() != null && solicitacao.getDependente() != null) {
            Optional<ViagemDependente> vdOpt = viagemDependenteRepository.findByViagemIdAndDependenteId(
                    solicitacao.getViagem().getId(),
                    solicitacao.getDependente().getId()
            );

            if (vdOpt.isPresent()) {
                ViagemDependente vd = vdOpt.get();
                vd.setAtivo(false);
                viagemDependenteRepository.save(vd);
            }
        }

        return solicitacao;
    }
}

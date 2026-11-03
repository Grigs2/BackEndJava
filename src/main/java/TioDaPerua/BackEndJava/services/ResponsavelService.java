package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.validation.SenhaPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TermosService termosService;

    @Autowired
    private CadastroUsuarioValidator cadastroUsuarioValidator;

    public Dependente buscarDependente(Long idDependente) {
        return dependenteRepository.findById(idDependente)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado com id: " + idDependente));
    }

    public List<ViagemPresenca> buscarHistoricoDependente(Long idDependente) {
        return viagemPresencaRepository.findHistoricoByDependenteId(idDependente);
    }

    public ViagemPresenca buscarStatusAtualDependente(Long idDependente) {
        List<ViagemPresenca> historico = viagemPresencaRepository.findHistoricoByDependenteId(idDependente);
        if (historico.isEmpty()) return null;

        return historico.stream()
                .filter(vp -> vp.getViagemDia().getStatus() == StatusViagemDia.EM_ANDAMENTO)
                .findFirst()
                .orElse(historico.get(0));
    }

    public Responsavel cadastrarResponsavel(Responsavel responsavel) {
        if (responsavel == null) throw new IllegalArgumentException("Responsável não pode ser nulo");

        if (responsavel.getId() != null) {
            Responsavel responsavelExistente = responsavelRepository.findById(responsavel.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado com id: " + responsavel.getId()));

            if (responsavel.getDependentes() != null) {
                for (Dependente d : responsavel.getDependentes()) {
                    if (!responsavelExistente.getDependentes().contains(d)) {
                        responsavelExistente.getDependentes().add(d);
                    }
                }
            }
            return responsavelRepository.save(responsavelExistente);
        }
        if (responsavel.getNome() == null || responsavel.getNome().isBlank()) {
            throw new BusinessException("Nome do responsável é obrigatório");
        }
        if (responsavel.getUsuario() == null || responsavel.getUsuario().getId() != null
                || responsavel.getUsuario().getSenha() == null
                || responsavel.getUsuario().getSenha().isBlank()
                || (responsavel.getDependentes() != null && !responsavel.getDependentes().isEmpty())) {
            throw new IllegalArgumentException("Cadastro deve conter credenciais novas e não pode vincular dependentes existentes");
        }
        responsavel.setCpf(validarCpf(responsavel.getCpf()));
        validarDataNascimento(responsavel.getDataNascimento());
        cadastroUsuarioValidator.validarNovoCadastro(responsavel.getUsuario());
        SenhaPolicy.exigirValida(responsavel.getUsuario().getSenha());
        termosService.registrarAceite(responsavel.getUsuario(),
                responsavel.getUsuario().getAceitouTermos());
        responsavel.getUsuario().setSenha(passwordEncoder.encode(responsavel.getUsuario().getSenha()));
        return responsavelRepository.save(responsavel);
    }

    @Transactional
    public Responsavel cadastrarDependente(Long idResponsavel, Dependente dependente) {
        if (idResponsavel == null || dependente == null) throw new IllegalArgumentException("IDs de responsável e dependente são obrigatórios");
        if (dependente.getDataNascimento() != null && dependente.getDataNascimento().isAfter(java.time.LocalDate.now())) {
            throw new BusinessException("A data de nascimento do dependente não pode estar no futuro");
        }
        if (dependente.getCpf() != null && !dependente.getCpf().isBlank()) {
            dependente.setCpf(validarCpfDependente(dependente.getCpf()));
        } else {
            dependente.setCpf(null);
        }
        if (dependente.getId() != null) {
            throw new IllegalArgumentException("Um novo dependente não pode reutilizar o ID de um cadastro existente");
        }
        
        Responsavel responsavelExistente = responsavelRepository.findById(idResponsavel)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado com id: " + idResponsavel));

        if (responsavelExistente.getUsuario() == null) {
            throw new BusinessException("Responsável sem usuário associado não pode cadastrar dependente");
        }
        if (responsavelExistente.getDependentes() != null) {
            if (!responsavelExistente.getDependentes().contains(dependente)) {
                responsavelExistente.getDependentes().add(dependente);
            } else {
                throw new ConflictException("Dependente já cadastrado para este responsável");
            }
        } else {
            responsavelExistente.setDependentes(new java.util.ArrayList<>());
            responsavelExistente.getDependentes().add(dependente);
        }
        dependente.setResponsavelCriador(responsavelExistente);
        dependente.setResponsavelCriadorDefinidoEm(java.time.Instant.now());
        dependente.setResponsavelCriadorDefinidoPorUsuarioId(responsavelExistente.getUsuario().getId());
        return responsavelRepository.save(responsavelExistente);
    }

    public Responsavel vincularEscola(Long idResponsavel, Long idEscola, Long idDependente) {
        if (idResponsavel == null || idEscola == null || idDependente == null) {
            throw new IllegalArgumentException("IDs de responsável, escola e dependente são obrigatórios");
        }

        Responsavel responsavel = responsavelRepository.findById(idResponsavel)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado com id: " + idResponsavel));
        Escola escola = escolaRepository.findById(idEscola)
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada com id: " + idEscola));

        Dependente dependente = dependenteRepository.findByIdAndResponsavelId(idDependente, idResponsavel)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado ou não pertence a este responsável"));

        dependente.setEscola(escola);
        dependenteRepository.save(dependente);
        return responsavel;
    }

    public List<Solicitacao> listarSolicitacoes(Long idResponsavel) {
        if (idResponsavel == null) throw new IllegalArgumentException("ID do responsável é obrigatório");
        return solicitacaoRepository.findByResponsavelId(idResponsavel);
    }

    public Solicitacao encerrarVinculoViagem(Long idSolicitacao) {
        if (idSolicitacao == null) throw new IllegalArgumentException("ID da solicitação é obrigatório");

        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com id: " + idSolicitacao));

        // Validar que a solicitação está aceita e respondida (vínculo ativo)
        if (!solicitacao.isAceito() || !solicitacao.isRespondido()) {
            throw new BusinessException("Apenas solicitações aceitas e respondidas podem ter o vínculo encerrado");
        }
        // Validar que já não foi encerrada
        if (solicitacao.getDataFim() != null) {
            throw new ConflictException("Vínculo já encerrado");
        }

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

    public Responsavel alterarInfos(Responsavel responsavel) {
        if (responsavel == null || responsavel.getId() == null) throw new IllegalArgumentException("ID do responsável é obrigatório");
        
        Responsavel existente = responsavelRepository.findById(responsavel.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado com id: " + responsavel.getId()));

        existente.setNome(responsavel.getNome());
        if (responsavel.getCpf() != null) {
            String cpf = DocumentoFiscalValidator.normalizarCpf(responsavel.getCpf());
            if (!DocumentoFiscalValidator.cpfValido(cpf)) {
                throw new BusinessException("CPF inválido");
            }
            if (responsavelRepository.existsByCpfAndIdNot(cpf, existente.getId())) {
                throw new BusinessException("CPF já cadastrado");
            }
            existente.setCpf(cpf);
        }
        if (responsavel.getDataNascimento() != null) {
            validarDataNascimento(responsavel.getDataNascimento());
            existente.setDataNascimento(responsavel.getDataNascimento());
        }

        if (responsavel.getUsuario() != null && existente.getUsuario() != null) {
            Usuario uExistente = existente.getUsuario();
            Usuario uNovo = responsavel.getUsuario();
            cadastroUsuarioValidator.validarAtualizacao(uNovo, uExistente.getId());
            uExistente.setEmail(uNovo.getEmail());
            uExistente.setEndereco(uNovo.getEndereco());
            uExistente.setTelefone(uNovo.getTelefone());
        }

        return responsavelRepository.save(existente);
    }

    private String validarCpf(String cpfInformado) {
        String cpf = DocumentoFiscalValidator.normalizarCpf(cpfInformado);
        if (!DocumentoFiscalValidator.cpfValido(cpf)) {
            throw new BusinessException("CPF inválido");
        }
        if (responsavelRepository.existsByCpf(cpf)) {
            throw new BusinessException("CPF já cadastrado");
        }
        return cpf;
    }

    private String validarCpfDependente(String cpfInformado) {
        String cpf = DocumentoFiscalValidator.normalizarCpf(cpfInformado);
        if (!DocumentoFiscalValidator.cpfValido(cpf)) {
            throw new BusinessException("CPF inválido");
        }
        if (dependenteRepository.existsByCpf(cpf)) {
            throw new BusinessException("CPF já cadastrado");
        }
        return cpf;
    }

    private void validarDataNascimento(java.time.LocalDate dataNascimento) {
        if (dataNascimento != null && dataNascimento.isAfter(java.time.LocalDate.now())) {
            throw new BusinessException("A data de nascimento do responsável não pode estar no futuro");
        }
    }
}

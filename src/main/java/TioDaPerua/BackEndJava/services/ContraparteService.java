package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.ContraparteDTO;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.EnderecoDependente;
import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.AvaliacaoRepository;
import TioDaPerua.BackEndJava.repositories.EnderecoDependenteRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;

/**
 * Dados de quem está do outro lado de uma solicitação, mostrados no pop-up antes de aceitar o vínculo.
 * Só os participantes da solicitação (o motorista da viagem e os responsáveis do aluno) podem consultar.
 */
@Service
public class ContraparteService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final EnderecoDependenteRepository enderecoDependenteRepository;

    public ContraparteService(SolicitacaoRepository solicitacaoRepository,
                              AvaliacaoRepository avaliacaoRepository,
                              EnderecoDependenteRepository enderecoDependenteRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.enderecoDependenteRepository = enderecoDependenteRepository;
    }

    /** O responsável do aluno vê o motorista da viagem. */
    @Transactional(readOnly = true)
    public ContraparteDTO motoristaDaSolicitacao(Long idSolicitacao, Long atorUsuarioId) {
        Solicitacao s = buscar(idSolicitacao);
        Dependente dependente = s.getDependente();
        boolean ehResponsavel = dependente != null && dependente.getResponsaveis() != null
                && dependente.getResponsaveis().stream().anyMatch(r -> r.getUsuario() != null
                && Objects.equals(r.getUsuario().getId(), atorUsuarioId));
        if (!ehResponsavel) {
            throw new AccessDeniedException("Somente o responsável do aluno pode ver os dados do motorista");
        }
        Viagem viagem = s.getViagem();
        Motorista m = viagem != null ? viagem.getMotorista() : null;
        if (m == null) {
            throw new ResourceNotFoundException("Motorista da solicitação não encontrado");
        }
        Usuario u = m.getUsuario();
        EnderecoOperacional end = u != null ? u.getEnderecoOperacional() : null;
        var v = m.getVeiculo();
        ContraparteDTO.VeiculoResumo veiculo = v == null ? null : new ContraparteDTO.VeiculoResumo(
                v.getModelo(), v.getCor(), v.getAno(), v.getCapacidade(), mascararPlaca(v.getPlaca()));
        var avaliacoes = avaliacaoRepository.listarDoMotorista(m.getId());
        Double media = avaliacoes.isEmpty() ? null : avaliacaoRepository.mediaDoMotorista(m.getId());
        return new ContraparteDTO(s.getId(), "MOTORISTA", m.getNome(),
                u != null ? u.getTelefone() : null,
                u != null ? mascararEmail(u.getEmail()) : null,
                mascararCpf(m.getCpf()),
                end != null ? end.getBairro() : null,
                end != null ? end.getCidade() : null,
                end != null ? end.getUf() : null,
                viagem.getNome(),
                viagem.getPeriodo() != null ? viagem.getPeriodo().name() : null,
                viagem.getEscolas() == null ? List.of() : viagem.getEscolas().stream().map(Escola::getNome).toList(),
                veiculo, media, avaliacoes.size(),
                aluno(dependente));
    }

    /** O motorista da viagem vê o responsável e o aluno da solicitação. */
    @Transactional(readOnly = true)
    public ContraparteDTO responsavelDaSolicitacao(Long idSolicitacao, Long atorUsuarioId) {
        Solicitacao s = buscar(idSolicitacao);
        Viagem viagem = s.getViagem();
        Usuario usuarioMotorista = viagem != null && viagem.getMotorista() != null ? viagem.getMotorista().getUsuario() : null;
        if (usuarioMotorista == null || !Objects.equals(usuarioMotorista.getId(), atorUsuarioId)) {
            throw new AccessDeniedException("Somente o motorista da viagem pode ver os dados do responsável");
        }
        Responsavel r = s.getResponsavel();
        if (r == null) {
            throw new ResourceNotFoundException("Responsável da solicitação não encontrado");
        }
        Usuario u = r.getUsuario();
        EnderecoOperacional end = u != null ? u.getEnderecoOperacional() : null;
        ContraparteDTO.AlunoResumo aluno = aluno(s.getDependente());
        return new ContraparteDTO(s.getId(), "RESPONSAVEL", r.getNome(),
                u != null ? u.getTelefone() : null,
                u != null ? mascararEmail(u.getEmail()) : null,
                mascararCpf(r.getCpf()),
                end != null ? end.getBairro() : aluno != null ? aluno.bairro() : null,
                end != null ? end.getCidade() : aluno != null ? aluno.cidade() : null,
                end != null ? end.getUf() : null,
                viagem.getNome(),
                viagem.getPeriodo() != null ? viagem.getPeriodo().name() : null,
                viagem.getEscolas() == null ? List.of() : viagem.getEscolas().stream().map(Escola::getNome).toList(),
                null, null, null,
                aluno);
    }

    private Solicitacao buscar(Long idSolicitacao) {
        return solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com id: " + idSolicitacao));
    }

    private ContraparteDTO.AlunoResumo aluno(Dependente d) {
        if (d == null) return null;
        Integer idade = d.getDataNascimento() == null ? null : Period.between(d.getDataNascimento(), LocalDate.now()).getYears();
        List<EnderecoDependente> enderecos = d.getId() == null ? List.of()
                : enderecoDependenteRepository.findByDependenteIdAndAtivoTrueOrderById(d.getId());
        EnderecoDependente e = enderecos.isEmpty() ? null : enderecos.get(0);
        return new ContraparteDTO.AlunoResumo(d.getNome(), idade,
                d.getEscola() != null ? d.getEscola().getNome() : null, d.getPeriodo(),
                e != null ? e.getBairro() : null, e != null ? e.getCidade() : null);
    }

    /** "529.982.247-25" → "***.982.247-**". */
    static String mascararCpf(String cpf) {
        String d = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (d.length() != 11) return null;
        return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }

    /** "marina.alves@exemplo.dev" → "ma***@exemplo.dev". */
    static String mascararEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        int at = email.indexOf('@');
        String usuario = email.substring(0, at);
        String inicio = usuario.length() <= 2 ? usuario.substring(0, 1) : usuario.substring(0, 2);
        return inicio + "***" + email.substring(at);
    }

    /** "ABC1D23" → "ABC****". */
    static String mascararPlaca(String placa) {
        if (placa == null || placa.isBlank()) return null;
        String p = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return p.length() <= 3 ? p : p.substring(0, 3) + "*".repeat(p.length() - 3);
    }
}

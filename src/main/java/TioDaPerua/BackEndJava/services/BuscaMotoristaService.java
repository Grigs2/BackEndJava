package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.MotoristaDisponivelDTO;
import TioDaPerua.BackEndJava.DTOs.ViagemDTO;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.EnderecoDependente;
import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.EnderecoDependenteRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.ViagemEscolaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Busca de motoristas para o responsável, por nome e por proximidade.
 *
 * A proximidade é aproximada pelo endereço: compara bairro e cidade de atuação do motorista com o endereço do aluno
 * (endereço estruturado, quando houver) ou com o endereço do responsável. O sistema não guarda coordenadas, então
 * não há cálculo de distância em quilômetros.
 */
@Service
public class BuscaMotoristaService {

    public static final String MESMO_BAIRRO = "MESMO_BAIRRO";
    public static final String MESMA_CIDADE = "MESMA_CIDADE";
    public static final String OUTRA_CIDADE = "OUTRA_CIDADE";
    public static final String SEM_REFERENCIA = "SEM_REFERENCIA";

    @Autowired private MotoristaRepository motoristaRepository;
    @Autowired private ViagemRepository viagemRepository;
    @Autowired private ViagemEscolaRepository viagemEscolaRepository;
    @Autowired private ResponsavelRepository responsavelRepository;
    @Autowired private DependenteRepository dependenteRepository;
    @Autowired private EnderecoDependenteRepository enderecoDependenteRepository;
    @Autowired private TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository viagemDependenteRepository;
    @Autowired private TioDaPerua.BackEndJava.repositories.SolicitacaoRepository solicitacaoRepository;

    /**
     * @param nome            filtro opcional pelo nome do motorista (ignora maiúsculas e acentos)
     * @param idDependente    aluno usado como referência de proximidade e de escola (opcional)
     * @param somenteProximos quando true, devolve só motoristas do mesmo bairro ou da mesma cidade
     */
    @Transactional(readOnly = true)
    public List<MotoristaDisponivelDTO> buscar(Long idResponsavel, String nome, Long idDependente, boolean somenteProximos) {
        Responsavel responsavel = responsavelRepository.findById(idResponsavel)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado com id: " + idResponsavel));

        Referencia referencia = referencia(responsavel, idDependente);
        String filtro = SolicitacaoService.normalizar(nome);

        List<Long> escolaIds = escolasDeReferencia(responsavel, idDependente);
        if (escolaIds.isEmpty()) {
            return List.of();
        }
        Set<Long> motoristasDasEscolas = Set.copyOf(
                viagemEscolaRepository.listarIdsMotoristasAtivosDasEscolas(escolaIds, StatusVinculoEscola.ATIVO));

        // Situação do aluno pesquisado em cada viagem, para o app não oferecer "Pedir vaga" onde ele já está.
        List<Long> viagensComAluno = idDependente == null || viagemDependenteRepository == null ? List.of()
                : viagemDependenteRepository.findAtivosByDependenteId(idDependente).stream()
                        .map(vd -> vd.getViagem().getId()).distinct().toList();
        List<Long> viagensPendentes = idDependente == null || solicitacaoRepository == null ? List.of()
                : solicitacaoRepository.findPendentesByDependenteId(idDependente).stream()
                        .filter(s -> s.getViagem() != null)
                        .map(s -> s.getViagem().getId()).distinct().toList();

        return motoristaRepository.findAtivosComVeiculo().stream()
                .filter(m -> motoristasDasEscolas.contains(m.getId()))
                .filter(m -> filtro.isEmpty() || SolicitacaoService.normalizar(m.getNome()).contains(filtro))
                .map(m -> toDTO(m, referencia, viagensComAluno, viagensPendentes))
                .filter(dto -> !somenteProximos || MESMO_BAIRRO.equals(dto.proximidade()) || MESMA_CIDADE.equals(dto.proximidade()))
                .sorted(Comparator.comparingInt((MotoristaDisponivelDTO dto) -> ordem(dto.proximidade()))
                        .thenComparing(MotoristaDisponivelDTO::nome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    /** Escolas do dependente informado ou, sem ele, de todos os dependentes do responsável. */
    private List<Long> escolasDeReferencia(Responsavel responsavel, Long idDependente) {
        List<Dependente> dependentes = idDependente != null
                ? dependenteRepository.findById(idDependente).map(List::of).orElse(List.of())
                : (responsavel.getDependentes() != null ? responsavel.getDependentes() : List.<Dependente>of());
        return dependentes.stream()
                .filter(d -> idDependente == null || Objects.equals(d.getId(), idDependente))
                .map(Dependente::getEscola)
                .filter(Objects::nonNull)
                .map(Escola::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private MotoristaDisponivelDTO toDTO(Motorista motorista, Referencia referencia,
                                         List<Long> viagensComAluno, List<Long> viagensPendentes) {
        EnderecoOperacional endereco = motorista.getUsuario() != null ? motorista.getUsuario().getEnderecoOperacional() : null;
        String bairro = endereco != null ? endereco.getBairro() : null;
        String cidade = endereco != null ? endereco.getCidade() : null;
        List<ViagemDTO> viagens = viagemRepository.findAtivasByMotoristaId(motorista.getId()).stream()
                .map(ViagemDTO::toDTO)
                .toList();
        var veiculo = motorista.getVeiculo();
        return new MotoristaDisponivelDTO(
                motorista.getId(),
                motorista.getNome(),
                veiculo != null ? veiculo.getModelo() : null,
                veiculo != null ? veiculo.getCor() : null,
                veiculo != null ? veiculo.getCapacidade() : null,
                bairro,
                cidade,
                endereco != null ? endereco.getUf() : null,
                proximidade(bairro, cidade, referencia),
                viagens,
                viagens.stream().map(ViagemDTO::id).filter(viagensComAluno::contains).toList(),
                viagens.stream().map(ViagemDTO::id).filter(viagensPendentes::contains).toList());
    }

    static String proximidade(String bairro, String cidade, Referencia referencia) {
        String b = SolicitacaoService.normalizar(bairro);
        String c = SolicitacaoService.normalizar(cidade);
        if (referencia == null || c.isEmpty()) return SEM_REFERENCIA;
        if (referencia.estruturada()) {
            if (!c.equals(referencia.cidade())) return OUTRA_CIDADE;
            return !b.isEmpty() && b.equals(referencia.bairro()) ? MESMO_BAIRRO : MESMA_CIDADE;
        }
        String texto = referencia.texto();
        boolean mesmaCidade = texto.contains(c) || c.equals(referencia.cidadeEscola());
        if (!mesmaCidade) return OUTRA_CIDADE;
        return !b.isEmpty() && texto.contains(b) ? MESMO_BAIRRO : MESMA_CIDADE;
    }

    /** Cidade da escola do dependente: o motorista só atende essa escola, então ela serve de referência quando o endereço texto não traz cidade. */
    private String cidadeDaEscola(Dependente dependente) {
        Escola escola = dependente.getEscola();
        if (escola == null || escola.getUsuario() == null || escola.getUsuario().getEnderecoOperacional() == null) {
            return null;
        }
        return SolicitacaoService.normalizar(escola.getUsuario().getEnderecoOperacional().getCidade());
    }

    private Referencia referencia(Responsavel responsavel, Long idDependente) {
        if (idDependente != null) {
            Dependente dependente = dependenteRepository.findById(idDependente)
                    .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado com id: " + idDependente));
            boolean doResponsavel = dependente.getResponsaveis() != null && dependente.getResponsaveis().stream()
                    .anyMatch(r -> Objects.equals(r.getId(), responsavel.getId()));
            if (!doResponsavel) {
                throw new AccessDeniedException("O dependente não pertence a este responsável");
            }
            List<EnderecoDependente> enderecos = enderecoDependenteRepository.findByDependenteIdAndAtivoTrueOrderById(idDependente);
            if (!enderecos.isEmpty()) {
                EnderecoDependente e = enderecos.get(0);
                return Referencia.estruturada(SolicitacaoService.normalizar(e.getBairro()), SolicitacaoService.normalizar(e.getCidade()));
            }
            if (dependente.getEndereco() != null && !dependente.getEndereco().isBlank()) {
                return Referencia.texto(SolicitacaoService.normalizar(dependente.getEndereco()), cidadeDaEscola(dependente));
            }
        }
        String enderecoResponsavel = responsavel.getUsuario() != null ? responsavel.getUsuario().getEndereco() : null;
        if (enderecoResponsavel == null || enderecoResponsavel.isBlank()) return null;
        return Referencia.texto(SolicitacaoService.normalizar(enderecoResponsavel), null);
    }

    private static int ordem(String proximidade) {
        return switch (proximidade) {
            case MESMO_BAIRRO -> 0;
            case MESMA_CIDADE -> 1;
            case SEM_REFERENCIA -> 2;
            default -> 3;
        };
    }

    record Referencia(boolean estruturada, String bairro, String cidade, String texto, String cidadeEscola) {
        static Referencia estruturada(String bairro, String cidade) {
            return new Referencia(true, bairro, cidade, null, null);
        }

        static Referencia texto(String texto, String cidadeEscola) {
            return new Referencia(false, null, null, texto, cidadeEscola);
        }
    }
}

package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.DependenteParadaDTO;
import TioDaPerua.BackEndJava.DTOs.LinhaParadaDTO;
import TioDaPerua.BackEndJava.DTOs.LinhaViagemDTO;
import TioDaPerua.BackEndJava.DTOs.ParadaDTO;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.TipoParada;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class LinhaViagemService {

    private final ViagemDiaRepository viagemDiaRepository;
    private final ViagemService viagemService;
    private final LocalizacaoViagemService localizacaoViagemService;

    public LinhaViagemService(ViagemDiaRepository viagemDiaRepository,
                              ViagemService viagemService,
                              LocalizacaoViagemService localizacaoViagemService) {
        this.viagemDiaRepository = viagemDiaRepository;
        this.viagemService = viagemService;
        this.localizacaoViagemService = localizacaoViagemService;
    }

    @Transactional(readOnly = true)
    public LinhaViagemDTO consultar(Long viagemDiaId, Long dependenteId, Long usuarioId) {
        ViagemDia execucao = viagemDiaRepository.findById(viagemDiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + viagemDiaId));
        localizacaoViagemService.validarLeituraLocalizacaoDoDependente(viagemDiaId, usuarioId, dependenteId);
        if (execucao.getStatus() != StatusViagemDia.EM_ANDAMENTO) {
            return LinhaViagemDTO.indisponivel("A linha aparece quando a viagem está em andamento.");
        }
        boolean ida = execucao.getViagem() != null && execucao.getViagem().getPeriodo() != null
                && execucao.getViagem().getPeriodo().name().endsWith("_IDA");
        return montar(viagemService.gerarParadasViagemDia(viagemDiaId), ida, dependenteId);
    }

    static LinhaViagemDTO montar(List<ParadaDTO> todas, boolean ida, long dependenteId) {
        List<ParadaDTO> paradas = todas.stream()
                .filter(p -> p.tipoParada() != TipoParada.MOTORISTA)
                .toList();

        DependenteParadaDTO aluno = paradas.stream()
                .flatMap(p -> alunos(p).stream())
                .filter(d -> d.id() != null && d.id() == dependenteId)
                .findFirst()
                .orElse(null);
        if (aluno == null) {
            return LinhaViagemDTO.indisponivel("Seu aluno não tem paradas nesta viagem.");
        }

        ParadaDTO proxima = ViagemService.proximaParadaPendente(todas, ida);
        Integer indiceAtual = proxima == null ? null : paradas.indexOf(proxima) + 1;
        int paradaDoAluno = RotaViagemService.paradaDoAluno(paradas, dependenteId, aluno.statusEmbarque(), ida);

        List<LinhaParadaDTO> linha = new ArrayList<>();
        for (int i = 0; i < paradas.size(); i++) {
            ParadaDTO parada = paradas.get(i);
            boolean embarque = RotaViagemService.embarque(parada, ida);
            boolean doFilho = i == paradaDoAluno;
            linha.add(new LinhaParadaDTO(
                    i + 1,
                    parada.tipoParada().name(),
                    embarque ? "EMBARQUE" : "DESEMBARQUE",
                    concluida(parada, embarque),
                    doFilho,
                    rotulo(parada, doFilho, aluno.nomeDependente())));
        }
        return new LinhaViagemDTO(true, null, indiceAtual, linha);
    }

    private static List<DependenteParadaDTO> alunos(ParadaDTO parada) {
        return parada.listaDependentes() == null ? List.of() : parada.listaDependentes();
    }

    /** Mesma regra do motorista: ninguém mais aguarda e ninguém a bordo deveria desembarcar nela. */
    private static boolean concluida(ParadaDTO parada, boolean embarque) {
        List<DependenteParadaDTO> alunos = alunos(parada);
        if (alunos.isEmpty()) {
            return false;
        }
        Set<String> resolvidos = embarque
                ? Set.of("EMBARCADO", "FALTOU")
                : Set.of("DESEMBARCADO", "FALTOU");
        return alunos.stream().allMatch(a -> resolvidos.contains(a.statusEmbarque()));
    }

    private static String rotulo(ParadaDTO parada, boolean doFilho, String nomeAluno) {
        if (parada.tipoParada() == TipoParada.ESCOLA) {
            return parada.nomeLocal();
        }
        if (!doFilho) {
            return null;
        }
        return "Casa do " + primeiroNome(nomeAluno);
    }

    private static String primeiroNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return "aluno";
        }
        return nome.trim().split("\\s+")[0];
    }
}

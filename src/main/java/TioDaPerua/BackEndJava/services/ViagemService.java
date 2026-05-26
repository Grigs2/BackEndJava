package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.*;
import TioDaPerua.BackEndJava.entities.*;
import TioDaPerua.BackEndJava.enums.*;
import TioDaPerua.BackEndJava.repositories.*;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViagemService {

    @Autowired
    private ViagemRepository viagemRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private ViagemDependenteRepository viagemDependenteRepository;

    @Autowired
    private ViagemDiaRepository viagemDiaRepository;

    @Autowired
    private ViagemPresencaRepository viagemPresencaRepository;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    public List<Viagem> listarViagensAtivas(Long idMotorista) {
        return viagemRepository.findAtivasByMotoristaId(idMotorista);
    }

    public List<ViagemDiaDTO> historicoViagens(Long idMotorista) {
        return viagemDiaRepository.findByMotoristaId(idMotorista).stream()
                .map(ViagemDiaDTO::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void desativarViagem(Long idViagem) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));

        if (!viagem.getAtivo()) {
            throw new ConflictException("Viagem já está desativada");
        }

        viagem.setAtivo(false);
        viagemRepository.save(viagem);

        viagemDependenteRepository.desativarPorViagemId(idViagem);
        solicitacaoRepository.encerrarPorViagemId(idViagem, LocalDateTime.now());
    }

    public MotoristaViagemDTO consultarMotorista(Long idViagemDia) {
        ViagemDia vd = viagemDiaRepository.findById(idViagemDia)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia));

        Viagem viagem = vd.getViagem();
        if (viagem == null) throw new BusinessException("Execução de viagem sem vínculo com itinerário");

        Motorista motorista = viagem.getMotorista();
        if (motorista == null) throw new BusinessException("Viagem sem motorista vinculado");

        Veiculo veiculo = motorista.getVeiculo();

        return new MotoristaViagemDTO(
                motorista.getId(),
                motorista.getNome(),
                motorista.getUsuario() != null ? motorista.getUsuario().getTelefone() : null,
                veiculo != null ? veiculo.getModelo() : null,
                veiculo != null ? veiculo.getPlaca() : null,
                veiculo != null ? veiculo.getCor() : "N/A"
        );
    }

    public List<ParadaDTO> gerarParadasViagemDia(Long idViagemDia) {
        ViagemDia viagemDia = viagemDiaRepository.findById(idViagemDia)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia));

        Viagem viagem = viagemDia.getViagem();
        if (viagem == null) return new ArrayList<>();

        List<ViagemDependente> vinculados = viagemDependenteRepository.buscarDependentesCompletos(viagem.getId());

        boolean ida = isViagemIda(viagem.getPeriodo());

        List<ParadaDTO> paradas = new ArrayList<>();
        int ordem = 1;

        paradas.add(criarParadaMotorista(ordem++, viagem.getMotorista(), "Saída: Casa do Motorista"));

        if (ida) {
            List<ParadaDTO> paradasResp = adicionarParadasResponsaveis(ordem, vinculados, idViagemDia);
            paradas.addAll(paradasResp);
            ordem = paradas.size() + 1;
            List<ParadaDTO> paradasEscola = adicionarParadasEscolas(ordem, vinculados, idViagemDia);
            paradas.addAll(paradasEscola);
        } else {
            List<ParadaDTO> paradasEscola = adicionarParadasEscolas(ordem, vinculados, idViagemDia);
            paradas.addAll(paradasEscola);
            ordem = paradas.size() + 1;
            List<ParadaDTO> paradasResp = adicionarParadasResponsaveis(ordem, vinculados, idViagemDia);
            paradas.addAll(paradasResp);
        }

        ordem = paradas.size() + 1;
        paradas.add(criarParadaMotorista(ordem, viagem.getMotorista(), "Retorno: Casa do Motorista"));

        return paradas;
    }

    private boolean isViagemIda(Periodo periodo) {
        return periodo == Periodo.MANHA_IDA || periodo == Periodo.TARDE_IDA || periodo == Periodo.NOITE_IDA;
    }

    private ParadaDTO criarParadaMotorista(int ordem, Motorista motorista, String label) {
        String endereco = (motorista != null && motorista.getUsuario() != null) ? motorista.getUsuario().getEndereco() : "Endereço não informado";
        return new ParadaDTO(ordem, TipoParada.MOTORISTA, label, endereco, new ArrayList<>());
    }

    private List<ParadaDTO> adicionarParadasResponsaveis(int ordemInicial, List<ViagemDependente> vinculados, Long idViagemDia) {
        Map<String, List<DependenteParadaDTO>> agrupamento = new LinkedHashMap<>();
        Map<String, String> nomesLocais = new HashMap<>();

        for (ViagemDependente vd : vinculados) {
            var dep = vd.getDependente();
            if (dep.getResponsaveis() != null && !dep.getResponsaveis().isEmpty()) {
                var resp = dep.getResponsaveis().get(0);
                String endereco = (resp.getUsuario() != null) ? resp.getUsuario().getEndereco() : "Endereço não informado";

                String statusEmbarque = viagemPresencaRepository.findByViagemDiaIdAndDependenteId(idViagemDia, dep.getId())
                        .map(p -> p.getStatus().name())
                        .orElse("N/A");

                agrupamento.computeIfAbsent(endereco, k -> new ArrayList<>())
                        .add(new DependenteParadaDTO(dep.getId(), dep.getNome(), resp.getNome(), statusEmbarque));
                nomesLocais.putIfAbsent(endereco, "Casa de " + resp.getNome());
            }
        }

        List<ParadaDTO> paradas = new ArrayList<>();
        int ordem = ordemInicial;
        for (Map.Entry<String, List<DependenteParadaDTO>> entry : agrupamento.entrySet()) {
            paradas.add(new ParadaDTO(ordem++, TipoParada.RESPONSAVEL, nomesLocais.get(entry.getKey()), entry.getKey(), entry.getValue()));
        }
        return paradas;
    }

    private List<ParadaDTO> adicionarParadasEscolas(int ordemInicial, List<ViagemDependente> vinculados, Long idViagemDia) {
        Map<String, List<DependenteParadaDTO>> agrupamento = new LinkedHashMap<>();
        Map<String, String> nomesLocais = new HashMap<>();

        for (ViagemDependente vd : vinculados) {
            var dep = vd.getDependente();
            var escola = dep.getEscola();
            if (escola != null) {
                String endereco = (escola.getUsuario() != null) ? escola.getUsuario().getEndereco() : "Endereço não informado";
                String nomeResp = (dep.getResponsaveis() != null && !dep.getResponsaveis().isEmpty()) ? dep.getResponsaveis().get(0).getNome() : "N/A";

                String statusEmbarque = viagemPresencaRepository.findByViagemDiaIdAndDependenteId(idViagemDia, dep.getId())
                        .map(p -> p.getStatus().name())
                        .orElse("N/A");

                agrupamento.computeIfAbsent(endereco, k -> new ArrayList<>())
                        .add(new DependenteParadaDTO(dep.getId(), dep.getNome(), nomeResp, statusEmbarque));
                nomesLocais.putIfAbsent(endereco, "Escola: " + escola.getNome());
            }
        }

        List<ParadaDTO> paradas = new ArrayList<>();
        int ordem = ordemInicial;
        for (Map.Entry<String, List<DependenteParadaDTO>> entry : agrupamento.entrySet()) {
            paradas.add(new ParadaDTO(ordem++, TipoParada.ESCOLA, nomesLocais.get(entry.getKey()), entry.getKey(), entry.getValue()));
        }
        return paradas;
    }

    public ViagemDia iniciarViagemDia(Long idViagem) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));

        LocalDate hoje = LocalDate.now();

        Optional<ViagemDia> existente = viagemDiaRepository.findByViagemIdAndData(idViagem, hoje);
        if (existente.isPresent()) return existente.get();

        ViagemDia viagemDia = new ViagemDia();
        viagemDia.setViagem(viagem);
        viagemDia.setData(hoje);
        viagemDia.setStatus(StatusViagemDia.PLANEJADA);
        viagemDia = viagemDiaRepository.save(viagemDia);

        List<ViagemDependente> ativos = viagemDependenteRepository.findAtivosByViagemId(idViagem);

        for (ViagemDependente vd : ativos) {
            ViagemPresenca presenca = new ViagemPresenca();
            presenca.setViagemDia(viagemDia);
            presenca.setDependente(vd.getDependente());
            presenca.setStatus(StatusViagemPresenca.ESPERANDO);
            viagemPresencaRepository.save(presenca);
        }

        return viagemDia;
    }

    public Viagem criarViagem(Long idMotorista, Viagem viagem) {
        if (viagem == null || idMotorista == null) throw new IllegalArgumentException("Dados da viagem e motorista são obrigatórios");

        Motorista motorista = motoristaRepository.findById(idMotorista)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + idMotorista));

        if (motorista.getVeiculo() == null) {
            throw new BusinessException("Motorista deve possuir um veículo cadastrado para criar viagens");
        }

        Optional<Viagem> viagemExistente = viagemRepository.findByMotoristaIdAndPeriodo(idMotorista, viagem.getPeriodo());
        if (viagemExistente.isPresent()) {
            throw new ConflictException("Motorista já possui uma viagem ativa para este período: " + viagem.getPeriodo());
        }

        viagem.setMotorista(motorista);
        return viagemRepository.save(viagem);
    }

    public List<ParadaDTO> alterarStatusPresenca(Long idViagemDia, Long idDependente, String novoStatus) {
        if (idViagemDia == null || idDependente == null || novoStatus == null) {
            throw new IllegalArgumentException("IDs de viagem, dependente e novo status são obrigatórios");
        }

        ViagemPresenca presenca = viagemPresencaRepository.findByViagemDiaIdAndDependenteId(idViagemDia, idDependente)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de presença não encontrado"));

        StatusViagemPresenca status;
        try {
            status = StatusViagemPresenca.valueOf(novoStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status de presença inválido: " + novoStatus);
        }

        if (status == StatusViagemPresenca.EMBARCADO) {
            presenca.setHorarioEmbarque(LocalDateTime.now());
        } else if (status == StatusViagemPresenca.DESEMBARCADO) {
            presenca.setHorarioDesembarque(LocalDateTime.now());
        }

        presenca.setStatus(status);
        viagemPresencaRepository.save(presenca);

        return gerarParadasViagemDia(idViagemDia);
    }

    public ViagemDia alterarStatusViagemDia(Long idViagemDia, String novoStatus) {
        if (idViagemDia == null || novoStatus == null) throw new IllegalArgumentException("ID da viagem e novo status são obrigatórios");

        ViagemDia viagemDia = viagemDiaRepository.findById(idViagemDia)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia));

        StatusViagemDia status;
        try {
            status = StatusViagemDia.valueOf(novoStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status de execução de viagem inválido: " + novoStatus);
        }

        viagemDia.setStatus(status);
        viagemDia.setDataUltimaAlteracaoStatus(LocalDateTime.now());

        return viagemDiaRepository.save(viagemDia);
    }

    public Viagem buscarViagem(Long idViagem) {
        return viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
    }

    public ViagemDiaDTO visualizarViagemDia(Long idViagemDia) {
        return viagemDiaRepository.findById(idViagemDia)
                .map(ViagemDiaDTO::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + idViagemDia));
    }

    public List<ViagemDependente> buscarVinculados(Long idViagem) {
        return viagemDependenteRepository.findDetalhesByViagemId(idViagem);
    }
}

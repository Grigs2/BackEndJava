package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.*;
import TioDaPerua.BackEndJava.entities.*;
import TioDaPerua.BackEndJava.enums.*;
import TioDaPerua.BackEndJava.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    public List<ParadaDTO> gerarParadasViagemDia(Long idViagemDia) {
        Optional<ViagemDia> viagemDiaOpt = viagemDiaRepository.findById(idViagemDia);
        if (viagemDiaOpt.isEmpty()) return new ArrayList<>();

        ViagemDia viagemDia = viagemDiaOpt.get();
        Viagem viagem = viagemDia.getViagem();
        if (viagem == null) return new ArrayList<>();

        List<ViagemDependente> vinculados = viagemDependenteRepository.buscarDependentesCompletos(viagem.getId());

        boolean ida = isViagemIda(viagem.getPeriodo());

        List<ParadaDTO> paradas = new ArrayList<>();
        int ordem = 1;

        // 1. Casa do Motorista (Início)
        paradas.add(criarParadaMotorista(ordem++, viagem.getMotorista(), "Saída: Casa do Motorista"));

        if (ida) {
            // IDA: Motorista -> Responsáveis -> Escolas -> Motorista
            List<ParadaDTO> paradasResp = adicionarParadasResponsaveis(ordem, vinculados, idViagemDia);
            paradas.addAll(paradasResp);
            ordem = paradas.size() + 1;
            List<ParadaDTO> paradasEscola = adicionarParadasEscolas(ordem, vinculados, idViagemDia);
            paradas.addAll(paradasEscola);
        } else {
            // VOLTA: Motorista -> Escolas -> Responsáveis -> Motorista
            List<ParadaDTO> paradasEscola = adicionarParadasEscolas(ordem, vinculados, idViagemDia);
            paradas.addAll(paradasEscola);
            ordem = paradas.size() + 1;
            List<ParadaDTO> paradasResp = adicionarParadasResponsaveis(ordem, vinculados, idViagemDia);
            paradas.addAll(paradasResp);
        }

        ordem = paradas.size() + 1;

        // Casa do Motorista (Fim)
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
        Optional<Viagem> viagemOpt = viagemRepository.findById(idViagem);
        if (viagemOpt.isEmpty()) return null;

        Viagem viagem = viagemOpt.get();
        LocalDate hoje = LocalDate.now();

        // Validar se já existe viagem dia para hoje
        Optional<ViagemDia> existente = viagemDiaRepository.findByViagemIdAndData(idViagem, hoje);
        if (existente.isPresent()) return existente.get();

        // Criar ViagemDia
        ViagemDia viagemDia = new ViagemDia();
        viagemDia.setViagem(viagem);
        viagemDia.setData(hoje);
        viagemDia.setStatus(StatusViagemDia.PLANEJADA);
        viagemDia = viagemDiaRepository.save(viagemDia);

        // Buscar dependentes ativos
        List<ViagemDependente> ativos = viagemDependenteRepository.findAtivosByViagemId(idViagem);

        // Criar presenças
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
        if (viagem == null || idMotorista == null) return null;

        // Validar motorista existente
        Optional<Motorista> motoristaOpt = motoristaRepository.findById(idMotorista);
        if (motoristaOpt.isEmpty()) return null;

        Motorista motorista = motoristaOpt.get();

        // Validar motorista possui veículo
        if (motorista.getVeiculo() == null) return null;

        // Validar duplicidade: motorista + periodo
        Optional<Viagem> viagemExistente = viagemRepository.findByMotoristaIdAndPeriodo(idMotorista, viagem.getPeriodo());
        if (viagemExistente.isPresent()) return null;

        // Configurar entidade
        viagem.setMotorista(motorista);

        // Persistir
        return viagemRepository.save(viagem);
    }

    public List<ParadaDTO> alterarStatusPresenca(Long idViagemDia, Long idDependente, String novoStatus) {
        if (idViagemDia == null || idDependente == null || novoStatus == null) return null;

        Optional<ViagemPresenca> presencaOpt = viagemPresencaRepository.findByViagemDiaIdAndDependenteId(idViagemDia, idDependente);
        if (presencaOpt.isEmpty()) return null;

        ViagemPresenca presenca = presencaOpt.get();
        StatusViagemPresenca status;
        try {
            status = StatusViagemPresenca.valueOf(novoStatus);
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Regras de transição e timestamps
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
        if (idViagemDia == null || novoStatus == null) return null;

        Optional<ViagemDia> viagemDiaOpt = viagemDiaRepository.findById(idViagemDia);
        if (viagemDiaOpt.isEmpty()) return null;

        ViagemDia viagemDia = viagemDiaOpt.get();
        StatusViagemDia status;
        try {
            status = StatusViagemDia.valueOf(novoStatus);
        } catch (IllegalArgumentException e) {
            return null;
        }

        viagemDia.setStatus(status);
        viagemDia.setDataUltimaAlteracaoStatus(LocalDateTime.now());

        return viagemDiaRepository.save(viagemDia);
    }

    public Viagem buscarViagem(Long idViagem) {
        return viagemRepository.findById(idViagem).orElse(null);
    }

    public List<ViagemDependente> buscarVinculados(Long idViagem) {
        return viagemDependenteRepository.findDetalhesByViagemId(idViagem);
    }
}

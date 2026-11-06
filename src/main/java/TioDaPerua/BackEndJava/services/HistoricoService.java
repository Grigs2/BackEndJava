package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.HistoricoAlunoDTO;
import TioDaPerua.BackEndJava.DTOs.ViagemDiaResumoDTO;
import TioDaPerua.BackEndJava.entities.HistoricoViagemDia;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.repositories.HistoricoViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Históricos de viagem: por aluno (responsável) e por motorista. Padrão: últimos 30 dias; máximo 1 ano. */
@Service
public class HistoricoService {

    static final int DIAS_PADRAO = 30;
    static final int DIAS_MAXIMOS = 366;

    @Autowired private ViagemPresencaRepository viagemPresencaRepository;
    @Autowired private ViagemDiaRepository viagemDiaRepository;
    @Autowired private HistoricoViagemDiaRepository historicoViagemDiaRepository;

    @Transactional(readOnly = true)
    public List<HistoricoAlunoDTO> doAluno(Long idDependente, LocalDate de, LocalDate ate) {
        LocalDate[] faixa = faixa(de, ate);
        return viagemPresencaRepository.findHistoricoByDependenteId(idDependente).stream()
                .filter(p -> p.getViagemDia() != null && dentro(p.getViagemDia().getData(), faixa))
                .map(this::paraAluno)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ViagemDiaResumoDTO> doMotorista(Long idMotorista, LocalDate de, LocalDate ate) {
        LocalDate[] faixa = faixa(de, ate);
        return viagemDiaRepository.findByMotoristaIdAndDataBetween(idMotorista, faixa[0], faixa[1]).stream()
                .sorted(Comparator.comparing(ViagemDia::getData).reversed().thenComparing(ViagemDia::getId, Comparator.reverseOrder()))
                .map(this::paraMotorista)
                .toList();
    }

    private HistoricoAlunoDTO paraAluno(ViagemPresenca p) {
        ViagemDia dia = p.getViagemDia();
        Viagem viagem = dia.getViagem();
        Motorista m = viagem == null ? null : viagem.getMotorista();
        Encerramento e = encerramento(dia.getId());
        return new HistoricoAlunoDTO(
                dia.getId(),
                dia.getData(),
                viagem == null ? null : viagem.getNome(),
                viagem == null || viagem.getPeriodo() == null ? null : viagem.getPeriodo().name(),
                m == null ? null : m.getNome(),
                m == null || m.getVeiculo() == null ? null : m.getVeiculo().getPlaca(),
                p.getStatus() == null ? null : p.getStatus().name(),
                p.getHorarioEmbarque(),
                p.getHorarioDesembarque(),
                dia.getStatus() == null ? null : dia.getStatus().name(),
                e.motivo());
    }

    private ViagemDiaResumoDTO paraMotorista(ViagemDia dia) {
        List<ViagemPresenca> presencas = viagemPresencaRepository.findByViagemDiaId(dia.getId());
        Viagem viagem = dia.getViagem();
        Encerramento e = encerramento(dia.getId());
        return new ViagemDiaResumoDTO(
                dia.getId(),
                dia.getData(),
                dia.getStatus() == null ? null : dia.getStatus().name(),
                dia.getDataUltimaAlteracaoStatus(),
                viagem == null ? null : viagem.getId(),
                viagem == null ? null : viagem.getNome(),
                viagem == null || viagem.getPeriodo() == null ? null : viagem.getPeriodo().name(),
                presencas.size(),
                (int) presencas.stream().filter(p -> p.getHorarioEmbarque() != null).count(),
                (int) presencas.stream().filter(p -> p.getStatus() == StatusViagemPresenca.DESEMBARCADO).count(),
                (int) presencas.stream().filter(p -> p.getStatus() == StatusViagemPresenca.FALTOU).count(),
                e.inicio(),
                e.fim(),
                e.motivo());
    }

    private record Encerramento(LocalDateTime inicio, LocalDateTime fim, String motivo) {
    }

    /** Início (passou a EM_ANDAMENTO), fim (finalizada, cancelada ou interrompida) e motivo, pelo histórico de status. */
    private Encerramento encerramento(Long viagemDiaId) {
        LocalDateTime inicio = null;
        LocalDateTime fim = null;
        String motivo = null;
        for (HistoricoViagemDia h : historicoViagemDiaRepository.listarPorViagemDia(viagemDiaId)) {
            if (h.getStatusNovo() == StatusViagemDia.EM_ANDAMENTO && inicio == null) inicio = h.getOcorridoEm();
            if (h.getStatusNovo() == StatusViagemDia.FINALIZADA || h.getStatusNovo() == StatusViagemDia.CANCELADA
                    || h.getStatusNovo() == StatusViagemDia.INTERROMPIDA) {
                fim = h.getOcorridoEm();
                motivo = h.getMotivo();
            }
        }
        return new Encerramento(inicio, fim, motivo);
    }

    static LocalDate[] faixa(LocalDate de, LocalDate ate) {
        LocalDate fim = ate == null ? LocalDate.now() : ate;
        LocalDate inicio = de == null ? fim.minusDays(DIAS_PADRAO) : de;
        if (inicio.isAfter(fim)) throw new BusinessException("A data inicial deve ser anterior à final");
        if (inicio.plusDays(DIAS_MAXIMOS).isBefore(fim)) throw new BusinessException("Consulte no máximo 1 ano por vez");
        return new LocalDate[]{inicio, fim};
    }

    private static boolean dentro(LocalDate data, LocalDate[] faixa) {
        return data != null && !data.isBefore(faixa[0]) && !data.isAfter(faixa[1]);
    }
}

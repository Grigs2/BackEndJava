package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.AusenciaDTO;
import TioDaPerua.BackEndJava.entities.AusenciaInformada;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import TioDaPerua.BackEndJava.enums.TipoNotificacao;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ausência avisada pelo responsável ("meu filho não vai amanhã").
 * - vale para uma data (de hoje até 60 dias) e um trecho: TODOS, IDA ou VOLTA;
 * - se a viagem daquele dia ainda não foi preparada, o aluno entra como FALTOU quando ela for;
 * - se já foi preparada e ainda não terminou, o aluno que está aguardando passa a FALTOU na hora;
 * - o motorista e os outros responsáveis do aluno recebem um aviso.
 */
@Service
public class AusenciaService {

    static final int DIAS_MAXIMOS = 60;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    @Autowired private AusenciaInformadaRepository ausenciaRepository;
    @Autowired private DependenteRepository dependenteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ViagemDependenteRepository viagemDependenteRepository;
    @Autowired private ViagemDiaRepository viagemDiaRepository;
    @Autowired private ViagemPresencaRepository viagemPresencaRepository;
    @Autowired private ResponsavelRepository responsavelRepository;
    @Autowired private ViagemService viagemService;
    @Autowired private NotificacaoService notificacaoService;

    @Transactional(readOnly = true)
    public List<AusenciaDTO> listar(Long idDependente) {
        return ausenciaRepository.findByDependenteIdAndDataGreaterThanEqualOrderByDataAsc(idDependente, LocalDate.now())
                .stream().map(AusenciaDTO::toDTO).toList();
    }

    @Transactional
    public AusenciaDTO registrar(Long idDependente, Long atorUsuarioId, LocalDate data, String trecho, String motivo) {
        String t = normalizarTrecho(trecho);
        LocalDate hoje = LocalDate.now();
        if (data == null || data.isBefore(hoje) || data.isAfter(hoje.plusDays(DIAS_MAXIMOS))) {
            throw new BusinessException("Escolha uma data entre hoje e os próximos " + DIAS_MAXIMOS + " dias");
        }
        String motivoNormalizado = motivo == null || motivo.isBlank() ? null : motivo.trim();
        if (motivoNormalizado != null && motivoNormalizado.length() > 200) {
            throw new BusinessException("O motivo deve ter no máximo 200 caracteres");
        }
        // "Ida e volta" conflita com qualquer aviso do dia; "só ida"/"só volta" conflita com o mesmo trecho ou com "ida e volta".
        boolean conflito = AusenciaInformada.TODOS.equals(t)
                ? ausenciaRepository.existsByDependenteIdAndData(idDependente, data)
                : ausenciaRepository.existsByDependenteIdAndDataAndTrecho(idDependente, data, t)
                || ausenciaRepository.existsByDependenteIdAndDataAndTrecho(idDependente, data, AusenciaInformada.TODOS);
        if (conflito) {
            throw new ConflictException("Já existe uma ausência avisada para este aluno nessa data");
        }
        Dependente dependente = dependenteRepository.findById(idDependente)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado com id: " + idDependente));
        Usuario ator = usuarioRepository.findById(atorUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        AusenciaInformada ausencia = new AusenciaInformada();
        ausencia.setDependente(dependente);
        ausencia.setInformadaPor(ator);
        ausencia.setData(data);
        ausencia.setTrecho(t);
        ausencia.setMotivo(motivoNormalizado);
        ausencia.setCriadaEm(Instant.now());
        ausencia = ausenciaRepository.save(ausencia);

        Set<Long> destinatarios = new LinkedHashSet<>();
        for (ViagemDependente vd : viagemDependenteRepository.findAtivosByDependenteId(idDependente)) {
            Viagem viagem = vd.getViagem();
            if (!ausencia.valePara(viagem.getPeriodo())) continue;
            if (viagem.getMotorista() != null && viagem.getMotorista().getUsuario() != null) {
                destinatarios.add(viagem.getMotorista().getUsuario().getId());
            }
            // Viagem do dia já preparada e não encerrada: marca a falta agora.
            viagemDiaRepository.findByViagemIdAndData(viagem.getId(), data)
                    .filter(dia -> dia.getStatus() == StatusViagemDia.PLANEJADA || dia.getStatus() == StatusViagemDia.EM_ANDAMENTO)
                    .flatMap(dia -> viagemPresencaRepository.findByViagemDiaIdAndDependenteId(dia.getId(), idDependente))
                    .filter(p -> p.getStatus() == StatusViagemPresenca.ESPERANDO)
                    .ifPresent(p -> viagemService.marcarFaltaPorAusencia(p, atorUsuarioId));
        }
        responsavelRepository.findUsuariosAtivosPorDependentes(List.of(idDependente))
                .forEach(v -> destinatarios.add(v.getUsuarioId()));

        if (!destinatarios.isEmpty()) {
            MensagensNotificacao.Texto texto = MensagensNotificacao.ausenciaInformada(
                    dependente, descricaoQuando(t, data, hoje), motivoNormalizado);
            notificacaoService.criarNotificacoesAutomaticas(atorUsuarioId, destinatarios, texto.titulo(), texto.mensagem(),
                    new NotificacaoService.Contexto(TipoNotificacao.AUSENCIA_INFORMADA, null, idDependente,
                            "ausencia:" + ausencia.getId()));
        }
        return AusenciaDTO.toDTO(ausencia);
    }

    /** Cancela a ausência enquanto a viagem daquele dia ainda não foi preparada. */
    @Transactional
    public void cancelar(Long idDependente, Long idAusencia) {
        AusenciaInformada ausencia = ausenciaRepository.findByIdAndDependenteId(idAusencia, idDependente)
                .orElseThrow(() -> new ResourceNotFoundException("Ausência não encontrada"));
        boolean jaPreparada = viagemDependenteRepository.findAtivosByDependenteId(idDependente).stream()
                .map(ViagemDependente::getViagem)
                .filter(v -> ausencia.valePara(v.getPeriodo()))
                .anyMatch(v -> viagemDiaRepository.findByViagemIdAndData(v.getId(), ausencia.getData()).isPresent());
        if (jaPreparada) {
            throw new ConflictException("A viagem desse dia já foi preparada. Fale com o motorista para incluir o aluno.");
        }
        ausenciaRepository.delete(ausencia);
    }

    static String normalizarTrecho(String trecho) {
        String t = trecho == null ? AusenciaInformada.TODOS : trecho.trim().toUpperCase();
        if (!t.equals(AusenciaInformada.TODOS) && !t.equals(AusenciaInformada.IDA) && !t.equals(AusenciaInformada.VOLTA)) {
            throw new BusinessException("Trecho inválido. Use TODOS, IDA ou VOLTA");
        }
        return t;
    }

    /** "no transporte de amanhã", "na ida de hoje", "na volta de 12/11". */
    static String descricaoQuando(String trecho, LocalDate data, LocalDate hoje) {
        String dia = data.equals(hoje) ? "hoje" : data.equals(hoje.plusDays(1)) ? "amanhã" : DIA.format(data);
        return switch (trecho) {
            case AusenciaInformada.IDA -> "na ida de " + dia;
            case AusenciaInformada.VOLTA -> "na volta de " + dia;
            default -> "no transporte de " + dia;
        };
    }
}

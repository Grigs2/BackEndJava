package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.EventoPresencaOfflineDTO;
import TioDaPerua.BackEndJava.DTOs.ResultadoEventoPresencaDTO;
import TioDaPerua.BackEndJava.DTOs.SincronizacaoPresencasRespostaDTO;
import TioDaPerua.BackEndJava.DTOs.SincronizarPresencasOfflineDTO;
import TioDaPerua.BackEndJava.enums.StatusEventoSincronizacao;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SincronizacaoOfflineService {

    private static final int TAMANHO_MAXIMO_LOTE = 100;

    private final ViagemService viagemService;

    public SincronizacaoOfflineService(ViagemService viagemService) {
        this.viagemService = viagemService;
    }

    public SincronizacaoPresencasRespostaDTO sincronizarPresencas(
            SincronizarPresencasOfflineDTO requisicao, Long usuarioId) {
        if (requisicao == null || requisicao.eventos() == null || requisicao.eventos().isEmpty()
                || requisicao.eventos().size() > TAMANHO_MAXIMO_LOTE || usuarioId == null) {
            throw new BusinessException("Informe entre 1 e 100 eventos e um usuário autenticado");
        }

        List<ResultadoEventoPresencaDTO> resultados = new ArrayList<>(requisicao.eventos().size());
        for (EventoPresencaOfflineDTO evento : requisicao.eventos()) {
            try {
                resultados.add(viagemService.sincronizarPresencaOffline(evento, usuarioId));
            } catch (AccessDeniedException exception) {
                resultados.add(resultado(evento, StatusEventoSincronizacao.REJEITADO, null,
                        "Usuário não autorizado para esta execução"));
            } catch (ResourceNotFoundException exception) {
                resultados.add(resultado(evento, StatusEventoSincronizacao.REJEITADO, null,
                        exception.getMessage()));
            } catch (BusinessException exception) {
                resultados.add(resultado(evento, StatusEventoSincronizacao.REJEITADO, null,
                        exception.getMessage()));
            } catch (ConflictException exception) {
                resultados.add(resultado(evento, StatusEventoSincronizacao.CONFLITO, null,
                        exception.getMessage()));
            }
        }
        return new SincronizacaoPresencasRespostaDTO(List.copyOf(resultados));
    }

    private ResultadoEventoPresencaDTO resultado(
            EventoPresencaOfflineDTO evento,
            StatusEventoSincronizacao status,
            TioDaPerua.BackEndJava.enums.StatusViagemPresenca estadoAtual,
            String mensagem) {
        return new ResultadoEventoPresencaDTO(
                evento == null ? null : evento.idEvento(),
                evento == null ? null : evento.idViagemDia(),
                evento == null ? null : evento.idDependente(),
                status, estadoAtual, mensagem);
    }
}

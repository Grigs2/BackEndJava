package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.*;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.services.SolicitacaoService;
import TioDaPerua.BackEndJava.services.ViagemService;
import TioDaPerua.BackEndJava.services.LocalizacaoViagemService;
import TioDaPerua.BackEndJava.services.SincronizacaoOfflineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import java.util.Map;
import java.time.Instant;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/Viagem")
public class ViagemController {

    @Autowired
    private ViagemService viagemService;

    @Autowired
    private SolicitacaoService solicitacaoService;

    @Autowired
    private LocalizacaoViagemService localizacaoViagemService;

    @Autowired
    private SincronizacaoOfflineService sincronizacaoOfflineService;

    @PostMapping("/CriarViagem/{id_motorista}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isMotorista(authentication, #p0)")
    public ResponseEntity<ViagemDTO> CriarViagem(@PathVariable Long id_motorista, @Valid @RequestBody ViagemDTO dto) {
        if (id_motorista == null || dto == null) {
            return ResponseEntity.badRequest().build();
        }
        Viagem entity = ViagemDTO.toEntity(dto);
        Viagem salva = viagemService.criarViagem(id_motorista, entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ViagemDTO.toDTO(salva));
    }

    @GetMapping("/ListarViagens/{idMotorista}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isMotorista(authentication, #p0)")
    public ResponseEntity<List<ViagemDTO>> ListarViagens(@PathVariable Long idMotorista) {
        if (idMotorista == null) {
            return ResponseEntity.badRequest().build();
        }
        List<ViagemDTO> viagens = viagemService.listarViagensAtivas(idMotorista).stream()
                .map(ViagemDTO::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(viagens);
    }

    @GetMapping("/Agenda/{idMotorista}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isMotorista(authentication, #p0)")
    public ResponseEntity<List<AgendaViagemDTO>> agenda(@PathVariable Long idMotorista) {
        return ResponseEntity.ok(viagemService.agendaMotorista(idMotorista));
    }

    @GetMapping("/Prontidao/{idViagem}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaViagem(authentication, #p0)")
    public ResponseEntity<ProntidaoViagemDTO> prontidao(@PathVariable Long idViagem) {
        return ResponseEntity.ok(viagemService.consultarProntidao(idViagem));
    }

    @PutMapping("/DesativarViagem/{idViagem}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaViagem(authentication, #p0)")
    public ResponseEntity<?> DesativarViagem(@PathVariable Long idViagem) {
        if (idViagem == null) {
            return ResponseEntity.badRequest().build();
        }
        
        viagemService.desativarViagem(idViagem);
        return ResponseEntity.ok(Map.of("mensagem", "Viagem desativada com sucesso"));
    }

    @GetMapping("/Historico/{idMotorista}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isMotorista(authentication, #p0)")
    public ResponseEntity<List<ViagemDiaDTO>> Historico(@PathVariable Long idMotorista) {
        if (idMotorista == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(viagemService.historicoViagens(idMotorista));
    }

    @GetMapping("/ConsultarMotorista/{idViagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.podeAcessarExecucao(authentication, #p0)")
    public ResponseEntity<MotoristaViagemDTO> ConsultarMotorista(@PathVariable Long idViagemDia) {
        if (idViagemDia == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(viagemService.consultarMotorista(idViagemDia));
    }

    @GetMapping("/Visualizar/{id_viagem}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.podeAcessarViagem(authentication, #p0)")
    public ResponseEntity<ViagemDetalhadaDTO> Visualizar(@PathVariable Long id_viagem) {
        if (id_viagem == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Viagem viagem = viagemService.buscarViagem(id_viagem);
        List<ViagemDependente> vinculados = viagemService.buscarVinculados(id_viagem);

        List<DependenteResumoDTO> dependentes = vinculados.stream().map(vd -> {
            var dep = vd.getDependente();
            
            EscolaResumoDTO escolaDTO = null;
            if (dep.getEscola() != null) {
                escolaDTO = new EscolaResumoDTO(dep.getEscola().getId(), dep.getEscola().getNome());
            }

            List<ResponsavelResumoDTO> responsaveisDTO = dep.getResponsaveis().stream()
                    .map(r -> new ResponsavelResumoDTO(r.getId(), r.getNome(), r.getUsuario() != null ? r.getUsuario().getTelefone() : null))
                    .collect(Collectors.toList());

            return new DependenteResumoDTO(dep.getId(), dep.getNome(), dep.getEndereco(), escolaDTO, responsaveisDTO);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(new ViagemDetalhadaDTO(
                viagem.getId(),
                viagem.getMotorista() != null ? viagem.getMotorista().getNome() : null,
                viagem.getPeriodo() != null ? viagem.getPeriodo().name() : null,
                dependentes
        ));
    }

    @GetMapping("/VisualizarViagemDia/{id_viagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.podeAcessarExecucao(authentication, #p0)")
    public ResponseEntity<ViagemDiaDTO> visualizarViagemDia(@PathVariable("id_viagemDia") Long id_viagemDia) {
        if (id_viagemDia == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(viagemService.visualizarViagemDia(id_viagemDia));
    }

    @GetMapping("/DependentesDisponiveis/{id_viagem}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaViagem(authentication, #p0)")
    public ResponseEntity<List<DependenteDTO>> DependentesDisponiveis(@PathVariable Long id_viagem) {
        if (id_viagem == null) {
            return ResponseEntity.badRequest().build();
        }
        List<DependenteDTO> dependentes = solicitacaoService.listarDependentesDisponiveis(id_viagem).stream()
                .map(DependenteDTO::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dependentes);
    }

    @PostMapping("/SolicitarDependente/{id_viagem}/{id_dependente}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaViagem(authentication, #p0)")
    public ResponseEntity<SolicitacaoDTO> SolicitarDependente(@PathVariable Long id_viagem, @PathVariable Long id_dependente) {
        if (id_viagem == null || id_dependente == null) {
            return ResponseEntity.badRequest().build();
        }
        Solicitacao solicitacao = solicitacaoService.solicitarDependente(id_viagem, id_dependente);
        return ResponseEntity.ok(SolicitacaoDTO.toDTO(solicitacao));
    }

    @PostMapping("/IniciarViagemDia/{id_viagem}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaViagem(authentication, #p0)")
    public ResponseEntity<ViagemDiaDTO> IniciarViagemDia(
            @PathVariable Long id_viagem,
            @AuthenticationPrincipal Jwt jwt) {
        if (id_viagem == null) {
            return ResponseEntity.badRequest().build();
        }
        ViagemDia viagemDia = viagemService.iniciarViagemDia(
                id_viagem, Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(ViagemDiaDTO.toDTO(viagemDia));
    }

    @PostMapping("/PrepararViagemDia/{id_viagem}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaViagem(authentication, #p0)")
    public ResponseEntity<ViagemDiaDTO> PrepararViagemDia(
            @PathVariable Long id_viagem,
            @Valid @RequestBody PrepararViagemDiaDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (id_viagem == null || dto == null) {
            return ResponseEntity.badRequest().build();
        }
        ViagemDia viagemDia = viagemService.prepararViagemDia(
                id_viagem, dto.data(), Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(ViagemDiaDTO.toDTO(viagemDia));
    }

    @PutMapping("/AlterarStatusViagemDia")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaExecucao(authentication, #p0.idViagemDia())")
    public ResponseEntity<ViagemDiaDTO> AlterarStatusViagemDia(
            @Valid @RequestBody AlterarStatusViagemDiaDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        ViagemDia atualizada = viagemService.alterarStatusViagemDia(
                dto.idViagemDia(), dto.novoStatus(), dto.motivo(), Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(ViagemDiaDTO.toDTO(atualizada));
    }

    @PutMapping("/AlterarStatusPresenca")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaExecucao(authentication, #p0.idViagemDia())")
    public ResponseEntity<List<ParadaDTO>> AlterarStatusPresenca(
            @Valid @RequestBody AlterarStatusPresencaDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        List<ParadaDTO> paradas = viagemService.alterarStatusPresenca(
                dto.idViagemDia(), dto.idDependente(), dto.novoStatus(), Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(paradas);
    }

    @GetMapping("/BuscarParadasViagemDia/{idViagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.podeAcessarExecucao(authentication, #p0)")
    public ResponseEntity<List<ParadaDTO>> BuscarParadasViagemDia(@PathVariable Long idViagemDia) {
        if (idViagemDia == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(viagemService.gerarParadasViagemDia(idViagemDia));
    }

    @PutMapping("/ReordenarParadasViagemDia/{idViagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaExecucao(authentication, #p0)")
    public ResponseEntity<List<ParadaDTO>> reordenarParadas(
            @PathVariable Long idViagemDia,
            @Valid @RequestBody ReordenarParadasViagemDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (idViagemDia == null || dto == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(viagemService.reordenarParadas(
                idViagemDia, dto.idsParadas(), Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/HistoricoStatusViagemDia/{idViagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaExecucao(authentication, #p0)")
    public ResponseEntity<List<HistoricoViagemDiaDTO>> historicoStatus(@PathVariable Long idViagemDia) {
        return ResponseEntity.ok(viagemService.historicoStatusViagemDia(idViagemDia));
    }

    @GetMapping("/HistoricoOrdemParadas/{idViagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaExecucao(authentication, #p0)")
    public ResponseEntity<List<HistoricoOrdemParadaDTO>> historicoOrdemParadas(
            @PathVariable Long idViagemDia) {
        return ResponseEntity.ok(viagemService.historicoOrdemParadas(idViagemDia));
    }

    @GetMapping("/HistoricoPresenca/{idViagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaExecucao(authentication, #p0)")
    public ResponseEntity<List<HistoricoViagemPresencaDTO>> historicoPresencas(
            @PathVariable Long idViagemDia) {
        return ResponseEntity.ok(viagemService.historicoPresencasViagemDia(idViagemDia));
    }

    @PutMapping("/Localizacao/Consentimento/Motorista")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<ConsentimentoLocalizacaoDTO> consentimentoLocalizacaoMotorista(
            @Valid @RequestBody RegistrarConsentimentoLocalizacaoDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(localizacaoViagemService.registrarConsentimentoMotorista(
                Long.valueOf(jwt.getSubject()), dto.aceito()));
    }

    @PutMapping("/Localizacao/Consentimento/Dependente/{idDependente}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<ConsentimentoLocalizacaoDTO> consentimentoLocalizacaoDependente(
            @PathVariable Long idDependente,
            @Valid @RequestBody RegistrarConsentimentoLocalizacaoDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(localizacaoViagemService.registrarConsentimentoResponsavel(
                idDependente, Long.valueOf(jwt.getSubject()), dto.aceito()));
    }

    @GetMapping("/Localizacao/Consentimento/Execucao/{idViagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaExecucao(authentication, #p0)")
    public ResponseEntity<EstadoConsentimentoLocalizacaoDTO> estadoConsentimentoLocalizacao(
            @PathVariable Long idViagemDia,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(localizacaoViagemService.consultarEstadoConsentimento(
                idViagemDia, Long.valueOf(jwt.getSubject())));
    }

    @PutMapping("/Localizacao")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDonoDaExecucao(authentication, #p0.idViagemDia())")
    public ResponseEntity<LocalizacaoViagemDTO> atualizarLocalizacao(
            @Valid @RequestBody AtualizarLocalizacaoViagemDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(localizacaoViagemService.registrarPosicao(
                dto, Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/Localizacao/Atual/{idViagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.podeAcessarExecucao(authentication, #p0)")
    public ResponseEntity<LocalizacaoViagemDTO> consultarLocalizacaoAtual(
            @PathVariable Long idViagemDia,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(localizacaoViagemService.consultarPosicaoAtual(
                idViagemDia, Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/Localizacao/Historico/{idViagemDia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.podeAcessarExecucao(authentication, #p0)")
    public ResponseEntity<Page<LocalizacaoViagemDTO>> consultarHistoricoLocalizacao(
            @PathVariable Long idViagemDia,
            @RequestParam Instant inicio,
            @RequestParam Instant fim,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "100") int tamanho,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(localizacaoViagemService.consultarHistorico(
                idViagemDia, Long.valueOf(jwt.getSubject()), inicio, fim, pagina, tamanho));
    }

    @PostMapping("/SincronizarPresencasOffline")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<SincronizacaoPresencasRespostaDTO> sincronizarPresencasOffline(
            @Valid @RequestBody SincronizarPresencasOfflineDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(sincronizacaoOfflineService.sincronizarPresencas(
                dto, Long.valueOf(jwt.getSubject())));
    }
}

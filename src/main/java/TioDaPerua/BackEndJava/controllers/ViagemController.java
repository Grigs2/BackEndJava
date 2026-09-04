package TioDaPerua.BackEndJava.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import TioDaPerua.BackEndJava.DTOs.AlterarStatusPresencaDTO;
import TioDaPerua.BackEndJava.DTOs.AlterarStatusViagemDiaDTO;
import TioDaPerua.BackEndJava.DTOs.DependenteDTO;
import TioDaPerua.BackEndJava.DTOs.DependenteResumoDTO;
import TioDaPerua.BackEndJava.DTOs.EscolaResumoDTO;
import TioDaPerua.BackEndJava.DTOs.MotoristaViagemDTO;
import TioDaPerua.BackEndJava.DTOs.ParadaDTO;
import TioDaPerua.BackEndJava.DTOs.ResponsavelResumoDTO;
import TioDaPerua.BackEndJava.DTOs.SolicitacaoDTO;
import TioDaPerua.BackEndJava.DTOs.ViagemDTO;
import TioDaPerua.BackEndJava.DTOs.ViagemDetalhadaDTO;
import TioDaPerua.BackEndJava.DTOs.ViagemDiaDTO;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.services.SolicitacaoService;
import TioDaPerua.BackEndJava.services.ViagemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/Viagem")
public class ViagemController {

    @Autowired
    private ViagemService viagemService;

    @Autowired
    private SolicitacaoService solicitacaoService;

    @PostMapping("/CriarViagem/{id_motorista}")
    public ResponseEntity<ViagemDTO> CriarViagem(@NotNull(message = "ID do motorista está nullo") @PathVariable Long id_motorista, @Valid @RequestBody ViagemDTO dto) {
        if (id_motorista == null || dto == null) {
            return ResponseEntity.badRequest().build();
        }
        Viagem entity = ViagemDTO.toEntity(dto);
        Viagem salva = viagemService.criarViagem(id_motorista, entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ViagemDTO.toDTO(salva));
    }

    @GetMapping("/ListarViagens/{idMotorista}")
    public ResponseEntity<List<ViagemDTO>> ListarViagens(@NotNull(message = "ID do motorista está nullo") @PathVariable Long idMotorista) {
        List<ViagemDTO> viagens = viagemService.listarViagensAtivas(idMotorista).stream()
                .map(ViagemDTO::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(viagens);
    }

    @PutMapping("/DesativarViagem/{idViagem}")
    public ResponseEntity<?> DesativarViagem(@NotNull(message = "ID da viagem está nullo") @PathVariable Long idViagem) {
        viagemService.desativarViagem(idViagem);
        return ResponseEntity.ok(Map.of("mensagem", "Viagem desativada com sucesso"));
    }

    @GetMapping("/Historico/{idMotorista}")
    public ResponseEntity<List<ViagemDiaDTO>> Historico(@NotNull(message = "ID do motorista está nullo") @PathVariable Long idMotorista) {
        return ResponseEntity.ok(viagemService.historicoViagens(idMotorista));
    }

    @GetMapping("/ConsultarMotorista/{idViagemDia}")
    public ResponseEntity<MotoristaViagemDTO> ConsultarMotorista(@NotNull(message = "ID da viagem está nullo") @PathVariable Long idViagemDia) {
        return ResponseEntity.ok(viagemService.consultarMotorista(idViagemDia));
    }

    @GetMapping("/Visualizar/{id_viagem}")
    public ResponseEntity<ViagemDetalhadaDTO> Visualizar(@NotNull(message = "ID da viagem está nullo") @PathVariable Long id_viagem) {
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
    public ResponseEntity<ViagemDiaDTO> visualizarViagemDia(@NotNull(message = "ID da viagem está nullo") @PathVariable("id_viagemDia") Long id_viagemDia) {
       return ResponseEntity.ok(viagemService.visualizarViagemDia(id_viagemDia));
    }

    @GetMapping("/DependentesDisponiveis/{id_viagem}")
    public ResponseEntity<List<DependenteDTO>> DependentesDisponiveis(@NotNull(message = "ID da viagem está nullo") @PathVariable Long id_viagem) {
        List<DependenteDTO> dependentes = solicitacaoService.listarDependentesDisponiveis(id_viagem).stream()
                .map(DependenteDTO::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dependentes);
    }

    @PostMapping("/SolicitarDependente/{id_viagem}/{id_dependente}")
    public ResponseEntity<SolicitacaoDTO> SolicitarDependente(@NotNull(message = "ID da viagem está nullo") @PathVariable Long id_viagem,
                                                              @NotNull(message = "ID do dependente está nullo") @PathVariable Long id_dependente) {
        Solicitacao solicitacao = solicitacaoService.solicitarDependente(id_viagem, id_dependente);
        return ResponseEntity.ok(SolicitacaoDTO.toDTO(solicitacao));
    }

    @PostMapping("/IniciarViagemDia/{id_viagem}")
    public ResponseEntity<ViagemDiaDTO> IniciarViagemDia(@NotNull(message = "ID da viagem está nullo") @PathVariable Long id_viagem) {
        ViagemDia viagemDia = viagemService.iniciarViagemDia(id_viagem);
        return ResponseEntity.ok(ViagemDiaDTO.toDTO(viagemDia));
    }

    @PutMapping("/AlterarStatusViagemDia")
    public ResponseEntity<ViagemDiaDTO> AlterarStatusViagemDia(@NotNull(message = "Viagem está nullo") @Valid @RequestBody AlterarStatusViagemDiaDTO dto) {
        ViagemDia atualizada = viagemService.alterarStatusViagemDia(dto.idViagemDia(), dto.novoStatus());
        return ResponseEntity.ok(ViagemDiaDTO.toDTO(atualizada));
    }

    @PutMapping("/AlterarStatusPresenca")
    public ResponseEntity<List<ParadaDTO>> AlterarStatusPresenca(@NotNull(message = "Viagem está nullo") @Valid @RequestBody AlterarStatusPresencaDTO dto) {
        List<ParadaDTO> paradas = viagemService.alterarStatusPresenca(dto.idViagemDia(), dto.idDependente(), dto.novoStatus());
        return ResponseEntity.ok(paradas);
    }

    @GetMapping("/BuscarParadasViagemDia/{idViagemDia}")
    public ResponseEntity<List<ParadaDTO>> BuscarParadasViagemDia(@NotNull(message = "ID da viagem está nullo") @PathVariable Long idViagemDia) {
        return ResponseEntity.ok(viagemService.gerarParadasViagemDia(idViagemDia));
    }
}

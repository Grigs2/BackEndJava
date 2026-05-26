package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.*;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.services.SolicitacaoService;
import TioDaPerua.BackEndJava.services.ViagemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import java.util.Map;

@RestController
@RequestMapping("/Viagem")
public class ViagemController {

    @Autowired
    private ViagemService viagemService;

    @Autowired
    private SolicitacaoService solicitacaoService;

    @PostMapping("/CriarViagem/{id_motorista}")
    public ResponseEntity<ViagemDTO> CriarViagem(@PathVariable Long id_motorista, @RequestBody ViagemDTO dto) {
        if (id_motorista == null || dto == null) {
            return ResponseEntity.badRequest().build();
        }
        Viagem entity = ViagemDTO.toEntity(dto);
        Viagem salva = viagemService.criarViagem(id_motorista, entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ViagemDTO.toDTO(salva));
    }

    @GetMapping("/ListarViagens/{idMotorista}")
    public ResponseEntity<List<ViagemDTO>> ListarViagens(@PathVariable Long idMotorista) {
        if (idMotorista == null) {
            return ResponseEntity.badRequest().build();
        }
        List<ViagemDTO> viagens = viagemService.listarViagensAtivas(idMotorista).stream()
                .map(ViagemDTO::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(viagens);
    }

    @PutMapping("/DesativarViagem/{idViagem}")
    public ResponseEntity<?> DesativarViagem(@PathVariable Long idViagem) {
        if (idViagem == null) {
            return ResponseEntity.badRequest().build();
        }
        
        viagemService.desativarViagem(idViagem);
        return ResponseEntity.ok(Map.of("mensagem", "Viagem desativada com sucesso"));
    }

    @GetMapping("/Historico/{idMotorista}")
    public ResponseEntity<List<ViagemDiaDTO>> Historico(@PathVariable Long idMotorista) {
        if (idMotorista == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(viagemService.historicoViagens(idMotorista));
    }

    @GetMapping("/ConsultarMotorista/{idViagemDia}")
    public ResponseEntity<MotoristaViagemDTO> ConsultarMotorista(@PathVariable Long idViagemDia) {
        if (idViagemDia == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(viagemService.consultarMotorista(idViagemDia));
    }

    @GetMapping("/Visualizar/{id_viagem}")
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
    public ResponseEntity<ViagemDiaDTO> visualizarViagemDia(@PathVariable("id_viagemDia") Long id_viagemDia) {
        if (id_viagemDia == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(viagemService.visualizarViagemDia(id_viagemDia));
    }

    @GetMapping("/DependentesDisponiveis/{id_viagem}")
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
    public ResponseEntity<SolicitacaoDTO> SolicitarDependente(@PathVariable Long id_viagem, @PathVariable Long id_dependente) {
        if (id_viagem == null || id_dependente == null) {
            return ResponseEntity.badRequest().build();
        }
        Solicitacao solicitacao = solicitacaoService.solicitarDependente(id_viagem, id_dependente);
        return ResponseEntity.ok(SolicitacaoDTO.toDTO(solicitacao));
    }

    @PostMapping("/IniciarViagemDia/{id_viagem}")
    public ResponseEntity<ViagemDiaDTO> IniciarViagemDia(@PathVariable Long id_viagem) {
        if (id_viagem == null) {
            return ResponseEntity.badRequest().build();
        }
        ViagemDia viagemDia = viagemService.iniciarViagemDia(id_viagem);
        return ResponseEntity.ok(ViagemDiaDTO.toDTO(viagemDia));
    }

    @PutMapping("/AlterarStatusViagemDia")
    public ResponseEntity<ViagemDiaDTO> AlterarStatusViagemDia(@RequestBody AlterarStatusViagemDiaDTO dto) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        ViagemDia atualizada = viagemService.alterarStatusViagemDia(dto.idViagemDia(), dto.novoStatus());
        return ResponseEntity.ok(ViagemDiaDTO.toDTO(atualizada));
    }

    @PutMapping("/AlterarStatusPresenca")
    public ResponseEntity<List<ParadaDTO>> AlterarStatusPresenca(@RequestBody AlterarStatusPresencaDTO dto) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        List<ParadaDTO> paradas = viagemService.alterarStatusPresenca(dto.idViagemDia(), dto.idDependente(), dto.novoStatus());
        return ResponseEntity.ok(paradas);
    }

    @GetMapping("/BuscarParadasViagemDia/{idViagemDia}")
    public ResponseEntity<List<ParadaDTO>> BuscarParadasViagemDia(@PathVariable Long idViagemDia) {
        if (idViagemDia == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(viagemService.gerarParadasViagemDia(idViagemDia));
    }
}

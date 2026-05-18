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

@RestController
@RequestMapping("/Viagem")
public class ViagemController {

    @Autowired
    private ViagemService viagemService;

    @Autowired
    private SolicitacaoService solicitacaoService;

    @PostMapping("/CriarViagem/{id_motorista}")
    public ViagemDTO CriarViagem(@PathVariable Long id_motorista, @RequestBody ViagemDTO dto) {
        if (id_motorista == null || dto == null) return null;
        Viagem entity = ViagemDTO.toEntity(dto);
        return ViagemDTO.toDTO(viagemService.criarViagem(id_motorista, entity));
    }

    @GetMapping("/Visualizar/{id_viagem}")
    public ViagemDetalhadaDTO Visualizar(@PathVariable Long id_viagem) {
        if (id_viagem == null) return null;
        
        Viagem viagem = viagemService.buscarViagem(id_viagem);
        if (viagem == null) return null;

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

        return new ViagemDetalhadaDTO(
                viagem.getId(),
                viagem.getMotorista() != null ? viagem.getMotorista().getNome() : null,
                viagem.getPeriodo() != null ? viagem.getPeriodo().name() : null,
                dependentes
        );
    }

    @GetMapping("/DependentesDisponiveis/{id_viagem}")
    public List<DependenteDTO> DependentesDisponiveis(@PathVariable Long id_viagem) {
        if (id_viagem == null) return null;
        return solicitacaoService.listarDependentesDisponiveis(id_viagem).stream()
                .map(DependenteDTO::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/SolicitarDependente/{id_viagem}/{id_dependente}")
    public SolicitacaoDTO SolicitarDependente(@PathVariable Long id_viagem, @PathVariable Long id_dependente) {
        if (id_viagem == null || id_dependente == null) return null;
        return SolicitacaoDTO.toDTO(solicitacaoService.solicitarDependente(id_viagem, id_dependente));
    }

    @PostMapping("/IniciarViagemDia/{id_viagem}")
    public ViagemDiaDTO IniciarViagemDia(@PathVariable Long id_viagem) {
        if (id_viagem == null) return null;
        return ViagemDiaDTO.toDTO(viagemService.iniciarViagemDia(id_viagem));
    }

    @PutMapping("/AlterarStatusViagemDia")
    public ViagemDiaDTO AlterarStatusViagemDia(@RequestBody AlterarStatusViagemDiaDTO dto) {
        if (dto == null) return null;
        ViagemDia atualizada = viagemService.alterarStatusViagemDia(dto.idViagemDia(), dto.novoStatus());
        return ViagemDiaDTO.toDTO(atualizada);
    }

    @PutMapping("/AlterarStatusPresenca")
    public List<ParadaDTO> AlterarStatusPresenca(@RequestBody AlterarStatusPresencaDTO dto) {
        if (dto == null) return null;
        return viagemService.alterarStatusPresenca(dto.idViagemDia(), dto.idDependente(), dto.novoStatus());
    }

    @GetMapping("/BuscarParadasViagemDia/{idViagemDia}")
    public List<ParadaDTO> BuscarParadasViagemDia(@PathVariable Long idViagemDia) {
        if (idViagemDia == null) return new ArrayList<>();
        return viagemService.gerarParadasViagemDia(idViagemDia);
    }
}

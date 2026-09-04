package TioDaPerua.BackEndJava.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import TioDaPerua.BackEndJava.DTOs.MotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.DTOs.VeiculoDTO;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.services.MotoristaService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/Motorista")
public class MotoristaController {

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private UsuarioService usuarioService;

    @PutMapping("/AlterarInfos")
    public ResponseEntity<MotoristaDTO> AlterarInfos(@Valid @RequestBody MotoristaDTO dto) {
        if (dto == null || dto.usuarioDTO() == null || dto.usuarioDTO().senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        usuarioService.validarSenha(dto.usuarioDTO().id(), dto.usuarioDTO().senha());

        Motorista atualizado = motoristaService.alterarInfos(MotoristaDTO.toEntity(dto));
        return ResponseEntity.ok(MotoristaDTO.toDTO(atualizado));
    }

    @PostMapping("/Cadastrar")
    public ResponseEntity<MotoristaDTO> Cadastrar(@Valid @RequestBody MotoristaDTO dto) {
        if (dto == null || dto.usuarioDTO() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!"MOTORISTA".equals(dto.usuarioDTO().tipoPerfil())) {
            return ResponseEntity.badRequest().build();
        }

        Motorista motoristaSalvo = motoristaService.cadastraMotorista(MotoristaDTO.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(MotoristaDTO.toDTO(motoristaSalvo));
    }

    @PostMapping("/CadastrarVeiculo/{id_motorista}")
    public ResponseEntity<MotoristaDTO> CadastrarVeiculo(@PathVariable Long id_motorista, @Valid @RequestBody VeiculoDTO dto) {
        if (dto == null || id_motorista == null) {
            return ResponseEntity.badRequest().build();
        }
        Motorista motoristaComVeiculo = motoristaService.cadastrarVeiculo(id_motorista, VeiculoDTO.toEntity(dto));
        return ResponseEntity.ok(MotoristaDTO.toDTO(motoristaComVeiculo));
    }

    @PostMapping("/Autenticar")
    public ResponseEntity<MotoristaDTO> autenticar(@Valid @RequestBody UsuarioDTO dto) {
        if (dto == null || dto.email() == null || dto.senha() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (dto.email().isEmpty() || dto.senha().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Usuario usuarioLogado = usuarioService.autenticar(dto.email(), dto.senha());
        Motorista motorista = usuarioService.logarMotorista(usuarioLogado);
        return ResponseEntity.ok(MotoristaDTO.toDTO(motorista));
    }
}

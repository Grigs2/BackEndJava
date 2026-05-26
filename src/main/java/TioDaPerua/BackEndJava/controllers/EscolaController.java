package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.EscolaDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.services.EscolaService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/Escola")
public class EscolaController {

    @Autowired
    private EscolaService escolaService;

    @Autowired
    private UsuarioService usuarioService;

    @PutMapping("/AlterarInfos")
    public ResponseEntity<EscolaDTO> AlterarInfos(@RequestBody EscolaDTO dto) {
        if (dto == null || dto.usuarioDTO() == null || dto.usuarioDTO().senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        usuarioService.validarSenha(dto.usuarioDTO().id(), dto.usuarioDTO().senha());

        Escola atualizada = escolaService.alterarInfos(EscolaDTO.toEntity(dto));
        return ResponseEntity.ok(EscolaDTO.toDTO(atualizada));
    }

    @PostMapping("/Cadastrar")
    public ResponseEntity<EscolaDTO> cadastrar(@RequestBody EscolaDTO dto) {
        if (dto == null || dto.usuarioDTO() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!"ESCOLA".equals(dto.usuarioDTO().tipoPerfil())) {
            return ResponseEntity.badRequest().build();
        }

        Escola escola = EscolaDTO.toEntity(dto);
        Escola escolaSalva = escolaService.cadastrarEscola(escola);
        return ResponseEntity.status(HttpStatus.CREATED).body(EscolaDTO.toDTO(escolaSalva));
    }

    @PostMapping("/Autenticar")
    public ResponseEntity<EscolaDTO> autenticar(@RequestBody UsuarioDTO dto) {
        if (dto == null || dto.email() == null || dto.senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        Usuario usuario = usuarioService.autenticar(dto.email(), dto.senha());
        Escola escola = usuarioService.logarEscola(usuario);
        return ResponseEntity.ok(EscolaDTO.toDTO(escola));
    }
}

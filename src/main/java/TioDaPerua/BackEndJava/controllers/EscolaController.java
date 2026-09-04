package TioDaPerua.BackEndJava.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import TioDaPerua.BackEndJava.DTOs.EscolaDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.services.EscolaService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/Escola")
public class EscolaController {

    @Autowired
    private EscolaService escolaService;

    @Autowired
    private UsuarioService usuarioService;

    @PutMapping("/AlterarInfos")
    public ResponseEntity<EscolaDTO> AlterarInfos(@Valid @RequestBody EscolaDTO dto) {
        usuarioService.validarSenha(dto.usuarioDTO().id(), dto.usuarioDTO().senha());

        Escola atualizada = escolaService.alterarInfos(EscolaDTO.toEntity(dto));
        return ResponseEntity.ok(EscolaDTO.toDTO(atualizada));
    }

    @PostMapping("/Cadastrar")
    public ResponseEntity<EscolaDTO> cadastrar(@Valid @RequestBody EscolaDTO dto) {
        if (!"ESCOLA".equals(dto.usuarioDTO().tipoPerfil())) {
            throw new IllegalArgumentException("Tipo de perfil inválido para cadastro de escola");
        }

        Escola escola = EscolaDTO.toEntity(dto);
        Escola escolaSalva = escolaService.cadastrarEscola(escola);
        return ResponseEntity.status(HttpStatus.CREATED).body(EscolaDTO.toDTO(escolaSalva));
    }

    @PostMapping("/Autenticar")
    public ResponseEntity<EscolaDTO> autenticar(@Valid @RequestBody UsuarioDTO dto) {
        
        Usuario usuario = usuarioService.autenticar(dto.email(), dto.senha());
        Escola escola = usuarioService.logarEscola(usuario);
        return ResponseEntity.ok(EscolaDTO.toDTO(escola));
    }
}

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

        if (!usuarioService.verificarSenha(dto.usuarioDTO().id(), dto.usuarioDTO().senha())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Escola atualizada = escolaService.alterarInfos(EscolaDTO.toEntity(dto));
        if (atualizada == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(EscolaDTO.toDTO(atualizada));
    }

    @PostMapping("/Cadastrar")
    public EscolaDTO cadastrar(@RequestBody EscolaDTO dto) {
        if (dto == null || dto.usuarioDTO() == null) return null;
        if (!"ESCOLA".equals(dto.usuarioDTO().tipoPerfil())) return null;

        Escola escola = EscolaDTO.toEntity(dto);
        Escola escolaSalva = escolaService.cadastrarEscola(escola);
        return EscolaDTO.toDTO(escolaSalva);
    }

    @PostMapping("/Autenticar")
    public EscolaDTO autenticar(@RequestBody UsuarioDTO dto) {
        if (dto == null || dto.email() == null || dto.senha() == null) return null;

        Usuario usuario = usuarioService.autenticar(dto.email(), dto.senha());
        if (usuario != null) {
            Escola escola = usuarioService.logarEscola(usuario);
            return EscolaDTO.toDTO(escola);
        }
        return null;
    }
}

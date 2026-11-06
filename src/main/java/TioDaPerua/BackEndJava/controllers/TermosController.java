package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.services.TermosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Informações públicas dos Termos de Uso e da Política de Privacidade.
 * O PDF fica em {@code src/main/resources/static/termos/} e é servido em {@code /TioDaPerua/api/termos/...}.
 */
@RestController
@RequestMapping("/Termos")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Termos")
public class TermosController {

    private final TermosService termosService;

    public TermosController(TermosService termosService) {
        this.termosService = termosService;
    }

    /** { versao, caminhoPdf } — caminhoPdf é relativo à base da API. */
    @GetMapping("/vigente")
    public ResponseEntity<Map<String, Object>> vigente() {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("versao", termosService.getVersaoVigente());
        corpo.put("caminhoPdf", TermosService.CAMINHO_PDF);
        return ResponseEntity.ok(corpo);
    }
}

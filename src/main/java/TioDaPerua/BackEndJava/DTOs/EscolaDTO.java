package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Escola;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EscolaDTO(
                        Long id,
                        @NotBlank(message = "Nome está nullo") String nome,
                        @NotBlank(message = "Administrador responsável está nullo") String admResponsavel,
                        @NotNull(message = "Usuário está nullo") @Valid UsuarioDTO usuarioDTO) {

    public static EscolaDTO toDTO(Escola escola) {
        if (escola == null) { return null; }
        return new EscolaDTO(
                escola.getId(),
                escola.getNome(),
                escola.getAdmResponsavel(),
                UsuarioDTO.toDTO(escola.getUsuario())
        );
    }

    public static Escola toEntity(EscolaDTO dto) {
        Escola escola = new Escola();
        escola.setId(dto.id());
        escola.setNome(dto.nome());
        escola.setAdmResponsavel(dto.admResponsavel());
        escola.setUsuario(UsuarioDTO.toEntity(dto.usuarioDTO()));
        return escola;
    }
}

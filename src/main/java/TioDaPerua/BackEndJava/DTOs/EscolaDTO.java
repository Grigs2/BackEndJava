package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Escola;

public record EscolaDTO(Long id,
                        String nome,
                        String admResponsavel,
                        UsuarioDTO usuarioDTO) {

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
        if (dto == null) { return null; }
        Escola escola = new Escola();
        escola.setId(dto.id());
        escola.setNome(dto.nome());
        escola.setAdmResponsavel(dto.admResponsavel());
        escola.setUsuario(UsuarioDTO.toEntity(dto.usuarioDTO()));
        return escola;
    }
}

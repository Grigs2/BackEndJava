package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Escola;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record EscolaDTO(Long id,
                        @NotBlank String nome,
                        String cnpj,
                        String admResponsavel,
                        @Valid UsuarioDTO usuarioDTO,
                        @Valid EnderecoOperacionalDTO enderecoOperacionalDTO) {

    public EscolaDTO(Long id, String nome, String cnpj, String admResponsavel, UsuarioDTO usuarioDTO) {
        this(id, nome, cnpj, admResponsavel, usuarioDTO, null);
    }

    public static EscolaDTO toDTO(Escola escola) {
        if (escola == null) { return null; }
        return new EscolaDTO(
                escola.getId(),
                escola.getNome(),
                escola.getCnpj(),
                escola.getAdmResponsavel(),
                UsuarioDTO.toDTO(escola.getUsuario()),
                escola.getUsuario() == null ? null
                        : EnderecoOperacionalDTO.toDTO(escola.getUsuario().getEnderecoOperacional())
        );
    }

    public static Escola toEntity(EscolaDTO dto) {
        if (dto == null) { return null; }
        Escola escola = new Escola();
        escola.setId(dto.id());
        escola.setNome(dto.nome());
        escola.setCnpj(dto.cnpj());
        escola.setAdmResponsavel(dto.admResponsavel());
        escola.setUsuario(UsuarioDTO.toEntity(dto.usuarioDTO()));
        if (escola.getUsuario() != null && dto.enderecoOperacionalDTO() != null) {
            escola.getUsuario().setEnderecoOperacional(
                    EnderecoOperacionalDTO.toEntity(dto.enderecoOperacionalDTO()));
        }
        return escola;
    }
}

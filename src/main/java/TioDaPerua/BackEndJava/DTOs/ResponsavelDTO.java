package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Responsavel;

import java.time.LocalDate;
import java.util.List;

public record ResponsavelDTO(Long id, String nome, String cpf, LocalDate dataNascimento,
                             UsuarioDTO usuario, List<DependenteDTO> dependentes) {

    public static ResponsavelDTO toDTO(Responsavel responsavel){
        if(responsavel == null){return null;}
        return new ResponsavelDTO(responsavel.getId(), responsavel.getNome(), responsavel.getCpf(), responsavel.getDataNascimento(),
                UsuarioDTO.toDTO(responsavel.getUsuario()), DependenteDTO.toListDTO(responsavel.getDependentes()) );
    }

    public static Responsavel toEntity(ResponsavelDTO dto){
        if(dto == null){return null;}
        return new Responsavel(dto.id(), dto.nome(), dto.cpf(), dto.dataNascimento(), DependenteDTO.toListEntity(dto.dependentes()),
                UsuarioDTO.toEntity(dto.usuario()));
    }

}

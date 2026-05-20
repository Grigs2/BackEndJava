package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Dependente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record DependenteDTO(Long id,
                            String nome,
                            String cpf,
                            LocalDate dataNascimento,
                            String periodo,
                            String endereco,
                            EscolaResumoDTO escola,
                            String senha) {


    public static DependenteDTO toDTO(Dependente dependente) {
        if (dependente == null) { return null; }
        
        EscolaResumoDTO escolaDTO = null;
        if (dependente.getEscola() != null) {
            escolaDTO = new EscolaResumoDTO(dependente.getEscola().getId(), dependente.getEscola().getNome());
        }

        return new DependenteDTO(dependente.getId(), dependente.getNome(), dependente.getCpf(), dependente.getDataNascimento()
        , dependente.getPeriodo(), dependente.getEndereco(), escolaDTO, null);
    }

    public static Dependente toEntity(DependenteDTO dto) {
        if (dto == null) { return null; }
        return new Dependente(dto.id(), dto.nome(), dto.cpf(), dto.dataNascimento(), dto.periodo(), dto.endereco());
    }

    public static List<DependenteDTO> toListDTO(List<Dependente> dependentes) {
        if (dependentes == null) { return null; }
        List<DependenteDTO> dependenteDTOs = new ArrayList<>();
        dependentes.stream().forEach(dependente -> dependenteDTOs.add(toDTO(dependente)));
        return dependenteDTOs;
    }

    public static List<Dependente> toListEntity(List<DependenteDTO> dependenteDTOs) {
        if (dependenteDTOs == null) { return null; }
        List<Dependente> dependentes = new ArrayList<>();
        dependenteDTOs.stream().forEach(dependente -> dependentes.add(toEntity(dependente)));
        return dependentes;
    }

}

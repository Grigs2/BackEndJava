package TioDaPerua.BackEndJava.DTOs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.constraints.br.CPF;

import com.fasterxml.jackson.annotation.JsonFormat;

import TioDaPerua.BackEndJava.entities.Dependente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

public record DependenteDTO(Long id,
                            @NotBlank(message = "Nome do dependente está nullo") String nome,
                            @NotBlank(message = "CPF do dependente está nullo")
                                @Pattern(regexp = "^\\d{8}$", message = "O CEP deve conter exatamente 8 dígitos numéricos.")
                                @CPF(message = "CPF inválido.") String cpf,
                            @NotBlank(message = "Data de nascimento do dependente está nullo")
                                @Past(message = "A data deve ser no passado.")
                                @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dataNascimento,
                            @NotBlank(message = "Período do dependente está nullo") String periodo,
                            @NotBlank(message = "Endereço do dependente está nullo") String endereco,
                            @NotBlank(message = "Escola do dependente está nullo") @Valid EscolaResumoDTO escola,
                            @NotBlank(message = "Senha do dependente está nullo") String senha) {


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

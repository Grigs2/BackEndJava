package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

/**
 * Visão do dependente para o motorista buscar alunos: sem nome completo, CPF, endereço ou senha.
 * Mostra iniciais, idade, período, escola e o responsável, que é o contato da solicitação.
 */
public record DependenteAnonimoDTO(
        Long id,
        String iniciais,
        Integer idade,
        String periodo,
        EscolaResumoDTO escola,
        String responsavelNome) {

    public static DependenteAnonimoDTO toDTO(Dependente dependente) {
        if (dependente == null) {
            return null;
        }
        EscolaResumoDTO escolaDTO = dependente.getEscola() != null
                ? new EscolaResumoDTO(dependente.getEscola().getId(), dependente.getEscola().getNome())
                : null;
        Responsavel responsavel = dependente.getResponsaveis() != null && !dependente.getResponsaveis().isEmpty()
                ? dependente.getResponsaveis().get(0)
                : null;
        return new DependenteAnonimoDTO(
                dependente.getId(),
                iniciais(dependente.getNome()),
                idade(dependente.getDataNascimento()),
                dependente.getPeriodo(),
                escolaDTO,
                responsavel != null ? responsavel.getNome() : null);
    }

    static String iniciais(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String parte : nomeCompleto.trim().split("\\s+")) {
            sb.append(parte.substring(0, 1).toUpperCase(Locale.ROOT)).append('.');
        }
        return sb.toString();
    }

    static Integer idade(LocalDate nascimento) {
        return nascimento != null ? Period.between(nascimento, LocalDate.now()).getYears() : null;
    }
}

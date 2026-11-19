package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

/**
 * Quem está do outro lado de uma solicitação de transporte, para conferir antes de aceitar o vínculo.
 * Nome e telefone vêm completos; CPF, e-mail e placa vêm mascarados; endereço só com bairro e cidade.
 * Exceção: quando o responsável vê o motorista com quem já tem vínculo ativo (solicitação aceita e não encerrada),
 * {@code cpf} vem completo.
 *
 * @param perfil       MOTORISTA (o responsável está vendo o motorista) ou RESPONSAVEL (o motorista está vendo o responsável)
 * @param cpf          CPF completo; preenchido só para o motorista com vínculo ativo, senão null
 * @param vinculoAtivo solicitação aceita e sem data de fim
 */
public record ContraparteDTO(Long solicitacaoId,
                             String perfil,
                             String nome,
                             String telefone,
                             String emailMascarado,
                             String cpfMascarado,
                             String bairro,
                             String cidade,
                             String uf,
                             String viagemNome,
                             String viagemPeriodo,
                             List<String> escolasAtendidas,
                             VeiculoResumo veiculo,
                             Double avaliacaoMedia,
                             Integer totalAvaliacoes,
                             AlunoResumo aluno,
                             String cpf,
                             boolean vinculoAtivo) {

    /** Veículo do motorista; a placa vem mascarada. */
    public record VeiculoResumo(String modelo, String cor, Integer ano, Integer capacidade, String placaMascarada) {
    }

    /** Aluno da solicitação: só o necessário para o transporte (sem CPF e sem endereço completo). */
    public record AlunoResumo(String nome, Integer idade, String escola, String periodo, String bairro, String cidade) {
    }
}

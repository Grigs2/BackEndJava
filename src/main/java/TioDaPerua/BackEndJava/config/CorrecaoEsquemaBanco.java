package TioDaPerua.BackEndJava.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ajustes de esquema que o {@code ddl-auto=update} do Hibernate não faz sozinho.
 *
 * Quando o Hibernate cria uma tabela com coluna de enum, ele adiciona uma restrição CHECK com os valores da época.
 * Ao incluir um valor novo no enum (ex.: {@code Periodo.MANHA_VOLTA}), bancos já existentes passam a recusar o valor,
 * porque o {@code update} não recria a restrição. Aqui a restrição antiga é removida na inicialização; a validação
 * dos valores continua sendo feita pelo próprio enum no código.
 *
 * É idempotente: se a restrição não existir (banco novo ou já corrigido), nada acontece.
 */
@Component
public class CorrecaoEsquemaBanco implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CorrecaoEsquemaBanco.class);

    /** Restrições CHECK de colunas de enum que precisam aceitar valores novos. */
    /** Padrões por turno aplicados às viagens já existentes. Só preenche valores nulos; execuções não são alteradas. */
    static final List<String> COMANDOS = List.of(
            "ALTER TABLE IF EXISTS public.viagem DROP CONSTRAINT IF EXISTS viagem_periodo_check",
            // INTERROMPIDA entrou depois em StatusViagemDia; o CHECK antigo recusava o valor.
            "ALTER TABLE IF EXISTS public.viagem_dia DROP CONSTRAINT IF EXISTS viagem_dia_status_check",
            "ALTER TABLE IF EXISTS public.historico_viagem_dia DROP CONSTRAINT IF EXISTS historico_viagem_dia_status_anterior_check",
            "ALTER TABLE IF EXISTS public.historico_viagem_dia DROP CONSTRAINT IF EXISTS historico_viagem_dia_status_novo_check",
            """
            UPDATE public.viagem SET horario_partida_padrao = CASE periodo
                WHEN 'MANHA_IDA' THEN TIME '07:00' WHEN 'MANHA_VOLTA' THEN TIME '07:00'
                WHEN 'TARDE_IDA' THEN TIME '12:00' WHEN 'TARDE_VOLTA' THEN TIME '12:00'
                WHEN 'NOITE_IDA' THEN TIME '17:00' WHEN 'NOITE_VOLTA' THEN TIME '17:00'
            END
            WHERE horario_partida_padrao IS NULL
            """,
            // Viagens legadas sem escola passam a atender as escolas dos alunos com vínculo ativo na rota.
            // Só casa viagens sem nenhuma escola, então não se repete depois que a viagem ganha escolas.
            """
            INSERT INTO public.viagem_escola_destino (viagem_id, escola_id)
            SELECT DISTINCT vd.viagem_id, d.escola_id
            FROM public.viagem_dependente vd
            JOIN public.dependente d ON d.id = vd.dependente_id
            WHERE vd.ativo = true
              AND d.escola_id IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM public.viagem_escola_destino ved WHERE ved.viagem_id = vd.viagem_id
              )
            """
    );

    private final JdbcTemplate jdbcTemplate;

    public CorrecaoEsquemaBanco(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String comando : COMANDOS) {
            try {
                jdbcTemplate.execute(comando);
            } catch (RuntimeException e) {
                // Não impede a aplicação de subir (ex.: banco que não é PostgreSQL em testes).
                log.warn("Não foi possível aplicar o ajuste de esquema '{}': {}", comando, e.getMessage());
            }
        }
    }
}

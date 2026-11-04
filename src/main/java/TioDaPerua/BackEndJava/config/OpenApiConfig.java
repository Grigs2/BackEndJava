package TioDaPerua.BackEndJava.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
@ConditionalOnProperty(name = "app.docs.enabled", havingValue = "true")
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    // Mesmas rotas liberadas em SecurityConfig.
    private static final Set<String> ROTAS_PUBLICAS = Set.of(
            "/Motorista/Cadastrar", "/Motorista/Autenticar",
            "/Responsavel/Cadastrar", "/Responsavel/Autenticar",
            "/Escola/Cadastrar", "/Escola/Autenticar",
            "/Autenticacao/Entrar", "/Autenticacao/Renovar",
            "/Autenticacao/SolicitarRedefinicaoSenha", "/Autenticacao/RedefinirSenha");

    @Bean
    public OpenAPI tioDaPeruaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tio da Perua - API")
                        .version("0.0.1")
                        .description("""
                                Autenticação: faça `POST /Autenticacao/Entrar`, copie o `accessToken` e informe em **Authorize**.
                                As rotas `/{Perfil}/Autenticar` retornam só o perfil, sem token.

                                Ordem sugerida de teste:
                                1. Cadastrar Escola, Motorista e Responsável; depois `Autenticacao/Entrar` de cada um.
                                2. Motorista: veículo e documentos (CNH, VISTORIA, LICENCIAMENTO); ADMIN aprova os documentos.
                                3. Vínculo Motorista x Escola (solicitação ou convite) até ficar ATIVO.
                                4. Responsável: dependente, escola, endereços (CEP real, validado no ViaCEP).
                                5. Motorista: criar viagem, solicitar dependente; responsável aceita; conferir `Viagem/Prontidao`.
                                6. Consentimento GPS, `PrepararViagemDia`, `AlterarStatusViagemDia` (EM_ANDAMENTO), presenças, localização, FINALIZADA.
                                7. Encerramentos, rotas `/Admin` e demais cenários de erro.

                                Os IDs de perfil (motorista, escola, responsável) diferem do ID de usuário, que é o `sub` do JWT e usado em `/Notificacao`.
                                """))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    public OpenApiCustomizer removerCadeadoDasRotasPublicas() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((caminho, item) -> {
                if (ROTAS_PUBLICAS.contains(caminho) && item.getPost() != null) {
                    item.getPost().setSecurity(List.of());
                }
            });
        };
    }
}

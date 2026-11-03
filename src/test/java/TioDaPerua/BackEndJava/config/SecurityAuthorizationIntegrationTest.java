package TioDaPerua.BackEndJava.config;

import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.StatusConviteResponsavel;
import TioDaPerua.BackEndJava.repositories.AuditoriaAdministrativaRepository;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemEscolaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.admin.bootstrap.enabled=false"
})
class SecurityAuthorizationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EscolaRepository escolaRepository;
    @Autowired private ViagemEscolaRepository viagemEscolaRepository;
    @Autowired private ResponsavelRepository responsavelRepository;
    @Autowired private AuditoriaAdministrativaRepository auditoriaRepository;

    @Test
    void rejectsBusinessRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/Responsavel/ListarEscolas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void passwordChangeRequiresAuthenticationAndReturnsStructuredValidationErrors() throws Exception {
        mockMvc.perform(post("/Autenticacao/AlterarSenha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"\",\"novaSenha\":\"curta\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/Autenticacao/AlterarSenha")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("2", "MOTORISTA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"\",\"novaSenha\":\"curta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("senhaAtual")))
                .andExpect(jsonPath("$.message", containsString("novaSenha")));
    }

    @Test
    void recoveryEndpointsArePublicButValidateTheirPayloads() throws Exception {
        mockMvc.perform(post("/Autenticacao/SolicitarRedefinicaoSenha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/Autenticacao/RedefinirSenha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsWrongRoleAndAnotherUsersNotification() throws Exception {
        String motoristaToken = token("2", "MOTORISTA");

        mockMvc.perform(get("/Responsavel/ListarEscolas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + motoristaToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/Notificacao/Listar/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + motoristaToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void restrictsDocumentReviewQueueToAdministrators() throws Exception {
        mockMvc.perform(get("/Admin/Documentos/Pendentes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("2", "MOTORISTA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        Usuario administrador = usuarioRepository.save(new Usuario(
                "security-test-" + java.util.UUID.randomUUID() + "@example.com",
                "test-password",
                "Endereco de teste",
                "11999999999",
                "ADMIN"));
        mockMvc.perform(get("/Admin/Documentos/Pendentes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "
                                + token(administrador.getId().toString(), "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void executesAdministrativeSearchQueries() {
        var pageable = PageRequest.of(0, 10);

        assertNotNull(usuarioRepository.pesquisarAdministrativamente(
                null, null, null, null, pageable));
        assertNotNull(escolaRepository.pesquisarAdministrativamente(
                null, null, null, pageable));
        assertNotNull(viagemEscolaRepository.pesquisarAdministrativamente(
                null, null, null, pageable));
        assertNotNull(responsavelRepository.pesquisarVinculosAdministrativamente(
                null, null, StatusConviteResponsavel.ACEITO, pageable));
        assertNotNull(auditoriaRepository.pesquisar(
                null, null, null, null, null, null, pageable));
    }

    @Test
    void protectsAddressAndGuardianEndpointsByProfile() throws Exception {
        String motoristaToken = token("2", "MOTORISTA");

        mockMvc.perform(get("/Responsavel/Dependente/1/Enderecos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + motoristaToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/Responsavel/Convites/Pendentes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + motoristaToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/Admin/Dependentes/ResponsavelCriadorPendentes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + motoristaToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void restrictsSchoolDriverLinkEndpointsToTheirProfiles() throws Exception {
        mockMvc.perform(get("/Motorista/Escolas")
                        .param("termo", "Es")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("3", "ESCOLA")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/Escola/Vinculos/Motoristas/Buscar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("2", "MOTORISTA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/Escola/Vinculos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("2", "MOTORISTA")))
                .andExpect(status().isForbidden());
    }

    private String token(String usuarioId, String role) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tiodaperua")
                .subject(usuarioId)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .claim("role", role)
                .claim("sid", "test-session")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
